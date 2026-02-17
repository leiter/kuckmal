package cut.the.crap.shared.repository

import cut.the.crap.shared.cache.TvosCache
import cut.the.crap.shared.cache.createCacheKey
import cut.the.crap.shared.data.HttpClientFactory
import cut.the.crap.shared.database.MediaEntry
import cut.the.crap.shared.sync.SyncStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.URLBuilder
import io.ktor.http.encodedPath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * tvOS API implementation of MediaRepository.
 * Connects to the Flask backend API for real media data.
 * Includes caching for offline support.
 */
class TvosApiMediaRepository(
    private val baseUrl: String = DEFAULT_API_URL
) : MediaRepository {

    companion object {
        // Production API URL
        const val DEFAULT_API_URL = "https://api.kuckmal.cutthecrap.link"

        // Cache TTLs
        private const val CHANNELS_TTL_MS = 60 * 60 * 1000L  // 1 hour
        private const val THEMES_TTL_MS = 15 * 60 * 1000L    // 15 minutes
        private const val ENTRIES_TTL_MS = 5 * 60 * 1000L    // 5 minutes
    }

    private val httpClient: HttpClient = HttpClientFactory.create()
    private val json = Json { ignoreUnknownKeys = true }

    // Response caches
    private val channelsCache = TvosCache<List<String>>(defaultTtlMs = CHANNELS_TTL_MS, maxEntries = 10)
    private val themesCache = TvosCache<List<String>>(defaultTtlMs = THEMES_TTL_MS, maxEntries = 50)
    private val entriesCache = TvosCache<List<MediaEntry>>(defaultTtlMs = ENTRIES_TTL_MS, maxEntries = 100)
    private val singleEntryCache = TvosCache<MediaEntry>(defaultTtlMs = ENTRIES_TTL_MS, maxEntries = 200)

    // Sync status tracking
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    // Cache for channels (they don't change often)
    private var cachedChannels: List<String>? = null

    /**
     * Get current time in milliseconds
     */
    private fun currentTimeMs(): Long =
        (NSDate().timeIntervalSince1970 * 1000).toLong()

    // MARK: - API Response DTOs

    @Serializable
    private data class ChannelsResponse(
        val data: List<String> = emptyList(),
        val count: Int = 0
    )

    @Serializable
    private data class ThemesResponse(
        val data: List<String> = emptyList(),
        val total: Int = 0,
        val count: Int = 0
    )

    @Serializable
    private data class TitlesResponse(
        val data: List<ApiMediaEntry> = emptyList(),
        val total: Int = 0,
        val count: Int = 0
    )

    @Serializable
    private data class EntryResponse(
        val data: ApiMediaEntry? = null
    )

    @Serializable
    private data class SearchResponse(
        val data: List<ApiMediaEntry> = emptyList(),
        val total: Int = 0,
        val count: Int = 0
    )

    @Serializable
    private data class ApiMediaEntry(
        val channel: String = "",
        val theme: String = "",
        val title: String = "",
        val date: String = "",
        val time: String = "",
        val duration: String = "",
        @SerialName("sizeMB") val sizeMB: String = "",
        val description: String = "",
        val url: String = "",
        val website: String = "",
        @SerialName("subtitleUrl") val subtitleUrl: String = "",
        @SerialName("smallUrl") val smallUrl: String = "",
        @SerialName("hdUrl") val hdUrl: String = "",
        val timestamp: Long = 0,
        val geo: String = "",
        @SerialName("isNew") val isNew: Boolean = false
    )

    private fun ApiMediaEntry.toMediaEntry(id: Long = 0): MediaEntry {
        return MediaEntry(
            id = id,
            channel = channel,
            theme = theme,
            title = title,
            date = date,
            time = time,
            duration = duration,
            sizeMB = sizeMB,
            description = description,
            url = url,
            website = website,
            subtitleUrl = subtitleUrl,
            smallUrl = smallUrl,
            hdUrl = hdUrl,
            timestamp = timestamp,
            geo = geo,
            isNew = isNew
        )
    }

    // MARK: - MediaRepository Implementation

    override fun getAllChannelsFlow(): Flow<List<String>> = flow {
        val cacheKey = "channels"

        // Try cache first
        channelsCache.get(cacheKey)?.let { cached ->
            emit(cached)
            return@flow
        }

        try {
            _syncStatus.value = SyncStatus.Syncing
            val response: ChannelsResponse = httpClient.get("$baseUrl/api/channels").body()
            cachedChannels = response.data
            channelsCache.put(cacheKey, response.data)
            _syncStatus.value = SyncStatus.Synced(currentTimeMs())
            emit(response.data)
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getAllChannelsFlow error: ${e.message}")
            _syncStatus.value = SyncStatus.Error(e.message ?: "Unknown error")
            // Try stale cache as fallback
            val stale = channelsCache.getStale(cacheKey) ?: cachedChannels ?: emptyList()
            emit(stale)
        }
    }

    override suspend fun getAllChannels(): List<String> {
        val cacheKey = "channels"

        // Try cache first
        channelsCache.get(cacheKey)?.let { return it }

        return try {
            _syncStatus.value = SyncStatus.Syncing
            val response: ChannelsResponse = httpClient.get("$baseUrl/api/channels").body()
            cachedChannels = response.data
            channelsCache.put(cacheKey, response.data)
            _syncStatus.value = SyncStatus.Synced(currentTimeMs())
            response.data
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getAllChannels error: ${e.message}")
            _syncStatus.value = SyncStatus.Error(e.message ?: "Unknown error")
            // Try stale cache as fallback
            channelsCache.getStale(cacheKey) ?: cachedChannels ?: emptyList()
        }
    }

    override fun getAllThemesFlow(minTimestamp: Long, limit: Int, offset: Int): Flow<List<String>> = flow {
        val cacheKey = createCacheKey("themes", mapOf(
            "minTimestamp" to minTimestamp,
            "limit" to limit,
            "offset" to offset
        ))

        // Try cache first
        themesCache.get(cacheKey)?.let { cached ->
            emit(cached)
            return@flow
        }

        try {
            _syncStatus.value = SyncStatus.Syncing
            val response: ThemesResponse = httpClient.get("$baseUrl/api/themes") {
                parameter("limit", limit)
                parameter("offset", offset)
                if (minTimestamp > 0) parameter("minTimestamp", minTimestamp)
            }.body()
            themesCache.put(cacheKey, response.data)
            _syncStatus.value = SyncStatus.Synced(currentTimeMs())
            emit(response.data)
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getAllThemesFlow error: ${e.message}")
            _syncStatus.value = SyncStatus.Error(e.message ?: "Unknown error")
            // Try stale cache as fallback
            emit(themesCache.getStale(cacheKey) ?: emptyList())
        }
    }

    override suspend fun getAllThemes(minTimestamp: Long, limit: Int, offset: Int): List<String> {
        val cacheKey = createCacheKey("themes", mapOf(
            "minTimestamp" to minTimestamp,
            "limit" to limit,
            "offset" to offset
        ))

        // Try cache first
        themesCache.get(cacheKey)?.let { return it }

        return try {
            val response: ThemesResponse = httpClient.get("$baseUrl/api/themes") {
                parameter("limit", limit)
                parameter("offset", offset)
                if (minTimestamp > 0) parameter("minTimestamp", minTimestamp)
            }.body()
            themesCache.put(cacheKey, response.data)
            response.data
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getAllThemes error: ${e.message}")
            themesCache.getStale(cacheKey) ?: emptyList()
        }
    }

    override fun getThemesForChannelFlow(
        channel: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): Flow<List<String>> = flow {
        val cacheKey = createCacheKey("themes/$channel", mapOf(
            "minTimestamp" to minTimestamp,
            "limit" to limit,
            "offset" to offset
        ))

        // Try cache first
        themesCache.get(cacheKey)?.let { cached ->
            emit(cached)
            return@flow
        }

        try {
            val response: ThemesResponse = httpClient.get("$baseUrl/api/themes") {
                parameter("channel", channel)
                parameter("limit", limit)
                parameter("offset", offset)
                if (minTimestamp > 0) parameter("minTimestamp", minTimestamp)
            }.body()
            themesCache.put(cacheKey, response.data)
            emit(response.data)
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getThemesForChannelFlow error: ${e.message}")
            emit(themesCache.getStale(cacheKey) ?: emptyList())
        }
    }

    override suspend fun getThemesForChannel(
        channel: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): List<String> {
        val cacheKey = createCacheKey("themes/$channel", mapOf(
            "minTimestamp" to minTimestamp,
            "limit" to limit,
            "offset" to offset
        ))

        // Try cache first
        themesCache.get(cacheKey)?.let { return it }

        return try {
            val response: ThemesResponse = httpClient.get("$baseUrl/api/themes") {
                parameter("channel", channel)
                parameter("limit", limit)
                parameter("offset", offset)
                if (minTimestamp > 0) parameter("minTimestamp", minTimestamp)
            }.body()
            themesCache.put(cacheKey, response.data)
            response.data
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getThemesForChannel error: ${e.message}")
            themesCache.getStale(cacheKey) ?: emptyList()
        }
    }

    override fun getTitlesForThemeFlow(theme: String, minTimestamp: Long): Flow<List<String>> = flow {
        try {
            val response: TitlesResponse = httpClient.get("$baseUrl/api/titles") {
                parameter("theme", theme)
                if (minTimestamp > 0) parameter("minTimestamp", minTimestamp)
            }.body()
            emit(response.data.map { it.title })
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getTitlesForThemeFlow error: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun getTitlesForTheme(theme: String, minTimestamp: Long): List<String> {
        return try {
            val response: TitlesResponse = httpClient.get("$baseUrl/api/titles") {
                parameter("theme", theme)
                if (minTimestamp > 0) parameter("minTimestamp", minTimestamp)
            }.body()
            response.data.map { it.title }
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getTitlesForTheme error: ${e.message}")
            emptyList()
        }
    }

    override fun getTitlesForChannelAndThemeFlow(
        channel: String,
        theme: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): Flow<List<String>> = flow {
        try {
            val response: TitlesResponse = httpClient.get("$baseUrl/api/titles") {
                parameter("channel", channel)
                parameter("theme", theme)
                parameter("limit", limit)
                parameter("offset", offset)
                if (minTimestamp > 0) parameter("minTimestamp", minTimestamp)
            }.body()
            emit(response.data.map { it.title })
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getTitlesForChannelAndThemeFlow error: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun getTitlesForChannelAndTheme(
        channel: String,
        theme: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): List<String> {
        return try {
            val response: TitlesResponse = httpClient.get("$baseUrl/api/titles") {
                parameter("channel", channel)
                parameter("theme", theme)
                parameter("limit", limit)
                parameter("offset", offset)
                if (minTimestamp > 0) parameter("minTimestamp", minTimestamp)
            }.body()
            response.data.map { it.title }
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getTitlesForChannelAndTheme error: ${e.message}")
            emptyList()
        }
    }

    override fun getMediaEntryFlow(channel: String, theme: String, title: String): Flow<MediaEntry?> = flow {
        try {
            val response: EntryResponse = httpClient.get("$baseUrl/api/entry") {
                parameter("channel", channel)
                parameter("theme", theme)
                parameter("title", title)
            }.body()
            emit(response.data?.toMediaEntry())
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getMediaEntryFlow error: ${e.message}")
            emit(null)
        }
    }

    override fun getMediaEntryByThemeAndTitleFlow(theme: String, title: String): Flow<MediaEntry?> = flow {
        try {
            val response: EntryResponse = httpClient.get("$baseUrl/api/entry") {
                parameter("theme", theme)
                parameter("title", title)
            }.body()
            emit(response.data?.toMediaEntry())
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getMediaEntryByThemeAndTitleFlow error: ${e.message}")
            emit(null)
        }
    }

    override fun getMediaEntryByTitleFlow(title: String): Flow<MediaEntry?> = flow {
        // Search for entry by title and return first match
        try {
            val response: SearchResponse = httpClient.get("$baseUrl/api/search") {
                parameter("q", title)
                parameter("limit", 1)
            }.body()
            emit(response.data.firstOrNull()?.toMediaEntry())
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getMediaEntryByTitleFlow error: ${e.message}")
            emit(null)
        }
    }

    override fun getAllThemesAsEntriesFlow(minTimestamp: Long, limit: Int, offset: Int): Flow<List<MediaEntry>> = flow {
        try {
            val response: TitlesResponse = httpClient.get("$baseUrl/api/titles") {
                parameter("limit", limit)
                parameter("offset", offset)
                if (minTimestamp > 0) parameter("minTimestamp", minTimestamp)
            }.body()
            // Group by theme and take one entry per theme
            val entriesByTheme = response.data.groupBy { it.theme }
            emit(entriesByTheme.values.mapNotNull { it.firstOrNull()?.toMediaEntry() })
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getAllThemesAsEntriesFlow error: ${e.message}")
            emit(emptyList())
        }
    }

    override fun getThemesForChannelAsEntriesFlow(
        channel: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): Flow<List<MediaEntry>> = flow {
        try {
            val response: TitlesResponse = httpClient.get("$baseUrl/api/titles") {
                parameter("channel", channel)
                parameter("limit", limit)
                parameter("offset", offset)
                if (minTimestamp > 0) parameter("minTimestamp", minTimestamp)
            }.body()
            // Group by theme and take one entry per theme
            val entriesByTheme = response.data.groupBy { it.theme }
            emit(entriesByTheme.values.mapNotNull { it.firstOrNull()?.toMediaEntry() })
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getThemesForChannelAsEntriesFlow error: ${e.message}")
            emit(emptyList())
        }
    }

    override fun getTitlesForThemeAsEntriesFlow(theme: String, minTimestamp: Long): Flow<List<MediaEntry>> = flow {
        try {
            val response: TitlesResponse = httpClient.get("$baseUrl/api/titles") {
                parameter("theme", theme)
                if (minTimestamp > 0) parameter("minTimestamp", minTimestamp)
            }.body()
            emit(response.data.mapIndexed { index, entry -> entry.toMediaEntry(index.toLong()) })
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getTitlesForThemeAsEntriesFlow error: ${e.message}")
            emit(emptyList())
        }
    }

    override fun getTitlesForChannelAndThemeAsEntriesFlow(
        channel: String,
        theme: String,
        minTimestamp: Long
    ): Flow<List<MediaEntry>> = flow {
        try {
            val response: TitlesResponse = httpClient.get("$baseUrl/api/titles") {
                parameter("channel", channel)
                parameter("theme", theme)
                if (minTimestamp > 0) parameter("minTimestamp", minTimestamp)
            }.body()
            emit(response.data.mapIndexed { index, entry -> entry.toMediaEntry(index.toLong()) })
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getTitlesForChannelAndThemeAsEntriesFlow error: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun getRecentEntries(minTimestamp: Long, limit: Int): List<MediaEntry> {
        return try {
            val response: TitlesResponse = httpClient.get("$baseUrl/api/titles") {
                parameter("limit", limit)
                if (minTimestamp > 0) parameter("minTimestamp", minTimestamp)
            }.body()
            response.data.mapIndexed { index, entry -> entry.toMediaEntry(index.toLong()) }
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getRecentEntries error: ${e.message}")
            emptyList()
        }
    }

    override suspend fun searchEntries(query: String, limit: Int): List<MediaEntry> {
        return try {
            val response: SearchResponse = httpClient.get("$baseUrl/api/search") {
                parameter("q", query)
                parameter("limit", limit)
            }.body()
            response.data.mapIndexed { index, entry -> entry.toMediaEntry(index.toLong()) }
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] searchEntries error: ${e.message}")
            emptyList()
        }
    }

    override suspend fun searchEntriesByChannel(channel: String, query: String, limit: Int): List<MediaEntry> {
        return try {
            val response: SearchResponse = httpClient.get("$baseUrl/api/search") {
                parameter("q", query)
                parameter("channel", channel)
                parameter("limit", limit)
            }.body()
            response.data.mapIndexed { index, entry -> entry.toMediaEntry(index.toLong()) }
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] searchEntriesByChannel error: ${e.message}")
            emptyList()
        }
    }

    override suspend fun searchEntriesByTheme(theme: String, query: String, limit: Int): List<MediaEntry> {
        return try {
            val response: SearchResponse = httpClient.get("$baseUrl/api/search") {
                parameter("q", query)
                parameter("theme", theme)
                parameter("limit", limit)
            }.body()
            response.data.mapIndexed { index, entry -> entry.toMediaEntry(index.toLong()) }
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] searchEntriesByTheme error: ${e.message}")
            emptyList()
        }
    }

    override suspend fun searchEntriesByChannelAndTheme(
        channel: String,
        theme: String,
        query: String,
        limit: Int
    ): List<MediaEntry> {
        return try {
            val response: SearchResponse = httpClient.get("$baseUrl/api/search") {
                parameter("q", query)
                parameter("channel", channel)
                parameter("theme", theme)
                parameter("limit", limit)
            }.body()
            response.data.mapIndexed { index, entry -> entry.toMediaEntry(index.toLong()) }
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] searchEntriesByChannelAndTheme error: ${e.message}")
            emptyList()
        }
    }

    override suspend fun searchEntriesWithOffset(query: String, limit: Int, offset: Int): List<MediaEntry> {
        return try {
            val response: SearchResponse = httpClient.get("$baseUrl/api/search") {
                parameter("q", query)
                parameter("limit", limit)
                parameter("offset", offset)
            }.body()
            response.data.mapIndexed { index, entry -> entry.toMediaEntry((offset + index).toLong()) }
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] searchEntriesWithOffset error: ${e.message}")
            emptyList()
        }
    }

    override suspend fun searchEntriesByChannelWithOffset(
        channel: String,
        query: String,
        limit: Int,
        offset: Int
    ): List<MediaEntry> {
        return try {
            val response: SearchResponse = httpClient.get("$baseUrl/api/search") {
                parameter("q", query)
                parameter("channel", channel)
                parameter("limit", limit)
                parameter("offset", offset)
            }.body()
            response.data.mapIndexed { index, entry -> entry.toMediaEntry((offset + index).toLong()) }
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] searchEntriesByChannelWithOffset error: ${e.message}")
            emptyList()
        }
    }

    override suspend fun searchEntriesByThemeWithOffset(
        theme: String,
        query: String,
        limit: Int,
        offset: Int
    ): List<MediaEntry> {
        return try {
            val response: SearchResponse = httpClient.get("$baseUrl/api/search") {
                parameter("q", query)
                parameter("theme", theme)
                parameter("limit", limit)
                parameter("offset", offset)
            }.body()
            response.data.mapIndexed { index, entry -> entry.toMediaEntry((offset + index).toLong()) }
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] searchEntriesByThemeWithOffset error: ${e.message}")
            emptyList()
        }
    }

    override suspend fun searchEntriesByChannelAndThemeWithOffset(
        channel: String,
        theme: String,
        query: String,
        limit: Int,
        offset: Int
    ): List<MediaEntry> {
        return try {
            val response: SearchResponse = httpClient.get("$baseUrl/api/search") {
                parameter("q", query)
                parameter("channel", channel)
                parameter("theme", theme)
                parameter("limit", limit)
                parameter("offset", offset)
            }.body()
            response.data.mapIndexed { index, entry -> entry.toMediaEntry((offset + index).toLong()) }
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] searchEntriesByChannelAndThemeWithOffset error: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getCount(): Int {
        // API doesn't have a count endpoint, return 0
        return 0
    }

    override suspend fun insert(entry: MediaEntry): Long {
        // API is read-only, no insert support
        return entry.id
    }

    override suspend fun insertAll(entries: List<MediaEntry>) {
        // API is read-only, no insert support
    }

    override suspend fun deleteAll() {
        // API is read-only, no delete support
    }

    override fun loadMediaListFromFile(filePath: String): Flow<MediaRepository.LoadingResult> = flow {
        // tvOS uses API, no file loading needed
        emit(MediaRepository.LoadingResult.Complete(0))
    }

    override fun applyDiffToDatabase(filePath: String): Flow<MediaRepository.LoadingResult> = flow {
        // tvOS uses API, no diff application needed
        emit(MediaRepository.LoadingResult.Complete(0))
    }

    override suspend fun checkAndLoadMediaList(privatePath: String): Boolean {
        // API data is always available (if server is reachable)
        return true
    }

    override fun getEntriesFlow(channel: String, theme: String): Flow<List<MediaEntry>> = flow {
        val cacheKey = createCacheKey("entries", mapOf(
            "channel" to channel.takeIf { it.isNotEmpty() },
            "theme" to theme.takeIf { it.isNotEmpty() }
        ))

        // Try cache first
        entriesCache.get(cacheKey)?.let { cached ->
            emit(cached)
            return@flow
        }

        try {
            val response: TitlesResponse = httpClient.get("$baseUrl/api/titles") {
                if (channel.isNotEmpty()) parameter("channel", channel)
                if (theme.isNotEmpty()) parameter("theme", theme)
            }.body()
            val entries = response.data.mapIndexed { index, entry -> entry.toMediaEntry(index.toLong()) }
            entriesCache.put(cacheKey, entries)
            emit(entries)
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getEntriesFlow error: ${e.message}")
            emit(entriesCache.getStale(cacheKey) ?: emptyList())
        }
    }

    // =========================================================================
    // Cache Management
    // =========================================================================

    /**
     * Clear all caches. Call this when you want to force a refresh.
     */
    suspend fun clearAllCaches() {
        channelsCache.clear()
        themesCache.clear()
        entriesCache.clear()
        singleEntryCache.clear()
        cachedChannels = null
        _syncStatus.value = SyncStatus.Idle
        println("[TvosApiMediaRepository] All caches cleared")
    }

    /**
     * Evict expired entries from all caches.
     * Call this periodically or on app resume.
     */
    suspend fun evictExpiredCaches() {
        channelsCache.evictExpired()
        themesCache.evictExpired()
        entriesCache.evictExpired()
        singleEntryCache.evictExpired()
        println("[TvosApiMediaRepository] Expired cache entries evicted")
    }

    /**
     * Get aggregated cache statistics
     */
    suspend fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "channels" to channelsCache.getStats(),
            "themes" to themesCache.getStats(),
            "entries" to entriesCache.getStats(),
            "singleEntry" to singleEntryCache.getStats()
        )
    }

    /**
     * Check if we have any cached data available (for offline mode)
     */
    suspend fun hasCachedData(): Boolean {
        return channelsCache.size() > 0 || themesCache.size() > 0 || entriesCache.size() > 0
    }
}
