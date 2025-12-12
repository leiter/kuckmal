package com.mediathekview.android.util

import android.content.Context
import android.util.Log
import com.mediathekview.android.R
import com.mediathekview.android.data.MediaViewModel

/**
 * Utility functions for formatting media information (file sizes, time, duration)
 */
object FormatUtils {

    private const val TAG = "FormatUtils"

    /**
     * Format right panel title based on current viewState
     * @param context Android context for accessing string resources
     * @param viewState Current view state (Themes mode)
     * @return Formatted title string
     */
    fun formatRightPanelTitle(context: Context, viewState: MediaViewModel.ViewState.Themes): String {
        return when {
            // Showing titles for a theme
            viewState.theme != null -> {
                context.getString(R.string.titles_for_theme, viewState.theme)
            }
            // Showing themes for a channel
            viewState.channel != null -> {
                context.getString(R.string.channel_themes, viewState.channel)
            }
            // Showing all themes
            else -> {
                context.getString(R.string.all_themes)
            }
        }
    }

    /**
     * Format file size from MB to human-readable format (MB or GB)
     * @param sizeInMB Size in megabytes as string
     * @return Formatted size string (e.g., "150 MB" or "1.5 GB")
     */
    fun formatFileSize(sizeInMB: String): String {
        if (sizeInMB.isEmpty() || sizeInMB == "0") {
            return "-"
        }

        return try {
            val sizeValue = sizeInMB.toDoubleOrNull() ?: return sizeInMB

            when {
                sizeValue >= 1024 -> {
                    val sizeInGB = sizeValue / 1024
                    String.format(java.util.Locale.getDefault(), "%.2f GB", sizeInGB)
                }
                else -> {
                    String.format(java.util.Locale.getDefault(), "%.0f MB", sizeValue)
                }
            }
        } catch (e: Exception) {
            sizeInMB // Return original value if formatting fails
        }
    }

    /**
     * Format time to localized format
     * @param context Android context for accessing string resources
     * @param time Time string (e.g., "20:15:00" or "20:15")
     * @return Formatted time (e.g., "8:15 PM" for English or "20:15 Uhr" for German)
     */
    fun formatTime(context: Context, time: String): String {
        if (time.isEmpty()) {
            return "-"
        }

        return try {
            val parts = time.split(":")
            if (parts.isEmpty()) return time

            val hours = parts[0].toIntOrNull() ?: return time
            val minutes = if (parts.size > 1) parts[1] else "00"

            // Check if we should use 12-hour format (English) or 24-hour format (German, etc.)
            val locale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            }
            val use12Hour = locale.language == "en"

            if (use12Hour) {
                // Convert to 12-hour format with AM/PM
                val period = if (hours >= 12) {
                    context.getString(R.string.time_pm)
                } else {
                    context.getString(R.string.time_am)
                }

                val displayHour = when {
                    hours == 0 -> 12
                    hours > 12 -> hours - 12
                    else -> hours
                }

                "$displayHour:$minutes $period"
            } else {
                // Use 24-hour format with optional suffix (e.g., "Uhr" for German)
                val suffix = context.getString(R.string.time_suffix)
                if (suffix.isNotEmpty()) {
                    "$hours:$minutes $suffix"
                } else {
                    "$hours:$minutes"
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to format time '$time': ${e.message}")
            time // Return original value if formatting fails
        }
    }

    /**
     * Format duration to human-readable format with localized units
     * @param context Android context for accessing string resources
     * @param duration Duration string (e.g., "01:30:45" or "45:30" or "90")
     * @return Formatted duration (e.g., "1h 30min 45sec" or "1 Std 30 Min 45 Sek")
     */
    fun formatDuration(context: Context, duration: String): String {
        if (duration.isEmpty() || duration == "0") {
            return "-"
        }

        return try {
            val parts = duration.split(":")
            val hours: Int
            val minutes: Int
            val seconds: Int

            when (parts.size) {
                3 -> {
                    // Format: HH:MM:SS
                    hours = parts[0].toIntOrNull() ?: 0
                    minutes = parts[1].toIntOrNull() ?: 0
                    seconds = parts[2].toIntOrNull() ?: 0
                }
                2 -> {
                    // Format: MM:SS
                    hours = 0
                    minutes = parts[0].toIntOrNull() ?: 0
                    seconds = parts[1].toIntOrNull() ?: 0
                }
                1 -> {
                    // Format: minutes only
                    hours = 0
                    minutes = duration.toIntOrNull() ?: 0
                    seconds = 0
                }
                else -> return duration
            }

            // Get localized unit strings
            val hoursUnit = context.getString(R.string.duration_hours_short)
            val minutesUnit = context.getString(R.string.duration_minutes_short)
            val secondsUnit = context.getString(R.string.duration_seconds_short)

            // Build formatted string
            val result = StringBuilder()

            if (hours > 0) {
                result.append("$hours$hoursUnit")
            }

            if (minutes > 0) {
                if (result.isNotEmpty()) result.append(" ")
                result.append("$minutes$minutesUnit")
            }

            if (seconds > 0) {
                if (result.isNotEmpty()) result.append(" ")
                result.append("$seconds$secondsUnit")
            }

            // If everything is zero, return "-"
            if (result.isEmpty()) {
                return "-"
            }

            result.toString()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to format duration '$duration': ${e.message}")
            duration // Return original value if formatting fails
        }
    }
}
