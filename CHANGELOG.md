# Changelog

## 0.0.2-alpha

- Added the foundation for the main menu screen with `Headlines`, `Breaking`, and `Important` sections.
- Limited the app to 2 curated test feeds instead of a broad all-news feed.
- Added BBC World and NPR News as the initial feed sources.
- Removed Yahoo RSS after it returned HTTP 403 in-app.
- Made feed loading resilient so one failed source does not blank the whole app.
- Added section-specific filtering for breaking and important stories.
- Added a feed source note so the current curated source setup is visible during testing.
