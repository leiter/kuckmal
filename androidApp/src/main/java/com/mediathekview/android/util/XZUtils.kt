package com.mediathekview.android.util

import android.util.Log
import org.tukaani.xz.XZInputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.Channels

/**
 * Utility class for XZ decompression
 */
object XZUtils {
    private const val TAG = "XZUtils"

    // Memory limit for XZ decompression - reduced to 32MB to fit within Android heap limits
    // This is the dictionary size limit for the XZ decompressor
    private const val MEMORY_LIMIT_MB = 32

    @JvmStatic
    @Throws(IOException::class)
    fun decode(inputPath: String?, outputPath: String?) {
        // Suggest garbage collection before decompression to free up memory
        System.gc()
        Log.d(TAG, "Starting XZ decompression of $inputPath")

        try {
            FileInputStream(inputPath).use { fis ->
                // Create XZInputStream without memory limit - rely on largeHeap in manifest
                // Setting a limit can cause issues if the file was compressed with a larger dictionary
                XZInputStream(fis).use { xz ->
                    FileOutputStream(outputPath).use { fos ->
                        // Use FileChannel for more efficient I/O
                        val inputChannel = Channels.newChannel(xz)
                        val outputChannel = fos.channel

                        // Detect available memory and adapt buffer size
                        val runtime = Runtime.getRuntime()
                        val maxMemory = runtime.maxMemory()
                        val availableMemory = maxMemory - (runtime.totalMemory() - runtime.freeMemory())

                        // Use larger buffer on devices with more memory
                        // Low memory (<1GB heap): 64KB
                        // Medium memory (1-2GB): 256KB
                        // High memory (>2GB): 512KB
                        val bufferSize = when {
                            maxMemory < 1024 * 1024 * 1024 -> 64 * 1024  // 64KB for low-memory devices
                            maxMemory < 2048 * 1024 * 1024 -> 256 * 1024 // 256KB for medium-memory devices
                            else -> 512 * 1024  // 512KB for high-memory devices
                        }

                        Log.d(TAG, "Using ${bufferSize / 1024}KB buffer for decompression (heap: ${maxMemory / 1024 / 1024}MB)")

                        val buffer = java.nio.ByteBuffer.allocateDirect(bufferSize)
                        var totalBytes = 0L
                        var lastLogBytes = 0L

                        while (inputChannel.read(buffer) != -1) {
                            buffer.flip()

                            // Write all buffered data
                            while (buffer.hasRemaining()) {
                                totalBytes += outputChannel.write(buffer)
                            }
                            buffer.clear()

                            // Log progress every 10MB
                            if (totalBytes - lastLogBytes >= 10 * 1024 * 1024) {
                                Log.d(TAG, "Decompressed ${totalBytes / (1024 * 1024)} MB")
                                lastLogBytes = totalBytes
                            }
                        }

                        // Force write to disk
                        outputChannel.force(true)

                        Log.i(TAG, "Successfully decompressed $totalBytes bytes")
                    }
                }
            }
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory during XZ decompression", e)
            throw IOException("Out of memory: File too large to decompress. Try closing other apps.", e)
        } catch (e: org.tukaani.xz.MemoryLimitException) {
            Log.e(TAG, "XZ memory limit exceeded", e)
            throw IOException("File requires too much memory to decompress (>${MEMORY_LIMIT_MB}MB)", e)
        }
    }
}
