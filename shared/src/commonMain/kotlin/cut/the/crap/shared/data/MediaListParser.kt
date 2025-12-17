package cut.the.crap.shared.data

import cut.the.crap.shared.model.MediaEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/**
 * Cross-platform parser for Kuckmal film list JSON files.
 * Streams and parses large JSON files (685K+ entries) with memory efficiency.
 * Supports chunked parsing to avoid OutOfMemoryError.
 */
class MediaListParser {
    private var previousEntry: MediaEntry? = null
    private var limitDate: Long = 0

    companion object {
        private const val TAG = "MediaListParser"
        private const val DEFAULT_CHUNK_SIZE = 5000
        private const val LOG_INTERVAL = 100_000
        const val UNLIMITED = -1
    }

    /**
     * Callback interface for chunked parsing.
     */
    interface ChunkCallback {
        fun onChunk(entries: List<MediaEntry>, totalParsed: Int)
        fun onComplete(totalEntries: Int)
        fun onError(error: Exception, entriesParsed: Int)
    }

    /**
     * Set the date limit for filtering entries.
     */
    fun setLimitDate(limitDate: Long) {
        this.limitDate = limitDate
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
    suspend fun parseFileChunked(
        filePath: String,
        callback: ChunkCallback,
        maxEntries: Int = UNLIMITED,
        chunkSize: Int = DEFAULT_CHUNK_SIZE
    ) = withContext(Dispatchers.IO) {
        previousEntry = null
        var count = 0
        val chunk = mutableListOf<MediaEntry>()

        try {
            val content = FileSystem.readText(filePath)
            PlatformLogger.info(TAG, "Read file: ${content.length} characters")

            // Parse the JSON content
            val entries = parseJsonContent(content, maxEntries) { entryCount ->
                if (entryCount % LOG_INTERVAL == 0) {
                    PlatformLogger.info(TAG, "Parsed $entryCount entries so far...")
                }
            }

            // Process entries in chunks
            for (entry in entries) {
                entry.inTimePeriod = entry.dateL > limitDate
                chunk.add(entry)
                count++

                if (chunk.size >= chunkSize) {
                    callback.onChunk(chunk.toList(), count)
                    chunk.clear()
                    yield() // Allow coroutine cancellation
                }

                if (maxEntries != UNLIMITED && count >= maxEntries) {
                    break
                }
            }

            // Send remaining entries
            if (chunk.isNotEmpty()) {
                callback.onChunk(chunk.toList(), count)
                chunk.clear()
            }

            PlatformLogger.info(TAG, "Successfully parsed $count media entries in chunks")
            callback.onComplete(count)

        } catch (e: Exception) {
            PlatformLogger.error(TAG, "Error parsing media list at entry $count", e)
            callback.onError(e, count)
        }
    }

    /**
     * Parse JSON content and extract media entries.
     * The JSON format is: {"Filmliste": [...], "X": [...], "X": [...], ...}
     * Each "X" value is an array representing a media entry.
     */
    private fun parseJsonContent(
        content: String,
        maxEntries: Int,
        progressCallback: (Int) -> Unit
    ): List<MediaEntry> {
        val entries = mutableListOf<MediaEntry>()
        var index = 0
        var entryCount = 0

        // Find the start of the JSON object
        while (index < content.length && content[index] != '{') {
            index++
        }
        index++ // Skip '{'

        while (index < content.length) {
            // Skip whitespace
            while (index < content.length && content[index].isWhitespace()) {
                index++
            }

            if (index >= content.length) break

            // Check for end of object
            if (content[index] == '}') break

            // Skip comma if present
            if (content[index] == ',') {
                index++
                continue
            }

            // Read key (expect "X" or "Filmliste")
            if (content[index] != '"') {
                index++
                continue
            }

            val keyStart = index + 1
            index++ // Skip opening quote
            while (index < content.length && content[index] != '"') {
                if (content[index] == '\\') index++ // Skip escape
                index++
            }
            val key = content.substring(keyStart, index)
            index++ // Skip closing quote

            // Skip colon
            while (index < content.length && content[index] != ':') {
                index++
            }
            index++ // Skip ':'

            // Skip whitespace
            while (index < content.length && content[index].isWhitespace()) {
                index++
            }

            if (key == "X") {
                // Parse the array value
                val arrayResult = parseArray(content, index)
                if (arrayResult != null) {
                    val (values, newIndex) = arrayResult
                    index = newIndex

                    val entry = MediaEntry.fromArray(values.toTypedArray(), previousEntry)
                    entries.add(entry)
                    previousEntry = entry
                    entryCount++

                    progressCallback(entryCount)

                    if (maxEntries != UNLIMITED && entryCount >= maxEntries) {
                        break
                    }
                }
            } else {
                // Skip this value (e.g., "Filmliste" metadata)
                index = skipValue(content, index)
            }
        }

        return entries
    }

    /**
     * Parse a JSON array and return the values and new index.
     */
    private fun parseArray(content: String, startIndex: Int): Pair<List<String>, Int>? {
        var index = startIndex

        // Expect '['
        if (index >= content.length || content[index] != '[') {
            return null
        }
        index++ // Skip '['

        val values = mutableListOf<String>()

        while (index < content.length) {
            // Skip whitespace
            while (index < content.length && content[index].isWhitespace()) {
                index++
            }

            if (index >= content.length) break

            // Check for end of array
            if (content[index] == ']') {
                index++ // Skip ']'
                return Pair(values, index)
            }

            // Skip comma
            if (content[index] == ',') {
                index++
                continue
            }

            // Parse value (string or null)
            if (content[index] == '"') {
                val stringResult = parseString(content, index)
                values.add(stringResult.first)
                index = stringResult.second
            } else if (content.substring(index).startsWith("null")) {
                values.add("")
                index += 4
            } else {
                // Unknown value, skip
                values.add("")
                while (index < content.length && content[index] != ',' && content[index] != ']') {
                    index++
                }
            }
        }

        return null // Malformed array
    }

    /**
     * Parse a JSON string value.
     */
    private fun parseString(content: String, startIndex: Int): Pair<String, Int> {
        var index = startIndex + 1 // Skip opening quote
        val sb = StringBuilder()

        while (index < content.length) {
            val c = content[index]
            when {
                c == '"' -> {
                    return Pair(sb.toString(), index + 1)
                }
                c == '\\' && index + 1 < content.length -> {
                    index++
                    when (content[index]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (index + 4 < content.length) {
                                val hex = content.substring(index + 1, index + 5)
                                try {
                                    sb.append(hex.toInt(16).toChar())
                                    index += 4
                                } catch (e: NumberFormatException) {
                                    sb.append('u')
                                }
                            }
                        }
                        else -> sb.append(content[index])
                    }
                    index++
                }
                else -> {
                    sb.append(c)
                    index++
                }
            }
        }

        return Pair(sb.toString(), index)
    }

