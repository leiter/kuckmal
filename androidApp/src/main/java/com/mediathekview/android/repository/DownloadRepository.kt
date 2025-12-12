package com.mediathekview.android.repository

import android.content.Context
import android.util.Log
import com.mediathekview.android.service.DownloadService
import com.mediathekview.android.util.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing file downloads
 * Provides a clean API with reactive StateFlow for download status
 * Wraps DownloadService and eliminates direct Activity/ViewModel dependencies
 */
class DownloadRepository(
    private val context: Context,
    private val downloadService: DownloadService
) {
    companion object {
        private const val TAG = "DownloadRepository"
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    init {
        // Set up callback from DownloadService
        downloadService.setDownloadCallback(object : DownloadService.DownloadCallback {
            override fun onDownloadProgress(progress: Int, downloadedMB: Int, totalMB: Int, speed: String) {
                _downloadState.value = DownloadState.Downloading(progress, downloadedMB, totalMB, speed)
            }

            override fun onDownloadSuccess(filename: String, shouldExtract: Boolean) {
                _downloadState.value = DownloadState.Success(filename, shouldExtract)
            }

            override fun onDownloadError(message: String, canRetry: Boolean, retryAction: (() -> Unit)?) {
                _downloadState.value = DownloadState.Error(message, canRetry, retryAction)
            }

        })
    }

    /**
     * Start downloading a file
     * @param url URL to download from
     * @param filename Local filename to save to
     * @return true if download started, false if network unavailable
     */
    fun startDownload(url: String, filename: String): Boolean {
        // Check network connectivity
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.w(TAG, "No network connection available")
            _downloadState.value = DownloadState.Error(
                message = "No internet connection",
                canRetry = true,
                retryAction = { startDownload(url, filename) }
            )
            return false
        }

        Log.d(TAG, "Starting download: $url -> $filename")
        downloadService.addDownload(url, filename)
        return true
    }

    /**
     * Reset download state to idle
     */
    fun resetState() {
        _downloadState.value = DownloadState.Idle
    }
}
