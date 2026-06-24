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
- Added backend article publishing, article fetch, and FCM data push endpoints.
- Added backend DB healing for articles and device token tables.
- Added light backend push queue records for article publish attempts.
- Limited locally pushed articles to the last 24 hours.
