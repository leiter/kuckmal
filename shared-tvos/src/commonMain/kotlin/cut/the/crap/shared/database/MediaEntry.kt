package cut.the.crap.shared.database

/**
 * Simple data class for tvOS (no Room database support)
 * Uses mock data repository instead
 */
data class MediaEntry(
    val id: Long = 0,

    // Core fields
    val channel: String,
    val theme: String,
    val title: String,
    val date: String = "",
    val time: String = "",
    val duration: String = "",
    val sizeMB: String = "",
    val description: String = "",

    // URLs
    val url: String = "",
    val website: String = "",
    val subtitleUrl: String = "",
    val smallUrl: String = "",
    val hdUrl: String = "",

    // Metadata
    val timestamp: Long = 0,
    val geo: String = "",
    val isNew: Boolean = false
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
