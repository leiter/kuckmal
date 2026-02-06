package cut.the.crap.android.util

/**
 * Fake implementation of UpdateChecker for testing MediaViewModel
 * Provides controllable update check results without actual network operations
 */
class FakeUpdateChecker : UpdateCheckerInterface {

    // Control the result returned by checkForUpdate
    var updateCheckResult: UpdateChecker.UpdateCheckResult = UpdateChecker.UpdateCheckResult.NoUpdateNeeded

    // Track method calls
    var checkForUpdateCalled = false
    var lastForceCheck: Boolean? = null
    var lastUseDiff: Boolean? = null
    var saveDownloadMetadataCalled = false

    /**
     * Simulate checking for update
     */
    override fun checkForUpdate(forceCheck: Boolean, useDiff: Boolean): UpdateChecker.UpdateCheckResult {
        checkForUpdateCalled = true
        lastForceCheck = forceCheck
        lastUseDiff = useDiff
        return updateCheckResult
    }

    /**
     * Simulate saving download metadata
     */
    override fun saveDownloadMetadata(lastModified: String?, etag: String?, contentLength: Long) {
        saveDownloadMetadataCalled = true
    }

    /**
     * Get next check description
     */
    override fun getNextCheckDescription(): String {
        return "Next check in ~4 hours"
    }

    /**
     * Configure to return update available
     */
    fun setUpdateAvailable(lastModified: String? = null, etag: String? = null, contentLength: Long = 50_000_000) {
        updateCheckResult = UpdateChecker.UpdateCheckResult.UpdateAvailable(
            lastModified = lastModified,
            etag = etag,
            contentLength = contentLength
        )
    }

    /**
     * Configure to return no update needed
     */
    fun setNoUpdateNeeded() {
        updateCheckResult = UpdateChecker.UpdateCheckResult.NoUpdateNeeded
    }

    /**
     * Configure to return check skipped
     */
    fun setCheckSkipped(nextCheckTime: Long = System.currentTimeMillis() + 4 * 60 * 60 * 1000) {
        updateCheckResult = UpdateChecker.UpdateCheckResult.CheckSkipped(nextCheckTime)
    }

    /**
     * Configure to return check failed
     */
    fun setCheckFailed(error: String, exception: Exception? = null) {
        updateCheckResult = UpdateChecker.UpdateCheckResult.CheckFailed(error, exception)
    }

    /**
     * Reset test state
     */
    fun reset() {
        updateCheckResult = UpdateChecker.UpdateCheckResult.NoUpdateNeeded
        checkForUpdateCalled = false
        lastForceCheck = null
        lastUseDiff = null
        saveDownloadMetadataCalled = false
    }
}
