package cut.the.crap.android.utils

import android.view.KeyEvent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import cut.the.crap.android.R
import org.hamcrest.Matchers.not

/**
 * Helper class for testing navigation flows in MediathekView app
 *
 * Provides utilities to:
 * - Navigate through the app screens
 * - Verify current screen state
 * - Check selection preservation
 * - Simulate back button presses
 */
class NavigationTestHelper {

    private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    /**
     * Right panel (RecyclerView) content states
     * The left panel (channels) is always visible
     * Only the right panel content changes
     */
    enum class RightPanelState {
        ALL_THEMES,       // Showing all themes from all channels
        CHANNEL_THEMES,   // Showing themes from selected channel only
        TITLES,           // Showing titles for selected theme
        UNKNOWN           // Cannot determine state
    }

    /**
     * App view states
     */
    enum class ExpectedScreen {
        BROWSE,           // Browse view showing channels (left) + content (right)
        DETAIL            // Media detail view overlay
    }

    /**
     * Simulate pressing the back button
     */
    fun pressBack() {
        device.pressBack()
        Thread.sleep(500) // Allow UI to settle
    }

    /**
     * Press back button using KeyEvent (alternative method)
     */
    fun pressBackKey() {
        device.pressKeyCode(KeyEvent.KEYCODE_BACK)
        Thread.sleep(500)
    }

    /**
     * Verify that we're on the browse screen (channels + themes panels)
     */
    fun assertOnBrowseScreen() {
        // Browse view should show both channel list and content list (RecyclerView)
        onView(withId(R.id.channelList))
            .check(matches(isDisplayed()))

        onView(withId(R.id.contentList))
            .check(matches(isDisplayed()))

        onView(withId(R.id.rightPanelTitle))
            .check(matches(isDisplayed()))
    }

    /**
     * Verify that we're on the detail screen
     */
    fun assertOnDetailScreen() {
        // Detail view should NOT show the browse view elements
        onView(withId(R.id.channelList))
            .check(matches(not(isDisplayed())))

        // Detail view elements should be visible
        onView(withId(R.id.detail_scroll_portrait))
            .check(matches(isDisplayed()))

        onView(withId(R.id.dvSenderImg))
            .check(matches(isDisplayed()))
    }

    /**
     * Verify the right panel title matches expected text
     */
    fun assertRightPanelTitle(expectedText: String) {
        onView(withId(R.id.rightPanelTitle))
            .check(matches(withText(expectedText)))
    }

