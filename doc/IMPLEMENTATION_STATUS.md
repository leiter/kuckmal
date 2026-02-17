# Kuckmal Implementation Status

**Last Updated**: February 18, 2026

This document provides a comprehensive overview of the implementation status across all platforms and features.

---

## Platform Implementation Status

### Android (Mobile) ✅ COMPLETE

| Feature | Status | Notes |
|---------|--------|-------|
| Channel browsing | ✅ | 19 broadcasters with brand colors |
| Theme/Title browsing | ✅ | Hierarchical navigation |
| Search | ✅ | Full-text search with debouncing |
| Media details | ✅ | Complete metadata display |
| Video playback | ✅ | ExoPlayer with quality selection |
| Downloads | ✅ | Background download service |
| Compose UI | ✅ | Modern Material Design 3 |
| Database | ✅ | Room KMP with SQLite |
| Koin DI | ✅ | Full dependency injection |
| Deep linking | ✅ | `kuckmal://` scheme for play/browse/search |
| Favorites/Watch Later | ✅ | Database layer ready |
| Playback history | ✅ | Database layer ready |

**Location**: `androidApp/src/main/java/cut/the/crap/android/`

---

### Android TV ✅ COMPLETE

| Feature | Status | Notes |
|---------|--------|-------|
| Leanback launcher | ✅ | TV-optimized home screen |
| D-pad navigation | ✅ | Focus management |
| Video playback | ✅ | Full-screen ExoPlayer |
| All Android features | ✅ | Shared with mobile |

---

### Desktop (JVM) ✅ COMPLETE

| Feature | Status | Notes |
|---------|--------|-------|
| Channel browsing | ✅ | Full browse UI |
| Theme/Title browsing | ✅ | Hierarchical navigation |
| Search | ✅ | Real-time search |
| Time period filter | ✅ | All/Today/3 days/7 days/30 days |
| Film list download | ✅ | XZ decompression, progress tracking |
| Update checker | ✅ | HTTP HEAD size comparison |
| Video playback | ✅ | VLC → MPV → System browser fallback |
| Video downloads | ✅ | Quality selection, progress tracking |
| Database | ✅ | Room KMP with SQLite |
| Favorites/Watch Later | ✅ | Database layer ready |
| Playback history | ✅ | Database layer ready |

**Notes**:
- Video player preference configurable via Settings menu (VLC, MPV, Browser, or Auto)
- Download cancellation supported with partial file cleanup

**Location**: `desktopApp/src/main/kotlin/cut/the/crap/desktop/`

---

### iOS ✅ COMPLETE

| Feature | Status | Notes |
|---------|--------|-------|
| Channel browsing | ✅ | Compose Multiplatform UI |
| Theme/Title browsing | ✅ | Hierarchical navigation |
| Search | ✅ | Full-text search |
| Media details | ✅ | Complete metadata |
| Film list download | ✅ | Background download |
| Database | ✅ | Room KMP |
| Koin DI | ✅ | Full dependency injection |
| Deep linking | ✅ | `kuckmal://` scheme for play/browse/search |
| Favorites/Watch Later | ✅ | Database layer ready |
| Playback history | ✅ | Database layer ready |

**Location**: `iosApp/iosApp/`

---

### Web ✅ COMPLETE (with API backend)

| Feature | Status | Notes |
|---------|--------|-------|
| Channel browsing | ✅ | 18 broadcasters |
| Theme/Title browsing | ✅ | Pagination support |
| Search | ✅ | Full-text search |
| Media details | ✅ | Complete metadata |
| Date/Duration filters | ✅ | Client-side filtering |
| Keyboard navigation | ✅ | ESC for back, Enter to select |
| API integration | ✅ | Flask backend |
| Mock fallback | ✅ | For development/testing |

**Location**: `webApp/src/jsMain/kotlin/cut/the/crap/web/`

---

### webOS TV ✅ COMPLETE

| Feature | Status | Notes |
|---------|--------|-------|
| Channel browsing | ✅ | Working |
| Theme/Title browsing | ✅ | Working |
| Search | ✅ | Working |
| Media details | ✅ | Working |
| Play button | ✅ | HTML5 fullscreen video player |
| Download button | ✅ | Programmatic anchor click |
| webOS manifest | ✅ | appinfo.json ready |
| Icons | ✅ | 80x80 and 130x130 PNG |
| Keyboard navigation | ✅ | Escape/Backspace to close player |

**Notes**:
- Video player uses HTML5 video element with fullscreen overlay
- webOS detection available via `isWebOS()` function
- ORF geo-restriction handling may still need investigation

**Location**: `webApp/src/jsMain/resources/webos/`

---

### tvOS ✅ COMPLETE (API Connected)

