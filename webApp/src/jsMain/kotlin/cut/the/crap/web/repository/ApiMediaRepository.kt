package cut.the.crap.web.repository

import cut.the.crap.web.Broadcaster
import cut.the.crap.web.MediaItem
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlin.js.json

/**
 * API-based implementation of MediaRepository.
 * Connects to the Flask backend API for real media data.
 */
class ApiMediaRepository(
    private val baseUrl: String = getDefaultApiUrl()
) : MediaRepository {

    companion object {
        /**
         * Get the API URL from the environment or use default.
         * For webOS deployment, this should point to the actual server.
         */
        fun getDefaultApiUrl(): String {
            // Check if running on localhost (development)
            val hostname = window.location.hostname
            return when {
                hostname == "localhost" || hostname == "127.0.0.1" ->
                    "http://localhost:5000"
                else ->
                    // For production/webOS, use the configured API server
                    // This can be overridden by setting window.KUCKMAL_API_URL
                    js("window.KUCKMAL_API_URL") as? String ?: "https://api.kuckmal.cutthecrap.link"
            }
        }
    }

    override suspend fun getChannels(): List<Broadcaster> {
        return try {
            val response = fetchJson("$baseUrl/api/channels")
            val data = response["data"] as? Array<*> ?: emptyArray<Any>()

            // Map API channel names to Broadcaster objects
            data.mapNotNull { channelName ->
                val name = channelName as? String ?: return@mapNotNull null
                // Find existing broadcaster or create new one
                Broadcaster.channelList.find { it.name == name }
                    ?: Broadcaster(name = name, abbreviation = name.take(4))
            }
        } catch (e: Exception) {
            console.error("Failed to fetch channels: ${e.message}")
            // Fallback to static list
            Broadcaster.channelList
        }
    }

    override suspend fun getThemes(
        channel: String?,
        page: Int,
        pageSize: Int
    ): PagedResult<String> {
        return try {
            val params = mutableListOf<String>()
            if (channel != null) params.add("channel=$channel")
            params.add("limit=$pageSize")
            params.add("offset=${page * pageSize}")

            val queryString = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
            val response = fetchJson("$baseUrl/api/themes$queryString")

            val data = response["data"] as? Array<*> ?: emptyArray<Any>()
            val total = (response["total"] as? Number)?.toInt() ?: data.size
            val count = (response["count"] as? Number)?.toInt() ?: data.size

            PagedResult(
                items = data.mapNotNull { it as? String },
                totalCount = total,
                page = page,
                pageSize = pageSize,
                hasMore = (page + 1) * pageSize < total
            )
        } catch (e: Exception) {
            console.error("Failed to fetch themes: ${e.message}")
            PagedResult.empty(page, pageSize)
        }
    }

    override suspend fun getTitles(
        channel: String,
        theme: String,
        page: Int,
        pageSize: Int
    ): PagedResult<MediaItem> {
        return try {
            val params = listOf(
                "channel=${encodeURIComponent(channel)}",
                "theme=${encodeURIComponent(theme)}",
                "limit=$pageSize",
                "offset=${page * pageSize}"
            )

            val response = fetchJson("$baseUrl/api/titles?${params.joinToString("&")}")

            val data = response["data"] as? Array<*> ?: emptyArray<Any>()
            val total = (response["total"] as? Number)?.toInt() ?: data.size

            val items = data.mapNotNull { entry ->
                parseMediaEntry(entry)
            }

            PagedResult(
                items = items,
                totalCount = total,
                page = page,
                pageSize = pageSize,
                hasMore = (page + 1) * pageSize < total
            )
        } catch (e: Exception) {
            console.error("Failed to fetch titles: ${e.message}")
            PagedResult.empty(page, pageSize)
        }
    }

    override suspend fun getMediaItem(
        channel: String,
        theme: String,
        title: String
    ): MediaItem? {
        return try {
            val params = listOf(
                "channel=${encodeURIComponent(channel)}",
                "theme=${encodeURIComponent(theme)}",
                "title=${encodeURIComponent(title)}"
            )

            val response = fetchJson("$baseUrl/api/entry?${params.joinToString("&")}")
            val data = response["data"]

            parseMediaEntry(data)
        } catch (e: Exception) {
            console.error("Failed to fetch media item: ${e.message}")
            null
        }
    }

    override suspend fun search(
        query: String,
        channel: String?,
        page: Int,
        pageSize: Int
    ): PagedResult<MediaItem> {
        return try {
            val params = mutableListOf(
                "q=${encodeURIComponent(query)}",
                "limit=$pageSize",
                "offset=${page * pageSize}"
            )
            if (channel != null) {
                params.add("channel=${encodeURIComponent(channel)}")
            }

            val response = fetchJson("$baseUrl/api/search?${params.joinToString("&")}")

            val data = response["data"] as? Array<*> ?: emptyArray<Any>()
            val total = (response["total"] as? Number)?.toInt() ?: data.size

            val items = data.mapNotNull { entry ->
                parseMediaEntry(entry)
            }

            PagedResult(
                items = items,
                totalCount = total,
                page = page,
                pageSize = pageSize,
                hasMore = (page + 1) * pageSize < total
            )
        } catch (e: Exception) {
            console.error("Failed to search: ${e.message}")
            PagedResult.empty(page, pageSize)
        }
    }

    override suspend fun getFilteredItems(
        dateFilter: DateFilter,
        durationFilter: DurationFilter,
        channel: String?,
        page: Int,
        pageSize: Int
    ): PagedResult<MediaItem> {
        // The API doesn't have a direct filter endpoint, so we use /api/titles
        // and filter client-side, or use recent entries endpoint
        return try {
            val params = mutableListOf(
                "limit=$pageSize",
                "offset=${page * pageSize}"
            )
            if (channel != null) {
                params.add("channel=${encodeURIComponent(channel)}")
            }

            // Use the titles endpoint for now (could add minTimestamp for date filtering)
            val response = fetchJson("$baseUrl/api/titles?${params.joinToString("&")}")

            val data = response["data"] as? Array<*> ?: emptyArray<Any>()
            val total = (response["total"] as? Number)?.toInt() ?: data.size

            var items = data.mapNotNull { entry ->
                parseMediaEntry(entry)
            }

            // Apply client-side duration filter
            if (durationFilter != DurationFilter.ALL) {
                items = items.filter { item ->
                    matchesDurationFilter(item, durationFilter)
                }
            }

            PagedResult(
                items = items,
                totalCount = total,
                page = page,
                pageSize = pageSize,
                hasMore = (page + 1) * pageSize < total
            )
        } catch (e: Exception) {
            console.error("Failed to fetch filtered items: ${e.message}")
            PagedResult.empty(page, pageSize)
        }
    }

    /**
     * Parse a media entry from the API response.
     */
    private fun parseMediaEntry(entry: Any?): MediaItem? {
        if (entry == null) return null

        val obj = entry.asDynamic()

        return try {
            MediaItem(
                channel = obj.channel as? String ?: "",
                theme = obj.theme as? String ?: "",
                title = obj.title as? String ?: "",
                date = obj.date as? String ?: "",
                time = obj.time as? String ?: "",
                duration = obj.duration as? String ?: "",
                size = obj.sizeMB as? String ?: "",
                description = obj.description as? String ?: "",
                url = obj.url as? String ?: "",
                hdUrl = obj.hdUrl as? String ?: "",
                geo = obj.geo as? String ?: ""
            )
        } catch (e: Exception) {
            console.error("Failed to parse media entry: ${e.message}")
            null
        }
    }

    /**
     * Check if an item matches the duration filter.
     */
    private fun matchesDurationFilter(item: MediaItem, filter: DurationFilter): Boolean {
        if (filter == DurationFilter.ALL) return true

        val minutes = parseDurationMinutes(item.duration)
        return when (filter) {
            DurationFilter.SHORT -> minutes < 15
            DurationFilter.MEDIUM -> minutes in 15..60
            DurationFilter.LONG -> minutes > 60
            DurationFilter.ALL -> true
        }
    }

    /**
     * Parse duration string to minutes.
     */
    private fun parseDurationMinutes(duration: String): Int {
        // Parse formats like "00:45:30", "45 Min", "1h 30min"
        val colonMatch = Regex("(\\d+):(\\d+):(\\d+)").find(duration)
        if (colonMatch != null) {
            val hours = colonMatch.groupValues[1].toIntOrNull() ?: 0
            val minutes = colonMatch.groupValues[2].toIntOrNull() ?: 0
            return hours * 60 + minutes
        }

        val hourMatch = Regex("(\\d+)\\s*[hH]").find(duration)
        val minMatch = Regex("(\\d+)\\s*[mM]").find(duration)

        val hours = hourMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        val minutes = minMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0

        return hours * 60 + minutes
    }

    /**
     * Fetch JSON from a URL using the Fetch API.
     */
    private suspend fun fetchJson(url: String): dynamic {
        val response = window.fetch(url).await()

        if (!response.ok) {
            throw Exception("HTTP ${response.status}: ${response.statusText}")
        }

        return response.json().await()
    }
}

/**
 * URL encode a string for use in query parameters.
 */
private fun encodeURIComponent(str: String): String {
    return js("encodeURIComponent(str)") as String
}

/**
 * Console logging for debugging.
 */
private external object console {
    fun log(vararg args: Any?)
    fun error(vararg args: Any?)
}
