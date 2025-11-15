package com.mediathekview.android.compose

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mediathekview.android.compose.navigation.MediathekViewNavHost
import com.mediathekview.android.compose.navigation.Screen
import com.mediathekview.android.compose.ui.theme.MediathekViewTheme
import com.mediathekview.android.data.MediaViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Main Compose Activity - Entry point for Jetpack Compose UI
 * Now uses Compose Navigation with animated transitions
 */
class ComposeActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ComposeActivity"
    }

    // Inject MediaViewModel using Koin
    private val viewModel: MediaViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure ViewModel is initialized with proper navigation state
        // If we're starting fresh, navigate to all themes
        if (savedInstanceState == null) {
            viewModel.navigateToThemes(null)
        }

        setContent {
            MediathekViewTheme {
                ComposeMainScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeMainScreen(viewModel: MediaViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Observe ViewModel state for synchronization with traditional UI
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val loadingState by viewModel.loadingState.collectAsStateWithLifecycle()

    // Observe start activity intents for video playback
    LaunchedEffect(viewModel) {
        Log.d("ComposeActivity", "Setting up intent collector...")
        viewModel.startActivityIntent.collect { intent ->
            Log.d("ComposeActivity", "Received playback intent, starting activity...")
            context.startActivity(intent)
            Log.d("ComposeActivity", "Activity started successfully")
        }
    }

    // Observe navigation state
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Log navigation changes
    LaunchedEffect(currentRoute) {
        Log.d("ComposeActivity", "Current route: $currentRoute")
    }

    // Sync with ViewModel state when needed (future integration)
    LaunchedEffect(viewState) {
        val currentViewState = viewState // Capture the value to enable smart cast
        when (currentViewState) {
            is MediaViewModel.ViewState.Themes -> {
                // Could sync navigation state here if needed
                Log.d("ComposeActivity", "ViewState: Themes - channel=${currentViewState.channel}, theme=${currentViewState.theme}")
            }
            is MediaViewModel.ViewState.Detail -> {
                Log.d("ComposeActivity", "ViewState: Detail - title=${currentViewState.title}")
            }
        }
    }

    Scaffold(
        topBar = {
            // Top bar can be customized based on current route
            // For now, keeping it minimal to match the original design
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            MediathekViewNavHost(
                navController = navController,
                viewModel = viewModel,
                startDestination = determineStartDestination(viewState)
            )
        }
    }

    // Handle loading state
    if (loadingState == MediaViewModel.LoadingState.LOADING) {
        // Show loading indicator overlay if needed
        // This could be a dialog or overlay composable
    }
}

/**
 * Determine the start destination based on current ViewModel state
 * This helps maintain state during configuration changes
 */
private fun determineStartDestination(viewState: MediaViewModel.ViewState): String {
    return when (viewState) {
        is MediaViewModel.ViewState.Themes -> {
            when {
                viewState.theme != null -> {
                    // Currently viewing titles within a theme
                    Screen.ThemeTitles.createRoute(viewState.channel, viewState.theme)
                }
                viewState.channel != null -> {
                    // Currently viewing themes for a channel
                    Screen.ChannelThemes.createRoute(viewState.channel)
                }
                else -> {
                    // Viewing all themes
                    Screen.AllThemes.route
                }
            }
        }
        is MediaViewModel.ViewState.Detail -> {
            // Currently viewing detail
            Screen.MediaDetail.createRoute(viewState.title)
        }
    }
}

// Preview removed - requires ViewModel injection
