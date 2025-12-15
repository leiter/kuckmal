package com.mediathekview.android.repository

import android.util.Log
import com.mediathekview.android.data.MediaListParser
import com.mediathekview.shared.database.MediaDao
import com.mediathekview.shared.database.MediaEntry
import com.mediathekview.shared.repository.MediaRepository.LoadingResult
import com.mediathekview.android.model.MediaEntry as ModelMediaEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Implementation of MediaRepository
 * Acts as single source of truth for all media data
 */
class MediaRepositoryImpl(
    private val mediaDao: MediaDao,
    private val parser: MediaListParser
) : MediaRepository {

    companion object {
        private const val TAG = "MediaRepository"
        private const val DEFAULT_CHUNK_SIZE = 5000
        private const val DEFAULT_BATCH_SIZE = 1000
    }

    // Detect low-memory device for adaptive search limits
    private val isLowMemory: Boolean by lazy {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        (maxMemory < 1024 * 1024 * 1024).also { isLow ->
            Log.d(TAG, "Device memory mode: ${if (isLow) "LOW" else "NORMAL"} (heap: ${maxMemory / 1024 / 1024}MB)")
        }
    }

    // Search cache instance for reducing database queries
    private val searchCache = SearchCache()

    /**
     * Get adaptive search limit based on device memory
     * Low-memory devices use smaller limits to reduce memory pressure
     */
    private fun getAdaptiveLimit(requestedLimit: Int): Int {
        return if (isLowMemory) {
            // Cap at 50 results on low-memory devices to reduce memory pressure
            minOf(requestedLimit, 200).also { adaptiveLimit ->
                if (adaptiveLimit < requestedLimit) {
                    Log.d(TAG, "Reducing search limit from $requestedLimit to $adaptiveLimit (low memory)")
                }
            }
        } else {
            requestedLimit
        }
    }

    /**
     * Filter search results to include only entries where ALL query words are present
     * (word order independent matching)
     *
     * Example: "Markus Lanz" and "Lanz Markus" will return the same results
     */
    private fun filterByAllWords(entries: List<MediaEntry>, query: String): List<MediaEntry> {
        // Split query into words and convert to lowercase for case-insensitive matching
        val queryWords = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }

        // If no words, return all entries
        if (queryWords.isEmpty()) {
            return entries
        }

        // Filter entries where ALL words appear in at least one of the searchable fields
        return entries.filter { entry ->
            val titleLower = entry.title.lowercase()
            val descriptionLower = entry.description.lowercase()
            val themeLower = entry.theme.lowercase()

            // Check that every word appears in at least one field
            queryWords.all { word ->
                titleLower.contains(word) || descriptionLower.contains(word) || themeLower.contains(word)
            }
        }
    }

    override fun getAllChannelsFlow(): Flow<List<String>> {
        return mediaDao.getAllChannelsFlow()
    }

    override fun getAllThemesFlow(
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): Flow<List<String>> {
        return mediaDao.getAllThemesFlow(minTimestamp, limit, offset)
    }

    override fun getThemesForChannelFlow(
        channel: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): Flow<List<String>> {
        return mediaDao.getThemesForChannelFlow(channel, minTimestamp, limit, offset)
    }

    override fun getTitlesForThemeFlow(
        theme: String,
        minTimestamp: Long
    ): Flow<List<String>> {
        return mediaDao.getTitlesForThemeFlow(theme, minTimestamp)
    }

    override fun getTitlesForChannelAndThemeFlow(
        channel: String,
        theme: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): Flow<List<String>> {
        return mediaDao.getTitlesForChannelAndThemeFlow(channel, theme, minTimestamp)
    }

    override fun getMediaEntryFlow(
        channel: String,
        theme: String,
        title: String
    ): Flow<MediaEntry?> {
        return mediaDao.getMediaEntryFlow(channel, theme, title)
    }

    override fun getAllThemesAsEntriesFlow(
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): Flow<List<MediaEntry>> {
        return mediaDao.getAllThemesAsEntriesFlow(minTimestamp, limit, offset)
    }

    override fun getThemesForChannelAsEntriesFlow(
        channel: String,
        minTimestamp: Long,
        limit: Int,
        offset: Int
    ): Flow<List<MediaEntry>> {
        return mediaDao.getThemesForChannelAsEntriesFlow(channel, minTimestamp, limit, offset)
    }

    override fun getTitlesForThemeAsEntriesFlow(
        theme: String,
        minTimestamp: Long
    ): Flow<List<MediaEntry>> {
        return mediaDao.getTitlesForThemeAsEntriesFlow(theme, minTimestamp)
    }

    override fun getTitlesForChannelAndThemeAsEntriesFlow(
        channel: String,
        theme: String,
        minTimestamp: Long
    ): Flow<List<MediaEntry>> {
        return mediaDao.getTitlesForChannelAndThemeAsEntriesFlow(channel, theme, minTimestamp)
    }

    override fun getMediaEntryByThemeAndTitleFlow(
        theme: String,
        title: String
    ): Flow<MediaEntry?> {
        return mediaDao.getMediaEntryByThemeAndTitleFlow(theme, title)
    }

    override fun getMediaEntryByTitleFlow(title: String): Flow<MediaEntry?> {
        return mediaDao.getMediaEntryByTitleFlow(title)
    }

    override suspend fun getRecentEntries(minTimestamp: Long, limit: Int): List<MediaEntry> {
        return withContext(Dispatchers.IO) {
            mediaDao.getRecentEntries(minTimestamp, limit)
        }
    }

    override suspend fun searchEntries(query: String, limit: Int): List<MediaEntry> {
        val adaptiveLimit = getAdaptiveLimit(limit)
        val cacheKey = searchCache.generateKey(query, limit = adaptiveLimit)

        // Check cache first
        searchCache.get(cacheKey)?.let {
            Log.d(TAG, "Cache hit for search: $query")
            return it
        }

        // Cache miss - query database
        return withContext(Dispatchers.IO) {
            // Get broader results using first word only (or whole query if single word)
            val queryWords = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val searchQuery = queryWords.firstOrNull() ?: query
            val rawResults = mediaDao.searchEntries(searchQuery, adaptiveLimit * 3)

            // Filter to ensure ALL words are present (order-independent)
            val results = filterByAllWords(rawResults, query).take(adaptiveLimit)

            searchCache.put(cacheKey, results)
            Log.d(TAG, "Cache miss for search: $query - filtered ${rawResults.size} to ${results.size} results")
            results
        }
    }

    override suspend fun searchEntriesByChannel(channel: String, query: String, limit: Int): List<MediaEntry> {
        val adaptiveLimit = getAdaptiveLimit(limit)
        val cacheKey = searchCache.generateKey(query, channel = channel, limit = adaptiveLimit)

        // Check cache first
        searchCache.get(cacheKey)?.let {
            Log.d(TAG, "Cache hit for channel search: $channel/$query")
            return it
        }

        // Cache miss - query database
        return withContext(Dispatchers.IO) {
            // Get broader results using first word only (or whole query if single word)
            val queryWords = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val searchQuery = queryWords.firstOrNull() ?: query
            val rawResults = mediaDao.searchEntriesByChannel(channel, searchQuery, adaptiveLimit * 3)

            // Filter to ensure ALL words are present (order-independent)
            val results = filterByAllWords(rawResults, query).take(adaptiveLimit)

            searchCache.put(cacheKey, results)
            results
        }
    }

    override suspend fun searchEntriesByTheme(theme: String, query: String, limit: Int): List<MediaEntry> {
        val adaptiveLimit = getAdaptiveLimit(limit)
        val cacheKey = searchCache.generateKey(query, theme = theme, limit = adaptiveLimit)

        // Check cache first
        searchCache.get(cacheKey)?.let {
            Log.d(TAG, "Cache hit for theme search: $theme/$query")
            return it
        }

        // Cache miss - query database
        return withContext(Dispatchers.IO) {
            // Get broader results using first word only (or whole query if single word)
            val queryWords = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val searchQuery = queryWords.firstOrNull() ?: query
            val rawResults = mediaDao.searchEntriesByTheme(theme, searchQuery, adaptiveLimit * 3)

            // Filter to ensure ALL words are present (order-independent)
            val results = filterByAllWords(rawResults, query).take(adaptiveLimit)

            searchCache.put(cacheKey, results)
            results
        }
    }

    override suspend fun searchEntriesByChannelAndTheme(channel: String, theme: String, query: String, limit: Int): List<MediaEntry> {
        val adaptiveLimit = getAdaptiveLimit(limit)
        val cacheKey = searchCache.generateKey(query, channel = channel, theme = theme, limit = adaptiveLimit)

        // Check cache first
        searchCache.get(cacheKey)?.let {
            Log.d(TAG, "Cache hit for channel+theme search: $channel/$theme/$query")
            return it
        }

        // Cache miss - query database
        return withContext(Dispatchers.IO) {
            // Get broader results using first word only (or whole query if single word)
            val queryWords = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val searchQuery = queryWords.firstOrNull() ?: query
            val rawResults = mediaDao.searchEntriesByChannelAndTheme(channel, theme, searchQuery, adaptiveLimit * 3)

            // Filter to ensure ALL words are present (order-independent)
            val results = filterByAllWords(rawResults, query).take(adaptiveLimit)

            searchCache.put(cacheKey, results)
            results
        }
    }

    override suspend fun searchEntriesWithOffset(query: String, limit: Int, offset: Int): List<MediaEntry> {
        val adaptiveLimit = getAdaptiveLimit(limit)
        val cacheKey = searchCache.generateKey(query, limit = adaptiveLimit, offset = offset)

        // Check cache first
        searchCache.get(cacheKey)?.let {
            Log.d(TAG, "Cache hit for offset search: $query (offset=$offset)")
            return it
        }

        // Cache miss - query database
        return withContext(Dispatchers.IO) {
            // Get broader results using first word only (or whole query if single word)
            val queryWords = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val searchQuery = queryWords.firstOrNull() ?: query
            val rawResults = mediaDao.searchEntriesWithOffset(searchQuery, adaptiveLimit * 3, offset)

            // Filter to ensure ALL words are present (order-independent)
            val results = filterByAllWords(rawResults, query).take(adaptiveLimit)

            searchCache.put(cacheKey, results)
            results
        }
    }

    override suspend fun searchEntriesByChannelWithOffset(channel: String, query: String, limit: Int, offset: Int): List<MediaEntry> {
        val adaptiveLimit = getAdaptiveLimit(limit)
        val cacheKey = searchCache.generateKey(query, channel = channel, limit = adaptiveLimit, offset = offset)

        // Check cache first
        searchCache.get(cacheKey)?.let {
            Log.d(TAG, "Cache hit for channel offset search: $channel/$query (offset=$offset)")
            return it
        }

        // Cache miss - query database
        return withContext(Dispatchers.IO) {
            // Get broader results using first word only (or whole query if single word)
            val queryWords = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val searchQuery = queryWords.firstOrNull() ?: query
            val rawResults = mediaDao.searchEntriesByChannelWithOffset(channel, searchQuery, adaptiveLimit * 3, offset)

            // Filter to ensure ALL words are present (order-independent)
            val results = filterByAllWords(rawResults, query).take(adaptiveLimit)

            searchCache.put(cacheKey, results)
            results
        }
    }

    override suspend fun searchEntriesByThemeWithOffset(theme: String, query: String, limit: Int, offset: Int): List<MediaEntry> {
        val adaptiveLimit = getAdaptiveLimit(limit)
        val cacheKey = searchCache.generateKey(query, theme = theme, limit = adaptiveLimit, offset = offset)

        // Check cache first
        searchCache.get(cacheKey)?.let {
            Log.d(TAG, "Cache hit for theme offset search: $theme/$query (offset=$offset)")
            return it
        }

        // Cache miss - query database
        return withContext(Dispatchers.IO) {
            // Get broader results using first word only (or whole query if single word)
            val queryWords = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val searchQuery = queryWords.firstOrNull() ?: query
            val rawResults = mediaDao.searchEntriesByThemeWithOffset(theme, searchQuery, adaptiveLimit * 3, offset)

            // Filter to ensure ALL words are present (order-independent)
            val results = filterByAllWords(rawResults, query).take(adaptiveLimit)

            searchCache.put(cacheKey, results)
            results
        }
    }

    override suspend fun searchEntriesByChannelAndThemeWithOffset(channel: String, theme: String, query: String, limit: Int, offset: Int): List<MediaEntry> {
        val adaptiveLimit = getAdaptiveLimit(limit)
        val cacheKey = searchCache.generateKey(query, channel = channel, theme = theme, limit = adaptiveLimit, offset = offset)

        // Check cache first
        searchCache.get(cacheKey)?.let {
            Log.d(TAG, "Cache hit for channel+theme offset search: $channel/$theme/$query (offset=$offset)")
            return it
        }

        // Cache miss - query database
        return withContext(Dispatchers.IO) {
            // Get broader results using first word only (or whole query if single word)
            val queryWords = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val searchQuery = queryWords.firstOrNull() ?: query
            val rawResults = mediaDao.searchEntriesByChannelAndThemeWithOffset(channel, theme, searchQuery, adaptiveLimit * 3, offset)

            // Filter to ensure ALL words are present (order-independent)
            val results = filterByAllWords(rawResults, query).take(adaptiveLimit)

            searchCache.put(cacheKey, results)
            results
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
            mediaDao.insertInBatches(entries, DEFAULT_BATCH_SIZE)
        }
    }

    override suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            mediaDao.deleteAll()
            // Clear cache when database is cleared
            searchCache.clear()
            Log.d(TAG, "Cleared database and search cache")
        }
    }

    override fun loadMediaListFromFile(filePath: String): Flow<LoadingResult> = callbackFlow {
        try {
            // Detect low-memory device for sequential vs parallel insertion strategy
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory()
            val isLowMemory = maxMemory < 1024 * 1024 * 1024 // < 1GB heap

            Log.d(TAG, "Loading with ${if (isLowMemory) "SEQUENTIAL" else "PARALLEL"} insertion (heap: ${maxMemory / 1024 / 1024}MB)")

            // Track pending insertions to coordinate completion
            val pendingInsertions = java.util.concurrent.atomic.AtomicInteger(0)
            var parsingComplete = false
            var totalCount = 0

            // Set up the chunk callback for database insertion
            val callback = object : MediaListParser.ChunkCallback {
                override fun onChunk(entries: List<ModelMediaEntry>, totalParsed: Int) {
                    // Convert model entries to database entries
                    val dbEntries = entries.mapNotNull { modelEntry ->
                        convertToDbEntry(modelEntry)
                    }

                    if (isLowMemory) {
                        // LOW-MEMORY MODE: Sequential blocking insertion
                        // This prevents memory accumulation by processing one chunk at a time
                        try {
                            // Block and insert synchronously to avoid parallel memory buildup
                            kotlinx.coroutines.runBlocking {
                                mediaDao.insertInBatches(dbEntries, DEFAULT_BATCH_SIZE)
                            }
                            // Emit progress after successful insertion
                            trySend(LoadingResult.Progress(totalParsed))
                            Log.d(TAG, "Processed $totalParsed entries (sequential)")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error inserting batch at $totalParsed", e)
                            trySend(LoadingResult.Error(e, totalParsed))
                        }
                    } else {
                        // HIGH-MEMORY MODE: Parallel async insertion (original behavior)
                        // Increment pending counter before launching
                        pendingInsertions.incrementAndGet()

                        // Launch coroutine for non-blocking database insertion
                        GlobalScope.launch(Dispatchers.IO) {
                            try {
                                mediaDao.insertInBatches(dbEntries, DEFAULT_BATCH_SIZE)
                                // Emit progress after successful insertion
                                trySend(LoadingResult.Progress(totalParsed))
                                Log.d(TAG, "Processed $totalParsed entries")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error inserting batch at $totalParsed", e)
                                trySend(LoadingResult.Error(e, totalParsed))
                            } finally {
                                // Decrement pending counter and check if all done
                                if (pendingInsertions.decrementAndGet() == 0 && parsingComplete) {
                                    Log.i(TAG, "All insertions complete for $totalCount entries")
                                    trySend(LoadingResult.Complete(totalCount))
                                    close()
                                }
                            }
                        }
                    }
                }

                override fun onComplete(count: Int) {
                    Log.i(TAG, "Successfully parsed $count entries")
                    parsingComplete = true
                    totalCount = count

                    if (isLowMemory) {
                        // LOW-MEMORY MODE: All insertions already complete (sequential)
                        Log.i(TAG, "All insertions complete for $count entries (sequential)")
                        trySend(LoadingResult.Complete(count))
                        close()
                    } else {
                        // HIGH-MEMORY MODE: Wait for async insertions to complete
                        if (pendingInsertions.get() == 0) {
                            Log.i(TAG, "All insertions complete for $count entries")
                            trySend(LoadingResult.Complete(count))
                            close()
                        }
                    }
                }

                override fun onError(error: Exception, entriesParsed: Int) {
                    Log.e(TAG, "Error parsing at entry $entriesParsed", error)
                    // Emit error
                    trySend(LoadingResult.Error(error, entriesParsed))
                    close(error)
                }
            }

            // Clear existing data and cache
            withContext(Dispatchers.IO) {
                mediaDao.deleteAll()
                searchCache.clear()
                Log.d(TAG, "Cleared database and cache before loading new data")
            }

            // Parse file with chunked processing
            withContext(Dispatchers.IO) {
                parser.parseFileChunked(
                    filePath = filePath,
                    callback = callback,
                    maxEntries = -1, // Unlimited by default
                    chunkSize = DEFAULT_CHUNK_SIZE
                )
            }

            awaitClose {
                Log.d(TAG, "Flow closed for loadMediaListFromFile")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading media list", e)
            send(LoadingResult.Error(e, 0))
            close(e)
        }
    }.flowOn(Dispatchers.IO)

    override fun applyDiffToDatabase(filePath: String): Flow<LoadingResult> = callbackFlow {
        try {
            // Detect low-memory device for sequential vs parallel insertion strategy
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory()
            val isLowMemory = maxMemory < 1024 * 1024 * 1024 // < 1GB heap

            Log.d(TAG, "Applying diff with ${if (isLowMemory) "SEQUENTIAL" else "PARALLEL"} insertion (heap: ${maxMemory / 1024 / 1024}MB)")

            // Track pending insertions to coordinate completion
            val pendingInsertions = java.util.concurrent.atomic.AtomicInteger(0)
            var parsingComplete = false
            var totalCount = 0

            // Set up the chunk callback for database insertion/update
            val callback = object : MediaListParser.ChunkCallback {
                override fun onChunk(entries: List<ModelMediaEntry>, totalParsed: Int) {
                    // Convert model entries to database entries
                    val dbEntries = entries.mapNotNull { modelEntry ->
                        convertToDbEntry(modelEntry)
                    }

                    if (isLowMemory) {
                        // LOW-MEMORY MODE: Sequential blocking insertion
                        try {
                            // Use INSERT OR REPLACE to update existing entries or insert new ones
                            kotlinx.coroutines.runBlocking {
                                mediaDao.insertInBatches(dbEntries, DEFAULT_BATCH_SIZE)
                            }
                            // Emit progress after successful insertion
                            trySend(LoadingResult.Progress(totalParsed))
                            Log.d(TAG, "Applied $totalParsed diff entries (sequential)")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error applying diff batch at $totalParsed", e)
                            trySend(LoadingResult.Error(e, totalParsed))
                        }
                    } else {
                        // HIGH-MEMORY MODE: Parallel async insertion
                        pendingInsertions.incrementAndGet()

                        GlobalScope.launch(Dispatchers.IO) {
                            try {
                                // Use INSERT OR REPLACE to update existing entries or insert new ones
                                mediaDao.insertInBatches(dbEntries, DEFAULT_BATCH_SIZE)
                                trySend(LoadingResult.Progress(totalParsed))
                                Log.d(TAG, "Applied $totalParsed diff entries")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error applying diff batch at $totalParsed", e)
                                trySend(LoadingResult.Error(e, totalParsed))
                            } finally {
                                if (pendingInsertions.decrementAndGet() == 0 && parsingComplete) {
                                    Log.i(TAG, "All diff insertions complete for $totalCount entries")
                                    trySend(LoadingResult.Complete(totalCount))
                                    close()
                                }
                            }
                        }
                    }
                }

                override fun onComplete(count: Int) {
                    Log.i(TAG, "Successfully parsed $count diff entries")
                    parsingComplete = true
                    totalCount = count

                    // Clear search cache after applying diff - data has changed
                    searchCache.clear()
                    Log.d(TAG, "Cleared search cache after diff application")

                    if (isLowMemory) {
                        // LOW-MEMORY MODE: All insertions already complete
                        Log.i(TAG, "All diff insertions complete for $count entries (sequential)")
                        trySend(LoadingResult.Complete(count))
                        close()
                    } else {
                        // HIGH-MEMORY MODE: Wait for async insertions
                        if (pendingInsertions.get() == 0) {
                            Log.i(TAG, "All diff insertions complete for $count entries")
                            trySend(LoadingResult.Complete(count))
                            close()
                        }
                    }
                }

                override fun onError(error: Exception, entriesParsed: Int) {
                    Log.e(TAG, "Error parsing diff at entry $entriesParsed", error)
                    trySend(LoadingResult.Error(error, entriesParsed))
                    close(error)
                }
            }

            // Parse diff file with chunked processing (does NOT clear database)
            withContext(Dispatchers.IO) {
                parser.parseFileChunked(
                    filePath = filePath,
                    callback = callback,
                    maxEntries = -1, // Unlimited
                    chunkSize = DEFAULT_CHUNK_SIZE
                )
            }

            awaitClose {
                Log.d(TAG, "Flow closed for applyDiffToDatabase")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying diff", e)
            send(LoadingResult.Error(e, 0))
            close(e)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun checkAndLoadMediaList(privatePath: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val mediaListFile = File(privatePath, "Filmliste-akt.json")
                if (!mediaListFile.exists()) {
                    Log.w(TAG, "Media list file not found: ${mediaListFile.absolutePath}")
                    return@withContext false
                }

                var success = false
                loadMediaListFromFile(mediaListFile.absolutePath).collect { result ->
                    when (result) {
                        is LoadingResult.Complete -> {
                            success = result.totalEntries > 0
                        }
                        is LoadingResult.Error -> {
                            success = false
                        }
                        else -> { /* Progress - do nothing */ }
                    }
                }
                success
            } catch (e: Exception) {
                Log.e(TAG, "Error checking/loading media list", e)
                false
            }
        }

    override fun getEntriesFlow(channel: String, theme: String): Flow<List<MediaEntry>> {
        return when {
            channel.isNotEmpty() && theme.isNotEmpty() -> {
                // Get entries for specific channel and theme
                mediaDao.getTitlesForChannelAndThemeFlow(channel, theme, 0)
                    .let { titlesFlow ->
                        kotlinx.coroutines.flow.flow {
                            titlesFlow.collect { titles ->
                                val entries = titles.mapNotNull { title ->
                                    mediaDao.getMediaEntry(channel, theme, title)
                                }
                                emit(entries)
                            }
                        }
                    }
            }
            channel.isNotEmpty() -> {
                // Get entries for specific channel
                kotlinx.coroutines.flow.flow {
                    val entries = mediaDao.getEntriesByChannel(channel, 0, 1200, 0)
                    emit(entries)
                }
            }
            else -> {
                // Get recent entries if no filters
                kotlinx.coroutines.flow.flow {
                    val sinceTimestamp = System.currentTimeMillis() / 1000 - (7 * 24 * 60 * 60)
                    val entries = mediaDao.getRecentEntries(sinceTimestamp, 100)
                    emit(entries)
                }
            }
        }
    }

    /**
     * Convert model MediaEntry to database MediaEntry
     */
    private fun convertToDbEntry(modelEntry: ModelMediaEntry): MediaEntry? {
        return try {
            MediaEntry(
                channel = modelEntry.channel,
                theme = modelEntry.theme,
                title = modelEntry.title,
                date = modelEntry.date,
                time = modelEntry.time,
                duration = modelEntry.duration,
                sizeMB = modelEntry.sizeMB,
                description = modelEntry.description,
                url = modelEntry.url,
                website = modelEntry.website,
                subtitleUrl = modelEntry.subtitleUrl,
                smallUrl = modelEntry.urlSmall,
                hdUrl = modelEntry.urlHd,
                timestamp = modelEntry.dateL,
                geo = modelEntry.geo,
                isNew = modelEntry.isNew
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting entry: ${e.message}")
            null
        }
    }

    /**
     * Get cache statistics for monitoring
     */
    override fun getCacheStats(): SearchCache.CacheStats {
        return searchCache.getStats()
    }
}