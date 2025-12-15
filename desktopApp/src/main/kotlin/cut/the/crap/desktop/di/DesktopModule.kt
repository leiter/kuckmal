package cut.the.crap.desktop.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import cut.the.crap.desktop.download.DesktopDownloadManager
import cut.the.crap.desktop.player.DesktopVideoPlayer
import cut.the.crap.desktop.repository.DesktopMediaRepository
import cut.the.crap.desktop.util.MediaUrlUtils
import cut.the.crap.shared.database.AppDatabase
import cut.the.crap.shared.database.MediaDao
import cut.the.crap.shared.database.MediaEntry
import cut.the.crap.shared.database.getDatabaseBuilder
import cut.the.crap.shared.model.Broadcaster
import cut.the.crap.shared.repository.MediaRepository
import cut.the.crap.shared.ui.Channel
import cut.the.crap.shared.viewmodel.SharedViewModel
import org.koin.dsl.module

/**
 * Callback for download requests - set by UI to handle downloads with progress
 */
var downloadCallback: ((MediaEntry, String, String) -> Unit)? = null

/**
 * Callback for showing messages to user
 */
var showMessageCallback: ((String) -> Unit)? = null

/**
 * Koin DI module for desktop application
 */
val desktopModule = module {
    // Database
    single<AppDatabase> {
        getDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(kotlinx.coroutines.Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    single<MediaDao> {
        get<AppDatabase>().mediaDao()
    }

    // Repository
    single<MediaRepository> {
        DesktopMediaRepository(get())
    }

    // Download manager
    single { DesktopDownloadManager() }

    // ViewModel with platform callbacks
    factory {
        SharedViewModel(
            repository = get(),
            onPlayVideo = { entry, isHighQuality ->
                // Resolve the URL from pipe-delimited format
                val targetUrl = if (isHighQuality && entry.hdUrl.isNotEmpty()) {
                    entry.hdUrl
                } else if (!isHighQuality && entry.smallUrl.isNotEmpty()) {
                    entry.smallUrl
                } else {
                    entry.url
                }
                val resolvedUrl = MediaUrlUtils.resolveUrl(entry.url, targetUrl)
                println("Playing: $resolvedUrl")

                // Play video using VLC/MPV/system player
                val result = DesktopVideoPlayer.play(resolvedUrl, entry.title)
                showMessageCallback?.invoke(result.message)
            },
            onDownloadVideo = { entry, url, quality ->
                // Resolve the URL from pipe-delimited format
                val resolvedUrl = MediaUrlUtils.resolveUrl(entry.url, url)
                println("Downloading: $resolvedUrl")

                // Trigger download via UI callback with resolved URL
                downloadCallback?.invoke(entry, resolvedUrl, quality)
            },
            onShowToast = { message ->
                showMessageCallback?.invoke(message)
            },
            getFilesPath = {
                getAppDataPath()
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
 * Get app data directory path
 */
private fun getAppDataPath(): String {
    val os = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")

    return when {
        os.contains("win") -> {
            val appData = System.getenv("APPDATA") ?: "$userHome\\AppData\\Roaming"
            "$appData\\Kuckmal\\"
        }
        os.contains("mac") -> {
            "$userHome/Library/Application Support/Kuckmal/"
        }
        else -> {
            val xdgDataHome = System.getenv("XDG_DATA_HOME") ?: "$userHome/.local/share"
            "$xdgDataHome/Kuckmal/"
        }
    }
}

