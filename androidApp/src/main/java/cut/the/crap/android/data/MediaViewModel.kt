package cut.the.crap.android.data

import android.app.Application
import android.app.DownloadManager
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import cut.the.crap.android.R
import cut.the.crap.android.ui.dialog.DialogModel
import cut.the.crap.android.util.AppConfig
import cut.the.crap.android.util.MediaUrlUtils
import cut.the.crap.android.util.UpdateChecker
import cut.the.crap.android.repository.DownloadRepository
import cut.the.crap.android.repository.DownloadState
import cut.the.crap.android.repository.MediaRepository
import cut.the.crap.shared.repository.MediaRepository.LoadingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Streamlined ViewModel for media data - database-backed with reactive Flows
 * No synchronous cache, everything flows from Room database
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MediaViewModel(
    application: Application,
    internal val repository: MediaRepository,
    private val downloadRepository: DownloadRepository,
    private val updateChecker: UpdateChecker
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MediaViewModel"
        private const val MAX_UI_ITEMS = 1200
    }

    // ===========================================================================================
    // STATE: Loading and Error Handling
    // ===========================================================================================

    enum class LoadingState {
        NOT_LOADED,
        LOADING,
        LOADED,
        ERROR
    }

    private val _loadingState = MutableStateFlow(LoadingState.NOT_LOADED)
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0)
    val loadingProgress: StateFlow<Int> = _loadingProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String>("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    // ===========================================================================================
    // STATE: Search Progress (observable)
    // ===========================================================================================

    private val _isSearching = MutableStateFlow(false)

    /**
     * Set search progress state
     * Used by UIManager to indicate when database queries are running
     */
    fun setSearching(searching: Boolean) {
        _isSearching.value = searching
        Log.d(TAG, "Search state: ${if (searching) "ACTIVE" else "IDLE"}")
    }

    // ===========================================================================================
    // STATE: Dialog Management (reactive stream)
    // ===========================================================================================

    private val _dialogModel = MutableStateFlow<DialogModel?>(null)
    val dialogModel: StateFlow<DialogModel?> = _dialogModel.asStateFlow()

    /**
     * Show a dialog by posting a DialogModel
     */
    fun showDialog(model: DialogModel) {
        _dialogModel.value = model
    }

    /**
     * Dismiss the current dialog
     */
    fun dismissDialog() {
        _dialogModel.value = null
    }

    // Flag to trigger welcome dialog (one-time event for first launch)
    private val _showWelcomeDialog = MutableStateFlow(false)
    val showWelcomeDialog: StateFlow<Boolean> = _showWelcomeDialog.asStateFlow()

    fun welcomeDialogShown() {
        _showWelcomeDialog.value = false
    }

    // ===========================================================================================
    // STATE: Intent Management (one-time events)
    // ===========================================================================================

    private val _startActivityIntent = MutableSharedFlow<Intent>(replay = 0, extraBufferCapacity = 1)
    val startActivityIntent: SharedFlow<Intent> = _startActivityIntent.asSharedFlow()

    // ===========================================================================================
    // STATE: Navigation (survives configuration changes)
    // ===========================================================================================

    /**
     * Search filter configuration model
     * Separate queries for themes and titles mode to preserve context across navigation
     */
    data class SearchFilter(
        val themesQuery: String? = null,  // Search query in themes mode (controls UI visibility)
        val titlesQuery: String? = null   // Search query in titles mode (hidden filter)
    ) {
        val hasThemesSearch: Boolean get() = themesQuery != null
        val hasTitlesSearch: Boolean get() = titlesQuery != null
        val isEmpty: Boolean get() = themesQuery == null && titlesQuery == null
    }

    sealed interface ViewState {
        val channel: String?
        val theme: String?

        data class Themes(
            override val channel: String? = null,
            override val theme: String? = null,
            val searchFilter: SearchFilter = SearchFilter(),  // Separate queries for themes and titles
            val selectedItem: cut.the.crap.shared.database.MediaEntry? = null  // Unified: context determines if theme or title
        ) : ViewState
        data class Detail(
            val mediaEntry: cut.the.crap.shared.database.MediaEntry,
            val navigationChannel: String? = null,  // User's filter context (may be null for "All Themes")
            val navigationTheme: String? = null,     // User's current theme filter
            val searchFilter: SearchFilter = SearchFilter(),  // Preserved search filter
            val selectedItem: cut.the.crap.shared.database.MediaEntry? = null  // Unified: preserved from Themes state
        ) : ViewState {
            // These return the NAVIGATION context, not the media entry's actual channel/theme
            override val channel: String? get() = navigationChannel
            override val theme: String? get() = navigationTheme ?: mediaEntry.theme
            val title: String get() = mediaEntry.title
        }
    }

    private val _viewState = MutableStateFlow<ViewState>(ViewState.Themes())
    val viewState: StateFlow<ViewState> = _viewState.asStateFlow()

    // Computed properties from ViewState
    val selectedChannel: String? get() = _viewState.value.channel
    val selectedTheme: String? get() = _viewState.value.theme
    val selectedTitle: String? get() = (_viewState.value as? ViewState.Detail)?.title
    val currentMediaEntry: cut.the.crap.shared.database.MediaEntry? get() = (_viewState.value as? ViewState.Detail)?.mediaEntry


    // Pagination
    private val _currentPart = MutableStateFlow(0)
    val currentPart: StateFlow<Int> = _currentPart.asStateFlow()

    // Date filter
    private val _dateFilter = MutableStateFlow(0L)

    private val _timePeriodId = MutableStateFlow(3) // Default
    val timePeriodId: StateFlow<Int> = _timePeriodId.asStateFlow()

    // ===========================================================================================
    // REACTIVE DATA FLOWS from Database
    // ===========================================================================================

    /**
     * Database entry count as Flow
     */
    val entryCount: Flow<Int> = flow {
        while (true) {
            val count = repository.getCount()
            emit(count)
            kotlinx.coroutines.delay(1000) // Poll every second during loading
        }
    }.flowOn(Dispatchers.IO)


    /**
     * Single unified content list for the right pane
     * Returns full MediaEntry objects filtered by ViewState
     * - When viewing themes: returns one entry per theme (grouped)
     * - When viewing titles: returns one entry per title (grouped)
     * This replaces the separate themes/titles flows with a unified architecture
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val contentList: StateFlow<List<cut.the.crap.shared.database.MediaEntry>> = combine(
        _viewState,
        _dateFilter,
        _currentPart,
        _loadingState
    ) { viewState, date, part, loadingState ->
        // Skip database queries during bulk loading to avoid hundreds of unnecessary emissions
        if (loadingState == LoadingState.LOADING) {
            return@combine null
        }
        when {
            // Viewing themes (theme == null)
            viewState is ViewState.Themes && viewState.theme == null -> {
                ContentType.Themes(viewState.channel, date, part)
            }
            // Viewing titles (theme != null)
            viewState is ViewState.Themes && viewState.theme != null -> {
                ContentType.Titles(viewState.channel, viewState.theme, date)
            }
            // Not showing browse content (e.g., in Detail view)
            else -> null
        }
    }.flatMapLatest { contentType ->
        when (contentType) {
            is ContentType.Themes -> {
                val offset = contentType.part * MAX_UI_ITEMS
                if (contentType.channel != null) {
                    repository.getThemesForChannelAsEntriesFlow(
                        contentType.channel,
                        contentType.date,
                        MAX_UI_ITEMS,
                        offset
                    )
                } else {
                    repository.getAllThemesAsEntriesFlow(
                        contentType.date,
                        MAX_UI_ITEMS,
                        offset
                    )
                }
            }
            is ContentType.Titles -> {
                if (contentType.channel != null) {
                    repository.getTitlesForChannelAndThemeAsEntriesFlow(
                        contentType.channel,
                        contentType.theme,
                        contentType.date
                    )
                } else {
                    repository.getTitlesForThemeAsEntriesFlow(
                        contentType.theme,
                        contentType.date
                    )
                }
            }
            null -> flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    /**
     * Sealed class to represent different content types for the right pane
     */
    private sealed class ContentType {
        data class Themes(val channel: String?, val date: Long, val part: Int) : ContentType()
        data class Titles(val channel: String?, val theme: String, val date: Long) : ContentType()
    }

    // Legacy flows - kept for backward compatibility during migration
    // TODO: Remove after UIManager is updated
    @Deprecated("Use contentList instead", ReplaceWith("contentList"))
    val themes: StateFlow<List<cut.the.crap.shared.database.MediaEntry>> = contentList

    @Deprecated("Use contentList instead", ReplaceWith("contentList"))
    val titles: StateFlow<List<cut.the.crap.shared.database.MediaEntry>> = contentList

    // ===========================================================================================
    // INITIALIZATION
    // ===========================================================================================

    init {
        Log.d(TAG, "MediaViewModel CREATED - instance: ${this.hashCode()}")

        // Observe download state and convert to dialog models
        // This must be in init block AFTER all StateFlows are initialized
        viewModelScope.launch {
            downloadRepository.downloadState.collect { state ->
                handleDownloadState(state)
            }
        }
    }

    // ===========================================================================================
    // ACTIONS: Download Management
    // ===========================================================================================

    /**
     * Start downloading the media list file
     * @param url URL to download from
     * @param filename Local filename to save to
     */
    fun startMediaListDownload(url: String, filename: String) {
        Log.d(TAG, "Starting media list download: $url -> $filename")
        val started = downloadRepository.startDownload(url, filename)
        if (!started) {
            Log.w(TAG, "Download could not start - no network connection")
        }
    }

    /**
     * Handle download state changes and convert to dialog models
     */
    private fun handleDownloadState(state: DownloadState) {
        when (state) {
            is DownloadState.Idle -> {
                // No download in progress, dismiss any dialogs
                dismissDialog()
            }
            is DownloadState.Downloading -> {
                // Show download progress dialog
                showDialog(
                    DialogModel.Progress(
                        title = getApplication<Application>().getString(R.string.dialog_title_downloading),
                        message = getApplication<Application>().getString(
                            R.string.dialog_msg_downloading_progress,
                            state.downloadedMB.toFloat(),
                            state.totalMB.toFloat(),
                            state.speed,
                            state.progress
                        )
                    )
                )
            }
            is DownloadState.Success -> {
                // Download completed successfully
                dismissDialog()
                Log.i(TAG, "Download completed: ${state.filename}")
                if (state.shouldExtract) {
                    // Check if this is a diff file or full file
                    val isDiffFile = state.filename.contains(AppConfig.FILENAME_DIFF_XZ)

                    if (isDiffFile) {
                        Log.d(TAG, "Diff file downloaded, decompressing and applying to database")
                        // Decompress and apply diff
                        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val outputPath = state.filename.replace(".xz", "")
                                // Show decompression dialog
                                showDialog(
                                    DialogModel.Progress(
                                        title = getApplication<Application>().getString(R.string.dialog_title_decompressing),
                                        message = getApplication<Application>().getString(R.string.dialog_msg_decompressing)
                                    )
                                )
                                // Decompress
                                cut.the.crap.android.util.XZUtils.decode(state.filename, outputPath)
                                // Apply diff to database
                                applyDiffToDatabase(outputPath)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error decompressing diff file", e)
                                _loadingState.value = LoadingState.ERROR
                                _errorMessage.value = "Failed to decompress diff: ${e.message}"
                            }
                        }
                    } else {
                        Log.d(TAG, "Full list downloaded, triggering decompression and database loading")
                        // Trigger decompression and loading through the unified code path
                        // This uses the same guarded logic as startup, preventing duplicate decompression
                        val privatePath = getApplication<Application>().filesDir.absolutePath + "/"
                        checkAndLoadMediaListToDatabase(privatePath)
                    }
                }
            }
            is DownloadState.Error -> {
                // Download failed
                if (state.canRetry && state.retryAction != null) {
                    showDialog(
                        DialogModel.Error(
                            title = getApplication<Application>().getString(R.string.dialog_title_download_failed),
                            message = state.message,
                            retryLabel = getApplication<Application>().getString(R.string.btn_retry),
                            cancelLabel = getApplication<Application>().getString(R.string.btn_cancel),
                            onRetry = state.retryAction,
                            onCancel = {
                                downloadRepository.resetState()
                            }
                        )
                    )
                } else {
                    showDialog(
                        DialogModel.Message(
                            title = getApplication<Application>().getString(R.string.dialog_title_download_failed),
                            message = state.message
                        )
                    )
                }
            }
            is DownloadState.Extracting -> {
                // Show extraction progress
                showDialog(
                    DialogModel.Progress(
                        title = getApplication<Application>().getString(R.string.dialog_title_decompressing),
                        message = getApplication<Application>().getString(R.string.dialog_msg_decompressing)
                    )
                )
            }
            is DownloadState.ExtractionComplete -> {
                // Extraction completed, load to database
                // Don't dismiss dialog - let loadingState update it to show database loading progress
                Log.i(TAG, "Extraction completed: ${state.extractedFile}")
                Log.i(TAG, "Loading media list to database with chunked parsing")
                loadMediaListToDatabase(state.extractedFile)
            }
        }
    }

    // ===========================================================================================
    // ACTIONS: Data Loading
    // ===========================================================================================

    /**
     * Load media list from file to database (chunked, memory-efficient)
     * Uses Flow for reactive progress updates
     *
     * IMPORTANT: Uses ProcessLifecycleOwner scope to survive Activity destruction.
     * This ensures parsing continues even when the app goes to background.
     */
    fun loadMediaListToDatabase(filePath: String) {
        Log.i(TAG, "Starting chunked parse to database: $filePath")
        _loadingState.value = LoadingState.LOADING

        // Use ProcessLifecycleOwner scope instead of viewModelScope
        // This survives Activity destruction but gets cancelled when app process is killed
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            try {
                repository.loadMediaListFromFile(filePath).collect { result ->
                    when (result) {
                        is LoadingResult.Progress -> {
                            _loadingProgress.value = result.entriesLoaded
                        }
                        is LoadingResult.Complete -> {
                            Log.i(TAG, "Successfully loaded ${result.totalEntries} entries to database")
                            _loadingState.value = LoadingState.LOADED
                            _loadingProgress.value = result.totalEntries

                            // Delete the uncompressed file to save storage space
                            // Keep the .xz compressed file for potential re-parsing
                            try {
                                val uncompressedFile = File(filePath)
                                if (uncompressedFile.exists() && !filePath.endsWith(".xz")) {
                                    val deleted = uncompressedFile.delete()
                                    if (deleted) {
                                        Log.i(TAG, "Deleted uncompressed file to save space: $filePath")
                                    } else {
                                        Log.w(TAG, "Failed to delete uncompressed file: $filePath")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error deleting uncompressed file: $filePath", e)
                            }
                        }
                        is LoadingResult.Error -> {
                            Log.e(TAG, "Error loading media list", result.exception)
                            _loadingState.value = LoadingState.ERROR
                            _errorMessage.value = result.exception.message ?: "Unknown error"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting media list flow", e)
                _loadingState.value = LoadingState.ERROR
                _errorMessage.value = e.message ?: "Unknown error"
            }
        }
    }

    /**
     * Check if media list exists and load it to database
     * Strategy:
     * 1. Check if database already has data (most common case after first run)
     * 2. Check if compressed .xz file exists (decompress and load)
     * 3. Otherwise, trigger download
     */
    fun checkAndLoadMediaListToDatabase(privatePath: String): Boolean {
        // First check if database already has data
        // Use runBlocking since this is called during synchronous startup flow
        val dataLoaded = runBlocking {
            isDataLoaded()
        }

        if (dataLoaded) {
            Log.i(TAG, "Database already contains data, skipping file check")
            return true
        }

        // Check if already loading (decompression or parsing in progress)
        // This prevents multiple decompression jobs from starting during config changes
        if (_loadingState.value == LoadingState.LOADING) {
            Log.i(TAG, "Data loading already in progress, skipping duplicate start")
            return true
        }

        // Database is empty - check for compressed .xz file
        val xzPath = privatePath + AppConfig.FILENAME_XZ
        val xzFile = File(xzPath)

        return if (xzFile.exists()) {
            Log.i(TAG, "Found compressed file, decompressing and loading to database")
            // Set loading state BEFORE launching the job to prevent duplicate starts
            _loadingState.value = LoadingState.LOADING

            // Trigger async decompression and loading using ProcessLifecycleOwner
            // This ensures the operation survives Activity recreation during config changes
            ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val outputPath = xzPath.replace(".xz", "")
                    // Show decompression dialog
                    showDialog(
                        DialogModel.Progress(
                            title = getApplication<Application>().getString(R.string.dialog_title_decompressing),
                            message = getApplication<Application>().getString(R.string.dialog_msg_decompressing)
                        )
                    )
                    // Decompress
                    cut.the.crap.android.util.XZUtils.decode(xzPath, outputPath)
                    // Load to database
                    loadMediaListToDatabase(outputPath)
                } catch (e: Exception) {
                    Log.e(TAG, "Error decompressing file", e)
                    _loadingState.value = LoadingState.ERROR
                    _errorMessage.value = "Failed to decompress media list: ${e.message}"
                }
            }
            true
        } else {
            Log.w(TAG, "No data in database and no compressed file found, need to trigger download")
            // Show welcome dialog for first-time setup through a flag
            // The Activity will observe this and show the appropriate dialog
            _loadingState.value = LoadingState.NOT_LOADED  // Reset to initial state
            _showWelcomeDialog.value = true
            false
        }
    }

    /**
     * Check if data is loaded in database
     */
    suspend fun isDataLoaded(): Boolean = withContext(Dispatchers.IO) {
        try {
            val count = repository.getCount()
            Log.d(TAG, "Database contains $count entries")
            count > 0
        } catch (e: Exception) {
            Log.w(TAG, "Error checking database: ${e.message}")
            false
        }
    }

    /**
     * Clear all data from database
     */
    fun clearData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteAll()
                _loadingState.value = LoadingState.NOT_LOADED
                _loadingProgress.value = 0
                Log.i(TAG, "Database cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing database", e)
            }
        }
    }

    /**
     * Check for available updates using differential update mechanism
     *
     * Server-Friendly Implementation:
     * - Only checks if the configured interval has elapsed (default: 4 hours)
     * - Uses HTTP HEAD requests (very lightweight, ~1KB vs ~50MB full download)
     * - Compares Last-Modified, ETag, and Content-Length headers
     * - Returns result indicating whether update is available
     *
     * @param forceCheck If true, bypass the interval check and check immediately
     * @return UpdateCheckResult indicating the outcome
     */
    suspend fun checkForUpdate(forceCheck: Boolean = false): UpdateChecker.UpdateCheckResult {
        return withContext(Dispatchers.IO) {
            // Determine if we should use diff or full download
            val hasData = isDataLoaded()
            val useDiff = hasData // Use diff if database already has data

            Log.d(TAG, "Checking for update (hasData: $hasData, useDiff: $useDiff)")
            updateChecker.checkForUpdate(forceCheck, useDiff)
        }
    }

    /**
     * Start downloading media list with automatic full/diff selection
     * If database has data, downloads diff file. Otherwise downloads full file.
     */
    fun startSmartMediaListDownload() {
        viewModelScope.launch {
            val hasData = isDataLoaded()
            val privatePath = getApplication<Application>().filesDir.absolutePath + "/"

            if (hasData) {
                // Database has data - download diff
                Log.i(TAG, "Database has data, downloading diff file")
                val diffUrl = AppConfig.HOST_FILE_DIFF
                val diffFilename = privatePath + AppConfig.FILENAME_DIFF_XZ
                startMediaListDownload(diffUrl, diffFilename)
            } else {
                // Database empty - download full list
                Log.i(TAG, "Database empty, downloading full list")
                val fullUrl = AppConfig.HOST_FILE
                val fullFilename = privatePath + AppConfig.FILENAME_XZ
                startMediaListDownload(fullUrl, fullFilename)
            }
        }
    }

    /**
     * Apply diff file to database (incremental update)
     * Uses Flow for reactive progress updates
     */
    fun applyDiffToDatabase(filePath: String) {
        Log.i(TAG, "Starting diff application to database: $filePath")
        _loadingState.value = LoadingState.LOADING

        ProcessLifecycleOwner.get().lifecycleScope.launch {
            try {
                repository.applyDiffToDatabase(filePath).collect { result ->
                    when (result) {
                        is LoadingResult.Progress -> {
                            _loadingProgress.value = result.entriesLoaded
                        }
                        is LoadingResult.Complete -> {
                            Log.i(TAG, "Successfully applied ${result.totalEntries} diff entries to database")
                            _loadingState.value = LoadingState.LOADED
                            _loadingProgress.value = result.totalEntries

                            // Delete the uncompressed diff file to save storage space
                            try {
                                val uncompressedFile = File(filePath)
                                if (uncompressedFile.exists() && !filePath.endsWith(".xz")) {
                                    val deleted = uncompressedFile.delete()
                                    if (deleted) {
                                        Log.i(TAG, "Deleted uncompressed diff file: $filePath")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error deleting uncompressed diff file: $filePath", e)
                            }
                        }
                        is LoadingResult.Error -> {
                            Log.e(TAG, "Error applying diff", result.exception)
                            _loadingState.value = LoadingState.ERROR
                            _errorMessage.value = result.exception.message ?: "Unknown error"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting diff application flow", e)
                _loadingState.value = LoadingState.ERROR
                _errorMessage.value = e.message ?: "Unknown error"
            }
        }
    }

    // ===========================================================================================
    // ACTIONS: Navigation State
    // ===========================================================================================

    fun navigateToThemes(channel: String? = null, theme: String? = null) {
        // Preserve search filter and selected item when navigating
        val currentState = _viewState.value

        // Handle search filter based on navigation context
        val searchFilter = when (currentState) {
            is ViewState.Themes -> {
                when {
                    // Entering title mode from theme mode: keep queries separate
                    theme != null && currentState.theme == null -> {
                        SearchFilter(
                            themesQuery = currentState.searchFilter.themesQuery,
                            titlesQuery = currentState.searchFilter.titlesQuery  // Preserve titlesQuery
                        )
                    }
                    // Returning to theme mode from title mode: preserve both
                    theme == null && currentState.theme != null -> {
                        SearchFilter(
                            themesQuery = currentState.searchFilter.themesQuery,
                            titlesQuery = currentState.searchFilter.titlesQuery  // Keep titlesQuery
                        )
                    }
                    // Other cases: preserve as-is
                    else -> {
                        currentState.searchFilter
                    }
                }
            }
            is ViewState.Detail -> currentState.searchFilter  // ✅ Restore from Detail!
        }

        val selectedItem = when (currentState) {
            is ViewState.Themes -> currentState.selectedItem
            is ViewState.Detail -> currentState.selectedItem  // ✅ Restore from Detail!
        }

        _viewState.value = ViewState.Themes(
            channel = channel,
            theme = theme,
            searchFilter = searchFilter,
            selectedItem = selectedItem
        )
        _currentPart.value = 0
        Log.d(TAG, "Navigated to themes view (channel: $channel, theme: $theme, themesQuery: ${searchFilter.themesQuery}, titlesQuery: ${searchFilter.titlesQuery})")
    }

    /**
     * Update selected item in ViewState
     * Called when user selects a theme or title from the list
     * Context (themes vs titles) is determined by whether theme is null
     */
    fun setSelectedItem(item: cut.the.crap.shared.database.MediaEntry?) {
        val currentState = _viewState.value
        if (currentState is ViewState.Themes) {
            _viewState.value = currentState.copy(selectedItem = item)
            val displayName = if (currentState.theme == null) item?.theme else item?.title
            Log.d(TAG, "Selected item: $displayName")
        }
    }

    /**
     * Clear cached selected item
     * Called when navigating to a different context
     */
    fun clearSelectedItem() {
        val currentState = _viewState.value
        if (currentState is ViewState.Themes) {
            _viewState.value = currentState.copy(selectedItem = null)
            Log.d(TAG, "Cleared selected item")
        }
    }

    /**
     * Update search query text in ViewState
     * Updates themesQuery in theme mode, titlesQuery in title mode
     * Stores query as-is (with trailing spaces) for UI display
     * Trimming happens when the query is used for filtering/searching
     */
    fun updateSearchQuery(query: String) {
        val currentState = _viewState.value
        if (currentState is ViewState.Themes) {
            val inTitleMode = currentState.theme != null

            val newFilter = if (inTitleMode) {
                // Title mode: update titlesQuery
                if (query.isEmpty()) {
                    SearchFilter(themesQuery = currentState.searchFilter.themesQuery, titlesQuery = null)
                } else {
                    SearchFilter(themesQuery = currentState.searchFilter.themesQuery, titlesQuery = query)
                }
            } else {
                // Theme mode: update themesQuery
                if (query.isEmpty()) {
                    SearchFilter(themesQuery = null, titlesQuery = currentState.searchFilter.titlesQuery)
                } else {
                    SearchFilter(themesQuery = query, titlesQuery = currentState.searchFilter.titlesQuery)
                }
            }

            _viewState.value = currentState.copy(searchFilter = newFilter)
            Log.d(TAG, "Updated search filter (${if (inTitleMode) "title" else "theme"} mode): themesQuery='${newFilter.themesQuery}', titlesQuery='${newFilter.titlesQuery}'")
        }
    }


    fun navigateToDetail(title: String) {
        val currentState = _viewState.value

        // Capture navigation context (user's filter state, not media entry's actual data)
        val navigationChannel = currentState.channel  // May be null for "All Themes"
        val navigationTheme = currentState.theme ?: ""

        // Preserve cached selection and search filter from Themes state
        val selectedItem = if (currentState is ViewState.Themes) currentState.selectedItem else null
        val searchFilter = if (currentState is ViewState.Themes) currentState.searchFilter else SearchFilter()

        // Load media entry asynchronously
        viewModelScope.launch {
            val entry = when {
                navigationChannel != null && navigationTheme.isNotEmpty() -> {
                    repository.getMediaEntryFlow(navigationChannel, navigationTheme, title).first()
                }
                navigationTheme.isNotEmpty() -> {
                    repository.getMediaEntryByThemeAndTitleFlow(navigationTheme, title).first()
                }
                else -> null
            }

            if (entry != null) {
                // ✅ Store navigation context separately from media entry data
                _viewState.value = ViewState.Detail(
                    mediaEntry = entry,
                    navigationChannel = navigationChannel,  // User's filter (may be null)
                    navigationTheme = navigationTheme,
                    searchFilter = searchFilter,  // ✅ Preserve search filter!
                    selectedItem = selectedItem
                )
                Log.d(TAG, "Navigated to detail view (navigationChannel: $navigationChannel, navigationTheme: $navigationTheme, title: $title, themesQuery: ${searchFilter.themesQuery}, titlesQuery: ${searchFilter.titlesQuery}, entryChannel: ${entry.channel})")
            } else {
                Log.e(TAG, "Failed to load media entry for title: $title")
            }
        }
    }


    // ===========================================================================================
    // ACTIONS: Filters and Pagination
    // ===========================================================================================

    fun setDateFilter(limitDate: Long, timePeriodId: Int) {
        _dateFilter.value = limitDate
        _timePeriodId.value = timePeriodId
        Log.d(TAG, "Date filter set: $limitDate (period: $timePeriodId)")
    }

    fun setCurrentPart(part: Int) {
        _currentPart.value = part
        Log.d(TAG, "Current part set to: $part")
    }

    fun nextPart() {
        _currentPart.value += 1
        Log.d(TAG, "Advanced to part: ${_currentPart.value}")
    }

    fun previousPart() {
        if (_currentPart.value > 0) {
            _currentPart.value -= 1
            Log.d(TAG, "Back to part: ${_currentPart.value}")
        }
    }

    // ===========================================================================================
    // ACTIONS: Video Playback
    // ===========================================================================================

    /**
     * Handle play button click - stream the video in ExoPlayer
     * @param isHighQuality Whether to play high quality version
     */
    fun onPlayButtonClicked(isHighQuality: Boolean) {
        val mediaEntry = currentMediaEntry
        if (mediaEntry == null) {
            Log.w(TAG, "Play button clicked but no media entry available")
            Toast.makeText(
                getApplication(),
                getApplication<Application>().getString(R.string.error_no_media_selected),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Log quality selection and URL details
        val qualitySelected = if (isHighQuality) "HIGH" else "LOW"
        Log.d(TAG, "=".repeat(80))
        Log.d(TAG, "PLAY BUTTON CLICKED - Video Playback URL Selection")
        Log.d(TAG, "=".repeat(80))
        Log.d(TAG, "Quality selected: $qualitySelected")
        Log.d(TAG, "Main URL: ${mediaEntry.url}")
        Log.d(TAG, "Small URL (raw): ${mediaEntry.smallUrl}")
        Log.d(TAG, "HD URL (raw): ${mediaEntry.hdUrl}")
        Log.d(TAG, "-".repeat(80))

        // TEMPORARY FIX: Force using main URL for BR.de videos due to quality URL availability issues
        // TODO: Implement proper fallback mechanism or quality detection
        val forceMainUrl = mediaEntry.url.contains("cdn-storage.br.de")

        // Get the appropriate video URL based on quality selection
        // URLs may be in pipe-delimited format that needs reconstruction using base URL
        // NOTE: Some servers (like BR.de) may not have all quality versions available
        val videoUrl = if (forceMainUrl) {
            Log.w(TAG, "Forcing main URL for BR.de video (quality-specific URLs may not be available)")
            Log.d(TAG, "Using main URL: ${mediaEntry.url}")
            MediaUrlUtils.cleanMediaUrl(mediaEntry.url)
        } else if (isHighQuality) {
            // Try HD URL if available
            if (mediaEntry.hdUrl.isNotEmpty()) {
                val hdUrl = mediaEntry.hdUrl
                Log.d(TAG, "Using HD quality: $hdUrl")
                val reconstructed = MediaUrlUtils.reconstructUrl(hdUrl, mediaEntry.url)
                Log.d(TAG, "HD URL reconstructed: $reconstructed")

                // Check if reconstruction resulted in same URL as main (meaning HD not available)
                if (reconstructed == MediaUrlUtils.cleanMediaUrl(mediaEntry.url)) {
                    Log.w(TAG, "HD URL same as main URL, using main URL")
                    MediaUrlUtils.cleanMediaUrl(mediaEntry.url)
                } else {
                    Log.w(TAG, "NOTE: Using HD quality URL. If playback fails, the HD version may not exist on server.")
                    reconstructed
                }
            } else {
                // No HD URL, fall back to main URL
                Log.d(TAG, "No HD URL available, using main URL")
                MediaUrlUtils.cleanMediaUrl(mediaEntry.url)
            }
        } else {
            // Try small URL if available
            if (mediaEntry.smallUrl.isNotEmpty()) {
                val smallUrl = mediaEntry.smallUrl
                Log.d(TAG, "Using LOW quality: $smallUrl")
                val reconstructed = MediaUrlUtils.reconstructUrl(smallUrl, mediaEntry.url)
                Log.d(TAG, "Small URL reconstructed: $reconstructed")

                // Check if reconstruction resulted in same URL as main
                if (reconstructed == MediaUrlUtils.cleanMediaUrl(mediaEntry.url)) {
                    Log.w(TAG, "Small URL same as main URL, using main URL")
                    MediaUrlUtils.cleanMediaUrl(mediaEntry.url)
                } else {
                    Log.w(TAG, "NOTE: Using LOW quality URL. If playback fails, the low quality version may not exist on server.")
                    reconstructed
                }
            } else {
                // No small URL, fall back to main URL
                Log.d(TAG, "No small URL available, using main URL")
                MediaUrlUtils.cleanMediaUrl(mediaEntry.url)
            }
        }

        if (videoUrl.isEmpty()) {
            Log.w(TAG, "No video URL available for: ${mediaEntry.title}")
            Toast.makeText(
                getApplication(),
                getApplication<Application>().getString(R.string.error_no_video_url),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        Log.d(TAG, "Final video URL to play: $videoUrl")
        Log.d(TAG, "=".repeat(80))
        Log.i(TAG, "Playing video: ${mediaEntry.title} -> $videoUrl")

        // Play video by emitting Intent
        playVideo(videoUrl, mediaEntry.title)
    }

    /**
     * Create and emit Intent to play video using in-app ExoPlayer
     * Supports HTTP/HTTPS streaming
     *
     * @param mediaUrl The video URL to play
     * @param videoTitle The title to display in the player
     */
    private fun playVideo(mediaUrl: String, videoTitle: String) {
        // Clean and validate the URL
        val cleanUrl = MediaUrlUtils.cleanMediaUrl(mediaUrl)
        if (cleanUrl.isEmpty()) {
            Log.e(TAG, "Invalid URL after cleaning: $mediaUrl")
            Toast.makeText(
                getApplication(),
                getApplication<Application>().getString(R.string.error_invalid_video_url),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        Log.d(TAG, "Clean URL: $cleanUrl")
        Log.d(TAG, "Protocol: ${if (cleanUrl.startsWith("https://")) "HTTPS (secure)" else "HTTP (cleartext)"}")

        // Create Intent for VideoPlayerActivity
        // Note: We can't use VideoPlayerActivity::class.java here in ViewModel
        // So we'll use the class name string and the Activity will resolve it
        val intent = Intent().apply {
            setClassName(
                getApplication<Application>().packageName,
                "cut.the.crap.android.android.ui.VideoPlayerActivity"
            )
            putExtra("video_url", cleanUrl)
            putExtra("video_title", videoTitle)
        }

        // Emit intent to be collected by Activity
        viewModelScope.launch {
            Log.i(TAG, "Emitting Intent for VideoPlayerActivity")
            Log.d(TAG, "Video URL: $cleanUrl")
            Log.d(TAG, "Video title: $videoTitle")
            _startActivityIntent.emit(intent)
        }
    }

    // ===========================================================================================
    // ACTIONS: Video Download
    // ===========================================================================================

    /**
     * Handle download button click - download the video file
     * @param isHighQuality Whether to download high quality version
     */
    fun onDownloadButtonClicked(isHighQuality: Boolean) {
        val mediaEntry = currentMediaEntry
        if (mediaEntry == null) {
            Log.w(TAG, "Download button clicked but no media entry available")
            Toast.makeText(
                getApplication(),
                getApplication<Application>().getString(R.string.error_no_media_selected),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Get the appropriate video URL based on quality selection
        // URLs may be in pipe-delimited format that needs reconstruction using base URL
        val videoUrl = if (isHighQuality) {
            val hdUrl = mediaEntry.hdUrl.ifEmpty { mediaEntry.url }
            // HD URL might be pipe-delimited, use main URL as base
            MediaUrlUtils.reconstructUrl(hdUrl, mediaEntry.url)
        } else {
            val smallUrl = mediaEntry.smallUrl.ifEmpty { mediaEntry.url }
            // Small URL is often pipe-delimited, use main URL as base
            MediaUrlUtils.reconstructUrl(smallUrl, mediaEntry.url)
        }

        if (videoUrl.isEmpty()) {
            Log.w(TAG, "No video URL available for: ${mediaEntry.title}")
            Toast.makeText(
                getApplication(),
                getApplication<Application>().getString(R.string.error_no_video_url),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        Log.i(TAG, "Downloading video: ${mediaEntry.title} -> $videoUrl")

        // Sanitize filename - remove invalid characters
        val sanitizedTitle = mediaEntry.title
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(100) // Limit filename length

        // Determine file extension from URL or default to .mp4
        val fileExtension = when {
            videoUrl.contains(".mp4", ignoreCase = true) -> ".mp4"
            videoUrl.contains(".m3u8", ignoreCase = true) -> ".m3u8"
            videoUrl.contains(".webm", ignoreCase = true) -> ".webm"
            else -> ".mp4"
        }

        // Use Android's DownloadManager for robust downloading
        val downloadManager = getApplication<Application>()
            .getSystemService(android.content.Context.DOWNLOAD_SERVICE) as? DownloadManager
        if (downloadManager == null) {
            Toast.makeText(
                getApplication(),
                getApplication<Application>()
                    .getString(R.string.error_download_manager_unavailable),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            val request = DownloadManager.Request(videoUrl.toUri()).apply {
                setTitle(mediaEntry.title)
                setDescription("${mediaEntry.channel} - ${mediaEntry.theme}")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(
                    android.os.Environment.DIRECTORY_DOWNLOADS,
                    "Kuckmal/${mediaEntry.channel}/${sanitizedTitle}${fileExtension}"
                )
                setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or
                    DownloadManager.Request.NETWORK_MOBILE
                )
            }

            val downloadId = downloadManager.enqueue(request)
            Log.i(TAG, "Download started with ID: $downloadId for ${mediaEntry.title}")

            Toast.makeText(
                getApplication(),
                "Downloading: ${mediaEntry.title}\nQuality: ${if (isHighQuality) "High" else "Low"}",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Log.e(TAG, "Error starting download", e)
            Toast.makeText(
                getApplication(),
                "Error starting download: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    /**
     * Log cache statistics for debugging and monitoring
     * Call this periodically or after searches to monitor cache performance
     */
    fun logCacheStats() {
        val stats = repository.getCacheStats()
        Log.i(TAG, "=== Search Cache Statistics ===")
        Log.i(TAG, "Cache Size: ${stats.size}/${stats.maxSize}")
        Log.i(TAG, "Hit Rate: ${(stats.hitRate * 100).toInt()}%")
        Log.i(TAG, "Hits: ${stats.hitCount}, Misses: ${stats.missCount}")
        Log.i(TAG, "================================")
    }

    override fun onCleared() {
        super.onCleared()
        // Log final cache stats before cleanup
        logCacheStats()
        Log.d(TAG, "MediaViewModel DESTROYED - instance: ${this.hashCode()}")
    }
}