| Feature | Status | Notes |
|---------|--------|-------|
| Channel browsing | ✅ | 17+ channels with brand colors |
| Theme/Title browsing | ✅ | Working with real API data |
| Search | ✅ | Working with real API data |
| Media details | ✅ | Complete UI with real data |
| Video player UI | ✅ | AVKit-based, quality selection |
| Deep linking | ✅ | `kuckmal://play` and `kuckmal://browse` |
| Top Shelf | ✅ | Extension scaffolded |
| Kotlin interop | ✅ | TvosApiMediaRepository via Koin |
| Real API connection | ✅ | Connected to Flask backend |
| Offline error handling | ✅ | Shows offline message when no network |
| **API caching** | ✅ | TTL-based with stale fallback |
| **Sync status** | ✅ | Idle/Syncing/Synced/Error/Offline states |
| Real video playback | ⚠️ | Uses real URLs from API (needs server config) |

**Notes**:
- Configured for production API at `https://api.kuckmal.cutthecrap.link`
- `TvosMockMediaRepository` still exists but is no longer used (replaced by `TvosApiMediaRepository`)
- Offline state properly handled with retry functionality
- **Enhanced caching**: `TvosCache` with TTLs (channels: 1hr, themes: 15min, entries: 5min)
- **Stale cache fallback**: Returns cached data when offline, even if expired

**Location**:
- Swift: `tvosApp/tvosApp/`
- Kotlin: `shared-tvos/src/`

---

## Backend API ✅ COMPLETE

| Endpoint | Method | Status |
|----------|--------|--------|
| `/api/channels` | GET | ✅ |
| `/api/themes` | GET | ✅ (with pagination) |
| `/api/titles` | GET | ✅ (with pagination) |
| `/api/entry` | GET | ✅ |
| `/api/search` | GET | ✅ (full-text) |
| `/api/broadcasters` | GET | ✅ (with brand colors) |
| `/api/filmlist/download` | POST | ✅ |
| `/api/filmlist/status` | GET | ✅ |

**Location**: `api/`

---

## Shared Module Structure

```
shared/src/commonMain/kotlin/cut/the/crap/shared/
├── model/
│   ├── Broadcaster.kt          # Broadcaster data class with brand colors
│   └── MediaEntry.kt           # Core media entry model
├── data/
│   ├── FileSystem.kt           # Platform file operations (expect/actual)
│   ├── FilmListDownloader.kt   # XZ download and decompression
│   ├── HttpClientFactory.kt    # Ktor HTTP client (expect/actual)
│   ├── MediaListParser.kt      # JSON/TSV parsing
│   ├── PlatformLogger.kt       # Platform logging (expect/actual)
│   └── XzDecompressor.kt       # XZ decompression (expect/actual)
├── database/
│   ├── AppDatabase.kt          # Room database definition (v2)
│   ├── MediaDao.kt             # Media data access object
│   ├── MediaEntry.kt           # Room entity for media
│   ├── FavoriteEntry.kt        # Room entity for favorites/watch later
│   ├── FavoriteDao.kt          # Favorites data access object
│   ├── HistoryEntry.kt         # Room entity for playback history
│   └── HistoryDao.kt           # History data access object
├── repository/
│   └── MediaRepository.kt      # Repository interface (includes favorites/history)
├── sync/
│   └── SyncStatus.kt           # Sync status sealed class
├── viewmodel/
│   ├── SharedViewModel.kt      # Cross-platform ViewModel
│   └── ViewState.kt            # UI state sealed classes
└── ui/
    ├── components/
    │   └── SearchableTopAppBar.kt
    ├── navigation/
    │   ├── Animations.kt       # Navigation animations
    │   └── Routes.kt           # Route definitions
    ├── screens/
    │   ├── BrowseView.kt       # Main browse screen
    │   └── DetailView.kt       # Media detail screen
    ├── theme/
    │   ├── Color.kt            # Color definitions
    │   ├── Theme.kt            # Material theme
    │   └── Typography.kt       # Typography
    └── util/
        └── OrientationUtil.kt  # Orientation helpers
```

### tvOS Shared Module Structure

```
shared-tvos/src/tvosMain/kotlin/cut/the/crap/shared/
├── cache/
│   └── TvosCache.kt            # TTL-based caching for offline support
├── sync/
│   └── SyncStatus.kt           # Sync status for tvOS
├── repository/
│   ├── TvosApiMediaRepository.kt   # API repository with caching
│   ├── TvosMockMediaRepository.kt  # Mock data (deprecated)
│   └── TvosFallbackMediaRepository.kt
└── ...
```

---

## Feature Matrix

