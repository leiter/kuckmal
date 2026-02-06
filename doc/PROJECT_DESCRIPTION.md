# Kuckmal - Project Description

## Overview

**Kuckmal** is a Kotlin Multiplatform (KMP) media browsing application that provides access to German, Austrian, and Swiss public broadcasting content. The app allows users to browse, search, and stream video content from 19 TV channels including ARD, ZDF, 3Sat, ARTE, ORF, SRF, and more.

The name "Kuckmal" is German for "look/check this out" - fitting for a media discovery app.

## Target Platforms

The application runs on 7 platforms from a shared codebase:

| Platform | Technology | Status |
|----------|------------|--------|
| Android | Native Android + Compose | ✅ Full implementation |
| Android TV | Leanback launcher | ✅ Full implementation |
| Desktop | JVM (Windows, macOS, Linux) | ✅ Full implementation (all features complete) |
| iOS | Swift + Compose Multiplatform | ✅ Full implementation |
| Web | Kotlin/JS + Compose HTML | ✅ Full implementation with API backend |
| webOS TV | Kotlin/JS + Compose HTML | ⚠️ Play/Download buttons not working (needs platform API) |
| tvOS | SwiftUI + Kotlin KMP | ⚠️ UI complete, uses mock data (API integration pending) |

## Core Features

### 1. Media Browsing
- **Channel Navigation**: Browse 19 German-speaking TV channels with branded colors
- **Theme/Category Filtering**: Explore content organized by themes (documentaries, news, entertainment, etc.)
- **Title Browse**: View individual programs and episodes
- **Hierarchical Navigation**: Channel → Theme → Title → Details

### 2. Search
- Full-text search across titles, descriptions, and themes
- Filter search results by channel or theme
- Pagination support for large result sets
- Real-time search results with Flow-based reactivity

### 3. Media Details
- Complete metadata display (date, time, duration, file size)
- Program descriptions
- Multiple video quality options:
  - HD (high quality)
  - Standard quality
  - Low quality (for bandwidth-limited scenarios)
- Subtitle support

### 4. Video Playback
- Integrated video player (ExoPlayer on Android)
- Native player integration on desktop
- Quality selection before playback
- Subtitle loading

### 5. Download Capability
- Download videos for offline viewing
- Quality selection for downloads
- Background download service (Android)
- Download progress tracking

### 6. Content Filtering
- **Date Range**: Today, Last Week, Last Month, All Time
- **Duration**: Short, Medium, Long programs
- **Channel**: Single or multiple channel selection
- Combinable filters

## Supported Broadcasters

| Broadcaster | Country | Abbreviation |
|-------------|---------|--------------|
| ARD | Germany | ARD |
| ZDF | Germany | ZDF |
| 3Sat | Germany/Austria/Switzerland | 3Sat |
| ARTE | Germany/France | ARTE |
| BR | Germany (Bavaria) | BR |
| HR | Germany (Hesse) | HR |
| KiKA | Germany (Children) | KiKA |
| MDR | Germany (Central) | MDR |
| NDR | Germany (North) | NDR |
| ORF | Austria | ORF |
| Phoenix | Germany | Phoenix |
| RBB | Germany (Berlin-Brandenburg) | RBB |
| SR | Germany (Saarland) | SR |
| SRF | Switzerland | SRF |
| SWR | Germany (Southwest) | SWR |
| WDR | Germany (West) | WDR |
| DW | Germany (International) | DW |
| Funk | Germany (Youth) | Funk |
| Radio Bremen | Germany (Bremen) | RB |

## Technical Architecture

### Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Kotlin | 2.1.0 |
| UI Framework | Compose Multiplatform | 1.7.3 |
| Database | Room KMP | 2.7.1 |
| DI Framework | Koin | 4.0.1 |
| Async | Kotlin Coroutines | 1.9.0 |
| Navigation | Compose Navigation KMP | 2.8.0-alpha10 |
| Build System | Gradle | Kotlin DSL |
| Min Android SDK | 21 (Android 5.0) | - |
| Target Android SDK | 36 (Android 15) | - |
| JVM Target | Java 17 | - |

