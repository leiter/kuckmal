package cut.the.crap.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cut.the.crap.shared.di.KoinHelper
import cut.the.crap.shared.di.downloadProgressCallback
import cut.the.crap.shared.di.showMessageCallback
import cut.the.crap.shared.ui.Channel
import cut.the.crap.shared.ui.screens.BrowseView
import cut.the.crap.shared.ui.screens.DetailView
import cut.the.crap.shared.ui.toMediaItem
import cut.the.crap.shared.data.FileSystem
import cut.the.crap.shared.viewmodel.DownloadState
import cut.the.crap.shared.viewmodel.LoadingState
import cut.the.crap.shared.viewmodel.SharedViewModel
import cut.the.crap.shared.viewmodel.ViewState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Main app content for iOS
 * Full implementation with BrowseView, DetailView, and navigation
 */
@Composable
fun AppContent() {
    // Get SharedViewModel from Koin
    val viewModel: SharedViewModel = remember { KoinHelper().getSharedViewModel() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Observe ViewModel state
    val viewState by viewModel.viewState.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()
    val contentList by viewModel.contentList.collectAsState()

    // Database state
    var dbCount by remember { mutableStateOf(0) }

    // UI state
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var selectedTitle by remember { mutableStateOf<String?>(null) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Search state
    var searchResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Download state
    val downloadState by viewModel.downloadState.collectAsState()

    // Video download progress state (title, percentage)
    var videoDownloadProgress by remember { mutableStateOf<Pair<String, Int>?>(null) }

    // Set up message callback
    LaunchedEffect(Unit) {
        showMessageCallback = { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            }
        }

        // Set up download progress callback
        downloadProgressCallback = { title, progress ->
            videoDownloadProgress = if (title != null && progress >= 0) {
                title to progress
            } else {
                null
            }
        }
    }

    // Check database on startup
    LaunchedEffect(Unit) {
        dbCount = viewModel.repository.getCount()
    }

    // Convert content list to titles for BrowseView
    val titles = remember(contentList, viewState) {
        contentList.map { entry ->
            if (viewState.theme != null) entry.title else entry.theme
        }.distinct()
    }

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // Video download progress bar
            videoDownloadProgress?.let { (title, progress) ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Downloading: $title",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "$progress%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
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
                dbCount == 0 -> {
                    // No data - show download screen
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "Kuckmal",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            when (val state = downloadState) {
                                is DownloadState.Idle -> {
                                    Text(
                                        text = "No film list found",
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Download the film list to browse available media.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(32.dp))
                                    Button(
                                        onClick = {
                                            val docsDir = FileSystem.getDocumentsDirectory()
                                            viewModel.downloadAndImportFilmList(docsDir)
                                        },
                                        modifier = Modifier.height(56.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Download Film List")
                                    }
                                }

                                is DownloadState.Downloading -> {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Downloading: ${state.progressPercent}%",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { state.progressPercent / 100f },
                                        modifier = Modifier.width(200.dp)
                                    )
                                }

                                is DownloadState.Decompressing -> {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Decompressing...",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }

                                is DownloadState.Parsing -> {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Importing entries...",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${state.entriesCount} entries imported",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                is DownloadState.Complete -> {
                                    // Refresh dbCount to show browse view
                                    LaunchedEffect(Unit) {
                                        dbCount = viewModel.repository.getCount()
                                        viewModel.resetDownloadState()
                                    }
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Import complete!",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }

                                is DownloadState.Error -> {
                                    Text(
                                        text = "Download failed",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(32.dp))
                                    Button(
                                        onClick = {
                                            viewModel.resetDownloadState()
                                        }
                                    ) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                    }
                }
                viewState is ViewState.Detail -> {
                    // Detail view with back button
                    val detail = viewState as ViewState.Detail
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Back button bar with safe area padding
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.statusBars)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    // Navigate back to themes/titles
                                    selectedTitle = null
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
                        showBackButton = viewState.theme != null || selectedChannel != null,
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
                        onBackClick = {
                            if (viewState.theme != null) {
                                // Navigate back from titles to themes (keep channel selection)
                                viewModel.navigateToThemes(
                                    channel = selectedChannel?.name,
                                    theme = null
                                )
                                selectedTitle = null
                            } else if (selectedChannel != null) {
                                // Unselect channel, go back to "Alle Themen"
                                selectedChannel = null
                                viewModel.navigateToThemes(
                                    channel = null,
                                    theme = null
                                )
                                selectedTitle = null
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
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    "Time period filter coming soon",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        onCheckUpdateClick = {
                            // Start download (will replace existing data)
                            val docsDir = FileSystem.getDocumentsDirectory()
                            viewModel.downloadAndImportFilmList(docsDir)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    "Checking for updates...",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        onReinstallClick = {
                            // Re-download the film list
                            val docsDir = FileSystem.getDocumentsDirectory()
                            viewModel.downloadAndImportFilmList(docsDir)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    "Re-downloading film list...",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
