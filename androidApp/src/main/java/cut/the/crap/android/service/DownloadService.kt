package cut.the.crap.android.service

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import cut.the.crap.android.R
import cut.the.crap.android.util.AppConfig
import cut.the.crap.android.util.UpdateChecker
import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service for downloading files with queue processing and progress tracking.
 * Clean architecture - no direct Activity/ViewModel references, uses callbacks instead.
 *
 * Architecture:
 * - Uses DownloadCallback interface to report progress/status
 * - DownloadRepository wraps this service and exposes StateFlow
 * - MediaViewModel observes StateFlow and converts to DialogModels
 */
class DownloadService(
    private val context: Context,
    private val updateChecker: UpdateChecker
) {

    private val downloadQueue = mutableListOf<DownloadTask>()
    private var isDownloading = false
    private var currentDownload: DownloadThread? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Callback for progress/status updates
    private var downloadCallback: DownloadCallback? = null

    /**
     * Callback interface for download status updates
     */
    interface DownloadCallback {
        fun onDownloadProgress(progress: Int, downloadedMB: Int, totalMB: Int, speed: String)
        fun onDownloadSuccess(filename: String, shouldExtract: Boolean)
        fun onDownloadError(message: String, canRetry: Boolean, retryAction: (() -> Unit)? = null)
    }

    /**
     * Set the callback for download updates
     */
    fun setDownloadCallback(callback: DownloadCallback) {
        this.downloadCallback = callback
    }

    /**
     * Download task data class
     */
    private data class DownloadTask(
        val url: String,
        val filename: String,
        var retryCount: Int = 0
    )

    /**
     * Add a download to the queue and start processing
     */
    fun addDownload(url: String, filename: String) {
        Log.d(TAG, "addDownload called: $url -> $filename")
        val task = DownloadTask(url, filename)
        downloadQueue.add(task)
        Log.d(TAG, "Download task added to queue, queue size: ${downloadQueue.size}")
        processQueue()
    }

    /**
     * Process the download queue
     */
    private fun processQueue() {
        Log.d(TAG, "processQueue called, isDownloading=$isDownloading, queue size=${downloadQueue.size}")
        if (isDownloading || downloadQueue.isEmpty()) {
            Log.d(TAG, "Skipping processQueue: isDownloading=$isDownloading, queue empty=${downloadQueue.isEmpty()}")
            return
        }

        isDownloading = true
        val task = downloadQueue.removeAt(0)
        Log.d(TAG, "Starting download thread for: ${task.url}")
        currentDownload = DownloadThread(task)
        currentDownload?.start()
    }

    /**
     * Clean up downloads and cancel current download
     */
    fun cleanup() {
        currentDownload?.interrupt()
        currentDownload = null
        downloadQueue.clear()
        isDownloading = false
        Log.d(TAG, "Download manager cleaned up")
    }

    /**
     * Thread for downloading files
     */
    private inner class DownloadThread(private val task: DownloadTask) : Thread() {

        override fun run() {
            // Notify start of download
            mainHandler.post {
                downloadCallback?.onDownloadProgress(0, 0, 0, "Initializing...")
            }

            var success = false
            var errorMessage = ""
            var lastModified: String? = null
            var etag: String? = null
            var contentLength: Long = 0
            try {
                val url = URL(task.url)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                connection.connect()

                // Capture HTTP headers for differential update tracking
                lastModified = connection.getHeaderField("Last-Modified")
                etag = connection.getHeaderField("ETag")
                val fileLength = connection.contentLength
                // Use contentLengthLong on API 24+, fall back to contentLength
                contentLength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    connection.contentLengthLong
                } else {
                    fileLength.toLong()
                }
                if (fileLength <= 0) {
                    throw java.io.IOException(context.getString(R.string.error_invalid_file_size))
                }

                val input = BufferedInputStream(url.openStream())
                val output = FileOutputStream(task.filename)

                val buffer = ByteArray(BUFFER_SIZE)
                var count: Int
                var total = 0
                var lastUpdateTotal = 0
                val updateInterval = 100 * 1024 // Update every 100KB
                val startTime = System.currentTimeMillis()

                while (input.read(buffer).also { count = it } != -1) {
                    total += count
                    val progress = (total.toLong() * 100 / fileLength).toInt()

                    // Update progress when we've downloaded at least 100KB since last update
                    if (total - lastUpdateTotal >= updateInterval) {
                        val currentTime = System.currentTimeMillis()
                        val elapsedSeconds = (currentTime - startTime) / 1000.0
                        val downloadedMB = total / (1024.0 * 1024.0)
                        val totalMB = fileLength / (1024.0 * 1024.0)
                        val speedMBps = if (elapsedSeconds > 0) downloadedMB / elapsedSeconds else 0.0

                        // Report progress via callback
                        mainHandler.post {
                            downloadCallback?.onDownloadProgress(
                                progress,
                                downloadedMB.toInt(),
                                totalMB.toInt(),
                                String.format(java.util.Locale.US, "%.2f MB/s", speedMBps)
                            )
                        }

                        lastUpdateTotal = total
                    }

                    output.write(buffer, 0, count)
                }

                // Report final progress at 100%
                val finalDownloadedMB = total / (1024.0 * 1024.0)
                val finalTotalMB = fileLength / (1024.0 * 1024.0)
                mainHandler.post {
                    downloadCallback?.onDownloadProgress(
                        100,
                        finalDownloadedMB.toInt(),
                        finalTotalMB.toInt(),
                        "Complete"
                    )
                }

                output.flush()
                output.close()
                input.close()

                // Verify download completed fully
                val downloadedFile = java.io.File(task.filename)
                if (!downloadedFile.exists() || downloadedFile.length() != fileLength.toLong()) {
                    throw java.io.IOException(context.getString(R.string.error_download_incomplete))
                }

                success = true

            } catch (e: java.net.UnknownHostException) {
                errorMessage = context.getString(R.string.error_no_connection)
                Log.e(TAG, "Download failed - no internet: ${task.url}", e)
            } catch (e: java.net.SocketTimeoutException) {
                errorMessage = context.getString(R.string.error_timeout)
                Log.e(TAG, "Download failed - timeout: ${task.url}", e)
            } catch (e: java.io.IOException) {
                val networkError = context.getString(R.string.error_download_network)
                errorMessage = context.getString(R.string.error_download_failed, e.message ?: networkError)
                Log.e(TAG, "Download failed - IO error: ${task.url}", e)
            } catch (e: Exception) {
                val unknownError = context.getString(R.string.error_unknown)
                errorMessage = context.getString(R.string.error_download_failed, e.message ?: unknownError)
                Log.e(TAG, "Download failed: ${task.url}", e)
            }

            // Handle completion on main thread
            mainHandler.post {
                isDownloading = false

                if (success) {
                    task.retryCount = 0
                    handleDownloadComplete(task, lastModified, etag, contentLength)
                } else {
                    handleDownloadFailure(task, errorMessage)
                }

                // Process next item in queue
                processQueue()
            }
        }
    }

    /**
     * Handle download failure with retry logic
     */
    private fun handleDownloadFailure(task: DownloadTask, errorMessage: String) {
        task.retryCount++

        if (task.retryCount < MAX_RETRIES) {
            Log.i(TAG, "Retrying download (attempt ${task.retryCount + 1}/$MAX_RETRIES): ${task.url}")

            // Auto-retry after delay
            mainHandler.postDelayed({
                downloadQueue.add(0, task)
                processQueue()
            }, 2000)
        } else {
            Log.e(TAG, "Max retries reached for: ${task.filename}")

            // Report error via callback
            downloadCallback?.onDownloadError(
                message = errorMessage,
                canRetry = true,
                retryAction = {
                    task.retryCount = 0
                    addDownload(task.url, task.filename)
                }
            )
        }
    }

    /**
     * Handle download completion
     */
    private fun handleDownloadComplete(
        task: DownloadTask,
        lastModified: String?,
        etag: String?,
        contentLength: Long
    ) {
        Log.i(TAG, "Download complete: ${task.filename}")

        // Save update metadata for differential updates
        updateChecker.saveDownloadMetadata(lastModified, etag, contentLength)
        Log.d(TAG, "Saved download metadata - Last-Modified: $lastModified, ETag: $etag, Size: $contentLength")

        // Report success and let the caller (MediaViewModel) handle decompression
        // This avoids duplicate decompression when Activity recreates during download
        // Check for .xz extension to handle both full list and diff files
        downloadCallback?.onDownloadSuccess(task.filename, shouldExtract = task.filename.endsWith(".xz"))
    }
}

private const val TAG = "DownloadService"
private const val BUFFER_SIZE = 1024
private const val MAX_RETRIES = 3