# Compose UI Code Shareability Analysis

Analysis of which Compose UI code can be moved to the shared module for Kotlin Multiplatform.

## Current State

### Shared Module (Already Platform-Agnostic)

The shared module at `shared/src/commonMain/kotlin/com/mediathekview/shared/` already contains:

| File | Description |
|------|-------------|
| `model/Broadcaster.kt` | Channel/station data with brand colors and abbreviations |
| `model/MediaEntry.kt` | Media data model with factory method for parsing |
| `util/FormatUtils.kt` | Duration, size, date formatting utilities |

### Compose UI Files in App Module

Located at `app/src/main/java/com/mediathekview/android/compose/`:

```
compose/
├── ComposeActivity.kt                 # Activity entry point
├── models/
│   ├── ComposeViewModel.kt            # 1438 lines - Android ViewModel
│   └── ComposeModels.kt               # Data classes
├── screens/
│   ├── BrowseView.kt                  # Browse/list screen
│   └── DetailView.kt                  # Detail screen
├── navigation/
│   └── Navigation.kt                  # Navigation logic
├── data/
│   └── ComposeDataMapper.kt           # Data transformation
└── ui/
    ├── theme/
    │   ├── Theme.kt                   # Theme with dynamic colors
    │   └── Type.kt                    # Typography
    └── dialogs/
        └── AppDialogs.kt              # Dialog composables
```

## Shareability Assessment

### Fully Shareable (Can Move Now)

| File | Notes |
|------|-------|
| **ComposeModels.kt** | Pure Kotlin data classes (`MediaItem`, `Channel`, `SampleData`), no Android dependencies |
| **Type.kt** | Typography definitions, likely platform-independent |

### Partially Shareable (Requires Refactoring)

| File | Shareable Parts | Android Dependencies |
|------|-----------------|---------------------|
| **BrowseView.kt** | UI composables, layout logic | Callbacks tied to ViewModel |
| **DetailView.kt** | UI composables, layout logic | Callbacks tied to ViewModel |
| **AppDialogs.kt** | Dialog composables | `R.string.*` resource references |
| **ComposeDataMapper.kt** | Transformation logic | Depends on Android database entities |
| **Navigation.kt** | State management | `NavHost`, `NavController`, `BackHandler` |

### Not Shareable (Must Remain Android-Only)

| File | Reason |
|------|--------|
| **ComposeActivity.kt** | Extends `ComponentActivity`, uses `setContent`, Android lifecycle |
| **ComposeViewModel.kt** | Extends `AndroidViewModel`, uses `DownloadManager`, `Intent`, `Toast`, `Application` context |
| **Theme.kt** | Uses `dynamicDarkColorScheme(context)`, `Build.VERSION`, `LocalContext` |

## Detailed Dependency Analysis

### ComposeViewModel.kt - Android Dependencies

```kotlin
// Framework
android.app.Application
android.app.DownloadManager
android.content.Intent
android.os.Build
android.util.Log
android.widget.Toast

// AndroidX
androidx.lifecycle.AndroidViewModel
androidx.lifecycle.viewModelScope
androidx.lifecycle.ProcessLifecycleOwner
androidx.core.net.toUri

// Project-specific Android
com.mediathekview.android.repository.MediaRepository
com.mediathekview.android.repository.DownloadRepository
com.mediathekview.android.video.VideoPlayerManager
com.mediathekview.android.util.UpdateChecker
com.mediathekview.android.util.AppConfig
```

### Theme.kt - Android Dependencies

```kotlin
android.os.Build
androidx.compose.ui.platform.LocalContext
androidx.compose.material3.dynamicDarkColorScheme
androidx.compose.material3.dynamicLightColorScheme
```

### Navigation.kt - Android Dependencies

```kotlin
android.net.Uri
androidx.activity.compose.BackHandler
androidx.lifecycle.compose.collectAsStateWithLifecycle
androidx.navigation.NavHostController
androidx.navigation.compose.NavHost
androidx.navigation.compose.composable
```

## Key Blockers for Full Migration

