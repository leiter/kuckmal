package com.mediathekview.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.mediathekview.desktop.data.FilmListDownloader
import com.mediathekview.desktop.di.desktopModule
import com.mediathekview.desktop.di.downloadCallback
import com.mediathekview.desktop.di.showMessageCallback
import com.mediathekview.desktop.download.DesktopDownloadManager
import com.mediathekview.shared.database.MediaEntry
import com.mediathekview.shared.repository.MediaRepository
import com.mediathekview.shared.ui.Channel
import com.mediathekview.shared.ui.screens.BrowseView
import com.mediathekview.shared.ui.screens.DetailView
import com.mediathekview.shared.ui.theme.MediathekViewTheme
import com.mediathekview.shared.ui.toMediaItem
import com.mediathekview.shared.viewmodel.LoadingState
import com.mediathekview.shared.viewmodel.SharedViewModel
import com.mediathekview.shared.viewmodel.ViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.java.KoinJavaComponent.get

fun main() = application {
    // Initialize Koin
    DisposableEffect(Unit) {
        startKoin {
            modules(desktopModule)
        }
        onDispose {
            stopKoin()
        }
    }

    val windowState = rememberWindowState(
        size = DpSize(1400.dp, 900.dp)
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "MediathekView"
    ) {
        MediathekViewTheme {
            DesktopApp()
        }
    }
}

