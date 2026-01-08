package cut.the.crap.shared.data

import cut.the.crap.shared.model.MediaEntry
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.posix.memcpy
import platform.posix.uint8_tVar

/**
 * iOS-specific streaming parser for media list JSON files.
 * Uses NSInputStream to read in chunks - true streaming, not loading entire file into memory.
 */
@OptIn(ExperimentalForeignApi::class)
class IosStreamingMediaListParser {
    private var previousEntry: MediaEntry? = null
    private var limitDate: Long = 0

    companion object {
        private const val TAG = "IosStreamingParser"
        private const val BUFFER_SIZE = 64 * 1024 // 64KB read buffer
        private const val LOG_INTERVAL = 50_000
    }

    fun setLimitDate(limitDate: Long) {
        this.limitDate = limitDate
    }

    /**
     * Parse JSON file using callback approach (no sequences).
     * Calls onEntry for each parsed entry.
     *
     * @param filePath Path to the JSON file
     * @param onEntry Called for each parsed entry
     * @param onProgress Called periodically with progress (entry count)
     * @param maxEntries Maximum entries to parse (-1 for unlimited)
     * @return Total number of entries parsed
     */
    fun parseFileWithCallback(
        filePath: String,
        onEntry: (MediaEntry) -> Unit,
        onProgress: (Int) -> Unit = {},
        maxEntries: Int = -1
    ): Int {
        PlatformLogger.info(TAG, "=== STARTING parseFileWithCallback ===")
        PlatformLogger.info(TAG, "Opening file: $filePath")

        previousEntry = null
        var totalCount = 0
        var bufferReadCount = 0

        val inputStream = NSInputStream.inputStreamWithFileAtPath(filePath)
        if (inputStream == null) {
            PlatformLogger.error(TAG, "FAILED: Cannot open file: $filePath")
            return 0
        }

        PlatformLogger.info(TAG, "File stream created, opening...")

        try {
            inputStream.open()
            PlatformLogger.info(TAG, "File stream opened successfully")
        } catch (openError: Exception) {
            PlatformLogger.error(TAG, "FAILED to open file stream", openError)
            return 0
        }

        try {
            val buffer = ByteArray(BUFFER_SIZE)

            PlatformLogger.info(TAG, "Starting to read buffer chunks...")

            while (inputStream.hasBytesAvailable) {
                bufferReadCount++

                val bytesRead = try {
                    buffer.usePinned { pinned ->
                        inputStream.read(pinned.addressOf(0).reinterpret<uint8_tVar>(), BUFFER_SIZE.toULong()).toInt()
                    }
                } catch (readError: Exception) {
                    PlatformLogger.error(TAG, "FAILED to read buffer #$bufferReadCount", readError)
                    throw readError
                }

                if (bytesRead <= 0) {
                    PlatformLogger.info(TAG, "End of stream after $bufferReadCount reads")
                    break
                }

                PlatformLogger.info(TAG, "Buffer #$bufferReadCount: $bytesRead bytes read")

                // Convert to string using NSString for iOS compatibility
                val chunk = try {
                    val nsData = buffer.usePinned { pinned ->
                        NSData.dataWithBytes(pinned.addressOf(0), bytesRead.toULong())
                    }
                    val nsString = NSString.create(nsData, NSUTF8StringEncoding)
                    if (nsString == null) {
                        PlatformLogger.error(TAG, "Failed to decode buffer #$bufferReadCount as UTF-8")
                        continue
                    }
                    nsString.toString()
                } catch (decodeError: Exception) {
                    PlatformLogger.error(TAG, "FAILED to decode buffer #$bufferReadCount", decodeError)
                    continue
                }

                PlatformLogger.info(TAG, "Buffer #$bufferReadCount decoded: ${chunk.length} chars")

                // Verify string is accessible before scanning
                val firstChar = try {
                    chunk[0]
                } catch (e: Exception) {
                    PlatformLogger.error(TAG, "Buffer #$bufferReadCount: FAILED to access first char!", e)
                    continue
                }
                val lastChar = try {
                    chunk[chunk.length - 1]
                } catch (e: Exception) {
                    PlatformLogger.error(TAG, "Buffer #$bufferReadCount: FAILED to access last char!", e)
                    continue
                }
                PlatformLogger.info(TAG, "Buffer #$bufferReadCount: first='$firstChar' last='$lastChar', starting scan...")

                // Process characters using indexOf instead of char-by-char
                var i = 0
                var xFoundInBuffer = 0

                // Try using indexOf to find "X": patterns - more iOS friendly
                var searchStart = 0
                while (true) {
                    val xPos = chunk.indexOf("\"X\":", searchStart)
                    if (xPos == -1) break

                    xFoundInBuffer++
                    if (xFoundInBuffer <= 3 || xFoundInBuffer % 500 == 0) {
                        PlatformLogger.info(TAG, "Buffer #$bufferReadCount: found X #$xFoundInBuffer at pos $xPos")
                    }

                    // Find the [ after "X":
                    var bracketPos = xPos + 4
                    while (bracketPos < chunk.length && chunk[bracketPos].isWhitespace()) bracketPos++

                    if (bracketPos < chunk.length && chunk[bracketPos] == '[') {
                        // Find matching ]
                        var depth = 1
                        var entryEnd = bracketPos + 1
                        var inString = false

                        while (entryEnd < chunk.length && depth > 0) {
                            val ec = chunk[entryEnd]
                            when {
                                inString -> {
                                    if (ec == '"' && chunk.getOrNull(entryEnd - 1) != '\\') {
                                        inString = false
                                    }
                                }
                                ec == '"' -> inString = true
                                ec == '[' -> depth++
                                ec == ']' -> depth--
                            }
                            entryEnd++
                        }

                        if (depth == 0) {
                            val entryJson = chunk.substring(bracketPos, entryEnd)
                            val entry = try {
                                parseEntryArray(entryJson)
                            } catch (e: Exception) {
                                PlatformLogger.error(TAG, "Failed to parse entry at pos $xPos")
                                null
                            }

                            if (entry != null) {
                                entry.inTimePeriod = entry.dateL > limitDate
                                previousEntry = entry
                                totalCount++
                                onEntry(entry)

                                if (totalCount % LOG_INTERVAL == 0) {
                                    PlatformLogger.info(TAG, "Parsed $totalCount entries")
                                    onProgress(totalCount)
                                }

                                if (maxEntries > 0 && totalCount >= maxEntries) {
                                    inputStream.close()
                                    return totalCount
                                }
                            }
                        }

                        searchStart = entryEnd
                    } else {
                        searchStart = xPos + 4
                    }
                }

                PlatformLogger.info(TAG, "Buffer #$bufferReadCount: scan complete, found $xFoundInBuffer X entries, total parsed: $totalCount")
            }

            PlatformLogger.info(TAG, "=== PARSE COMPLETE: $totalCount entries ===")
            return totalCount

        } catch (e: Exception) {
            PlatformLogger.error(TAG, "=== PARSE FAILED ===", e)
            return totalCount
        } finally {
            try {
                inputStream.close()
            } catch (e: Exception) {
                PlatformLogger.error(TAG, "Error closing stream", e)
            }
        }
    }

