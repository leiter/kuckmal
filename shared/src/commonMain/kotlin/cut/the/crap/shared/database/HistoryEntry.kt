package cut.the.crap.shared.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a playback history entry.
 * Tracks watched content with resume position for "Continue Watching" functionality.
 */
@Entity(
    tableName = "history_entries",
    indices = [
        Index(value = ["channel", "theme", "title"], unique = true),
        Index(value = ["watchedAt"]),
        Index(value = ["isCompleted"])
    ]
)
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Media identification (matches MediaEntry)
    val channel: String,
    val theme: String,
    val title: String,

    // Playback progress
    val resumePositionSeconds: Long = 0,
    val durationSeconds: Long = 0,

    // Timestamp when last watched
    val watchedAt: Long,

    // Whether the video was watched to completion (>90%)
    val isCompleted: Boolean = false
) {
    /**
     * Calculate the watch progress as a percentage (0.0 to 1.0)
     */
    val progressPercent: Float
        get() = if (durationSeconds > 0) {
            (resumePositionSeconds.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    /**
     * Check if there is meaningful progress to resume from
     * (at least 10 seconds and not completed)
     */
    val hasResumePosition: Boolean
        get() = resumePositionSeconds >= 10 && !isCompleted
}
