package cut.the.crap.desktop.data

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import cut.the.crap.shared.database.MediaEntry
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Parser for MediathekView film list JSON files on desktop.
 * Streams and parses large JSON files with memory efficiency.
 */
class DesktopMediaListParser {

    private var previousChannel: String = ""
    private var previousTheme: String = ""

    /**
     * Callback interface for chunked parsing
     */
    interface ChunkCallback {
        fun onChunk(entries: List<MediaEntry>, totalParsed: Int)
        fun onComplete(totalEntries: Int)
        fun onError(error: Exception, entriesParsed: Int)
    }

    /**
     * Parse JSON film list from file with chunked callback for database insertion.
     */
    fun parseFileChunked(
        filePath: String,
        callback: ChunkCallback,
        maxEntries: Int = -1,
        chunkSize: Int = 5000
    ) {
        previousChannel = ""
        previousTheme = ""
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
                                val entry = readMediaEntry(reader)
                                if (entry != null) {
                                    chunk.add(entry)
                                    count++

                                    if (chunk.size >= chunkSize) {
                                        callback.onChunk(chunk.toList(), count)
                                        chunk.clear()

                                        if (count % 50000 == 0) {
                                            println("Parsed $count entries...")
                                        }
                                    }

                                    if (maxEntries > 0 && count >= maxEntries) {
                                        break
                                    }
                                }
                            } else {
                                reader.skipValue()
                            }
                        }

                        reader.endObject()

                        if (chunk.isNotEmpty()) {
                            callback.onChunk(chunk.toList(), count)
                            chunk.clear()
                        }
                    }
                }
            }

            println("Successfully parsed $count media entries")
            callback.onComplete(count)

        } catch (e: Exception) {
            println("Error parsing media list at entry $count: ${e.message}")
            callback.onError(e, count)
        }
    }

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

        return createMediaEntry(values.toTypedArray())
    }

    private fun createMediaEntry(arr: Array<String>): MediaEntry {
        // Handle inherited channel/theme
        val channel = if (arr.isNotEmpty() && arr[0].isNotEmpty()) {
            previousChannel = arr[0]
            arr[0]
        } else previousChannel

        val theme = if (arr.size > 1 && arr[1].isNotEmpty()) {
            previousTheme = arr[1]
            arr[1]
        } else previousTheme

        val dateL = if (arr.size > 16) arr[16].toLongOrNull() ?: 0L else 0L
        val isNew = arr.getOrNull(19)?.lowercase() == "true"

        return MediaEntry(
            channel = channel,
            theme = theme,
            title = arr.getOrNull(2) ?: "",
            date = arr.getOrNull(3) ?: "",
            time = arr.getOrNull(4) ?: "",
            duration = arr.getOrNull(5) ?: "",
            sizeMB = arr.getOrNull(6) ?: "",
            description = arr.getOrNull(7) ?: "",
            url = arr.getOrNull(8) ?: "",
            website = arr.getOrNull(9) ?: "",
            subtitleUrl = arr.getOrNull(10) ?: "",
            smallUrl = arr.getOrNull(12) ?: "",
            hdUrl = arr.getOrNull(14) ?: "",
            timestamp = dateL,
            geo = arr.getOrNull(18) ?: "",
            isNew = isNew
        )
    }
}
