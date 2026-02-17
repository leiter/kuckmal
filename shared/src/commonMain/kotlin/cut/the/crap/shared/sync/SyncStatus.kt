package cut.the.crap.shared.sync

/**
 * Represents the synchronization status of the app's data.
 * Used for tracking offline state and sync progress across all platforms.
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
         * @param currentTimeMs Current time in milliseconds (for testability)
         */
        fun getTimeAgo(currentTimeMs: Long): String {
            val diffMs = currentTimeMs - timestamp

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
         * Check if the status indicates data is available (synced or idle with cached data)
         */
        fun hasData(status: SyncStatus): Boolean = when (status) {
            is Synced -> true
            is Idle -> true
            is Syncing -> true  // Data might still be available during sync
            is Error -> false
            is Offline -> false  // Could still have cached data
        }
    }
}
