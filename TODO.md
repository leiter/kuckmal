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

## Download Feature — Fix and Refinement

### Context

`DownloadManager` cannot download HLS: it fetches the `.m3u8` manifest and stops.
Before the fix below, tapping "Herunterladen" on an HLS entry wrote a ~6 KB
playlist file named `.mp4` that looked like a video and would not play.

Measured over a 1,094-entry sample of `Filmliste-diff.xz`:

- **99.4 %** of entries are progressive `.mp4` → download works
- **0.6 %** are HLS → download impossible with `DownloadManager`
- HLS is concentrated per broadcaster: **100 % of ORF** entries in the sample, and
  the SRF entries that exposed the bug. ZDF/ARD/Arte were all progressive.
- For every HLS entry, **all** quality variants (`Url`, `Url_HD`, `Url_Klein`) were
  HLS too — 7 of 7. There is no progressive fallback to switch to.

(The sample is a few hours of updates and is ZDF-heavy, so the percentage is
directional; the per-broadcaster pattern is the reliable part. Re-measure against
the full `Filmliste-akt.xz` before acting on the exact numbers.)

### Done

- [x] **Option 2 — detect HLS and refuse cleanly** (shipped)
  - `MediaUrlUtils.isHlsStream()` and `MediaUrlUtils.downloadFileExtension()`
  - Guard in `di/AppModule.kt` (Compose path, live) and
    `data/MediaViewModel.kt` (legacy XML path)
  - Shows `error_download_not_supported_stream` (EN/DE) instead of writing junk
  - Fixed extension ordering: `.m3u8` is now tested **before** `.mp4`, because
    SRF URLs contain both (`…/name.mp4.csmil/index-f4-v1-a1.m3u8`)
  - Verified on device: HLS entry → toast, no file written; ARTE progressive
    entry → real 355 MB `ftyp mp42` MP4 in `/sdcard/Download/Kuckmal/`

### Options for making HLS actually downloadable

Listed cheapest first. None are required for the Play release — option 2 already
removes the broken-file behaviour.

- [ ] **Option 3 — media3 `HlsDownloader` (recommended next step)**
  - `media3-exoplayer-hls` is **already a dependency**, so the machinery is paid for
  - Downloads all segments into an ExoPlayer `Cache`; gives genuine offline playback
  - Needs a `DownloadService` + notification, and the player must read from the cache
  - Trade-off: content lands in app-private cache, **not** as a file in Downloads —
    so "download" would mean two different things depending on the entry
- [ ] **Option 4 — fetch segments and concatenate to `.ts`**
  - MPEG-TS concatenates cleanly, so no ffmpeg is needed
  - Work: parse media playlist, fetch sequentially, progress/cancel/retry handling
  - Output is `.ts` — plays in VLC, less reliably in stock gallery apps
- [ ] **Option 5 — option 4 plus remux to MP4 via `MediaExtractor` + `MediaMuxer`**
  - Android can remux TS→MP4 without re-encoding
  - Yields a real `.mp4` in Downloads, consistent with every other entry
  - Most work of the three, best end result
- [ ] ~~Option 1 — fall back to a progressive URL for the same entry~~ — **ruled out**,
  all quality variants of an HLS entry are HLS (verified, 7/7)

Before investing in 3–5: bulk-downloading segmented streams is a different
posture toward the broadcasters than linking to their files, and ORF in
particular geo-restricts heavily. Worth a deliberate decision, not just a
technical one.

### Related cleanups

- [ ] **De-duplicate the download logic.** It exists twice — `di/AppModule.kt`
  (Compose, live) and `data/MediaViewModel.kt` (legacy XML via `UIManager`).
  Both had to be patched for this fix. Deleting the legacy path is likely right:
  `MediaActivity` is `exported=false` and `CLAUDE.md` marks the XML code legacy.
- [ ] **`Size` shows an empty "MB"** in the detail view for entries with no size
  field (seen on SRF Tagesschau). Hide the row when the value is missing.

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

- [x] Favorites/Watch Later functionality - IMPLEMENTED
  - Database entities: `FavoriteEntry`, `FavoriteDao` in `shared/src/commonMain/kotlin/cut/the/crap/shared/database/`
  - Repository methods: `addToFavorites()`, `removeFromFavorites()`, `getFavoritesFlow()`, `isFavoriteFlow()`
  - Supports "favorite" and "watchLater" list types
  - Platform implementations: Android (full), iOS (full), Desktop (full)
- [x] Playback history and resume position - IMPLEMENTED
  - Database entities: `HistoryEntry`, `HistoryDao` in `shared/src/commonMain/kotlin/cut/the/crap/shared/database/`
  - Repository methods: `recordPlaybackProgress()`, `getResumePosition()`, `getContinueWatchingFlow()`, `getHistoryFlow()`, `clearHistory()`
  - Auto-marks as completed when >90% watched
  - Platform implementations: Android (full), iOS (full), Desktop (full)
- [x] Deep linking support - IMPLEMENTED (Android, iOS, tvOS)
  - URL scheme: `kuckmal://`
  - Supported URLs:
    - `kuckmal://play?channel=ARD&theme=Tagesschau&title=VideoTitle` - Navigate to video detail
    - `kuckmal://browse?channel=ZDF` - Browse specific channel
    - `kuckmal://search?q=tatort` - Search query
  - Android: Intent filter in `AndroidManifest.xml`, handler in `ComposeActivity.kt`
  - iOS: CFBundleURLTypes in `Info.plist`, handler in `iOSApp.swift` + `KoinHelper.kt`
  - tvOS: Already implemented in `KuckmalTVApp.swift`
- [x] Enhanced offline capabilities (tvOS) - IMPLEMENTED
  - `TvosCache` class: `shared-tvos/src/tvosMain/kotlin/cut/the/crap/shared/cache/TvosCache.kt`
  - TTL-based caching with stale fallback for offline mode
  - Cache TTLs: channels (1hr), themes (15min), entries (5min)
  - Methods: `clearAllCaches()`, `evictExpiredCaches()`, `hasCachedData()`
- [x] Sync status tracking - IMPLEMENTED
  - `SyncStatus` sealed class in `shared/src/commonMain/kotlin/cut/the/crap/shared/sync/SyncStatus.kt`
  - States: Idle, Syncing, Synced(timestamp), Error(message), Offline
  - tvOS implementation tracks sync status via StateFlow
- [ ] User preferences synchronization
- [ ] Subtitle integration in video player
- [ ] Parental controls / content rating
- [ ] UI components for favorites/history (heart icon, continue watching section, etc.)

## Backend API

- [x] Flask API fully implemented with browse, search, and filmlist endpoints
- [ ] Add authentication for write endpoints (currently public)
- [ ] Add rate limiting
- [ ] Add caching layer for frequently accessed data
