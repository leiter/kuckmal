# Kotlin Multiplatform (KMP) Migration Analysis

**Project**: Kuckmal Android
**Date**: November 2025
**Current Status**: Pure Android (100% Kotlin)
**Target**: Kotlin Multiplatform (KMP)

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current Project Analysis](#current-project-analysis)
3. [Dependency Analysis](#dependency-analysis)
4. [Migration Strategy](#migration-strategy)
5. [Challenges & Solutions](#challenges--solutions)
6. [Migration Roadmap](#migration-roadmap)
7. [Recommendations](#recommendations)

---

## Executive Summary

### Current State
- **Language**: 100% Kotlin (no Java source files)
- **Total Kotlin Files**: 44 files (~7,824 lines)
- **Kotlin Version**: 2.1.0 ✅ (excellent for KMP)
- **Android Gradle Plugin**: 8.10.1
- **Min SDK**: 21, Target SDK: 36

### Migration Feasibility: **HIGH** ✅

**Strengths for KMP Migration**:
- ✅ Pure Kotlin codebase (zero Java dependencies)
- ✅ Modern Kotlin 2.1.0 with full KMP support
- ✅ Clean architecture with separable concerns
- ✅ Dependency injection via Koin (has KMP support)
- ✅ Coroutines-based async operations

**Challenges**:
- ❌ Heavy Android-specific UI (XML layouts, AndroidX)
- ❌ Room database (needs migration to SQLDelight or Room KMP)
- ❌ Android-specific media playback (ExoPlayer)
- ❌ JVM-only libraries (Gson, XZ compression)

---

## Current Project Analysis

### Source Code Structure

```
androidApp/src/main/java/cut/the/crap/android/
├── database/          # Room database (3 files)
├── data/              # ViewModels and parsers (2 files)
├── di/                # Dependency injection - Koin (1 file)
├── model/             # Data models (2 files)
├── repository/        # Data repositories (4 files)
├── service/           # Download service (1 file)
├── ui/                # UI components and activities (6 files)
└── util/              # Utilities (6 files)
```

### Java Reliance: **ZERO**
- **Java source files**: 0
- **Kotlin files**: 44
- Java is only used as JVM bytecode target (standard for Kotlin/Android)

---

## Dependency Analysis

### Build Configuration

**Root-level** (`build.gradle`):
```gradle
kotlin_version = '2.1.0'
gradle = '8.10.1'
```

**App-level** (`app/build.gradle`):
```gradle
compileSdk 36
minSdk 21
targetSdk 36
```

---

### Dependency Breakdown by KMP Compatibility

## ✅ **KMP-Compatible Dependencies**

### 1. Kotlin & Coroutines
```gradle
org.jetbrains.kotlin:kotlin-stdlib:2.1.0                    ✅ Full KMP
org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0        ✅ Full KMP
org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0     ⚠️ Android-only
```

**KMP Strategy**:
- Use `kotlinx-coroutines-core` in `commonMain`
- Keep `kotlinx-coroutines-android` in `androidMain`

---

### 2. Dependency Injection (Koin)
```gradle
io.insert-koin:koin-core:4.0.1                  ✅ Full KMP
io.insert-koin:koin-android:4.0.1               ⚠️ Android-only
io.insert-koin:koin-androidx-compose:4.0.1      ⚠️ Android-only
```

**KMP Strategy**:
- Use `koin-core` in `commonMain`
- Keep Android-specific Koin modules in `androidMain`

---

### 3. Parsing & Compression
```gradle
com.google.code.gson:gson:2.11.0        ❌ JVM-only
org.tukaani:xz:1.10                     ❌ JVM-only
```

**KMP Replacement**:
- **Gson** → `kotlinx.serialization` (official KMP serialization)
- **XZ** → Platform-specific expect/actual or KMP compression library

---

## ⚠️ **Android-Specific Dependencies**

### 4. AndroidX Core
```gradle
androidx.appcompat:appcompat:1.7.1                          ❌ Android-only
androidx.activity:activity-ktx:1.9.3                        ❌ Android-only
androidx.core:core-ktx:1.15.0                               ❌ Android-only
androidx.constraintlayout:constraintlayout:2.2.1            ❌ Android-only
```

**KMP Strategy**: Keep in `androidMain` source set

---

### 5. Lifecycle Components
```gradle
androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4            ⚠️ Has KMP support (2.8.0+)
androidx.lifecycle:lifecycle-livedata-ktx:2.9.4             ❌ Android-only
androidx.lifecycle:lifecycle-runtime-ktx:2.9.4              ❌ Android-only
androidx.lifecycle:lifecycle-process:2.9.4                  ❌ Android-only
```

**KMP Alternative**:
- Use `androidx.lifecycle:lifecycle-viewmodel` (KMP support since 2.8.0)
- Replace LiveData with Kotlin Flow in `commonMain`

---

### 6. Room Database
```gradle
androidx.room:room-runtime:2.6.1                ⚠️ Experimental KMP (2.7.0+)
androidx.room:room-ktx:2.6.1                    ⚠️ Experimental KMP
ksp 'androidx.room:room-compiler:2.6.1'         ⚠️ Experimental KMP
```

**KMP Alternatives**:
1. **SQLDelight** (recommended - mature KMP solution)
2. **Room 2.7+** (experimental KMP support with SQLite driver)

---

### 7. Media Playback (ExoPlayer)
```gradle
androidx.media3:media3-exoplayer:1.5.0          ❌ Android-only
androidx.media3:media3-ui:1.5.0                 ❌ Android-only
androidx.media3:media3-exoplayer-hls:1.5.0      ❌ Android-only
```

**KMP Strategy**:
- Keep in `androidMain`
- Use expect/actual pattern for media playback on other platforms

---

### 8. TV Support
```gradle
androidx.leanback:leanback:1.2.0-alpha04        ❌ Android-only
```

**KMP Strategy**: Keep in `androidMain`

---

### 9. Testing Dependencies
```gradle
# Unit Tests
junit:junit:4.13.2                                          ⚠️ JVM-only
kotlinx-coroutines-test:1.9.0                               ✅ KMP support
koin-test:4.0.1                                             ✅ KMP support

# Android Tests
androidx.test.ext:junit:1.2.1                               ❌ Android-only
androidx.test:runner:1.6.2                                  ❌ Android-only
androidx.test.espresso:espresso-core:3.6.1                  ❌ Android-only
androidx.test.uiautomator:uiautomator:2.3.0                 ❌ Android-only
```

**KMP Alternative**: Use `kotlin.test` for common tests

---

## Migration Strategy

### Architecture Overview

```
Project Structure (After KMP Migration):

shared/
├── commonMain/
│   ├── kotlin/
│   │   ├── model/              # Data models (MediaEntry, Broadcaster)
│   │   ├── repository/         # Repository interfaces
│   │   ├── data/               # Business logic, parsers
│   │   ├── di/                 # Koin DI (core modules)
│   │   └── util/               # Shared utilities
│   └── resources/
│
├── androidMain/
│   ├── kotlin/
│   │   ├── ui/                 # Activities, Adapters, Fragments
│   │   ├── database/           # Room implementation (or SQLDelight driver)
│   │   ├── service/            # Download service
│   │   └── di/                 # Android-specific Koin modules
│   └── res/                    # Android resources (layouts, drawables)
│
├── iosMain/                    # Future iOS support
└── desktopMain/                # Future Desktop support

androidApp/                     # Android application module
```

---

### What Can Be Shared (commonMain)

✅ **Can move to commonMain**:
- Data models (`MediaEntry`, `Broadcaster`)
- Repository interfaces (`MediaRepository`)
- Business logic (parsing, data transformations)
- Network utilities (if using Ktor)
- ViewModels (with androidx.lifecycle KMP)
- Serialization logic (with kotlinx.serialization)
- Koin core DI modules

---

### What Stays Android-Specific (androidMain)

❌ **Must stay in androidMain**:
- UI components (Activities, Fragments, Adapters, XML layouts)
- Room database implementation (or SQLDelight Android driver)
- ExoPlayer media integration
- Download service
- Android-specific utilities
- TV/Leanback UI components
- Android permissions & background tasks

---

## Challenges & Solutions

### Challenge 1: UI Layer (100% Android-specific)

**Current**: XML layouts + AndroidX components

**Solutions**:
1. **Option A**: Keep UI in `androidMain` (easiest)
   - Android UI stays as-is
   - Other platforms get their own UI layer

2. **Option B**: Migrate to Compose Multiplatform
   - Share UI across Android/iOS/Desktop
   - Significant effort but maximum code reuse

**Recommendation**: Start with Option A, migrate to Compose later

---

### Challenge 2: Database (Room is Android-centric)

**Current**: Room 2.6.1

**Solutions**:
1. **Option A**: Migrate to SQLDelight (recommended)
   - Mature KMP support
   - Type-safe SQL
   - Native performance

2. **Option B**: Use Room 2.7+ experimental KMP support
   - Familiar API
   - Experimental/unstable
   - Limited platform support

**Recommendation**: SQLDelight for production-ready KMP

---

### Challenge 3: Serialization (Gson is JVM-only)

**Current**: Gson 2.11.0

**Solution**: Migrate to `kotlinx.serialization`
```kotlin
// Before (Gson)
val json = Gson().toJson(mediaEntry)

// After (kotlinx.serialization)
val json = Json.encodeToString(mediaEntry)
```

**Migration Steps**:
1. Add `@Serializable` annotation to data classes
2. Replace Gson calls with kotlinx.serialization
3. Update custom type adapters if needed

---

### Challenge 4: File Compression (XZ is JVM-only)

**Current**: org.tukaani:xz:1.10

**Solutions**:
1. Use platform-specific implementations (expect/actual)
2. Find KMP compression library (if available)
3. Keep compression in Android-specific code

**Recommendation**: Use expect/actual pattern

```kotlin
// commonMain
expect suspend fun decompressXZ(input: ByteArray): ByteArray

// androidMain
actual suspend fun decompressXZ(input: ByteArray): ByteArray {
    // XZ decompression using tukaani library
}
```

---

### Challenge 5: Video Playback (ExoPlayer is Android-only)

**Current**: Media3 ExoPlayer

**Solution**: Use expect/actual pattern per platform

```kotlin
// commonMain
expect class VideoPlayer {
    fun play(url: String)
    fun pause()
    fun stop()
}

// androidMain
actual class VideoPlayer {
    private val exoPlayer = ExoPlayer.Builder(context).build()

    actual fun play(url: String) {
        // ExoPlayer implementation
    }
}
```

---

## Migration Roadmap

### Phase 1: Preparation (2-3 weeks)

**Goal**: Update dependencies and restructure for KMP

1. **Update Serialization**
   - [ ] Add `kotlinx-serialization` plugin
   - [ ] Add `@Serializable` to data models
   - [ ] Replace Gson with kotlinx.serialization
   - [ ] Test serialization/deserialization

2. **Database Planning**
   - [ ] Decide: SQLDelight vs Room KMP
   - [ ] Design database migration strategy
   - [ ] Create database schema documentation

3. **Code Organization**
   - [ ] Identify shareable code
   - [ ] Separate UI from business logic
   - [ ] Extract repository interfaces
   - [ ] Document platform-specific requirements

---

### Phase 2: KMP Structure Setup (1-2 weeks)

**Goal**: Convert project to KMP structure

1. **Create Shared Module**
   - [ ] Add KMP Gradle plugin
   - [ ] Create `shared` module with source sets:
     - `commonMain`
     - `androidMain`
     - `commonTest`
   - [ ] Configure build scripts

2. **Configure Targets**
   - [ ] Set up Android target
   - [ ] Configure JVM toolchain
   - [ ] Set up test infrastructure

3. **Update Dependencies**
   - [ ] Move common dependencies to `commonMain`
   - [ ] Keep Android-specific in `androidMain`
   - [ ] Update Koin configuration

---

### Phase 3: Extract Common Code (3-4 weeks)

**Goal**: Move business logic to commonMain

1. **Move Data Models**
   - [ ] Move `MediaEntry` to `commonMain`
   - [ ] Move `Broadcaster` to `commonMain`
   - [ ] Update serialization annotations
   - [ ] Test model classes

2. **Move Repository Layer**
   - [ ] Move repository interfaces to `commonMain`
   - [ ] Keep Android implementation in `androidMain`
   - [ ] Create expect/actual for platform-specific repos

3. **Move Business Logic**
   - [ ] Move parsing logic to `commonMain`
   - [ ] Move data transformation logic
   - [ ] Extract network utilities (if any)
   - [ ] Move validation logic

4. **Move ViewModels**
   - [ ] Migrate to androidx.lifecycle KMP ViewModel
   - [ ] Move ViewModels to `commonMain`
   - [ ] Replace LiveData with StateFlow/Flow
   - [ ] Test ViewModel logic

5. **Update Dependency Injection**
   - [ ] Move core Koin modules to `commonMain`
   - [ ] Keep Android-specific modules in `androidMain`
   - [ ] Test DI configuration

---

### Phase 4: Database Migration (2-3 weeks)

**Goal**: Migrate to KMP-compatible database

**Option A: SQLDelight Migration**

1. **Setup SQLDelight**
   - [ ] Add SQLDelight Gradle plugin
   - [ ] Create SQL schema files
   - [ ] Configure SQLDelight for Android

2. **Migrate Room to SQLDelight**
   - [ ] Convert Room entities to SQL tables
   - [ ] Convert Room DAOs to SQLDelight queries
   - [ ] Update repository implementations
   - [ ] Test database operations

3. **Data Migration**
   - [ ] Create migration script from Room to SQLDelight
   - [ ] Test data migration with sample data
   - [ ] Add migration tests

**Option B: Room KMP (Experimental)**

1. **Update Room**
   - [ ] Update to Room 2.7+
   - [ ] Configure Room KMP support
   - [ ] Add SQLite driver for KMP

2. **Test Room KMP**
   - [ ] Test on Android
   - [ ] Verify database operations
   - [ ] Monitor for stability issues

---

### Phase 5: Platform-Specific Implementations (2-3 weeks)

**Goal**: Implement expect/actual for platform-specific features

1. **File Compression**
   - [ ] Define expect interface for compression
   - [ ] Implement actual for Android (XZ library)
   - [ ] Test compression/decompression

2. **Media Playback**
   - [ ] Define expect interface for video player
   - [ ] Implement actual for Android (ExoPlayer)
   - [ ] Add playback controls interface

3. **File I/O**
   - [ ] Define expect interface for file operations
   - [ ] Implement actual for Android
   - [ ] Test file read/write operations

4. **Download Service**
   - [ ] Define expect interface for downloads
   - [ ] Implement actual for Android (WorkManager/Service)
   - [ ] Test download functionality

---

### Phase 6: Testing & Validation (2-3 weeks)

**Goal**: Ensure KMP migration is stable

1. **Unit Tests**
   - [ ] Migrate tests to `commonTest`
   - [ ] Add tests for shared code
   - [ ] Test serialization
   - [ ] Test business logic

2. **Android Tests**
   - [ ] Update Android instrumentation tests
   - [ ] Test UI layer
   - [ ] Test database migrations
   - [ ] Test media playback

3. **Integration Tests**
   - [ ] Test end-to-end workflows
   - [ ] Test repository layer
   - [ ] Test DI configuration
   - [ ] Performance testing

4. **Regression Testing**
   - [ ] Test all existing features
   - [ ] Compare with pre-migration build
   - [ ] Fix any bugs

---

### Phase 7: Optimization & Documentation (1-2 weeks)

**Goal**: Optimize and document the KMP architecture

1. **Code Optimization**
   - [ ] Remove dead code
   - [ ] Optimize build configuration
   - [ ] Reduce APK size
   - [ ] Profile performance

2. **Documentation**
   - [ ] Document KMP architecture
   - [ ] Update README with KMP setup
   - [ ] Document expect/actual patterns
   - [ ] Create contribution guide

3. **CI/CD Updates**
   - [ ] Update build scripts
   - [ ] Configure multi-platform builds
   - [ ] Add KMP-specific checks

---

## Recommendations

### Immediate Actions

1. **Start with Gson → kotlinx.serialization migration**
   - Low risk
   - Necessary for KMP
   - Can be done incrementally

2. **Extract repository interfaces**
   - Separate interface from implementation
   - Makes KMP migration easier

3. **Replace LiveData with StateFlow**
   - Prepare for ViewModel migration to KMP
   - Better performance

---

### Database Decision

**Recommended**: **SQLDelight**

**Reasons**:
- ✅ Mature KMP support
- ✅ Type-safe SQL queries
- ✅ Native performance
- ✅ Active community
- ✅ Good documentation

**Alternative**: Room 2.7+ KMP (if you prefer familiar API and accept experimental status)

---

### UI Strategy

**Phase 1**: Keep Android UI as-is in `androidMain`

**Phase 2 (Future)**: Consider Compose Multiplatform
- Share UI logic across platforms
- Modern declarative UI
- Growing ecosystem

---

### Timeline Estimate

**Total Duration**: 12-18 weeks (3-4.5 months)

**Breakdown**:
- Phase 1 (Prep): 2-3 weeks
- Phase 2 (Setup): 1-2 weeks
- Phase 3 (Common Code): 3-4 weeks
- Phase 4 (Database): 2-3 weeks
- Phase 5 (Platform-specific): 2-3 weeks
- Phase 6 (Testing): 2-3 weeks
- Phase 7 (Optimization): 1-2 weeks

**Risk Buffer**: +20-30% for unforeseen issues

---

### Success Metrics

✅ **Migration Complete When**:
- [ ] All business logic in `commonMain`
- [ ] Database fully migrated (SQLDelight or Room KMP)
- [ ] All tests passing (commonTest + androidTest)
- [ ] No performance regression
- [ ] Android APK builds and runs correctly
- [ ] Code coverage maintained or improved
- [ ] Documentation complete

---

## Dependency Replacement Summary

| Current Dependency | KMP Alternative | Migration Effort |
|-------------------|-----------------|------------------|
| `gson:2.11.0` | `kotlinx-serialization` | Low (1-2 weeks) |
| `room:2.6.1` | `SQLDelight` or `Room 2.7+` | High (2-3 weeks) |
| `tukaani:xz:1.10` | expect/actual pattern | Medium (1 week) |
| `media3-exoplayer` | expect/actual pattern | Low (keep in androidMain) |
| `lifecycle-livedata` | `StateFlow`/`Flow` | Medium (1-2 weeks) |
| `koin-android` | `koin-core` + androidMain | Low (1 week) |
| AndroidX UI | Keep in androidMain or Compose MP | Low/High |

---

## Conclusion

The **Kuckmal Android** project is an **excellent candidate for KMP migration** due to its pure Kotlin codebase and clean architecture. The main effort will be:

1. **Serialization migration** (Gson → kotlinx.serialization)
2. **Database migration** (Room → SQLDelight or Room KMP)
3. **Code organization** (common vs platform-specific)

**Expected Benefits**:
- ✅ Code reuse across platforms (30-50% shared code initially)
- ✅ Type-safe, modern architecture
- ✅ Better testability
- ✅ Future-proof for iOS/Desktop expansion

**Estimated Effort**: 3-4.5 months for full migration

**Recommended Approach**: Incremental migration starting with serialization and data layer, then moving up to business logic and ViewModels.

---

**Document Version**: 1.0
**Last Updated**: November 14, 2025
