package com.mediathekview.android.compose

import android.os.Bundle
import android.util.Log
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
import com.mediathekview.android.compose.models.ComposeViewModel
import com.mediathekview.android.compose.navigation.MediathekViewNavHost
import com.mediathekview.android.compose.navigation.Screen
import com.mediathekview.android.compose.ui.theme.MediathekViewTheme
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
    private val viewModel: ComposeViewModel by viewModel()

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
fun ComposeMainScreen(viewModel: ComposeViewModel) {
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
            is ComposeViewModel.ViewState.Themes -> {
                // Could sync navigation state here if needed
                Log.d("ComposeActivity", "ViewState: Themes - channel=${currentViewState.channel}, theme=${currentViewState.theme}")
            }
            is ComposeViewModel.ViewState.Detail -> {
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
                startDestination = determineStartDestination(viewState)
            )
        }
    }

    // Handle loading state
    if (loadingState == ComposeViewModel.LoadingState.LOADING) {
        // Show loading indicator overlay if needed
        // This could be a dialog or overlay composable
    }
}

/**
 * Determine the start destination based on current ViewModel state
 * With simplified navigation, we only have Overview and Detail screens
 */
private fun determineStartDestination(viewState: ComposeViewModel.ViewState): String {
    return when (viewState) {
        is ComposeViewModel.ViewState.Themes -> {
            // All browse states use the Overview screen
            Screen.Overview.route
        }
        is ComposeViewModel.ViewState.Detail -> {
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
