package cut.the.crap.android.video

/**
 * Common interface for video playback across platforms
 * This is the "expect" declaration that defines the contract for video playback
 */
interface VideoPlayer {
    /**
     * Start playing a video
     * @param url The video URL to play
     * @param title The title to display in the player
     * @param quality The quality setting for playback
     * @return Result indicating success or failure
     */
    suspend fun play(
        url: String,
        title: String,
        quality: VideoQuality = VideoQuality.AUTO
    ): Result<Unit>

    /**
     * Check if a URL is playable
     * @param url The video URL to check
     * @return true if the URL can be played
     */
    fun isPlayable(url: String): Boolean

    /**
     * Get supported video formats
     * @return List of supported video formats/extensions
     */
    fun getSupportedFormats(): List<String>
}

/**
 * Video quality settings
 */
enum class VideoQuality {
    LOW,      // Lower quality, smaller bandwidth
    MEDIUM,   // Standard quality
    HIGH,     // High quality
    AUTO      // Let the player decide based on connection
}

/**
 * Video playback configuration
 */
data class VideoPlayerConfig(
    val enableSubtitles: Boolean = true,
    val autoPlay: Boolean = true,
    val rememberPosition: Boolean = true,
    val userAgent: String? = null,
    val httpHeaders: Map<String, String> = emptyMap()
)

/**
 * Factory interface for creating VideoPlayer instances
 */
interface VideoPlayerFactory {
    /**
     * Create a VideoPlayer instance with the given configuration
     */
    fun create(config: VideoPlayerConfig = VideoPlayerConfig()): VideoPlayer
}

/**
 * Exception thrown when video playback fails
 */
class VideoPlaybackException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Result class for video operations
 */
sealed class VideoResult<out T> {
    data class Success<T>(val data: T) : VideoResult<T>()
    data class Error(val exception: VideoPlaybackException) : VideoResult<Nothing>()
    object Loading : VideoResult<Nothing>()
}