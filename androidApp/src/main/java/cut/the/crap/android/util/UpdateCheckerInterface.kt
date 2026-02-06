package cut.the.crap.android.util

/**
 * Interface for update checker to enable testing
 */
interface UpdateCheckerInterface {
    fun checkForUpdate(forceCheck: Boolean = false, useDiff: Boolean = false): UpdateChecker.UpdateCheckResult
    fun saveDownloadMetadata(lastModified: String?, etag: String?, contentLength: Long)
    fun getNextCheckDescription(): String
}