    /**
     * Parse JSON file using true streaming.
     * Returns a sequence of parsed entries for memory efficiency.
     *
     * @param filePath Path to the JSON file
     * @param onProgress Called periodically with progress (entry count)
     * @param maxEntries Maximum entries to parse (-1 for unlimited)
     * @return Sequence of MediaEntry
     */
    fun parseFileStreaming(
        filePath: String,
        onProgress: (Int) -> Unit = {},
        maxEntries: Int = -1
    ): Sequence<MediaEntry> = sequence {
        PlatformLogger.info(TAG, "=== STARTING parseFileStreaming (sequence) ===")
        PlatformLogger.info(TAG, "Opening file: $filePath")

        previousEntry = null
        var totalCount = 0
        var bufferReadCount = 0

        val inputStream = NSInputStream.inputStreamWithFileAtPath(filePath)
        if (inputStream == null) {
            PlatformLogger.error(TAG, "FAILED: Cannot open file: $filePath")
            return@sequence
        }

        PlatformLogger.info(TAG, "File stream created, opening...")

        try {
            inputStream.open()
            PlatformLogger.info(TAG, "File stream opened successfully")
        } catch (openError: Exception) {
            PlatformLogger.error(TAG, "FAILED to open file stream", openError)
            return@sequence
        }

        try {
            val buffer = ByteArray(BUFFER_SIZE)
            var insideEntry = false
            var bracketDepth = 0
            var currentEntryBuilder = StringBuilder()

            PlatformLogger.info(TAG, "Starting to read buffer chunks (${BUFFER_SIZE} bytes each)...")

            while (inputStream.hasBytesAvailable) {
                bufferReadCount++

                val bytesRead = try {
                    buffer.usePinned { pinned ->
                        inputStream.read(pinned.addressOf(0).reinterpret<uint8_tVar>(), BUFFER_SIZE.toULong()).toInt()
                    }
                } catch (readError: Exception) {
                    PlatformLogger.error(TAG, "FAILED to read buffer at read #$bufferReadCount", readError)
                    throw readError
                }

                if (bytesRead <= 0) {
                    PlatformLogger.info(TAG, "End of stream reached after $bufferReadCount reads")
                    break
                }

                PlatformLogger.info(TAG, "Buffer read #$bufferReadCount: $bytesRead bytes, entries parsed so far: $totalCount")

                PlatformLogger.info(TAG, "Decoding buffer #$bufferReadCount to string...")
                val chunk = try {
                    val decoded = buffer.decodeToString(0, bytesRead)
                    PlatformLogger.info(TAG, "Buffer #$bufferReadCount decoded OK, length: ${decoded.length} chars")
                    decoded
                } catch (decodeError: Exception) {
                    PlatformLogger.error(TAG, "FAILED to decode buffer at read #$bufferReadCount", decodeError)
                    throw decodeError
                }

                PlatformLogger.info(TAG, "Starting to scan buffer #$bufferReadCount (${chunk.length} chars)...")
                var i = 0
                var loopCount = 0

                while (i < chunk.length) {
                    loopCount++

                    // Log first 10 iterations to catch the crash
                    if (loopCount <= 10 || loopCount % 5000 == 0) {
                        PlatformLogger.info(TAG, "Loop #$loopCount: getting char at $i")
                    }

                    val c = try {
                        chunk[i]
                    } catch (e: Exception) {
                        PlatformLogger.error(TAG, "CRASH getting char at index $i", e)
                        throw e
                    }

                    if (loopCount <= 10) {
                        PlatformLogger.info(TAG, "Loop #$loopCount: char='$c' (${c.code})")
                    }

                    // Check for "X": pattern
                    val isXPattern = !insideEntry && i + 4 < chunk.length && run {
                        if (loopCount <= 10) {
                            PlatformLogger.info(TAG, "Loop #$loopCount: checking substring at $i")
                        }
                        try {
                            chunk.substring(i, i + 4) == "\"X\":"
                        } catch (e: Exception) {
                            PlatformLogger.error(TAG, "CRASH in substring at $i", e)
                            throw e
                        }
                    }

                    when {
                        isXPattern -> {
                            PlatformLogger.info(TAG, "Found \"X\": pattern at position $i")
                            i += 4
                            // Skip whitespace
                            while (i < chunk.length && chunk[i].isWhitespace()) i++
                            if (i < chunk.length && chunk[i] == '[') {
                                insideEntry = true
                                bracketDepth = 1
                                currentEntryBuilder.clear()
                                currentEntryBuilder.append('[')
                            }
                            i++
                            continue
                        }

                        insideEntry -> {
                            currentEntryBuilder.append(c)

                            when (c) {
                                '[' -> bracketDepth++
                                ']' -> {
                                    bracketDepth--
                                    if (bracketDepth == 0) {
                                        // Complete entry found
                                        insideEntry = false
                                        val entryJson = currentEntryBuilder.toString()

                                        val entry = try {
                                            parseEntryArray(entryJson)
                                        } catch (parseError: Exception) {
                                            PlatformLogger.error(TAG, "FAILED to parse entry #${totalCount + 1}: ${parseError.message}")
                                            PlatformLogger.error(TAG, "Entry JSON (first 200 chars): ${entryJson.take(200)}")
                                            null
                                        }

                                        if (entry != null) {
                                            entry.inTimePeriod = entry.dateL > limitDate
                                            previousEntry = entry
                                            totalCount++

                                            yield(entry)

                                            if (totalCount % LOG_INTERVAL == 0) {
                                                PlatformLogger.info(TAG, "Parsed $totalCount entries, last: ${entry.channel}/${entry.title?.take(30)}")
                                                onProgress(totalCount)
                                            }

                                            if (maxEntries > 0 && totalCount >= maxEntries) {
                                                PlatformLogger.info(TAG, "Reached max entries limit: $maxEntries")
                                                inputStream.close()
                                                return@sequence
                                            }
                                        }
                                        currentEntryBuilder.clear()
                                    }
                                }
                                '"' -> {
                                    // Handle string - skip to end of string
                                    i++
                                    while (i < chunk.length) {
                                        val sc = chunk[i]
                                        currentEntryBuilder.append(sc)
                                        if (sc == '"' && chunk.getOrNull(i - 1) != '\\') {
                                            break
                                        }
                                        i++
                                    }
                                }
                            }
                        }
                    }
                    i++
                }

                // Safety check for overly large entries
                if (insideEntry && currentEntryBuilder.length > 10000) {
                    PlatformLogger.error(TAG, "Entry too large (${currentEntryBuilder.length} chars), resetting parser state at entry #$totalCount")
                    insideEntry = false
                    currentEntryBuilder.clear()
                }
            }

            PlatformLogger.info(TAG, "=== STREAMING PARSE COMPLETE ===")
            PlatformLogger.info(TAG, "Total entries parsed: $totalCount")
            PlatformLogger.info(TAG, "Total buffer reads: $bufferReadCount")

        } catch (e: Exception) {
            PlatformLogger.error(TAG, "=== STREAMING PARSE FAILED ===", e)
            PlatformLogger.error(TAG, "Failed at entry #$totalCount after $bufferReadCount buffer reads")
        } finally {
            try {
                inputStream.close()
                PlatformLogger.info(TAG, "File stream closed")
            } catch (closeError: Exception) {
                PlatformLogger.error(TAG, "Error closing file stream", closeError)
            }
        }
    }

