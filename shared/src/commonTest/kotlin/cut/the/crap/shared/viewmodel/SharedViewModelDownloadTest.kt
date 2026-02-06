package cut.the.crap.shared.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for SharedViewModel download and callback functionality
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SharedViewModelDownloadTest : SharedViewModelTestBase() {

    // =================================================================================
    // Download State Tests
    // =================================================================================

    @Test
    fun `initial downloadState is Idle`() = runTest {
        val viewModel = createViewModel()
        assertTrue(viewModel.downloadState.value is DownloadState.Idle)
    }

    @Test
    fun `resetDownloadState returns to Idle`() = runTest {
        val viewModel = createViewModel()

        // Assuming some internal state change (we can't directly set it to non-Idle)
        viewModel.resetDownloadState()

        assertTrue(viewModel.downloadState.value is DownloadState.Idle)
    }

    @Test
    fun `downloadState emits Idle initially`() = runTest {
        val viewModel = createViewModel()

        viewModel.downloadState.test {
            val initial = awaitItem()
            assertTrue(initial is DownloadState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // =================================================================================
    // Callback Tests - playVideo
    // =================================================================================

    @Test
    fun `playVideo invokes callback with correct entry`() = runTest {
        val viewModel = createViewModel()
        val entry = createTestEntry(title = "Test Video")

        viewModel.playVideo(entry, isHighQuality = true)

        assertEquals(1, playVideoCallCount)
        assertEquals(entry, lastPlayedEntry)
    }

    @Test
    fun `playVideo passes isHighQuality true`() = runTest {
        val viewModel = createViewModel()
        val entry = createTestEntry()

        viewModel.playVideo(entry, isHighQuality = true)

        assertEquals(true, lastPlayedHighQuality)
    }

    @Test
    fun `playVideo passes isHighQuality false`() = runTest {
        val viewModel = createViewModel()
        val entry = createTestEntry()

        viewModel.playVideo(entry, isHighQuality = false)

        assertEquals(false, lastPlayedHighQuality)
    }

    @Test
    fun `multiple playVideo calls invoke callback each time`() = runTest {
        val viewModel = createViewModel()
        val entry1 = createTestEntry(id = 1, title = "Video 1")
        val entry2 = createTestEntry(id = 2, title = "Video 2")

        viewModel.playVideo(entry1, isHighQuality = true)
        viewModel.playVideo(entry2, isHighQuality = false)

        assertEquals(2, playVideoCallCount)
        assertEquals(entry2, lastPlayedEntry)
        assertEquals(false, lastPlayedHighQuality)
    }

    // =================================================================================
    // Callback Tests - downloadVideo
    // =================================================================================

    @Test
    fun `downloadVideo invokes callback with correct parameters`() = runTest {
        val viewModel = createViewModel()
        val entry = createTestEntry()

        viewModel.downloadVideo(entry, "https://example.com/video.mp4", "high")

        assertEquals(1, downloadVideoCallCount)
        assertEquals(entry, lastDownloadedEntry)
        assertEquals("https://example.com/video.mp4", lastDownloadedUrl)
        assertEquals("high", lastDownloadedQuality)
    }

    @Test
    fun `downloadVideo passes quality correctly`() = runTest {
        val viewModel = createViewModel()
        val entry = createTestEntry()

        viewModel.downloadVideo(entry, "https://example.com/video.mp4", "low")

        assertEquals("low", lastDownloadedQuality)
    }

    @Test
    fun `multiple downloadVideo calls invoke callback each time`() = runTest {
        val viewModel = createViewModel()
        val entry = createTestEntry()

        viewModel.downloadVideo(entry, "https://example.com/video1.mp4", "high")
        viewModel.downloadVideo(entry, "https://example.com/video2.mp4", "low")

        assertEquals(2, downloadVideoCallCount)
        assertEquals("https://example.com/video2.mp4", lastDownloadedUrl)
        assertEquals("low", lastDownloadedQuality)
    }

    // =================================================================================
    // Callback Tests - showToast
    // =================================================================================

    @Test
    fun `showToast invokes callback with message`() = runTest {
        val viewModel = createViewModel()

        viewModel.showToast("Test message")

        assertEquals(1, showToastCallCount)
        assertEquals("Test message", lastToastMessage)
    }

    @Test
    fun `multiple showToast calls invoke callback each time`() = runTest {
        val viewModel = createViewModel()

        viewModel.showToast("Message 1")
        viewModel.showToast("Message 2")
        viewModel.showToast("Message 3")

        assertEquals(3, showToastCallCount)
        assertEquals("Message 3", lastToastMessage)
    }

    @Test
    fun `showToast handles empty message`() = runTest {
        val viewModel = createViewModel()

        viewModel.showToast("")

        assertEquals(1, showToastCallCount)
        assertEquals("", lastToastMessage)
    }

    // =================================================================================
    // setHasData Tests
    // =================================================================================

    @Test
    fun `setHasData updates isDataLoadedFlow`() = runTest {
        fakeRepository.clearEntries()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setHasData(true)
        advanceUntilIdle() // Allow combined flow to emit

        assertTrue(viewModel.isDataLoadedFlow.value)
    }

    @Test
    fun `setHasData false with NOT_LOADED state results in not loaded`() = runTest {
        val viewModel = createViewModel()
        viewModel.setLoadingState(LoadingState.NOT_LOADED)
        viewModel.setHasData(false)
        advanceUntilIdle()

        // isDataLoadedFlow should be false when both state is NOT_LOADED and hasData is false
        assertEquals(false, viewModel.isDataLoadedFlow.value)
    }

    // =================================================================================
    // Channels Property Tests
    // =================================================================================

    @Test
    fun `channels property returns value from callback`() = runTest {
        val viewModel = createViewModel()

        // Our test setup returns empty list
        assertTrue(viewModel.channels.isEmpty())
    }

    // =================================================================================
    // Error State Tests
    // =================================================================================

    @Test
    fun `error message can be set via loading state`() = runTest {
        val viewModel = createViewModel()

        viewModel.setLoadingState(LoadingState.ERROR)

        assertEquals(LoadingState.ERROR, viewModel.loadingState.value)
    }

    // =================================================================================
    // Combined Callback and State Tests
    // =================================================================================

    @Test
    fun `callbacks work independently of loading state`() = runTest {
        val viewModel = createViewModel()
        viewModel.setLoadingState(LoadingState.LOADING)

        // Callbacks should still work during loading
        viewModel.showToast("Loading message")
        viewModel.playVideo(createTestEntry(), true)

        assertEquals(1, showToastCallCount)
        assertEquals(1, playVideoCallCount)
    }

    @Test
    fun `callbacks work with empty repository`() = runTest {
        fakeRepository.clearEntries()
        val viewModel = createViewModel()

        viewModel.showToast("Message")
        viewModel.playVideo(createTestEntry(), false)
        viewModel.downloadVideo(createTestEntry(), "url", "quality")

        assertEquals(1, showToastCallCount)
        assertEquals(1, playVideoCallCount)
        assertEquals(1, downloadVideoCallCount)
    }
}
