package com.mediathekview.android.ui

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.mediathekview.android.R
import com.mediathekview.android.data.MediaViewModel
import com.mediathekview.android.ui.dialog.DialogFactory.dismissSafely
import com.mediathekview.android.ui.dialog.DialogModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Main activity - Clean architecture with database-first reactive Flows
 */
class MediaActivity : FragmentActivity() {

    companion object {
        private const val TAG = "MediaActivity"
    }

    // Dependency injection
    private val viewModel: MediaViewModel by viewModel()

    private lateinit var uiManager: UIManager
    private var currentDialog: AlertDialog? = null  // All dialogs from reactive stream (survives config changes)
    private var welcomeDialog: AlertDialog? = null  // Welcome dialog shown directly (dismissed on destroy)

    // Paths
    private val privatePath: String by lazy { filesDir.absolutePath + "/" }
//    private val storagePath: String by lazy {
//        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/"
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Force dark mode on TV devices BEFORE super.onCreate()
        // TV launchers often don't expose dark mode settings
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as android.app.UiModeManager
        val isTvDevice = uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        if (isTvDevice) {
            // Override configuration to force night mode
            val config = resources.configuration
            config.uiMode = (config.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or
                            android.content.res.Configuration.UI_MODE_NIGHT_YES

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                createConfigurationContext(config)
                resources.updateConfiguration(config, resources.displayMetrics)
            } else {
                @Suppress("DEPRECATION")
                resources.updateConfiguration(config, resources.displayMetrics)
            }
            Log.d(TAG, "TV device detected - forced dark mode")
        }

        super.onCreate(savedInstanceState)

        // Ensure content doesn't draw behind status bar (fixes API 35 edge-to-edge change)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContentView(R.layout.activity_mv)

        // Initialize UI manager
        uiManager = UIManager(this, viewModel)

