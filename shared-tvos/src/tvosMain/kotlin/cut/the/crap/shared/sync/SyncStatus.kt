package cut.the.crap.shared.sync

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * Represents the synchronization status of the app's data.
 * Used for tracking offline state and sync progress.
 */
sealed class SyncStatus {
    /**
     * No sync operation in progress
     */
    object Idle : SyncStatus()

    /**
     * Currently syncing data
     */
    object Syncing : SyncStatus()

    /**
     * Successfully synced at the given timestamp
     * @param timestamp Unix timestamp in milliseconds when sync completed
     */
    data class Synced(val timestamp: Long) : SyncStatus() {
        /**
         * Get human-readable "time ago" string
         */
        fun getTimeAgo(): String {
            val now = (NSDate().timeIntervalSince1970 * 1000).toLong()
            val diffMs = now - timestamp

            return when {
                diffMs < 60_000 -> "just now"
                diffMs < 3_600_000 -> "${diffMs / 60_000} min ago"
                diffMs < 86_400_000 -> "${diffMs / 3_600_000} hours ago"
                else -> "${diffMs / 86_400_000} days ago"
            }
        }
    }

    /**
     * Sync failed with an error message
     * @param message Error description
     */
    data class Error(val message: String) : SyncStatus()

    /**
     * Device is offline, no network connection
     */
    object Offline : SyncStatus()

    companion object {
        /**
         * Check if the status indicates data is available
         */
        fun hasData(status: SyncStatus): Boolean = when (status) {
            is Synced -> true
            is Idle -> true
            is Syncing -> true
            is Error -> false
            is Offline -> false
        }
    }
}
