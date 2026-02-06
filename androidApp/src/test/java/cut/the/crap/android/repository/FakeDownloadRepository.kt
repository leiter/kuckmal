package cut.the.crap.android.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake implementation of download functionality for testing MediaViewModel
 * Provides controllable download state without actual network operations
 */
class FakeDownloadRepository : DownloadRepositoryInterface {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    override val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // Track method calls for verification
    var startDownloadCalled = false
    var lastDownloadUrl: String? = null
    var lastDownloadFilename: String? = null
    var resetStateCalled = false

    // Control behavior
    var shouldFailDownload = false
    var networkAvailable = true

    /**
     * Simulate starting a download
     */
    override fun startDownload(url: String, filename: String): Boolean {
        startDownloadCalled = true
        lastDownloadUrl = url
        lastDownloadFilename = filename

        if (!networkAvailable) {
            _downloadState.value = DownloadState.Error(
                message = "No internet connection",
                canRetry = true,
                retryAction = { startDownload(url, filename) }
            )
            return false
        }

        if (shouldFailDownload) {
            _downloadState.value = DownloadState.Error(
                message = "Download failed",
                canRetry = true,
                retryAction = { startDownload(url, filename) }
            )
            return false
        }

        _downloadState.value = DownloadState.Downloading(0, 0, 10, "0 KB/s")
        return true
    }

    /**
     * Simulate download progress
     */
    fun simulateProgress(progress: Int, downloadedMB: Int, totalMB: Int, speed: String = "1 MB/s") {
        _downloadState.value = DownloadState.Downloading(progress, downloadedMB, totalMB, speed)
    }

    /**
     * Simulate download success
     */
    fun simulateSuccess(filename: String, shouldExtract: Boolean = true) {
        _downloadState.value = DownloadState.Success(filename, shouldExtract)
    }

    /**
     * Simulate download error
     */
    fun simulateError(message: String, canRetry: Boolean = true) {
        _downloadState.value = DownloadState.Error(message, canRetry, null)
    }

    /**
     * Simulate extraction in progress
     */
    fun simulateExtracting(progress: Int = 50) {
        _downloadState.value = DownloadState.Extracting(progress)
    }

    /**
     * Simulate extraction complete
     */
    fun simulateExtractionComplete(extractedFile: String) {
        _downloadState.value = DownloadState.ExtractionComplete(extractedFile)
    }

    /**
     * Reset state to idle
     */
    override fun resetState() {
        resetStateCalled = true
        _downloadState.value = DownloadState.Idle
    }

    /**
     * Reset test state for next test
     */
    fun reset() {
        resetState()
        startDownloadCalled = false
        lastDownloadUrl = null
        lastDownloadFilename = null
        resetStateCalled = false
        shouldFailDownload = false
        networkAvailable = true
    }
}
