package cut.the.crap.android.repository

import cut.the.crap.shared.database.MediaEntry
import cut.the.crap.shared.repository.MediaRepository.LoadingResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of MediaRepository for testing MediaViewModel
 * Implements the Android-specific MediaRepository interface
 */
class FakeMediaRepository : MediaRepository {

    // In-memory storage
    private val entries = mutableListOf<MediaEntry>()
    private val entriesFlow = MutableStateFlow<List<MediaEntry>>(emptyList())

    // Control flags for testing error scenarios
    var shouldThrowOnLoad = false
    var loadException: Exception = Exception("Test load error")
    var shouldThrowOnCount = false

    // Track method calls for verification
    var deleteAllCalled = false
    var lastSearchQuery: String? = null
    var lastLoadFilePath: String? = null

    // Cache stats for testing
    private var cacheHits = 0
    private var cacheMisses = 0

    /**
     * Add entries to the fake repository
     */
    fun addEntries(newEntries: List<MediaEntry>) {
        entries.addAll(newEntries)
        entriesFlow.value = entries.toList()
    }

    /**
     * Set entries (replacing existing)
     */
    fun setEntries(newEntries: List<MediaEntry>) {
        entries.clear()
        entries.addAll(newEntries)
        entriesFlow.value = entries.toList()
    }

    /**
     * Clear all entries
     */
    fun clearEntries() {
        entries.clear()
        entriesFlow.value = emptyList()
    }

    /**
     * Reset test state
     */
    fun reset() {
        clearEntries()
        shouldThrowOnLoad = false
        shouldThrowOnCount = false
        deleteAllCalled = false
        lastSearchQuery = null
        lastLoadFilePath = null
        cacheHits = 0
        cacheMisses = 0
    }

    // =================================================================================
    // Android-specific MediaRepository Interface
    // =================================================================================

    override fun getCacheStats(): SearchCache.CacheStats {
        return SearchCache.CacheStats(
            hitCount = cacheHits,
            missCount = cacheMisses,
            size = 10,
            maxSize = 50
        )
    }

    // =================================================================================
    // Shared MediaRepository Interface Implementation
    // =================================================================================

    override fun getAllChannelsFlow(): Flow<List<String>> {
        return entriesFlow.map { list ->
            list.map { it.channel }.distinct().sorted()
        }
    }

    override fun getAllThemesFlow(minTimestamp: Long, limit: Int, offset: Int): Flow<List<String>> {
        return entriesFlow.map { list ->
            list.filter { it.timestamp >= minTimestamp }
                .map { it.theme }
                .distinct()
                .sorted()
                .drop(offset)
                .take(limit)
        }
    }

    override fun getThemesForChannelFlow(
        channel: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): Flow<List<String>> {
        return entriesFlow.map { list ->
            list.filter { it.channel == channel && it.timestamp >= minTimestamp }
                .map { it.theme }
                .distinct()
                .sorted()
                .drop(offset)
                .take(limit)
        }
    }

    override fun getTitlesForThemeFlow(theme: String, minTimestamp: Long): Flow<List<String>> {
        return entriesFlow.map { list ->
            list.filter { it.theme == theme && it.timestamp >= minTimestamp }
                .map { it.title }
                .distinct()
                .sorted()
        }
    }

    override fun getTitlesForChannelAndThemeFlow(
        channel: String,
        theme: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): Flow<List<String>> {
        return entriesFlow.map { list ->
            list.filter { it.channel == channel && it.theme == theme && it.timestamp >= minTimestamp }
                .map { it.title }
                .distinct()
                .sorted()
                .drop(offset)
                .take(limit)
        }
    }

    override fun getMediaEntryFlow(channel: String, theme: String, title: String): Flow<MediaEntry?> {
        return entriesFlow.map { list ->
            list.find { it.channel == channel && it.theme == theme && it.title == title }
        }
    }

    override fun getMediaEntryByThemeAndTitleFlow(theme: String, title: String): Flow<MediaEntry?> {
        return entriesFlow.map { list ->
            list.find { it.theme == theme && it.title == title }
        }
    }

    override fun getMediaEntryByTitleFlow(title: String): Flow<MediaEntry?> {
        return entriesFlow.map { list ->
            list.find { it.title == title }
        }
    }

    override suspend fun getRecentEntries(minTimestamp: Long, limit: Int): List<MediaEntry> {
        return entries.filter { it.timestamp >= minTimestamp }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }

    override suspend fun searchEntries(query: String, limit: Int): List<MediaEntry> {
        lastSearchQuery = query
        cacheMisses++
        val lowerQuery = query.lowercase()
        return entries.filter {
            it.title.lowercase().contains(lowerQuery) ||
                    it.theme.lowercase().contains(lowerQuery) ||
                    it.description.lowercase().contains(lowerQuery)
        }.take(limit)
    }

    override suspend fun searchEntriesByChannel(channel: String, query: String, limit: Int): List<MediaEntry> {
        lastSearchQuery = query
        val lowerQuery = query.lowercase()
        return entries.filter { it.channel == channel }
            .filter {
                it.title.lowercase().contains(lowerQuery) ||
                        it.theme.lowercase().contains(lowerQuery) ||
                        it.description.lowercase().contains(lowerQuery)
            }.take(limit)
    }

    override suspend fun searchEntriesByTheme(theme: String, query: String, limit: Int): List<MediaEntry> {
        lastSearchQuery = query
        val lowerQuery = query.lowercase()
        return entries.filter { it.theme == theme }
            .filter {
                it.title.lowercase().contains(lowerQuery) ||
                        it.description.lowercase().contains(lowerQuery)
            }.take(limit)
    }

