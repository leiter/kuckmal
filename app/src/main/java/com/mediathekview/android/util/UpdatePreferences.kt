package com.mediathekview.android.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Manages preferences for film list updates to implement server-friendly differential updates.
 *
 * Server-Friendly Best Practices:
 * - MediathekView team recommends avoiding unnecessary downloads
 * - Film lists are updated approximately every 4 hours
 * - Check if server file has changed before downloading
 * - Use HTTP headers (Last-Modified, ETag, Content-Length) for validation
 */
class UpdatePreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "update_preferences"

        // Preference keys
        private const val KEY_LAST_CHECK_TIME = "last_check_time"
        private const val KEY_LAST_DOWNLOAD_TIME = "last_download_time"
        private const val KEY_LAST_MODIFIED = "last_modified"
        private const val KEY_ETAG = "etag"
        private const val KEY_CONTENT_LENGTH = "content_length"
        private const val KEY_AUTO_UPDATE_ENABLED = "auto_update_enabled"
        private const val KEY_UPDATE_INTERVAL_HOURS = "update_interval_hours"

        // Default values
        const val DEFAULT_UPDATE_INTERVAL_HOURS = 2 // Check every 2 hours when app comes to foreground
        const val DEFAULT_AUTO_UPDATE_ENABLED = true
    }

    /**
     * Get the timestamp of the last update check
     */
    var lastCheckTime: Long
        get() = prefs.getLong(KEY_LAST_CHECK_TIME, 0)
        set(value) = prefs.edit { putLong(KEY_LAST_CHECK_TIME, value) }

    /**
     * Get the timestamp of the last successful download
     */
    var lastDownloadTime: Long
        get() = prefs.getLong(KEY_LAST_DOWNLOAD_TIME, 0)
        set(value) = prefs.edit { putLong(KEY_LAST_DOWNLOAD_TIME, value) }

    /**
     * Get the Last-Modified header from the last download
     */
    var lastModified: String?
        get() = prefs.getString(KEY_LAST_MODIFIED, null)
        set(value) = prefs.edit { putString(KEY_LAST_MODIFIED, value) }

    /**
     * Get the ETag header from the last download
     */
    var etag: String?
        get() = prefs.getString(KEY_ETAG, null)
        set(value) = prefs.edit { putString(KEY_ETAG, value) }

    /**
     * Get the Content-Length from the last download
     */
    var contentLength: Long
        get() = prefs.getLong(KEY_CONTENT_LENGTH, 0)
        set(value) = prefs.edit { putLong(KEY_CONTENT_LENGTH, value) }

    /**
     * Check if automatic update checking is enabled
     */
    var autoUpdateEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_UPDATE_ENABLED, DEFAULT_AUTO_UPDATE_ENABLED)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_UPDATE_ENABLED, value) }

    /**
     * Get the update check interval in hours
     */
    var updateIntervalHours: Int
        get() = prefs.getInt(KEY_UPDATE_INTERVAL_HOURS, DEFAULT_UPDATE_INTERVAL_HOURS)
        set(value) = prefs.edit { putInt(KEY_UPDATE_INTERVAL_HOURS, value) }

    /**
     * Check if enough time has passed since the last check based on the configured interval
     */
    fun shouldCheckForUpdate(): Boolean {
        if (!autoUpdateEnabled) return false

        val now = System.currentTimeMillis()
        val intervalMillis = updateIntervalHours * 60 * 60 * 1000L
        return (now - lastCheckTime) >= intervalMillis
    }

    /**
     * Save metadata from a successful download
     */
    fun saveDownloadMetadata(lastModified: String?, etag: String?, contentLength: Long) {
        prefs.edit().apply {
            putLong(KEY_LAST_DOWNLOAD_TIME, System.currentTimeMillis())
            putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
            if (lastModified != null) {
                putString(KEY_LAST_MODIFIED, lastModified)
            }
            if (etag != null) {
                putString(KEY_ETAG, etag)
            }
            putLong(KEY_CONTENT_LENGTH, contentLength)
            apply()
        }
    }

    /**
     * Clear all stored update metadata (useful for forcing a fresh download)
     */
    fun clearMetadata() {
        prefs.edit().apply {
            remove(KEY_LAST_MODIFIED)
            remove(KEY_ETAG)
            remove(KEY_CONTENT_LENGTH)
            remove(KEY_LAST_DOWNLOAD_TIME)
            remove(KEY_LAST_CHECK_TIME)
            apply()
        }
    }
}
