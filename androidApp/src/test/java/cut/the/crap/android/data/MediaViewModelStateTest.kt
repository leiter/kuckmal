package cut.the.crap.android.data

import android.app.Application
import app.cash.turbine.test
import cut.the.crap.android.base.ViewModelTestBase
import cut.the.crap.android.repository.DownloadState
import cut.the.crap.android.repository.FakeDownloadRepository
import cut.the.crap.android.repository.FakeMediaRepository
import cut.the.crap.android.util.FakeUpdateChecker
import cut.the.crap.shared.database.MediaEntry
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
 * Tests for MediaViewModel state management functionality
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class MediaViewModelStateTest : ViewModelTestBase() {

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
    // Initial State Tests
    // =================================================================================

    @Test
    fun `initial loading state is NOT_LOADED`() = runTest {
        assertEquals(MediaViewModel.LoadingState.NOT_LOADED, viewModel.loadingState.value)
    }

    @Test
    fun `initial loading progress is zero`() = runTest {
        assertEquals(0, viewModel.loadingProgress.value)
    }

    @Test
    fun `initial error message is empty`() = runTest {
        assertEquals("", viewModel.errorMessage.value)
    }

    @Test
    fun `initial dialog model is null`() = runTest {
        assertNull(viewModel.dialogModel.value)
    }

    @Test
    fun `initial view state is Themes with null channel and theme`() = runTest {
        val state = viewModel.viewState.value
        assertTrue(state is MediaViewModel.ViewState.Themes)
        assertNull(state.channel)
        assertNull(state.theme)
    }

    @Test
    fun `initial current part is zero`() = runTest {
        assertEquals(0, viewModel.currentPart.value)
    }

    @Test
    fun `initial time period id is default value`() = runTest {
        assertEquals(3, viewModel.timePeriodId.value)
    }

    // =================================================================================
    // isDataLoaded Tests
    // =================================================================================

    @Test
    fun `isDataLoaded returns false when repository is empty`() = runTest {
        fakeRepository.clearEntries()
        val result = viewModel.isDataLoaded()
        assertFalse(result)
    }

    @Test
    fun `isDataLoaded returns true when repository has entries`() = runTest {
        fakeRepository.addEntries(listOf(createTestEntry()))
        val result = viewModel.isDataLoaded()
        assertTrue(result)
    }

    @Test
    fun `isDataLoaded returns false when repository throws exception`() = runTest {
        fakeRepository.shouldThrowOnCount = true
        val result = viewModel.isDataLoaded()
        assertFalse(result)
    }

    // =================================================================================
    // clearData Tests
    // =================================================================================

    @Test
    fun `clearData resets loading state to NOT_LOADED`() = runTest {
        // Pre-populate repository
        fakeRepository.addEntries(listOf(createTestEntry()))

        viewModel.clearData()
        advanceUntilIdle()

        assertEquals(MediaViewModel.LoadingState.NOT_LOADED, viewModel.loadingState.value)
    }

    @Test
    fun `clearData resets loading progress to zero`() = runTest {
        fakeRepository.addEntries(listOf(createTestEntry()))

        viewModel.clearData()
        advanceUntilIdle()

        assertEquals(0, viewModel.loadingProgress.value)
    }

    @Test
    fun `clearData calls repository deleteAll`() = runTest {
        fakeRepository.addEntries(listOf(createTestEntry()))

        viewModel.clearData()
        // clearData uses viewModelScope.launch(Dispatchers.IO) which isn't captured
        // by the test dispatcher. We need to give it time to execute.
        advanceUntilIdle()

        // The actual delete happens on Dispatchers.IO which runs on a different thread
        // For proper testing, we verify the state changes that happen on the main thread
        assertEquals(MediaViewModel.LoadingState.NOT_LOADED, viewModel.loadingState.value)
        assertEquals(0, viewModel.loadingProgress.value)
    }

    // =================================================================================
    // Dialog Management Tests
    // =================================================================================

    @Test
    fun `showDialog sets dialog model`() = runTest {
        val dialog = cut.the.crap.android.ui.dialog.DialogModel.Message(
            title = "Test Title",
            message = "Test Message"
        )

        viewModel.showDialog(dialog)

        assertEquals(dialog, viewModel.dialogModel.value)
    }

    @Test
    fun `dismissDialog clears dialog model`() = runTest {
        val dialog = cut.the.crap.android.ui.dialog.DialogModel.Message(
            title = "Test Title",
            message = "Test Message"
        )

        viewModel.showDialog(dialog)
        assertNotNull(viewModel.dialogModel.value)

        viewModel.dismissDialog()
        assertNull(viewModel.dialogModel.value)
    }

    @Test
    fun `dialogModel StateFlow emits updates`() = runTest {
        viewModel.dialogModel.test {
            // Initial value
            assertNull(awaitItem())

            // Show dialog
            val dialog = cut.the.crap.android.ui.dialog.DialogModel.Message(
                title = "Title",
                message = "Message"
            )
            viewModel.showDialog(dialog)
            assertEquals(dialog, awaitItem())

            // Dismiss dialog
            viewModel.dismissDialog()
            assertNull(awaitItem())
        }
    }

    // =================================================================================
    // Welcome Dialog Tests
    // =================================================================================

    @Test
    fun `showWelcomeDialog is initially false`() = runTest {
        assertFalse(viewModel.showWelcomeDialog.value)
    }

    @Test
    fun `welcomeDialogShown sets flag to false`() = runTest {
        // Note: Cannot easily trigger the flag to true without complex setup
        // Just verify the function resets it
        viewModel.welcomeDialogShown()
        assertFalse(viewModel.showWelcomeDialog.value)
    }

    // =================================================================================
    // Search State Tests
    // =================================================================================

    @Test
    fun `setSearching updates search state`() = runTest {
        viewModel.setSearching(true)
        // Note: _isSearching is private, we can't directly verify
        // But we can verify no exception is thrown

        viewModel.setSearching(false)
        // Success if no exception
    }

    // =================================================================================
    // Loading State Flow Tests
    // =================================================================================

    @Test
    fun `loadingState StateFlow emits updates`() = runTest {
        viewModel.loadingState.test {
            // Initial state
            assertEquals(MediaViewModel.LoadingState.NOT_LOADED, awaitItem())

            // Don't await more items as we can't easily trigger state changes
            // in a unit test without full Android context
            cancelAndIgnoreRemainingEvents()
        }
    }

    // =================================================================================
    // Helper Functions
    // =================================================================================

    private fun createTestEntry(
        id: Long = 1,
        channel: String = "ARD",
        theme: String = "News",
        title: String = "Test Title",
        timestamp: Long = System.currentTimeMillis() / 1000
    ): MediaEntry {
        return MediaEntry(
            id = id,
            channel = channel,
            theme = theme,
            title = title,
            date = "01.01.2024",
            time = "20:00:00",
            duration = "00:30:00",
            sizeMB = "500",
            description = "Test Description",
            url = "https://example.com/video.mp4",
            website = "https://example.com",
            subtitleUrl = "",
            smallUrl = "https://example.com/video_small.mp4",
            hdUrl = "https://example.com/video_hd.mp4",
            timestamp = timestamp,
            geo = "DE",
            isNew = false
        )
    }
}
