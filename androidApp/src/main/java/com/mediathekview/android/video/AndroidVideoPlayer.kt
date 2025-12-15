package com.mediathekview.android.video

import android.content.Context
import android.content.Intent
import android.util.Log
import com.mediathekview.android.util.MediaUrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

/**
 * Android-specific implementation of VideoPlayer
 * This is the "actual" implementation for the Android platform
 */
class AndroidVideoPlayer(
    private val context: Context,
    private val config: VideoPlayerConfig = VideoPlayerConfig(),
) : VideoPlayer {

    companion object {
        private const val TAG = "AndroidVideoPlayer"

        // Supported video formats on Android
        private val SUPPORTED_FORMATS = listOf(
            "mp4", "m4v", "3gp", "3gpp", "3g2", "3gpp2",
            "mkv", "webm", "ts", "m3u8", "mpd", "avi"
        )

        // Supported protocols
        private val SUPPORTED_PROTOCOLS = listOf("http", "https", "rtsp", "file")
    }

    // SharedFlow to emit intents for video playback
    // replay = 1 ensures intent is available even if collector isn't ready yet
    private val _playbackIntent = MutableSharedFlow<Intent>(replay = 1, extraBufferCapacity = 0)
    val playbackIntent: SharedFlow<Intent> = _playbackIntent

    override suspend fun play(
        url: String,
        title: String,
        quality: VideoQuality
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting video playback - URL: $url, Title: $title, Quality: $quality")

            // Validate URL
            if (!isPlayable(url)) {
                val error = VideoPlaybackException("URL is not playable: $url")
                Log.e(TAG, "Playback failed", error)
                return@withContext Result.failure(error)
            }

            // Clean the URL
            val cleanUrl = MediaUrlUtils.cleanMediaUrl(url)
            if (cleanUrl.isEmpty()) {
                val error = VideoPlaybackException("Invalid URL after cleaning: $url")
                Log.e(TAG, "Playback failed", error)
                return@withContext Result.failure(error)
            }

            // Determine which player to use based on configuration or URL
            val intent = when {
                shouldUseExoPlayer(cleanUrl) -> createExoPlayerIntent(cleanUrl, title)
                shouldUseSystemPlayer(cleanUrl) -> createSystemPlayerIntent(cleanUrl, title)
                else -> createDefaultPlayerIntent(cleanUrl, title)
            }

            // Apply configuration to intent
            applyConfigToIntent(intent)

            // Emit the intent
            Log.i(TAG, "Emitting playback intent for: $title")
            _playbackIntent.emit(intent)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start video playback", e)
            Result.failure(VideoPlaybackException("Failed to start playback: ${e.message}", e))
        }
    }

    override fun isPlayable(url: String): Boolean {
        if (url.isBlank()) return false

        return try {
            val uri = url.toUri()
            val scheme = uri.scheme?.lowercase()
            val path = uri.path?.lowercase() ?: ""

            // Check protocol
            val hasValidProtocol = scheme in SUPPORTED_PROTOCOLS

            // Check file extension if present
            val extension = path.substringAfterLast('.', "")
            val hasValidExtension = extension.isEmpty() || extension in SUPPORTED_FORMATS

            // Check for streaming protocols
            val isStreamingUrl = path.endsWith(".m3u8") || path.endsWith(".mpd")

            hasValidProtocol && (hasValidExtension || isStreamingUrl)
        } catch (e: Exception) {
            Log.w(TAG, "Error checking if URL is playable: $url", e)
            false
        }
    }

    override fun getSupportedFormats(): List<String> = SUPPORTED_FORMATS

    /**
     * Determine if ExoPlayer should be used for this URL
     */
    private fun shouldUseExoPlayer(url: String): Boolean {
        // Use ExoPlayer for HLS/DASH streams and when custom headers are needed
        return url.contains(".m3u8") ||
               url.contains(".mpd") ||
               config.httpHeaders.isNotEmpty() ||
               config.userAgent != null
    }

    /**
     * Determine if system player should be used
     */
    private fun shouldUseSystemPlayer(url: String): Boolean {
        // Use system player for local files or simple streams
        return url.startsWith("file://") && config.httpHeaders.isEmpty()
    }

    /**
     * Create intent for ExoPlayer (internal player activity)
     */
    private fun createExoPlayerIntent(url: String, title: String): Intent {
        return Intent().apply {
            setClassName(
                context.packageName,
                "com.mediathekview.android.ui.VideoPlayerActivity"
            )
            putExtra("video_url", url)
            putExtra("video_title", title)
            putExtra("quality", getQualityExtra())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)  // Clear existing instances to avoid duplicates
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) // Reuse existing instance if at top
        }
    }

    /**
     * Create intent for system video player
     */
    private fun createSystemPlayerIntent(url: String, title: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(url.toUri(), "video/*")
            putExtra(Intent.EXTRA_TITLE, title)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Create default player intent (uses ExoPlayer by default)
     */
    private fun createDefaultPlayerIntent(url: String, title: String): Intent {
        return createExoPlayerIntent(url, title)
    }

    /**
     * Apply configuration settings to the intent
     */
    private fun applyConfigToIntent(intent: Intent) {
        // Add HTTP headers if configured
        if (config.httpHeaders.isNotEmpty()) {
            val headersArray = config.httpHeaders.flatMap { (key, value) ->
                listOf(key, value)
            }.toTypedArray()
            intent.putExtra("http_headers", headersArray)
        }

        // Add user agent if configured
        config.userAgent?.let {
            intent.putExtra("user_agent", it)
        }

        // Add other configuration options
        intent.putExtra("enable_subtitles", config.enableSubtitles)
        intent.putExtra("auto_play", config.autoPlay)
        intent.putExtra("remember_position", config.rememberPosition)
    }

    /**
     * Get quality extra value for intent
     */
    private fun getQualityExtra(): String {
        // This would be set based on the quality parameter passed to play()
        // For now, returning AUTO as default
        return "AUTO"
    }
}

/**
 * Factory for creating Android video player instances
 */
class AndroidVideoPlayerFactory(
    private val context: Context,
) : VideoPlayerFactory {

    override fun create(config: VideoPlayerConfig): VideoPlayer {
        return AndroidVideoPlayer(context, config)
    }
}