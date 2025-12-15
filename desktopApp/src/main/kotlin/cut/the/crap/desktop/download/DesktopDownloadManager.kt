package cut.the.crap.desktop.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Desktop download manager for video files
 */
class DesktopDownloadManager {

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
    }

    /**
     * Download a video file with progress updates
     */
    fun download(
        url: String,
        title: String,
        channel: String,
        quality: String
    ): Flow<DownloadState> = flow {
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
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 30000
        connection.readTimeout = 60000

        // Handle redirects
        connection.instanceFollowRedirects = true

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