1. **AndroidViewModel** - ViewModel extends `AndroidViewModel(application)` and requires Application context
2. **Navigation Framework** - `NavHost`/`NavController` are Android Compose specific implementations
3. **Resource Strings** - Dialogs use `stringResource(R.string.*)` pattern
4. **Dynamic Theming** - Material 3 dynamic colors require Android Context and Build version
5. **Repository Layer** - Room database entities are Android-specific
6. **Video Playback** - `VideoPlayerManager` uses Android Media3

## Migration Recommendations

### Phase 1: Quick Wins (Immediate)

Move to `shared/commonMain/kotlin/com/mediathekview/shared/`:

1. **ComposeModels.kt** → `shared/model/`
   - `data class MediaItem`
   - `data class Channel`
   - `object SampleData`

2. **Type.kt** → `shared/ui/theme/` (if no Android dependencies)

### Phase 2: Abstract Dependencies

Create interfaces in shared module, implementations in Android:

```kotlin
// shared/commonMain
interface MediaRepository {
    fun getMediaItems(): Flow<List<MediaEntry>>
    fun searchMedia(query: String): Flow<List<MediaEntry>>
}

interface VideoPlayer {
    fun play(url: String)
    fun pause()
}

interface StringProvider {
    fun getString(key: StringKey): String
}
```

### Phase 3: Extract Pure UI Composables

Refactor screen composables to receive data and callbacks as parameters:

```kotlin
// shared/commonMain - Pure composable
@Composable
fun MediaList(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit
)

// androidMain - Android-specific wrapper
@Composable
fun MediaListScreen(viewModel: ComposeViewModel) {
    val items by viewModel.items.collectAsState()
    MediaList(
        items = items,
        onItemClick = { viewModel.onItemClick(it) },
        onItemLongClick = { viewModel.onItemLongClick(it) }
    )
}
```

### Phase 4: Platform-Specific Theme

Use expect/actual for theming:

```kotlin
// shared/commonMain
expect fun getPlatformColorScheme(darkTheme: Boolean): ColorScheme

// androidMain
actual fun getPlatformColorScheme(darkTheme: Boolean): ColorScheme {
    val context = LocalContext.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context)
        else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) DarkColorScheme else LightColorScheme
    }
}

// jsMain
actual fun getPlatformColorScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) DarkColorScheme else LightColorScheme
}
```

### Phase 5: Refactor ViewModel

Replace AndroidViewModel with multiplatform approach:

```kotlin
// shared/commonMain
class SharedViewModel(
    private val mediaRepository: MediaRepository,
    private val coroutineScope: CoroutineScope
) {
    // Business logic here
}

// androidMain
class AndroidComposeViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedViewModel = SharedViewModel(
        mediaRepository = AndroidMediaRepository(application),
        coroutineScope = viewModelScope
    )
}
```

## Summary Table

| File | Status | Action |
|------|--------|--------|
| ComposeModels.kt | Ready | Move to shared |
| Type.kt | Likely Ready | Verify and move |
| BrowseView.kt | Needs Work | Extract pure composables |
| DetailView.kt | Needs Work | Extract pure composables |
| AppDialogs.kt | Needs Work | Inject string resources |
| ComposeDataMapper.kt | Needs Work | Update imports to shared models |
| Navigation.kt | Needs Work | Extract state management |
| Theme.kt | Android-Only | Use expect/actual pattern |
| ComposeViewModel.kt | Android-Only | Create shared business logic layer |
| ComposeActivity.kt | Android-Only | Keep as Android entry point |

## Estimated Effort

- **Phase 1 (Quick Wins)**: Low effort - simple file moves
- **Phase 2 (Interfaces)**: Medium effort - design abstractions
- **Phase 3 (UI Extraction)**: Medium-High effort - refactor composables
- **Phase 4 (Theme)**: Low effort - expect/actual pattern
- **Phase 5 (ViewModel)**: High effort - architectural change

## Conclusion

Approximately **20-30%** of the Compose UI code can be moved to shared with minimal changes (data models, typography). Another **40-50%** (screen composables, dialogs, data mapper) can be shared after refactoring to remove Android dependencies. The remaining **20-30%** (Activity, ViewModel, Theme) must stay Android-specific but can delegate to shared logic through abstractions.
