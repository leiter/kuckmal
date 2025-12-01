package com.mediathekview.android.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a media entry from the media list
 */
@Entity(
    tableName = "media_entries",
    indices = [
        Index(value = ["channel"]),
        Index(value = ["theme"]),
        Index(value = ["timestamp"]),
        // Unique constraint for diff updates - allows REPLACE strategy to work correctly
        Index(value = ["channel", "theme", "title"], unique = true)
    ]
)
data class MediaEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Core fields
    val channel: String,           // Sender
    val theme: String,             // Thema
    val title: String,             // Titel
    val date: String = "",         // Datum (formatted)
    val time: String = "",         // Zeit
    val duration: String = "",     // Dauer
    val sizeMB: String = "",       // Größe [MB]
    val description: String = "",  // Beschreibung

    // URLs
    val url: String = "",          // Main video URL
    val website: String = "",      // Website
    val subtitleUrl: String = "",  // Url Untertitel
    val smallUrl: String = "",     // Url Klein
    val hdUrl: String = "",        // Url HD

    // Metadata
    val timestamp: Long = 0,       // DatumL (for filtering/sorting)
    val geo: String = "",          // Geographic restriction (e.g., "DE-AT-CH")
    val isNew: Boolean = false     // neu flag
) {
    /**
     * Get media info as array (compatible with legacy code)
     */
    fun toInfoArray(): Array<String> {
        return arrayOf(
            channel,
            theme,
            title,
            date,
            time,
            duration,
            sizeMB,
            description,
            url,
            website,
            subtitleUrl,
            "",  // Url RTMP (deprecated/unused)
            smallUrl,
            "",  // Url RTMP Klein (deprecated/unused)
            hdUrl,
            "",  // Url RTMP HD (deprecated/unused)
            timestamp.toString(),
            "",  // Url History (unused)
            geo,
            isNew.toString()
        )
    }

    companion object {
        /**
         * Create MediaEntry from parsed JSON array
         */
        fun fromArray(data: Array<String>): MediaEntry? {
            if (data.size < 20) return null

            return try {
                MediaEntry(
                    channel = data[0],
                    theme = data[1],
                    title = data[2],
                    date = data[3],
                    time = data[4],
                    duration = data[5],
                    sizeMB = data[6],
                    description = data[7],
                    url = data[8],
                    website = data[9],
                    subtitleUrl = data[10],
                    smallUrl = data[12],
                    hdUrl = data[14],
                    timestamp = data[16].toLongOrNull() ?: 0L,
                    geo = data[18],
                    isNew = data[19] == "true"
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