@Composable
fun DesktopApp() {
    val viewModel: SharedViewModel = remember { get(SharedViewModel::class.java) }
    val repository: MediaRepository = remember { get(MediaRepository::class.java) }
    val downloadManager: DesktopDownloadManager = remember { get(DesktopDownloadManager::class.java) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val viewState by viewModel.viewState.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()
    val contentList by viewModel.contentList.collectAsState()

    // Film list download state
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf("") }
    var dbCount by remember { mutableStateOf(0) }

    // Video download state
    var videoDownloadEntry by remember { mutableStateOf<MediaEntry?>(null) }
    var videoDownloadUrl by remember { mutableStateOf("") }
    var videoDownloadQuality by remember { mutableStateOf("") }
    var videoDownloadProgress by remember { mutableStateOf(0f) }
    var videoDownloadSpeed by remember { mutableStateOf("") }
    var isVideoDownloading by remember { mutableStateOf(false) }

    // Set up callbacks for ViewModel
    LaunchedEffect(Unit) {
        // Message callback for snackbar
        showMessageCallback = { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            }
        }

        // Download callback
        downloadCallback = { entry, url, quality ->
            videoDownloadEntry = entry
            videoDownloadUrl = url
            videoDownloadQuality = quality
            isVideoDownloading = true
            videoDownloadProgress = 0f
            videoDownloadSpeed = ""

            coroutineScope.launch(Dispatchers.IO) {
                downloadManager.download(url, entry.title, entry.channel, quality).collect { state ->
                    when (state) {
                        is DesktopDownloadManager.DownloadState.Starting -> {
                            videoDownloadSpeed = "Starting..."
                        }
                        is DesktopDownloadManager.DownloadState.Progress -> {
                            val progress = if (state.totalBytes > 0) {
                                state.bytesDownloaded.toFloat() / state.totalBytes.toFloat()
                            } else 0f
                            videoDownloadProgress = progress
                            val speedMb = state.speedBytesPerSec / 1024.0 / 1024.0
                            val downloadedMb = state.bytesDownloaded / 1024.0 / 1024.0
                            val totalMb = state.totalBytes / 1024.0 / 1024.0
                            videoDownloadSpeed = "%.1f MB / %.1f MB (%.1f MB/s)".format(downloadedMb, totalMb, speedMb)
                        }
                        is DesktopDownloadManager.DownloadState.Complete -> {
                            isVideoDownloading = false
                            snackbarHostState.showSnackbar(
                                "Downloaded: ${state.file.name}",
                                duration = SnackbarDuration.Long
                            )
                        }
                        is DesktopDownloadManager.DownloadState.Error -> {
                            isVideoDownloading = false
                            snackbarHostState.showSnackbar(
                                "Download failed: ${state.message}",
                                duration = SnackbarDuration.Long
                            )
                        }
                    }
                }
            }
        }
    }

    // Check database on startup
    LaunchedEffect(Unit) {
        dbCount = repository.getCount()
    }

    // Convert content list to titles for BrowseView
    val titles = remember(contentList) {
        contentList.map { entry ->
            if (viewState.theme != null) entry.title else entry.theme
        }.distinct()
    }

    // Track selected channel
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var selectedTitle by remember { mutableStateOf<String?>(null) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Search results
    var searchResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Perform search when query changes
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            isSearching = true
            viewModel.searchContentFlow(searchQuery, viewState.theme != null)
                .collectLatest { results ->
                    searchResults = results.take(100)
                    isSearching = false
                }
        } else {
            searchResults = emptyList()
            isSearching = false
        }
    }

    // Download and load film list
    fun downloadAndLoad() {
        isDownloading = true
        downloadProgress = "Starting download..."

        coroutineScope.launch(Dispatchers.IO) {
            val downloader = FilmListDownloader()
            val dataDir = getAppDataPath()

            downloader.downloadFilmList(dataDir, object : FilmListDownloader.DownloadCallback {
                override fun onProgress(bytesDownloaded: Long, totalBytes: Long) {
                    val mb = bytesDownloaded / 1024 / 1024
                    val totalMb = if (totalBytes > 0) totalBytes / 1024 / 1024 else 0
                    downloadProgress = "Downloading: $mb MB / $totalMb MB"
                }

                override fun onDecompressing() {
                    downloadProgress = "Decompressing..."
                }

                override fun onComplete(filePath: String) {
                    downloadProgress = "Parsing film list..."

                    coroutineScope.launch(Dispatchers.IO) {
                        repository.loadMediaListFromFile(filePath).collect { result ->
                            when (result) {
                                is MediaRepository.LoadingResult.Progress -> {
                                    downloadProgress = "Loading: ${result.entriesLoaded} entries..."
                                }
                                is MediaRepository.LoadingResult.Complete -> {
                                    downloadProgress = "Complete! ${result.totalEntries} entries loaded"
                                    dbCount = result.totalEntries
                                    isDownloading = false
                                    // Trigger refresh
                                    viewModel.navigateToThemes(null, null)
                                }
                                is MediaRepository.LoadingResult.Error -> {
                                    downloadProgress = "Error: ${result.exception.message}"
                                    isDownloading = false
                                }
                            }
                        }
                    }
                }

                override fun onError(error: Exception) {
                    downloadProgress = "Error: ${error.message}"
                    isDownloading = false
                }
            })
        }
    }

    // Download progress dialog
    if (isVideoDownloading) {
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss while downloading */ },
            title = { Text("Downloading Video") },
            text = {
                Column {
                    Text(
                        text = videoDownloadEntry?.title ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { videoDownloadProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = videoDownloadSpeed,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                // Could add cancel button here
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                isDownloading -> {
                // Download/Loading screen
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(downloadProgress)
                    }
                }
            }
            dbCount == 0 -> {
                // No data - show download prompt
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "MediathekView",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "No film list found",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Download the current film list (~150 MB) to browse content",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { downloadAndLoad() },
                            modifier = Modifier.height(56.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download Film List")
                        }
                    }
                }
            }
            loadingState == LoadingState.LOADING -> {
                // Loading screen
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading media database...")
                    }
                }
            }
            loadingState == LoadingState.ERROR -> {
                // Error screen
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error loading data",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(viewModel.errorMessage.collectAsState().value)
                    }
                }
            }
            viewState is ViewState.Detail -> {
                // Detail view with back button
                val detail = viewState as ViewState.Detail
                Column(modifier = Modifier.fillMaxSize()) {
                    // Back button bar
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                // Navigate back to themes/titles
                                viewModel.navigateToThemes(
                                    channel = detail.navigationChannel,
                                    theme = detail.navigationTheme
                                )
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = detail.mediaEntry.theme,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                        }
                    }

                    // Detail content
                    DetailView(
                        mediaItem = detail.mediaEntry.toMediaItem(),
                        onPlayClick = { isHighQuality ->
                            viewModel.playVideo(detail.mediaEntry, isHighQuality)
                        },
                        onDownloadClick = { isHighQuality ->
                            val url = if (isHighQuality) {
                                detail.mediaEntry.hdUrl.ifEmpty { detail.mediaEntry.url }
                            } else {
                                detail.mediaEntry.smallUrl.ifEmpty { detail.mediaEntry.url }
                            }
                            viewModel.downloadVideo(detail.mediaEntry, url, if (isHighQuality) "HD" else "SD")
                        }
                    )
                }
            }
            else -> {
                // Browse view
                val displayTitles = if (searchQuery.isNotEmpty() && searchResults.isNotEmpty()) {
                    searchResults
                } else {
                    titles
                }

                BrowseView(
                    channels = viewModel.channels,
                    titles = displayTitles,
                    selectedChannel = selectedChannel,
                    selectedTitle = selectedTitle,
                    currentTheme = when {
                        viewState.theme != null -> viewState.theme!!
                        viewState.channel != null -> "Themen (${viewState.channel})"
                        else -> "Alle Themen"
                    },
                    isShowingTitles = viewState.theme != null,
                    hasMoreItems = contentList.size >= 1200,
                    searchQuery = searchQuery,
                    isSearching = isSearching,
                    isSearchVisible = isSearchVisible,
                    onSearchQueryChanged = { query ->
                        searchQuery = query
                    },
                    onSearchVisibilityChanged = { visible ->
                        isSearchVisible = visible
                        if (!visible) {
                            searchQuery = ""
                            searchResults = emptyList()
                        }
                    },
                    onChannelSelected = { channel ->
                        selectedChannel = if (selectedChannel == channel) null else channel
                        viewModel.navigateToThemes(
                            channel = selectedChannel?.name,
                            theme = null
                        )
                        selectedTitle = null
                    },
                    onTitleSelected = { title ->
                        selectedTitle = title
                        if (viewState.theme != null) {
                            // Selecting a title within a theme -> go to detail
                            viewModel.navigateToDetail(title)
                        } else {
                            // Selecting a theme -> show titles
                            viewModel.navigateToThemes(
                                channel = selectedChannel?.name,
                                theme = title
                            )
                        }
                    },
                    onLoadMore = {
                        viewModel.nextPart()
                    },
                    onTimePeriodClick = {
                        // TODO: Show time period dialog
                        println("Time period clicked")
                    },
                    onCheckUpdateClick = {
                        // TODO: Check for updates
                        println("Check update clicked")
                    },
                    onReinstallClick = {
                        // Reload film list
                        downloadAndLoad()
                    }
                )
            }
            }
        }
    }
}

/**
 * Get app data directory path for the current OS
 */
private fun getAppDataPath(): String {
    val os = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")

    val path = when {
        os.contains("win") -> {
            val appData = System.getenv("APPDATA") ?: "$userHome\\AppData\\Roaming"
            "$appData\\MediathekView"
        }
        os.contains("mac") -> {
            "$userHome/Library/Application Support/MediathekView"
        }
        else -> {
            val xdgDataHome = System.getenv("XDG_DATA_HOME") ?: "$userHome/.local/share"
            "$xdgDataHome/mediathekview"
        }
    }

    // Ensure directory exists
    java.io.File(path).mkdirs()
    return path
}
