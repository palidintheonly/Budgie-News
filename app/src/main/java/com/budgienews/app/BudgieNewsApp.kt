package com.budgienews.app

import android.app.Activity
import android.app.Application
import android.os.Bundle

class BudgieNewsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BudgieNotifications.ensureChannels(this)
        AppVisibility.register(this)
        FeedNotificationWorker.schedule(this)
    }
}

object AppVisibility {
    @Volatile
    var isForeground: Boolean = false
        private set

    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            private var startedCount = 0

            override fun onActivityStarted(activity: Activity) {
                startedCount++
                isForeground = true
            }

            override fun onActivityStopped(activity: Activity) {
                startedCount--
                if (startedCount <= 0) {
                    startedCount = 0
                    isForeground = false
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
