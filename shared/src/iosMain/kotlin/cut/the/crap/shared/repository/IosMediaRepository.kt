package cut.the.crap.shared.repository

import cut.the.crap.shared.data.IosStreamingMediaListParser
import cut.the.crap.shared.data.PlatformLogger
import cut.the.crap.shared.database.FavoriteDao
import cut.the.crap.shared.database.FavoriteEntry
import cut.the.crap.shared.database.HistoryDao
import cut.the.crap.shared.database.HistoryEntry
import cut.the.crap.shared.database.MediaDao
import cut.the.crap.shared.database.MediaEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import cut.the.crap.shared.data.currentTimeMillis
import cut.the.crap.shared.model.MediaEntry as ModelMediaEntry

/**
 * Convert parser MediaEntry to database MediaEntry
 */
private fun ModelMediaEntry.toDatabaseEntry() = MediaEntry(
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
    smallUrl = urlSmall,
    hdUrl = urlHd,
    timestamp = dateL,
    geo = geo,
    isNew = isNew
)

/**
 * iOS implementation of MediaRepository
 * Wraps the Room database DAO for media data access
 */
class IosMediaRepository(
    private val mediaDao: MediaDao,
    private val favoriteDao: FavoriteDao? = null,
    private val historyDao: HistoryDao? = null
) : MediaRepository {

    override fun getAllChannelsFlow(): Flow<List<String>> {
        return mediaDao.getAllChannelsFlow()
    }

    override fun getAllThemesFlow(minTimestamp: Long, limit: Int, offset: Int): Flow<List<String>> {
        return mediaDao.getAllThemesFlow(minTimestamp, limit, offset)
    }

    override fun getThemesForChannelFlow(channel: String, minTimestamp: Long, limit: Int, offset: Int): Flow<List<String>> {
        return mediaDao.getThemesForChannelFlow(channel, minTimestamp, limit, offset)
    }

    override fun getTitlesForThemeFlow(theme: String, minTimestamp: Long): Flow<List<String>> {
        return mediaDao.getTitlesForThemeFlow(theme, minTimestamp)
    }

    override fun getTitlesForChannelAndThemeFlow(channel: String, theme: String, minTimestamp: Long, limit: Int, offset: Int): Flow<List<String>> {
        return mediaDao.getTitlesForChannelAndThemeFlow(channel, theme, minTimestamp)
    }

    override fun getMediaEntryFlow(channel: String, theme: String, title: String): Flow<MediaEntry?> {
        return mediaDao.getMediaEntryFlow(channel, theme, title)
    }

    override fun getMediaEntryByThemeAndTitleFlow(theme: String, title: String): Flow<MediaEntry?> {
        return mediaDao.getMediaEntryByThemeAndTitleFlow(theme, title)
    }

    override fun getMediaEntryByTitleFlow(title: String): Flow<MediaEntry?> {
        return mediaDao.getMediaEntryByTitleFlow(title)
    }

    override fun getAllThemesAsEntriesFlow(minTimestamp: Long, limit: Int, offset: Int): Flow<List<MediaEntry>> {
        return mediaDao.getAllThemesAsEntriesFlow(minTimestamp, limit, offset)
    }

    override fun getThemesForChannelAsEntriesFlow(channel: String, minTimestamp: Long, limit: Int, offset: Int): Flow<List<MediaEntry>> {
        return mediaDao.getThemesForChannelAsEntriesFlow(channel, minTimestamp, limit, offset)
    }

    override fun getTitlesForThemeAsEntriesFlow(theme: String, minTimestamp: Long): Flow<List<MediaEntry>> {
        return mediaDao.getTitlesForThemeAsEntriesFlow(theme, minTimestamp)
    }

    override fun getTitlesForChannelAndThemeAsEntriesFlow(channel: String, theme: String, minTimestamp: Long): Flow<List<MediaEntry>> {
        return mediaDao.getTitlesForChannelAndThemeAsEntriesFlow(channel, theme, minTimestamp)
    }

    override suspend fun getRecentEntries(minTimestamp: Long, limit: Int): List<MediaEntry> {
        return withContext(Dispatchers.IO) {
            mediaDao.getRecentEntries(minTimestamp, limit)
        }
    }

    override suspend fun searchEntries(query: String, limit: Int): List<MediaEntry> {
        return withContext(Dispatchers.IO) {
            mediaDao.searchEntries(query, limit)
        }
    }

    override suspend fun searchEntriesByChannel(channel: String, query: String, limit: Int): List<MediaEntry> {
        return withContext(Dispatchers.IO) {
            mediaDao.searchEntriesByChannel(channel, query, limit)
        }
    }

    override suspend fun searchEntriesByTheme(theme: String, query: String, limit: Int): List<MediaEntry> {
        return withContext(Dispatchers.IO) {
            mediaDao.searchEntriesByTheme(theme, query, limit)
        }
    }

    override suspend fun searchEntriesByChannelAndTheme(channel: String, theme: String, query: String, limit: Int): List<MediaEntry> {
        return withContext(Dispatchers.IO) {
            mediaDao.searchEntriesByChannelAndTheme(channel, theme, query, limit)
        }
    }

    override suspend fun searchEntriesWithOffset(query: String, limit: Int, offset: Int): List<MediaEntry> {
        return withContext(Dispatchers.IO) {
            mediaDao.searchEntriesWithOffset(query, limit, offset)
        }
    }

    override suspend fun searchEntriesByChannelWithOffset(channel: String, query: String, limit: Int, offset: Int): List<MediaEntry> {
        return withContext(Dispatchers.IO) {
            mediaDao.searchEntriesByChannelWithOffset(channel, query, limit, offset)
        }
    }

    override suspend fun searchEntriesByThemeWithOffset(theme: String, query: String, limit: Int, offset: Int): List<MediaEntry> {
        return withContext(Dispatchers.IO) {
            mediaDao.searchEntriesByThemeWithOffset(theme, query, limit, offset)
        }
    }

    override suspend fun searchEntriesByChannelAndThemeWithOffset(channel: String, theme: String, query: String, limit: Int, offset: Int): List<MediaEntry> {
        return withContext(Dispatchers.IO) {
            mediaDao.searchEntriesByChannelAndThemeWithOffset(channel, theme, query, limit, offset)
        }
    }

    override suspend fun getCount(): Int {
        return withContext(Dispatchers.IO) {
            mediaDao.getCount()
        }
    }

    override suspend fun insert(entry: MediaEntry): Long {
        return withContext(Dispatchers.IO) {
            mediaDao.insert(entry)
        }
    }

    override suspend fun insertAll(entries: List<MediaEntry>) {
        withContext(Dispatchers.IO) {
            mediaDao.insertInBatches(entries, 1000)
        }
    }

    override suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            mediaDao.deleteAll()
        }
    }

    override fun loadMediaListFromFile(filePath: String): Flow<MediaRepository.LoadingResult> = flow {
        val TAG = "IosMediaRepository"

        PlatformLogger.info(TAG, "=== STARTING loadMediaListFromFile ===")
        PlatformLogger.info(TAG, "File path: $filePath")

        emit(MediaRepository.LoadingResult.Progress(0))

        try {
            // Clear existing data before import
            PlatformLogger.info(TAG, "Step 1: About to delete all existing entries...")
            try {
                mediaDao.deleteAll()
                PlatformLogger.info(TAG, "Step 1: Delete completed successfully")
            } catch (deleteError: Exception) {
                PlatformLogger.error(TAG, "Step 1: FAILED to delete entries", deleteError)
                throw deleteError
            }

            // Small delay after delete
            delay(50)
            PlatformLogger.info(TAG, "Step 2: Creating streaming parser...")

            // Use iOS streaming parser - true streaming with NSInputStream
            val parser = IosStreamingMediaListParser()
            val chunkSize = 1000
            val chunk = mutableListOf<MediaEntry>()
            var totalCount = 0
            var chunkNumber = 0

            PlatformLogger.info(TAG, "Step 3: Starting to parse file with callback method...")

            // Parse entries using callback (avoids sequence/coroutine issues on iOS)
            parser.parseFileWithCallback(
                filePath = filePath,
                onEntry = { modelEntry ->
                    try {
                        val dbEntry = modelEntry.toDatabaseEntry()
                        chunk.add(dbEntry)
                        totalCount++
                    } catch (conversionError: Exception) {
                        PlatformLogger.error(TAG, "Error converting entry $totalCount: ${modelEntry.title}", conversionError)
                        return@parseFileWithCallback
                    }

                    // Insert in batches
                    if (chunk.size >= chunkSize) {
                        chunkNumber++
                        PlatformLogger.info(TAG, "Step 4.$chunkNumber: Inserting chunk $chunkNumber (${chunk.size} entries, total: $totalCount)")

                        try {
                            kotlinx.coroutines.runBlocking {
                                mediaDao.insertAll(chunk)
                            }
                            PlatformLogger.info(TAG, "Step 4.$chunkNumber: Chunk $chunkNumber inserted successfully")
                        } catch (insertError: Exception) {
                            PlatformLogger.error(TAG, "Step 4.$chunkNumber: FAILED to insert chunk $chunkNumber", insertError)
                            if (chunk.isNotEmpty()) {
                                PlatformLogger.error(TAG, "First entry: channel=${chunk[0].channel}, title=${chunk[0].title?.take(50)}")
                            }
                        }

                        chunk.clear()
                    }
                },
                onProgress = { count ->
                    PlatformLogger.info(TAG, "Parse progress: $count entries parsed")
                }
            )

            // Insert remaining entries
            if (chunk.isNotEmpty()) {
                chunkNumber++
                PlatformLogger.info(TAG, "Step 5: Inserting final chunk $chunkNumber (${chunk.size} entries, total: $totalCount)")

                try {
                    mediaDao.insertAll(chunk)
                    PlatformLogger.info(TAG, "Step 5: Final chunk inserted successfully")
                } catch (finalInsertError: Exception) {
                    PlatformLogger.error(TAG, "Step 5: FAILED to insert final chunk", finalInsertError)
                    throw finalInsertError
                }
            }

            PlatformLogger.info(TAG, "=== IMPORT COMPLETE: $totalCount entries ===")
            emit(MediaRepository.LoadingResult.Complete(totalCount))

        } catch (e: Exception) {
            PlatformLogger.error(TAG, "=== IMPORT FAILED ===", e)
            emit(MediaRepository.LoadingResult.Error(e, 0))
        }
    }.flowOn(Dispatchers.IO)

    override fun applyDiffToDatabase(filePath: String): Flow<MediaRepository.LoadingResult> = flow {
        emit(MediaRepository.LoadingResult.Error(
            Exception("Diff application not yet implemented for iOS"),
            0
        ))
    }.flowOn(Dispatchers.IO)

    override suspend fun checkAndLoadMediaList(privatePath: String): Boolean {
        // Check if database has data
        return getCount() > 0
    }

    override fun getEntriesFlow(channel: String, theme: String): Flow<List<MediaEntry>> {
        return when {
            channel.isNotEmpty() && theme.isNotEmpty() -> {
                getTitlesForChannelAndThemeAsEntriesFlow(channel, theme, 0)
            }
            channel.isNotEmpty() -> {
                flow {
                    val entries = mediaDao.getEntriesByChannel(channel, 0, 1200, 0)
                    emit(entries)
                }
            }
            else -> flowOf(emptyList())
        }
    }

    /**
     * Import media entries directly (for iOS where file parsing happens in Swift/ObjC)
     */
    suspend fun importMediaEntries(entries: List<MediaEntry>, clearFirst: Boolean = true) {
        withContext(Dispatchers.IO) {
            if (clearFirst) {
                mediaDao.deleteAll()
            }
            mediaDao.insertInBatches(entries, 1000)
        }
    }

    // =========================================================================
    // Favorites / Watch Later Implementation
    // =========================================================================

    override fun getFavoritesFlow(listType: String): Flow<List<FavoriteEntry>> {
        return favoriteDao?.getAllByType(listType) ?: flowOf(emptyList())
    }

    override suspend fun addToFavorites(
        channel: String,
        theme: String,
        title: String,
        listType: String
    ) {
        withContext(Dispatchers.IO) {
            favoriteDao?.let { dao ->
                val entry = FavoriteEntry(
                    channel = channel,
                    theme = theme,
                    title = title,
                    addedAt = currentTimeMillis(),
                    listType = listType
                )
                dao.insert(entry)
                PlatformLogger.info("IosMediaRepository", "Added to $listType: $channel/$theme/$title")
            }
        }
    }

    override suspend fun removeFromFavorites(channel: String, theme: String, title: String) {
        withContext(Dispatchers.IO) {
            favoriteDao?.delete(channel, theme, title)
            PlatformLogger.info("IosMediaRepository", "Removed from favorites: $channel/$theme/$title")
        }
    }

    override fun isFavoriteFlow(channel: String, theme: String, title: String): Flow<Boolean> {
        return favoriteDao?.isFavorite(channel, theme, title) ?: flowOf(false)
    }

    override fun isFavoriteByTypeFlow(
        channel: String,
        theme: String,
        title: String,
        listType: String
    ): Flow<Boolean> {
        return favoriteDao?.isFavoriteByType(channel, theme, title, listType) ?: flowOf(false)
    }

    // =========================================================================
    // Playback History Implementation
    // =========================================================================

    override fun getContinueWatchingFlow(limit: Int): Flow<List<HistoryEntry>> {
        return historyDao?.getContinueWatching(limit) ?: flowOf(emptyList())
    }

    override fun getHistoryFlow(limit: Int): Flow<List<HistoryEntry>> {
        return historyDao?.getHistory(limit) ?: flowOf(emptyList())
    }

    override suspend fun recordPlaybackProgress(
        channel: String,
        theme: String,
        title: String,
        positionSeconds: Long,
        durationSeconds: Long
    ) {
        withContext(Dispatchers.IO) {
            historyDao?.let { dao ->
                val now = currentTimeMillis()
                // Consider completed if >90% watched
                val isCompleted = durationSeconds > 0 &&
                    (positionSeconds.toFloat() / durationSeconds.toFloat()) >= 0.9f

                // Check if entry exists
                val existingEntry = dao.getEntry(channel, theme, title)
                if (existingEntry != null) {
                    // Update existing entry
                    dao.updateProgress(
                        channel = channel,
                        theme = theme,
                        title = title,
                        positionSeconds = positionSeconds,
                        durationSeconds = durationSeconds,
                        watchedAt = now,
                        isCompleted = isCompleted
                    )
                } else {
                    // Create new entry
                    val entry = HistoryEntry(
                        channel = channel,
                        theme = theme,
                        title = title,
                        resumePositionSeconds = positionSeconds,
                        durationSeconds = durationSeconds,
                        watchedAt = now,
                        isCompleted = isCompleted
                    )
                    dao.upsert(entry)
                }
                PlatformLogger.info("IosMediaRepository", "Recorded progress: $channel/$theme/$title at $positionSeconds/${durationSeconds}s")
            }
        }
    }

    override suspend fun getResumePosition(channel: String, theme: String, title: String): Long? {
        return withContext(Dispatchers.IO) {
            historyDao?.getResumePosition(channel, theme, title)
        }
    }

    override fun getHistoryEntryFlow(channel: String, theme: String, title: String): Flow<HistoryEntry?> {
        return historyDao?.getEntryFlow(channel, theme, title) ?: flowOf(null)
    }

    override suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            historyDao?.clearAll()
            PlatformLogger.info("IosMediaRepository", "Cleared all playback history")
        }
    }
}
