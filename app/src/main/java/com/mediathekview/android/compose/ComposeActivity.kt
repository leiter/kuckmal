package com.mediathekview.android.compose

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mediathekview.android.compose.models.Channel
import com.mediathekview.android.compose.models.MediaItem
import com.mediathekview.android.compose.models.SampleData
import com.mediathekview.android.compose.screens.BrowseView
import com.mediathekview.android.compose.screens.DetailView
import com.mediathekview.android.compose.ui.theme.MediathekViewTheme
import com.mediathekview.android.data.MediaViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Main Compose Activity - Entry point for Jetpack Compose UI
 * Uses Koin for dependency injection
 */
class ComposeActivity : ComponentActivity() {

    // Inject MediaViewModel using Koin
    private val viewModel: MediaViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediathekViewTheme {
                ComposeMainScreen(viewModel)
            }
        }
    }
}

enum class Screen {
    BROWSE, DETAIL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeMainScreen(viewModel: MediaViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.BROWSE) }
    var selectedChannel by remember { mutableStateOf<Channel?>(SampleData.sampleChannels[9]) }
    val context = LocalContext.current

    // Observe ViewModel state (for future use)
    val loadingState by viewModel.loadingState.collectAsState()

    // Handle back button
    BackHandler(enabled = currentScreen == Screen.DETAIL) {
        currentScreen = Screen.BROWSE
    }

    Scaffold(
        topBar = {
//            TopAppBar(
//                title = {
//                    Text(
//                        when (currentScreen) {
//                            Screen.BROWSE -> "MediathekView - Browse"
//                            Screen.DETAIL -> "MediathekView - Detail"
//                        }
//                    )
//                },
//                navigationIcon = {
//                    if (currentScreen == Screen.DETAIL) {
//                        IconButton(onClick = { currentScreen = Screen.BROWSE }) {
//                            Icon(
//                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                                contentDescription = "Back"
//                            )
//                        }
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primary,
//                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
//                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
//                )
//            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                Screen.BROWSE -> {
                    BrowseView(
                        channels = SampleData.sampleChannels,
                        titles = SampleData.sampleTitles,
                        selectedChannel = selectedChannel,
                        onChannelSelected = { channel ->
                            selectedChannel = channel
                            Toast.makeText(
                                context,
                                "Selected: ${channel.displayName}",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onTitleSelected = { title ->
                            Toast.makeText(
                                context,
                                "Opening: $title",
                                Toast.LENGTH_SHORT
                            ).show()
                            currentScreen = Screen.DETAIL
                        },
                        onMenuClick = {
                            Toast.makeText(
                                context,
                                "Menu clicked",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
                Screen.DETAIL -> {
                    DetailView(
                        mediaItem = SampleData.sampleMediaItem,
                        onPlayClick = { isHighQuality ->
                            Toast.makeText(
                                context,
                                "Play clicked - Quality: ${if (isHighQuality) "High" else "Low"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onDownloadClick = { isHighQuality ->
                            Toast.makeText(
                                context,
                                "Download clicked - Quality: ${if (isHighQuality) "High" else "Low"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}

// Preview removed - requires ViewModel injection
