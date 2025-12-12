package com.mediathekview.android.compose

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mediathekview.android.compose.navigation.MediathekViewNavHost
import com.mediathekview.android.compose.ui.theme.MediathekViewTheme
import com.mediathekview.android.model.Broadcaster
import com.mediathekview.android.util.MediaUrlUtils
import com.mediathekview.android.video.VideoPlayerManager
import com.mediathekview.shared.database.MediaEntry
import com.mediathekview.shared.ui.navigation.Screen
import com.mediathekview.shared.viewmodel.SharedViewModel
import com.mediathekview.shared.viewmodel.ViewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Main Compose Activity - Entry point for Jetpack Compose UI
 * Now uses Compose Navigation with animated transitions
 */
class ComposeActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ComposeActivity"
    }

    // Inject SharedViewModel using Koin (replaces ComposeViewModel)
    private val viewModel: SharedViewModel by viewModel()

    // Inject VideoPlayerManager for playback intent observation
    private val videoPlayerManager: VideoPlayerManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Use fallback display (brand color + abbreviation) for channel identification
        // instead of logo icons (MediathekViewWeb style)
        Broadcaster.useFallbackDisplay = true

        // Check and load media list on startup (fresh install flow)
        val privatePath = filesDir.absolutePath + "/"
        val hasData = viewModel.checkAndLoadMediaListToDatabase(privatePath)
        Log.d(TAG, "Startup check: hasData=$hasData")

        // Ensure ViewModel is initialized with proper navigation state
        // If we're starting fresh, navigate to all themes
        if (savedInstanceState == null) {
            viewModel.navigateToThemes(null)
        }

        setContent {
            MediathekViewTheme {
                ComposeMainScreen(viewModel, videoPlayerManager)
            }
        }
    }

    // Let Compose handle back navigation via OnBackPressedDispatcher
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Check if OnBackPressedDispatcher has any enabled callbacks
        if (onBackPressedDispatcher.hasEnabledCallbacks()) {
            // Let Compose BackHandlers handle it
            super.onBackPressed()
        } else {
            // No Compose handlers, finish activity
            super.onBackPressed()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeMainScreen(viewModel: SharedViewModel, videoPlayerManager: VideoPlayerManager) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }

    // Observe ViewModel state for synchronization with traditional UI
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val loadingState by viewModel.loadingState.collectAsStateWithLifecycle()

    // Observe playback intents from VideoPlayerManager for video playback
    LaunchedEffect(videoPlayerManager) {
        Log.d("ComposeActivity", "Setting up playback intent collector...")
        videoPlayerManager.playbackIntents.collect { intent ->
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
            is ViewState.Themes -> {
                // Could sync navigation state here if needed
                Log.d("ComposeActivity", "ViewState: Themes - channel=${currentViewState.channel}, theme=${currentViewState.theme}")
            }
            is ViewState.Detail -> {
                Log.d("ComposeActivity", "ViewState: Detail - title=${currentViewState.title}")
            }
        }
    }

    // Overflow menu state
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
//            TopAppBar(
//                title = { Text("MediathekView") },
//                actions = {
//                    IconButton(onClick = { showMenu = true }) {
//                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
//                    }
//                    DropdownMenu(
//                        expanded = showMenu,
//                        onDismissRequest = { showMenu = false }
//                    ) {
//                        DropdownMenuItem(
//                            text = { Text("Legacy UI") },
//                            onClick = {
//                                showMenu = false
//                                val intent = Intent(context, MediaActivity::class.java)
//                                context.startActivity(intent)
//                            }
//                        )
//                    }
//                }
//            )
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
                onTimePeriodClick = {
                    // TODO: Implement time period dialog
                    Toast.makeText(context, "Time period filter not yet implemented", Toast.LENGTH_SHORT).show()
                },
                onCheckUpdateClick = {
                    // TODO: Implement update check
                    Toast.makeText(context, "Update check not yet implemented", Toast.LENGTH_SHORT).show()
                },
                onReinstallClick = {
                    // TODO: Implement reinstall
                    Toast.makeText(context, "Reinstall not yet implemented", Toast.LENGTH_SHORT).show()
                },
                onPlayVideo = { entry, isHighQuality ->
                    coroutineScope.launch {
                        videoPlayerManager.playVideo(entry, isHighQuality)
                    }
                },
                onDownloadVideo = { entry, isHighQuality ->
                    // Get the appropriate video URL based on quality selection
                    val videoUrl = if (isHighQuality) {
                        val hdUrl = entry.hdUrl.ifEmpty { entry.url }
                        MediaUrlUtils.reconstructUrl(hdUrl, entry.url)
                    } else {
                        val smallUrl = entry.smallUrl.ifEmpty { entry.url }
                        MediaUrlUtils.reconstructUrl(smallUrl, entry.url)
                    }
                    val quality = if (isHighQuality) "High" else "Low"
                    viewModel.downloadVideo(entry, videoUrl, quality)
                }
            )
        }
    }

    // TODO: Implement SharedViewModel-compatible dialogs
    // AppDialogs(viewModel = viewModel)
}

/**
 * Determine the start destination based on current ViewModel state
 * With simplified navigation, we only have Overview and Detail screens
 */
private fun determineStartDestination(viewState: ViewState): String {
    return when (viewState) {
        is ViewState.Themes -> {
            // All browse states use the Overview screen
            Screen.Overview.route
        }
        is ViewState.Detail -> {
            // Detail screen
            Screen.Detail.createRoute(
                viewState.title,
                viewState.navigationChannel,
                viewState.navigationTheme
            )
        }
    }
}

// Preview removed - requires ViewModel injection
