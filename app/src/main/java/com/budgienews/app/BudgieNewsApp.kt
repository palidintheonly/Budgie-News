package com.budgienews.app

import android.app.Application

class BudgieNewsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BudgieNotifications.ensureChannels(this)
        FeedNotificationWorker.schedule(this)
    }
}
