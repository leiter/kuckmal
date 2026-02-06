# TODO

## webOS Issues (High Priority)

- [ ] Fix "Abspielen" (Play) button - currently uses `window.open()` which doesn't work on webOS TVs
  - Needs webOS-specific video player integration (webOS Media API or platform detection)
  - Location: `webApp/src/jsMain/kotlin/cut/the/crap/web/Main.kt:989-1014`
- [ ] Fix "Herunterladen" (Download) button - opening URL in `_blank` doesn't initiate download
  - Needs webOS download API or file system access
- [ ] Investigate ORF channel related issue (possibly geo-restricted content handling)

## tvOS (Medium Priority)

- [ ] Connect tvOS app to real API instead of mock data
  - Kotlin interop framework is complete and working
  - `TvosMockMediaRepository` needs to be replaced with API-based implementation
  - Location: `shared-tvos/src/tvosMain/kotlin/cut/the/crap/shared/repository/TvosMockMediaRepository.kt`
- [ ] Fix `loadMediaEntry()` to use Kotlin repository instead of creating fallback entries in Swift
  - Location: `tvosApp/tvosApp/TvOSViewModel.swift:136-159`
- [ ] Implement actual video playback (currently uses placeholder URLs)
- [ ] Add proper error handling for network requests

## Desktop (Completed)

- [x] ~~Time period filter dialog~~ - IMPLEMENTED in `desktopApp/src/main/kotlin/cut/the/crap/desktop/Main.kt:91-293`
- [x] ~~Update checker functionality~~ - IMPLEMENTED in `desktopApp/src/main/kotlin/cut/the/crap/desktop/util/DesktopUpdateChecker.kt`
- [ ] Fix update checker to store/compare actual downloaded file size (currently always shows "update available")
- [ ] Implement diff application for incremental updates
  - Location: `desktopApp/src/main/kotlin/cut/the/crap/desktop/repository/DesktopMediaRepository.kt:185-187`
- [ ] Add user preference for video player selection (currently hardcoded VLC → MPV → Browser)
- [ ] Add video download cancellation support

## Android (Low Priority)

- [ ] Integrate broadcaster logo images in Compose BrowseView (logos exist in `res/drawable/`)
- [ ] Add shared element transitions for detail view (requires Compose 1.7.0+)

## iOS (Low Priority)

- [ ] No major issues identified - app is fully functional

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
