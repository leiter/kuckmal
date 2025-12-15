package cut.the.crap.android.utils

import android.graphics.Bitmap
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.Screenshot
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for capturing screenshots during instrumentation tests
 *
 * Screenshots are saved to:
 * - Primary: app/build/outputs/androidTest-results/screenshots/
 * - External (optional): /sdcard/Pictures/Kuckmal/test-screenshots/
 */
object ScreenshotUtil {

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /**
     * Storage location for screenshots
     */
    enum class StorageLocation {
        /** Standard build outputs directory */
        BUILD_OUTPUT,
        /** External storage for easy manual access */
        EXTERNAL_STORAGE,
        /** Both locations */
        BOTH
    }

    /**
     * Captures a screenshot and saves it with the given name
     *
     * @param name Base name for the screenshot file (without extension)
     * @param location Where to save the screenshot (default: BUILD_OUTPUT)
     * @return List of File objects where the screenshot was saved
     */
    fun capture(
        name: String,
        location: StorageLocation = StorageLocation.BUILD_OUTPUT
    ): List<File> {
        val timestamp = dateFormat.format(Date())
        val fileName = "${name}_${timestamp}.png"

        val savedFiles = mutableListOf<File>()

        // Capture using Screenshot API
        val capture = Screenshot.capture()
        capture.format = Bitmap.CompressFormat.PNG
        capture.name = name

        val bitmap = capture.bitmap

        if (location == StorageLocation.BUILD_OUTPUT || location == StorageLocation.BOTH) {
            saveToBuildOutput(bitmap, fileName)?.let { savedFiles.add(it) }
        }

        if (location == StorageLocation.EXTERNAL_STORAGE || location == StorageLocation.BOTH) {
            saveToExternalStorage(bitmap, fileName)?.let { savedFiles.add(it) }
        }

        return savedFiles
    }

    /**
     * Saves screenshot to build output directory
     */
    private fun saveToBuildOutput(bitmap: Bitmap, fileName: String): File? {
        return try {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val screenshotDir = File(context.filesDir, "screenshots")
            screenshotDir.mkdirs()

            val file = File(screenshotDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            println("Screenshot saved to: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            println("Failed to save screenshot to build output: ${e.message}")
            null
        }
    }

    /**
     * Saves screenshot to external storage
     * Requires WRITE_EXTERNAL_STORAGE permission for API < 29
     */
    private fun saveToExternalStorage(bitmap: Bitmap, fileName: String): File? {
        return try {
            val externalDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use app-specific directory (no permission needed)
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                File(context.getExternalFilesDir(null), "test-screenshots")
            } else {
                // Use public Pictures directory
                File("/sdcard/Pictures/Kuckmal/test-screenshots")
            }

            externalDir.mkdirs()
            val file = File(externalDir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            println("Screenshot saved to external: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            println("Failed to save screenshot to external storage: ${e.message}")
            null
        }
    }
}
