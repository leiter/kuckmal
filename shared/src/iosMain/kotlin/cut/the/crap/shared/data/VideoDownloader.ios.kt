@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package cut.the.crap.shared.data

import cut.the.crap.shared.database.MediaEntry
import kotlinx.cinterop.ObjCSignatureOverride
import platform.Foundation.*
import platform.darwin.NSObject

/**
 * iOS Video Downloader using NSURLSession.
 * Downloads video files to the Documents directory where users can access them via Files app.
 */
object VideoDownloader {

    private const val TAG = "VideoDownloader"
    private const val DOWNLOAD_FOLDER = "Downloads"

    /**
     * Callback for download progress updates
     */
    var onProgressUpdate: ((String, Int) -> Unit)? = null

    /**
     * Callback for download completion
     */
    var onDownloadComplete: ((String, String, Boolean, String?) -> Unit)? = null

    /**
     * Download a video file from the given URL.
     *
     * @param entry The media entry being downloaded
     * @param url The URL of the video to download
     * @param quality Quality label (HD/SD)
     */
    fun downloadVideo(entry: MediaEntry, url: String, quality: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl == null) {
            PlatformLogger.error(TAG, "Invalid URL: $url")
            onDownloadComplete?.invoke(entry.title, url, false, "Invalid URL")
            return
        }

        // Create download directory if needed
        val documentsDir = FileSystem.getDocumentsDirectory()
        val downloadDir = "$documentsDir/$DOWNLOAD_FOLDER/${sanitizeFilename(entry.channel)}"
        FileSystem.createDirectories(downloadDir)

        // Generate filename
        val fileExtension = getFileExtension(url)
        val sanitizedTitle = sanitizeFilename(entry.title)
        val filename = "${sanitizedTitle}_$quality$fileExtension"
        val destinationPath = "$downloadDir/$filename"

        // Check if file already exists
        if (FileSystem.fileExists(destinationPath)) {
            PlatformLogger.info(TAG, "File already exists: $destinationPath")
            onDownloadComplete?.invoke(entry.title, destinationPath, true, null)
            return
        }

        PlatformLogger.info(TAG, "Starting download: $url -> $destinationPath")

        // Create download task
        val session = NSURLSession.sharedSession
        val downloadTask = session.downloadTaskWithURL(nsUrl) { location, response, error ->
            if (error != null) {
                PlatformLogger.error(TAG, "Download error: ${error.localizedDescription}")
                NSOperationQueue.mainQueue.addOperationWithBlock {
                    onDownloadComplete?.invoke(entry.title, url, false, error.localizedDescription)
                }
                return@downloadTaskWithURL
            }

            if (location == null) {
                PlatformLogger.error(TAG, "No download location returned")
                NSOperationQueue.mainQueue.addOperationWithBlock {
                    onDownloadComplete?.invoke(entry.title, url, false, "Download failed - no file")
                }
                return@downloadTaskWithURL
            }

            // Move downloaded file to destination
            val fileManager = NSFileManager.defaultManager
            val destinationUrl = NSURL.fileURLWithPath(destinationPath)

            try {
                // Remove existing file if any
                if (fileManager.fileExistsAtPath(destinationPath)) {
                    fileManager.removeItemAtPath(destinationPath, null)
                }

                // Move temp file to destination
                val success = fileManager.moveItemAtURL(location, destinationUrl, null)
                if (success) {
                    PlatformLogger.info(TAG, "Download complete: $destinationPath")

                    // Get file size for logging
                    val fileSize = FileSystem.getFileSize(destinationPath)
                    PlatformLogger.info(TAG, "Downloaded file size: ${fileSize / 1024 / 1024} MB")

                    NSOperationQueue.mainQueue.addOperationWithBlock {
                        onDownloadComplete?.invoke(entry.title, destinationPath, true, null)
                    }
                } else {
                    PlatformLogger.error(TAG, "Failed to move downloaded file")
                    NSOperationQueue.mainQueue.addOperationWithBlock {
                        onDownloadComplete?.invoke(entry.title, url, false, "Failed to save file")
                    }
                }
            } catch (e: Exception) {
                PlatformLogger.error(TAG, "Error moving file: ${e.message}")
                NSOperationQueue.mainQueue.addOperationWithBlock {
                    onDownloadComplete?.invoke(entry.title, url, false, e.message)
                }
            }
        }

