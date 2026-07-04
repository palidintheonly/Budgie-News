package com.budgienews.app

import android.app.Application
import androidx.work.WorkManager

class BudgieNewsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BudgieNotifications.ensureChannels(this)
        runCatching { WorkManager.getInstance(this).cancelUniqueWork("budgie-feed-notifications") }
    }
}
