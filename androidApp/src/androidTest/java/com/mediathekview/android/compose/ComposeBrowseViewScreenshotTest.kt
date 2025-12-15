package com.mediathekview.android.compose

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.mediathekview.android.utils.ScreenshotUtil
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumentation test for capturing screenshots of Compose BrowseView
 *
 * Run with:
 * ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.mediathekview.android.compose.ComposeBrowseViewScreenshotTest
 *
 * Or run specific test:
 * ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.mediathekview.android.compose.ComposeBrowseViewScreenshotTest#captureBrowseView
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ComposeBrowseViewScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComposeActivity>()

    @Test
    fun captureBrowseView() {
        // Wait for composition to complete
        composeTestRule.waitForIdle()
        Thread.sleep(1000) // Additional delay to ensure rendering is complete

        // Verify BrowseView is displayed by checking for channel list
        composeTestRule.onNodeWithText("3sat").assertExists()
        composeTestRule.onNodeWithText("phoenix").assertExists()

        // Capture screenshot
        val files = ScreenshotUtil.capture(
            name = "compose_browse_view",
            location = ScreenshotUtil.StorageLocation.EXTERNAL_STORAGE
        )

        // Copy to /sdcard/tmp for easy access
        copyToTmp(files, "screenshot_compose_browse.png")

        println("=== BrowseView Screenshot Captured ===")
        println("Screenshot saved to: /sdcard/tmp/screenshot_compose_browse.png")
        println("Pull with: adb pull /sdcard/tmp/screenshot_compose_browse.png ./tmp/")
    }

    @Test
    fun captureDetailView() {
        // Wait for composition
        composeTestRule.waitForIdle()
        Thread.sleep(1000)

        // Click on a title to navigate to detail view
        composeTestRule.onNodeWithText("Von Liebe und Leidenschaft").performClick()

        // Wait for navigation, rendering, and Toast to disappear
        composeTestRule.waitForIdle()
        Thread.sleep(3000) // Increased delay to let Toast disappear

        // Verify DetailView is displayed
        composeTestRule.onNodeWithText("Thema").assertExists()
        composeTestRule.onNodeWithText("1000 Inseln im Sankt-Lorenz-Strom").assertExists()

        // Capture screenshot
        val files = ScreenshotUtil.capture(
            name = "compose_detail_view",
            location = ScreenshotUtil.StorageLocation.EXTERNAL_STORAGE
        )

        // Copy to /sdcard for easy access
        copyToSdcard(files, "screenshot_compose_detail.png")

        println("=== DetailView Screenshot Captured ===")
        println("Screenshot saved to: /sdcard/screenshot_compose_detail.png")
        println("Pull with: adb pull /sdcard/screenshot_compose_detail.png ./tmp/")
    }

    @Test
    fun captureBothViews() {
        // Capture BrowseView
        composeTestRule.waitForIdle()
        Thread.sleep(1000)

        var files = ScreenshotUtil.capture(
            name = "compose_browse",
            location = ScreenshotUtil.StorageLocation.EXTERNAL_STORAGE
        )
        copyToTmp(files, "screenshot_compose_browse.png")

        // Navigate to DetailView
        composeTestRule.onNodeWithText("Von Liebe und Leidenschaft").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(1000)

        files = ScreenshotUtil.capture(
            name = "compose_detail",
            location = ScreenshotUtil.StorageLocation.EXTERNAL_STORAGE
        )
        copyToTmp(files, "screenshot_compose_detail.png")

        println("=== Both Screenshots Captured ===")
        println("BrowseView: /sdcard/tmp/screenshot_compose_browse.png")
        println("DetailView: /sdcard/tmp/screenshot_compose_detail.png")
        println("\nPull with:")
        println("adb pull /sdcard/tmp/screenshot_compose_browse.png ./tmp/")
        println("adb pull /sdcard/tmp/screenshot_compose_detail.png ./tmp/")
    }

    /**
     * Helper function to copy screenshot to /sdcard/tmp for easy adb pull access
     */
    private fun copyToTmp(files: List<File>, filename: String) {
        try {
            val sourceFile = files.firstOrNull() ?: run {
                println("No screenshot file to copy")
                return
            }

            val tmpDir = File("/sdcard/tmp")
            tmpDir.mkdirs()
            val destFile = File(tmpDir, filename)

            sourceFile.copyTo(destFile, overwrite = true)
            println("Copied to: ${destFile.absolutePath}")
        } catch (e: Exception) {
            println("Error copying to tmp: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Helper function to copy screenshot directly to /sdcard for easy access
     */
    private fun copyToSdcard(files: List<File>, filename: String) {
        try {
            val sourceFile = files.firstOrNull() ?: run {
                println("No screenshot file to copy")
                return
            }

            val destFile = File("/sdcard", filename)
            sourceFile.copyTo(destFile, overwrite = true)
            println("Copied to: ${destFile.absolutePath}")
        } catch (e: Exception) {
            println("Error copying to sdcard: ${e.message}")
            e.printStackTrace()
        }
    }
}
