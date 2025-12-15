package cut.the.crap.android.util

import android.content.Context
import cut.the.crap.android.R

/**
 * Application-level configuration and constants
 * Replaces the mutable AppState with immutable configuration
 */
object AppConfig {

    // Download URLs
    const val HOST_FILE = "https://verteiler1.mediathekview.de/Filmliste-akt.xz"
    const val HOST_FILE_DIFF = "https://verteiler1.mediathekview.de/Filmliste-diff.xz"

    // File names
    const val FILENAME_XZ = "Filmliste-akt.xz"
    const val FILENAME = "Filmliste-akt"
    const val FILENAME_SAVE = "FilmlisteSave-akt"
    const val FILENAME_DIFF_XZ = "Filmliste-diff.xz"
    const val FILENAME_DIFF = "Filmliste-diff"

    // Time period IDs for date filtering
    const val TIME_PERIOD_ALL = 0
    const val TIME_PERIOD_1_DAY = 1
    const val TIME_PERIOD_3_DAYS = 2
    const val TIME_PERIOD_7_DAYS = 3
    const val TIME_PERIOD_30_DAYS = 4

    /**
     * Get timestamp limit for time period
     */
    fun getTimePeriodLimit(timePeriodId: Int): Long {
        val now = System.currentTimeMillis() / 1000
        val daysInSeconds = 24 * 60 * 60

        return when (timePeriodId) {
            TIME_PERIOD_1_DAY -> now - (1 * daysInSeconds)
            TIME_PERIOD_3_DAYS -> now - (3 * daysInSeconds)
            TIME_PERIOD_7_DAYS -> now - (7 * daysInSeconds)
            TIME_PERIOD_30_DAYS -> now - (30 * daysInSeconds)
            else -> 0L // All time
        }
    }

    /**
     * Get localized display name for time period
     * @param context Context for accessing string resources
     * @param timePeriodId The time period ID constant
     * @return Localized time period name
     */
    fun getTimePeriodName(context: Context, timePeriodId: Int): String {
        return when (timePeriodId) {
            TIME_PERIOD_ALL -> context.getString(R.string.time_period_all)
            TIME_PERIOD_1_DAY -> context.getString(R.string.time_period_1_day)
            TIME_PERIOD_3_DAYS -> context.getString(R.string.time_period_3_days)
            TIME_PERIOD_7_DAYS -> context.getString(R.string.time_period_7_days)
            TIME_PERIOD_30_DAYS -> context.getString(R.string.time_period_30_days)
            else -> context.getString(R.string.time_period_unknown)
        }
    }
}