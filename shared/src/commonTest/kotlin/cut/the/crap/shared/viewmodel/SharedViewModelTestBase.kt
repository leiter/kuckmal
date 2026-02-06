package cut.the.crap.shared.viewmodel

import cut.the.crap.shared.database.MediaEntry
import cut.the.crap.shared.repository.FakeMediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Base test class for SharedViewModel tests
 * Sets up coroutine test dispatcher for testing
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class SharedViewModelTestBase {

    protected val testDispatcher: TestDispatcher = StandardTestDispatcher()
    protected lateinit var fakeRepository: FakeMediaRepository

    // Callback tracking
    protected var playVideoCallCount = 0
    protected var lastPlayedEntry: MediaEntry? = null
    protected var lastPlayedHighQuality: Boolean? = null

    protected var downloadVideoCallCount = 0
    protected var lastDownloadedEntry: MediaEntry? = null
    protected var lastDownloadedUrl: String? = null
    protected var lastDownloadedQuality: String? = null

    protected var showToastCallCount = 0
    protected var lastToastMessage: String? = null

    @BeforeTest
    open fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeMediaRepository()
        resetCallbackTracking()
    }

    @AfterTest
    open fun tearDown() {
        Dispatchers.resetMain()
    }

    protected fun resetCallbackTracking() {
        playVideoCallCount = 0
        lastPlayedEntry = null
        lastPlayedHighQuality = null
        downloadVideoCallCount = 0
        lastDownloadedEntry = null
        lastDownloadedUrl = null
        lastDownloadedQuality = null
        showToastCallCount = 0
        lastToastMessage = null
    }

    protected fun createViewModel(): SharedViewModel {
        return SharedViewModel(
            repository = fakeRepository,
            onPlayVideo = { entry, isHighQuality ->
                playVideoCallCount++
                lastPlayedEntry = entry
                lastPlayedHighQuality = isHighQuality
            },
            onDownloadVideo = { entry, url, quality ->
                downloadVideoCallCount++
                lastDownloadedEntry = entry
                lastDownloadedUrl = url
                lastDownloadedQuality = quality
            },
            onShowToast = { message ->
                showToastCallCount++
                lastToastMessage = message
            },
            getFilesPath = { "/test/files/path" },
            getAllChannels = { emptyList() }
        )
    }

    protected fun advanceUntilIdle() {
        testDispatcher.scheduler.advanceUntilIdle()
    }

    protected fun advanceTimeBy(delayMillis: Long) {
        testDispatcher.scheduler.advanceTimeBy(delayMillis)
    }

    protected fun createTestEntry(
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
