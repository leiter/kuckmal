package cut.the.crap.shared.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for history entries.
 * Handles playback history and continue watching functionality.
 */
@Dao
interface HistoryDao {

    /**
     * Insert or update a history entry (upsert)
     * Uses REPLACE to update existing entries with same channel/theme/title
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: HistoryEntry): Long

    /**
     * Get continue watching entries (not completed, sorted by most recently watched)
     */
    @Query("""
        SELECT * FROM history_entries
        WHERE isCompleted = 0
        ORDER BY watchedAt DESC
        LIMIT :limit
    """)
    fun getContinueWatching(limit: Int = 20): Flow<List<HistoryEntry>>

    /**
     * Get full playback history (sorted by most recently watched)
     */
    @Query("""
        SELECT * FROM history_entries
        ORDER BY watchedAt DESC
        LIMIT :limit
    """)
    fun getHistory(limit: Int = 50): Flow<List<HistoryEntry>>

    /**
     * Get all history entries as Flow (reactive)
     */
    @Query("SELECT * FROM history_entries ORDER BY watchedAt DESC")
    fun getAll(): Flow<List<HistoryEntry>>

    /**
     * Get resume position for a specific entry
     */
    @Query("""
        SELECT resumePositionSeconds FROM history_entries
        WHERE channel = :channel AND theme = :theme AND title = :title
    """)
    suspend fun getResumePosition(channel: String, theme: String, title: String): Long?

    /**
     * Get the history entry for a specific media item
     */
    @Query("""
        SELECT * FROM history_entries
        WHERE channel = :channel AND theme = :theme AND title = :title
        LIMIT 1
    """)
    suspend fun getEntry(channel: String, theme: String, title: String): HistoryEntry?

    /**
     * Get the history entry for a specific media item as Flow
     */
    @Query("""
        SELECT * FROM history_entries
        WHERE channel = :channel AND theme = :theme AND title = :title
        LIMIT 1
    """)
    fun getEntryFlow(channel: String, theme: String, title: String): Flow<HistoryEntry?>

    /**
     * Update playback progress for an entry
     */
    @Query("""
        UPDATE history_entries
        SET resumePositionSeconds = :positionSeconds,
            durationSeconds = :durationSeconds,
            watchedAt = :watchedAt,
            isCompleted = :isCompleted
        WHERE channel = :channel AND theme = :theme AND title = :title
    """)
    suspend fun updateProgress(
        channel: String,
        theme: String,
        title: String,
        positionSeconds: Long,
        durationSeconds: Long,
        watchedAt: Long,
        isCompleted: Boolean
    )

    /**
     * Mark an entry as completed
     */
    @Query("""
        UPDATE history_entries
        SET isCompleted = 1, watchedAt = :watchedAt
        WHERE channel = :channel AND theme = :theme AND title = :title
    """)
    suspend fun markCompleted(channel: String, theme: String, title: String, watchedAt: Long)

    /**
     * Delete a specific history entry
     */
    @Query("DELETE FROM history_entries WHERE channel = :channel AND theme = :theme AND title = :title")
    suspend fun delete(channel: String, theme: String, title: String)

    /**
     * Clear all history
     */
    @Query("DELETE FROM history_entries")
    suspend fun clearAll()

    /**
     * Clear completed entries only
     */
    @Query("DELETE FROM history_entries WHERE isCompleted = 1")
    suspend fun clearCompleted()

    /**
     * Get count of continue watching entries
     */
    @Query("SELECT COUNT(*) FROM history_entries WHERE isCompleted = 0")
    fun getContinueWatchingCount(): Flow<Int>

    /**
     * Get total history count
     */
    @Query("SELECT COUNT(*) FROM history_entries")
    fun getHistoryCount(): Flow<Int>

    /**
     * Delete old entries (older than timestamp)
     */
    @Query("DELETE FROM history_entries WHERE watchedAt < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)
}
