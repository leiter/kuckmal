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
import cut.the.crap.shared.di.showMessageCallback
import cut.the.crap.shared.ui.Channel
import cut.the.crap.shared.ui.screens.BrowseView
import cut.the.crap.shared.ui.screens.DetailView
import cut.the.crap.shared.ui.toMediaItem
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

    // Set up message callback
    LaunchedEffect(Unit) {
        showMessageCallback = { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    // No data - show info screen
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
                            Text(
                                text = "No film list found",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "The film list needs to be downloaded.\nThis feature is coming soon for iOS.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Film list download coming soon",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                },
                                modifier = Modifier.height(56.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download Film List")
                            }
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
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    "Time period filter coming soon",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        onCheckUpdateClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    "Update check coming soon",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        onReinstallClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    "Reload film list coming soon",
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