    /**
     * Parse a JSON array string into a MediaEntry
     */
    private fun parseEntryArray(json: String): MediaEntry? {
        try {
            val values = mutableListOf<String>()
            var i = 1 // Skip opening [

            while (i < json.length - 1) { // Stop before closing ]
                // Skip whitespace and commas
                while (i < json.length && (json[i].isWhitespace() || json[i] == ',')) i++
                if (i >= json.length - 1) break

                when (json[i]) {
                    '"' -> {
                        // Parse string
                        i++ // Skip opening quote
                        val sb = StringBuilder()
                        while (i < json.length) {
                            val c = json[i]
                            when {
                                c == '"' -> {
                                    i++ // Skip closing quote
                                    break
                                }
                                c == '\\' && i + 1 < json.length -> {
                                    i++
                                    when (json[i]) {
                                        '"' -> sb.append('"')
                                        '\\' -> sb.append('\\')
                                        '/' -> sb.append('/')
                                        'n' -> sb.append('\n')
                                        'r' -> sb.append('\r')
                                        't' -> sb.append('\t')
                                        'u' -> {
                                            if (i + 4 < json.length) {
                                                try {
                                                    val hex = json.substring(i + 1, i + 5)
                                                    sb.append(hex.toInt(16).toChar())
                                                    i += 4
                                                } catch (e: Exception) {
                                                    sb.append('u')
                                                }
                                            }
                                        }
                                        else -> sb.append(json[i])
                                    }
                                    i++
                                }
                                else -> {
                                    sb.append(c)
                                    i++
                                }
                            }
                        }
                        values.add(sb.toString())
                    }
                    'n' -> {
                        // null
                        values.add("")
                        i += 4 // Skip "null"
                    }
                    else -> {
                        // Skip unknown
                        while (i < json.length && json[i] != ',' && json[i] != ']') i++
                        values.add("")
                    }
                }
            }

            return MediaEntry.fromArray(values.toTypedArray(), previousEntry)
        } catch (e: Exception) {
            PlatformLogger.error(TAG, "Error parsing entry: ${e.message}")
            return null
        }
    }
}
