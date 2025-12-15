package com.mediathekview.android.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.mediathekview.android.R
import com.mediathekview.android.utils.ScreenshotUtil
import com.mediathekview.android.utils.uiTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Automated screenshot capture test
 *
 * This test replaces the manual adb screenshot workflow with automated
 * screenshot capture. Screenshots are saved to the tmp/ directory for
 * easy access and documentation purposes.
 *
 * Run with:
 * ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.mediathekview.android.ui.ScreenshotCaptureTest
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ScreenshotCaptureTest {

    private lateinit var scenario: ActivityScenario<MediaActivity>
    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Ensure device is in portrait orientation initially
        device.setOrientationNatural()

        // Launch the main activity
        scenario = ActivityScenario.launch(MediaActivity::class.java)
    }

    /**
     * Capture comprehensive screenshots of the app in various states
     * Screenshots are saved to tmp/ directory
     */
    @Test
    fun captureAllScreenshots() = uiTest("CaptureScreenshots") {
        // Wait for app to fully load
        delay(3000)

        // === PORTRAIT MODE - BROWSE VIEW ===
        screenshot("current", ScreenshotUtil.StorageLocation.EXTERNAL_STORAGE)

        action("Capture initial browse view") {
            copyScreenshotToTmp("screenshot_current.png")
        }

        // === SELECT CHANNEL ===
        action("Select 3Sat channel (first in list)") {
            clickListItem(R.id.channelList, 0)
        }
        delay(1500)

        screenshot("channel_selected", ScreenshotUtil.StorageLocation.EXTERNAL_STORAGE)

        // === SELECT THEME ===
        action("Select first theme") {
            clickListItem(R.id.contentList, 0)
        }
        delay(1500)

        screenshot("theme_selected", ScreenshotUtil.StorageLocation.EXTERNAL_STORAGE)

        // === SELECT TITLE TO SHOW DETAIL VIEW ===
        action("Select first title to show detail view") {
            clickListItem(R.id.contentList, 0)
        }
        delay(2000)

        screenshot("detail_view_portrait", ScreenshotUtil.StorageLocation.EXTERNAL_STORAGE)
        action("Capture portrait detail view") {
            copyScreenshotToTmp("screenshot_detail_portrait.png")
        }

        // === ROTATE TO LANDSCAPE ===
        action("Rotate to landscape") {
            device.setOrientationLeft()
        }
        delay(2000)

        screenshot("detail_view_landscape", ScreenshotUtil.StorageLocation.EXTERNAL_STORAGE)
        action("Capture landscape detail view") {
            copyScreenshotToTmp("screenshot_landscape_with_detail.png")
        }

        // Scroll down in detail view to show more content
        action("Scroll down to see more detail content") {
            swipe(
                startX = 500,
                startY = 1000,
                endX = 500,
                endY = 300,
                steps = 20
            )
        }
        delay(1000)

        screenshot("detail_view_landscape_scrolled", ScreenshotUtil.StorageLocation.EXTERNAL_STORAGE)
        action("Capture landscape detail view scrolled") {
            copyScreenshotToTmp("screenshot_landscape_detail_complete.png")
        }

        // === BACK TO PORTRAIT ===
        action("Rotate back to portrait") {
            device.setOrientationNatural()
        }
        delay(1500)

        // === GO BACK TO BROWSE VIEW ===
        pressBack()
        delay(1000)
        pressBack()
        delay(1000)

        screenshot("back_to_browse", ScreenshotUtil.StorageLocation.EXTERNAL_STORAGE)

        // === TEST DIFFERENT CHANNELS ===
        val channelIndices = listOf(1, 2, 3, 4)
        channelIndices.forEach { index ->
            action("Capture channel at index $index") {
                clickListItem(R.id.channelList, index)
            }
            delay(1000)
            screenshot("channel_$index", ScreenshotUtil.StorageLocation.EXTERNAL_STORAGE)
        }

        // Return to first channel
        clickListItem(R.id.channelList, 0)
        delay(1000)

        println("=== Screenshot Capture Complete ===")
        println("Screenshots saved to:")
        println("1. External storage: /sdcard/Android/data/com.mediathekview.android/files/test-screenshots/")
        println("2. tmp/ directory (via copyScreenshotToTmp)")
        println("")
        println("To pull screenshots:")
        println("adb pull /sdcard/Android/data/com.mediathekview.android/files/test-screenshots/ ./tmp/")
    }

    /**
     * Capture screenshots for specific drawable/icon testing
     */
    @Test
    fun captureDrawableReferences() = uiTest("DrawableRefs") {
        delay(2000)

        // Capture main view showing all channel icons
        screenshot("drawable_refs_main", ScreenshotUtil.StorageLocation.EXTERNAL_STORAGE)
        action("Capture channel list with all icons") {
            copyScreenshotToTmp("screenshot_drawable_refs.png")
        }

        // Capture detail view with channel icon
        clickListItem(R.id.channelList, 0)
        delay(1000)
        clickListItem(R.id.contentList, 0)
        delay(1000)
        clickListItem(R.id.contentList, 0)
        delay(2000)

        screenshot("detail_channel_icon", ScreenshotUtil.StorageLocation.EXTERNAL_STORAGE)
        action("Capture detail view showing channel icon") {
            copyScreenshotToTmp("screenshot_detail_channel_icon.png")
        }

        pressBack()
        delay(500)
        pressBack()
        delay(500)

        screenshot("detail_with_icon", ScreenshotUtil.StorageLocation.EXTERNAL_STORAGE)
        action("Capture another view with icon") {
            copyScreenshotToTmp("screenshot_detail_with_icon.png")
        }
    }

    /**
     * Quick screenshot capture - just current state
     */
    @Test
    fun captureCurrentState() = uiTest("CurrentState") {
        delay(2000)

        screenshot("current_state", ScreenshotUtil.StorageLocation.EXTERNAL_STORAGE)

        action("Copy to tmp directory") {
            copyScreenshotToTmp("screenshot_current.png")
        }

        println("Current state screenshot saved to tmp/screenshot_current.png")
    }

    /**
     * Capture test detail view
     */
    @Test
    fun captureTestDetail() = uiTest("TestDetail") {
        delay(2000)

        // Navigate to detail view
        clickListItem(R.id.channelList, 0)
        delay(1000)
        clickListItem(R.id.contentList, 0)
        delay(1000)
        clickListItem(R.id.contentList, 0)
        delay(2000)

        screenshot("test_detail", ScreenshotUtil.StorageLocation.EXTERNAL_STORAGE)
        action("Save test detail screenshot") {
            copyScreenshotToTmp("screenshot_test_detail.png")
        }

        println("Test detail screenshot saved to tmp/screenshot_test_detail.png")
    }

    /**
     * Helper function to copy screenshots to tmp/ directory
     * This mimics the old adb workflow where screenshots were saved to tmp/
     */
    private fun copyScreenshotToTmp(filename: String) {
        try {
            val instrumentation = InstrumentationRegistry.getInstrumentation()

            // Use adb to pull the most recent screenshot and save to tmp/
            // We'll use shell commands to achieve this
            val externalDir = instrumentation.targetContext.getExternalFilesDir(null)
            val screenshotsDir = File(externalDir, "test-screenshots")

            if (screenshotsDir.exists()) {
                // Find the most recently created file
                val latestFile = screenshotsDir.listFiles()
                    ?.filter { it.extension == "png" }
                    ?.maxByOrNull { it.lastModified() }

                if (latestFile != null) {
                    // Copy to /sdcard/tmp for easy access
                    val tmpDir = File("/sdcard/tmp")
                    tmpDir.mkdirs()
                    val destFile = File(tmpDir, filename)

                    latestFile.copyTo(destFile, overwrite = true)
                    println("Copied screenshot to: ${destFile.absolutePath}")
                } else {
                    println("No screenshot file found to copy")
                }
            }
        } catch (e: Exception) {
            println("Error copying screenshot to tmp: ${e.message}")
            e.printStackTrace()
        }
    }
}