        // Set up modern back press handling
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val handled = uiManager.onBackPressed()
                if (!handled) {
                    // At top level, exit app
                    isEnabled = false  // Disable this callback
                    onBackPressedDispatcher.onBackPressed()  // Let system handle (exit app)
                    isEnabled = true  // Re-enable for next time
                }
            }
        })

        // Set up ViewModel observers
        setupViewModelObservers()

        // Load data on startup
        lifecycleScope.launch {
            val dataLoaded = viewModel.isDataLoaded()
            if (dataLoaded) {
                // Check if ViewModel is in initial state or has preserved state from config change
                val currentViewState = viewModel.viewState.value
                val selectedChannel = viewModel.selectedChannel
                val selectedTheme = viewModel.selectedTheme
                val selectedTitle = viewModel.selectedTitle

                Log.d(TAG, "=== State Check ===")
                Log.d(TAG, "ViewModel instance: ${viewModel.hashCode()}")
                Log.d(TAG, "viewState: $currentViewState")
                Log.d(TAG, "selectedChannel: $selectedChannel")
                Log.d(TAG, "selectedTheme: $selectedTheme")
                Log.d(TAG, "selectedTitle: $selectedTitle")

                // Determine if this is truly initial state or preserved state
                val isInitialState = currentViewState is MediaViewModel.ViewState.Themes &&
                    currentViewState.channel == null &&
                    selectedChannel == null && selectedTheme == null && selectedTitle == null

                if (isInitialState) {
                    Log.d(TAG, "Initial state detected - calling navigateToThemes(null)")
                    // This is the first launch - initialize to All Themes
                    viewModel.navigateToThemes(null)
                } else {
                    Log.d(TAG, "Preserved state detected - restoring UI")
                    // This is a config change - ViewModel has preserved state
                    // Restore UI to match the preserved state
                    uiManager.restoreNavigationState()
                }
            } else {
                // Check if loading is already in progress (e.g., download/decompression from previous Activity instance)
                val loadingState = viewModel.loadingState.value
                if (loadingState != MediaViewModel.LoadingState.LOADING) {
                    Log.d(TAG, "Loading media list from storage")
                    viewModel.checkAndLoadMediaListToDatabase(privatePath)
                    // Welcome dialog will be shown automatically by ViewModel if needed
                } else {
                    Log.d(TAG, "Loading already in progress, skipping duplicate check")
                }
            }
        }
    }

    /**
     * Called when activity comes to foreground
     * Checks for updates once per day (configurable interval)
     */
    override fun onResume() {
        super.onResume()

        // Only check for updates if data is loaded
        lifecycleScope.launch {
            val dataLoaded = viewModel.isDataLoaded()
            if (dataLoaded) {
                // Check for updates when app comes to foreground (respects 24-hour interval)
                checkForUpdatesOnForeground()
            }
        }
    }

    /**
     * Set up observers for ViewModel state changes
     */
    private fun setupViewModelObservers() {
        // Observe loading state (StateFlow)
        lifecycleScope.launch {
            viewModel.loadingState.collectLatest { state ->
                when (state) {
                    MediaViewModel.LoadingState.LOADING -> {
                        // Post loading dialog to ViewModel for config change preservation
                        viewModel.showDialog(
                            DialogModel.Progress(
                                title = getString(R.string.dialog_title_please_wait),
                                message = getString(R.string.dialog_msg_loading)
                            )
                        )
                    }
                    MediaViewModel.LoadingState.LOADED -> {
                        viewModel.dismissDialog()
                        uiManager.onMediaListLoaded()
                    }
                    MediaViewModel.LoadingState.ERROR -> {
                        viewModel.dismissDialog()
                        val error = viewModel.errorMessage.value
                        Toast.makeText(this@MediaActivity, getString(R.string.error_format, error), Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }

        // Observe loading progress (StateFlow)
        lifecycleScope.launch {
            viewModel.loadingProgress.collectLatest { progress ->
                // Update progress dialog via ViewModel (only if loading)
                if (viewModel.loadingState.value == MediaViewModel.LoadingState.LOADING) {
                    viewModel.showDialog(
                        DialogModel.Progress(
                            title = getString(R.string.dialog_title_please_wait),
                            message = getString(R.string.dialog_msg_loaded_entries, progress)
                        )
                    )
                }
            }
        }

        // Observe view state changes for navigation and detail updates
        lifecycleScope.launch {
            viewModel.viewState.collectLatest { viewState ->
                Log.d(TAG, "View state changed to: $viewState")

                // Update detail view when entering Detail state
                if (viewState is MediaViewModel.ViewState.Detail) {
                    Log.d(TAG, "Media entry updated: ${viewState.mediaEntry.title}")
                    uiManager.updateDetailViewFromFlow(viewState.mediaEntry)
                } else {
                    // Clear detail view when leaving Detail state
                    uiManager.updateDetailViewFromFlow(null)
                }
            }
        }

        // Observe reactive content list (unified themes/titles) and update UI
        lifecycleScope.launch {
            viewModel.contentList.collectLatest { entries ->
                val currentView = viewModel.viewState.value
                if (currentView is MediaViewModel.ViewState.Themes) {
                    if (currentView.theme == null) {
                        Log.d(TAG, "Content updated (themes): ${entries.size} entries")
                    } else {
                        Log.d(TAG, "Content updated (titles): ${entries.size} entries")
                    }
                }
                uiManager.updateContentListFromFlow(entries)
            }
        }

        // Observe dialog stream and create/update/dismiss dialogs
        lifecycleScope.launch {
            viewModel.dialogModel.collectLatest { model ->
                handleDialogModel(model)
            }
        }

        // Observe welcome dialog trigger (first-time setup)
        lifecycleScope.launch {
            viewModel.showWelcomeDialog.collectLatest { shouldShow ->
                if (shouldShow) {
                    Log.d(TAG, "Showing welcome dialog for first-time setup")
                    welcomeDialog = showWelcomeDialog(viewModel)
                    viewModel.welcomeDialogShown()
                }
            }
        }

        // Observe Intent emissions and start activities
        lifecycleScope.launch {
            viewModel.startActivityIntent.collect { intent ->
                try {
                    Log.i(TAG, "Starting activity from ViewModel Intent")
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting activity from Intent", e)
                    Toast.makeText(
                        this@MediaActivity,
                        "Error opening video player: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Handle dialog model changes from ViewModel
     * Delegates to extension function in ActivityDialogExtensions.kt
     */
    private fun handleDialogModel(model: DialogModel?) {
        currentDialog = handleDialogModel(model, currentDialog)
    }


    /**
     * Start downloading the media list with automatic full/diff selection
     * If database has data, downloads diff file. Otherwise downloads full file.
     */
    fun startMediaListDownload() {
        Log.i(TAG, "Starting smart media list download (auto-selects full or diff)")

        viewModel.startSmartMediaListDownload()

        Toast.makeText(
            this,
            getString(R.string.dialog_msg_downloading),
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Manually check for updates from the menu
     * Forces an update check regardless of interval
     */
    fun checkForUpdatesManually() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Manual update check initiated by user")

                // Show progress dialog
                viewModel.showDialog(
                    DialogModel.Progress(
                        title = getString(R.string.dialog_title_checking_update),
                        message = getString(R.string.dialog_msg_checking_update)
                    )
                )

                val result = viewModel.checkForUpdate(forceCheck = true)

                // Dismiss progress dialog
                viewModel.dismissDialog()

                // Check if activity is still valid before showing dialogs
                if (isFinishing || isDestroyed) {
                    Log.d(TAG, "Activity finishing/destroyed - skipping update result dialog")
                    return@launch
                }

                when (result) {
                    is com.mediathekview.android.util.UpdateChecker.UpdateCheckResult.UpdateAvailable -> {
                        Log.i(TAG, "Update available - prompting user")
                        showUpdateAvailableDialog(viewModel)
                    }
                    is com.mediathekview.android.util.UpdateChecker.UpdateCheckResult.NoUpdateNeeded -> {
                        Log.d(TAG, "Film list is up to date")
                        Toast.makeText(
                            this@MediaActivity,
                            getString(R.string.toast_no_update_available),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is com.mediathekview.android.util.UpdateChecker.UpdateCheckResult.CheckFailed -> {
                        Log.w(TAG, "Update check failed: ${result.error}")
                        Toast.makeText(
                            this@MediaActivity,
                            getString(R.string.toast_update_check_failed, result.error),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is com.mediathekview.android.util.UpdateChecker.UpdateCheckResult.CheckSkipped -> {
                        // This shouldn't happen with forceCheck = true, but handle it anyway
                        Log.d(TAG, "Update check skipped")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for updates", e)
                viewModel.dismissDialog()

                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(
                        this@MediaActivity,
                        getString(R.string.toast_update_check_failed, e.message ?: "Unknown error"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Check for updates when app comes to foreground using server-friendly differential update mechanism
     *
     * Server-Friendly Implementation:
     * - Only checks if the configured interval has elapsed (default: once per day)
     * - Uses lightweight HTTP HEAD requests (~1KB vs ~50MB)
     * - Avoids unnecessary server load by comparing metadata
     * - Respects MediathekView team's best practices
     * - Silently checks in background, only shows dialog if update available
     */
    private fun checkForUpdatesOnForeground() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Checking for film list updates...")
                val result = viewModel.checkForUpdate(forceCheck = false)

                // Check if activity is still valid before showing dialogs
                if (isFinishing || isDestroyed) {
                    Log.d(TAG, "Activity finishing/destroyed - skipping update dialog")
                    return@launch
                }

                when (result) {
                    is com.mediathekview.android.util.UpdateChecker.UpdateCheckResult.UpdateAvailable -> {
                        Log.i(TAG, "Update available - prompting user")
                        showUpdateAvailableDialog(viewModel)
                    }
                    is com.mediathekview.android.util.UpdateChecker.UpdateCheckResult.NoUpdateNeeded -> {
                        Log.d(TAG, "Film list is up to date")
                    }
                    is com.mediathekview.android.util.UpdateChecker.UpdateCheckResult.CheckSkipped -> {
                        Log.d(TAG, "Update check skipped - interval not elapsed. Next check: ${result.nextCheckTime}")
                    }
                    is com.mediathekview.android.util.UpdateChecker.UpdateCheckResult.CheckFailed -> {
                        Log.w(TAG, "Update check failed: ${result.error}")
                        // Silently fail - don't bother the user on startup
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for updates", e)
                // Silently fail - don't bother the user on startup
            }
        }
    }


    /**
     * Handle back button press
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val handled = uiManager.onBackPressed()
            if (!handled) {
                // At top level, exit app
                finish()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


    /**
     * Override to prevent view hierarchy state restoration crash
     * when switching between portrait and landscape layouts.
     * ViewModel handles all important state preservation.
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // Skip the default view hierarchy restoration to prevent ScrollView state mismatch
        // between portrait (1 ScrollView) and landscape (2 ScrollViews) layouts.
        // All important app state is preserved by the ViewModel which survives config changes.
        try {
            super.onRestoreInstanceState(savedInstanceState)
        } catch (e: ClassCastException) {
            Log.w(TAG, "Skipping view hierarchy restoration due to layout mismatch: ${e.message}")
            // Ignore the exception - ViewModel has all the important state
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Always dismiss welcome dialog (it can't survive config changes due to custom View)
        welcomeDialog?.dismissSafely()
        welcomeDialog = null

        // Always dismiss current dialog to prevent window leaks
        // During config changes, the ViewModel's StateFlow will re-trigger dialog creation in the new Activity
        currentDialog?.dismissSafely()
        currentDialog = null
    }
}
