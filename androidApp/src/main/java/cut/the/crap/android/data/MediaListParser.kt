package cut.the.crap.android.data

import android.util.Log
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import cut.the.crap.android.model.MediaEntry
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

/**
 * Parser for MediathekView film list JSON files.
 * Streams and parses large JSON files (685K+ entries) with memory efficiency.
 * Supports chunked parsing to avoid OutOfMemoryError.
 */
class MediaListParser {
    private val mediaList = mutableListOf<MediaEntry>()
    private var previousEntry: MediaEntry? = null
    private var limitDate: Long = 0

    /**
     * Callback interface for chunked parsing
     */
    interface ChunkCallback {
        fun onChunk(entries: List<MediaEntry>, totalParsed: Int)
        fun onComplete(totalEntries: Int)
        fun onError(error: Exception, entriesParsed: Int)
    }

    /**
     * Set the date limit for filtering. Updates all existing entries.
     */
    fun setLimitDate(limitDate: Long) {
        this.limitDate = limitDate
        // Update existing entries
        mediaList.forEach { entry ->
            entry.inTimePeriod = entry.dateL > limitDate
        }
    }

    /**
     * Parse JSON film list from file with chunked callback for database insertion.
     * This avoids OutOfMemoryError by processing entries in chunks.
     *
     * @param filePath Path to the JSON file
     * @param callback Callback for chunk processing
     * @param maxEntries Maximum number of entries to parse (-1 for unlimited)
     * @param chunkSize Size of chunks for processing (default 5000)
     */
    fun parseFileChunked(
        filePath: String,
        callback: ChunkCallback,
        maxEntries: Int = DEFAULT_MAX_ENTRIES,
        chunkSize: Int = DEFAULT_CHUNK_SIZE
    ) {
        previousEntry = null
        var count = 0
        val chunk = mutableListOf<MediaEntry>()

        try {
            FileInputStream(filePath).use { fis ->
                InputStreamReader(fis, StandardCharsets.UTF_8).use { isr ->
                    JsonReader(isr).use { reader ->
                        reader.beginObject()

                        while (reader.hasNext()) {
                            val name = reader.nextName()

                            if (name == "X") {
                                // This is a media entry
                                val entry = readMediaEntry(reader)
                                if (entry != null) {
                                    entry.inTimePeriod = entry.dateL > limitDate
                                    chunk.add(entry)
                                    previousEntry = entry
                                    count++

                                    // When chunk is full, send to callback and release memory
                                    if (chunk.size >= chunkSize) {
                                        callback.onChunk(chunk.toList(), count)
                                        chunk.clear()

                                        // Suggest garbage collection to free memory
                                        if (count % (chunkSize * 10) == 0) {
                                            System.gc()
                                        }

                                        // Log progress
                                        if (count % LOG_INTERVAL == 0) {
                                            Log.i(TAG, "Parsed $count entries so far...")
                                        }
                                    }

                                    // Check if we've reached the max entries limit
                                    if (maxEntries != UNLIMITED && count >= maxEntries) {
                                        Log.i(TAG, "Reached maximum entry limit of $maxEntries")
                                        break
                                    }
                                }
                            } else {
                                // Skip other fields (Filmliste metadata)
                                reader.skipValue()
                            }
                        }

                        reader.endObject()

                        // Send remaining entries
                        if (chunk.isNotEmpty()) {
                            callback.onChunk(chunk.toList(), count)
                            chunk.clear()
                        }
                    }
                }
            }

            Log.i(TAG, "Successfully parsed $count media entries in chunks")
            callback.onComplete(count)

        } catch (e: IOException) {
            Log.e(TAG, "Error parsing media list at entry $count", e)
            callback.onError(e, count)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory after parsing $count entries", e)
            callback.onError(Exception("Out of memory", e), count)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing media list at entry $count", e)
            callback.onError(e, count)
        }
    }

