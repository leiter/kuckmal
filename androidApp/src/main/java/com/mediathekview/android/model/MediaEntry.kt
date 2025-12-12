package com.mediathekview.android.model

/**
 * Represents a single media entry from the film list.
 *
 * Now includes ALL available fields from the JSON for complete data storage.
 *
 * @property channel Broadcast channel name (e.g., "ARD", "ZDF")
 * @property theme Theme/category (e.g., "Nachrichten", "Sport")
 * @property title Media title
 * @property date Human-readable date (e.g., "30.11.2023")
 * @property time Broadcast time (e.g., "14:05:00")
 * @property duration Duration in format HH:MM:SS (e.g., "00:43:27")
 * @property sizeMB File size in MB (e.g., "864")
 * @property description Description text
 * @property url Main video URL (high quality)
 * @property website Official website link
 * @property subtitleUrl URL for subtitles
 * @property urlSmall Low-quality video URL
 * @property urlHd HD quality video URL
 * @property dateL Unix timestamp for the broadcast date
 * @property geo Geographic restrictions (e.g., "DE-AT-CH")
 * @property isNew Flag indicating if content is new
 * @property inTimePeriod Flag for date filtering
 */
data class MediaEntry(
    @JvmField val channel: String = "",
    @JvmField val theme: String = "",
    @JvmField val title: String = "",
    @JvmField val date: String = "",
    @JvmField val time: String = "",
    @JvmField val duration: String = "",
    @JvmField val sizeMB: String = "",
    @JvmField val description: String = "",
    @JvmField val url: String = "",
    @JvmField val website: String = "",
    @JvmField val subtitleUrl: String = "",
    @JvmField val urlSmall: String = "",
    @JvmField val urlHd: String = "",
    @JvmField val dateL: Long = 0,
    @JvmField val geo: String = "",
    @JvmField val isNew: Boolean = false,
    @JvmField var inTimePeriod: Boolean = true  // var for mutability
) {
    companion object {
        /**
         * Factory method to create MediaEntry from array, handling empty strings
         * by inheriting from previous entry.
         *
         * This pattern is used in the JSON parser where consecutive entries may
         * omit channel/theme if they're the same as the previous entry.
         *
         * @param arr String array from JSON parsing
         * @param previous Previous MediaEntry to inherit channel/theme from
         * @return New MediaEntry instance
         */
        @JvmStatic
        fun fromArray(arr: Array<String>, previous: MediaEntry?): MediaEntry {
            // Helper to get value or inherit from previous
            fun getOrInherit(index: Int, previousValue: String): String =
                if (arr.size > index && arr[index].isNotEmpty()) arr[index]
                else previousValue

            // Parse dateL with safe conversion
            val dateL = if (arr.size > 16) {
                arr[16].toLongOrNull() ?: 0L
            } else {
                0L
            }

            // Parse isNew flag
            val isNew = arr.getOrNull(19)?.lowercase() == "true"

            return MediaEntry(
                channel = getOrInherit(0, previous?.channel ?: ""),
                theme = getOrInherit(1, previous?.theme ?: ""),
                title = arr.getOrNull(2) ?: "",
                date = arr.getOrNull(3) ?: "",
                time = arr.getOrNull(4) ?: "",
                duration = arr.getOrNull(5) ?: "",
                sizeMB = arr.getOrNull(6) ?: "",
                description = arr.getOrNull(7) ?: "",
                url = arr.getOrNull(8) ?: "",
                website = arr.getOrNull(9) ?: "",
                subtitleUrl = arr.getOrNull(10) ?: "",
                urlSmall = arr.getOrNull(12) ?: "",
                urlHd = arr.getOrNull(14) ?: "",
                dateL = dateL,
                geo = arr.getOrNull(18) ?: "",
                isNew = isNew,
                inTimePeriod = true
            )
        }
    }

    /**
     * Convert to database MediaEntry for Room storage
     */
    fun toDatabaseEntry(): com.mediathekview.android.database.MediaEntry {
        return com.mediathekview.android.database.MediaEntry(
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
    }
}
