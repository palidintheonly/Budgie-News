# Changelog

## 0.1.0-beta (Beta Release & Near-Live Push Notification Fix)

- **Foreground Notification Suppression**: Implemented activity lifecycle tracking (`AppVisibility`) to completely suppress system tray notifications while the user is actively viewing the app. Clicking tabs or opening news feeds no longer triggers redundant, pointless status bar notifications.
- **Near-Live Background Polling**: Replaced startup worker cancellation with `FeedNotificationWorker`, scheduled via WorkManager. To overcome the standard 15-minute periodic minimum and achieve a "closer to live" real-time feel, the worker chains a self-rescheduling `OneTimeWorkRequest` with an initial delay of **3 minutes** after every run, backed by a 15-minute periodic heartbeat. Users now reliably receive near-instant notifications for new Breaking and Important news when the app is closed or in the background without needing to open the app.
- **Version Synchronizing**: Advanced project versioning, fallback constants, and Firestore configuration checks to `0.1.0-beta` (`versionCode = 1`).
