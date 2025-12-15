package cut.the.crap.android.di

import android.app.DownloadManager
import android.content.Context
import android.widget.Toast
import androidx.core.net.toUri
import cut.the.crap.android.compose.data.ComposeDataMapper
import cut.the.crap.android.data.MediaListParser
import cut.the.crap.android.data.MediaViewModel
import cut.the.crap.android.repository.DownloadRepository
import cut.the.crap.android.repository.MediaRepository
import cut.the.crap.android.repository.MediaRepositoryImpl
import cut.the.crap.android.service.DownloadService
import cut.the.crap.android.util.UpdateChecker
import cut.the.crap.android.video.VideoPlayerManager
import cut.the.crap.android.video.createVideoPlayerManager
import cut.the.crap.shared.database.AppDatabase
import cut.the.crap.shared.database.MediaEntry
import cut.the.crap.shared.database.getDatabaseBuilder
import cut.the.crap.shared.database.getRoomDatabase
import cut.the.crap.shared.viewmodel.SharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import cut.the.crap.shared.repository.MediaRepository as SharedMediaRepository

/**
 * Koin dependency injection module
 * Defines how to create and provide dependencies throughout the app
 */
val appModule = module {

    // Database - Singleton (using shared KMP Room)
    single {
        getRoomDatabase(
            getDatabaseBuilder(androidApplication())
        )
    }

    // DAO - Singleton (provided by database)
    single { get<AppDatabase>().mediaDao() }

    // Parser - Factory (create new instance each time)
    factory { MediaListParser() }

    // MediaRepository - Singleton
    single<MediaRepository> {
        MediaRepositoryImpl(
            mediaDao = get(),
            parser = get()
        )
    }

    // UpdateChecker - Singleton
    single {
        UpdateChecker(androidContext())
    }

    // DownloadService - Singleton (refactored with no Activity/ViewModel deps)
    single {
        DownloadService(
            context = androidContext(),
            updateChecker = get()
        )
    }

    // DownloadRepository - Singleton
    single {
        DownloadRepository(
            context = androidContext(),
            downloadService = get()
        )
    }

    // VideoPlayerManager - Singleton
    single {
        androidApplication().createVideoPlayerManager()
    }

    // Coroutine scope for callbacks - Singleton
    single {
        CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    // SharedViewModel (KMP) - scoped to Activity/Fragment lifecycle
    // Uses callbacks for Android-specific operations
    viewModel {
        val context = androidContext()
        val videoPlayerManager: VideoPlayerManager = get()
        val callbackScope: CoroutineScope = get()

        SharedViewModel(
            repository = get<MediaRepository>() as SharedMediaRepository,
            onPlayVideo = { entry: MediaEntry, isHighQuality: Boolean ->
                callbackScope.launch {
                    videoPlayerManager.playVideo(entry, isHighQuality)
                }
            },
            onDownloadVideo = { entry: MediaEntry, url: String, quality: String ->
                downloadVideo(context, entry, url, quality)
            },
            onShowToast = { message: String ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
            getFilesPath = {
                context.filesDir.absolutePath + "/"
            },
            getAllChannels = {
                ComposeDataMapper.getAllChannels()
            }
        )
    }

    // MediaViewModel - scoped to Activity/Fragment lifecycle (legacy XML UI)
    viewModel {
        MediaViewModel(
            application = androidApplication(),
            repository = get(),
            downloadRepository = get(),
            updateChecker = get()
        )
    }
}

/**
 * Helper function to download a video using Android's DownloadManager
 */
private fun downloadVideo(context: Context, entry: MediaEntry, url: String, quality: String) {
    if (url.isEmpty()) {
        Toast.makeText(context, "No video URL available", Toast.LENGTH_SHORT).show()
        return
    }

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
    if (downloadManager == null) {
        Toast.makeText(context, "Download manager unavailable", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        // Sanitize filename
        val sanitizedTitle = entry.title
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(100)

        // Determine file extension
        val fileExtension = when {
            url.contains(".mp4", ignoreCase = true) -> ".mp4"
            url.contains(".m3u8", ignoreCase = true) -> ".m3u8"
            url.contains(".webm", ignoreCase = true) -> ".webm"
            else -> ".mp4"
        }

        val request = DownloadManager.Request(url.toUri()).apply {
            setTitle(entry.title)
            setDescription("${entry.channel} - ${entry.theme}")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(
                android.os.Environment.DIRECTORY_DOWNLOADS,
                "Kuckmal/${entry.channel}/${sanitizedTitle}${fileExtension}"
            )
            setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or
                    DownloadManager.Request.NETWORK_MOBILE
            )
        }

        val downloadId = downloadManager.enqueue(request)
        Toast.makeText(
            context,
            "Downloading: ${entry.title}\nQuality: $quality",
            Toast.LENGTH_LONG
        ).show()

    } catch (e: Exception) {
        Toast.makeText(context, "Error starting download: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
