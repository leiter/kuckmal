package cut.the.crap.android.base

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule

/**
 * Base test class for Android ViewModel tests
 * Sets up coroutine test dispatcher and instant task executor for LiveData
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelTestBase {

    /**
     * Rule to run LiveData operations synchronously on the main thread
     */
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    /**
     * Test dispatcher for coroutines
     */
    protected val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    open fun setup() {
        // Set the main dispatcher to the test dispatcher
        Dispatchers.setMain(testDispatcher)
    }

    @After
    open fun tearDown() {
        // Reset the main dispatcher
        Dispatchers.resetMain()
    }

    /**
     * Advance the test dispatcher until all pending coroutines are complete
     */
    protected fun advanceUntilIdle() {
        testDispatcher.scheduler.advanceUntilIdle()
    }

    /**
     * Advance the test dispatcher by a specific time
     */
    protected fun advanceTimeBy(delayMillis: Long) {
        testDispatcher.scheduler.advanceTimeBy(delayMillis)
    }

    /**
     * Run a single pending task
     */
    protected fun runCurrent() {
        testDispatcher.scheduler.runCurrent()
    }
}
