package cut.the.crap.shared.data

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

/**
 * iOS XZ decompressor that delegates to a native Swift implementation.
 * The Swift implementation must be registered at app startup via [registerDecompressor].
 */
actual class XzDecompressor actual constructor() {

    companion object {
        private const val TAG = "XzDecompressor"

        /**
         * Native decompressor function registered from Swift.
         * Signature: (NSData) -> NSData?
         */
        private var nativeDecompressor: ((NSData) -> NSData?)? = null

        /**
         * Native file decompressor function registered from Swift.
         * Signature: (inputPath: String, outputPath: String) -> Boolean
         */
        private var nativeFileDecompressor: ((String, String) -> Boolean)? = null

        /**
         * Register the native Swift decompressor.
         * Call this from iOS app initialization before using XzDecompressor.
         *
         * @param decompressor Function that takes compressed NSData and returns decompressed NSData
         * @param fileDecompressor Function that decompresses a file from inputPath to outputPath
         */
        fun registerDecompressor(
            decompressor: (NSData) -> NSData?,
            fileDecompressor: (String, String) -> Boolean
        ) {
            nativeDecompressor = decompressor
            nativeFileDecompressor = fileDecompressor
            PlatformLogger.info(TAG, "Native XZ decompressor registered")
        }

        /**
         * Check if XZ decompression is supported on this platform.
         */
        fun isSupported(): Boolean = nativeDecompressor != null
    }

    actual fun decompress(inputData: ByteArray): ByteArray {
        val decompressor = nativeDecompressor
            ?: throw UnsupportedOperationException(
                "XZ decompressor not registered. Call XzDecompressor.registerDecompressor() from iOS app initialization."
            )

        val inputNSData = inputData.toNSData()
        val outputNSData = decompressor(inputNSData)
            ?: throw Exception("XZ decompression failed")

        return outputNSData.toByteArray()
    }

    actual suspend fun decompressFile(inputPath: String, outputPath: String) {
        withContext(Dispatchers.IO) {
            val fileDecompressor = nativeFileDecompressor
                ?: throw UnsupportedOperationException(
                    "XZ decompressor not registered. Call XzDecompressor.registerDecompressor() from iOS app initialization."
                )

            val success = fileDecompressor(inputPath, outputPath)
            if (!success) {
                throw Exception("XZ file decompression failed: $inputPath -> $outputPath")
            }

            PlatformLogger.info(TAG, "Decompressed file: $inputPath -> $outputPath")
        }
    }

    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
    private fun ByteArray.toNSData(): NSData {
        if (this.isEmpty()) return NSData()
        return this.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
        }
    }

    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    private fun NSData.toByteArray(): ByteArray {
        val size = this.length.toInt()
        if (size == 0) return ByteArray(0)

        val bytes = ByteArray(size)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
        return bytes
    }
}
