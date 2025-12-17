package cut.the.crap.shared.data

/**
 * Platform-specific file system operations.
 */
expect object FileSystem {
    fun getCacheDirectory(): String
    fun getDocumentsDirectory(): String
    fun fileExists(path: String): Boolean
    fun deleteFile(path: String): Boolean
    fun getFileSize(path: String): Long
    fun getLastModified(path: String): Long
    fun createDirectories(path: String): Boolean
    fun writeBytes(path: String, data: ByteArray)
    fun readBytes(path: String): ByteArray
    fun readText(path: String): String
}
