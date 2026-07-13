package com.budgienews.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

open class FeedNotificationWorker(
    context: Context,
    params: WorkerParameters,
    private val targetEdition: NewsEdition? = null,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Always reschedule the next 3-minute check for this worker's region
        scheduleNextOneTimeForEdition(applicationContext, targetEdition)

        // Do not trigger system notifications if the user is actively viewing the app
        if (AppVisibility.isForeground) return@withContext Result.success()

        runCatching {
            // 1. Fetch live news feeds for the targeted region while user is out of the app
            val sourcesToFetch = if (targetEdition != null) {
                FeedSources.filter { it.edition == targetEdition }
            } else {
                FeedSources
            }
            val liveItems = sourcesToFetch
                .map { source ->
                    runCatching { fetchFeed(source).take(15) }
                        .onFailure { if (!it.isExpectedFirestoreMissingError()) FirebaseCrashlytics.getInstance().recordException(it) }
                        .getOrDefault(emptyList())
                }
                .flatten()
                .filter { isFreeNewsSource(it.source) }
                .distinctBy { it.link.ifBlank { it.title } }

            val breakingLive = liveItems.filterFor(NewsSection.BREAKING)
            breakingLive.firstOrNull()?.let { topBreaking ->
                BudgieNotifications.notifyNewArticle(applicationContext, topBreaking, NewsSection.BREAKING)
                publishLiveArticleToFirestore(topBreaking, NewsSection.BREAKING)
            }

            val importantLive = liveItems.filterFor(NewsSection.IMPORTANT)
            importantLive.firstOrNull()?.let { topImportant ->
                BudgieNotifications.notifyNewArticle(applicationContext, topImportant, NewsSection.IMPORTANT)
                publishLiveArticleToFirestore(topImportant, NewsSection.IMPORTANT)
            }

            // 2. Also check Firestore articles collection for backend alerts
            val newestAllowed = BudgieTime.minAllowedMillis()
            val snapshot = Firebase.firestore.collection("articles")
                .whereGreaterThanOrEqualTo("publishedAtMillis", newestAllowed)
                .orderBy("publishedAtMillis", Query.Direction.DESCENDING)
                .limit(25)
                .get()
                .await()

            val db = BudgieArticleDatabase.get(applicationContext)
            val documents = snapshot.documents
            for (document in documents) {
                val publishedAtMillis = document.getLong("publishedAtMillis") ?: continue
                if (publishedAtMillis < newestAllowed) continue
                val article = LocalArticle(
                    articleId = document.getString("articleId") ?: document.id,
                    title = document.getString("title").orEmpty(),
                    description = document.getString("description").orEmpty(),
                    link = document.getString("link").orEmpty(),
                    source = document.getString("source") ?: "Budgie News",
                    publishedAt = document.getString("publishedAt").orEmpty(),
                    imageUrl = document.getString("imageUrl")?.takeIf { it.isNotBlank() },
                    category = document.getString("category") ?: NewsSection.HEADLINES.label,
                    isRead = document.getBoolean("isRead") == true,
                )
                if (!isFreeNewsSource(article.source)) continue
                db.upsertArticle(article)

                val section = runCatching { NewsSection.valueOf(article.category.uppercase()) }.getOrDefault(NewsSection.HEADLINES)
                if (section == NewsSection.BREAKING || section == NewsSection.IMPORTANT) {
                    BudgieNotifications.notifyNewArticle(applicationContext, article.toFeedItem(), section)
                }
            }
            Result.success()
        }.getOrElse { error ->
            if (!error.isExpectedFirestoreMissingError()) FirebaseCrashlytics.getInstance().recordException(error)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun publishLiveArticleToFirestore(item: FeedItem, section: NewsSection) {
        runCatching {
            val articleMap = mapOf(
                "articleId" to item.id,
                "title" to item.title,
                "description" to item.description,
                "link" to item.link,
                "source" to item.source,
                "publishedAt" to item.publishedAt,
                "publishedAtMillis" to System.currentTimeMillis(),
                "imageUrl" to item.imageUrl,
                "category" to section.label,
                "isRead" to false,
            )
            Firebase.firestore.collection("articles")
                .document(item.id.safeFirestoreId())
                .set(articleMap)
                .await()
        }.onFailure { if (!it.isExpectedFirestoreMissingError()) FirebaseCrashlytics.getInstance().recordException(it) }
    }

    companion object {
        const val WORK_NAME_PERIODIC = "budgie-feed-notifications-periodic"
        const val WORK_NAME_ONE_TIME = "budgie-feed-notifications-live"
        const val WORK_NAME_PERIODIC_GB = "budgie-feed-notifications-gb-periodic"
        const val WORK_NAME_PERIODIC_USA = "budgie-feed-notifications-usa-periodic"
        const val WORK_NAME_ONE_TIME_GB = "budgie-feed-notifications-gb-live"
        const val WORK_NAME_ONE_TIME_USA = "budgie-feed-notifications-usa-live"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val gbRequest = PeriodicWorkRequestBuilder<GbFeedNotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC_GB,
                ExistingPeriodicWorkPolicy.KEEP,
                gbRequest,
            )

            val usaRequest = PeriodicWorkRequestBuilder<UsaFeedNotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC_USA,
                ExistingPeriodicWorkPolicy.KEEP,
                usaRequest,
            )

            scheduleNextOneTime(context)
        }

        fun scheduleNextOneTime(context: Context) {
            scheduleNextOneTimeForEdition(context, null)
        }

        fun scheduleNextOneTimeForEdition(context: Context, edition: NewsEdition?) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            if (edition == NewsEdition.GB || edition == null) {
                val gbOneTime = OneTimeWorkRequestBuilder<GbFeedNotificationWorker>()
                    .setInitialDelay(3, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME_ONE_TIME_GB,
                    ExistingWorkPolicy.REPLACE,
                    gbOneTime,
                )
            }

            if (edition == NewsEdition.USA || edition == null) {
                val usaOneTime = OneTimeWorkRequestBuilder<UsaFeedNotificationWorker>()
                    .setInitialDelay(3, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME_ONE_TIME_USA,
                    ExistingWorkPolicy.REPLACE,
                    usaOneTime,
                )
            }
        }
    }
}

class GbFeedNotificationWorker(
    context: Context,
    params: WorkerParameters,
) : FeedNotificationWorker(context, params, NewsEdition.GB)

class UsaFeedNotificationWorker(
    context: Context,
    params: WorkerParameters,
) : FeedNotificationWorker(context, params, NewsEdition.USA)