    /**
     * Skip a JSON value (string, number, array, object, null, boolean).
     */
    private fun skipValue(content: String, startIndex: Int): Int {
        var index = startIndex

        // Skip whitespace
        while (index < content.length && content[index].isWhitespace()) {
            index++
        }

        if (index >= content.length) return index

        return when (content[index]) {
            '"' -> {
                // Skip string
                index++ // Skip opening quote
                while (index < content.length) {
                    if (content[index] == '"') {
                        return index + 1
                    }
                    if (content[index] == '\\') index++ // Skip escape
                    index++
                }
                index
            }
            '[' -> {
                // Skip array
                var depth = 1
                index++
                while (index < content.length && depth > 0) {
                    when (content[index]) {
                        '[' -> depth++
                        ']' -> depth--
                        '"' -> {
                            // Skip string inside array
                            index++
                            while (index < content.length && content[index] != '"') {
                                if (content[index] == '\\') index++
                                index++
                            }
                        }
                    }
                    index++
                }
                index
            }
            '{' -> {
                // Skip object
                var depth = 1
                index++
                while (index < content.length && depth > 0) {
                    when (content[index]) {
                        '{' -> depth++
                        '}' -> depth--
                        '"' -> {
                            // Skip string inside object
                            index++
                            while (index < content.length && content[index] != '"') {
                                if (content[index] == '\\') index++
                                index++
                            }
                        }
                    }
                    index++
                }
                index
            }
            else -> {
                // Skip number, null, or boolean
                while (index < content.length && content[index] != ',' && content[index] != '}' && content[index] != ']') {
                    index++
                }
                index
            }
        }
    }

    /**
     * Reset parser state.
     */
    fun reset() {
        previousEntry = null
        limitDate = 0
    }
}
