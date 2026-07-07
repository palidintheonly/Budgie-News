package com.budgienews.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.android.gms.ads.MobileAds

class BudgieNewsApp : Application() {
    override fun onCreate() {
        val testDeviceIds = listOf("41A10FE4-933C-4E6F-9DDD-1840C05C23B3", com.google.android.gms.ads.AdRequest.DEVICE_ID_EMULATOR)
        val configuration = com.google.android.gms.ads.RequestConfiguration.Builder()
            .setTestDeviceIds(testDeviceIds)
            .build()
        MobileAds.setRequestConfiguration(configuration)
        MobileAds.initialize(this) {}
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
