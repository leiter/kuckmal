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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mediathekview.android.R
import com.mediathekview.android.compose.navigation.MediathekViewNavHost
import com.mediathekview.android.compose.ui.dialogs.AppDialogs
import com.mediathekview.android.compose.ui.dialogs.ConfirmationDialog
import com.mediathekview.android.compose.ui.dialogs.SingleChoiceDialog
import com.mediathekview.android.compose.ui.theme.MediathekViewTheme
import com.mediathekview.android.model.Broadcaster
import com.mediathekview.android.util.MediaUrlUtils
import com.mediathekview.android.util.UpdateChecker
import com.mediathekview.android.video.VideoPlayerManager
import com.mediathekview.shared.ui.navigation.Screen
import com.mediathekview.shared.viewmodel.DialogState
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

    // Inject UpdateChecker for update functionality
    private val updateChecker: UpdateChecker by inject()

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
                ComposeMainScreen(
                    viewModel = viewModel,
                    videoPlayerManager = videoPlayerManager,
                    updateChecker = updateChecker,
                    filesPath = filesDir.absolutePath + "/"
                )
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
fun ComposeMainScreen(
    viewModel: SharedViewModel,
    videoPlayerManager: VideoPlayerManager,
    updateChecker: UpdateChecker,
    filesPath: String
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }

    // Observe ViewModel state for synchronization with traditional UI
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val loadingState by viewModel.loadingState.collectAsStateWithLifecycle()
    val timePeriodId by viewModel.timePeriodId.collectAsStateWithLifecycle()

    // Dialog states for menu actions
    var showTimePeriodDialog by remember { mutableStateOf(false) }
    var showReinstallDialog by remember { mutableStateOf(false) }

    // Time period options
    val timePeriodLabels = listOf(
        stringResource(R.string.time_period_all),
        stringResource(R.string.time_period_today),
        stringResource(R.string.time_period_week),
        stringResource(R.string.time_period_month)
    )

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
                Log.d("ComposeActivity", "ViewState: Themes - channel=${currentViewState.channel}, theme=${currentViewState.theme}")
            }
            is ViewState.Detail -> {
                Log.d("ComposeActivity", "ViewState: Detail - title=${currentViewState.title}")
            }
        }
    }

    Scaffold { paddingValues ->
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
                    showTimePeriodDialog = true
                },
                onCheckUpdateClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val result = updateChecker.checkForUpdate()
                            coroutineScope.launch(Dispatchers.Main) {
                                when (result) {
                                    is UpdateChecker.UpdateCheckResult.UpdateAvailable -> {
                                        Toast.makeText(context, R.string.update_available, Toast.LENGTH_LONG).show()
                                        // Trigger reload through viewModel
                                        viewModel.checkAndLoadMediaListToDatabase(filesPath)
                                    }
                                    is UpdateChecker.UpdateCheckResult.NoUpdateNeeded -> {
                                        Toast.makeText(context, R.string.no_update_available, Toast.LENGTH_SHORT).show()
                                    }
                                    is UpdateChecker.UpdateCheckResult.CheckSkipped -> {
                                        Toast.makeText(context, R.string.no_update_available, Toast.LENGTH_SHORT).show()
                                    }
                                    is UpdateChecker.UpdateCheckResult.CheckFailed -> {
                                        Toast.makeText(context, "Error: ${result.error}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            coroutineScope.launch(Dispatchers.Main) {
                                Toast.makeText(context, "Error checking for updates: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onReinstallClick = {
                    showReinstallDialog = true
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

    // Time Period Selection Dialog
    if (showTimePeriodDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.time_period_title),
            items = timePeriodLabels,
            selectedIndex = timePeriodId,
            onItemSelected = { index ->
                showTimePeriodDialog = false
                // Calculate timestamp based on selection
                val now = System.currentTimeMillis() / 1000
                val limitDate = when (index) {
                    1 -> now - 86400        // Today (24 hours)
                    2 -> now - 604800       // Week (7 days)
                    3 -> now - 2592000      // Month (30 days)
                    else -> 0L              // All time
                }
                viewModel.setDateFilter(limitDate, index)
                Log.d("ComposeActivity", "Time period changed to: ${timePeriodLabels[index]}")
            },
            negativeLabel = stringResource(R.string.btn_cancel),
            onNegative = { showTimePeriodDialog = false }
        )
    }

    // Reinstall Confirmation Dialog
    if (showReinstallDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.reinstall_title),
            message = stringResource(R.string.reinstall_message),
            positiveLabel = stringResource(R.string.btn_reinstall),
            negativeLabel = stringResource(R.string.btn_cancel),
            onPositive = {
                showReinstallDialog = false
                // Clear data and trigger fresh load
                viewModel.clearData()
                Toast.makeText(context, "Clearing data and reloading...", Toast.LENGTH_SHORT).show()
                coroutineScope.launch(Dispatchers.IO) {
                    // Wait a moment for data to clear
                    kotlinx.coroutines.delay(500)
                    viewModel.checkAndLoadMediaListToDatabase(filesPath)
                    coroutineScope.launch(Dispatchers.Main) {
                        Toast.makeText(context, R.string.reinstall_complete, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onNegative = { showReinstallDialog = false }
        )
    }

    // SharedViewModel dialogs (for progress, errors, etc.)
    AppDialogs(
        viewModel = viewModel,
        onDialogAction = { action, _ ->
            when (action) {
                DialogState.Action.RETRY -> {
                    // Retry loading
                    viewModel.checkAndLoadMediaListToDatabase(filesPath)
                }
                DialogState.Action.START_DOWNLOAD -> {
                    // Start initial download
                    viewModel.checkAndLoadMediaListToDatabase(filesPath)
                }
                else -> { /* Other actions handled by dismiss */ }
            }
        }
    )
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
