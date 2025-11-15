package com.mediathekview.android.compose.navigation

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mediathekview.android.compose.data.ComposeDataMapper
import com.mediathekview.android.compose.models.Channel
import com.mediathekview.android.compose.models.MediaItem
import com.mediathekview.android.compose.screens.BrowseView
import com.mediathekview.android.compose.screens.DetailView
import com.mediathekview.android.compose.screens.rememberComposeMediaState
import com.mediathekview.android.data.MediaViewModel

/**
 * Navigation routes for the Compose screens
 */
sealed class Screen(val route: String) {
    // Browse screens with navigation hierarchy
    object AllThemes : Screen("all_themes")
    data class ChannelThemes(val channelId: String) : Screen("channel_themes/{channelId}") {
        companion object {
            fun createRoute(channelId: String) = "channel_themes/$channelId"
        }
    }
    data class ThemeTitles(val channelId: String?, val theme: String) : Screen("theme_titles?channel={channelId}&theme={theme}") {
        companion object {
            fun createRoute(channelId: String?, theme: String): String {
                return if (channelId != null) {
                    "theme_titles?channel=$channelId&theme=$theme"
                } else {
                    "theme_titles?theme=$theme"
                }
            }
        }
    }

    // Detail screen with optional channel and theme context
    data class MediaDetail(
        val title: String,
        val channel: String? = null,
        val theme: String? = null
    ) : Screen("media_detail/{title}?channel={channel}&theme={theme}") {
        companion object {
            fun createRoute(
                title: String,
                channel: String? = null,
                theme: String? = null
            ): String {
                val encodedTitle = Uri.encode(title)
                val encodedChannel = channel?.let { Uri.encode(it) } ?: ""
                val encodedTheme = theme?.let { Uri.encode(it) } ?: ""
                return "media_detail/$encodedTitle?channel=$encodedChannel&theme=$encodedTheme"
            }
        }
    }

    // Search screen (future implementation)
    object Search : Screen("search")
}

/**
 * Animation specifications for navigation transitions
 */
object NavigationAnimations {
    const val ANIMATION_DURATION = 300

    // Forward navigation (going deeper into hierarchy)
    val slideInFromRight = slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(ANIMATION_DURATION)
    ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))

    val slideOutToLeft = slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth / 3 },
        animationSpec = tween(ANIMATION_DURATION)
    ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))

    // Backward navigation (going back in hierarchy)
    val slideInFromLeft = slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth / 3 },
        animationSpec = tween(ANIMATION_DURATION)
    ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))

    val slideOutToRight = slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(ANIMATION_DURATION)
    ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))

    // Fade transitions for same-level navigation
    val fadeIn = fadeIn(animationSpec = tween(ANIMATION_DURATION))
    val fadeOut = fadeOut(animationSpec = tween(ANIMATION_DURATION))
}

/**
 * Main navigation host for the Compose UI with real data
 */
