package com.budgienews.app

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BudgieMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val articleId = message.data["articleId"].orEmpty()
        if (articleId.isBlank()) return

        val category = message.data["category"].orEmpty().ifBlank { "Headlines" }
        FirebaseCrashlytics.getInstance().setCustomKey("last_push_article_id", articleId)
        ArticleSyncWorker.enqueue(
            context = applicationContext,
            articleId = articleId,
            category = category,
        )
    }

    override fun onNewToken(token: String) {
        FirebaseCrashlytics.getInstance().setCustomKey("fcm_token_ready", token.isNotBlank())
        if (token.isNotBlank()) {
            BudgiePrefs.saveDeviceToken(applicationContext, token)
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { BudgieAccountApi.registerDevice(applicationContext, token) }
                    .onFailure { FirebaseCrashlytics.getInstance().recordException(it) }
            }
        }
    }
}
