package cut.the.crap.android.util

import android.content.Context
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks if a new film list update is available on the server without downloading the full file.
 *
 * Server-Friendly Implementation:
 * - Uses HTTP HEAD requests to check metadata (very lightweight)
 * - Compares Last-Modified, ETag, and Content-Length headers
 * - Only recommends download if the file has actually changed
 * - Respects configurable update intervals (default: once per day)
 * - Checks when app comes to foreground, not just on startup
 *
 * This approach significantly reduces server load by avoiding unnecessary downloads
 * of the ~50MB film list file when no changes have occurred.
 */
class UpdateChecker(private val context: Context) : UpdateCheckerInterface {

    private val updatePrefs = UpdatePreferences(context)

    companion object {
        private const val TAG = "UpdateChecker"
        private const val HEAD_REQUEST_TIMEOUT = 10000 // 10 seconds
    }

    /**
     * Result of an update check
     */
    sealed class UpdateCheckResult {
        /**
         * Update is available - server has a newer file
         * @param lastModified The Last-Modified header from server
         * @param etag The ETag header from server
         * @param contentLength The file size in bytes
         */
        data class UpdateAvailable(
            val lastModified: String?,
            val etag: String?,
            val contentLength: Long
        ) : UpdateCheckResult()

        /**
         * No update needed - file hasn't changed
         */
        object NoUpdateNeeded : UpdateCheckResult()

        /**
         * Check was skipped - interval hasn't elapsed yet
         * @param nextCheckTime When the next check can occur
         */
        data class CheckSkipped(val nextCheckTime: Long) : UpdateCheckResult()

        /**
         * Check failed due to network or server error
         * @param error Error message
         * @param exception The exception that occurred
         */
        data class CheckFailed(
            val error: String,
            val exception: Exception?
        ) : UpdateCheckResult()
    }

    /**
     * Check if an update is available on the server
     *
     * @param forceCheck If true, bypass the interval check
     * @param useDiff If true, check diff file instead of full file
     * @return UpdateCheckResult indicating what action to take
     */
    override fun checkForUpdate(forceCheck: Boolean, useDiff: Boolean): UpdateCheckResult {
        // Check if we should perform the update check based on interval
        if (!forceCheck && !updatePrefs.shouldCheckForUpdate()) {
            val nextCheckTime = updatePrefs.lastCheckTime +
                    (updatePrefs.updateIntervalHours * 60 * 60 * 1000L)
            Log.d(TAG, "Update check skipped - interval not elapsed. Next check: $nextCheckTime")
            return UpdateCheckResult.CheckSkipped(nextCheckTime)
        }

        // Check network availability
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.w(TAG, "No network connection available for update check")
            return UpdateCheckResult.CheckFailed(
                error = "No network connection available",
                exception = null
            )
        }

        return try {
            val checkUrl = if (useDiff) AppConfig.HOST_FILE_DIFF else AppConfig.HOST_FILE
            Log.d(TAG, "Checking for updates using HTTP HEAD request: $checkUrl (diff: $useDiff)")
            val metadata = fetchFileMetadata(checkUrl)

            // Update the last check time
            updatePrefs.lastCheckTime = System.currentTimeMillis()

            // Compare with stored metadata
            if (hasFileChanged(metadata)) {
                Log.i(TAG, "Update available - file has changed on server")
                UpdateCheckResult.UpdateAvailable(
                    lastModified = metadata.lastModified,
                    etag = metadata.etag,
                    contentLength = metadata.contentLength
                )
            } else {
                Log.d(TAG, "No update needed - file unchanged")
                UpdateCheckResult.NoUpdateNeeded
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            UpdateCheckResult.CheckFailed(
                error = e.message ?: "Unknown error",
                exception = e
            )
        }
    }

    /**
     * File metadata from HTTP headers
     */
    private data class FileMetadata(
        val lastModified: String?,
        val etag: String?,
        val contentLength: Long
    )

