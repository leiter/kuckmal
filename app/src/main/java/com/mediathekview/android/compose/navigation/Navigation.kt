package com.mediathekview.android.compose.navigation

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mediathekview.android.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mediathekview.android.compose.data.ComposeDataMapper
import com.mediathekview.android.compose.models.ComposeViewModel
import com.mediathekview.android.compose.screens.BrowseView
import com.mediathekview.android.compose.screens.DetailView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

/**
 * Navigation routes - simplified to just two screens
 */
sealed class Screen(val route: String) {
    // Overview screen (handles all browsing: themes, channels, titles)
    object Overview : Screen("overview")

    // Detail screen
    data class Detail(
        val title: String,
        val channel: String? = null,
        val theme: String? = null
    ) : Screen("detail/{title}?channel={channel}&theme={theme}") {
        companion object {
            fun createRoute(
                title: String,
                channel: String? = null,
                theme: String? = null
            ): String {
                val encodedTitle = Uri.encode(title)
                val encodedChannel = channel?.let { Uri.encode(it) } ?: ""
                val encodedTheme = theme?.let { Uri.encode(it) } ?: ""
                return "detail/$encodedTitle?channel=$encodedChannel&theme=$encodedTheme"
            }
        }
    }
}

/**
 * Overview screen state - tracks what data is being displayed
 */
sealed class OverviewState {
    // Showing all themes (no channel filter)
    object AllThemes : OverviewState()

    // Showing themes for a specific channel
    data class ChannelThemes(val channelName: String) : OverviewState()

    // Showing titles within a theme
    data class ThemeTitles(val channelName: String?, val themeName: String) : OverviewState()
}

/**
 * Animation specifications for navigation transitions
 */
object NavigationAnimations {
    const val ANIMATION_DURATION = 300

    // Forward navigation (going to detail)
    val slideInFromRight = slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(ANIMATION_DURATION)
    ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))

    val slideOutToLeft = slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth / 3 },
        animationSpec = tween(ANIMATION_DURATION)
    ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))

    // Backward navigation (returning from detail)
    val slideInFromLeft = slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth / 3 },
        animationSpec = tween(ANIMATION_DURATION)
    ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))

    val slideOutToRight = slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(ANIMATION_DURATION)
    ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))

    // Fade transitions
    val fadeIn = fadeIn(animationSpec = tween(ANIMATION_DURATION))
    val fadeOut = fadeOut(animationSpec = tween(ANIMATION_DURATION))
}

/**
 * Main navigation host - simplified to two destinations
 */
