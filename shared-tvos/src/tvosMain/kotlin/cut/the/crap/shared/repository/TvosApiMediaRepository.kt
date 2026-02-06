package cut.the.crap.shared.repository

import cut.the.crap.shared.data.HttpClientFactory
import cut.the.crap.shared.database.MediaEntry
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.URLBuilder
import io.ktor.http.encodedPath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * tvOS API implementation of MediaRepository.
 * Connects to the Flask backend API for real media data.
 */
class TvosApiMediaRepository(
    private val baseUrl: String = DEFAULT_API_URL
) : MediaRepository {

    companion object {
        // Default API URL - can be overridden for testing or different environments
        const val DEFAULT_API_URL = "http://localhost:5000"
    }

    private val httpClient: HttpClient = HttpClientFactory.create()

    // Cache for channels (they don't change often)
    private var cachedChannels: List<String>? = null

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
        try {
            val response: ChannelsResponse = httpClient.get("$baseUrl/api/channels").body()
            cachedChannels = response.data
            emit(response.data)
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getAllChannelsFlow error: ${e.message}")
            emit(cachedChannels ?: emptyList())
        }
    }

    override suspend fun getAllChannels(): List<String> {
        return try {
            val response: ChannelsResponse = httpClient.get("$baseUrl/api/channels").body()
            cachedChannels = response.data
            response.data
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getAllChannels error: ${e.message}")
            cachedChannels ?: emptyList()
        }
    }

    override fun getAllThemesFlow(minTimestamp: Long, limit: Int, offset: Int): Flow<List<String>> = flow {
        try {
            val response: ThemesResponse = httpClient.get("$baseUrl/api/themes") {
                parameter("limit", limit)
                parameter("offset", offset)
                if (minTimestamp > 0) parameter("minTimestamp", minTimestamp)
            }.body()
            emit(response.data)
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getAllThemesFlow error: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun getAllThemes(minTimestamp: Long, limit: Int, offset: Int): List<String> {
        return try {
            val response: ThemesResponse = httpClient.get("$baseUrl/api/themes") {
                parameter("limit", limit)
                parameter("offset", offset)
                if (minTimestamp > 0) parameter("minTimestamp", minTimestamp)
            }.body()
            response.data
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getAllThemes error: ${e.message}")
            emptyList()
        }
    }

    override fun getThemesForChannelFlow(
        channel: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): Flow<List<String>> = flow {
        try {
            val response: ThemesResponse = httpClient.get("$baseUrl/api/themes") {
                parameter("channel", channel)
                parameter("limit", limit)
                parameter("offset", offset)
                if (minTimestamp > 0) parameter("minTimestamp", minTimestamp)
            }.body()
            emit(response.data)
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getThemesForChannelFlow error: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun getThemesForChannel(
        channel: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): List<String> {
        return try {
            val response: ThemesResponse = httpClient.get("$baseUrl/api/themes") {
                parameter("channel", channel)
                parameter("limit", limit)
                parameter("offset", offset)
                if (minTimestamp > 0) parameter("minTimestamp", minTimestamp)
            }.body()
            response.data
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getThemesForChannel error: ${e.message}")
            emptyList()
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
        try {
            val response: TitlesResponse = httpClient.get("$baseUrl/api/titles") {
                if (channel.isNotEmpty()) parameter("channel", channel)
                if (theme.isNotEmpty()) parameter("theme", theme)
            }.body()
            emit(response.data.mapIndexed { index, entry -> entry.toMediaEntry(index.toLong()) })
        } catch (e: Exception) {
            println("[TvosApiMediaRepository] getEntriesFlow error: ${e.message}")
            emit(emptyList())
        }
    }
}
