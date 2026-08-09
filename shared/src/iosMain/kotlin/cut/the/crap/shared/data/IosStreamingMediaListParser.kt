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
        private const val MAX_PENDING_TEXT = 1 shl 20 // 1M chars of carry-over
    }

    fun setLimitDate(limitDate: Long) {
        this.limitDate = limitDate
    }

    /**
     * Index of the first byte of the trailing incomplete UTF-8 sequence in
     * [bytes] (considering only the first [length] bytes), or [length] when the
     * buffer ends on a complete character.
     *
     * A 64 KB read lands at an arbitrary byte offset, so it regularly cuts a
     * multi-byte character (every umlaut is two bytes) in half. Decoding such a
     * chunk on its own corrupts that character - and NSString.create returns null
     * for the whole chunk, which previously discarded all ~100 entries in it.
     */
    private fun completeUtf8Length(bytes: ByteArray, length: Int): Int {
        if (length == 0) return 0
        // A character is at most 4 bytes, so the split can only be in the last 3.
        var start = length - 1
        val floor = maxOf(0, length - 4)
        while (start >= floor && (bytes[start].toInt() and 0xC0) == 0x80) start--
        if (start < floor) return length // no lead byte in reach; leave it alone

        val lead = bytes[start].toInt() and 0xFF
        val expected = when {
            lead and 0x80 == 0x00 -> 1
            lead and 0xE0 == 0xC0 -> 2
            lead and 0xF0 == 0xE0 -> 3
            lead and 0xF8 == 0xF0 -> 4
            else -> return length // not a valid lead byte; let the decoder deal with it
        }
        return if (start + expected <= length) length else start
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
            // Bytes of a character cut in half by the end of the last read.
            var pendingBytes = ByteArray(0)
            // Text after the last fully parsed entry: an entry routinely spans reads.
            var pendingText = ""

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

                // Prepend the partial character carried over from the previous read,
                // then hold back any partial character at the end of this one.
                val combined = ByteArray(pendingBytes.size + bytesRead)
                pendingBytes.copyInto(combined)
                buffer.copyInto(combined, pendingBytes.size, 0, bytesRead)

                val decodableLength = completeUtf8Length(combined, combined.size)
                pendingBytes = combined.copyOfRange(decodableLength, combined.size)

                if (decodableLength == 0) continue

                val decoded = try {
                    val nsData = combined.usePinned { pinned ->
                        NSData.dataWithBytes(pinned.addressOf(0), decodableLength.toULong())
                    }
                    NSString.create(nsData, NSUTF8StringEncoding)?.toString()
                } catch (decodeError: Exception) {
                    PlatformLogger.error(TAG, "FAILED to decode buffer #$bufferReadCount", decodeError)
                    null
                }

                if (decoded == null) {
                    // Genuinely malformed input rather than a split character. Drop
                    // this window but keep the carried entry so the stream resyncs.
                    PlatformLogger.error(TAG, "Buffer #$bufferReadCount not valid UTF-8, skipping $decodableLength bytes")
                    continue
                }

                val chunk = pendingText + decoded
                var searchStart = 0
                var consumed = 0

                while (true) {
                    val xPos = chunk.indexOf("\"X\":", searchStart)
                    if (xPos == -1) {
                        // Keep the last few chars in case "X": itself straddles the read.
                        consumed = maxOf(consumed, chunk.length - 3)
                        break
                    }

                    var bracketPos = xPos + 4
                    while (bracketPos < chunk.length && chunk[bracketPos].isWhitespace()) bracketPos++

                    if (bracketPos >= chunk.length) {
                        consumed = xPos // need more input to see the bracket
                        break
                    }

                    if (chunk[bracketPos] != '[') {
                        searchStart = xPos + 4
                        continue
                    }

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

                    if (depth != 0) {
                        // Entry continues past this read; carry it whole to the next.
                        consumed = xPos
                        break
                    }

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

                    searchStart = entryEnd
                    consumed = entryEnd
                }

                pendingText = chunk.substring(consumed.coerceIn(0, chunk.length))

                // A single entry is far smaller than this; anything larger means the
                // input is malformed and we would otherwise grow without bound.
                if (pendingText.length > MAX_PENDING_TEXT) {
                    PlatformLogger.error(TAG, "Carry-over exceeded $MAX_PENDING_TEXT chars, resetting at entry #$totalCount")
                    pendingText = ""
                }
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
