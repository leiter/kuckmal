# TODO

## webOS Issues (Complete)

- [x] ~~Fix "Abspielen" (Play) button~~ - FIXED
  - Implemented HTML5 fullscreen video player overlay
  - Works on webOS TVs and standard browsers
  - Supports Escape/Backspace to close player
  - Location: `webApp/src/jsMain/kotlin/cut/the/crap/web/Main.kt`
- [x] ~~Fix "Herunterladen" (Download) button~~ - IMPROVED
  - Now uses programmatic anchor click with download attribute
  - Better cross-platform compatibility
- [x] ~~ORF geo-restriction handling~~ - IMPLEMENTED (all platforms)
  - Added `geo` field to MediaItem models
  - Geo-restriction warning banner in detail views
  - Supports: AT, DE, CH, DE-AT, DE-CH, AT-CH, DE-AT-CH restrictions
  - User geo detection using ip-api.com (free API)
  - Smart warnings: orange for info, red if user is likely blocked

## tvOS (Complete)

- [x] ~~Connect tvOS app to real API instead of mock data~~ - DONE
  - `TvosApiMediaRepository` implemented in `shared-tvos/src/tvosMain/kotlin/cut/the/crap/shared/repository/TvosApiMediaRepository.kt`
  - Koin module updated to inject `TvosApiMediaRepository`
- [x] ~~Fix `loadMediaEntry()` to use Kotlin repository~~ - DONE (uses `searchEntries` with fallback)
- [x] ~~Add proper error handling for network requests~~ - DONE (offline state with retry)
- [x] ~~Configure production API URL~~ - DONE (`https://api.kuckmal.cutthecrap.link`)
- [ ] Verify video playback with real URLs from production API

## Desktop (Complete)

- [x] ~~Time period filter dialog~~ - IMPLEMENTED
- [x] ~~Update checker functionality~~ - IMPLEMENTED
- [x] ~~Fix update checker to store/compare actual downloaded file size~~ - FIXED
  - `DesktopPreferences.kt` added for persistent storage
  - `FilmListDownloader.kt` passes compressed size to callback
  - Size saved after download, compared when checking for updates
- [x] ~~Implement diff application for incremental updates~~ - IMPLEMENTED
  - Location: `desktopApp/src/main/kotlin/cut/the/crap/desktop/repository/DesktopMediaRepository.kt`
- [x] ~~Add user preference for video player selection~~ - IMPLEMENTED
  - Settings dialog accessible via menu → "Einstellungen"
  - Options: Auto, VLC, MPV, Browser (shows only available players)
  - Preference persisted in `preferences.properties`
- [x] ~~Add video download cancellation support~~ - IMPLEMENTED
  - Cancel button in download progress dialog
  - Cleans up partial files on cancellation

## Android (Low Priority)

- [ ] Integrate broadcaster logo images in Compose BrowseView (logos exist in `res/drawable/`)
- [ ] Add shared element transitions for detail view (requires Compose 1.7.0+)

## iOS (App Store Preparation)

- [x] App is fully functional
- [x] App Store submission preparation
  - [x] Privacy policies (EN/DE): `appstore/privacy-policy-*.md`
  - [x] App descriptions (EN/DE): `appstore/ios/*/description.txt`
  - [x] App subtitles (EN/DE): `appstore/ios/*/subtitle.txt`
  - [x] Keywords (EN/DE): `appstore/ios/*/keywords.txt`
  - [x] Release notes (EN/DE): `appstore/ios/*/release-notes.txt`
  - [x] Review notes: `appstore/review-notes.txt`
  - [x] Support page: `appstore/support-page.md`
  - [x] Screenshot guide: `appstore/SCREENSHOT_GUIDE.md`
  - [x] Submission guide: `appstore/APP_STORE_SUBMISSION.md`
  - [x] Maestro automation: `iosApp/.maestro/appstore_screenshots.yaml`
- [ ] **User action required**:
  - [ ] Replace `[YOUR_EMAIL]` placeholders in privacy policies, review notes, support page
  - [ ] Host privacy policy and support page at URLs
  - [ ] Capture screenshots using Maestro flows
  - [ ] Create App Store Connect record and upload

## Future Enhancements (All Platforms)

- [ ] Favorites/Watch Later functionality
- [ ] Playback history and resume position
- [ ] Deep linking support (partially implemented in tvOS)
- [ ] Enhanced offline capabilities
- [ ] User preferences synchronization
- [ ] Subtitle integration in video player
- [ ] Parental controls / content rating

## Backend API

- [x] Flask API fully implemented with browse, search, and filmlist endpoints
- [ ] Add authentication for write endpoints (currently public)
- [ ] Add rate limiting
- [ ] Add caching layer for frequently accessed data
