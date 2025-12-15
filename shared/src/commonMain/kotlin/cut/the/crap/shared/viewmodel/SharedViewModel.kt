package cut.the.crap.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cut.the.crap.shared.database.MediaEntry
import cut.the.crap.shared.repository.MediaRepository
import cut.the.crap.shared.ui.Channel
import cut.the.crap.shared.ui.MediaItem
import cut.the.crap.shared.ui.toMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Callback type aliases for platform-specific operations
 */
typealias PlayVideoCallback = (MediaEntry, Boolean) -> Unit
typealias DownloadVideoCallback = (MediaEntry, String, String) -> Unit
typealias ShowToastCallback = (String) -> Unit
typealias GetFilesPathCallback = () -> String
typealias GetAllChannelsCallback = () -> List<Channel>

/**
 * Shared ViewModel for KMP - contains all platform-independent business logic
 * Platform-specific operations are handled via callbacks injected at creation time
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SharedViewModel(
    val repository: MediaRepository,
    private val onPlayVideo: PlayVideoCallback = { _, _ -> },
    private val onDownloadVideo: DownloadVideoCallback = { _, _, _ -> },
    private val onShowToast: ShowToastCallback = { },
    private val getFilesPath: GetFilesPathCallback = { "" },
    private val getAllChannels: GetAllChannelsCallback = { emptyList() }
) : ViewModel() {

    companion object {
        private const val MAX_UI_ITEMS = 1200
    }

    // ===========================================================================================
    // STATE: Loading and Error Handling
    // ===========================================================================================

    private val _loadingState = MutableStateFlow(LoadingState.NOT_LOADED)
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0)
    val loadingProgress: StateFlow<Int> = _loadingProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String>("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    // ===========================================================================================
    // STATE: Compose UI Data
    // ===========================================================================================

    /**
     * All available channels - provided by platform callback
     */
    val channels: List<Channel> by lazy { getAllChannels() }

    /**
     * Internal state to track if database has content
     */
    private val _hasDataInDatabase = MutableStateFlow(false)

    /**
     * Computed StateFlow indicating if data is loaded and available
     */
    val isDataLoadedFlow: StateFlow<Boolean> = combine(
        _loadingState,
        _hasDataInDatabase
    ) { state, hasData ->
        state == LoadingState.LOADED || hasData
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    // ===========================================================================================
    // STATE: Search Progress
    // ===========================================================================================

    private val _isSearching = MutableStateFlow(false)

    fun setSearching(searching: Boolean) {
        _isSearching.value = searching
    }

    // ===========================================================================================
    // STATE: Dialog Management
    // ===========================================================================================

    private val _dialogState = MutableStateFlow<DialogState?>(null)
    val dialogState: StateFlow<DialogState?> = _dialogState.asStateFlow()

    fun showDialog(dialog: DialogState) {
        _dialogState.value = dialog
    }

    fun dismissDialog() {
        _dialogState.value = null
    }

    // ===========================================================================================
    // STATE: Navigation
    // ===========================================================================================

    private val _viewState = MutableStateFlow<ViewState>(ViewState.Themes())
    val viewState: StateFlow<ViewState> = _viewState.asStateFlow()

    // Computed properties from ViewState
    val selectedChannel: String? get() = _viewState.value.channel
    val selectedTheme: String? get() = _viewState.value.theme
    val selectedTitle: String? get() = (_viewState.value as? ViewState.Detail)?.title
    val currentMediaEntry: MediaEntry? get() = (_viewState.value as? ViewState.Detail)?.mediaEntry

    // Pagination
    private val _currentPart = MutableStateFlow(0)
    val currentPart: StateFlow<Int> = _currentPart.asStateFlow()

    // Date filter
    private val _dateFilter = MutableStateFlow(0L)
    private val _timePeriodId = MutableStateFlow(3)
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
            kotlinx.coroutines.delay(1000)
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Single unified content list for the right pane
     */
    val contentList: StateFlow<List<MediaEntry>> = combine(
        _viewState,
        _dateFilter,
        _currentPart,
        _loadingState
    ) { viewState, date, part, loadingState ->
        if (loadingState == LoadingState.LOADING) {
            return@combine null
        }
        when {
            viewState is ViewState.Themes && viewState.theme == null -> {
                ContentType.Themes(viewState.channel, date, part)
            }
            viewState is ViewState.Themes && viewState.theme != null -> {
                ContentType.Titles(viewState.channel, viewState.theme, date)
            }
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

    // ===========================================================================================
    // DATA QUERY METHODS
    // ===========================================================================================

    fun getThemesFlow(channelName: String?, limit: Int = MAX_UI_ITEMS): Flow<List<String>> {
        return if (channelName != null) {
            repository.getThemesForChannelFlow(channelName, limit = limit)
        } else {
            repository.getAllThemesFlow(limit = limit)
        }
    }

    fun getTitlesFlow(channelName: String?, theme: String): Flow<List<String>> {
        return if (channelName != null) {
            repository.getTitlesForChannelAndThemeFlow(channelName, theme, 0)
        } else {
            repository.getTitlesForThemeFlow(theme, 0)
        }
    }

    fun getMediaEntryByTitleFlow(
        title: String,
        channel: String? = null,
        theme: String? = null
    ): Flow<MediaEntry?> {
        return if (theme != null && channel != null) {
            repository.getMediaEntryFlow(channel, theme, title)
        } else if (theme != null) {
            repository.getMediaEntryByThemeAndTitleFlow(theme, title)
        } else {
            repository.getMediaEntryByTitleFlow(title)
        }
    }

    fun getMediaItemFlow(
        title: String,
        channel: String? = null,
        theme: String? = null
    ): Flow<MediaItem?> {
        return getMediaEntryByTitleFlow(title, channel, theme).map { entry ->
            entry?.toMediaItem()
        }
    }

    fun searchContentFlow(query: String, searchInTitles: Boolean): Flow<List<String>> {
        if (query.isBlank()) {
            return flowOf(emptyList())
        }

        return flow {
            val results = repository.searchEntries(query, 5000)
            val items = if (searchInTitles) {
                results.map { it.title }.distinct()
            } else {
                results.map { it.theme }.distinct()
            }
            emit(items)
        }.flowOn(Dispatchers.Default)
    }

    // ===========================================================================================
    // INITIALIZATION
    // ===========================================================================================

    init {
        // Check if database already has data
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val count = repository.getCount()
                val hasData = count > 0
                _hasDataInDatabase.value = hasData
                if (hasData) {
                    _loadingState.value = LoadingState.LOADED
                }
            } catch (e: Exception) {
                // Log error - platform-specific logging can be added via callback if needed
            }
        }
    }

    // ===========================================================================================
    // NAVIGATION ACTIONS
    // ===========================================================================================

    fun navigateToThemes(channel: String? = null, theme: String? = null) {
        val currentState = _viewState.value
        val currentSearch = when (currentState) {
            is ViewState.Themes -> currentState.searchFilter
            is ViewState.Detail -> currentState.searchFilter
        }
        val currentSelected = when (currentState) {
            is ViewState.Themes -> currentState.selectedItem
            is ViewState.Detail -> currentState.selectedItem
        }
        _viewState.value = ViewState.Themes(
            channel = channel,
            theme = theme,
            searchFilter = currentSearch,
            selectedItem = currentSelected
        )
    }

    fun navigateToDetail(title: String) {
        val currentState = _viewState.value
        viewModelScope.launch(Dispatchers.Default) {
            val entry = when {
                currentState.theme != null && currentState.channel != null -> {
                    repository.getMediaEntryFlow(currentState.channel!!, currentState.theme!!, title)
                }
                currentState.theme != null -> {
                    repository.getMediaEntryByThemeAndTitleFlow(currentState.theme!!, title)
                }
                else -> {
                    repository.getMediaEntryByTitleFlow(title)
                }
            }.firstOrNull()

            if (entry != null) {
                val searchFilter = when (currentState) {
                    is ViewState.Themes -> currentState.searchFilter
                    is ViewState.Detail -> currentState.searchFilter
                }
                val selectedItem = when (currentState) {
                    is ViewState.Themes -> currentState.selectedItem
                    is ViewState.Detail -> currentState.selectedItem
                }
                _viewState.value = ViewState.Detail(
                    mediaEntry = entry,
                    navigationChannel = currentState.channel,
                    navigationTheme = currentState.theme,
                    searchFilter = searchFilter,
                    selectedItem = selectedItem
                )
            }
        }
    }

    fun setSelectedItem(item: MediaEntry?) {
        val currentState = _viewState.value
        _viewState.value = when (currentState) {
            is ViewState.Themes -> currentState.copy(selectedItem = item)
            is ViewState.Detail -> currentState.copy(selectedItem = item)
        }
    }

    fun clearSelectedItem() {
        setSelectedItem(null)
    }

    fun setDateFilter(limitDate: Long, timePeriodId: Int) {
        _dateFilter.value = limitDate
        _timePeriodId.value = timePeriodId
    }

    fun setCurrentPart(part: Int) {
        _currentPart.value = part
    }

    fun nextPart() {
        _currentPart.value++
    }

    fun previousPart() {
        if (_currentPart.value > 0) {
            _currentPart.value--
        }
    }

    // ===========================================================================================
    // PLAYBACK & DOWNLOAD ACTIONS (via callbacks)
    // ===========================================================================================

    fun playVideo(entry: MediaEntry, isHighQuality: Boolean) {
        onPlayVideo(entry, isHighQuality)
    }

    fun downloadVideo(entry: MediaEntry, url: String, quality: String) {
        onDownloadVideo(entry, url, quality)
    }

    fun showToast(message: String) {
        onShowToast(message)
    }

    // ===========================================================================================
    // DATA LOADING
    // ===========================================================================================

    fun checkAndLoadMediaListToDatabase(privatePath: String): Boolean {
        var hasData = false
        viewModelScope.launch(Dispatchers.Default) {
            try {
                hasData = repository.checkAndLoadMediaList(privatePath)
                if (hasData) {
                    _hasDataInDatabase.value = true
                    _loadingState.value = LoadingState.LOADED
                }
            } catch (e: Exception) {
                _loadingState.value = LoadingState.ERROR
                _errorMessage.value = e.message ?: "Unknown error"
            }
        }
        return hasData
    }

    fun setLoadingState(state: LoadingState) {
        _loadingState.value = state
    }

    fun setLoadingProgress(progress: Int) {
        _loadingProgress.value = progress
    }

    fun setHasData(hasData: Boolean) {
        _hasDataInDatabase.value = hasData
    }

    suspend fun isDataLoaded(): Boolean {
        return repository.getCount() > 0
    }

    fun clearData() {
        viewModelScope.launch(Dispatchers.Default) {
            repository.deleteAll()
            _hasDataInDatabase.value = false
            _loadingState.value = LoadingState.NOT_LOADED
        }
    }
}
