package cut.the.crap.desktop.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tukaani.xz.XZInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads and decompresses the MediathekView film list
 */
class FilmListDownloader {

    companion object {
        const val FILM_LIST_URL = "https://liste.mediathekview.de/Filmliste-akt.xz"
        const val FILM_LIST_FILENAME = "Filmliste-akt.json"
    }

    /**
     * Download progress callback
     */
    interface DownloadCallback {
        fun onProgress(bytesDownloaded: Long, totalBytes: Long)
        fun onDecompressing()
        fun onComplete(filePath: String)
        fun onError(error: Exception)
    }

    /**
     * Download and decompress the film list
     */
    suspend fun downloadFilmList(
        targetDir: String,
        callback: DownloadCallback
    ): String? = withContext(Dispatchers.IO) {
        val targetFile = File(targetDir, FILM_LIST_FILENAME)
        val tempFile = File(targetDir, "Filmliste-akt.xz.tmp")

        try {
            // Ensure target directory exists
            File(targetDir).mkdirs()

            // Download XZ file
            println("Downloading film list from $FILM_LIST_URL")
            val url = URL(FILM_LIST_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000

            val totalBytes = connection.contentLengthLong
            var bytesDownloaded = 0L

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead
                        callback.onProgress(bytesDownloaded, totalBytes)
                    }
                }
            }

            println("Download complete: ${bytesDownloaded / 1024 / 1024} MB")

            // Decompress XZ
            callback.onDecompressing()
            println("Decompressing XZ file...")

            XZInputStream(tempFile.inputStream()).use { xzInput ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (xzInput.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }

            // Clean up temp file
            tempFile.delete()

            println("Decompression complete: ${targetFile.length() / 1024 / 1024} MB")
            callback.onComplete(targetFile.absolutePath)

            targetFile.absolutePath

        } catch (e: Exception) {
            println("Error downloading film list: ${e.message}")
            tempFile.delete()
            callback.onError(e)
            null
        }
    }

    /**
     * Check if film list exists and is recent (less than 24 hours old)
     */
    fun isFilmListCurrent(targetDir: String): Boolean {
        val file = File(targetDir, FILM_LIST_FILENAME)
        if (!file.exists()) return false

        val ageMs = System.currentTimeMillis() - file.lastModified()
        val maxAgeMs = 24 * 60 * 60 * 1000 // 24 hours

        return ageMs < maxAgeMs
    }
}
