package cut.the.crap.shared.data

/**
 * Platform-specific file system operations.
 */
expect object FileSystem {
    /**
     * Get the app's cache directory path.
     */
    fun getCacheDirectory(): String

    /**
     * Get the app's documents directory path.
     */
    fun getDocumentsDirectory(): String

    /**
     * Check if a file exists at the given path.
     */
    fun fileExists(path: String): Boolean

    /**
     * Delete a file at the given path.
     * @return true if deleted, false otherwise
     */
    fun deleteFile(path: String): Boolean

    /**
     * Get the file size in bytes.
     * @return file size or -1 if file doesn't exist
     */
    fun getFileSize(path: String): Long

    /**
     * Get the file's last modified timestamp in milliseconds.
     * @return timestamp or 0 if file doesn't exist
     */
    fun getLastModified(path: String): Long

    /**
     * Create directories at the given path (including parents).
     * @return true if created or already exists
     */
    fun createDirectories(path: String): Boolean

    /**
     * Write bytes to a file.
     */
    fun writeBytes(path: String, data: ByteArray)

    /**
     * Read bytes from a file.
     */
    fun readBytes(path: String): ByteArray

    /**
     * Read text from a file with UTF-8 encoding.
     */
    fun readText(path: String): String
}
