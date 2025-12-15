package cut.the.crap.shared.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for media entries
 */
@Dao
interface MediaDao {

    /**
     * Insert a single media entry
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MediaEntry): Long

    /**
     * Insert multiple media entries in batch (much faster for bulk inserts)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<MediaEntry>)

    /**
     * Insert multiple media entries in batches with transaction
     * This is more efficient for very large datasets
     */
    @Transaction
    suspend fun insertInBatches(entries: List<MediaEntry>, batchSize: Int = 500) {
        entries.chunked(batchSize).forEach { batch ->
            insertAll(batch)
        }
    }

    /**
     * Replace all data in a transaction
     * Deletes all existing entries and inserts new ones
     */
    @Transaction
    suspend fun replaceAll(entries: List<MediaEntry>) {
        deleteAll()
        insertInBatches(entries)
    }

    /**
     * Delete all media entries (for refresh/reload)
     */
    @Query("DELETE FROM media_entries")
    suspend fun deleteAll()

    /**
     * Get total count of media entries
     */
    @Query("SELECT COUNT(*) FROM media_entries")
    suspend fun getCount(): Int

    /**
     * Get count as Flow for observing
     */
    @Query("SELECT COUNT(*) FROM media_entries")
    fun getCountFlow(): Flow<Int>

    /**
     * Get all unique channels sorted alphabetically
     */
    @Query("SELECT DISTINCT channel FROM media_entries ORDER BY channel ASC")
    suspend fun getAllChannels(): List<String>

    /**
     * Get all unique channels as Flow (reactive)
     */
    @Query("SELECT DISTINCT channel FROM media_entries ORDER BY channel ASC")
    fun getAllChannelsFlow(): Flow<List<String>>

