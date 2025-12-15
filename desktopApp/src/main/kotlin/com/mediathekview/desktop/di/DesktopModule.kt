package com.mediathekview.desktop.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.mediathekview.desktop.download.DesktopDownloadManager
import com.mediathekview.desktop.player.DesktopVideoPlayer
import com.mediathekview.desktop.repository.DesktopMediaRepository
import com.mediathekview.desktop.util.MediaUrlUtils
import com.mediathekview.shared.database.AppDatabase
import com.mediathekview.shared.database.MediaDao
import com.mediathekview.shared.database.MediaEntry
import com.mediathekview.shared.database.getDatabaseBuilder
import com.mediathekview.shared.model.Broadcaster
import com.mediathekview.shared.repository.MediaRepository
import com.mediathekview.shared.ui.Channel
import com.mediathekview.shared.viewmodel.SharedViewModel
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
            "$appData\\MediathekView\\"
        }
        os.contains("mac") -> {
            "$userHome/Library/Application Support/MediathekView/"
        }
        else -> {
            val xdgDataHome = System.getenv("XDG_DATA_HOME") ?: "$userHome/.local/share"
            "$xdgDataHome/mediathekview/"
        }
    }
}

