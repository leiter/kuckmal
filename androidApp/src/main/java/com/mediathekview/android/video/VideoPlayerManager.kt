package com.mediathekview.android.video

import android.app.Application
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.mediathekview.android.R
import com.mediathekview.shared.database.MediaEntry
import com.mediathekview.android.util.MediaUrlUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Manager class that handles video playback using the expect/actual pattern
 * This provides a clean abstraction layer between the ViewModel and platform-specific video players
 */
class VideoPlayerManager(
    private val application: Application,
    private val videoPlayerFactory: VideoPlayerFactory
) {
    companion object {
        private const val TAG = "VideoPlayerManager"
    }

    // Coroutine scope for this manager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Flow for emitting intents to be handled by activities
    private val _playbackIntents = MutableSharedFlow<Intent>(replay = 0, extraBufferCapacity = 1)
    val playbackIntents: SharedFlow<Intent> = _playbackIntents.asSharedFlow()

    // Video player instance - initialized eagerly to ensure collector is ready
    private val videoPlayer: VideoPlayer = videoPlayerFactory.create(
        VideoPlayerConfig(
            enableSubtitles = true,
            autoPlay = true,
            rememberPosition = false,
            userAgent = "MediathekView-Android/1.0"
        )
    ).also { player ->
        // Set up collection from AndroidVideoPlayer's flow at initialization time
        if (player is AndroidVideoPlayer) {
            scope.launch {
                Log.d(TAG, "Setting up playback intent collector")
                player.playbackIntent.collect { intent ->
                    Log.d(TAG, "Forwarding playback intent")
                    _playbackIntents.emit(intent)
                }
            }
        }
    }

    /**
     * Play a video from a MediaEntry
     * @param mediaEntry The media entry containing video information
     * @param isHighQuality Whether to play high quality version
     */
    suspend fun playVideo(mediaEntry: MediaEntry, isHighQuality: Boolean) {
        Log.d(TAG, "=".repeat(80))
        Log.d(TAG, "VIDEO PLAYBACK REQUEST")
        Log.d(TAG, "=".repeat(80))
        Log.d(TAG, "Title: ${mediaEntry.title}")
        Log.d(TAG, "Quality requested: ${if (isHighQuality) "HIGH" else "LOW"}")

        // Select the appropriate video URL
        val videoUrl = selectVideoUrl(mediaEntry, isHighQuality)

        if (videoUrl.isEmpty()) {
            Log.e(TAG, "No video URL available for: ${mediaEntry.title}")
            showToast(R.string.error_no_video_url)
            return
        }

        Log.d(TAG, "Selected video URL: $videoUrl")
        Log.d(TAG, "=".repeat(80))

        // Determine quality setting for player
        val quality = when {
            isHighQuality -> VideoQuality.HIGH
            !isHighQuality -> VideoQuality.LOW
            else -> VideoQuality.AUTO
        }

        // Start playback using the video player
        val result = videoPlayer.play(
            url = videoUrl,
            title = mediaEntry.title,
            quality = quality
        )

        result.fold(
            onSuccess = {
                Log.i(TAG, "Successfully started playback for: ${mediaEntry.title}")
                // Intent forwarding is handled by the collector set up in init
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to start playback", error)
                showToast(R.string.error_playback_failed)
            }
        )
    }

    /**
     * Select the appropriate video URL based on quality preference and availability
     * @param mediaEntry The media entry
     * @param isHighQuality Whether to prefer high quality
     * @return The selected video URL, or empty string if none available
     */
    private fun selectVideoUrl(mediaEntry: MediaEntry, isHighQuality: Boolean): String {
        // TEMPORARY FIX: Force using main URL for BR.de videos
        val forceMainUrl = mediaEntry.url.contains("cdn-storage.br.de")

        return when {
            forceMainUrl -> {
                Log.w(TAG, "Forcing main URL for BR.de video (quality-specific URLs may not be available)")
                MediaUrlUtils.cleanMediaUrl(mediaEntry.url)
            }
            isHighQuality -> selectHighQualityUrl(mediaEntry)
            else -> selectLowQualityUrl(mediaEntry)
        }
    }

    /**
     * Select high quality video URL
     */
    private fun selectHighQualityUrl(mediaEntry: MediaEntry): String {
        if (mediaEntry.hdUrl.isNotEmpty()) {
            val hdUrl = mediaEntry.hdUrl
            Log.d(TAG, "HD URL available: $hdUrl")

            val reconstructed = MediaUrlUtils.reconstructUrl(hdUrl, mediaEntry.url)
            Log.d(TAG, "HD URL reconstructed: $reconstructed")

            // Check if reconstruction resulted in same URL as main
            return if (reconstructed == MediaUrlUtils.cleanMediaUrl(mediaEntry.url)) {
                Log.w(TAG, "HD URL same as main URL, using main URL")
                MediaUrlUtils.cleanMediaUrl(mediaEntry.url)
            } else {
                Log.i(TAG, "Using HD quality URL")
                reconstructed
            }
        } else {
            Log.d(TAG, "No HD URL available, falling back to main URL")
            return MediaUrlUtils.cleanMediaUrl(mediaEntry.url)
        }
    }

    /**
     * Select low quality video URL
     */
    private fun selectLowQualityUrl(mediaEntry: MediaEntry): String {
        if (mediaEntry.smallUrl.isNotEmpty()) {
            val smallUrl = mediaEntry.smallUrl
            Log.d(TAG, "Small URL available: $smallUrl")

            val reconstructed = MediaUrlUtils.reconstructUrl(smallUrl, mediaEntry.url)
            Log.d(TAG, "Small URL reconstructed: $reconstructed")

            // Check if reconstruction resulted in same URL as main
            return if (reconstructed == MediaUrlUtils.cleanMediaUrl(mediaEntry.url)) {
                Log.w(TAG, "Small URL same as main URL, using main URL")
                MediaUrlUtils.cleanMediaUrl(mediaEntry.url)
            } else {
                Log.i(TAG, "Using LOW quality URL")
                reconstructed
            }
        } else {
            Log.d(TAG, "No small URL available, falling back to main URL")
            return MediaUrlUtils.cleanMediaUrl(mediaEntry.url)
        }
    }

    /**
     * Check if a URL can be played
     */
    fun canPlayUrl(url: String): Boolean {
        return videoPlayer.isPlayable(url)
    }

    /**
     * Get supported video formats
     */
    fun getSupportedFormats(): List<String> {
        return videoPlayer.getSupportedFormats()
    }

    /**
     * Show a toast message
     */
    private fun showToast(messageResId: Int) {
        Toast.makeText(
            application,
            application.getString(messageResId),
            Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * Extension function to create VideoPlayerManager easily
 */
fun Application.createVideoPlayerManager(): VideoPlayerManager {
    val factory = AndroidVideoPlayerFactory(this)
    return VideoPlayerManager(this, factory)
}