package cut.the.crap.desktop.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

/**
 * Desktop download manager for video files with cancellation support
 */
class DesktopDownloadManager {

    // Track cancellation state for active download
    private val isCancelled = AtomicBoolean(false)
    private var currentDownloadFile: File? = null

    /**
     * Cancel the current download
     */
    fun cancelDownload() {
        isCancelled.set(true)
    }

    /**
     * Check if a download is being cancelled
     */
    fun isCancelling(): Boolean = isCancelled.get()

    companion object {
        fun getDownloadsFolder(): File {
            val os = System.getProperty("os.name").lowercase()
            val userHome = System.getProperty("user.home")

            val downloadsDir = when {
                os.contains("win") -> File(userHome, "Downloads")
                os.contains("mac") -> File(userHome, "Downloads")
                else -> {
                    // Linux: check XDG_DOWNLOAD_DIR or default to ~/Downloads
                    val xdgDownload = System.getenv("XDG_DOWNLOAD_DIR")
                    if (xdgDownload != null) File(xdgDownload)
                    else File(userHome, "Downloads")
                }
            }

            // Create Kuckmal subfolder
            val mediaThekDir = File(downloadsDir, "Kuckmal")
            if (!mediaThekDir.exists()) {
                mediaThekDir.mkdirs()
            }

            return mediaThekDir
        }

        /**
         * Sanitize filename for filesystem
         */
        fun sanitizeFilename(name: String): String {
            return name
                .replace(Regex("[/\\\\:*?\"<>|]"), "_")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(200) // Limit length
        }
    }

    /**
     * Download state
     */
    sealed class DownloadState {
        object Starting : DownloadState()
        data class Progress(
            val bytesDownloaded: Long,
            val totalBytes: Long,
            val speedBytesPerSec: Long
        ) : DownloadState()
        data class Complete(val file: File) : DownloadState()
        data class Error(val message: String) : DownloadState()
        object Cancelled : DownloadState()
    }

    /**
     * Download a video file with progress updates and cancellation support
     */
    fun download(
        url: String,
        title: String,
        channel: String,
        quality: String
    ): Flow<DownloadState> = flow {
        // Reset cancellation state for new download
        isCancelled.set(false)
        currentDownloadFile = null

        emit(DownloadState.Starting)

        try {
            val downloadsDir = getDownloadsFolder()

            // Create filename from title
            val sanitizedTitle = sanitizeFilename(title)
            val sanitizedChannel = sanitizeFilename(channel)
            val extension = getExtensionFromUrl(url)
            val filename = "${sanitizedChannel}_${sanitizedTitle}_$quality.$extension"

            val targetFile = File(downloadsDir, filename)

            // Check if file already exists
            if (targetFile.exists()) {
                // Add number suffix
                var counter = 1
                var newFile: File
                do {
                    val newFilename = "${sanitizedChannel}_${sanitizedTitle}_${quality}_$counter.$extension"
                    newFile = File(downloadsDir, newFilename)
                    counter++
                } while (newFile.exists())
                downloadFile(url, newFile).collect { emit(it) }
            } else {
                downloadFile(url, targetFile).collect { emit(it) }
            }

        } catch (e: Exception) {
            emit(DownloadState.Error("Download failed: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    private fun downloadFile(url: String, targetFile: File): Flow<DownloadState> = flow {
        currentDownloadFile = targetFile

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 30000
        connection.readTimeout = 60000

        // Handle redirects
        connection.instanceFollowRedirects = true

        try {
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                emit(DownloadState.Error("Server returned HTTP $responseCode"))
                return@flow
            }

            val totalBytes = connection.contentLengthLong
            var bytesDownloaded = 0L
            var lastUpdateTime = System.currentTimeMillis()
            var lastBytesDownloaded = 0L

            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        // Check for cancellation
                        if (isCancelled.get() || !coroutineContext.isActive) {
                            output.close()
                            // Delete partial file
                            if (targetFile.exists()) {
                                targetFile.delete()
                            }
                            emit(DownloadState.Cancelled)
                            return@flow
                        }

                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead

                        // Update progress every 100ms
                        val now = System.currentTimeMillis()
                        if (now - lastUpdateTime > 100) {
                            val elapsed = (now - lastUpdateTime) / 1000.0
                            val speed = if (elapsed > 0) {
                                ((bytesDownloaded - lastBytesDownloaded) / elapsed).toLong()
                            } else 0L

                            emit(DownloadState.Progress(bytesDownloaded, totalBytes, speed))

                            lastUpdateTime = now
                            lastBytesDownloaded = bytesDownloaded
                        }
                    }
                }
            }

            emit(DownloadState.Complete(targetFile))
        } finally {
            connection.disconnect()
            currentDownloadFile = null
        }
    }

    private fun getExtensionFromUrl(url: String): String {
        return when {
            url.contains(".mp4") -> "mp4"
            url.contains(".webm") -> "webm"
            url.contains(".m3u8") -> "mp4" // HLS streams saved as mp4
            else -> "mp4"
        }
    }
}
