package cut.the.crap.android.ui

import android.widget.AdapterView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import cut.the.crap.android.R
import cut.the.crap.android.utils.NavigationTestHelper
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Back Navigation Tests - Focus on RecyclerView State Changes
 *
 * Tests the correct back button behavior:
 * - Browse view structure: Channels (left, always visible) + RecyclerView (right, changes content)
 * - RecyclerView states: All Themes → Channel Themes → Titles
 * - Back button flow: Titles → Channel Themes → All Themes → Exit
 * - Detail view: Back returns to browse with previous RecyclerView state
 *
 * Run with:
 * ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=cut.the.crap.android.ui.BackNavigationTest
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BackNavigationTest {

    private lateinit var scenario: ActivityScenario<MediaActivity>
    private lateinit var navHelper: NavigationTestHelper

    // Store counts for verification
    private var allThemesCount = 0
    private var channelThemesCount = 0
    private var titlesCount = 0

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
     * Test 1: Verify initial state shows All Themes
     */
    @Test
    fun test01_initialStateShowsAllThemes() {
        navHelper.screenshot("back_01_initial")

        // Verify browse screen is shown
        navHelper.assertOnBrowseScreen()

        // Verify showing "All Themes" in right panel
        navHelper.assertShowingAllThemes()

        allThemesCount = navHelper.getRecyclerViewItemCount(R.id.contentList)
        assert(allThemesCount > 0) { "No themes loaded" }

        println("✓ Test 1 PASSED: Initial state shows All Themes ($allThemesCount items)")
    }

    /**
     * Test 2: Select channel → Should show Channel Themes
     */
    @Test
    fun test02_selectChannelShowsChannelThemes() {
        navHelper.delay(2000)
        navHelper.screenshot("back_02_before_channel")

        allThemesCount = navHelper.getRecyclerViewItemCount(R.id.contentList)

        // Click first channel
        clickListItem(R.id.channelList, 0)
        navHelper.delay(1500)
        navHelper.screenshot("back_02_after_channel")

        // Verify still on browse screen
        navHelper.assertOnBrowseScreen()

        // Verify showing Channel Themes (not "All Themes")
        navHelper.assertShowingChannelThemes()

        channelThemesCount = navHelper.getRecyclerViewItemCount(R.id.contentList)
        assert(channelThemesCount > 0) { "No channel themes loaded" }
        assert(channelThemesCount != allThemesCount) {
            "Channel themes count ($channelThemesCount) should differ from all themes ($allThemesCount)"
        }

        println("✓ Test 2 PASSED: Channel selection shows Channel Themes ($channelThemesCount items)")
    }

    /**
     * Test 3: Select theme → Should show Titles
     */
    @Test
    fun test03_selectThemeShowsTitles() {
        navHelper.delay(2000)

        // Navigate: Select channel → Select theme
        clickListItem(R.id.channelList, 0)
        navHelper.delay(1000)
        channelThemesCount = navHelper.getRecyclerViewItemCount(R.id.contentList)
        navHelper.screenshot("back_03_channel_themes")

        // Click first theme
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(1500)
        navHelper.screenshot("back_03_titles_shown")

        // Verify still on browse screen
        navHelper.assertOnBrowseScreen()

        // Verify showing titles
        titlesCount = navHelper.getRecyclerViewItemCount(R.id.contentList)
        assert(titlesCount > 0) { "No titles loaded" }
        assert(titlesCount != channelThemesCount) {
            "Titles count should differ from themes count"
        }

        println("✓ Test 3 PASSED: Theme selection shows Titles ($titlesCount items)")
    }

    /**
     * Test 4: From Titles → Detail → Back should return to Titles
     */
    @Test
    fun test04_detailBackReturnsToTitles() {
        navHelper.delay(2000)

        // Navigate to titles
        clickListItem(R.id.channelList, 0)
        navHelper.delay(1000)
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(1500)
        titlesCount = navHelper.getRecyclerViewItemCount(R.id.contentList)
        navHelper.screenshot("back_04_at_titles")

        // Click first title to open detail
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(2000)
        navHelper.screenshot("back_04_detail_shown")

        // Verify on detail screen
        navHelper.assertOnDetailScreen()

        // Press back
        navHelper.pressBack()
        navHelper.delay(1500)
        navHelper.screenshot("back_04_back_to_titles")

        // Verify back on browse screen
        navHelper.assertOnBrowseScreen()

        // Verify showing same titles
        val titlesCountAfterBack = navHelper.getRecyclerViewItemCount(R.id.contentList)
        assert(titlesCountAfterBack == titlesCount) {
            "Expected $titlesCount titles after back from detail, but found $titlesCountAfterBack"
        }

        println("✓ Test 4 PASSED: Back from Detail returned to Titles")
    }

    /**
     * Test 5: CRITICAL - From Titles → Back should show Channel Themes
     */
    @Test
    fun test05_backFromTitlesShowsChannelThemes() {
        navHelper.delay(2000)

        // Navigate to titles and record counts
        clickListItem(R.id.channelList, 0)
        navHelper.delay(1000)
        channelThemesCount = navHelper.getRecyclerViewItemCount(R.id.contentList)
        navHelper.screenshot("back_05_channel_themes")

        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(1500)
        titlesCount = navHelper.getRecyclerViewItemCount(R.id.contentList)
        navHelper.screenshot("back_05_at_titles")

        // Press back - should go to Channel Themes
        navHelper.pressBack()
        navHelper.delay(1500)
        navHelper.screenshot("back_05_back_to_channel_themes")

        // Verify on browse screen
        navHelper.assertOnBrowseScreen()

        // Verify showing Channel Themes
        navHelper.assertShowingChannelThemes()

        val countAfterBack = navHelper.getRecyclerViewItemCount(R.id.contentList)
        assert(countAfterBack == channelThemesCount) {
            "Expected $channelThemesCount channel themes, but found $countAfterBack"
        }

        println("✓ Test 5 PASSED: Back from Titles returned to Channel Themes")
    }

    /**
     * Test 6: CRITICAL - From Channel Themes → Back should show All Themes
     * THIS TEST WILL LIKELY FAIL - app exits instead of showing All Themes
     */
    @Test
    fun test06_backFromChannelThemesShowsAllThemes() {
        navHelper.delay(2000)
        navHelper.screenshot("back_06_initial")

        // Record All Themes count
        allThemesCount = navHelper.getRecyclerViewItemCount(R.id.contentList)

        // Select channel → Channel Themes
        clickListItem(R.id.channelList, 0)
        navHelper.delay(1500)
        channelThemesCount = navHelper.getRecyclerViewItemCount(R.id.contentList)
        navHelper.screenshot("back_06_at_channel_themes")

        // Verify showing Channel Themes
        navHelper.assertShowingChannelThemes()

        // Press back - should show All Themes again
        navHelper.pressBack()
        navHelper.delay(1500)
        navHelper.screenshot("back_06_after_back")

        // Verify app is still running
        assert(navHelper.isAppInForeground()) {
            "❌ FAILED: App exited when back pressed from Channel Themes - should show All Themes!"
        }

        // Verify on browse screen
        navHelper.assertOnBrowseScreen()

        // Verify showing All Themes
        navHelper.assertShowingAllThemes()

        val countAfterBack = navHelper.getRecyclerViewItemCount(R.id.contentList)
        assert(countAfterBack == allThemesCount) {
            "Expected $allThemesCount themes (All Themes), but found $countAfterBack"
        }

        println("✓ Test 6 PASSED: Back from Channel Themes returned to All Themes")
    }

    /**
     * Test 7: CRITICAL - From All Themes → Back should exit app
     */
    @Test
    fun test07_backFromAllThemesExitsApp() {
        navHelper.delay(2000)
        navHelper.screenshot("back_07_at_all_themes")

        // Verify showing All Themes
        navHelper.assertShowingAllThemes()

        // Press back - should exit app
        navHelper.pressBack()
        navHelper.delay(1500)

        // Verify app has exited
        val stillRunning = navHelper.isAppInForeground()
        assert(!stillRunning) {
            "App should have exited after back from All Themes"
        }

        println("✓ Test 7 PASSED: Back from All Themes exits app")
    }

    /**
     * Test 8: Complete back navigation cycle
     * Forward: All Themes → Channel Themes → Titles → Detail
     * Backward: Detail → Titles → Channel Themes → All Themes → Exit
     */
    @Test
    fun test08_completeBackNavigationCycle() {
        navHelper.delay(2000)
        navHelper.screenshot("back_08_01_start")

        // === Record initial state ===
        allThemesCount = navHelper.getRecyclerViewItemCount(R.id.contentList)
        navHelper.assertShowingAllThemes()

        // === FORWARD NAVIGATION ===

        // Step 1: All Themes → Channel Themes
        clickListItem(R.id.channelList, 0)
        navHelper.delay(1000)
        navHelper.screenshot("back_08_02_channel_themes")
        channelThemesCount = navHelper.getRecyclerViewItemCount(R.id.contentList)
        navHelper.assertShowingChannelThemes()

        // Step 2: Channel Themes → Titles
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(1000)
        navHelper.screenshot("back_08_03_titles")
        titlesCount = navHelper.getRecyclerViewItemCount(R.id.contentList)

        // Step 3: Titles → Detail
        clickRecyclerViewItem(R.id.contentList, 0)
        navHelper.delay(2000)
        navHelper.screenshot("back_08_04_detail")
        navHelper.assertOnDetailScreen()

        // === BACKWARD NAVIGATION ===

        // Back 1: Detail → Titles
        navHelper.pressBack()
        navHelper.delay(1000)
        navHelper.screenshot("back_08_05_back_to_titles")
        navHelper.assertOnBrowseScreen()
        assert(navHelper.getRecyclerViewItemCount(R.id.contentList) == titlesCount) {
            "Not showing correct titles after back from detail"
        }

        // Back 2: Titles → Channel Themes
        navHelper.pressBack()
        navHelper.delay(1000)
        navHelper.screenshot("back_08_06_back_to_channel_themes")
        navHelper.assertOnBrowseScreen()
        navHelper.assertShowingChannelThemes()
        assert(navHelper.getRecyclerViewItemCount(R.id.contentList) == channelThemesCount) {
            "Not showing correct channel themes after back"
        }

        // Back 3: Channel Themes → All Themes
        // THIS IS WHERE THE BUG LIKELY OCCURS
        navHelper.pressBack()
        navHelper.delay(1000)
        navHelper.screenshot("back_08_07_back_to_all_themes")

        assert(navHelper.isAppInForeground()) {
            "❌ FAILED: App exited after 3rd back press - should show All Themes!"
        }

        navHelper.assertOnBrowseScreen()
        navHelper.assertShowingAllThemes()
        assert(navHelper.getRecyclerViewItemCount(R.id.contentList) == allThemesCount) {
            "Not showing all themes after back"
        }

        // Back 4: All Themes → Exit
        navHelper.pressBack()
        navHelper.delay(1000)
        navHelper.screenshot("back_08_08_should_exit")

        val stillRunning = navHelper.isAppInForeground()
        assert(!stillRunning) {
            "App should have exited after 4th back press"
        }

        println("✓ Test 8 PASSED: Complete back navigation cycle works correctly")
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
