package cut.the.crap.shared.repository

import cut.the.crap.shared.database.FavoriteEntry
import cut.the.crap.shared.database.HistoryEntry
import cut.the.crap.shared.database.MediaEntry
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for media data access
 * Provides an abstraction layer between data sources (Room, parser, network) and ViewModels
 */
interface MediaRepository {

    /**
     * Sealed class representing loading states for media list file parsing
     */
    sealed class LoadingResult {
        data class Progress(val entriesLoaded: Int) : LoadingResult()
        data class Complete(val totalEntries: Int) : LoadingResult()
        data class Error(val exception: Exception, val entriesParsed: Int) : LoadingResult()
    }

    /**
     * Get all unique channels sorted alphabetically as Flow
     */
    fun getAllChannelsFlow(): Flow<List<String>>

    /**
     * Get all unique themes with optional date filter as Flow
     * @param minTimestamp Filter entries newer than this timestamp (0 = no filter)
     * @param limit Maximum number of themes to return
     * @param offset Pagination offset
     */
    fun getAllThemesFlow(minTimestamp: Long = 0, limit: Int = 1200, offset: Int = 0): Flow<List<String>>

    /**
     * Get themes for a specific channel as Flow
     * @param channel The channel name
     * @param minTimestamp Filter entries newer than this timestamp
     * @param limit Maximum number of themes to return
     * @param offset Pagination offset
     */
    fun getThemesForChannelFlow(
        channel: String,
        minTimestamp: Long = 0,
        limit: Int = 1200,
        offset: Int = 0
    ): Flow<List<String>>

    /**
     * Get titles for a specific theme across all channels as Flow
     */
    fun getTitlesForThemeFlow(
        theme: String,
        minTimestamp: Long = 0
    ): Flow<List<String>>

    /**
     * Get titles for a specific channel and theme as Flow
     */
    fun getTitlesForChannelAndThemeFlow(
        channel: String,
        theme: String,
        minTimestamp: Long = 0,
        limit: Int = 1200,
        offset: Int = 0
    ): Flow<List<String>>

    /**
     * Get a specific media entry as Flow
     */
    fun getMediaEntryFlow(channel: String, theme: String, title: String): Flow<MediaEntry?>

    /**
     * Get a specific media entry by theme and title only (when channel not selected)
     */
    fun getMediaEntryByThemeAndTitleFlow(theme: String, title: String): Flow<MediaEntry?>

    /**
     * Get a specific media entry by title only (for search results navigation)
     * Used when navigating from search results where the entry could be from any theme
     */
    fun getMediaEntryByTitleFlow(title: String): Flow<MediaEntry?>

    /**
     * Get recent entries (within a certain timestamp)
     */
    suspend fun getRecentEntries(minTimestamp: Long, limit: Int): List<MediaEntry>

    /**
     * Search entries by query string
     */
    suspend fun searchEntries(query: String, limit: Int): List<MediaEntry>

    /**
     * Search entries by query string within a specific channel
     */
    suspend fun searchEntriesByChannel(channel: String, query: String, limit: Int): List<MediaEntry>

    /**
     * Search entries by query string within a specific theme (across all channels)
     */
    suspend fun searchEntriesByTheme(theme: String, query: String, limit: Int): List<MediaEntry>

    /**
     * Search entries by query string within a specific channel and theme
     */
    suspend fun searchEntriesByChannelAndTheme(channel: String, theme: String, query: String, limit: Int): List<MediaEntry>

    /**
     * Search entries with offset support for incremental loading
     */
    suspend fun searchEntriesWithOffset(query: String, limit: Int, offset: Int): List<MediaEntry>

    /**
     * Search entries by channel with offset support
     */
    suspend fun searchEntriesByChannelWithOffset(channel: String, query: String, limit: Int, offset: Int): List<MediaEntry>

    /**
     * Search entries by theme with offset support
     */
    suspend fun searchEntriesByThemeWithOffset(theme: String, query: String, limit: Int, offset: Int): List<MediaEntry>

    /**
     * Search entries by channel and theme with offset support
     */
    suspend fun searchEntriesByChannelAndThemeWithOffset(channel: String, theme: String, query: String, limit: Int, offset: Int): List<MediaEntry>

    /**
     * Get total count of entries
     */
    suspend fun getCount(): Int

    /**
     * Insert a single entry
     */
    suspend fun insert(entry: MediaEntry): Long

    /**
     * Insert multiple entries (bulk insert)
     */
    suspend fun insertAll(entries: List<MediaEntry>)

    /**
     * Delete all entries
     */
    suspend fun deleteAll()

    /**
     * Load media list from file and populate database
     * Emits progress updates as a Flow
     * @param filePath Path to the media list file
     * @return Flow of LoadingResult (Progress, Complete, or Error)
     */
    fun loadMediaListFromFile(filePath: String): Flow<LoadingResult>

