package cut.the.crap.shared.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*

/**
 * iOS file-based logger for crash debugging.
 * Writes logs to Documents folder with immediate flush to ensure logs persist before crash.
 */
@OptIn(ExperimentalForeignApi::class)
object IosFileLogger {
    private var fileHandle: NSFileHandle? = null
    private var logFilePath: String? = null
    private var isInitialized = false

    /**
     * Initialize the file logger. Creates a new log file with timestamp.
     */
    fun initialize() {
        if (isInitialized) return

        try {
            val fileManager = NSFileManager.defaultManager
            val documentsUrl = fileManager.URLsForDirectory(
                NSDocumentDirectory,
                NSUserDomainMask
            ).firstOrNull() as? NSURL

            if (documentsUrl == null) {
                NSLog("IosFileLogger: Cannot get Documents directory")
                return
            }

            // Create timestamp for filename
            val dateFormatter = NSDateFormatter()
            dateFormatter.dateFormat = "yyyy-MM-dd_HH-mm-ss"
            val timestamp = dateFormatter.stringFromDate(NSDate())

            val fileName = "kuckmal_debug_$timestamp.log"
            val fileUrl = documentsUrl.URLByAppendingPathComponent(fileName)

            if (fileUrl == null) {
                NSLog("IosFileLogger: Cannot create file URL")
                return
            }

            logFilePath = fileUrl.path

            // Create empty file
            fileManager.createFileAtPath(
                path = logFilePath!!,
                contents = null,
                attributes = null
            )

            // Open file handle for writing
            fileHandle = NSFileHandle.fileHandleForWritingAtPath(logFilePath!!)

            if (fileHandle != null) {
                isInitialized = true
                log("INFO", "IosFileLogger", "Log file initialized: $logFilePath")
                NSLog("IosFileLogger: Initialized at $logFilePath")
            } else {
                NSLog("IosFileLogger: Failed to create file handle for $logFilePath")
            }
        } catch (e: Exception) {
            NSLog("IosFileLogger: Initialization error - ${e.message}")
        }
    }

    /**
     * Log a message to the file with immediate flush.
     */
    fun log(level: String, tag: String, message: String) {
        if (!isInitialized) {
            initialize()
        }

        val handle = fileHandle ?: return

        try {
            // Create timestamp
            val dateFormatter = NSDateFormatter()
            dateFormatter.dateFormat = "HH:mm:ss.SSS"
            val timestamp = dateFormatter.stringFromDate(NSDate())

            // Format log line
            val logLine = "[$timestamp] $level/$tag: $message\n"

            // Convert to NSData and write
            val data = logLine.encodeToByteArray()
            data.usePinned { pinned ->
                val nsData = NSData.dataWithBytes(pinned.addressOf(0), data.size.toULong())
                handle.writeData(nsData)
            }

            // Force flush to disk
            handle.synchronizeFile()
        } catch (e: Exception) {
            NSLog("IosFileLogger: Write error - ${e.message}")
        }
    }

    /**
     * Close the file handle.
     */
    fun close() {
        try {
            fileHandle?.closeFile()
            fileHandle = null
            isInitialized = false
        } catch (e: Exception) {
            NSLog("IosFileLogger: Close error - ${e.message}")
        }
    }

    /**
     * Get the current log file path.
     */
    fun getLogFilePath(): String? = logFilePath
}