@Composable
fun MediathekViewNavHost(
    navController: NavHostController,
    viewModel: ComposeViewModel,
) {
    // Use ViewModel directly - no wrapper needed
    val isDataLoaded by viewModel.isDataLoadedFlow.collectAsStateWithLifecycle()
    val loadingState by viewModel.loadingState.collectAsStateWithLifecycle()

    // Show loading indicator if data is not yet loaded
    if (!isDataLoaded && loadingState != ComposeViewModel.LoadingState.ERROR) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Show error state if loading failed
    if (loadingState == ComposeViewModel.LoadingState.ERROR) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.error_loading_data))
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Overview.route  // Always start with Overview to ensure proper back stack
    ) {
        // Overview screen (manages its own internal state)
        composable(
            route = Screen.Overview.route,
            enterTransition = { NavigationAnimations.fadeIn },
            exitTransition = { NavigationAnimations.slideOutToLeft },
            popEnterTransition = { NavigationAnimations.slideInFromLeft },
            popExitTransition = { NavigationAnimations.fadeOut }
        ) {
            // Overview screen manages its own state - use rememberSaveable for persistence
            // Store state as strings since sealed classes aren't directly saveable
            var stateType by rememberSaveable { mutableStateOf("AllThemes") }
            var stateChannel by rememberSaveable { mutableStateOf<String?>(null) }
            var stateTheme by rememberSaveable { mutableStateOf<String?>(null) }

            // Convert to OverviewState
            val overviewState = when (stateType) {
                "ChannelThemes" -> OverviewState.ChannelThemes(stateChannel ?: "")
                "ThemeTitles" -> OverviewState.ThemeTitles(stateChannel, stateTheme ?: "")
                else -> OverviewState.AllThemes
            }

            // Helper function to update state
            fun updateOverviewState(newState: OverviewState) {
                when (newState) {
                    is OverviewState.AllThemes -> {
                        stateType = "AllThemes"
                        stateChannel = null
                        stateTheme = null
                    }
                    is OverviewState.ChannelThemes -> {
                        stateType = "ChannelThemes"
                        stateChannel = newState.channelName
                        stateTheme = null
                    }
                    is OverviewState.ThemeTitles -> {
                        stateType = "ThemeTitles"
                        stateChannel = newState.channelName
                        stateTheme = newState.themeName
                    }
                }
            }

            // Handle back button press within Overview
            BackHandler(enabled = overviewState !is OverviewState.AllThemes) {
                when (overviewState) {
                    is OverviewState.ChannelThemes -> {
                        updateOverviewState(OverviewState.AllThemes)
                    }
                    is OverviewState.ThemeTitles -> {
                        val state = overviewState
                        if (state.channelName != null) {
                            updateOverviewState(OverviewState.ChannelThemes(state.channelName))
                        } else {
                            updateOverviewState(OverviewState.AllThemes)
                        }
                    }
                    else -> { /* Already at root */ }
                }
            }

            // Pagination state - reset when state changes
            var currentPart by remember { mutableIntStateOf(0) }

            // Search state - persisted across navigation
            var searchQuery by rememberSaveable { mutableStateOf("") }
            var debouncedSearchQuery by remember { mutableStateOf("") }
            var isSearching by remember { mutableStateOf(false) }
            var searchResults by remember { mutableStateOf<List<String>>(emptyList()) }

            // Debounce search query - wait 1200ms after user stops typing
            @OptIn(FlowPreview::class)
            LaunchedEffect(Unit) {
                snapshotFlow { searchQuery }
                    .debounce(1200)
                    .distinctUntilChanged()
                    .collect { query ->
                        debouncedSearchQuery = query
                    }
            }

            // Perform actual search when debounced query changes
            LaunchedEffect(debouncedSearchQuery, overviewState) {
                val query = debouncedSearchQuery.trim()
                if (query.isBlank()) {
                    searchResults = emptyList()
                    isSearching = false
                    return@LaunchedEffect
                }

                isSearching = true
                android.util.Log.d("Navigation", "Performing search for: '$query'")

                try {
                    val results = withContext(Dispatchers.IO) {
                        when (val state = overviewState) {
                            is OverviewState.AllThemes -> {
                                // Search all entries, extract unique themes
                                val entries = viewModel.repository.searchEntries(query, 5000)
                                entries.map { it.theme }.distinct()
                            }
                            is OverviewState.ChannelThemes -> {
                                // Search entries in channel, extract unique themes
                                val entries = viewModel.repository.searchEntriesByChannel(state.channelName, query, 5000)
                                entries.map { it.theme }.distinct()
                            }
                            is OverviewState.ThemeTitles -> {
                                // Search entries in theme, extract unique titles
                                val entries = if (state.channelName != null) {
                                    viewModel.repository.searchEntriesByChannelAndTheme(state.channelName, state.themeName, query, 5000)
                                } else {
                                    viewModel.repository.searchEntriesByTheme(state.themeName, query, 5000)
                                }
                                entries.map { it.title }.distinct()
                            }
                        }
                    }
                    searchResults = results
                    android.util.Log.d("Navigation", "Search found ${results.size} results")
                } catch (e: Exception) {
                    android.util.Log.e("Navigation", "Search error", e)
                    searchResults = emptyList()
                }
                isSearching = false
            }

            // Show loading state when user is typing
            LaunchedEffect(searchQuery) {
                if (searchQuery.isNotBlank() && searchQuery != debouncedSearchQuery) {
                    isSearching = true
                }
            }

            // Selection state - tracks separately for themes and titles
            // This allows preserving selection when navigating back
            var selectedTheme by rememberSaveable { mutableStateOf<String?>(null) }
            var selectedTitle by rememberSaveable { mutableStateOf<String?>(null) }

            // Determine which selection to show based on current state
            val currentSelection = when (overviewState) {
                is OverviewState.AllThemes -> selectedTheme
                is OverviewState.ChannelThemes -> selectedTheme
                is OverviewState.ThemeTitles -> selectedTitle
            }

            // Reset pagination and search when overview state changes (but NOT selection)
            LaunchedEffect(overviewState) {
                currentPart = 0
                searchQuery = "" // Clear search when switching states
                // Don't clear selection - we want to preserve it for back navigation
            }

            // Extract state parameters for reactive data fetching
            val currentChannelName = when (val state = overviewState) {
                is OverviewState.ChannelThemes -> state.channelName
                is OverviewState.ThemeTitles -> state.channelName
                else -> null
            }
            val currentThemeName = when (val state = overviewState) {
                is OverviewState.ThemeTitles -> state.themeName
                else -> null
            }

            // Fetch themes data with pagination (used for AllThemes and ChannelThemes states)
            val themesLimit = (currentPart + 1) * 1200
            val themesFlow = remember(currentChannelName, themesLimit) {
                viewModel.getThemesFlow(currentChannelName, themesLimit)
            }
            val themesData by themesFlow.collectAsStateWithLifecycle(emptyList())

            // Fetch titles data (used for ThemeTitles state)
            val titlesFlow = remember(currentChannelName, currentThemeName) {
                if (currentThemeName != null) {
                    viewModel.getTitlesFlow(currentChannelName, currentThemeName)
                } else {
                    flowOf(emptyList())
                }
            }
            val titlesData by titlesFlow.collectAsStateWithLifecycle(emptyList())

            // Select the appropriate data based on current state
            val rawData = when (overviewState) {
                is OverviewState.AllThemes -> themesData
                is OverviewState.ChannelThemes -> themesData
                is OverviewState.ThemeTitles -> titlesData
            }

            // Use search results when searching, otherwise show raw data
            val displayData = if (debouncedSearchQuery.isBlank()) {
                rawData
            } else {
                searchResults
            }

            // Determine if there are more items to load
            // Show "more" if we have exactly the limit (1200 * (currentPart + 1)) items
            // Don't show more when search is active
            val expectedLimit = (currentPart + 1) * 1200
            val hasMoreItems = if (debouncedSearchQuery.isNotBlank()) {
                false // Don't show "more" during search
            } else {
                when (overviewState) {
                    is OverviewState.AllThemes -> themesData.size >= expectedLimit
                    is OverviewState.ChannelThemes -> themesData.size >= expectedLimit
                    is OverviewState.ThemeTitles -> false // Don't paginate titles
                }
            }

            // Get current context info for display
            val allThemesLabel = stringResource(R.string.all_themes)
            val channelThemesLabel = stringResource(R.string.channel_themes, "")
            val (selectedChannel, currentThemeLabel, showingTitles) = when (val state = overviewState) {
                is OverviewState.AllThemes -> {
                    Triple(null, allThemesLabel, false)
                }
                is OverviewState.ChannelThemes -> {
                    val channel = ComposeDataMapper.findChannelByName(state.channelName)
                    Triple(channel, stringResource(R.string.channel_themes, channel?.displayName ?: state.channelName), false)
                }
                is OverviewState.ThemeTitles -> {
                    val channel = state.channelName?.let { ComposeDataMapper.findChannelByName(it) }
                    Triple(channel, state.themeName, true)
                }
            }

            BrowseView(
                channels = viewModel.channels,
                titles = displayData,
                selectedChannel = selectedChannel,
                selectedTitle = currentSelection, // Pass appropriate selection based on state
                currentTheme = currentThemeLabel,
                isShowingTitles = showingTitles,
                hasMoreItems = hasMoreItems,
                searchQuery = searchQuery,
                isSearching = isSearching,
                onSearchQueryChanged = { query ->
                    searchQuery = query
                    android.util.Log.d("Navigation", "Search query updated: '$query'")
                },
                onTimePeriodClick = { /* TODO: viewModel.showTimePeriodDialog() */ },
                onCheckUpdateClick = { /* TODO: viewModel.checkForUpdatesManually() */ },
                onReinstallClick = { /* TODO: viewModel.showReinstallConfirmation() */ },
                onChannelSelected = { channel ->
                    // Clear theme selection when changing channels
                    selectedTheme = null
                    selectedTitle = null
                    // Change to that channel's themes (internal state change, no navigation)
                    updateOverviewState(OverviewState.ChannelThemes(channel.name))
                },
                onTitleSelected = { item ->
                    when (val state = overviewState) {
                        is OverviewState.AllThemes -> {
                            // Clicked on a theme -> update theme selection and show titles
                            selectedTheme = item
                            selectedTitle = null // Clear title selection for new theme
                            android.util.Log.d("Navigation", "Selected theme: '$item'")
                            updateOverviewState(OverviewState.ThemeTitles(null, item))
                        }
                        is OverviewState.ChannelThemes -> {
                            // Clicked on a theme -> update theme selection and show titles
                            selectedTheme = item
                            selectedTitle = null // Clear title selection for new theme
                            android.util.Log.d("Navigation", "Selected theme: '$item'")
                            updateOverviewState(OverviewState.ThemeTitles(state.channelName, item))
                        }
                        is OverviewState.ThemeTitles -> {
                            // Clicked on a title -> update title selection and navigate to detail
                            selectedTitle = item
                            android.util.Log.d("Navigation", "Selected title: '$item'")
                            // First set the navigation context in ViewModel, then navigate to detail
                            viewModel.navigateToThemes(channel = state.channelName, theme = state.themeName)
                            viewModel.navigateToDetail(item)
                            navController.navigate(
                                Screen.Detail.createRoute(item, state.channelName, state.themeName)
                            )
                        }
                    }
                },
                onLoadMore = {
                    // Load more items by incrementing currentPart
                    currentPart += 1
                    android.util.Log.d("Navigation", "Loading more items, currentPart: $currentPart")
                }
            )
        }

        // Detail screen
        composable(
            route = "detail/{title}?channel={channel}&theme={theme}",
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("channel") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("theme") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            enterTransition = { NavigationAnimations.slideInFromRight },
            exitTransition = { NavigationAnimations.slideOutToLeft },
            popEnterTransition = { NavigationAnimations.slideInFromLeft },
            popExitTransition = { NavigationAnimations.slideOutToRight }
        ) { backStackEntry ->
            val title = Uri.decode(backStackEntry.arguments?.getString("title") ?: "")
            val channel = backStackEntry.arguments?.getString("channel")?.takeIf { it.isNotEmpty() }?.let { Uri.decode(it) }
            val theme = backStackEntry.arguments?.getString("theme")?.takeIf { it.isNotEmpty() }?.let { Uri.decode(it) }

            // Handle back button press to navigate back to Overview (titles list)
            BackHandler(enabled = true) {
                android.util.Log.d("Navigation", "BackHandler in Detail - navigating back to Overview")
                val popped = navController.popBackStack()
                //android.util.Log.d("Navigation", "popBackStack result: $popped, backQueue size: ${navController.currentBackStack.value.size}")
                if (!popped) {
                    // If pop failed, navigate explicitly to Overview
                    android.util.Log.d("Navigation", "popBackStack failed, navigating explicitly to Overview")
                    navController.navigate(Screen.Overview.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            // Get media item and raw entry using ViewModel flows directly
            val mediaItemFlow = remember(title, channel, theme) {
                viewModel.getMediaItemFlow(title, channel, theme)
            }
            val mediaEntryFlow = remember(title, channel, theme) {
                viewModel.getMediaEntryByTitleFlow(title, channel, theme)
            }
            val mediaItem by mediaItemFlow.collectAsStateWithLifecycle(null)
            val mediaEntry by mediaEntryFlow.collectAsStateWithLifecycle(null)

            // Log for debugging
            android.util.Log.d("Navigation", "Detail Screen - Title: '$title', Channel: '$channel', Theme: '$theme'")
            android.util.Log.d("Navigation", "MediaItem found: ${mediaItem != null}, MediaEntry found: ${mediaEntry != null}")

            // Show loading or content - wait for BOTH mediaItem AND mediaEntry to be available
            val item = mediaItem
            val entry = mediaEntry
            // Get localized strings for loading state
            val loadingText = stringResource(R.string.loading_title, title)
            val contextAll = stringResource(R.string.context_all)
            val contextInfo = stringResource(R.string.context_info, channel ?: contextAll, theme ?: contextAll)

            if (item == null || entry == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            loadingText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (channel != null || theme != null) {
                            Text(
                                contextInfo,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                DetailView(
                    mediaItem = item,
                    onPlayClick = { isHighQuality ->
                        // Use the entry that is guaranteed to be non-null
                        viewModel.playVideoWithEntry(entry, isHighQuality)
                    },
                    onDownloadClick = { isHighQuality ->
                        // Use the entry that is guaranteed to be non-null
                        viewModel.setCurrentMediaEntry(entry)
                        viewModel.onDownloadButtonClicked(isHighQuality)
                    }
                )
            }
        }
    }
}
