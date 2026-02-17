package cut.the.crap.desktop.repository

import cut.the.crap.desktop.data.DesktopMediaListParser
import cut.the.crap.shared.database.FavoriteDao
import cut.the.crap.shared.database.FavoriteEntry
import cut.the.crap.shared.database.HistoryDao
import cut.the.crap.shared.database.HistoryEntry
import cut.the.crap.shared.database.MediaDao
import cut.the.crap.shared.database.MediaEntry
import cut.the.crap.shared.repository.MediaRepository
import cut.the.crap.shared.repository.MediaRepository.LoadingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Desktop implementation of MediaRepository
 * Simplified version for initial desktop support
 */
class DesktopMediaRepository(
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

    override fun loadMediaListFromFile(filePath: String): Flow<LoadingResult> = callbackFlow {
        val parser = DesktopMediaListParser()

        // Clear existing data first
        mediaDao.deleteAll()

        parser.parseFileChunked(
            filePath = filePath,
            callback = object : DesktopMediaListParser.ChunkCallback {
                override fun onChunk(entries: List<MediaEntry>, totalParsed: Int) {
                    // Insert batch into database
                    kotlinx.coroutines.runBlocking {
                        mediaDao.insertInBatches(entries, 1000)
                    }
                    trySend(LoadingResult.Progress(totalParsed))
                }

                override fun onComplete(totalEntries: Int) {
                    trySend(LoadingResult.Complete(totalEntries))
                    close()
                }

                override fun onError(error: Exception, entriesParsed: Int) {
                    trySend(LoadingResult.Error(error, entriesParsed))
                    close(error)
                }
            },
            chunkSize = 5000
        )

        awaitClose { }
    }.flowOn(Dispatchers.IO)

    override fun applyDiffToDatabase(filePath: String): Flow<LoadingResult> = callbackFlow {
        try {
            val parser = DesktopMediaListParser()
            val pendingInsertions = AtomicInteger(0)
            var parsingComplete = false
            var totalCount = 0

            val callback = object : DesktopMediaListParser.ChunkCallback {
                override fun onChunk(entries: List<MediaEntry>, totalParsed: Int) {
                    pendingInsertions.incrementAndGet()

                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            // Use INSERT OR REPLACE via insertInBatches
                            mediaDao.insertInBatches(entries, 500)
                            trySend(LoadingResult.Progress(totalParsed))
                        } catch (e: Exception) {
                            trySend(LoadingResult.Error(e, totalParsed))
                        } finally {
                            if (pendingInsertions.decrementAndGet() == 0 && parsingComplete) {
                                trySend(LoadingResult.Complete(totalCount))
                                close()
                            }
                        }
                    }
                }

                override fun onComplete(count: Int) {
                    parsingComplete = true
                    totalCount = count
                    if (pendingInsertions.get() == 0) {
                        trySend(LoadingResult.Complete(count))
                        close()
                    }
                }

                override fun onError(error: Exception, entriesParsed: Int) {
                    trySend(LoadingResult.Error(error, entriesParsed))
                    close(error)
                }
            }

            // Parse diff file with same parser (doesn't clear DB)
            withContext(Dispatchers.IO) {
                parser.parseFileChunked(
                    filePath = filePath,
                    callback = callback,
                    maxEntries = -1,
                    chunkSize = 5000
                )
            }

            awaitClose { }
        } catch (e: Exception) {
            send(LoadingResult.Error(e, 0))
            close(e)
        }
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
                    addedAt = System.currentTimeMillis(),
                    listType = listType
                )
                dao.insert(entry)
            }
        }
    }

    override suspend fun removeFromFavorites(channel: String, theme: String, title: String) {
        withContext(Dispatchers.IO) {
            favoriteDao?.delete(channel, theme, title)
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
                val now = System.currentTimeMillis()
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
        }
    }
}
