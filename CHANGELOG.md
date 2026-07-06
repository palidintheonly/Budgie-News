# Changelog

## 0.0.15-alpha (Pre-Beta Polish Release)

- Achieved pre-beta level UI polish, readability, and responsiveness across the entire application, ensuring seamless performance and legibility across all form factors and Android API levels (Android 8.1.0 through Android 17).
- Fixed text readability and formatting by establishing proportional font size and line height ratios across news cards, lists, detail views, and settings, eliminating line overlap under system font scaling.
- Restored the signature retro typewriter text animation on the main page (news feed cards, lead stories, section headers, and article details), while maintaining instant text rendering exclusively in Settings and Dialogs to prevent lag during preference changes.
- Completely rebuilt the push notification and live alert engine (`BudgieNotifications`). Added robust multi-key payload parsing in Firebase Messaging Service, integrated real-time notification triggers into live Firestore feed syncing and RSS loading, and implemented preference-aware deduplication.
- Enhanced HTML stripping and whitespace normalization in RSS feed descriptions and titles to prevent awkward gaps, line wraps, and uncleaned formatting.

