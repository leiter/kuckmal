# Video Player Expect/Actual Implementation

## Overview
Successfully implemented an expect/actual pattern for video playback in the Kuckmal Android app. This provides a clean abstraction layer that separates platform-specific video player implementations from the business logic, making the code more maintainable and potentially supporting multiple platforms in the future.

## Architecture

### Layer Separation
```
MediaViewModel → VideoPlayerManager → VideoPlayer (interface) → AndroidVideoPlayer (implementation)
```

This architecture provides:
- **Clean separation** between business logic and platform code
- **Testability** through interface abstraction
- **Flexibility** to support different video players or platforms
- **Maintainability** with clear responsibilities for each layer

## Implementation Details

### 1. VideoPlayer Interface (`VideoPlayer.kt`)
The core interface that defines the contract for video playback:

```kotlin
interface VideoPlayer {
    suspend fun play(url: String, title: String, quality: VideoQuality): Result<Unit>
    fun isPlayable(url: String): Boolean
    fun getSupportedFormats(): List<String>
}
```

**Key Components:**
- **VideoQuality enum**: LOW, MEDIUM, HIGH, AUTO
- **VideoPlayerConfig**: Configuration for playback settings
- **VideoPlayerFactory**: Factory pattern for creating player instances
- **VideoPlaybackException**: Custom exception for playback errors
- **VideoResult**: Sealed class for operation results

### 2. Android Implementation (`AndroidVideoPlayer.kt`)
Platform-specific implementation for Android:

**Features:**
- Supports multiple video formats (mp4, webm, m3u8, etc.)
- Handles both streaming and local files
- Smart player selection:
  - ExoPlayer for HLS/DASH streams
  - System player for local files
  - Configurable HTTP headers and user agent
- Intent-based communication with VideoPlayerActivity

**Supported Formats:**
- Container: mp4, m4v, 3gp, mkv, webm, ts, avi
- Streaming: m3u8 (HLS), mpd (DASH)
- Protocols: http, https, rtsp, file

### 3. VideoPlayerManager (`VideoPlayerManager.kt`)
Manager class that coordinates video playback:

**Responsibilities:**
- URL selection based on quality preference
- Fallback logic for missing quality variants
- Special handling for specific CDNs (e.g., BR.de)
- Intent flow management
- Error handling and user feedback

**URL Selection Logic:**
1. Check for forced main URL (BR.de workaround)
2. For HIGH quality:
   - Try HD URL if available
   - Reconstruct from pipe-delimited format
   - Fall back to main URL if needed
3. For LOW quality:
   - Try small URL if available
   - Reconstruct from pipe-delimited format
   - Fall back to main URL if needed

### 4. Integration with MediaViewModel
Simplified integration replacing 100+ lines of code with:

```kotlin
fun onPlayButtonClicked(isHighQuality: Boolean) {
    val mediaEntry = currentMediaEntry ?: return

    viewModelScope.launch {
        videoPlayerManager.playVideo(mediaEntry, isHighQuality)
    }
}
```

**Flow Collection:**
```kotlin
init {
    viewModelScope.launch {
        videoPlayerManager.playbackIntents.collect { intent ->
            _startActivityIntent.emit(intent)
        }
    }
}
```

## Benefits

### 1. Code Quality
- **Reduced complexity**: Removed 100+ lines of URL selection logic from ViewModel
- **Single responsibility**: Each class has a clear, focused purpose
- **Testability**: Can mock VideoPlayer interface for testing

### 2. Maintainability
- **Centralized logic**: Video URL selection in one place
- **Easy updates**: Can modify player behavior without touching ViewModel
- **Clear boundaries**: Platform-specific code isolated in actual implementation

### 3. Extensibility
- **Multiple players**: Easy to add support for different players
- **Platform support**: Ready for Kotlin Multiplatform if needed
- **Configuration**: Flexible configuration through VideoPlayerConfig

### 4. Error Handling
- **Result type**: Type-safe error handling with Result<Unit>
- **Custom exceptions**: Clear error types with VideoPlaybackException
- **User feedback**: Consistent toast messages for errors

## Usage Example

```kotlin
// Create player manager
val playerManager = application.createVideoPlayerManager()

// Play video
lifecycleScope.launch {
    val result = playerManager.playVideo(mediaEntry, isHighQuality = true)

    result.fold(
        onSuccess = { /* Video started */ },
        onFailure = { error -> /* Handle error */ }
    )
}
```

## Configuration Options

The VideoPlayerConfig provides various options:
- `enableSubtitles`: Enable/disable subtitle support
- `autoPlay`: Start playback automatically
- `rememberPosition`: Resume from last position
- `userAgent`: Custom user agent string
- `httpHeaders`: Additional HTTP headers for requests

## Future Enhancements

### Short-term
1. **Subtitle Support**: Add subtitle URL handling
2. **Quality Detection**: Auto-detect available qualities
3. **Playback History**: Track watched videos
4. **Resume Support**: Continue from last position

### Long-term
1. **Kotlin Multiplatform**: Add iOS/Desktop implementations
2. **Download Integration**: Unified download/playback interface
3. **Analytics**: Track playback metrics
4. **Adaptive Streaming**: Better quality switching

## Testing

The implementation can be tested by:
1. Playing videos with different quality settings
2. Testing fallback when HD/Low quality URLs are unavailable
3. Verifying BR.de videos use main URL
4. Testing error handling for invalid URLs

## Files Created/Modified

### Created
- `/androidApp/src/main/java/cut/the/crap/android/video/VideoPlayer.kt`
- `/androidApp/src/main/java/cut/the/crap/android/video/AndroidVideoPlayer.kt`
- `/androidApp/src/main/java/cut/the/crap/android/video/VideoPlayerManager.kt`

### Modified
- `/androidApp/src/main/java/cut/the/crap/android/data/MediaViewModel.kt`
- `/androidApp/src/main/res/values/strings.xml`

## Migration Notes

The implementation maintains 100% backward compatibility:
- Same Intent structure for VideoPlayerActivity
- Same URL selection logic (moved to VideoPlayerManager)
- Same quality handling
- Same error messages

## Conclusion

The expect/actual pattern implementation for video playback provides a robust, maintainable, and extensible architecture. It successfully abstracts platform-specific details while maintaining all existing functionality. The code is now better organized, easier to test, and ready for future enhancements or platform expansions.