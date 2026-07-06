package com.budgienews.app

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BudgieMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        BudgieNotifications.ensureChannels(applicationContext)

        val articleId = message.data["articleId"]
            ?: message.data["id"]
            ?: message.data["link"]
            ?: message.data["url"]
            ?: message.data["storyId"]
            ?: message.messageId
            ?: ""

        val category = message.data["category"]
            ?: message.data["section"]
            ?: message.data["type"]
            ?: "Headlines"

        val title = message.data["title"]
            ?: message.data["headline"]
            ?: message.notification?.title
            ?: "New ${category.lowercase()} story"

        val description = message.data["description"]
            ?: message.data["body"]
            ?: message.notification?.body
            ?: ""

        val source = message.data["source"]
            ?: message.data["outlet"]
            ?: "Budgie News"

        val link = message.data["link"]
            ?: message.data["url"]
            ?: articleId

        if (articleId.isNotBlank() && !articleId.startsWith("0:1") && !articleId.startsWith("msg_")) {
            FirebaseCrashlytics.getInstance().setCustomKey("last_push_article_id", articleId)
            ArticleSyncWorker.enqueue(
                context = applicationContext,
                articleId = articleId,
                category = category,
            )
        }

        val section = runCatching { NewsSection.valueOf(category.uppercase()) }.getOrDefault(NewsSection.HEADLINES)
        val feedItem = FeedItem(
            id = articleId.ifBlank { link }.ifBlank { title },
            title = title,
            description = description,
            link = link.ifBlank { articleId },
            source = source,
            publishedAt = "",
            imageUrl = null,
        )

        BudgieNotifications.notifyNewArticle(
            context = applicationContext,
            item = feedItem,
            section = section,
            isPush = true,
        )
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        FirebaseCrashlytics.getInstance().setCustomKey("fcm_token_ready", token.isNotBlank())
        if (token.isNotBlank()) {
            BudgiePrefs.saveDeviceToken(applicationContext, token)
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    BudgieAccountApi.ensureSession()
                    BudgieAccountApi.registerDevice(applicationContext, token)
                }.onFailure { FirebaseCrashlytics.getInstance().recordException(it) }
            }
        }
    }
}
