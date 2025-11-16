package com.mediathekview.android.compose.navigation

import android.net.Uri
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.flow.flowOf

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
    startDestination: String = Screen.Overview.route
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
            Text("Error loading data. Please try again.")
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Overview screen (manages its own internal state)
        composable(
            route = Screen.Overview.route,
            enterTransition = { NavigationAnimations.fadeIn },
            exitTransition = { NavigationAnimations.slideOutToLeft },
            popEnterTransition = { NavigationAnimations.slideInFromLeft },
            popExitTransition = { NavigationAnimations.fadeOut }
        ) {
            // Overview screen manages its own state
            var overviewState by remember { mutableStateOf<OverviewState>(OverviewState.AllThemes) }

            // Pagination state - reset when state changes
            var currentPart by remember { mutableStateOf(0) }

            // Reset pagination when overview state changes
            LaunchedEffect(overviewState) {
                currentPart = 0
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
            val displayData = when (overviewState) {
                is OverviewState.AllThemes -> themesData
                is OverviewState.ChannelThemes -> themesData
                is OverviewState.ThemeTitles -> titlesData
            }

            // Determine if there are more items to load
            // Show "more" if we have exactly the limit (1200 * (currentPart + 1)) items
            val expectedLimit = (currentPart + 1) * 1200
            val hasMoreItems = when (overviewState) {
                is OverviewState.AllThemes -> themesData.size >= expectedLimit
                is OverviewState.ChannelThemes -> themesData.size >= expectedLimit
                is OverviewState.ThemeTitles -> false // Don't paginate titles
            }

            // Get current context info for display
            val (selectedChannel, currentThemeLabel, showingTitles) = when (val state = overviewState) {
                is OverviewState.AllThemes -> {
                    Triple(null, "All Themes", false)
                }
                is OverviewState.ChannelThemes -> {
                    val channel = ComposeDataMapper.findChannelByName(state.channelName)
                    Triple(channel, "${channel?.displayName ?: state.channelName} Themes", false)
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
                currentTheme = currentThemeLabel,
                isShowingTitles = showingTitles,
                hasMoreItems = hasMoreItems,
                onChannelSelected = { channel ->
                    // Change to that channel's themes (internal state change, no navigation)
                    overviewState = OverviewState.ChannelThemes(channel.name)
                },
                onTitleSelected = { item ->
                    when (val state = overviewState) {
                        is OverviewState.AllThemes -> {
                            // Clicked on a theme -> show titles in that theme
                            overviewState = OverviewState.ThemeTitles(null, item)
                        }
                        is OverviewState.ChannelThemes -> {
                            // Clicked on a theme -> show titles in that theme for this channel
                            overviewState = OverviewState.ThemeTitles(state.channelName, item)
                        }
                        is OverviewState.ThemeTitles -> {
                            // Clicked on a title -> navigate to detail (actual navigation!)
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
                },
                onMenuClick = {
                    // Could handle back navigation within overview
                    when (overviewState) {
                        is OverviewState.AllThemes -> { /* Already at root */ }
                        is OverviewState.ChannelThemes -> {
                            overviewState = OverviewState.AllThemes
                        }
                        is OverviewState.ThemeTitles -> {
                            val state = overviewState as OverviewState.ThemeTitles
                            overviewState = if (state.channelName != null) {
                                OverviewState.ChannelThemes(state.channelName)
                            } else {
                                OverviewState.AllThemes
                            }
                        }
                    }
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
                            "Loading: $title",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (channel != null || theme != null) {
                            Text(
                                "Context: ${channel ?: "All"} / ${theme ?: "All"}",
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

/**
 * Extension functions for navigation
 */
fun NavHostController.navigateToOverview() {
    navigate(Screen.Overview.route) {
        popUpTo(Screen.Overview.route) { inclusive = true }
    }
}

fun NavHostController.navigateToDetail(
    title: String,
    channel: String? = null,
    theme: String? = null
) {
    navigate(Screen.Detail.createRoute(title, channel, theme))
}