    /**
     * Get the current right panel title text
     */
    fun getRightPanelTitle(): String {
        var title = ""
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = getCurrentActivity()
            val titleView = activity?.findViewById<android.widget.TextView>(R.id.rightPanelTitle)
            title = titleView?.text?.toString() ?: ""
        }
        return title
    }

    /**
     * Determine which state the right panel is in based on title and content
     */
    fun getRightPanelState(): RightPanelState {
        val title = getRightPanelTitle()
        val itemCount = getRecyclerViewItemCount(R.id.contentList)

        // Heuristic:
        // - "Alle Themen" or "All Themes" = ALL_THEMES
        // - Title contains channel name or has moderate item count = CHANNEL_THEMES
        // - Lower item count after selecting theme = TITLES

        return when {
            title.contains("Alle", ignoreCase = true) ||
            title.contains("All", ignoreCase = true) -> RightPanelState.ALL_THEMES

            // If we just selected a theme, we're showing titles
            // This needs to be determined by context in tests
            else -> RightPanelState.UNKNOWN
        }
    }

    /**
     * Verify right panel is showing all themes
     */
    fun assertShowingAllThemes() {
        val title = getRightPanelTitle()
        assert(title.contains("Alle", ignoreCase = true) ||
               title.contains("All", ignoreCase = true)) {
            "Expected right panel to show 'All Themes', but title is: $title"
        }
    }

    /**
     * Verify right panel is showing channel-specific themes
     * (by checking it's not "All Themes" and has content)
     */
    fun assertShowingChannelThemes() {
        val title = getRightPanelTitle()
        val itemCount = getRecyclerViewItemCount(R.id.contentList)

        assert(!title.contains("Alle", ignoreCase = true) &&
               !title.contains("All", ignoreCase = true)) {
            "Expected channel themes, but showing: $title"
        }

        assert(itemCount > 0) {
            "Expected channel themes to be loaded, but found 0 items"
        }
    }

    /**
     * Verify right panel is showing titles
     * (by checking content and context)
     */
    fun assertShowingTitles(expectedCount: Int? = null) {
        val itemCount = getRecyclerViewItemCount(R.id.contentList)

        assert(itemCount > 0) {
            "Expected titles to be loaded, but found 0 items"
        }

        if (expectedCount != null) {
            assert(itemCount == expectedCount) {
                "Expected $expectedCount titles, but found $itemCount"
            }
        }
    }

    /**
     * Verify specific text is displayed on screen
     */
    fun assertTextDisplayed(text: String) {
        val element = device.findObject(UiSelector().textContains(text))
        if (!element.exists()) {
            throw AssertionError("Text '$text' not found on screen")
        }
    }

    /**
     * Wait for text to appear on screen
     */
    fun waitForText(text: String, timeoutMs: Long = 5000): Boolean {
        return device.wait(Until.hasObject(By.textContains(text)), timeoutMs)
    }

    /**
     * Wait for view with ID to be displayed
     */
    fun waitForView(resourceId: Int, timeoutMs: Long = 5000) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                onView(withId(resourceId)).check(matches(isDisplayed()))
                return
            } catch (e: Exception) {
                Thread.sleep(100)
            }
        }
        throw AssertionError("View with ID $resourceId not displayed after ${timeoutMs}ms")
    }

    /**
     * Get the number of items in a list view
     */
    fun getListItemCount(listResourceId: Int): Int {
        var count = 0
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = getCurrentActivity()
            val listView = activity?.findViewById<android.widget.AdapterView<*>>(listResourceId)
            count = listView?.count ?: 0
        }
        return count
    }

    /**
     * Get the number of items in a RecyclerView
     */
    fun getRecyclerViewItemCount(recyclerViewId: Int): Int {
        var count = 0
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = getCurrentActivity()
            val recyclerView = activity?.findViewById<androidx.recyclerview.widget.RecyclerView>(recyclerViewId)
            count = recyclerView?.adapter?.itemCount ?: 0
        }
        return count
    }

    /**
     * Get currently selected position in a list view
     */
    fun getSelectedPosition(listResourceId: Int): Int {
        var position = -1
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = getCurrentActivity()
            val listView = activity?.findViewById<android.widget.ListView>(listResourceId)
            position = listView?.selectedItemPosition ?: -1
        }
        return position
    }

    /**
     * Verify list has expected number of items
     */
    fun assertListItemCount(listResourceId: Int, expectedCount: Int) {
        val actualCount = getListItemCount(listResourceId)
        if (actualCount != expectedCount) {
            throw AssertionError("Expected $expectedCount items in list, but found $actualCount")
        }
    }

    /**
     * Verify RecyclerView has expected number of items
     */
    fun assertRecyclerViewItemCount(recyclerViewId: Int, expectedCount: Int) {
        val actualCount = getRecyclerViewItemCount(recyclerViewId)
        if (actualCount != expectedCount) {
            throw AssertionError("Expected $expectedCount items in RecyclerView, but found $actualCount")
        }
    }

    /**
     * Verify a specific item is selected in a list
     */
    fun assertItemSelected(listResourceId: Int, expectedPosition: Int) {
        val actualPosition = getSelectedPosition(listResourceId)
        if (actualPosition != expectedPosition) {
            throw AssertionError("Expected item at position $expectedPosition to be selected, but position $actualPosition is selected")
        }
    }

    /**
     * Delay execution
     */
    fun delay(millis: Long) {
        Thread.sleep(millis)
    }

    /**
     * Get current activity
     */
    private fun getCurrentActivity(): android.app.Activity? {
        val activities = mutableListOf<android.app.Activity>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val resumedActivities = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
                .getInstance()
                .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
            activities.addAll(resumedActivities)
        }
        return activities.firstOrNull()
    }

    /**
     * Check if app has exited (no activity in foreground)
     */
    fun isAppInForeground(): Boolean {
        return getCurrentActivity() != null
    }

    /**
     * Take screenshot for debugging
     */
    fun screenshot(name: String) {
        ScreenshotUtil.capture(name, ScreenshotUtil.StorageLocation.EXTERNAL_STORAGE)
    }
}
