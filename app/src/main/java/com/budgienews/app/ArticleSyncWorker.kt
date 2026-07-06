package com.budgienews.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ArticleSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val articleId = inputData.getString(KEY_ARTICLE_ID).orEmpty()
        val category = inputData.getString(KEY_CATEGORY).orEmpty().ifBlank { "Headlines" }
        if (articleId.isBlank()) return@withContext Result.failure()

        runCatching {
            val article = fetchArticle(articleId, category)
            if (isFreeNewsSource(article.source)) {
                BudgieArticleDatabase.get(applicationContext).upsertArticle(article)
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                FirebaseCrashlytics.getInstance().recordException(error)
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            },
        )
    }

    private suspend fun fetchArticle(articleId: String, fallbackCategory: String): LocalArticle {
        val document = Firebase.firestore.collection("articles").document(articleId).get().await()
        if (!document.exists()) error("Firestore article not found: $articleId")
        val publishedAtMillis = document.getLong("publishedAtMillis") ?: System.currentTimeMillis()
        val newestAllowed = BudgieTime.minAllowedMillis()
        if (publishedAtMillis < newestAllowed) error("Firestore article is older than allowed retention window: $articleId")

        return LocalArticle(
            articleId = document.getString("articleId") ?: document.id,
            title = document.getString("title").orEmpty(),
            description = document.getString("description").orEmpty(),
            link = document.getString("link").orEmpty(),
            source = document.getString("source") ?: "Budgie News",
            publishedAt = document.getString("publishedAt").orEmpty(),
            imageUrl = document.getString("imageUrl")?.takeIf { it.isNotBlank() },
            category = document.getString("category") ?: fallbackCategory,
            isRead = false,
        )
    }

    companion object {
        private const val KEY_ARTICLE_ID = "article_id"
        private const val KEY_CATEGORY = "category"

        fun enqueue(context: Context, articleId: String, category: String) {
            val request = OneTimeWorkRequestBuilder<ArticleSyncWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(KEY_ARTICLE_ID, articleId)
                        .putString(KEY_CATEGORY, category)
                        .build(),
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "article-sync-$articleId",
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
