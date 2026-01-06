package cut.the.crap.android.video

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Manager for storing and retrieving video playback positions.
 * Uses SharedPreferences for persistent storage of last viewed positions.
 */
class PlaybackPositionManager(context: Context) {

    companion object {
        private const val TAG = "PlaybackPositionManager"
        private const val PREFS_NAME = "kuckmal_playback_positions"
        private const val KEY_PREFIX = "position_"
        private const val MAX_STORED_POSITIONS = 100 // Limit storage to prevent unbounded growth

        // Minimum position to save (avoid saving very short views)
        private const val MIN_POSITION_TO_SAVE_MS = 5000L // 5 seconds

        // Position threshold for "completed" (don't resume if near end)
        private const val COMPLETION_THRESHOLD_PERCENT = 95
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Save playback position for a video
     * @param videoId Unique identifier for the video
     * @param positionMs Current playback position in milliseconds
     * @param durationMs Total video duration in milliseconds
     */
    fun savePosition(videoId: String, positionMs: Long, durationMs: Long) {
        if (videoId.isBlank()) {
            Log.w(TAG, "Cannot save position: videoId is blank")
            return
        }

        // Don't save if position is too early
        if (positionMs < MIN_POSITION_TO_SAVE_MS) {
            Log.d(TAG, "Position too early to save: ${positionMs}ms < ${MIN_POSITION_TO_SAVE_MS}ms")
            return
        }

        // Don't save if video is almost complete (clear position instead)
        if (durationMs > 0) {
            val percentComplete = (positionMs * 100 / durationMs).toInt()
            if (percentComplete >= COMPLETION_THRESHOLD_PERCENT) {
                Log.d(TAG, "Video nearly complete ($percentComplete%), clearing saved position")
                clearPosition(videoId)
                return
            }
        }

        val key = KEY_PREFIX + videoId
        prefs.edit().putLong(key, positionMs).apply()
        Log.d(TAG, "Saved position for $videoId: ${formatPosition(positionMs)}")

        // Cleanup old entries if needed
        cleanupOldEntries()
    }

    /**
     * Get saved playback position for a video
     * @param videoId Unique identifier for the video
     * @return Saved position in milliseconds, or 0 if no position saved
     */
    fun getPosition(videoId: String): Long {
        if (videoId.isBlank()) {
            Log.w(TAG, "Cannot get position: videoId is blank")
            return 0L
        }

        val key = KEY_PREFIX + videoId
        val position = prefs.getLong(key, 0L)

        if (position > 0) {
            Log.d(TAG, "Retrieved position for $videoId: ${formatPosition(position)}")
        } else {
            Log.d(TAG, "No saved position for $videoId")
        }

        return position
    }

    /**
     * Clear saved position for a video
     * @param videoId Unique identifier for the video
     */
    fun clearPosition(videoId: String) {
        if (videoId.isBlank()) return

        val key = KEY_PREFIX + videoId
        prefs.edit().remove(key).apply()
        Log.d(TAG, "Cleared position for $videoId")
    }

    /**
     * Check if there's a saved position for a video
     * @param videoId Unique identifier for the video
     * @return true if there's a saved position > 0
     */
    fun hasPosition(videoId: String): Boolean {
        return getPosition(videoId) > 0
    }

    /**
     * Clear all saved positions
     */
    fun clearAllPositions() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Cleared all saved positions")
    }

    /**
     * Get count of saved positions
     */
    fun getSavedCount(): Int {
        return prefs.all.count { it.key.startsWith(KEY_PREFIX) }
    }

    /**
     * Cleanup old entries if we have too many stored positions
     * Uses LRU-like strategy - removes entries that haven't been accessed recently
     */
    private fun cleanupOldEntries() {
        val allEntries = prefs.all.filter { it.key.startsWith(KEY_PREFIX) }

        if (allEntries.size > MAX_STORED_POSITIONS) {
            Log.d(TAG, "Cleaning up old entries: ${allEntries.size} > $MAX_STORED_POSITIONS")

            // Remove oldest entries (first half of excess)
            val toRemove = allEntries.keys.take(allEntries.size - MAX_STORED_POSITIONS + 10)
            prefs.edit().apply {
                toRemove.forEach { remove(it) }
            }.apply()

            Log.d(TAG, "Removed ${toRemove.size} old position entries")
        }
    }

    /**
     * Format position for logging
     */
    private fun formatPosition(positionMs: Long): String {
        val seconds = positionMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
        } else {
            String.format("%d:%02d", minutes, seconds % 60)
        }
    }
}
