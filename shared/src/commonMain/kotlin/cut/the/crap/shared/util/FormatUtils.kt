package cut.the.crap.shared.util

/**
 * Platform-agnostic formatting utilities
 */
object FormatUtils {
    /**
     * Convert duration string (HH:MM:SS) to readable format
     */
    fun formatDuration(duration: String): String {
        if (duration.isBlank()) return ""

        val parts = duration.split(":")
        if (parts.size != 3) return duration

        val hours = parts[0].toIntOrNull() ?: 0
        val minutes = parts[1].toIntOrNull() ?: 0

        return when {
            hours > 0 -> "${hours}h ${minutes}min"
            minutes > 0 -> "${minutes} min"
            else -> "< 1 min"
        }
    }

    /**
     * Convert size in MB to human-readable format
     */
    fun formatSize(sizeMB: String): String {
        val size = sizeMB.toIntOrNull() ?: return ""
        return when {
            size >= 1024 -> "${size / 1024}.${(size % 1024) / 100} GB"
            else -> "$size MB"
        }
    }

    /**
     * Format date from DD.MM.YYYY to readable format
     */
    fun formatDate(date: String): String {
        if (date.isBlank()) return ""
        return date // Already in readable format
    }
}
