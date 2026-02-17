# TODO

## webOS Issues (High Priority)

- [ ] Fix "Abspielen" (Play) button - currently uses `window.open()` which doesn't work on webOS TVs
  - Needs webOS-specific video player integration (webOS Media API or platform detection)
  - Location: `webApp/src/jsMain/kotlin/cut/the/crap/web/Main.kt:989-1014`
- [ ] Fix "Herunterladen" (Download) button - opening URL in `_blank` doesn't initiate download
  - Needs webOS download API or file system access
- [ ] Investigate ORF channel related issue (possibly geo-restricted content handling)

## tvOS (Completed - Minor Config Remaining)

- [x] ~~Connect tvOS app to real API instead of mock data~~ - DONE
  - `TvosApiMediaRepository` implemented in `shared-tvos/src/tvosMain/kotlin/cut/the/crap/shared/repository/TvosApiMediaRepository.kt`
  - Koin module updated to inject `TvosApiMediaRepository`
- [x] ~~Fix `loadMediaEntry()` to use Kotlin repository~~ - DONE (uses `searchEntries` with fallback)
- [x] ~~Add proper error handling for network requests~~ - DONE (offline state with retry)
- [ ] Configure production API URL (currently defaults to `localhost:5000`)
  - Location: `shared-tvos/src/tvosMain/kotlin/cut/the/crap/shared/repository/TvosApiMediaRepository.kt:27`
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
- [ ] Add user preference for video player selection (currently hardcoded VLC → MPV → Browser)
- [ ] Add video download cancellation support

## Android (Low Priority)

- [ ] Integrate broadcaster logo images in Compose BrowseView (logos exist in `res/drawable/`)
- [ ] Add shared element transitions for detail view (requires Compose 1.7.0+)

## iOS (App Store Preparation - WIP)

- [x] App is fully functional
- [ ] App Store submission preparation (in progress)
  - **WIP**: `appstore/` directory with privacy policies (EN/DE), review notes, screenshot guide
  - **WIP**: Maestro automation for App Store screenshots (`iosApp/.maestro/appstore_screenshots.yaml`)
  - **WIP**: App preparation flow (`iosApp/.maestro/prepare_app.yaml`)

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
