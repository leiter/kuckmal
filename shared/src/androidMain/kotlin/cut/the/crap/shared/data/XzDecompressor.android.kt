package cut.the.crap.shared.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tukaani.xz.XZInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream

actual class XzDecompressor actual constructor() {

    actual fun decompress(inputData: ByteArray): ByteArray {
        val inputStream = ByteArrayInputStream(inputData)
        val outputStream = ByteArrayOutputStream()

        XZInputStream(inputStream).use { xzInput ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (xzInput.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
        }

        return outputStream.toByteArray()
    }

    actual suspend fun decompressFile(inputPath: String, outputPath: String) {
        withContext(Dispatchers.IO) {
            FileInputStream(inputPath).use { fileInput ->
                XZInputStream(fileInput).use { xzInput ->
                    FileOutputStream(outputPath).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (xzInput.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                }
            }
        }
    }
}
