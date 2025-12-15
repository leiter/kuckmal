package cut.the.crap.android.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import cut.the.crap.android.R

/**
 * Video player activity using ExoPlayer for in-app video playback
 * Supports HTTP/HTTPS streaming with custom user agent
 */
@UnstableApi
class VideoPlayerActivity : Activity() {

    companion object {
        private const val TAG = "VideoPlayerActivity"

        // Intent extras
        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_VIDEO_TITLE = "video_title"
    }

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        playerView = findViewById(R.id.player_view)

        // Get video URL from intent
        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
        val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE)

        if (videoUrl.isNullOrEmpty()) {
            Log.e(TAG, "No video URL provided")
            Toast.makeText(this, "Error: No video URL", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.i(TAG, "VideoPlayerActivity started")
        Log.d(TAG, "Video URL: $videoUrl")
        Log.d(TAG, "Video title: $videoTitle")

        // Set title if provided
        if (!videoTitle.isNullOrEmpty()) {
            title = videoTitle
        }

        initializePlayer(videoUrl)
    }

    private fun initializePlayer(videoUrl: String) {
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

                // Create media item
                val mediaItem = MediaItem.fromUri(videoUrl)
                exoPlayer.setMediaItem(mediaItem)

                // Add listener for playback events
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_IDLE -> Log.d(TAG, "Player state: IDLE")
                            Player.STATE_BUFFERING -> Log.d(TAG, "Player state: BUFFERING")
                            Player.STATE_READY -> Log.d(TAG, "Player state: READY")
                            Player.STATE_ENDED -> {
                                Log.d(TAG, "Player state: ENDED")
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

    override fun onStart() {
        super.onStart()
        // Resume playback if needed
        player?.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        // Pause playback when activity is not visible
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
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
