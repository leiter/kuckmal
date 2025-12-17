package cut.the.crap.shared.data

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Cross-platform film list downloader.
 * Downloads and decompresses the Kuckmal film list.
 */
class FilmListDownloader {

    companion object {
        const val FILM_LIST_URL = "https://liste.mediathekview.de/Filmliste-akt.xz"
        const val FILM_LIST_FILENAME = "Filmliste-akt.json"
        const val TEMP_FILENAME = "Filmliste-akt.xz.tmp"
        private const val TAG = "FilmListDownloader"
        private const val MAX_AGE_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    /**
     * Download state for progress tracking.
     */
    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
        object Decompressing : DownloadState()
        data class Parsing(val entriesParsed: Int) : DownloadState()
        data class Complete(val filePath: String, val totalEntries: Int) : DownloadState()
        data class Error(val message: String, val exception: Throwable? = null) : DownloadState()
    }

    /**
     * Callback interface for download progress.
     */
    interface DownloadCallback {
        fun onProgress(bytesDownloaded: Long, totalBytes: Long)
        fun onDecompressing()
        fun onComplete(filePath: String)
        fun onError(error: Exception)
    }

    private val httpClient = HttpClientFactory.create()

    /**
     * Download and decompress the film list.
     *
     * @param targetDir Directory to save the decompressed file
     * @param callback Progress callback
     * @return Path to the decompressed JSON file, or null on error
     */
    suspend fun downloadFilmList(
        targetDir: String,
        callback: DownloadCallback
    ): String? = withContext(Dispatchers.IO) {
        val targetFile = "$targetDir/$FILM_LIST_FILENAME"
        val tempFile = "$targetDir/$TEMP_FILENAME"

        try {
            // Ensure target directory exists
            FileSystem.createDirectories(targetDir)

            PlatformLogger.info(TAG, "Downloading film list from $FILM_LIST_URL")

            // Download with progress tracking
            val response = httpClient.get(FILM_LIST_URL)

            if (!response.status.isSuccess()) {
                throw Exception("HTTP error: ${response.status}")
            }

            val totalBytes = response.contentLength() ?: -1L
            var bytesDownloaded = 0L

            // Read response body in chunks
            val channel = response.bodyAsChannel()
            val chunks = mutableListOf<ByteArray>()

            while (!channel.isClosedForRead) {
                val buffer = ByteArray(8192)
                val bytesRead = channel.readAvailable(buffer)
                if (bytesRead > 0) {
                    chunks.add(buffer.copyOf(bytesRead))
                    bytesDownloaded += bytesRead
                    callback.onProgress(bytesDownloaded, totalBytes)
                }
            }

            // Combine chunks into single array
            val compressedData = ByteArray(bytesDownloaded.toInt())
            var offset = 0
            for (chunk in chunks) {
                chunk.copyInto(compressedData, offset)
                offset += chunk.size
            }

            PlatformLogger.info(TAG, "Download complete: ${bytesDownloaded / 1024 / 1024} MB")

            // Write compressed data to temp file
            FileSystem.writeBytes(tempFile, compressedData)

            // Decompress XZ
            callback.onDecompressing()
            PlatformLogger.info(TAG, "Decompressing XZ file...")

            val decompressor = XzDecompressor()
            decompressor.decompressFile(tempFile, targetFile)

            // Clean up temp file
            FileSystem.deleteFile(tempFile)

            val fileSize = FileSystem.getFileSize(targetFile)
            PlatformLogger.info(TAG, "Decompression complete: ${fileSize / 1024 / 1024} MB")

            callback.onComplete(targetFile)
            targetFile

        } catch (e: Exception) {
            PlatformLogger.error(TAG, "Error downloading film list: ${e.message}", e)
            FileSystem.deleteFile(tempFile)
            callback.onError(e)
            null
        }
    }

    /**
     * Check if film list exists and is recent (less than 24 hours old).
     */
    fun isFilmListCurrent(targetDir: String): Boolean {
        val filePath = "$targetDir/$FILM_LIST_FILENAME"
        if (!FileSystem.fileExists(filePath)) return false

        val ageMs = currentTimeMillis() - FileSystem.getLastModified(filePath)
        return ageMs < MAX_AGE_MS
    }

    /**
     * Get the path to the film list file.
     */
    fun getFilmListPath(targetDir: String): String {
        return "$targetDir/$FILM_LIST_FILENAME"
    }

    /**
     * Close the HTTP client when done.
     */
    fun close() {
        httpClient.close()
    }
}

/**
 * Platform-specific time function.
 */
expect fun currentTimeMillis(): Long