### Module Structure

```
kuckmal/
├── shared/                    # KMP shared code (25 Kotlin files)
│   ├── model/                 # Data models (MediaEntry, Broadcaster)
│   ├── data/                  # FilmListDownloader, MediaListParser, HttpClientFactory
│   ├── database/              # Room database (AppDatabase, MediaDao, MediaEntry)
│   ├── repository/            # MediaRepository interface
│   ├── viewmodel/             # SharedViewModel, ViewState
│   └── ui/                    # Shared Compose UI (BrowseView, DetailView, theme)
│
├── shared-tvos/               # tvOS-specific KMP module (14 Kotlin files)
│   ├── di/                    # Koin setup (TvosModule, KoinHelper)
│   ├── data/                  # tvOS-specific implementations
│   └── repository/            # TvosMockMediaRepository
│
├── androidApp/                # Android implementation (40 Kotlin files)
│   ├── ui/                    # Android-specific UI (Activities, Compose screens)
│   ├── compose/               # Compose UI and navigation
│   ├── service/               # Download service
│   ├── video/                 # VideoPlayer abstraction, ExoPlayer integration
│   └── di/                    # Koin dependency injection
│
├── desktopApp/                # Desktop JVM application (9 Kotlin files)
│   ├── data/                  # FilmListDownloader, MediaListParser
│   ├── repository/            # DesktopMediaRepository
│   ├── download/              # DesktopDownloadManager
│   ├── player/                # DesktopVideoPlayer (VLC, MPV, system)
│   └── util/                  # DesktopUpdateChecker, MediaUrlUtils
│
├── webApp/                    # Web/webOS application (5 Kotlin files)
│   └── repository/            # ApiMediaRepository, MockMediaRepository
│
├── iosApp/                    # iOS Swift wrapper
│   └── iosApp/                # Swift source with Compose Multiplatform
│
├── tvosApp/                   # tvOS SwiftUI application (13 Swift files)
│   ├── Views/                 # ContentView, DetailView, VideoPlayerView
│   ├── ViewModels/            # TvOSViewModel
│   └── KotlinInterop/         # Swift-Kotlin bridge
│
└── api/                       # Python Flask backend (9 Python files)
    ├── routes/                # browse, search, detail, filmlist endpoints
    ├── services/              # media_service, search_service, parser_service
    └── models/                # SQLAlchemy ORM models
```

### Data Flow Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      UI Layer                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │ BrowseView  │  │ DetailView  │  │  SearchUI   │          │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘          │
│         │                │                │                  │
│         └────────────────┼────────────────┘                  │
│                          │                                   │
│                  ┌───────▼───────┐                          │
│                  │SharedViewModel │ (StateFlow/Callbacks)    │
│                  └───────┬───────┘                          │
└──────────────────────────┼───────────────────────────────────┘
                           │
┌──────────────────────────┼───────────────────────────────────┐
│                          │  Domain Layer                     │
│                  ┌───────▼───────┐                          │
│                  │MediaRepository│ (Interface)               │
│                  └───────┬───────┘                          │
└──────────────────────────┼───────────────────────────────────┘
                           │
