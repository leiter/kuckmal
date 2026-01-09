@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package cut.the.crap.shared.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import cut.the.crap.shared.database.AppDatabase
import cut.the.crap.shared.database.MediaDao
import cut.the.crap.shared.database.MediaEntry
import cut.the.crap.shared.database.getDatabaseBuilder
import cut.the.crap.shared.data.VideoDownloader
import cut.the.crap.shared.model.Broadcaster
import cut.the.crap.shared.repository.IosMediaRepository
import cut.the.crap.shared.repository.MediaRepository
import cut.the.crap.shared.ui.Channel
import cut.the.crap.shared.viewmodel.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * Callback for download requests - set by UI to handle downloads with progress
 */
var downloadCallback: ((MediaEntry, String, String) -> Unit)? = null

/**
 * Callback for showing messages to user
 */
var showMessageCallback: ((String) -> Unit)? = null

/**
 * Callback for download progress updates (title, progress percentage)
 * Set to (null, -1) to clear the progress bar
 */
var downloadProgressCallback: ((String?, Int) -> Unit)? = null

/**
 * Initialize video downloader callbacks.
 * Call this during app initialization.
 */
fun initializeVideoDownloader() {
    var lastProgress = -1

    // Set up progress update callback with throttling
    VideoDownloader.onProgressUpdate = { title, progress ->
        // Only update if progress percentage changed
        if (progress != lastProgress) {
            lastProgress = progress
            downloadProgressCallback?.invoke(title, progress)
        }
    }

    // Set up completion callback
    VideoDownloader.onDownloadComplete = { title, path, success, error ->
        lastProgress = -1
        downloadProgressCallback?.invoke(null, -1)  // Clear progress bar
        if (success) {
            showMessageCallback?.invoke("Download complete: $title\nSaved to Downloads folder")
        } else {
            showMessageCallback?.invoke("Download failed: ${error ?: "Unknown error"}")
        }
    }
}

/**
 * Koin DI module for iOS application
 */
val iosModule = module {
    // Database
    single<AppDatabase> {
        getDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    single<MediaDao> {
        get<AppDatabase>().mediaDao()
    }

    // Repository
    single<MediaRepository> {
        IosMediaRepository(get())
    }

    // ViewModel with platform callbacks
    factory {
        SharedViewModel(
            repository = get(),
            onPlayVideo = { entry, isHighQuality ->
                // Get the appropriate URL based on quality
                val targetUrl = when {
                    isHighQuality && entry.hdUrl.isNotEmpty() -> entry.hdUrl
                    !isHighQuality && entry.smallUrl.isNotEmpty() -> entry.smallUrl
                    else -> entry.url
                }

                // Resolve pipe-delimited URL format if needed
                val resolvedUrl = resolveUrl(entry.url, targetUrl)
                println("Playing: $resolvedUrl")

                if (resolvedUrl.isEmpty()) {
                    showMessageCallback?.invoke("No video URL available")
                    return@SharedViewModel
                }

                // Open URL in system player using modern API
                NSURL.URLWithString(resolvedUrl)?.let { url ->
                    UIApplication.sharedApplication.openURL(
                        url,
                        options = emptyMap<Any?, Any>(),
                        completionHandler = { success ->
                            if (!success) {
                                showMessageCallback?.invoke("Could not open video player")
                            }
                        }
                    )
                } ?: run {
                    showMessageCallback?.invoke("Invalid video URL")
                }
            },
            onDownloadVideo = { entry, url, quality ->
                // Resolve pipe-delimited URL format if needed
                val resolvedUrl = resolveUrl(entry.url, url)
                println("Downloading: $resolvedUrl")

                // Show start message
                showMessageCallback?.invoke("Starting download: ${entry.title}")

                // Start the download using VideoDownloader
                VideoDownloader.downloadVideoWithProgress(entry, resolvedUrl, quality)
            },
            onShowToast = { message ->
                showMessageCallback?.invoke(message)
            },
            getFilesPath = {
                getIosDocumentsPath()
            },
            getAllChannels = {
                Broadcaster.channelList.map { broadcaster ->
                    Channel(
                        name = broadcaster.name,
                        displayName = broadcaster.abbreviation.ifEmpty { broadcaster.name }
                    )
                }
            }
        )
    }
}

/**
 * Resolve pipe-delimited URL format used by media entries
 * The format is: baseUrl|offset|suffix where the actual URL is baseUrl + suffix
 */
private fun resolveUrl(baseUrl: String, targetUrl: String): String {
    // If target URL contains pipe delimiter, resolve it
    if (targetUrl.contains("|")) {
        val parts = targetUrl.split("|")
        if (parts.size >= 2) {
            val offset = parts[0].toIntOrNull() ?: 0
            val suffix = parts[1]
            // Take first 'offset' chars from base URL and append suffix
            return if (offset > 0 && offset <= baseUrl.length) {
                baseUrl.substring(0, offset) + suffix
            } else {
                targetUrl
            }
        }
    }

    // If target is empty, use base URL
    if (targetUrl.isEmpty()) {
        return baseUrl
    }

    // Return target URL as-is if it's a complete URL
    return targetUrl
}

/**
 * Get iOS documents directory path
 */
private fun getIosDocumentsPath(): String {
    return platform.Foundation.NSFileManager.defaultManager.URLForDirectory(
        directory = platform.Foundation.NSDocumentDirectory,
        inDomain = platform.Foundation.NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )?.path ?: ""
}
