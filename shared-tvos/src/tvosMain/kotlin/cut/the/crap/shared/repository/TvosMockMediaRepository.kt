package cut.the.crap.shared.repository

import cut.the.crap.shared.data.currentTimeMillis
import cut.the.crap.shared.database.MediaEntry
import cut.the.crap.shared.ui.SampleData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * tvOS Mock implementation of MediaRepository
 * Uses in-memory sample data for demonstration purposes.
 * Will be replaced with API-based repository when backend is ready.
 */
class TvosMockMediaRepository : MediaRepository {

    private val mockEntries = MutableStateFlow(generateMockEntries())

    private fun generateMockEntries(): List<MediaEntry> {
        val entries = mutableListOf<MediaEntry>()
        var id = 1L
        val currentTimestamp = currentTimeMillis() / 1000

        SampleData.sampleChannels.forEach { channel ->
            val themes = SampleData.getThemesForChannel(channel.name)
            themes.forEach { theme ->
                val titles = SampleData.getTitlesForTheme(theme)
                titles.forEach { title ->
                    entries.add(
                        MediaEntry(
                            id = id++,
                            channel = channel.name,
                            theme = theme,
                            title = title,
                            date = "25.07.2024",
                            time = "20:15",
                            duration = "45 Min",
                            sizeMB = "750",
                            description = "Beschreibung fuer $title im Thema $theme von ${channel.displayName}. " +
                                "Eine spannende Sendung mit interessanten Inhalten und informativen Beitraegen.",
                            url = "https://example.com/video/${id}.mp4",
                            website = "https://www.${channel.name.lowercase()}.de",
                            subtitleUrl = "",
                            smallUrl = "https://example.com/video/${id}_small.mp4",
                            hdUrl = "https://example.com/video/${id}_hd.mp4",
                            timestamp = currentTimestamp - (id * 3600), // Spread entries over time
                            geo = "DE-AT-CH",
                            isNew = id <= 10
                        )
                    )
                }
            }
        }

        return entries
    }

    override fun getAllChannelsFlow(): Flow<List<String>> {
        return mockEntries.map { entries ->
            entries.map { it.channel }.distinct().sorted()
        }
    }

    override suspend fun getAllChannels(): List<String> {
        return mockEntries.value.map { it.channel }.distinct().sorted()
    }

    override fun getAllThemesFlow(minTimestamp: Long, limit: Int, offset: Int): Flow<List<String>> {
        return mockEntries.map { entries ->
            entries
                .filter { it.timestamp >= minTimestamp }
                .map { it.theme }
                .distinct()
                .drop(offset)
                .take(limit)
        }
    }

    override suspend fun getAllThemes(minTimestamp: Long, limit: Int, offset: Int): List<String> {
        return mockEntries.value
            .filter { it.timestamp >= minTimestamp }
            .map { it.theme }
            .distinct()
            .drop(offset)
            .take(limit)
    }

    override fun getThemesForChannelFlow(
        channel: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): Flow<List<String>> {
        return mockEntries.map { entries ->
            entries
                .filter { it.channel == channel && it.timestamp >= minTimestamp }
                .map { it.theme }
                .distinct()
                .drop(offset)
                .take(limit)
        }
    }

    override suspend fun getThemesForChannel(
        channel: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): List<String> {
        return mockEntries.value
            .filter { it.channel == channel && it.timestamp >= minTimestamp }
            .map { it.theme }
            .distinct()
            .drop(offset)
            .take(limit)
    }

    override fun getTitlesForThemeFlow(theme: String, minTimestamp: Long): Flow<List<String>> {
        return mockEntries.map { entries ->
            entries
                .filter { it.theme == theme && it.timestamp >= minTimestamp }
                .map { it.title }
                .distinct()
        }
    }

    override suspend fun getTitlesForTheme(theme: String, minTimestamp: Long): List<String> {
        return mockEntries.value
            .filter { it.theme == theme && it.timestamp >= minTimestamp }
            .map { it.title }
            .distinct()
    }

