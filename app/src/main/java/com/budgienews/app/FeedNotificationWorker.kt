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

class FeedNotificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Always reschedule the next 3-minute check to maintain near-live background checking
        scheduleNextOneTime(applicationContext)

        // Do not trigger system notifications if the user is actively viewing the app
        if (AppVisibility.isForeground) return@withContext Result.success()

        runCatching {
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
                db.upsertArticle(article)

                val section = runCatching { NewsSection.valueOf(article.category.uppercase()) }.getOrDefault(NewsSection.HEADLINES)
                if (section == NewsSection.BREAKING || section == NewsSection.IMPORTANT) {
                    BudgieNotifications.notifyNewArticle(applicationContext, article.toFeedItem(), section)
                }
            }
            Result.success()
        }.getOrElse { error ->
            FirebaseCrashlytics.getInstance().recordException(error)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME_PERIODIC = "budgie-feed-notifications-periodic"
        const val WORK_NAME_ONE_TIME = "budgie-feed-notifications-live"

        fun schedule(context: Context) {
            val periodicRequest = PeriodicWorkRequestBuilder<FeedNotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )

            scheduleNextOneTime(context)
        }

        fun scheduleNextOneTime(context: Context) {
            val oneTimeRequest = OneTimeWorkRequestBuilder<FeedNotificationWorker>()
                .setInitialDelay(3, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONE_TIME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        }
    }
}
