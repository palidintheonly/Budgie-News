# Changelog

## 0.0.14-alpha

- Implemented primary custom notification sound (`sound_chirp.ogg`) across the Android app and Firebase Cloud Functions.
- Registered baseline notification channel `channel_budgie_default` ("Budgie News Alerts") configured with high importance and custom audio attributes.
- Updated Firebase push notification payloads to specify `sound: "sound_chirp"` and route standard alerts to the new custom channel.
