package cut.the.crap.shared.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual object FileSystem {

    actual fun getCacheDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSCachesDirectory,
            NSUserDomainMask,
            true
        )
        return (paths.firstOrNull() as? String) ?: ""
    }

    actual fun getDocumentsDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        )
        return (paths.firstOrNull() as? String) ?: ""
    }

    actual fun fileExists(path: String): Boolean {
        return NSFileManager.defaultManager.fileExistsAtPath(path)
    }

    actual fun deleteFile(path: String): Boolean {
        return try {
            NSFileManager.defaultManager.removeItemAtPath(path, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    actual fun getFileSize(path: String): Long {
        val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, null)
        return (attrs?.get(NSFileSize) as? NSNumber)?.longValue ?: -1L
    }

    actual fun getLastModified(path: String): Long {
        val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, null)
        val date = attrs?.get(NSFileModificationDate) as? NSDate
        return date?.let { (it.timeIntervalSince1970 * 1000).toLong() } ?: 0L
    }

    actual fun createDirectories(path: String): Boolean {
        return try {
            NSFileManager.defaultManager.createDirectoryAtPath(
                path,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    actual fun writeBytes(path: String, data: ByteArray) {
        val nsData = data.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = data.size.toULong())
        }
        nsData.writeToFile(path, atomically = true)
    }

    actual fun readBytes(path: String): ByteArray {
        val nsData = NSData.dataWithContentsOfFile(path) ?: return ByteArray(0)
        val size = nsData.length.toInt()
        if (size == 0) return ByteArray(0)

        val bytes = ByteArray(size)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), nsData.bytes, nsData.length)
        }
        return bytes
    }

    actual fun readText(path: String): String {
        return NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) ?: ""
    }
}
