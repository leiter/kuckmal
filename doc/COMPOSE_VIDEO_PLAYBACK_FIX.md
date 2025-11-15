# Compose Video Playback Fix

## Issue
Video playback was not working on the first click in the Compose detail view. The video would only start on the second attempt or when switching tabs.

## Root Causes
1. **Initial Issue**: The `currentMediaEntry` in MediaViewModel wasn't set when the play button was first clicked due to asynchronous loading.
2. **SharedFlow Issue**: The `startActivityIntent` SharedFlow had `replay = 0`, meaning if the collector wasn't ready when the intent was emitted, the intent would be lost.

## Solution
1. **Direct MediaEntry Passing**: Instead of relying on the asynchronously loaded `currentMediaEntry`, we now pass the MediaEntry directly when the play button is clicked.
2. **SharedFlow Replay**: Changed the `startActivityIntent` SharedFlow from `replay = 0` to `replay = 1` to cache the last emitted intent, ensuring it's not lost if the collector isn't ready immediately.

## Implementation Details

### 1. Added MediaEntry Retrieval (`ComposeMediaScreen.kt`)
Created a new function to get the raw MediaEntry:

```kotlin
@Composable
fun getMediaEntry(
    title: String,
    channel: String? = null,
    theme: String? = null
): State<MediaEntry?>
```

This function retrieves the MediaEntry directly from the repository data flow.

### 2. Updated Play/Download Handlers (`ComposeMediaScreen.kt`)
Modified the click handlers to accept and use the MediaEntry parameter:

```kotlin
fun onPlayClicked(mediaEntry: MediaEntry?, isHighQuality: Boolean) {
    if (mediaEntry != null) {
        // Set the media entry in the ViewModel state first
        viewModel.setCurrentMediaEntry(mediaEntry)
        // Then trigger playback
        viewModel.onPlayButtonClicked(isHighQuality)
    }
}
```

### 3. Added Direct Entry Setting (`MediaViewModel.kt`)
Created a new method to set the current media entry directly:

```kotlin
fun setCurrentMediaEntry(mediaEntry: MediaEntry) {
    // Set the detail state with the provided entry
    _viewState.value = ViewState.Detail(
        mediaEntry = mediaEntry,
        navigationChannel = navigationChannel,
        navigationTheme = navigationTheme,
        searchFilter = searchFilter,
        selectedItem = selectedItem
    )
}
```

### 4. Updated Navigation (`Navigation.kt`)
Modified the detail screen to:
- Retrieve both MediaItem (for display) and MediaEntry (for actions)
- Pass the MediaEntry to play/download click handlers

```kotlin
val mediaItem by mediaState.getMediaItem(title, channel, theme)
val mediaEntry by mediaState.getMediaEntry(title, channel, theme)

DetailView(
    mediaItem = item,
    onPlayClick = { isHighQuality ->
        mediaState.onPlayClicked(mediaEntry, isHighQuality)
    },
    onDownloadClick = { isHighQuality ->
        mediaState.onDownloadClicked(mediaEntry, isHighQuality)
    }
)
```

### 5. Intent Handling (`ComposeActivity.kt`)
Added LaunchedEffect to collect and handle video player intents:

```kotlin
LaunchedEffect(viewModel) {
    viewModel.startActivityIntent.collect { intent ->
        context.startActivity(intent)
    }
}
```

### 6. Fixed SharedFlow Replay (`MediaViewModel.kt`)
Changed the SharedFlow configuration to cache the last emitted intent:

```kotlin
// Before (intent could be lost if collector wasn't ready)
private val _startActivityIntent = MutableSharedFlow<Intent>(replay = 0, extraBufferCapacity = 1)

// After (last intent is cached and replayed to new collectors)
private val _startActivityIntent = MutableSharedFlow<Intent>(replay = 1, extraBufferCapacity = 1)
```

## Benefits
1. **Immediate Playback**: Video plays on the first click without waiting for async loading
2. **Reliable State**: The MediaEntry is guaranteed to be available when needed
3. **Better UX**: No confusion or frustration from buttons that don't work on first click
4. **Consistent Behavior**: Same playback experience as the traditional UI

## Testing
1. Navigate to Compose UI via the menu
2. Browse to: All Themes → Select Theme → Select Title
3. In detail view, select quality (Hoch/Niedrig)
4. Click play button - video should start immediately on first click
5. Test with different videos to ensure consistent behavior

## Files Modified
- `/app/src/main/java/com/mediathekview/android/compose/screens/ComposeMediaScreen.kt`
- `/app/src/main/java/com/mediathekview/android/compose/navigation/Navigation.kt`
- `/app/src/main/java/com/mediathekview/android/data/MediaViewModel.kt`
- `/app/src/main/java/com/mediathekview/android/compose/ComposeActivity.kt`

## Future Improvements
1. Consider preloading media entries when navigating to detail view
2. Add loading state feedback while preparing video playback
3. Implement error handling for failed playback attempts
4. Add analytics to track playback success rates