    /**
     * Parse JSON film list from file.
     * Returns 0 on success, error code otherwise.
     * NOTE: This loads all entries into memory - use parseFileChunked() for large files.
     */
    fun parseFile(filePath: String): Int {
        mediaList.clear()
        previousEntry = null
        var count = 0

        try {
            FileInputStream(filePath).use { fis ->
                InputStreamReader(fis, StandardCharsets.UTF_8).use { isr ->
                    JsonReader(isr).use { reader ->
                        reader.beginObject()

                        while (reader.hasNext()) {
                            val name = reader.nextName()

                            if (name == "X") {
                                // This is a media entry
                                val entry = readMediaEntry(reader)
                                if (entry != null) {
                                    entry.inTimePeriod = entry.dateL > limitDate
                                    mediaList.add(entry)
                                    previousEntry = entry
                                    count++

                                    // Log progress
                                    if (count % LOG_INTERVAL == 0) {
                                        Log.i(TAG, "Parsed $count entries so far...")
                                    }

                                    // Stop parsing after reaching maximum entries
                                    if (count >= MAX_ENTRIES) {
                                        Log.i(TAG, "Reached maximum entry limit of $MAX_ENTRIES, stopping parse")
                                        // Skip remaining entries to properly close JSON
                                        while (reader.hasNext()) {
                                            reader.nextName()
                                            reader.skipValue()
                                        }
                                        break
                                    }
                                }
                            } else {
                                // Skip other fields (Filmliste metadata)
                                reader.skipValue()
                            }
                        }

                        reader.endObject()
                    }
                }
            }

            Log.i(TAG, "Parsed ${mediaList.size} media entries successfully")
            return 0 // Success

        } catch (e: IOException) {
            Log.e(TAG, "Error parsing media list at entry $count", e)
            return 3 // Read error
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory after parsing $count entries", e)
            // Keep what we have - don't clear
            Log.i(TAG, "Keeping ${mediaList.size} entries that were successfully parsed")
            return 0 // Return success with partial data
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing media list at entry $count", e)
            return 5 // Premature end
        }
    }

    /**
     * Read a single media entry from JSON array
     */
    private fun readMediaEntry(reader: JsonReader): MediaEntry? {
        val values = mutableListOf<String>()

        reader.beginArray()
        while (reader.hasNext()) {
            when (reader.peek()) {
                JsonToken.NULL -> {
                    reader.nextNull()
                    values.add("")
                }
                else -> values.add(reader.nextString())
            }
        }
        reader.endArray()

        return MediaEntry.fromArray(values.toTypedArray(), previousEntry)
    }

    /**
     * Get complete media information as array
     */
    fun getMediaInfo(channel: String, theme: String, title: String): Array<String>? {
        val entry = mediaList.find { entry ->
            entry.channel == channel && entry.theme == theme && entry.title == title
        } ?: return null

        return arrayOf(
            entry.channel,
            entry.theme,
            entry.title,
            entry.date,
            entry.time,
            entry.duration,
            entry.sizeMB,
            entry.description,
            entry.url,
            entry.urlSmall,
            entry.dateL.toString()
        )
    }

    /**
     * Get list of themes for a specific channel with pagination
     */
    fun getThemesOfChannel(channel: String, part: Int, maxPart: Int): List<String> {
        val themes = mutableListOf<String>()
        var lastTheme = ""
        var foundChannel = false

        for (entry in mediaList) {
            // Check if this entry belongs to the channel we're looking for
            if (entry.channel == channel) {
                foundChannel = true

                if (entry.inTimePeriod && entry.theme.isNotEmpty() && entry.theme != lastTheme) {
                    themes.add(entry.theme)
                    lastTheme = entry.theme

                    if (themes.size >= MAX_ITEMS) break
                }
            } else if (foundChannel && entry.channel.isNotEmpty()) {
                // We've moved to a different channel, stop
                break
            }
            // Otherwise continue - either haven't found the channel yet, or it's an inherited entry
        }

        // Return the requested part
        val itemsPerPart = maxPart
        val start = part * itemsPerPart
        val end = (start + itemsPerPart).coerceAtMost(themes.size)

        if (start >= themes.size) return emptyList()

        val result = themes.subList(start, end).toMutableList()

        // Add "more" indicator if there's more data
        if (end < themes.size) {
            result.add("+++ more +++")
        }

        return result
    }

    /**
     * Get list of titles for a specific theme
     */
    fun getTitlesOfTheme(channel: String, theme: String): List<String> {
        val titles = mutableListOf<String>()

        for (entry in mediaList) {
            if (entry.channel != channel && entry.channel.isNotEmpty()) {
                continue
            }

            if (entry.theme == theme && entry.inTimePeriod) {
                titles.add(entry.title)

                if (titles.size >= MAX_ITEMS) break
            }
        }

        return titles
    }

    /**
     * Get all unique themes across all channels
     */
    fun getAllThemes(part: Int, maxPart: Int): List<String> {
        val uniqueThemes = mutableSetOf<String>()

        for (entry in mediaList) {
            if (entry.theme.isNotEmpty() && entry.inTimePeriod) {
                uniqueThemes.add(entry.theme)
            }
        }

        val sortedThemes = uniqueThemes.sorted()

        // Handle pagination
        val start = part * maxPart
        val end = (start + maxPart).coerceAtMost(sortedThemes.size)

        if (start >= sortedThemes.size) return emptyList()

        val result = sortedThemes.subList(start, end).toMutableList()

        // Add "more" indicator if there's more data
        if (end < sortedThemes.size) {
            result.add("+++ more +++")
        }

        return result
    }

