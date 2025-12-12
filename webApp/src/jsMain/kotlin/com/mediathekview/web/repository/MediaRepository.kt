package com.mediathekview.web.repository

import com.mediathekview.web.Broadcaster
import com.mediathekview.web.MediaItem

/**
 * Paged response wrapper for list results
 */
data class PagedResult<T>(
    val items: List<T>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean
) {
    companion object {
        fun <T> empty(page: Int = 0, pageSize: Int = 50): PagedResult<T> = PagedResult(
            items = emptyList(),
            totalCount = 0,
            page = page,
            pageSize = pageSize,
            hasMore = false
        )
    }
}

/**
 * Duration filter options
 */
enum class DurationFilter {
    ALL,
    SHORT,      // < 15 min
    MEDIUM,     // 15-60 min
    LONG        // > 60 min
}

/**
 * Date filter options
 */
enum class DateFilter {
    ALL,
    TODAY,
    LAST_WEEK,
    LAST_MONTH
}

/**
 * Repository interface for media data access.
 * Abstracts data source (mock/API) from UI layer.
 */
interface MediaRepository {

    /**
     * Get list of all available channels/broadcasters
     */
    suspend fun getChannels(): List<Broadcaster>

    /**
     * Get themes/shows, optionally filtered by channel
     * @param channel Filter by channel name, or null for all channels
     * @param page Page number (0-indexed)
     * @param pageSize Items per page
     */
    suspend fun getThemes(
        channel: String? = null,
        page: Int = 0,
        pageSize: Int = 50
    ): PagedResult<String>

    /**
     * Get media items for a specific channel and theme
     * @param channel Channel name
     * @param theme Theme/show name
     * @param page Page number (0-indexed)
     * @param pageSize Items per page
     */
    suspend fun getTitles(
        channel: String,
        theme: String,
        page: Int = 0,
        pageSize: Int = 50
    ): PagedResult<MediaItem>

    /**
     * Get a single media item by channel, theme, and title
     */
    suspend fun getMediaItem(
        channel: String,
        theme: String,
        title: String
    ): MediaItem?

    /**
     * Search media items across all fields
     * @param query Search query
     * @param channel Optional channel filter
     * @param page Page number (0-indexed)
     * @param pageSize Items per page
     */
    suspend fun search(
        query: String,
        channel: String? = null,
        page: Int = 0,
        pageSize: Int = 50
    ): PagedResult<MediaItem>

    /**
     * Get recent items within a date range
     * @param dateFilter Date filter option
     * @param durationFilter Duration filter option
     * @param channel Optional channel filter
     * @param page Page number (0-indexed)
     * @param pageSize Items per page
     */
    suspend fun getFilteredItems(
        dateFilter: DateFilter = DateFilter.ALL,
        durationFilter: DurationFilter = DurationFilter.ALL,
        channel: String? = null,
        page: Int = 0,
        pageSize: Int = 50
    ): PagedResult<MediaItem>
}
