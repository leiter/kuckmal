package com.mediathekview.android.utils

import android.view.View
import android.widget.AdapterView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.hamcrest.Matcher

/**
 * DSL for creating UI test scenarios with clicks, waits, and screenshots
 *
 * Example usage:
 * ```
 * uiTest("Browse channels test") {
 *     delay(1000)
 *     screenshot("initial_state")
 *     clickListItem(R.id.channelList, 0)
 *     delay(500)
 *     screenshot("channel_selected")
 *     clickByText("3Sat")
 *     screenshot("theme_list")
 * }
 * ```
 */

/**
 * Main entry point for UI test DSL
 */
fun uiTest(testName: String, block: UiTestScope.() -> Unit) {
    val scope = UiTestScope(testName)
    scope.block()
}

/**
 * Scope class that provides DSL methods for UI testing
 */
class UiTestScope(private val testName: String) {

    private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private var stepCounter = 0

    /**
     * Delay for specified milliseconds
     */
    fun delay(millis: Long) {
        Thread.sleep(millis)
        println("[$testName] Delayed ${millis}ms")
    }

    /**
     * Wait for specified milliseconds (alias for delay)
     */
    @Deprecated("Use delay() instead to avoid conflicts with Object.wait()", ReplaceWith("delay(millis)"))
    fun waitFor(millis: Long) = delay(millis)

    /**
     * Wait until a condition is true or timeout
     */
    fun waitUntil(timeoutMs: Long = 5000, condition: () -> Boolean) {
        val startTime = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw AssertionError("Timeout waiting for condition after ${timeoutMs}ms")
            }
            Thread.sleep(100)
        }
        println("[$testName] Condition met")
    }

    /**
     * Capture a screenshot with the given name
     */
    fun screenshot(
        name: String,
        location: ScreenshotUtil.StorageLocation = ScreenshotUtil.StorageLocation.BUILD_OUTPUT
    ) {
        stepCounter++
        val fileName = "${testName}_${stepCounter}_${name}"
        val files = ScreenshotUtil.capture(fileName, location)
        println("[$testName] Screenshot: $name -> ${files.map { it.absolutePath }}")
    }

    /**
     * Click on a view by resource ID
     */
    fun clickById(resourceId: Int) {
        try {
            onView(withId(resourceId)).perform(scrollTo(), click())
            println("[$testName] Clicked view with ID: $resourceId")
        } catch (e: Exception) {
            println("[$testName] Failed to click view with ID $resourceId: ${e.message}")
            throw e
        }
    }

    /**
     * Click on a list item by resource ID and index
     */
    fun clickListItem(listResourceId: Int, index: Int) {
        try {
            // Get the activity and find the list view
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.runOnMainSync {
                val activity = getCurrentActivity()
                val listView = activity?.findViewById<AdapterView<*>>(listResourceId)

                if (listView != null && index < listView.count) {
                    listView.performItemClick(
                        listView.getChildAt(index),
                        index,
                        listView.getItemIdAtPosition(index)
                    )
                    println("[$testName] Clicked list item at index $index")
                } else {
                    throw IllegalStateException("List not found or index $index out of bounds")
                }
            }
        } catch (e: Exception) {
            println("[$testName] Failed to click list item at index $index: ${e.message}")
            throw e
        }
    }

    /**
     * Click using UiAutomator by text
     */
    fun clickByText(text: String, exactMatch: Boolean = false) {
        try {
            val selector = if (exactMatch) {
                UiSelector().text(text)
            } else {
                UiSelector().textContains(text)
            }

            val element = device.findObject(selector)
            if (element.exists()) {
                element.click()
                println("[$testName] Clicked element with text: $text")
            } else {
                throw IllegalStateException("Element with text '$text' not found")
            }
        } catch (e: Exception) {
            println("[$testName] Failed to click element with text '$text': ${e.message}")
            throw e
        }
    }

    /**
     * Click using UiAutomator by resource ID (string format)
     */
    fun clickByResourceId(resourceIdName: String) {
        try {
            val fullResourceId = "com.mediathekview.android:id/$resourceIdName"
            val element = device.findObject(UiSelector().resourceId(fullResourceId))

            if (element.exists()) {
                element.click()
                println("[$testName] Clicked element with resource ID: $resourceIdName")
            } else {
                throw IllegalStateException("Element with resource ID '$resourceIdName' not found")
            }
        } catch (e: Exception) {
            println("[$testName] Failed to click element with resource ID '$resourceIdName': ${e.message}")
            throw e
        }
    }

    /**
     * Click at specific coordinates
     */
    fun clickAt(x: Int, y: Int) {
        device.click(x, y)
        println("[$testName] Clicked at coordinates ($x, $y)")
    }

    /**
     * Swipe gesture
     */
    fun swipe(startX: Int, startY: Int, endX: Int, endY: Int, steps: Int = 10) {
        device.swipe(startX, startY, endX, endY, steps)
        println("[$testName] Swiped from ($startX, $startY) to ($endX, $endY)")
    }

    /**
     * Press back button
     */
    fun pressBack() {
        device.pressBack()
        println("[$testName] Pressed back button")
    }

    /**
     * Press home button
     */
    fun pressHome() {
        device.pressHome()
        println("[$testName] Pressed home button")
    }

    /**
     * Verify text is displayed
     */
    fun assertTextDisplayed(text: String) {
        val element = device.findObject(UiSelector().textContains(text))
        if (!element.exists()) {
            throw AssertionError("Text '$text' not found on screen")
        }
        println("[$testName] Verified text displayed: $text")
    }

    /**
     * Verify text is not displayed
     */
    fun assertTextNotDisplayed(text: String) {
        val element = device.findObject(UiSelector().textContains(text))
        if (element.exists()) {
            throw AssertionError("Text '$text' should not be displayed but was found")
        }
        println("[$testName] Verified text not displayed: $text")
    }

    /**
     * Custom action with a description
     */
    fun action(description: String, block: () -> Unit) {
        println("[$testName] Action: $description")
        try {
            block()
            println("[$testName] Action completed: $description")
        } catch (e: Exception) {
            println("[$testName] Action failed: $description - ${e.message}")
            throw e
        }
    }

    /**
     * Get the current activity
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
}

/**
 * Extension function to find views matching a matcher
 */
fun UiTestScope.findView(matcher: Matcher<View>): Boolean {
    return try {
        onView(matcher).check { view, _ ->
            view != null
        }
        true
    } catch (e: Exception) {
        false
    }
}
