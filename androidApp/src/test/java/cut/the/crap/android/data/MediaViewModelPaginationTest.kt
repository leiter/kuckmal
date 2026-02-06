package cut.the.crap.android.data

import android.app.Application
import app.cash.turbine.test
import cut.the.crap.android.base.ViewModelTestBase
import cut.the.crap.android.repository.FakeDownloadRepository
import cut.the.crap.android.repository.FakeMediaRepository
import cut.the.crap.android.util.FakeUpdateChecker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Tests for MediaViewModel pagination and filtering functionality
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class MediaViewModelPaginationTest : ViewModelTestBase() {

    private lateinit var viewModel: MediaViewModel
    private lateinit var fakeRepository: FakeMediaRepository
    private lateinit var fakeDownloadRepository: FakeDownloadRepository
    private lateinit var fakeUpdateChecker: FakeUpdateChecker
    private lateinit var application: Application

    @Before
    override fun setup() {
        super.setup()
        application = RuntimeEnvironment.getApplication()
        fakeRepository = FakeMediaRepository()
        fakeDownloadRepository = FakeDownloadRepository()
        fakeUpdateChecker = FakeUpdateChecker()

        viewModel = MediaViewModel(
            application = application,
            repository = fakeRepository,
            downloadRepository = fakeDownloadRepository,
            updateChecker = fakeUpdateChecker
        )
    }

    // =================================================================================
    // Current Part Tests
    // =================================================================================

    @Test
    fun `initial currentPart is zero`() = runTest {
        assertEquals(0, viewModel.currentPart.value)
    }

    @Test
    fun `setCurrentPart updates current part`() = runTest {
        viewModel.setCurrentPart(3)
        assertEquals(3, viewModel.currentPart.value)
    }

    @Test
    fun `nextPart increments current part`() = runTest {
        assertEquals(0, viewModel.currentPart.value)

        viewModel.nextPart()
        assertEquals(1, viewModel.currentPart.value)

        viewModel.nextPart()
        assertEquals(2, viewModel.currentPart.value)
    }

    @Test
    fun `previousPart decrements current part`() = runTest {
        viewModel.setCurrentPart(3)

        viewModel.previousPart()
        assertEquals(2, viewModel.currentPart.value)

        viewModel.previousPart()
        assertEquals(1, viewModel.currentPart.value)
    }

    @Test
    fun `previousPart does not go below zero`() = runTest {
        assertEquals(0, viewModel.currentPart.value)

        viewModel.previousPart()
        assertEquals(0, viewModel.currentPart.value)

        viewModel.previousPart()
        assertEquals(0, viewModel.currentPart.value)
    }

    @Test
    fun `currentPart emits updates`() = runTest {
        viewModel.currentPart.test {
            assertEquals(0, awaitItem())

            viewModel.setCurrentPart(5)
            assertEquals(5, awaitItem())

            viewModel.nextPart()
            assertEquals(6, awaitItem())

            viewModel.previousPart()
            assertEquals(5, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    // =================================================================================
    // Date Filter Tests
    // =================================================================================

    @Test
    fun `initial timePeriodId is default value`() = runTest {
        assertEquals(3, viewModel.timePeriodId.value)
    }

    @Test
    fun `setDateFilter updates timePeriodId`() = runTest {
        viewModel.setDateFilter(limitDate = 1000000L, timePeriodId = 5)
        assertEquals(5, viewModel.timePeriodId.value)
    }

    @Test
    fun `timePeriodId emits updates`() = runTest {
        viewModel.timePeriodId.test {
            assertEquals(3, awaitItem())

            viewModel.setDateFilter(limitDate = 1000000L, timePeriodId = 1)
            assertEquals(1, awaitItem())

            viewModel.setDateFilter(limitDate = 2000000L, timePeriodId = 7)
            assertEquals(7, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    // =================================================================================
    // Navigation Resets Part Tests
    // =================================================================================

    @Test
    fun `navigateToThemes resets currentPart to zero`() = runTest {
        viewModel.setCurrentPart(10)
        assertEquals(10, viewModel.currentPart.value)

        viewModel.navigateToThemes(channel = "ARD")

        assertEquals(0, viewModel.currentPart.value)
    }

    @Test
    fun `navigateToThemes with theme resets currentPart to zero`() = runTest {
        viewModel.setCurrentPart(5)

        viewModel.navigateToThemes(channel = "ARD", theme = "News")

        assertEquals(0, viewModel.currentPart.value)
    }

    @Test
    fun `multiple navigations always reset currentPart`() = runTest {
        viewModel.navigateToThemes(channel = "ARD")
        viewModel.setCurrentPart(3)
        assertEquals(3, viewModel.currentPart.value)

        viewModel.navigateToThemes(channel = "ZDF")
        assertEquals(0, viewModel.currentPart.value)

        viewModel.setCurrentPart(7)
        viewModel.navigateToThemes(channel = null)
        assertEquals(0, viewModel.currentPart.value)
    }

    // =================================================================================
    // Combined Pagination and Filter Tests
    // =================================================================================

    @Test
    fun `date filter and pagination work independently`() = runTest {
        // Set date filter
        viewModel.setDateFilter(limitDate = 1000000L, timePeriodId = 2)

        // Set pagination
        viewModel.setCurrentPart(4)

        // Verify both
        assertEquals(2, viewModel.timePeriodId.value)
        assertEquals(4, viewModel.currentPart.value)

        // Change one doesn't affect the other
        viewModel.setDateFilter(limitDate = 2000000L, timePeriodId = 5)
        assertEquals(4, viewModel.currentPart.value)

        viewModel.setCurrentPart(8)
        assertEquals(5, viewModel.timePeriodId.value)
    }
}
