package cut.the.crap.shared.repository

import cut.the.crap.shared.data.currentTimeMillis
import cut.the.crap.shared.database.MediaEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Fallback repository that tries API first, falls back to mock data if API fails.
 * Provides graceful degradation when network is unavailable.
 */
class TvosFallbackMediaRepository(
    private val apiRepository: TvosApiMediaRepository = TvosApiMediaRepository(),
    private val mockRepository: TvosMockMediaRepository = TvosMockMediaRepository()
) : MediaRepository {

    private var apiAvailable = true
    private var lastApiCheck = 0L
    private val apiCheckInterval = 30_000L // Retry API every 30 seconds after failure

    private fun shouldTryApi(): Boolean {
        if (apiAvailable) return true
        val now = currentTimeMillis()
        if (now - lastApiCheck > apiCheckInterval) {
            apiAvailable = true // Reset to try API again
            return true
        }
        return false
    }

    private fun markApiFailed() {
        apiAvailable = false
        lastApiCheck = currentTimeMillis()
        println("[TvosFallbackMediaRepository] API unavailable, using mock data")
    }

    private fun markApiSuccess() {
        if (!apiAvailable) {
            println("[TvosFallbackMediaRepository] API recovered, using real data")
        }
        apiAvailable = true
    }

    // Helper for Flow-based methods with fallback
    private fun <T> flowWithFallback(
        apiCall: () -> Flow<T>,
        mockCall: () -> Flow<T>
    ): Flow<T> = flow {
        if (shouldTryApi()) {
            try {
                apiCall()
                    .catch { e ->
                        println("[TvosFallbackMediaRepository] API flow error: ${e.message}")
                        markApiFailed()
                        emitAll(mockCall())
                    }
                    .collect { value ->
                        markApiSuccess()
                        emit(value)
                    }
            } catch (e: Exception) {
                println("[TvosFallbackMediaRepository] API call error: ${e.message}")
                markApiFailed()
                emitAll(mockCall())
            }
        } else {
            emitAll(mockCall())
        }
    }

    // Helper for suspend methods with fallback
    private suspend fun <T> suspendWithFallback(
        apiCall: suspend () -> T,
        mockCall: suspend () -> T
    ): T {
        return if (shouldTryApi()) {
            try {
                val result = apiCall()
                markApiSuccess()
                result
            } catch (e: Exception) {
                println("[TvosFallbackMediaRepository] API suspend error: ${e.message}")
                markApiFailed()
                mockCall()
            }
        } else {
            mockCall()
        }
    }

    // Helper for suspend methods returning List with empty check fallback
    private suspend fun <T> suspendListWithFallback(
        apiCall: suspend () -> List<T>,
        mockCall: suspend () -> List<T>
    ): List<T> {
        return if (shouldTryApi()) {
            try {
                val result = apiCall()
                if (result.isEmpty()) {
                    // Empty result might indicate API issue, try mock
                    mockCall()
                } else {
                    markApiSuccess()
                    result
                }
            } catch (e: Exception) {
                println("[TvosFallbackMediaRepository] API list error: ${e.message}")
                markApiFailed()
                mockCall()
            }
        } else {
            mockCall()
        }
    }

    // MARK: - MediaRepository Implementation

    override fun getAllChannelsFlow(): Flow<List<String>> = flowWithFallback(
        apiCall = { apiRepository.getAllChannelsFlow() },
        mockCall = { mockRepository.getAllChannelsFlow() }
    )

    override suspend fun getAllChannels(): List<String> = suspendListWithFallback(
        apiCall = { apiRepository.getAllChannels() },
        mockCall = { mockRepository.getAllChannels() }
    )

    override fun getAllThemesFlow(minTimestamp: Long, limit: Int, offset: Int): Flow<List<String>> = flowWithFallback(
        apiCall = { apiRepository.getAllThemesFlow(minTimestamp, limit, offset) },
        mockCall = { mockRepository.getAllThemesFlow(minTimestamp, limit, offset) }
    )

    override suspend fun getAllThemes(minTimestamp: Long, limit: Int, offset: Int): List<String> = suspendListWithFallback(
        apiCall = { apiRepository.getAllThemes(minTimestamp, limit, offset) },
        mockCall = { mockRepository.getAllThemes(minTimestamp, limit, offset) }
    )

    override fun getThemesForChannelFlow(
        channel: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): Flow<List<String>> = flowWithFallback(
        apiCall = { apiRepository.getThemesForChannelFlow(channel, minTimestamp, limit, offset) },
        mockCall = { mockRepository.getThemesForChannelFlow(channel, minTimestamp, limit, offset) }
    )

    override suspend fun getThemesForChannel(
        channel: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): List<String> = suspendListWithFallback(
        apiCall = { apiRepository.getThemesForChannel(channel, minTimestamp, limit, offset) },
        mockCall = { mockRepository.getThemesForChannel(channel, minTimestamp, limit, offset) }
    )

    override fun getTitlesForThemeFlow(theme: String, minTimestamp: Long): Flow<List<String>> = flowWithFallback(
        apiCall = { apiRepository.getTitlesForThemeFlow(theme, minTimestamp) },
        mockCall = { mockRepository.getTitlesForThemeFlow(theme, minTimestamp) }
    )

    override suspend fun getTitlesForTheme(theme: String, minTimestamp: Long): List<String> = suspendListWithFallback(
        apiCall = { apiRepository.getTitlesForTheme(theme, minTimestamp) },
        mockCall = { mockRepository.getTitlesForTheme(theme, minTimestamp) }
    )

    override fun getTitlesForChannelAndThemeFlow(
        channel: String,
        theme: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): Flow<List<String>> = flowWithFallback(
        apiCall = { apiRepository.getTitlesForChannelAndThemeFlow(channel, theme, minTimestamp, limit, offset) },
        mockCall = { mockRepository.getTitlesForChannelAndThemeFlow(channel, theme, minTimestamp, limit, offset) }
    )

    override suspend fun getTitlesForChannelAndTheme(
        channel: String,
        theme: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): List<String> = suspendListWithFallback(
        apiCall = { apiRepository.getTitlesForChannelAndTheme(channel, theme, minTimestamp, limit, offset) },
        mockCall = { mockRepository.getTitlesForChannelAndTheme(channel, theme, minTimestamp, limit, offset) }
    )

    override fun getMediaEntryFlow(channel: String, theme: String, title: String): Flow<MediaEntry?> = flowWithFallback(
        apiCall = { apiRepository.getMediaEntryFlow(channel, theme, title) },
        mockCall = { mockRepository.getMediaEntryFlow(channel, theme, title) }
    )

    override fun getMediaEntryByThemeAndTitleFlow(theme: String, title: String): Flow<MediaEntry?> = flowWithFallback(
        apiCall = { apiRepository.getMediaEntryByThemeAndTitleFlow(theme, title) },
        mockCall = { mockRepository.getMediaEntryByThemeAndTitleFlow(theme, title) }
    )

    override fun getMediaEntryByTitleFlow(title: String): Flow<MediaEntry?> = flowWithFallback(
        apiCall = { apiRepository.getMediaEntryByTitleFlow(title) },
        mockCall = { mockRepository.getMediaEntryByTitleFlow(title) }
    )

    override fun getAllThemesAsEntriesFlow(minTimestamp: Long, limit: Int, offset: Int): Flow<List<MediaEntry>> = flowWithFallback(
        apiCall = { apiRepository.getAllThemesAsEntriesFlow(minTimestamp, limit, offset) },
        mockCall = { mockRepository.getAllThemesAsEntriesFlow(minTimestamp, limit, offset) }
    )

    override fun getThemesForChannelAsEntriesFlow(
        channel: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): Flow<List<MediaEntry>> = flowWithFallback(
        apiCall = { apiRepository.getThemesForChannelAsEntriesFlow(channel, minTimestamp, limit, offset) },
        mockCall = { mockRepository.getThemesForChannelAsEntriesFlow(channel, minTimestamp, limit, offset) }
    )

    override fun getTitlesForThemeAsEntriesFlow(theme: String, minTimestamp: Long): Flow<List<MediaEntry>> = flowWithFallback(
        apiCall = { apiRepository.getTitlesForThemeAsEntriesFlow(theme, minTimestamp) },
        mockCall = { mockRepository.getTitlesForThemeAsEntriesFlow(theme, minTimestamp) }
    )

    override fun getTitlesForChannelAndThemeAsEntriesFlow(
        channel: String,
        theme: String,
        minTimestamp: Long
    ): Flow<List<MediaEntry>> = flowWithFallback(
        apiCall = { apiRepository.getTitlesForChannelAndThemeAsEntriesFlow(channel, theme, minTimestamp) },
        mockCall = { mockRepository.getTitlesForChannelAndThemeAsEntriesFlow(channel, theme, minTimestamp) }
    )

    override suspend fun getRecentEntries(minTimestamp: Long, limit: Int): List<MediaEntry> = suspendListWithFallback(
        apiCall = { apiRepository.getRecentEntries(minTimestamp, limit) },
        mockCall = { mockRepository.getRecentEntries(minTimestamp, limit) }
    )

    override suspend fun searchEntries(query: String, limit: Int): List<MediaEntry> = suspendListWithFallback(
        apiCall = { apiRepository.searchEntries(query, limit) },
        mockCall = { mockRepository.searchEntries(query, limit) }
    )

    override suspend fun searchEntriesByChannel(channel: String, query: String, limit: Int): List<MediaEntry> = suspendListWithFallback(
        apiCall = { apiRepository.searchEntriesByChannel(channel, query, limit) },
        mockCall = { mockRepository.searchEntriesByChannel(channel, query, limit) }
    )

    override suspend fun searchEntriesByTheme(theme: String, query: String, limit: Int): List<MediaEntry> = suspendListWithFallback(
        apiCall = { apiRepository.searchEntriesByTheme(theme, query, limit) },
        mockCall = { mockRepository.searchEntriesByTheme(theme, query, limit) }
    )

    override suspend fun searchEntriesByChannelAndTheme(
        channel: String,
        theme: String,
        query: String,
        limit: Int
    ): List<MediaEntry> = suspendListWithFallback(
        apiCall = { apiRepository.searchEntriesByChannelAndTheme(channel, theme, query, limit) },
        mockCall = { mockRepository.searchEntriesByChannelAndTheme(channel, theme, query, limit) }
    )

    override suspend fun searchEntriesWithOffset(query: String, limit: Int, offset: Int): List<MediaEntry> = suspendListWithFallback(
        apiCall = { apiRepository.searchEntriesWithOffset(query, limit, offset) },
        mockCall = { mockRepository.searchEntriesWithOffset(query, limit, offset) }
    )

    override suspend fun searchEntriesByChannelWithOffset(
        channel: String,
        query: String,
        limit: Int,
        offset: Int
    ): List<MediaEntry> = suspendListWithFallback(
        apiCall = { apiRepository.searchEntriesByChannelWithOffset(channel, query, limit, offset) },
        mockCall = { mockRepository.searchEntriesByChannelWithOffset(channel, query, limit, offset) }
    )

    override suspend fun searchEntriesByThemeWithOffset(
        theme: String,
        query: String,
        limit: Int,
        offset: Int
    ): List<MediaEntry> = suspendListWithFallback(
        apiCall = { apiRepository.searchEntriesByThemeWithOffset(theme, query, limit, offset) },
        mockCall = { mockRepository.searchEntriesByThemeWithOffset(theme, query, limit, offset) }
    )

    override suspend fun searchEntriesByChannelAndThemeWithOffset(
        channel: String,
        theme: String,
        query: String,
        limit: Int,
        offset: Int
    ): List<MediaEntry> = suspendListWithFallback(
        apiCall = { apiRepository.searchEntriesByChannelAndThemeWithOffset(channel, theme, query, limit, offset) },
        mockCall = { mockRepository.searchEntriesByChannelAndThemeWithOffset(channel, theme, query, limit, offset) }
    )

    override suspend fun getCount(): Int = suspendWithFallback(
        apiCall = { apiRepository.getCount() },
        mockCall = { mockRepository.getCount() }
    )

    override suspend fun insert(entry: MediaEntry): Long = mockRepository.insert(entry)

    override suspend fun insertAll(entries: List<MediaEntry>) = mockRepository.insertAll(entries)

    override suspend fun deleteAll() = mockRepository.deleteAll()

    override fun loadMediaListFromFile(filePath: String): Flow<MediaRepository.LoadingResult> =
        mockRepository.loadMediaListFromFile(filePath)

    override fun applyDiffToDatabase(filePath: String): Flow<MediaRepository.LoadingResult> =
        mockRepository.applyDiffToDatabase(filePath)

    override suspend fun checkAndLoadMediaList(privatePath: String): Boolean =
        mockRepository.checkAndLoadMediaList(privatePath)

    override fun getEntriesFlow(channel: String, theme: String): Flow<List<MediaEntry>> = flowWithFallback(
        apiCall = { apiRepository.getEntriesFlow(channel, theme) },
        mockCall = { mockRepository.getEntriesFlow(channel, theme) }
    )
}