    /**
     * Apply diff file to existing database (incremental update)
     * Emits progress updates as a Flow
     * @param filePath Path to the diff file
     * @return Flow of LoadingResult (Progress, Complete, or Error)
     */
    fun applyDiffToDatabase(filePath: String): Flow<LoadingResult>

    /**
     * Check if media list exists and load it
     * @param privatePath Path to private storage directory
     * @return True if media list was found and loaded
     */
    suspend fun checkAndLoadMediaList(privatePath: String): Boolean

    /**
     * Get entries as Flow for reactive updates
     */
    fun getEntriesFlow(channel: String = "", theme: String = ""): Flow<List<MediaEntry>>

    /**
     * Get one representative MediaEntry per theme (all channels)
     * Returns full MediaEntry objects instead of just theme strings
     */
    fun getAllThemesAsEntriesFlow(minTimestamp: Long = 0, limit: Int = 1200, offset: Int = 0): Flow<List<MediaEntry>>

    /**
     * Get one representative MediaEntry per theme (specific channel)
     * Returns full MediaEntry objects instead of just theme strings
     */
    fun getThemesForChannelAsEntriesFlow(
        channel: String,
        minTimestamp: Long = 0,
        limit: Int = 1200,
        offset: Int = 0
    ): Flow<List<MediaEntry>>

    /**
     * Get one representative MediaEntry per title (specific theme, all channels)
     * Returns full MediaEntry objects instead of just title strings
     */
    fun getTitlesForThemeAsEntriesFlow(
        theme: String,
        minTimestamp: Long = 0
    ): Flow<List<MediaEntry>>

    /**
     * Get one representative MediaEntry per title (specific channel and theme)
     * Returns full MediaEntry objects instead of just title strings
     */
    fun getTitlesForChannelAndThemeAsEntriesFlow(
        channel: String,
        theme: String,
        minTimestamp: Long = 0
    ): Flow<List<MediaEntry>>

    // =========================================================================
    // Favorites / Watch Later
    // Default implementations provided to avoid breaking existing code.
    // Platform-specific implementations should override these methods.
    // =========================================================================

    /**
     * Get all favorites of a specific type as Flow (reactive)
     * @param listType "favorite" or "watchLater"
     */
    fun getFavoritesFlow(listType: String = "favorite"): Flow<List<FavoriteEntry>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    /**
     * Add an entry to favorites
     * @param listType "favorite" or "watchLater"
     */
    suspend fun addToFavorites(
        channel: String,
        theme: String,
        title: String,
        listType: String = "favorite"
    ) {
        // Default no-op implementation
    }

    /**
     * Remove an entry from favorites (any type)
     */
    suspend fun removeFromFavorites(channel: String, theme: String, title: String) {
        // Default no-op implementation
    }

    /**
     * Check if an entry is in favorites (any type) as Flow (reactive)
     */
    fun isFavoriteFlow(channel: String, theme: String, title: String): Flow<Boolean> =
        kotlinx.coroutines.flow.flowOf(false)

    /**
     * Check if an entry is in a specific list type as Flow (reactive)
     */
    fun isFavoriteByTypeFlow(
        channel: String,
        theme: String,
        title: String,
        listType: String
    ): Flow<Boolean> = kotlinx.coroutines.flow.flowOf(false)

    // =========================================================================
    // Playback History
    // Default implementations provided to avoid breaking existing code.
    // Platform-specific implementations should override these methods.
    // =========================================================================

    /**
     * Get continue watching entries (not completed) as Flow (reactive)
     */
    fun getContinueWatchingFlow(limit: Int = 20): Flow<List<HistoryEntry>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    /**
     * Get full playback history as Flow (reactive)
     */
    fun getHistoryFlow(limit: Int = 50): Flow<List<HistoryEntry>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    /**
     * Record playback progress for an entry
     * Automatically marks as completed when >90% watched
     */
    suspend fun recordPlaybackProgress(
        channel: String,
        theme: String,
        title: String,
        positionSeconds: Long,
        durationSeconds: Long
    ) {
        // Default no-op implementation
    }

    /**
     * Get resume position for a specific entry (in seconds)
     * Returns null if no history exists
     */
    suspend fun getResumePosition(channel: String, theme: String, title: String): Long? = null

    /**
     * Get history entry for a specific media item as Flow (reactive)
     */
    fun getHistoryEntryFlow(channel: String, theme: String, title: String): Flow<HistoryEntry?> =
        kotlinx.coroutines.flow.flowOf(null)

    /**
     * Clear all playback history
     */
    suspend fun clearHistory() {
        // Default no-op implementation
    }
}
