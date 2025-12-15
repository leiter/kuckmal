package com.mediathekview.android.repository

/**
 * Represents the state of a download operation
 * Used with StateFlow for reactive download status updates
 */
sealed class DownloadState {
    /**
     * No download in progress
     */
    object Idle : DownloadState()

    /**
     * Download is in progress
     * @param progress Download progress (0-100)
     * @param downloadedMB Amount downloaded in MB
     * @param totalMB Total file size in MB
     * @param speed Current download speed
     */
    data class Downloading(
        val progress: Int,
        val downloadedMB: Int,
        val totalMB: Int,
        val speed: String
    ) : DownloadState()

    /**
     * Download completed successfully
     * @param filename Path to the downloaded file
     * @param shouldExtract Whether the file needs extraction (e.g., .xz files)
     */
    data class Success(
        val filename: String,
        val shouldExtract: Boolean
    ) : DownloadState()

    /**
     * Download failed with an error
     * @param message Error message
     * @param canRetry Whether the download can be retried
     * @param retryAction Optional retry action
     */
    data class Error(
        val message: String,
        val canRetry: Boolean,
        val retryAction: (() -> Unit)? = null
    ) : DownloadState()

    /**
     * Extracting downloaded file
     * @param progress Extraction progress (0-100)
     */
    data class Extracting(
        val progress: Int
    ) : DownloadState()

    /**
     * File extraction completed successfully
     * @param extractedFile Path to the extracted file
     */
    data class ExtractionComplete(
        val extractedFile: String
    ) : DownloadState()
}
