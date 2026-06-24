# Changelog

## 0.0.9-alpha

- Added silent FCM data-message handling for live article updates.
- Added expedited WorkManager article sync for pushed article IDs.
- Added local article storage with read status and category fields.
- Added live feed refresh from pushed local articles without manual refresh.
- Added runtime notification and location permission requests for alpha testing.
- Added Firebase Auth register/login for Budgie accounts.
- Added Firestore account/settings sync.
- Added Firestore device token registration for push delivery.
- Added Firestore live article listener for instant in-app feed updates.
- Switched pushed article fetches from the old backend API to Firestore documents.
- Added Firebase Cloud Functions setup for Firestore article publish pushes.
- Added Firestore security rules and indexes for Firebase-only data storage.
- Removed the custom MySQL/backend module from Gradle sync.
- Limited locally pushed articles to the last 24 hours.
- Set `versionName` to `0.0.9-alpha`.