@Composable
fun MediathekViewNavHost(
    navController: NavHostController,
    viewModel: MediaViewModel,
    startDestination: String = Screen.AllThemes.route
) {
    // Create state holder for managing data flow
    val mediaState = rememberComposeMediaState(viewModel)
    val isDataLoaded by mediaState.isDataLoaded()
    val loadingState by mediaState.getLoadingState()

    // Ensure navigation state is initialized on first composition
    LaunchedEffect(Unit) {
        // If ViewModel is in initial state, navigate to all themes
        val currentViewState = viewModel.viewState.value
        if (currentViewState is MediaViewModel.ViewState.Themes &&
            currentViewState.channel == null &&
            currentViewState.theme == null) {
            viewModel.navigateToThemes(null)
        }
    }

    // Show loading indicator if data is not yet loaded
    if (!isDataLoaded && loadingState != MediaViewModel.LoadingState.ERROR) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Show error state if loading failed
    if (loadingState == MediaViewModel.LoadingState.ERROR) {
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
        // All Themes screen (initial screen)
        composable(
            route = Screen.AllThemes.route,
            enterTransition = { NavigationAnimations.fadeIn },
            exitTransition = {
                // When going to channel themes, use fade instead of slide
                if (targetState.destination.route?.startsWith("channel_themes") == true) {
                    NavigationAnimations.fadeOut
                } else {
                    NavigationAnimations.slideOutToLeft
                }
            },
            popEnterTransition = { NavigationAnimations.slideInFromLeft },
            popExitTransition = { NavigationAnimations.fadeOut }
        ) {
            val themes by mediaState.getThemes(channelName = null)

            BrowseView(
                channels = mediaState.channels,
                titles = themes, // These are actually themes in this context
                selectedChannel = null,
                currentTheme = "All Themes",
                onChannelSelected = { channel ->
                    // Navigate to channel themes
                    mediaState.navigateToThemes(channel.name)
                    navController.navigate(Screen.ChannelThemes.createRoute(channel.name))
                },
                onTitleSelected = { theme ->
                    // From all themes, clicking a theme navigates to theme titles
                    mediaState.navigateToTitles(null, theme)
                    navController.navigate(Screen.ThemeTitles.createRoute(null, theme))
                },
                onMenuClick = {
                    // Handle menu click
                }
            )
        }

        // Channel Themes screen
        composable(
            route = "channel_themes/{channelId}",
            arguments = listOf(navArgument("channelId") { type = NavType.StringType }),
            enterTransition = {
                // Check if coming from AllThemes or another channel
                when {
                    initialState.destination.route == Screen.AllThemes.route -> {
                        NavigationAnimations.fadeIn  // Fade when coming from AllThemes (channel selection)
                    }
                    initialState.destination.route?.startsWith("channel_themes") == true -> {
                        NavigationAnimations.fadeIn  // Fade for channel-to-channel
                    }
                    else -> {
                        NavigationAnimations.slideInFromRight  // Slide for other navigations
                    }
                }
            },
            exitTransition = {
                // Only slide out when going to detail or titles, not channel switching
                if (targetState.destination.route?.startsWith("channel_themes") == true) {
                    NavigationAnimations.fadeOut  // Simple fade for channel-to-channel
                } else {
                    NavigationAnimations.slideOutToLeft  // Slide for other navigations
                }
            },
            popEnterTransition = { NavigationAnimations.slideInFromLeft },
            popExitTransition = { NavigationAnimations.slideOutToRight }
        ) { backStackEntry ->
            val channelName = backStackEntry.arguments?.getString("channelId") ?: ""
            val channel = ComposeDataMapper.findChannelByName(channelName)
            val themes by mediaState.getThemes(channelName = channelName)

            BrowseView(
                channels = mediaState.channels,
                titles = themes, // These are themes for this channel
                selectedChannel = channel,
                currentTheme = "${channel?.displayName ?: ""} Themes",
                onChannelSelected = { newChannel ->
                    if (newChannel.name != channelName) {
                        // Navigate to different channel themes, replacing current
                        mediaState.navigateToThemes(newChannel.name)
                        navController.navigate(Screen.ChannelThemes.createRoute(newChannel.name)) {
                            popUpTo(Screen.AllThemes.route) { inclusive = false }
                        }
                    }
                },
                onTitleSelected = { theme ->
                    // Navigate to theme titles
                    mediaState.navigateToTitles(channelName, theme)
                    navController.navigate(Screen.ThemeTitles.createRoute(channelName, theme))
                },
                onMenuClick = {
                    // Handle menu click
                }
            )
        }

        // Theme Titles screen
        composable(
            route = "theme_titles?channel={channelId}&theme={theme}",
            arguments = listOf(
                navArgument("channelId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("theme") { type = NavType.StringType }
            ),
            enterTransition = { NavigationAnimations.slideInFromRight },
            exitTransition = { NavigationAnimations.slideOutToLeft },
            popEnterTransition = { NavigationAnimations.slideInFromLeft },
            popExitTransition = { NavigationAnimations.slideOutToRight }
        ) { backStackEntry ->
            val channelName = backStackEntry.arguments?.getString("channelId")
            val theme = backStackEntry.arguments?.getString("theme") ?: ""
            val channel = ComposeDataMapper.findChannelByName(channelName)
            val titles by mediaState.getTitles(channelName = channelName, theme = theme)

            BrowseView(
                channels = mediaState.channels,
                titles = titles, // These are actual titles within the theme
                selectedChannel = channel,
                currentTheme = theme,
                onChannelSelected = { newChannel ->
                    // Navigate to channel themes
                    mediaState.navigateToThemes(newChannel.name)
                    navController.navigate(Screen.ChannelThemes.createRoute(newChannel.name)) {
                        popUpTo(Screen.AllThemes.route) { inclusive = false }
                    }
                },
                onTitleSelected = { title ->
                    // Navigate to media detail with context
                    mediaState.navigateToDetail(title, channelName, theme)
                    navController.navigate(Screen.MediaDetail.createRoute(title, channelName, theme))
                },
                onMenuClick = {
                    // Handle menu click
                }
            )
        }

        // Media Detail screen
        composable(
            route = "media_detail/{title}?channel={channel}&theme={theme}",
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

            // Get media item and raw entry with context for better matching
            val mediaItem by mediaState.getMediaItem(title, channel, theme)
            val mediaEntry by mediaState.getMediaEntry(title, channel, theme)

            // Log for debugging
            android.util.Log.d("Navigation", "Detail Screen - Title: '$title', Channel: '$channel', Theme: '$theme'")
            android.util.Log.d("Navigation", "MediaItem found: ${mediaItem != null}, MediaEntry found: ${mediaEntry != null}")

            // Show loading or empty state if media item is not found
            val item = mediaItem
            if (item == null) {
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
                        mediaState.onPlayClicked(mediaEntry, isHighQuality)
                    },
                    onDownloadClick = { isHighQuality ->
                        mediaState.onDownloadClicked(mediaEntry, isHighQuality)
                    }
                )
            }
        }
    }
}

/**
 * Extension functions for navigation
 */
fun NavHostController.navigateToAllThemes() {
    navigate(Screen.AllThemes.route) {
        popUpTo(Screen.AllThemes.route) { inclusive = true }
    }
}

fun NavHostController.navigateToChannelThemes(channelId: String) {
    navigate(Screen.ChannelThemes.createRoute(channelId))
}

fun NavHostController.navigateToThemeTitles(channelId: String?, theme: String) {
    navigate(Screen.ThemeTitles.createRoute(channelId, theme))
}

fun NavHostController.navigateToMediaDetail(
    title: String,
    channel: String? = null,
    theme: String? = null
) {
    navigate(Screen.MediaDetail.createRoute(title, channel, theme))
}