    override fun getTitlesForChannelAndThemeFlow(
        channel: String,
        theme: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): Flow<List<String>> {
        return mockEntries.map { entries ->
            entries
                .filter { it.channel == channel && it.theme == theme && it.timestamp >= minTimestamp }
                .map { it.title }
                .distinct()
        }
    }

    override suspend fun getTitlesForChannelAndTheme(
        channel: String,
        theme: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): List<String> {
        return mockEntries.value
            .filter { it.channel == channel && it.theme == theme && it.timestamp >= minTimestamp }
            .map { it.title }
            .distinct()
    }

    override fun getMediaEntryFlow(channel: String, theme: String, title: String): Flow<MediaEntry?> {
        return mockEntries.map { entries ->
            entries.find { it.channel == channel && it.theme == theme && it.title == title }
        }
    }

    override fun getMediaEntryByThemeAndTitleFlow(theme: String, title: String): Flow<MediaEntry?> {
        return mockEntries.map { entries ->
            entries.find { it.theme == theme && it.title == title }
        }
    }

    override fun getMediaEntryByTitleFlow(title: String): Flow<MediaEntry?> {
        return mockEntries.map { entries ->
            entries.find { it.title == title }
        }
    }

    override fun getAllThemesAsEntriesFlow(minTimestamp: Long, limit: Int, offset: Int): Flow<List<MediaEntry>> {
        return mockEntries.map { entries ->
            entries
                .filter { it.timestamp >= minTimestamp }
                .distinctBy { it.theme }
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
        return mockEntries.map { entries ->
            entries
                .filter { it.channel == channel && it.timestamp >= minTimestamp }
                .distinctBy { it.theme }
                .drop(offset)
                .take(limit)
        }
    }

    override fun getTitlesForThemeAsEntriesFlow(theme: String, minTimestamp: Long): Flow<List<MediaEntry>> {
        return mockEntries.map { entries ->
            entries
                .filter { it.theme == theme && it.timestamp >= minTimestamp }
                .distinctBy { it.title }
        }
    }

    override fun getTitlesForChannelAndThemeAsEntriesFlow(
        channel: String,
        theme: String,
        minTimestamp: Long
    ): Flow<List<MediaEntry>> {
        return mockEntries.map { entries ->
            entries.filter {
                it.channel == channel && it.theme == theme && it.timestamp >= minTimestamp
            }
        }
    }

    override suspend fun getRecentEntries(minTimestamp: Long, limit: Int): List<MediaEntry> {
        return mockEntries.value
            .filter { it.timestamp >= minTimestamp }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }

    override suspend fun searchEntries(query: String, limit: Int): List<MediaEntry> {
        val lowerQuery = query.lowercase()
        return mockEntries.value
            .filter {
                it.title.lowercase().contains(lowerQuery) ||
                it.theme.lowercase().contains(lowerQuery) ||
                it.description.lowercase().contains(lowerQuery)
            }
            .take(limit)
    }

    override suspend fun searchEntriesByChannel(channel: String, query: String, limit: Int): List<MediaEntry> {
        val lowerQuery = query.lowercase()
        return mockEntries.value
            .filter { it.channel == channel }
            .filter {
                it.title.lowercase().contains(lowerQuery) ||
                it.theme.lowercase().contains(lowerQuery) ||
                it.description.lowercase().contains(lowerQuery)
            }
            .take(limit)
    }

    override suspend fun searchEntriesByTheme(theme: String, query: String, limit: Int): List<MediaEntry> {
        val lowerQuery = query.lowercase()
        return mockEntries.value
            .filter { it.theme == theme }
            .filter {
                it.title.lowercase().contains(lowerQuery) ||
                it.description.lowercase().contains(lowerQuery)
            }
            .take(limit)
    }

    override suspend fun searchEntriesByChannelAndTheme(
        channel: String,
        theme: String,
        query: String,
        limit: Int
    ): List<MediaEntry> {
        val lowerQuery = query.lowercase()
        return mockEntries.value
            .filter { it.channel == channel && it.theme == theme }
            .filter {
                it.title.lowercase().contains(lowerQuery) ||
                it.description.lowercase().contains(lowerQuery)
            }
            .take(limit)
    }

    override suspend fun searchEntriesWithOffset(query: String, limit: Int, offset: Int): List<MediaEntry> {
        val lowerQuery = query.lowercase()
        return mockEntries.value
            .filter {
                it.title.lowercase().contains(lowerQuery) ||
                it.theme.lowercase().contains(lowerQuery) ||
                it.description.lowercase().contains(lowerQuery)
            }
            .drop(offset)
            .take(limit)
    }

    override suspend fun searchEntriesByChannelWithOffset(
        channel: String,
        query: String,
        limit: Int,
        offset: Int
    ): List<MediaEntry> {
        val lowerQuery = query.lowercase()
        return mockEntries.value
            .filter { it.channel == channel }
            .filter {
                it.title.lowercase().contains(lowerQuery) ||
                it.theme.lowercase().contains(lowerQuery) ||
                it.description.lowercase().contains(lowerQuery)
            }
            .drop(offset)
            .take(limit)
    }

    override suspend fun searchEntriesByThemeWithOffset(
        theme: String,
        query: String,
        limit: Int,
        offset: Int
    ): List<MediaEntry> {
        val lowerQuery = query.lowercase()
        return mockEntries.value
            .filter { it.theme == theme }
            .filter {
                it.title.lowercase().contains(lowerQuery) ||
                it.description.lowercase().contains(lowerQuery)
            }
            .drop(offset)
            .take(limit)
    }

    override suspend fun searchEntriesByChannelAndThemeWithOffset(
        channel: String,
        theme: String,
        query: String,
        limit: Int,
        offset: Int
    ): List<MediaEntry> {
        val lowerQuery = query.lowercase()
        return mockEntries.value
            .filter { it.channel == channel && it.theme == theme }
            .filter {
                it.title.lowercase().contains(lowerQuery) ||
                it.description.lowercase().contains(lowerQuery)
            }
            .drop(offset)
            .take(limit)
    }

    override suspend fun getCount(): Int {
        return mockEntries.value.size
    }

    override suspend fun insert(entry: MediaEntry): Long {
        // In-memory mock - no persistence needed for tvOS demo
        return entry.id
    }

    override suspend fun insertAll(entries: List<MediaEntry>) {
        // In-memory mock - no persistence needed for tvOS demo
    }

    override suspend fun deleteAll() {
        // In-memory mock - regenerate sample data
        mockEntries.value = generateMockEntries()
    }

    override fun loadMediaListFromFile(filePath: String): Flow<MediaRepository.LoadingResult> = flow {
        // tvOS uses mock data, no file loading needed
        emit(MediaRepository.LoadingResult.Complete(mockEntries.value.size))
    }

    override fun applyDiffToDatabase(filePath: String): Flow<MediaRepository.LoadingResult> = flow {
        // tvOS uses mock data, no diff application needed
        emit(MediaRepository.LoadingResult.Complete(0))
    }

    override suspend fun checkAndLoadMediaList(privatePath: String): Boolean {
        // Mock data is always available
        return true
    }

    override fun getEntriesFlow(channel: String, theme: String): Flow<List<MediaEntry>> {
        return when {
            channel.isNotEmpty() && theme.isNotEmpty() -> {
                getTitlesForChannelAndThemeAsEntriesFlow(channel, theme, 0)
            }
            channel.isNotEmpty() -> {
                mockEntries.map { entries ->
                    entries.filter { it.channel == channel }
                }
            }
            else -> flowOf(emptyList())
        }
    }
}