    override suspend fun searchEntriesByChannelAndTheme(
        channel: String,
        theme: String,
        query: String,
        limit: Int
    ): List<MediaEntry> {
        lastSearchQuery = query
        val lowerQuery = query.lowercase()
        return entries.filter { it.channel == channel && it.theme == theme }
            .filter {
                it.title.lowercase().contains(lowerQuery) ||
                        it.description.lowercase().contains(lowerQuery)
            }.take(limit)
    }

    override suspend fun searchEntriesWithOffset(query: String, limit: Int, offset: Int): List<MediaEntry> {
        lastSearchQuery = query
        val lowerQuery = query.lowercase()
        return entries.filter {
            it.title.lowercase().contains(lowerQuery) ||
                    it.theme.lowercase().contains(lowerQuery) ||
                    it.description.lowercase().contains(lowerQuery)
        }.drop(offset).take(limit)
    }

    override suspend fun searchEntriesByChannelWithOffset(
        channel: String,
        query: String,
        limit: Int,
        offset: Int
    ): List<MediaEntry> {
        lastSearchQuery = query
        val lowerQuery = query.lowercase()
        return entries.filter { it.channel == channel }
            .filter {
                it.title.lowercase().contains(lowerQuery) ||
                        it.theme.lowercase().contains(lowerQuery) ||
                        it.description.lowercase().contains(lowerQuery)
            }.drop(offset).take(limit)
    }

    override suspend fun searchEntriesByThemeWithOffset(
        theme: String,
        query: String,
        limit: Int,
        offset: Int
    ): List<MediaEntry> {
        lastSearchQuery = query
        val lowerQuery = query.lowercase()
        return entries.filter { it.theme == theme }
            .filter {
                it.title.lowercase().contains(lowerQuery) ||
                        it.description.lowercase().contains(lowerQuery)
            }.drop(offset).take(limit)
    }

    override suspend fun searchEntriesByChannelAndThemeWithOffset(
        channel: String,
        theme: String,
        query: String,
        limit: Int,
        offset: Int
    ): List<MediaEntry> {
        lastSearchQuery = query
        val lowerQuery = query.lowercase()
        return entries.filter { it.channel == channel && it.theme == theme }
            .filter {
                it.title.lowercase().contains(lowerQuery) ||
                        it.description.lowercase().contains(lowerQuery)
            }.drop(offset).take(limit)
    }

    override suspend fun getCount(): Int {
        if (shouldThrowOnCount) {
            throw Exception("Test count error")
        }
        return entries.size
    }

    override suspend fun insert(entry: MediaEntry): Long {
        val newEntry = entry.copy(id = (entries.maxOfOrNull { it.id } ?: 0) + 1)
        entries.add(newEntry)
        entriesFlow.value = entries.toList()
        return newEntry.id
    }

    override suspend fun insertAll(entries: List<MediaEntry>) {
        var nextId = (this.entries.maxOfOrNull { it.id } ?: 0) + 1
        entries.forEach { entry ->
            this.entries.add(entry.copy(id = nextId++))
        }
        entriesFlow.value = this.entries.toList()
    }

    override suspend fun deleteAll() {
        deleteAllCalled = true
        entries.clear()
        entriesFlow.value = emptyList()
    }

    override fun loadMediaListFromFile(filePath: String): Flow<LoadingResult> {
        lastLoadFilePath = filePath
        return if (shouldThrowOnLoad) {
            flowOf(LoadingResult.Error(loadException, 0))
        } else {
            flow {
                emit(LoadingResult.Progress(50))
                emit(LoadingResult.Complete(entries.size))
            }
        }
    }

    override fun applyDiffToDatabase(filePath: String): Flow<LoadingResult> {
        lastLoadFilePath = filePath
        return if (shouldThrowOnLoad) {
            flowOf(LoadingResult.Error(loadException, 0))
        } else {
            flow {
                emit(LoadingResult.Progress(50))
                emit(LoadingResult.Complete(entries.size))
            }
        }
    }

    override suspend fun checkAndLoadMediaList(privatePath: String): Boolean {
        lastLoadFilePath = privatePath
        return entries.isNotEmpty()
    }

    override fun getEntriesFlow(channel: String, theme: String): Flow<List<MediaEntry>> {
        return entriesFlow.map { list ->
            list.filter {
                (channel.isEmpty() || it.channel == channel) &&
                        (theme.isEmpty() || it.theme == theme)
            }
        }
    }

    override fun getAllThemesAsEntriesFlow(
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): Flow<List<MediaEntry>> {
        return entriesFlow.map { list ->
            list.filter { it.timestamp >= minTimestamp }
                .distinctBy { it.theme }
                .sortedBy { it.theme }
                .drop(offset)
                .take(limit)
        }
    }

    override fun getThemesForChannelAsEntriesFlow(
        channel: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): Flow<List<MediaEntry>> {
        return entriesFlow.map { list ->
            list.filter { it.channel == channel && it.timestamp >= minTimestamp }
                .distinctBy { it.theme }
                .sortedBy { it.theme }
                .drop(offset)
                .take(limit)
        }
    }

    override fun getTitlesForThemeAsEntriesFlow(
        theme: String,
        minTimestamp: Long
    ): Flow<List<MediaEntry>> {
        return entriesFlow.map { list ->
            list.filter { it.theme == theme && it.timestamp >= minTimestamp }
                .distinctBy { it.title }
                .sortedBy { it.title }
        }
    }

    override fun getTitlesForChannelAndThemeAsEntriesFlow(
        channel: String,
        theme: String,
        minTimestamp: Long
    ): Flow<List<MediaEntry>> {
        return entriesFlow.map { list ->
            list.filter { it.channel == channel && it.theme == theme && it.timestamp >= minTimestamp }
                .distinctBy { it.title }
                .sortedBy { it.title }
        }
    }
}