    /**
     * Fetch file metadata using HTTP HEAD request (doesn't download the file)
     */
    private fun fetchFileMetadata(urlString: String): FileMetadata {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "HEAD"
            connection.connectTimeout = HEAD_REQUEST_TIMEOUT
            connection.readTimeout = HEAD_REQUEST_TIMEOUT
            connection.instanceFollowRedirects = true

            // Add conditional request headers if we have previous metadata
            updatePrefs.lastModified?.let {
                connection.setRequestProperty("If-Modified-Since", it)
            }
            updatePrefs.etag?.let {
                connection.setRequestProperty("If-None-Match", it)
            }

            connection.connect()

            val responseCode = connection.responseCode
            Log.d(TAG, "HEAD request response code: $responseCode")

            when (responseCode) {
                HttpURLConnection.HTTP_NOT_MODIFIED -> {
                    // 304 Not Modified - file hasn't changed
                    Log.d(TAG, "Server returned 304 Not Modified")
                    FileMetadata(
                        lastModified = updatePrefs.lastModified,
                        etag = updatePrefs.etag,
                        contentLength = updatePrefs.contentLength
                    )
                }
                HttpURLConnection.HTTP_OK -> {
                    // 200 OK - file exists, get metadata
                    val lastModified = connection.getHeaderField("Last-Modified")
                    val etag = connection.getHeaderField("ETag")
                    // Use contentLength for display purposes only, not for change detection
                    // Prefer contentLengthLong (API 24+), fall back to contentLength if needed
                    val contentLength = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        connection.contentLengthLong
                    } else {
                        connection.contentLength.toLong()
                    }

                    Log.d(TAG, "Server metadata - Last-Modified: $lastModified, ETag: $etag, Size: $contentLength")

                    FileMetadata(
                        lastModified = lastModified,
                        etag = etag,
                        contentLength = contentLength
                    )
                }
                else -> {
                    throw java.io.IOException("Unexpected HTTP response code: $responseCode")
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Check if the file has changed compared to our stored metadata
     */
    private fun hasFileChanged(newMetadata: FileMetadata): Boolean {
        val storedLastModified = updatePrefs.lastModified
        val storedETag = updatePrefs.etag
        val storedContentLength = updatePrefs.contentLength

        // If we have no stored metadata, we need to download
        if (storedLastModified == null && storedETag == null && storedContentLength == 0L) {
            Log.d(TAG, "No stored metadata - first time download needed")
            return true
        }

        // Check ETag first (most reliable)
        if (newMetadata.etag != null && storedETag != null) {
            if (newMetadata.etag != storedETag) {
                Log.d(TAG, "ETag changed: $storedETag -> ${newMetadata.etag}")
                return true
            }
        }

        // Check Last-Modified
        if (newMetadata.lastModified != null && storedLastModified != null) {
            if (newMetadata.lastModified != storedLastModified) {
                Log.d(TAG, "Last-Modified changed: $storedLastModified -> ${newMetadata.lastModified}")
                return true
            }
        }

        // Note: We intentionally don't check Content-Length for change detection
        // because it's not a reliable indicator (can change due to compression, etc.)
        // and requires API 24+ (contentLengthLong). ETag and Last-Modified are sufficient.

        // No changes detected
        return false
    }

    /**
     * Save metadata after a successful download
     */
    override fun saveDownloadMetadata(lastModified: String?, etag: String?, contentLength: Long) {
        updatePrefs.saveDownloadMetadata(lastModified, etag, contentLength)
        Log.d(TAG, "Saved download metadata - Last-Modified: $lastModified, ETag: $etag, Size: $contentLength")
    }

    /**
     * Get a human-readable description of when the next update check will occur
     */
    override fun getNextCheckDescription(): String {
        if (!updatePrefs.autoUpdateEnabled) {
            return "Automatic updates disabled"
        }

        val nextCheckTime = updatePrefs.lastCheckTime +
                (updatePrefs.updateIntervalHours * 60 * 60 * 1000L)
        val now = System.currentTimeMillis()

        return if (now >= nextCheckTime) {
            "Update check available now"
        } else {
            val hoursRemaining = ((nextCheckTime - now) / (60 * 60 * 1000L)).toInt()
            "Next check in ~$hoursRemaining hours"
        }
    }
}