        downloadTask.resume()
    }

    /**
     * Download video with progress tracking using a delegate-based approach.
     */
    fun downloadVideoWithProgress(entry: MediaEntry, url: String, quality: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl == null) {
            PlatformLogger.error(TAG, "Invalid URL: $url")
            onDownloadComplete?.invoke(entry.title, url, false, "Invalid URL")
            return
        }

        // Create download directory if needed
        val documentsDir = FileSystem.getDocumentsDirectory()
        val downloadDir = "$documentsDir/$DOWNLOAD_FOLDER/${sanitizeFilename(entry.channel)}"
        FileSystem.createDirectories(downloadDir)

        // Generate filename
        val fileExtension = getFileExtension(url)
        val sanitizedTitle = sanitizeFilename(entry.title)
        val filename = "${sanitizedTitle}_$quality$fileExtension"
        val destinationPath = "$downloadDir/$filename"

        // Check if file already exists
        if (FileSystem.fileExists(destinationPath)) {
            PlatformLogger.info(TAG, "File already exists: $destinationPath")
            onDownloadComplete?.invoke(entry.title, destinationPath, true, null)
            return
        }

        PlatformLogger.info(TAG, "Starting download with progress: $url -> $destinationPath")

        // Create session configuration
        val config = NSURLSessionConfiguration.defaultSessionConfiguration

        // Create delegate for progress tracking
        val delegate = DownloadDelegate(entry.title, destinationPath)

        // Create session with delegate
        val session = NSURLSession.sessionWithConfiguration(
            configuration = config,
            delegate = delegate,
            delegateQueue = NSOperationQueue.mainQueue
        )

        val downloadTask = session.downloadTaskWithURL(nsUrl)
        downloadTask.resume()
    }

    /**
     * Sanitize filename by removing invalid characters.
     */
    private fun sanitizeFilename(name: String): String {
        return name
            .replace(Regex("[^a-zA-Z0-9äöüÄÖÜß._\\- ]"), "_")
            .replace(Regex("\\s+"), "_")
            .take(100)
    }

    /**
     * Get file extension from URL.
     */
    private fun getFileExtension(url: String): String {
        return when {
            url.contains(".mp4", ignoreCase = true) -> ".mp4"
            url.contains(".m3u8", ignoreCase = true) -> ".m3u8"
            url.contains(".webm", ignoreCase = true) -> ".webm"
            url.contains(".mov", ignoreCase = true) -> ".mov"
            else -> ".mp4"
        }
    }

    /**
     * NSURLSession delegate for tracking download progress.
     */
    private class DownloadDelegate(
        private val title: String,
        private val destinationPath: String
    ) : NSObject(), NSURLSessionDownloadDelegateProtocol {

        @ObjCSignatureOverride
        override fun URLSession(
            session: NSURLSession,
            downloadTask: NSURLSessionDownloadTask,
            didFinishDownloadingToURL: NSURL
        ) {
            val location = didFinishDownloadingToURL
            val fileManager = NSFileManager.defaultManager
            val destinationUrl = NSURL.fileURLWithPath(destinationPath)

            try {
                // Remove existing file if any
                if (fileManager.fileExistsAtPath(destinationPath)) {
                    fileManager.removeItemAtPath(destinationPath, null)
                }

                // Move temp file to destination
                val success = fileManager.moveItemAtURL(location, destinationUrl, null)
                if (success) {
                    PlatformLogger.info(TAG, "Download complete: $destinationPath")
                    onDownloadComplete?.invoke(title, destinationPath, true, null)
                } else {
                    PlatformLogger.error(TAG, "Failed to move downloaded file")
                    onDownloadComplete?.invoke(title, destinationPath, false, "Failed to save file")
                }
            } catch (e: Exception) {
                PlatformLogger.error(TAG, "Error moving file: ${e.message}")
                onDownloadComplete?.invoke(title, destinationPath, false, e.message)
            }
        }

        @ObjCSignatureOverride
        override fun URLSession(
            session: NSURLSession,
            downloadTask: NSURLSessionDownloadTask,
            didWriteData: Long,
            totalBytesWritten: Long,
            totalBytesExpectedToWrite: Long
        ) {
            if (totalBytesExpectedToWrite > 0) {
                val progress = ((totalBytesWritten * 100) / totalBytesExpectedToWrite).toInt()
                onProgressUpdate?.invoke(title, progress)
            }
        }

        @ObjCSignatureOverride
        override fun URLSession(
            session: NSURLSession,
            task: NSURLSessionTask,
            didCompleteWithError: NSError?
        ) {
            val error = didCompleteWithError
            if (error != null) {
                PlatformLogger.error(TAG, "Download failed: ${error.localizedDescription}")
                onDownloadComplete?.invoke(title, destinationPath, false, error.localizedDescription)
            }
        }
    }
}
