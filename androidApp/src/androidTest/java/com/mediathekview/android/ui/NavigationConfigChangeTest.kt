package com.mediathekview.android.ui

import android.widget.AdapterView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.mediathekview.android.R
import com.mediathekview.android.utils.NavigationTestHelper
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Navigation tests with configuration changes (screen rotation)
 *
 * Tests navigation flow with screen rotations to ensure:
 * - State is preserved across configuration changes
 * - Back navigation still works after rotation
 * - Selections are maintained
 * - Data remains consistent
 *
 * Run after basic NavigationTest passes with:
 * ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.mediathekview.android.ui.NavigationConfigChangeTest
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class NavigationConfigChangeTest {

    private lateinit var scenario: ActivityScenario<MediaActivity>
    private lateinit var navHelper: NavigationTestHelper
    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        navHelper = NavigationTestHelper()

        // Start in natural (portrait) orientation
        device.setOrientationNatural()
        navHelper.delay(500)

        scenario = ActivityScenario.launch(MediaActivity::class.java)
        navHelper.delay(3000)
    }

    @After
    fun tearDown() {
        // Reset to natural orientation
        device.setOrientationNatural()
        navHelper.delay(500)

        if (::scenario.isInitialized) {
            scenario.close()
        }
    }

    /**
     * Test 1: Rotate on browse screen - verify state preserved
     */
    @Test
    fun test01_rotateOnBrowseScreen() {
        navHelper.delay(2000)
        navHelper.screenshot("config_01_portrait")

        // Get initial state
        val channelCount = navHelper.getListItemCount(R.id.channelList)
        val initialContentCount = navHelper.getRecyclerViewItemCount(R.id.contentList)

        // Rotate to landscape
        device.setOrientationLeft()
        navHelper.delay(2000)
        navHelper.screenshot("config_01_landscape")

        // Verify still on browse screen
        navHelper.assertOnBrowseScreen()

        // Verify counts are same
        assert(navHelper.getListItemCount(R.id.channelList) == channelCount) {
            "Channel count changed after rotation"
        }

        // Rotate back to portrait
        device.setOrientationNatural()
        navHelper.delay(2000)
        navHelper.screenshot("config_01_portrait_again")

        navHelper.assertOnBrowseScreen()

        println("✓ Config Test 1 PASSED: Rotation on browse screen preserves state")
    }

    /**
     * Test 2: Rotate on detail screen - verify state preserved
     */
    @Test
    fun test02_rotateOnDetailScreen() {
        navHelper.delay(2000)

        // Navigate to detail
        clickListItem(R.id.channelList, 0)
        navHelper.delay(1000)
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(1000)
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(2000)
        navHelper.screenshot("config_02_detail_portrait")

        navHelper.assertOnDetailScreen()

        // Rotate to landscape
        device.setOrientationLeft()
        navHelper.delay(2000)
        navHelper.screenshot("config_02_detail_landscape")

        // Verify still on detail screen
        navHelper.assertOnDetailScreen()

        // Rotate back
        device.setOrientationNatural()
        navHelper.delay(2000)
        navHelper.screenshot("config_02_detail_portrait_again")

        navHelper.assertOnDetailScreen()

        println("✓ Config Test 2 PASSED: Rotation on detail screen preserves state")
    }

    /**
     * Test 3: Navigate after rotation
     * Rotate, then test navigation still works
     */
    @Test
    fun test03_navigationAfterRotation() {
        navHelper.delay(2000)

        // Rotate to landscape
        device.setOrientationLeft()
        navHelper.delay(2000)
        navHelper.screenshot("config_03_landscape_start")

        // Navigate: Channel → Theme → Title → Detail
        clickListItem(R.id.channelList, 0)
        navHelper.delay(1000)
        navHelper.screenshot("config_03_channel_selected")

        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(1000)
        navHelper.screenshot("config_03_theme_selected")

        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(2000)
        navHelper.screenshot("config_03_detail_shown")

        navHelper.assertOnDetailScreen()

        println("✓ Config Test 3 PASSED: Navigation works in landscape mode")
    }

    /**
     * Test 4: Back navigation after rotation
     * Navigate forward, rotate, then test back navigation
     */
    @Test
    fun test04_backNavigationAfterRotation() {
        navHelper.delay(2000)

        // Navigate to detail
        clickListItem(R.id.channelList, 0)
        navHelper.delay(1000)
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(1000)
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(2000)
        navHelper.screenshot("config_04_detail_portrait")

        // Rotate
        device.setOrientationLeft()
        navHelper.delay(2000)
        navHelper.screenshot("config_04_detail_landscape")

        // Press back - should go to titles
        navHelper.pressBack()
        navHelper.delay(1000)
        navHelper.screenshot("config_04_back_to_titles")

        navHelper.assertOnBrowseScreen()

        // Rotate back to portrait
        device.setOrientationNatural()
        navHelper.delay(1500)

        // Press back again - should go to themes
        navHelper.pressBack()
        navHelper.delay(1000)
        navHelper.screenshot("config_04_back_to_themes")

        navHelper.assertOnBrowseScreen()

        println("✓ Config Test 4 PASSED: Back navigation works after rotation")
    }

    /**
     * Test 5: Rotate multiple times during navigation
     * Stress test: rotate at each navigation step
     */
    @Test
    fun test05_rotateAtEachNavigationStep() {
        navHelper.delay(2000)
        navHelper.screenshot("config_05_01_start")

        // Step 1: Portrait → Select channel → Landscape
        clickListItem(R.id.channelList, 0)
        navHelper.delay(1000)
        navHelper.screenshot("config_05_02_channel_selected")

        device.setOrientationLeft()
        navHelper.delay(2000)
        navHelper.screenshot("config_05_03_landscape_1")
        navHelper.assertOnBrowseScreen()

        // Step 2: Landscape → Select theme → Portrait
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(1000)
        navHelper.screenshot("config_05_04_theme_selected")

        device.setOrientationNatural()
        navHelper.delay(2000)
        navHelper.screenshot("config_05_05_portrait_2")
        navHelper.assertOnBrowseScreen()

        // Step 3: Portrait → Select title → Landscape
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(2000)
        navHelper.screenshot("config_05_06_detail_portrait")

        device.setOrientationLeft()
        navHelper.delay(2000)
        navHelper.screenshot("config_05_07_detail_landscape")
        navHelper.assertOnDetailScreen()

        // Step 4: Back navigation with rotation
        navHelper.pressBack()
        navHelper.delay(1000)
        navHelper.screenshot("config_05_08_back_1")

        device.setOrientationNatural()
        navHelper.delay(1500)

        navHelper.pressBack()
        navHelper.delay(1000)
        navHelper.screenshot("config_05_09_back_2")

        navHelper.assertOnBrowseScreen()

        println("✓ Config Test 5 PASSED: Multiple rotations during navigation handled correctly")
    }

    /**
     * Test 6: Selection preservation across rotations
     */
    @Test
    fun test06_selectionPreservedAcrossRotation() {
        navHelper.delay(2000)

        // Select second channel
        clickListItem(R.id.channelList, 1)
        navHelper.delay(1000)
        navHelper.screenshot("config_06_channel_1_selected")

        val themeCountBefore = navHelper.getRecyclerViewItemCount(R.id.contentList)

        // Rotate
        device.setOrientationLeft()
        navHelper.delay(2000)
        navHelper.screenshot("config_06_after_rotation")

        // Verify same data still loaded
        val themeCountAfter = navHelper.getRecyclerViewItemCount(R.id.contentList)
        assert(themeCountBefore == themeCountAfter) {
            "Theme count changed after rotation: was $themeCountBefore, now $themeCountAfter"
        }

        // Verify still on browse screen with data
        navHelper.assertOnBrowseScreen()

        println("✓ Config Test 6 PASSED: Selection preserved across rotation")
    }

    /**
     * Test 7: Complete cycle with rotations
     * Full navigation cycle with rotation at each back step
     */
    @Test
    fun test07_completeCycleWithRotations() {
        navHelper.delay(2000)

        // Forward navigation in portrait
        clickListItem(R.id.channelList, 0)
        navHelper.delay(1000)
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(1000)
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(2000)
        navHelper.screenshot("config_07_01_at_detail")

        navHelper.assertOnDetailScreen()

        // Rotate before first back
        device.setOrientationLeft()
        navHelper.delay(2000)

        // Back 1: Detail → Titles
        navHelper.pressBack()
        navHelper.delay(1000)
        navHelper.screenshot("config_07_02_back_to_titles")
        navHelper.assertOnBrowseScreen()

        // Rotate before second back
        device.setOrientationNatural()
        navHelper.delay(1500)

        // Back 2: Titles → Themes
        navHelper.pressBack()
        navHelper.delay(1000)
        navHelper.screenshot("config_07_03_back_to_themes")
        navHelper.assertOnBrowseScreen()

        // Rotate before third back
        device.setOrientationLeft()
        navHelper.delay(1500)

        // Back 3: Themes → Channels (will likely fail with current implementation)
        navHelper.pressBack()
        navHelper.delay(1000)
        navHelper.screenshot("config_07_04_after_third_back")

        assert(navHelper.isAppInForeground()) {
            "App exited too early during rotated back navigation"
        }

        println("✓ Config Test 7 PASSED: Complete navigation cycle with rotations works")
    }

    // ===========================================================================================
    // Helper Methods
    // ===========================================================================================

    private fun clickListItem(listResourceId: Int, index: Int) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            scenario.onActivity { activity ->
                val listView = activity.findViewById<AdapterView<*>>(listResourceId)
                listView?.let {
                    if (index < it.count) {
                        it.performItemClick(
                            it.getChildAt(index),
                            index,
                            it.getItemIdAtPosition(index)
                        )
                    }
                }
            }
        }
    }

    private fun clickRecyclerViewItem(recyclerViewId: Int, index: Int) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            scenario.onActivity { activity ->
                val recyclerView = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(recyclerViewId)
                recyclerView?.let {
                    val viewHolder = it.findViewHolderForAdapterPosition(index)
                    viewHolder?.itemView?.performClick()
                }
            }
        }
    }
}