    /**
     * Get all unique themes across all channels, with pagination
     */
    @Query("""
        SELECT DISTINCT theme FROM media_entries
        WHERE timestamp >= :limitDate
        ORDER BY theme ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getAllThemes(limitDate: Long, limit: Int, offset: Int): List<String>

    /**
     * Get all unique themes as Flow (reactive)
     */
    @Query("""
        SELECT DISTINCT theme FROM media_entries
        WHERE timestamp >= :limitDate
        ORDER BY theme ASC
        LIMIT :limit OFFSET :offset
    """)
    fun getAllThemesFlow(limitDate: Long, limit: Int, offset: Int): Flow<List<String>>

    /**
     * Get themes for a specific channel with pagination
     */
    @Query("""
        SELECT DISTINCT theme FROM media_entries
        WHERE channel = :channel AND timestamp >= :limitDate
        ORDER BY theme ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getThemesForChannel(channel: String, limitDate: Long, limit: Int, offset: Int): List<String>

    /**
     * Get themes for a specific channel as Flow (reactive)
     */
    @Query("""
        SELECT DISTINCT theme FROM media_entries
        WHERE channel = :channel AND timestamp >= :limitDate
        ORDER BY theme ASC
        LIMIT :limit OFFSET :offset
    """)
    fun getThemesForChannelFlow(channel: String, limitDate: Long, limit: Int, offset: Int): Flow<List<String>>

    /**
     * Get titles for a specific theme across all channels
     */
    @Query("""
        SELECT title FROM media_entries
        WHERE theme = :theme AND timestamp >= :limitDate
        ORDER BY timestamp DESC
    """)
    suspend fun getTitlesForTheme(theme: String, limitDate: Long): List<String>

    /**
     * Get titles for a specific theme and channel
     */
    @Query("""
        SELECT title FROM media_entries
        WHERE channel = :channel AND theme = :theme AND timestamp >= :limitDate
        ORDER BY timestamp DESC
    """)
    suspend fun getTitlesForChannelAndTheme(channel: String, theme: String, limitDate: Long): List<String>

    /**
     * Get titles for a specific theme across all channels as Flow (reactive)
     */
    @Query("""
        SELECT title FROM media_entries
        WHERE theme = :theme AND timestamp >= :limitDate
        ORDER BY timestamp DESC
    """)
    fun getTitlesForThemeFlow(theme: String, limitDate: Long): Flow<List<String>>

    /**
     * Get titles for a specific channel and theme as Flow (reactive)
     */
    @Query("""
        SELECT title FROM media_entries
        WHERE channel = :channel AND theme = :theme AND timestamp >= :limitDate
        ORDER BY timestamp DESC
    """)
    fun getTitlesForChannelAndThemeFlow(channel: String, theme: String, limitDate: Long): Flow<List<String>>

    /**
     * Get full media info for a specific entry
     */
    @Query("""
        SELECT * FROM media_entries
        WHERE channel = :channel AND theme = :theme AND title = :title
        LIMIT 1
    """)
    suspend fun getMediaEntry(channel: String, theme: String, title: String): MediaEntry?

    /**
     * Get full media info for a specific entry as Flow (reactive)
     */
    @Query("""
        SELECT * FROM media_entries
        WHERE channel = :channel AND theme = :theme AND title = :title
        LIMIT 1
    """)
    fun getMediaEntryFlow(channel: String, theme: String, title: String): Flow<MediaEntry?>

    /**
     * Get full media info for a specific entry by theme and title only (when channel not selected)
     */
    @Query("""
        SELECT * FROM media_entries
        WHERE theme = :theme AND title = :title
        LIMIT 1
    """)
    fun getMediaEntryByThemeAndTitleFlow(theme: String, title: String): Flow<MediaEntry?>

    /**
     * Get full media info for a specific entry by title only (for search results navigation)
     * Used when navigating from search results where the entry could be from any theme
     */
    @Query("""
        SELECT * FROM media_entries
        WHERE title = :title
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    fun getMediaEntryByTitleFlow(title: String): Flow<MediaEntry?>

    /**
     * Search media entries by title (for future search feature)
     */
    @Query("""
        SELECT * FROM media_entries
        WHERE title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
           OR theme LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun searchEntries(query: String, limit: Int = 100): List<MediaEntry>

    /**
     * Search media entries within a specific channel
     */
    @Query("""
        SELECT * FROM media_entries
        WHERE channel = :channel
          AND (title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
           OR theme LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun searchEntriesByChannel(channel: String, query: String, limit: Int = 100): List<MediaEntry>

    /**
     * Search media entries within a specific theme (across all channels)
     */
    @Query("""
        SELECT * FROM media_entries
        WHERE theme = :theme
          AND (title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun searchEntriesByTheme(theme: String, query: String, limit: Int = 100): List<MediaEntry>

    /**
     * Search media entries within a specific channel and theme
     */
    @Query("""
        SELECT * FROM media_entries
        WHERE channel = :channel
          AND theme = :theme
          AND (title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun searchEntriesByChannelAndTheme(channel: String, theme: String, query: String, limit: Int = 100): List<MediaEntry>

    /**
     * Search media entries with offset support for incremental loading
     */
    @Query("""
        SELECT * FROM media_entries
        WHERE title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
           OR theme LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchEntriesWithOffset(query: String, limit: Int = 100, offset: Int = 0): List<MediaEntry>

    /**
     * Search media entries within a specific channel with offset support
     */
    @Query("""
        SELECT * FROM media_entries
        WHERE channel = :channel
          AND (title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
           OR theme LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchEntriesByChannelWithOffset(channel: String, query: String, limit: Int = 100, offset: Int = 0): List<MediaEntry>

    /**
     * Search media entries within a specific theme with offset support
     */
    @Query("""
        SELECT * FROM media_entries
        WHERE theme = :theme
          AND (title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchEntriesByThemeWithOffset(theme: String, query: String, limit: Int = 100, offset: Int = 0): List<MediaEntry>

    /**
     * Search media entries within a specific channel and theme with offset support
     */
    @Query("""
        SELECT * FROM media_entries
        WHERE channel = :channel
          AND theme = :theme
          AND (title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchEntriesByChannelAndThemeWithOffset(channel: String, theme: String, query: String, limit: Int = 100, offset: Int = 0): List<MediaEntry>

    /**
     * Get entries by channel with pagination
     */
    @Query("""
        SELECT * FROM media_entries
        WHERE channel = :channel AND timestamp >= :limitDate
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getEntriesByChannel(
        channel: String,
        limitDate: Long,
        limit: Int,
        offset: Int
    ): List<MediaEntry>

    /**
     * Get recent entries (for "new" content)
     */
    @Query("""
        SELECT * FROM media_entries
        WHERE isNew = 1 OR timestamp >= :sinceTimestamp
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getRecentEntries(sinceTimestamp: Long, limit: Int = 100): List<MediaEntry>

    /**
     * Get one representative MediaEntry per theme (all channels)
     * Returns the first entry for each theme, sorted by theme name
     * Used for displaying themes as full MediaEntry objects
     */
    @Query("""
        SELECT e.* FROM media_entries e
        INNER JOIN (
            SELECT theme, MIN(id) as min_id
            FROM media_entries
            WHERE timestamp >= :limitDate
            GROUP BY theme
            ORDER BY theme ASC
            LIMIT :limit OFFSET :offset
        ) t ON e.id = t.min_id
        ORDER BY e.theme ASC
    """)
    fun getAllThemesAsEntriesFlow(limitDate: Long, limit: Int, offset: Int): Flow<List<MediaEntry>>

    /**
     * Get one representative MediaEntry per theme (specific channel)
     * Returns the first entry for each theme in the channel
     */
    @Query("""
        SELECT e.* FROM media_entries e
        INNER JOIN (
            SELECT theme, MIN(id) as min_id
            FROM media_entries
            WHERE channel = :channel AND timestamp >= :limitDate
            GROUP BY theme
            ORDER BY theme ASC
            LIMIT :limit OFFSET :offset
        ) t ON e.id = t.min_id
        ORDER BY e.theme ASC
    """)
    fun getThemesForChannelAsEntriesFlow(
        channel: String,
        limitDate: Long,
        limit: Int,
        offset: Int
    ): Flow<List<MediaEntry>>

    /**
     * Get one representative MediaEntry per title (specific theme, all channels)
     */
    @Query("""
        SELECT e.* FROM media_entries e
        INNER JOIN (
            SELECT title, MIN(id) as min_id
            FROM media_entries
            WHERE theme = :theme AND timestamp >= :limitDate
            GROUP BY title
        ) t ON e.id = t.min_id
        ORDER BY e.timestamp DESC
    """)
    fun getTitlesForThemeAsEntriesFlow(theme: String, limitDate: Long): Flow<List<MediaEntry>>

    /**
     * Get one representative MediaEntry per title (specific channel and theme)
     */
    @Query("""
        SELECT e.* FROM media_entries e
        INNER JOIN (
            SELECT title, MIN(id) as min_id
            FROM media_entries
            WHERE channel = :channel AND theme = :theme AND timestamp >= :limitDate
            GROUP BY title
        ) t ON e.id = t.min_id
        ORDER BY e.timestamp DESC
    """)
    fun getTitlesForChannelAndThemeAsEntriesFlow(
        channel: String,
        theme: String,
        limitDate: Long
    ): Flow<List<MediaEntry>>
}
