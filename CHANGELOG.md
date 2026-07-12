# Changelog

## 1.0.1 (House Ads Engine Upgrades)

- **GAID Query & Package Registration Correction**: Updated `HouseAdRepository` to query and append the device Google Advertising ID (`GAID` via `AdvertisingIdClient`) when requesting `https://house.monkey-network.xyz/api/public/ads`, strictly respecting `isLimitAdTrackingEnabled` opt-outs. Corrected internal `REGISTERED_PACKAGE_NAME` validation to `com.monkeybytes.budgienews`.
- **Randomized Campaign Rotation (`activeAds.random()`)**: Replaced static ad selection (`ads.first()` and deterministic modulus indexing) across all ad slots (`NewsList`, `StoryDetail`). Each ad banner now picks from active campaigns at random independently and re-rotates across feed refreshes (`refresh()`).
- **Google Native Ad Shape Upgrade (`HouseAdBanner`)**: Upgraded the ad model and UI layout to parse and render the full official native ad hierarchy: `icon_url` (40dp square thumbnail), `headline` (`title` fallback), `advertiser`, `body` secondary text, `star_rating` (`★ 4.5`), `media_url` (`2.2:1` creative banner), and a dedicated `call_to_action` button (`Learn More` fallback).