┌──────────────────────────┼───────────────────────────────────┐
│                          │  Data Layer                       │
│        ┌─────────────────┼─────────────────┐                │
│        │                 │                 │                 │
│  ┌─────▼─────┐    ┌──────▼──────┐   ┌─────▼─────┐          │
│  │Room/SQLite│    │File Parser  │   │  Network  │          │
│  │ Database  │    │(XZ/TSV/CSV) │   │ Download  │          │
│  └───────────┘    └─────────────┘   └───────────┘          │
└─────────────────────────────────────────────────────────────┘
```

### Key Design Patterns

1. **Repository Pattern**: Abstracts data sources from ViewModels
2. **MVVM/MVI**: StateFlow-based state management
3. **Dependency Injection**: Koin for cross-platform DI
4. **Callback Injection**: Platform-specific operations via lambda callbacks
5. **Sealed Classes**: Type-safe state representation (LoadingResult, DialogState)

## Data Model

### MediaEntry (Core Entity)

```kotlin
data class MediaEntry(
    val channel: String,      // Broadcasting channel
    val theme: String,        // Program category
    val title: String,        // Episode/program title
    val date: String,         // Broadcast date
    val time: String,         // Broadcast time
    val duration: String,     // Program duration
    val sizeMB: String,       // File size in MB
    val description: String,  // Program description
    val url: String,          // Standard quality video URL
    val website: String,      // Associated website
    val subtitleUrl: String,  // Subtitle file URL
    val urlSmall: String,     // Low-quality video URL
    val urlHd: String,        // High-quality video URL
    val dateL: Long,          // Unix timestamp for filtering
    val geo: String,          // Geographic restrictions
    val isNew: Boolean        // Recently added flag
)
```

## Data Source

The app uses publicly available media metadata from German public broadcasters:

- **Format**: XZ-compressed TSV/CSV files
- **Size**: ~150MB compressed, several GB uncompressed
- **Update Method**: Full downloads + incremental diff files
- **Content**: Metadata only (titles, descriptions, URLs) - no actual video hosting

## Build & Distribution

### Android
- Package ID: `cut.the.crap.kuckmal`
- APK and AAB builds
- ProGuard/R8 minification in release builds
- Android TV category (Leanback launcher)

### Desktop
- Application Name: "Kuckmal"
- Distributions:
  - **Windows**: MSI installer
  - **macOS**: DMG package
  - **Linux**: Deb and RPM packages

### Web
- Single JavaScript bundle
- Static assets deployment
- webOS app manifest support

## Project Status

- **Active Development**: Yes (ongoing refinements)
- **Architecture**: Modern KMP with Compose Multiplatform
- **Code Quality**: 100% Kotlin, no Java
- **Documentation**: Extensive internal docs in `/doc/`

## Future Development Areas

Based on codebase analysis (January 2026):

### Completed Features ✅
- ~~Search functionality~~ ✅ Implemented across all platforms
- ~~Broadcaster logos~~ ✅ All 20+ logos available in drawable resources
- ~~webOS TV icons~~ ✅ Icons ready (icon.png, largeIcon.png)
- ~~iOS: Film list download feature~~ ✅ Fully implemented
- ~~Web app: Database integration~~ ✅ API backend implemented (ApiMediaRepository)
- ~~Desktop: Time period filter dialog~~ ✅ Fully implemented with 5 options
- ~~Desktop: Update checker functionality~~ ✅ Implemented with HTTP HEAD checks

### In Progress / Known Issues
- webOS: Fix Play/Download button functionality (needs platform-specific video API)
- tvOS: Connect to real API (Kotlin interop framework complete, mock data in use)
- Desktop: Update checker size comparison fix (always shows "update available")
- Desktop: Diff application for incremental updates

### Planned Features
- Favorites/Watch Later functionality
- Playback history and resume position
- Deep linking support (partially in tvOS)
- Enhanced offline capabilities
- User preferences synchronization
- Subtitle integration in video player
- Parental controls / content rating

## Related Documentation

- [KMP Migration Analysis](KMP_MIGRATION_ANALYSIS.md)
- [Compose Navigation Implementation](COMPOSE_NAVIGATION_IMPLEMENTATION.md)
- [Koin Injection Setup](KOIN_INJECTION_SETUP.md)
- [Video Player Integration](VIDEO_PLAYER_EXPECT_ACTUAL.md)
- [WebOS Setup Guide](WEBOS_SETUP.md)