| Feature | Android | Desktop | iOS | Web | webOS | tvOS |
|---------|---------|---------|-----|-----|-------|------|
| Browse channels | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Browse themes | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Browse titles | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Search | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Media details | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Geo detection | ✅ | ✅ | ✅ | ✅ | ✅ | - |
| Geo warnings | ✅ | ✅ | ✅ | ✅ | ✅ | - |
| Video playback | ✅ | ✅ | ✅ | ⚠️ | ❌ | ⚠️ |
| Video download | ✅ | ✅ | - | ⚠️ | ❌ | - |
| Film list download | ✅ | ✅ | ✅ | via API | via API | via API |
| Time period filter | ✅ | ✅ | ✅ | ✅ | ✅ | - |
| Real data | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Offline support | ✅ | ✅ | ✅ | ❌ | ❌ | ✅ |
| **Deep linking** | ✅ | - | ✅ | - | - | ✅ |
| **Favorites DB** | ✅ | ✅ | ✅ | - | - | - |
| **History DB** | ✅ | ✅ | ✅ | - | - | - |
| **API caching** | - | - | - | - | - | ✅ |
| **Sync status** | - | - | - | - | - | ✅ |

Legend: ✅ Complete | ⚠️ Partial | ❌ Not working | - Not applicable

### New Features (February 2026)

- **Favorites/Watch Later**: Database layer for saving content (`FavoriteEntry`, `FavoriteDao`)
- **Playback History**: Database layer for resume position (`HistoryEntry`, `HistoryDao`)
- **Deep Linking**: `kuckmal://play`, `kuckmal://browse`, `kuckmal://search` URLs
- **tvOS Caching**: TTL-based caching with stale fallback for offline mode
- **Sync Status**: Tracking sync state (Idle, Syncing, Synced, Error, Offline)

---

## Priority Fixes

### Medium Priority
1. **UI for Favorites/History**: Add heart icon to detail view, favorites screen, continue watching section
2. **Video player integration**: Hook up `recordPlaybackProgress()` to video players on each platform
3. **tvOS favorites/history**: Extend to API-based caching or sync

### Low Priority
1. Android Compose broadcaster logo images

---

## Technology Stack Summary

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Kotlin | 2.1.0 |
| UI Framework | Compose Multiplatform | 1.7.3 |
| Database | Room KMP | 2.7.1 |
| DI | Koin | 4.0.1 |
| Async | Kotlin Coroutines | 1.9.0 |
| HTTP | Ktor Client | 3.0.3 |
| Serialization | kotlinx.serialization | 1.7.3 |
| Backend | Flask (Python) | 3.0.0+ |
| Video (Android) | ExoPlayer/Media3 | 1.5.0 |
| Build | Gradle | 8.12+ |

---

## Documentation Index

| Document | Description |
|----------|-------------|
| [PROJECT_DESCRIPTION.md](PROJECT_DESCRIPTION.md) | Project overview and architecture |
| [KMP_MIGRATION_ANALYSIS.md](KMP_MIGRATION_ANALYSIS.md) | KMP migration plan (completed) |
| [COMPOSE_NAVIGATION_IMPLEMENTATION.md](COMPOSE_NAVIGATION_IMPLEMENTATION.md) | Navigation system details |
| [KOIN_INJECTION_SETUP.md](KOIN_INJECTION_SETUP.md) | Dependency injection setup |
| [VIDEO_PLAYER_EXPECT_ACTUAL.md](VIDEO_PLAYER_EXPECT_ACTUAL.md) | Video player abstraction |
| [WEBOS_SETUP.md](WEBOS_SETUP.md) | webOS deployment guide |
| [flask-api-plan.md](flask-api-plan.md) | Backend API design |
| [TODO.md](../TODO.md) | Current task list |

---

## Database Schema (v2)

### Tables

| Table | Description |
|-------|-------------|
| `media_entries` | Main media content (channels, themes, titles) |
| `favorite_entries` | User favorites and watch later items |
| `history_entries` | Playback history with resume position |

### New in v2

**favorite_entries**:
- `id` (PK), `channel`, `theme`, `title`, `addedAt`, `listType`
- Unique constraint on (channel, theme, title)

**history_entries**:
- `id` (PK), `channel`, `theme`, `title`, `resumePositionSeconds`, `durationSeconds`, `watchedAt`, `isCompleted`
- Unique constraint on (channel, theme, title)

---

## Deep Link URLs

| URL | Description | Platforms |
|-----|-------------|-----------|
| `kuckmal://play?channel=ARD&theme=Tagesschau&title=Video` | Open specific video | Android, iOS, tvOS |
| `kuckmal://browse?channel=ZDF` | Browse channel | Android, iOS, tvOS |
| `kuckmal://search?q=tatort` | Search query | Android, iOS |
