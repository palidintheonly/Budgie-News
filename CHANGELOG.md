# Changelog

## 0.1.0-beta (Beta Release & Background Push Notification Fix)

- **Foreground Notification Suppression**: Implemented activity lifecycle tracking (`AppVisibility`) to completely suppress system tray notifications while the user is actively viewing the app. Clicking tabs or opening news feeds no longer triggers redundant, pointless status bar notifications.
- **Background Periodic News Worker**: Replaced startup worker cancellation with `FeedNotificationWorker`, scheduled via WorkManager to run periodically in the background every 15 minutes. Users now reliably receive notifications for new Breaking and Important news when the app is closed or in the background without needing to open the app.
