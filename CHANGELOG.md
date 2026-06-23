# Changelog

## 0.0.2-alpha

- Added the foundation for the main menu screen with `Headlines`, `Breaking`, and `Important` sections.
- Limited the app to 2 curated test feeds instead of a broad all-news feed.
- Added Yahoo Headlines and BBC World as the initial feed sources.
- Added section-specific filtering for breaking and important stories.
- Added a feed source note so the current curated source setup is visible during testing.
- Set `versionCode` to `2`.
- Set `versionName` to `0.0.2-alpha`.

## 0.0.1-alpha

- Created the initial Budgie News Android app.
- Added a modern Jetpack Compose news feed UI.
- Added Yahoo News RSS loading for early testing.
- Added RSS parsing for story title, description, link, source, date, and image URL.
- Added story list, top story layout, refresh action, loading state, and error state.
- Added browser opening for tapped stories.
- Added internet permission.
- Added a custom Budgie reading a generic newspaper app icon.
- Generated Android launcher icon assets for all density folders.
- Set package/application id to `com.budgienews.app`.
- Set `versionCode` to `1`.
- Set `versionName` to `0.0.1-alpha`.
- Added `local.properties` SDK path so Android Studio sync works locally.
- Verified resource processing, Kotlin compilation, and Gradle configuration without building an APK.
