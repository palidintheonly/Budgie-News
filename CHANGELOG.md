# Changelog

## 0.0.12-alpha

- Enabled mobile push notifications for the standard "Headlines" news category.
- Added a new Android notification channel for "Headlines".
- Added a "Headlines alerts" toggle in the Settings screen to allow users to opt in or out of regular news notifications.
- Firebase Cloud Functions now pushes "Headlines" notifications unless explicitly disabled by the user's device preference.
- Fixed a bug in the RSS XML parser that caused feeds to fail to load when encountering CDATA or mixed content tags.
- Applied official news organization brand colors to the source badges across the app.
- Ensure strict support for Android 8.1.0 (API 27) and above by locking minSdk.
- Fixed an Android SDK build compatibility issue by downgrading `androidx.core` dependencies to 1.15.0.
