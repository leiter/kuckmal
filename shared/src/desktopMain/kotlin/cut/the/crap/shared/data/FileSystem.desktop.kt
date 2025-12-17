package cut.the.crap.shared.data

import java.io.File

actual object FileSystem {
    private val userHome = System.getProperty("user.home")
    private val appName = "Kuckmal"

    actual fun getCacheDirectory(): String {
        val os = System.getProperty("os.name").lowercase()
        val cacheDir = when {
            os.contains("mac") -> "$userHome/Library/Caches/$appName"
            os.contains("win") -> System.getenv("LOCALAPPDATA")?.let { "$it/$appName/cache" }
                ?: "$userHome/AppData/Local/$appName/cache"
            else -> "$userHome/.cache/$appName"
        }
        File(cacheDir).mkdirs()
        return cacheDir
    }

    actual fun getDocumentsDirectory(): String {
        val os = System.getProperty("os.name").lowercase()
        val docsDir = when {
            os.contains("mac") -> "$userHome/Library/Application Support/$appName"
            os.contains("win") -> System.getenv("APPDATA")?.let { "$it/$appName" }
                ?: "$userHome/AppData/Roaming/$appName"
            else -> "$userHome/.local/share/$appName"
        }
        File(docsDir).mkdirs()
        return docsDir
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
