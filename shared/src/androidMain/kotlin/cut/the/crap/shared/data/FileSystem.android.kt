package cut.the.crap.shared.data

import android.os.Environment
import java.io.File

actual object FileSystem {
    // These will be set by the Android app during initialization
    private var cacheDir: String = ""
    private var documentsDir: String = ""

    /**
     * Initialize file system with Android context directories.
     * Call this from Application.onCreate() or MainActivity.
     */
    fun initialize(cacheDirectory: File, filesDirectory: File) {
        cacheDir = cacheDirectory.absolutePath
        documentsDir = filesDirectory.absolutePath
    }

    actual fun getCacheDirectory(): String {
        return cacheDir.ifEmpty {
            // Fallback to external cache if not initialized
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        }
    }

    actual fun getDocumentsDirectory(): String {
        return documentsDir.ifEmpty {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
        }
    }

    actual fun fileExists(path: String): Boolean {
        return File(path).exists()
    }

    actual fun deleteFile(path: String): Boolean {
        return File(path).delete()
    }

    actual fun getFileSize(path: String): Long {
        val file = File(path)
        return if (file.exists()) file.length() else -1L
    }

    actual fun getLastModified(path: String): Long {
        val file = File(path)
        return if (file.exists()) file.lastModified() else 0L
    }

    actual fun createDirectories(path: String): Boolean {
        return File(path).mkdirs() || File(path).exists()
    }

    actual fun writeBytes(path: String, data: ByteArray) {
        File(path).writeBytes(data)
    }

    actual fun readBytes(path: String): ByteArray {
        return File(path).readBytes()
    }

    actual fun readText(path: String): String {
        return File(path).readText(Charsets.UTF_8)
    }
}
