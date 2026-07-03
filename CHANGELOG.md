# Changelog

## 0.0.13-alpha

- Fixed a bug where out-of-app push notifications would fail to open the app by adding the missing `OPEN_ARTICLE` intent filter to `AndroidManifest.xml`.
- Removed Firebase In-App Messaging to reduce bloat as it was deemed pointless.
- Rewrote "curated" UI taglines to sound more journalistic and professional ("Reporting from leading UK newsrooms").
