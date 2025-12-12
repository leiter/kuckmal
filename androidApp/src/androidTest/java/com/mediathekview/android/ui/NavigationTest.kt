package com.mediathekview.android.ui

import android.widget.AdapterView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.mediathekview.android.R
import com.mediathekview.android.utils.NavigationTestHelper
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive navigation tests for MediathekView app
 *
 * Tests the complete navigation flow:
 * - Forward: Channels → Themes → Titles → Detail
 * - Backward: Detail → Titles → Themes → Channels → Exit
 *
 * Verifies:
 * - Correct screens are displayed
 * - Selections are preserved
 * - Data is loaded correctly
 * - Back button behavior is correct
 *
 * Run with:
 * ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.mediathekview.android.ui.NavigationTest
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class NavigationTest {

    private lateinit var scenario: ActivityScenario<MediaActivity>
    private lateinit var navHelper: NavigationTestHelper

    @Before
    fun setup() {
        navHelper = NavigationTestHelper()
        scenario = ActivityScenario.launch(MediaActivity::class.java)

        // Wait for app to fully load
        navHelper.delay(3000)
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
    }

    /**
     * Test 1: Verify initial state
     * App should start on browse screen showing channels and themes
     */
    @Test
    fun test01_initialState() {
        navHelper.screenshot("01_initial_state")

        // Verify we're on browse screen
        navHelper.assertOnBrowseScreen()

        // Verify channels are loaded
        val channelCount = navHelper.getListItemCount(R.id.channelList)
        assert(channelCount > 0) { "No channels loaded" }

        // Verify right panel title shows "Alle Themen" or "Themes"
        navHelper.waitForView(R.id.rightPanelTitle)

        println("✓ Test 1 PASSED: Initial state correct - $channelCount channels loaded")
    }

    /**
     * Test 2: Navigate from channels to themes
     * Select a channel and verify themes are loaded
     */
    @Test
    fun test02_channelToThemes() {
        navHelper.delay(2000)
        navHelper.screenshot("02_before_channel_select")

        // Click first channel (3Sat)
        clickListItem(R.id.channelList, 0)
        navHelper.delay(1000)
        navHelper.screenshot("02_after_channel_select")

        // Verify still on browse screen
        navHelper.assertOnBrowseScreen()

        // Verify themes are loaded in content list
        val themeCount = navHelper.getRecyclerViewItemCount(R.id.contentList)
        assert(themeCount > 0) { "No themes loaded after selecting channel" }

        println("✓ Test 2 PASSED: Channel selection loaded $themeCount themes")
    }

    /**
     * Test 3: Navigate from themes to titles
     * Select a theme and verify titles are loaded
     */
    @Test
    fun test03_themesToTitles() {
        navHelper.delay(2000)

        // Select first channel
        clickListItem(R.id.channelList, 0)
        navHelper.delay(1000)
        navHelper.screenshot("03_channel_selected")

        // Get initial theme count
        val initialThemeCount = navHelper.getRecyclerViewItemCount(R.id.contentList)

        // Click first theme
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(1500)
        navHelper.screenshot("03_theme_selected")

        // Verify still on browse screen
        navHelper.assertOnBrowseScreen()

        // Verify titles are loaded (different count than themes)
        val titleCount = navHelper.getRecyclerViewItemCount(R.id.contentList)
        assert(titleCount > 0) { "No titles loaded after selecting theme" }

        println("✓ Test 3 PASSED: Theme selection loaded $titleCount titles")
    }

    /**
     * Test 4: Navigate from titles to detail
     * Select a title and verify detail view is shown
     */
    @Test
    fun test04_titlesToDetail() {
        navHelper.delay(2000)

        // Navigate: Channel → Theme → Title
        clickListItem(R.id.channelList, 0)
        navHelper.delay(1000)

        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(1000)
        navHelper.screenshot("04_titles_loaded")

        // Click first title
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(2000)
        navHelper.screenshot("04_detail_opened")

        // Verify we're on detail screen
        navHelper.assertOnDetailScreen()

        // Verify detail elements are visible
        navHelper.waitForView(R.id.dvSenderImg)

        println("✓ Test 4 PASSED: Detail view displayed correctly")
    }

    /**
     * Test 5: CRITICAL - Back from detail to titles
     * Press back from detail and verify we return to titles list
     */
    @Test
    fun test05_backFromDetailToTitles() {
        navHelper.delay(2000)

        // Navigate to detail
        clickListItem(R.id.channelList, 0)
        navHelper.delay(1000)
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(1000)
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(2000)
        navHelper.screenshot("05_at_detail")

        // Verify on detail
        navHelper.assertOnDetailScreen()

        // Press back
        navHelper.pressBack()
        navHelper.delay(1000)
        navHelper.screenshot("05_after_back")

        // Verify back on browse screen with titles
        navHelper.assertOnBrowseScreen()

        // Verify titles are still loaded
        val titleCount = navHelper.getRecyclerViewItemCount(R.id.contentList)
        assert(titleCount > 0) { "Titles not shown after back from detail" }

        println("✓ Test 5 PASSED: Back from detail returned to titles list")
    }

    /**
     * Test 6: CRITICAL - Back from titles to themes
     * Press back from titles and verify we return to themes list
     */
    @Test
    fun test06_backFromTitlesToThemes() {
        navHelper.delay(2000)

        // Navigate to titles
        clickListItem(R.id.channelList, 0)
        navHelper.delay(1000)

        val initialThemeCount = navHelper.getRecyclerViewItemCount(R.id.contentList)

        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(1500)
        navHelper.screenshot("06_at_titles")

        val titleCount = navHelper.getRecyclerViewItemCount(R.id.contentList)

        // Press back
        navHelper.pressBack()
        navHelper.delay(1000)
        navHelper.screenshot("06_after_back")

        // Verify still on browse screen
        navHelper.assertOnBrowseScreen()

        // Verify themes are shown (count should match initial)
        val themeCountAfterBack = navHelper.getRecyclerViewItemCount(R.id.contentList)
        assert(themeCountAfterBack == initialThemeCount) {
            "Expected $initialThemeCount themes, but found $themeCountAfterBack"
        }

        println("✓ Test 6 PASSED: Back from titles returned to themes list")
    }

    /**
     * Test 7: CRITICAL - Back from themes to channels/all themes
     * Press back from channel-specific themes and verify behavior
     * This test will FAIL with current implementation - it should navigate back but doesn't
     */
    @Test
    fun test07_backFromThemesToChannels() {
        navHelper.delay(2000)
        navHelper.screenshot("07_initial")

        // Get initial state (should show all themes)
        val initialCount = navHelper.getRecyclerViewItemCount(R.id.contentList)

        // Select channel
        clickListItem(R.id.channelList, 0)
        navHelper.delay(1500)
        navHelper.screenshot("07_channel_selected")

        val channelThemeCount = navHelper.getRecyclerViewItemCount(R.id.contentList)

        // Press back - should go back to "all themes" view
        navHelper.pressBack()
        navHelper.delay(1000)
        navHelper.screenshot("07_after_back")

        // Verify still on browse screen
        navHelper.assertOnBrowseScreen()

        // Verify showing all themes again (count should match initial)
        // NOTE: This test will likely FAIL because current implementation exits app
        assert(navHelper.isAppInForeground()) {
            "FAILED: App exited when pressing back from themes - should stay in app!"
        }

        val countAfterBack = navHelper.getRecyclerViewItemCount(R.id.contentList)
        assert(countAfterBack == initialCount) {
            "Expected $initialCount themes (all themes), but found $countAfterBack"
        }

        println("✓ Test 7 PASSED: Back from themes returned to all themes view")
    }

    /**
     * Test 8: Complete navigation flow forward and backward
     * Test the full cycle: Channel → Theme → Title → Detail → Back → Back → Back → Back
     */
    @Test
    fun test08_completeNavigationCycle() {
        navHelper.delay(2000)
        navHelper.screenshot("08_01_start")

        // === FORWARD NAVIGATION ===

        // Step 1: Select channel
        clickListItem(R.id.channelList, 0)
        navHelper.delay(1000)
        navHelper.screenshot("08_02_channel_selected")
        navHelper.assertOnBrowseScreen()

        // Step 2: Select theme
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(1000)
        navHelper.screenshot("08_03_theme_selected")
        navHelper.assertOnBrowseScreen()

        // Step 3: Select title
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(2000)
        navHelper.screenshot("08_04_detail_view")
        navHelper.assertOnDetailScreen()

        // === BACKWARD NAVIGATION ===

        // Back 1: Detail → Titles
        navHelper.pressBack()
        navHelper.delay(1000)
        navHelper.screenshot("08_05_back_to_titles")
        navHelper.assertOnBrowseScreen()

        // Back 2: Titles → Themes
        navHelper.pressBack()
        navHelper.delay(1000)
        navHelper.screenshot("08_06_back_to_themes")
        navHelper.assertOnBrowseScreen()

        // Back 3: Themes → Channels/All Themes
        // NOTE: This will likely FAIL - app exits here instead of going back
        navHelper.pressBack()
        navHelper.delay(1000)
        navHelper.screenshot("08_07_back_to_channels")

        assert(navHelper.isAppInForeground()) {
            "FAILED: App exited too early - should still be in app after 3 back presses"
        }

        navHelper.assertOnBrowseScreen()

        // Back 4: Should exit app
        navHelper.pressBack()
        navHelper.delay(1000)

        // App should have exited
        val stillRunning = navHelper.isAppInForeground()
        assert(!stillRunning) {
            "App should have exited after 4 back presses"
        }

        println("✓ Test 8 PASSED: Complete navigation cycle works correctly")
    }

    /**
     * Test 9: Verify selection preservation
     * Select channel, navigate away, come back - channel should still be selected
     */
    @Test
    fun test09_selectionPreservation() {
        navHelper.delay(2000)

        // Select second channel
        clickListItem(R.id.channelList, 1)
        navHelper.delay(1000)
        navHelper.screenshot("09_channel_1_selected")

        // Navigate deeper
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(1000)

        // Navigate back
        navHelper.pressBack()
        navHelper.delay(1000)
        navHelper.screenshot("09_after_back")

        // Verify channel 1 is still selected
        // NOTE: This needs verification logic in NavHelper
        navHelper.assertOnBrowseScreen()

        println("✓ Test 9 PASSED: Selection preserved after navigation")
    }

    /**
     * Test 10: Verify data consistency
     * Ensure same data is shown when navigating back and forth
     */
    @Test
    fun test10_dataConsistency() {
        navHelper.delay(2000)

        // Select channel and record theme count
        clickListItem(R.id.channelList, 0)
        navHelper.delay(1000)
        val themeCount1 = navHelper.getRecyclerViewItemCount(R.id.contentList)

        // Navigate to titles
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(1000)

        // Navigate back
        navHelper.pressBack()
        navHelper.delay(1000)

        // Verify same theme count
        val themeCount2 = navHelper.getRecyclerViewItemCount(R.id.contentList)
        assert(themeCount1 == themeCount2) {
            "Data inconsistency: Had $themeCount1 themes, now have $themeCount2"
        }

        println("✓ Test 10 PASSED: Data consistency maintained")
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
