# Changelog

## 0.1.0-beta (Beta Release & Near-Live Push Notification Fix)

- **Complete Notification Engine Rebuild**: Completely decoupled notifications from in-app feed loading. Switching tabs (Headlines, Breaking, Important) or opening the app no longer triggers redundant or pointless status bar notifications while actively viewing the app.
- **Live Background RSS Polling**: Empowered `FeedNotificationWorker` to directly fetch and parse live RSS feeds (BBC UK, Sky News UK, Guardian UK, The Sun News) in the background when the app is closed or out of focus. Chaining a self-rescheduling 3-minute `OneTimeWorkRequest` alongside a 15-minute periodic heartbeat ensures users receive near-instant notifications for new Breaking and Important stories without ever needing to open the app.
- **100% Free Journalism & Paywall Removal**: Completely removed news outlets requiring paid subscriptions or hard paywalls (Financial Times UK, Independent UK, and Daily Mail News). Sourcing is now strictly dedicated to free public newsfeeds (BBC UK, Sky News UK, Sky Politics, Guardian UK, Guardian Politics, and The Sun News). Added global filtering (`isFreeNewsSource`) to block any cached or incoming paywalled stories across RSS fetching, Firestore live syncing, and background notifications.
- **Version Synchronizing**: Advanced project versioning, fallback constants, and Firestore configuration checks to `0.1.0-beta` (`versionCode = 1`).
