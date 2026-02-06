package cut.the.crap.shared.viewmodel

import app.cash.turbine.test
import cut.the.crap.shared.database.MediaEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for SharedViewModel state management functionality
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SharedViewModelTest : SharedViewModelTestBase() {

    // =================================================================================
    // Initial State Tests
    // =================================================================================

    @Test
    fun `initial loading state is NOT_LOADED when repository is empty`() = runTest {
        // Empty repository - must be cleared BEFORE creating viewModel
        fakeRepository.clearEntries()
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Should be NOT_LOADED when repository is empty
        // Note: The init block checks repository.getCount() asynchronously
        assertEquals(LoadingState.NOT_LOADED, viewModel.loadingState.value)
    }

    @Test
    fun `loading state is LOADED when repository has data`() = runTest {
        // Repository with data
        fakeRepository.addEntries(listOf(createTestEntry()))
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(LoadingState.LOADED, viewModel.loadingState.value)
    }

    @Test
    fun `initial loading progress is zero`() = runTest {
        val viewModel = createViewModel()
        assertEquals(0, viewModel.loadingProgress.value)
    }

    @Test
    fun `initial error message is empty`() = runTest {
        val viewModel = createViewModel()
        assertEquals("", viewModel.errorMessage.value)
    }

    @Test
    fun `initial download state is Idle`() = runTest {
        val viewModel = createViewModel()
        assertTrue(viewModel.downloadState.value is DownloadState.Idle)
    }

    @Test
    fun `initial dialog state is null`() = runTest {
        val viewModel = createViewModel()
        assertNull(viewModel.dialogState.value)
    }

    @Test
    fun `initial view state is Themes`() = runTest {
        val viewModel = createViewModel()
        assertTrue(viewModel.viewState.value is ViewState.Themes)
    }

    @Test
    fun `initial current part is zero`() = runTest {
        val viewModel = createViewModel()
        assertEquals(0, viewModel.currentPart.value)
    }

    @Test
    fun `initial time period id is default value`() = runTest {
        val viewModel = createViewModel()
        assertEquals(3, viewModel.timePeriodId.value)
    }

    // =================================================================================
    // Loading State Management Tests
    // =================================================================================

    @Test
    fun `setLoadingState updates loading state`() = runTest {
        val viewModel = createViewModel()

        viewModel.setLoadingState(LoadingState.LOADING)
        assertEquals(LoadingState.LOADING, viewModel.loadingState.value)

        viewModel.setLoadingState(LoadingState.LOADED)
        assertEquals(LoadingState.LOADED, viewModel.loadingState.value)

        viewModel.setLoadingState(LoadingState.ERROR)
        assertEquals(LoadingState.ERROR, viewModel.loadingState.value)
    }

    @Test
    fun `setLoadingProgress updates loading progress`() = runTest {
        val viewModel = createViewModel()

        viewModel.setLoadingProgress(50)
        assertEquals(50, viewModel.loadingProgress.value)

        viewModel.setLoadingProgress(100)
        assertEquals(100, viewModel.loadingProgress.value)
    }

    @Test
    fun `loadingState emits updates`() = runTest {
        val viewModel = createViewModel()

        viewModel.loadingState.test {
            // Initial state
            awaitItem()

            viewModel.setLoadingState(LoadingState.LOADING)
            assertEquals(LoadingState.LOADING, awaitItem())

            viewModel.setLoadingState(LoadingState.LOADED)
            assertEquals(LoadingState.LOADED, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    // =================================================================================
    // isDataLoadedFlow Tests
    // =================================================================================

    @Test
    fun `isDataLoadedFlow returns false when NOT_LOADED and no data`() = runTest {
        fakeRepository.clearEntries()
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.isDataLoadedFlow.value)
    }

    @Test
    fun `isDataLoadedFlow returns true when LOADED`() = runTest {
        fakeRepository.clearEntries()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setLoadingState(LoadingState.LOADED)
        advanceUntilIdle()

        assertTrue(viewModel.isDataLoadedFlow.value)
    }

    @Test
    fun `isDataLoadedFlow returns true when repository has data`() = runTest {
        fakeRepository.addEntries(listOf(createTestEntry()))
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.isDataLoadedFlow.value)
    }

    // =================================================================================
    // clearData Tests
    // =================================================================================

    @Test
    fun `clearData resets loading state to NOT_LOADED`() = runBlocking {
        // Use runBlocking because clearData uses Dispatchers.Default internally
        fakeRepository.addEntries(listOf(createTestEntry()))
        val viewModel = createViewModel()
        delay(100) // Wait for init to complete

        viewModel.clearData()
        delay(100) // Wait for clearData coroutine on Dispatchers.Default

        assertEquals(LoadingState.NOT_LOADED, viewModel.loadingState.value)
    }

    @Test
    fun `clearData calls repository deleteAll`() = runBlocking {
        // Use runBlocking because clearData uses Dispatchers.Default internally
        fakeRepository.addEntries(listOf(createTestEntry()))
        val viewModel = createViewModel()
        delay(100) // Wait for init to complete

        viewModel.clearData()
        delay(100) // Wait for clearData coroutine on Dispatchers.Default

        assertTrue(fakeRepository.deleteAllCalled)
    }

    // =================================================================================
    // isDataLoaded Tests
    // =================================================================================

    @Test
    fun `isDataLoaded returns false when repository is empty`() = runTest {
        fakeRepository.clearEntries()
        val viewModel = createViewModel()

        val result = viewModel.isDataLoaded()
        assertFalse(result)
    }

    @Test
    fun `isDataLoaded returns true when repository has entries`() = runTest {
        fakeRepository.addEntries(listOf(createTestEntry()))
        val viewModel = createViewModel()

        val result = viewModel.isDataLoaded()
        assertTrue(result)
    }

    // =================================================================================
    // Dialog Management Tests
    // =================================================================================

    @Test
    fun `showDialog sets dialog state`() = runTest {
        val viewModel = createViewModel()
        val dialog = DialogState.Message(
            title = "Test Title",
            message = "Test Message"
        )

        viewModel.showDialog(dialog)

        assertEquals(dialog, viewModel.dialogState.value)
    }

    @Test
    fun `dismissDialog clears dialog state`() = runTest {
        val viewModel = createViewModel()
        val dialog = DialogState.Message(
            title = "Test Title",
            message = "Test Message"
        )

        viewModel.showDialog(dialog)
        assertTrue(viewModel.dialogState.value != null)

        viewModel.dismissDialog()
        assertNull(viewModel.dialogState.value)
    }

    @Test
    fun `dialogState emits updates`() = runTest {
        val viewModel = createViewModel()

        viewModel.dialogState.test {
            // Initial null
            assertNull(awaitItem())

            // Show dialog
            val dialog = DialogState.Message(title = "Title", message = "Message")
            viewModel.showDialog(dialog)
            assertEquals(dialog, awaitItem())

            // Dismiss dialog
            viewModel.dismissDialog()
            assertNull(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    // =================================================================================
    // Search State Tests
    // =================================================================================

    @Test
    fun `setSearching does not throw`() = runTest {
        val viewModel = createViewModel()

        viewModel.setSearching(true)
        viewModel.setSearching(false)
        // Success if no exception
    }

    // =================================================================================
    // Date Filter Tests
    // =================================================================================

    @Test
    fun `setDateFilter updates timePeriodId`() = runTest {
        val viewModel = createViewModel()

        viewModel.setDateFilter(limitDate = 1000000L, timePeriodId = 5)

        assertEquals(5, viewModel.timePeriodId.value)
    }

    @Test
    fun `timePeriodId emits updates`() = runTest {
        val viewModel = createViewModel()

        viewModel.timePeriodId.test {
            assertEquals(3, awaitItem()) // Default

            viewModel.setDateFilter(limitDate = 1000000L, timePeriodId = 1)
            assertEquals(1, awaitItem())

            viewModel.setDateFilter(limitDate = 2000000L, timePeriodId = 7)
            assertEquals(7, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    // =================================================================================
    // Pagination Tests
    // =================================================================================

    @Test
    fun `setCurrentPart updates current part`() = runTest {
        val viewModel = createViewModel()

        viewModel.setCurrentPart(3)
        assertEquals(3, viewModel.currentPart.value)
    }

    @Test
    fun `nextPart increments current part`() = runTest {
        val viewModel = createViewModel()

        viewModel.nextPart()
        assertEquals(1, viewModel.currentPart.value)

        viewModel.nextPart()
        assertEquals(2, viewModel.currentPart.value)
    }

    @Test
    fun `previousPart decrements current part`() = runTest {
        val viewModel = createViewModel()
        viewModel.setCurrentPart(3)

        viewModel.previousPart()
        assertEquals(2, viewModel.currentPart.value)
    }

    @Test
    fun `previousPart does not go below zero`() = runTest {
        val viewModel = createViewModel()
        assertEquals(0, viewModel.currentPart.value)

        viewModel.previousPart()
        assertEquals(0, viewModel.currentPart.value)
    }

    // =================================================================================
    // Computed Properties Tests
    // =================================================================================

    @Test
    fun `selectedChannel returns null initially`() = runTest {
        val viewModel = createViewModel()
        assertNull(viewModel.selectedChannel)
    }

    @Test
    fun `selectedTheme returns null initially`() = runTest {
        val viewModel = createViewModel()
        assertNull(viewModel.selectedTheme)
    }

    @Test
    fun `selectedTitle returns null in Themes state`() = runTest {
        val viewModel = createViewModel()
        assertNull(viewModel.selectedTitle)
    }

    @Test
    fun `currentMediaEntry returns null in Themes state`() = runTest {
        val viewModel = createViewModel()
        assertNull(viewModel.currentMediaEntry)
    }
}