    /**
     * Get total number of parsed entries
     */
    fun getNumberOfLines(): Int = mediaList.size

    /**
     * Clear all parsed data
     */
    fun clean() {
        mediaList.clear()
        previousEntry = null
    }

    /**
     * Get the full media list (use with caution - large data)
     */
    fun getMediaList(): List<MediaEntry> = mediaList

    // Cache-friendly methods that work with provided list

    /**
     * Get themes from a cached list with unique filtering
     * Memory-optimized version with MAX_ITEMS limit to prevent OOM
     */
    fun getThemesOfChannelFromCache(
        cachedList: List<MediaEntry>,
        channel: String,
        part: Int,
        maxPart: Int
    ): List<String> {
        // Use LinkedHashSet to maintain insertion order and avoid duplicates
        // Stop collecting after MAX_ITEMS to prevent OOM
        val uniqueThemes = LinkedHashSet<String>(MAX_ITEMS)
        var foundChannel = false

        for (entry in cachedList) {
            if (entry.channel == channel) {
                foundChannel = true
                if (entry.theme.isNotEmpty() && entry.inTimePeriod) {
                    uniqueThemes.add(entry.theme)
                    if (uniqueThemes.size >= MAX_ITEMS) break
                }
            } else if (foundChannel && entry.channel.isNotEmpty()) {
                break
            }
        }

        val themes = uniqueThemes.sorted()

        // Handle pagination
        val start = part * maxPart
        val end = (start + maxPart).coerceAtMost(themes.size)

        if (start >= themes.size) return emptyList()

        val result = themes.subList(start, end).toMutableList()

        // Add "more" indicator if there's more data
        if (end < themes.size) {
            result.add("+++ more +++")
        }

        return result
    }

    /**
     * Get all themes from cached list (across all channels)
     * Memory-optimized version with MAX_ITEMS limit to prevent OOM
     */
    fun getAllThemesFromCache(
        cachedList: List<MediaEntry>,
        part: Int,
        maxPart: Int
    ): List<String> {
        // Use LinkedHashSet to maintain insertion order and avoid duplicates
        // Stop collecting after MAX_ITEMS to prevent OOM
        val uniqueThemes = LinkedHashSet<String>(MAX_ITEMS)

        for (entry in cachedList) {
            if (entry.theme.isNotEmpty() && entry.inTimePeriod) {
                uniqueThemes.add(entry.theme)
                if (uniqueThemes.size >= MAX_ITEMS) break
            }
        }

        val themes = uniqueThemes.sorted()

        // Handle pagination
        val start = part * maxPart
        val end = (start + maxPart).coerceAtMost(themes.size)

        if (start >= themes.size) return emptyList()

        val result = themes.subList(start, end).toMutableList()

        // Add "more" indicator if there's more data
        if (end < themes.size) {
            result.add("+++ more +++")
        }

        return result
    }

    /**
     * Get titles from a cached list
     */
    fun getTitlesOfThemeFromCache(
        cachedList: List<MediaEntry>,
        channel: String,
        theme: String
    ): List<String> {
        val titles = mutableListOf<String>()

        for (entry in cachedList) {
            if (entry.channel != channel && entry.channel.isNotEmpty()) {
                continue
            }

            if (entry.theme == theme && entry.inTimePeriod) {
                titles.add(entry.title)

                if (titles.size >= MAX_ITEMS) break
            }
        }

        return titles
    }

    /**
     * Get media info from a cached list with formatted date
     */
    fun getMediaInfoFromCache(
        cachedList: List<MediaEntry>,
        channel: String,
        theme: String,
        title: String
    ): Array<String>? {
        val entry = cachedList.find { entry ->
            entry.channel == channel && entry.theme == theme && entry.title == title
        } ?: return null

        // Use the parsed date if available, otherwise format from timestamp
        val displayDate = if (entry.date.isNotEmpty()) {
            entry.date
        } else {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
            dateFormat.format(Date(entry.dateL * 1000))
        }

        return arrayOf(
            entry.channel,
            entry.theme,
            entry.title,
            displayDate,
            entry.time,
            entry.duration,
            entry.sizeMB,
            entry.description,
            entry.url,
            entry.urlSmall,
            entry.dateL.toString()
        )
    }
}

private const val TAG = "MediaListParser"
private const val MAX_ITEMS = 1200 // Limit for UI performance
private const val MAX_ENTRIES = 100_000 // Default limit for in-memory parsing
private const val UNLIMITED = -1 // No limit on entries
private const val DEFAULT_MAX_ENTRIES = UNLIMITED // Default to unlimited
private const val LOG_INTERVAL = 100_000 // Log progress every 100K entries
private const val DEFAULT_CHUNK_SIZE = 5000 // Default chunk size for DB insertion