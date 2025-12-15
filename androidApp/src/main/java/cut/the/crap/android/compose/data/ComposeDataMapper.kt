package cut.the.crap.android.compose.data

import cut.the.crap.shared.ui.Channel
import cut.the.crap.shared.ui.MediaItem
import cut.the.crap.shared.database.MediaEntry
import cut.the.crap.android.model.Broadcaster
import cut.the.crap.android.util.FormatUtils

/**
 * Data mapper for converting between database entities and Compose UI models
 */
object ComposeDataMapper {

    /**
     * Convert Broadcaster data to Compose Channel model
     */
    fun getAllChannels(): List<Channel> {
        return Broadcaster.channelListArray.map { broadcaster ->
            Channel(
                name = broadcaster.name,
                displayName = broadcaster.name // Broadcaster only has name property
            )
        }
    }

    /**
     * Convert MediaEntry to Compose MediaItem model
     */
    fun MediaEntry.toMediaItem(): MediaItem {
        return MediaItem(
            channel = this.channel,
            theme = this.theme,
            title = this.title,
            date = this.date,
            time = this.time,
            duration = this.duration,
            size = "${this.sizeMB} MB",
            description = this.description
        )
    }

    /**
     * Get unique themes from a list of MediaEntry objects
     */
    fun List<MediaEntry>.extractUniqueThemes(): List<String> {
        return this.map { it.theme }.distinct()
    }

    /**
     * Get unique titles from a list of MediaEntry objects
     */
    fun List<MediaEntry>.extractUniqueTitles(): List<String> {
        return this.map { it.title }.distinct()
    }

    /**
     * Format duration for display
     */
    fun formatDuration(seconds: String): String {
        return try {
            val totalSeconds = seconds.toIntOrNull() ?: 0
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val secs = totalSeconds % 60

            when {
                hours > 0 -> "${hours}h ${minutes}min"
                minutes > 0 -> "${minutes}min ${secs}s"
                else -> "${secs}s"
            }
        } catch (e: Exception) {
            seconds // Return original if parsing fails
        }
    }

    /**
     * Find channel by name
     */
    fun findChannelByName(name: String?): Channel? {
        return name?.let { channelName ->
            getAllChannels().find { it.name == channelName }
        }
    }
}