# Changelog

## 0.1.0-beta (Beta Release & Near-Live Push Notification Fix)

- **Foreground Notification Suppression**: Implemented activity lifecycle tracking (`AppVisibility`) to completely suppress system tray notifications while the user is actively viewing the app. Clicking tabs or opening news feeds no longer triggers redundant, pointless status bar notifications.
- **Near-Live Background Polling**: Replaced startup worker cancellation with `FeedNotificationWorker`, scheduled via WorkManager. To overcome the standard 15-minute periodic minimum and achieve a "closer to live" real-time feel, the worker chains a self-rescheduling `OneTimeWorkRequest` with an initial delay of **3 minutes** after every run, backed by a 15-minute periodic heartbeat. Users now reliably receive near-instant notifications for new Breaking and Important news when the app is closed or in the background without needing to open the app.
- **100% Free Journalism & Paywall Removal**: Completely removed news outlets requiring paid subscriptions or hard paywalls (Financial Times UK, Independent UK, and Daily Mail News). Sourcing is now strictly dedicated to free public newsfeeds (BBC UK, Sky News UK, Sky Politics, Guardian UK, Guardian Politics, and The Sun News). Added global filtering (`isFreeNewsSource`) to block any cached or incoming paywalled stories across RSS fetching, Firestore live syncing, and background notifications.
- **Version Synchronizing**: Advanced project versioning, fallback constants, and Firestore configuration checks to `0.1.0-beta` (`versionCode = 1`).
