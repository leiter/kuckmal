package cut.the.crap.android.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for download repository to enable testing
 */
interface DownloadRepositoryInterface {
    val downloadState: StateFlow<DownloadState>
    fun startDownload(url: String, filename: String): Boolean
    fun resetState()
}
