package cut.the.crap.android.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import cut.the.crap.android.R
import cut.the.crap.android.video.PlaybackPositionManager

/**
 * Video player activity using ExoPlayer for in-app video playback
 * Supports HTTP/HTTPS streaming with custom user agent, subtitles, and resume position
 */
@UnstableApi
class VideoPlayerActivity : Activity() {

    companion object {
        private const val TAG = "VideoPlayerActivity"

        // Intent extras
        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_VIDEO_TITLE = "video_title"
        const val EXTRA_SUBTITLE_URL = "subtitle_url"
        const val EXTRA_VIDEO_ID = "video_id"
        const val EXTRA_REMEMBER_POSITION = "remember_position"
    }

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var positionManager: PlaybackPositionManager

    // Track video info for position saving
    private var videoId: String? = null
    private var shouldRememberPosition: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        playerView = findViewById(R.id.player_view)
        positionManager = PlaybackPositionManager(this)

        // Get video URL from intent
        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
        val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE)
        val subtitleUrl = intent.getStringExtra(EXTRA_SUBTITLE_URL)
        videoId = intent.getStringExtra(EXTRA_VIDEO_ID)
        shouldRememberPosition = intent.getBooleanExtra(EXTRA_REMEMBER_POSITION, true)

        if (videoUrl.isNullOrEmpty()) {
            Log.e(TAG, "No video URL provided")
            Toast.makeText(this, "Error: No video URL", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.i(TAG, "VideoPlayerActivity started")
        Log.d(TAG, "Video URL: $videoUrl")
        Log.d(TAG, "Video title: $videoTitle")
        Log.d(TAG, "Subtitle URL: ${subtitleUrl ?: "none"}")
        Log.d(TAG, "Video ID: ${videoId ?: "none"}")
        Log.d(TAG, "Remember position: $shouldRememberPosition")

        // Set title if provided
        if (!videoTitle.isNullOrEmpty()) {
            title = videoTitle
        }

        initializePlayer(videoUrl, subtitleUrl)
    }

    private fun initializePlayer(videoUrl: String, subtitleUrl: String?) {
        // Create HTTP data source factory with custom user agent
        // Using browser-like User-Agent for better compatibility with streaming services (ORF, etc.)
        val userAgent = "Mozilla/5.0 (Linux; Android 10; Android TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.210 Safari/537.36"
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS)
            .setReadTimeoutMs(DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS)

        Log.d(TAG, "Using User-Agent: $userAgent")

        // Create media source factory
        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(dataSourceFactory)

        // Create ExoPlayer instance
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .also { exoPlayer ->
                // Bind player to view
                playerView.player = exoPlayer

                // Create media item with optional subtitles
                val mediaItem = buildMediaItem(videoUrl, subtitleUrl)
                exoPlayer.setMediaItem(mediaItem)

                // Add listener for playback events
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_IDLE -> Log.d(TAG, "Player state: IDLE")
                            Player.STATE_BUFFERING -> Log.d(TAG, "Player state: BUFFERING")
                            Player.STATE_READY -> {
                                Log.d(TAG, "Player state: READY")
                                // Restore position after player is ready
                                restoreSavedPosition(exoPlayer)
                            }
                            Player.STATE_ENDED -> {
                                Log.d(TAG, "Player state: ENDED")
                                // Clear saved position when video completes
                                videoId?.let { positionManager.clearPosition(it) }
                                finish()
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "Playback error: ${error.message}", error)
                        Toast.makeText(
                            this@VideoPlayerActivity,
                            "Playback error: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        Log.d(TAG, "Is playing: $isPlaying")
                    }
                })

                // Prepare and start playback
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true

                Log.i(TAG, "Player initialized and playback started")
            }
    }

    /**
     * Build MediaItem with optional subtitles
     */
    private fun buildMediaItem(videoUrl: String, subtitleUrl: String?): MediaItem {
        val builder = MediaItem.Builder()
            .setUri(videoUrl)

        // Add subtitle configuration if URL provided
        if (!subtitleUrl.isNullOrBlank()) {
            Log.d(TAG, "Adding subtitle track: $subtitleUrl")

            // Determine MIME type based on URL extension
            val mimeType = when {
                subtitleUrl.endsWith(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
                subtitleUrl.endsWith(".srt", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
                subtitleUrl.endsWith(".ttml", ignoreCase = true) -> MimeTypes.APPLICATION_TTML
                subtitleUrl.endsWith(".xml", ignoreCase = true) -> MimeTypes.APPLICATION_TTML
                else -> MimeTypes.TEXT_VTT // Default to WebVTT
            }

            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitleUrl))
                .setMimeType(mimeType)
                .setLanguage("de") // German subtitles by default
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()

            builder.setSubtitleConfigurations(listOf(subtitleConfig))
        }

        return builder.build()
    }

    /**
     * Restore saved position if available
     */
    private fun restoreSavedPosition(exoPlayer: ExoPlayer) {
        if (!shouldRememberPosition || videoId.isNullOrBlank()) {
            Log.d(TAG, "Position restore skipped (remember=$shouldRememberPosition, videoId=$videoId)")
            return
        }

        val savedPosition = positionManager.getPosition(videoId!!)
        if (savedPosition > 0) {
            Log.i(TAG, "Restoring position: ${savedPosition}ms")
            exoPlayer.seekTo(savedPosition)
            Toast.makeText(
                this,
                getString(R.string.resuming_playback),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Save current playback position
     */
    private fun saveCurrentPosition() {
        if (!shouldRememberPosition || videoId.isNullOrBlank()) {
            return
        }

        player?.let { exoPlayer ->
            val position = exoPlayer.currentPosition
            val duration = exoPlayer.duration
            if (position > 0 && duration > 0) {
                positionManager.savePosition(videoId!!, position, duration)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Resume playback if needed
        player?.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        // Save position before stopping
        saveCurrentPosition()
        // Pause playback when activity is not visible
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        // Final save before destroying
        saveCurrentPosition()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            Log.d(TAG, "Releasing player")
            exoPlayer.release()
        }
        player = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Handle back button and escape key to exit player
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
