# Changelog

## 0.0.14-alpha

- Implemented primary custom notification sound (`sound_chirp.ogg`) across the Android app and Firebase Cloud Functions.
- Registered baseline notification channel `channel_budgie_default` ("Budgie News Alerts") configured with high importance and custom audio attributes.
- Updated Firebase push notification payloads to specify `sound: "sound_chirp"` and route standard alerts to the new custom channel.
- Removed experimental biometric screen lock and user account login/registration features to streamline the app experience and keep it lightweight.
- Extended news feed story retention from 24 hours to 1 week (7 days) across local SQLite storage, live Firestore feeds, background sync workers, and Cloud Functions.
- Added active SQLite database pruning to permanently delete articles older than 1 week upon saving and querying, saving storage space on the device.
- Implemented a news feed reset rule so that stories start fresh from today onwards (July 4, 2026), automatically clearing older cached stories while maintaining the ongoing 7-day retention rule.
- Set up the "Send technical feedback" settings option to launch an email composer addressed to support with app version details.
- Formatted version display in settings to cleanly show `0.0.14-alpha` without internal build codes.
- Cleaned and optimized codebase by removing unused Compose components (`SettingsTextField`, `OutlinedTextField`).
