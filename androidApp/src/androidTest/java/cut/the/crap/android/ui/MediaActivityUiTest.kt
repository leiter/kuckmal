package cut.the.crap.android.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import cut.the.crap.android.R
import cut.the.crap.android.utils.ScreenshotUtil
import cut.the.crap.android.utils.uiTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for MediaActivity using the DSL framework
 *
 * These tests demonstrate the custom UI test DSL with:
 * - Click actions
 * - Wait delays
 * - Screenshot capture
 * - Assertions
 *
 * Run these tests with:
 * ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MediaActivityUiTest {

    private lateinit var scenario: ActivityScenario<MediaActivity>

    @Before
    fun setup() {
        // Launch the main activity
        scenario = ActivityScenario.launch(MediaActivity::class.java)
    }

    @Test
    fun testBrowseChannelsFlow() = uiTest("BrowseChannels") {
        // Delay for initial load
        delay(2000)

        // Capture initial state
        screenshot("01_initial_browse_view", ScreenshotUtil.StorageLocation.BOTH)

        // Verify initial text is displayed
        assertTextDisplayed("Channels")

        // Click on first channel (3Sat)
        action("Select first channel in list") {
            clickListItem(R.id.channelList, 0)
        }

        delay(1000)
        screenshot("02_first_channel_selected")

        // Click on a theme item
        action("Select first theme") {
            clickByText("Grad", exactMatch = false)
        }

        delay(1000)
        screenshot("03_theme_selected")

        // Verify theme list is displayed
        assertTextDisplayed("3sat")
    }

    @Test
    fun testChannelNavigation() = uiTest("ChannelNavigation") {
        delay(1500)
        screenshot("01_start")

        // Navigate through multiple channels
        val channels = listOf(0, 1, 2, 3, 4)

        channels.forEach { index ->
            action("Select channel at index $index") {
                clickListItem(R.id.channelList, index)
            }
            delay(500)
            screenshot("02_channel_$index")
        }

        // Return to first channel
        clickListItem(R.id.channelList, 0)
        delay(500)
        screenshot("03_back_to_first")
    }

    @Test
    fun testMenuButtonInteraction() = uiTest("MenuButton") {
        delay(1500)
        screenshot("01_before_menu")

        // Click the overflow menu button
        action("Open overflow menu") {
            clickById(R.id.menuButton)
        }

        delay(500)
        screenshot("02_menu_opened")

        // Press back to close menu
        pressBack()
        delay(300)
        screenshot("03_menu_closed")
    }

    @Test
    fun testLandscapeOrientation() = uiTest("LandscapeMode") {
        delay(1500)

        // Capture portrait
        screenshot("01_portrait_mode")

        // Rotate to landscape (using UiDevice)
        action("Rotate to landscape") {
            // This would require UiDevice rotation
            // For now, just demonstrate the flow
        }

        delay(1000)
        screenshot("02_landscape_mode")
    }

    @Test
    fun testThemeSelection() = uiTest("ThemeSelection") {
        delay(2000)
        screenshot("01_initial")

        // Select a channel first
        clickListItem(R.id.channelList, 0)
        delay(1000)
        screenshot("02_channel_selected")

        // Try to find and click specific themes
        val themes = listOf("3sat", "Grad", "Leben")

        themes.forEachIndexed { index, theme ->
            try {
                action("Click theme: $theme") {
                    clickByText(theme, exactMatch = false)
                }
                delay(800)
                screenshot("03_theme_${index}_$theme")
            } catch (e: Exception) {
                println("Theme '$theme' not found or not clickable: ${e.message}")
            }
        }
    }

    @Test
    fun testScrollBehavior() = uiTest("ScrollBehavior") {
        delay(1500)
        screenshot("01_top_of_list")

        // Select first channel to load content
        clickListItem(R.id.channelList, 0)
        delay(1000)

        // Perform swipe gestures to scroll
        action("Scroll down in content list") {
            // Get screen dimensions for swipe
            val displayHeight = 1920 // Adjust based on device
            val displayWidth = 1080

            // Swipe up to scroll down
            swipe(
                startX = displayWidth / 2,
                startY = displayHeight * 2 / 3,
                endX = displayWidth / 2,
                endY = displayHeight / 3,
                steps = 20
            )
        }

        delay(500)
        screenshot("02_scrolled_down")

        // Scroll back up
        action("Scroll back up") {
            val displayHeight = 1920
            val displayWidth = 1080

            swipe(
                startX = displayWidth / 2,
                startY = displayHeight / 3,
                endX = displayWidth / 2,
                endY = displayHeight * 2 / 3,
                steps = 20
            )
        }

        delay(500)
        screenshot("03_scrolled_back_up")
    }

    @Test
    fun testRapidChannelSwitching() = uiTest("RapidSwitching") {
        delay(1500)
        screenshot("01_start")

        // Rapidly switch between channels
        repeat(10) { iteration ->
            val index = iteration % 5 // Cycle through first 5 channels
            clickListItem(R.id.channelList, index)
            delay(200)

            // Only take screenshots every other iteration to reduce overhead
            if (iteration % 2 == 0) {
                screenshot("02_iteration_$iteration")
            }
        }

        screenshot("03_final_state")
    }

    @Test
    fun testAssertions() = uiTest("AssertionTests") {
        delay(1500)

        // Test positive assertions
        assertTextDisplayed("Channels")
        assertTextDisplayed("Themes")

        // Test that certain text is not displayed initially
        try {
            assertTextNotDisplayed("NonExistentText12345")
            println("Assertion passed: Text not found as expected")
        } catch (e: AssertionError) {
            println("Unexpected: Text was found when it shouldn't be")
            throw e
        }

        screenshot("assertions_complete")
    }

    @Test
    fun testWaitUntilCondition() = uiTest("WaitUntilTest") {
        screenshot("01_before_wait")

        // Wait until a condition is met (with timeout)
        action("Wait for activity to be ready") {
            waitUntil(timeoutMs = 5000) {
                // Custom condition: check if text exists
                try {
                    assertTextDisplayed("Channels")
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }

        screenshot("02_after_wait_condition_met")
    }

    @Test
    fun testCompleteUserFlow() = uiTest("CompleteFlow") {
        // Simulate a complete user journey
        delay(2000)
        screenshot("01_app_launched")

        // Step 1: Browse channels
        action("Browse channels") {
            assertTextDisplayed("Channels")
        }
        screenshot("02_viewing_channels")

        // Step 2: Select a channel
        action("Select ARD channel") {
            clickListItem(R.id.channelList, 1) // Assuming ARD is at index 1
        }
        delay(1000)
        screenshot("03_ard_selected")

        // Step 3: Browse themes
        action("View themes for ARD") {
            assertTextDisplayed("Themes")
        }
        screenshot("04_viewing_themes")

        // Step 4: Open menu
        action("Open options menu") {
            clickById(R.id.menuButton)
        }
        delay(500)
        screenshot("05_menu_opened")

        // Step 5: Close menu
        pressBack()
        delay(300)
        screenshot("06_menu_closed")

        // Step 6: Switch to different channel
        action("Switch to another channel") {
            clickListItem(R.id.channelList, 3)
        }
        delay(1000)
        screenshot("07_different_channel")

        // Final state
        screenshot("08_flow_complete")
    }
}
