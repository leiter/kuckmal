package com.mediathekview.android.ui

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mediathekview.android.R
import com.mediathekview.android.data.MediaViewModel
import com.mediathekview.android.model.Broadcaster
import com.mediathekview.android.ui.adapter.BroadCasterListAdapter
import com.mediathekview.android.ui.adapter.MediaBrowseAdapter
import com.mediathekview.android.util.FormatUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages UI components - refactored for side-by-side browse layout
 */
class UIManager(
    private val activity: FragmentActivity,
    private val viewModel: MediaViewModel,
) {

    // Detect if device is a TV for adaptive UI behavior
    private val isTvDevice: Boolean by lazy {
        val uiModeManager = activity.getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
        val currentMode = uiModeManager.currentModeType
        (currentMode == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION).also { isTV ->
            Log.d(TAG, "Device mode: ${if (isTV) "TV" else "Mobile/Tablet"}")
        }
    }

    // Adaptive debounce delay: TV devices use shorter delay (faster response with remote input)
    private val searchDebounceDelayMs: Long
        get() = if (isTvDevice) 300L else 1200L

    // Check if device is in landscape orientation
    private val isLandscape: Boolean
        get() = activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Track whether search UI is currently visible/active
    // This state persists when switching between themes and titles view
    private var isSearchActive = false

    // Main views
    // Get main views
    private val browseView: View = activity.findViewById(R.id.browse_view)
    private val detailView: View = activity.findViewById(R.id.detail_view)

    // Browse view components
    private val channelListView: ListView = activity.findViewById(R.id.channelList)
    private val contentRecyclerView: RecyclerView = activity.findViewById(R.id.contentList)
    private val rightPanelTitle: TextView = activity.findViewById(R.id.rightPanelTitle)
    private val searchToggleButton: ImageButton = activity.findViewById(R.id.searchToggleButton)
    private val stickySearchContainer: View = activity.findViewById(R.id.stickySearchContainer)
    private val searchButton: ImageButton = activity.findViewById(R.id.searchButton)
    private val searchLoadingSpinner: android.widget.ProgressBar = activity.findViewById(R.id.searchLoadingSpinner)
    private val menuButton: ImageButton = activity.findViewById(R.id.menuButton)
    private val searchInput: EditText = activity.findViewById(R.id.searchInput)
    private val searchClearButton: ImageButton = activity.findViewById(R.id.searchClearButton)

    // Detail view components

    // Detail view text fields
    private val themeText: TextView = activity.findViewById(R.id.dvThema)
    private val titleText: TextView = activity.findViewById(R.id.dvTitel)
    private val dateText: TextView = activity.findViewById(R.id.dvDatum)
    private val timeText: TextView = activity.findViewById(R.id.dvZeit)
    private val durationText: TextView = activity.findViewById(R.id.dvDauer)
    private val sizeText: TextView = activity.findViewById(R.id.dvGroesse)
    private val descriptionText: TextView = activity.findViewById(R.id.dvBeschreibung)
    private val channelImg: ImageView = activity.findViewById(R.id.dvSenderImg)
    private val showMoreButton: View = activity.findViewById(R.id.showMoreButton)
    private val showMoreText: TextView = activity.findViewById(R.id.showMoreText)
    private val showMoreChevron: ImageView = activity.findViewById(R.id.showMoreChevron)
    private val descriptionContainer: View = activity.findViewById(R.id.descriptionContainer)

    // Quality radio buttons
    private val qualityHigh: RadioButton = activity.findViewById(R.id.qualityHigh)

    // Play and download buttons
    private val playButton: ImageButton? = activity.findViewById(R.id.play)
    private val downloadButton: ImageButton? = activity.findViewById(R.id.download)

    // Data adapters
    // Initialize with exact capacity (19 channels) to avoid resizing
    private val broadCasterListData = ArrayList<Broadcaster>(Broadcaster.channelListArray.size)
    private val broadCasterListAdapter: BroadCasterListAdapter
    private val contentAdapter: MediaBrowseAdapter

    // Flag to prevent infinite loop when updating search input from viewState
    private var isUpdatingSearchFromViewState = false

    // Track current search job for cancellation
    private var currentSearchJob: Job? = null

    // Track debounce job for search input
    private var debounceJob: Job? = null

    // Track pending search runnable for cancellation
    private var pendingSearchRunnable: Runnable? = null

    init {

        // Setup adapters
        broadCasterListData.addAll(Broadcaster.channelListArray)
        broadCasterListAdapter = BroadCasterListAdapter(activity, broadCasterListData)
        channelListView.adapter = broadCasterListAdapter

        // Clear any initial selection from the channel list
        // Post to ensure it runs after the view is fully laid out
        channelListView.post {
            channelListView.clearChoices()
            broadCasterListAdapter.notifyDataSetChanged()
        }

        // Setup RecyclerView with adapter
        contentAdapter = MediaBrowseAdapter { item, position ->
            onContentItemClick(item, position)
        }
        contentRecyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = contentAdapter
            // Enable item animations for smooth transitions
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
                addDuration = 400L
                removeDuration = 400L
                moveDuration = 400L
                changeDuration = 400L
            }
        }

        setupListeners()

        // Set up viewState observer to sync search input with query from viewState
        setupViewStateObserver()

        // Configure title line count based on orientation
        updateTitleLineCount()

        // Initialize browse view to show channels and empty content list
        // This ensures the UI is visible even before media list is loaded
        showBrowseView(animated = false)

        // Set initial focus for TV/DPAD navigation on menu button (top bar)
        activity.window.decorView.post {
            menuButton.requestFocus()
        }
    }

    /**
     * Update the title line count based on device orientation
     * Landscape: 1 line (more horizontal space)
     * Portrait: 2 lines (compact & polished header)
     */
    private fun updateTitleLineCount() {
        rightPanelTitle.maxLines = (if (isLandscape) 1 else 3)
    }

    private fun setupListeners() {
        // Channel list item click
        channelListView.setOnItemClickListener { _, _, position, _ ->
            onChannelSelected(position)
        }

        // Play button click
        playButton?.setOnClickListener {
            viewModel.onPlayButtonClicked(isQualityHigh())
        }

        // Download button click
        downloadButton?.setOnClickListener {
            viewModel.onDownloadButtonClicked(isQualityHigh())
        }

        // Search toggle button click (in header)
        searchToggleButton.setOnClickListener {
            if (stickySearchContainer.isVisible) {
                deactivateSearch()
            } else {
                activateSearch()
            }
        }

        // Search button click (in sticky container) - same as toggle
        searchButton.setOnClickListener {
            if (stickySearchContainer.isVisible) {
                deactivateSearch()
            } else {
                activateSearch()
            }
        }

        // Search clear/close button click
        searchClearButton.setOnClickListener {
            // Just clear the input, don't deactivate search
            searchInput.text.clear()
        }

        // Search input text change listener with debouncing
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Don't trigger search if we're updating from viewState
                if (!isUpdatingSearchFromViewState) {
                    val query = s?.toString() ?: ""

                    // Cancel any existing debounce timer
                    debounceJob?.cancel()

                    // Start a new debounce timer
                    debounceJob = activity.lifecycleScope.launch {
                        // Wait for the debounce delay (adaptive: 300ms on TV, 1200ms on mobile)
                        delay(searchDebounceDelayMs)

                        // Update ViewModel with the query
                        viewModel.updateSearchQuery(query)
                        // Perform the search
                        performSearch(query)
                    }
                }
            }
        })

        // Menu button click
        menuButton.setOnClickListener { view ->
            showOverflowMenu(view)
        }

        // Description expand/collapse click
        setupCollapsibleDescription()
    }

    /**
     * Set up observer for viewState changes to sync search input
     * Reads search filter from viewState and updates search UI
     */
    private fun setupViewStateObserver() {
        activity.lifecycleScope.launch {
            viewModel.viewState.collect { viewState ->
                if (viewState is MediaViewModel.ViewState.Themes) {
                    // Get search filter from viewState
                    val searchFilter = viewState.searchFilter
                    val inTitleMode = viewState.theme != null

                    // In title mode: show titlesQuery in search input
                    // In theme mode: show themesQuery in search input
                    val displayQuery = if (inTitleMode) {
                        searchFilter.titlesQuery ?: ""
                    } else {
                        searchFilter.themesQuery ?: ""
                    }

                    // Update search input if it differs from display query
                    if (searchInput.text.toString() != displayQuery) {
                        isUpdatingSearchFromViewState = true
                        searchInput.setText(displayQuery)
                        searchInput.setSelection(displayQuery.length)  // Move cursor to end
                        isUpdatingSearchFromViewState = false
                    }

                    // Preserve search visibility state across mode transitions
                    // If search is currently active, keep it visible when switching modes
                    // Otherwise, show it if there's a search query in the current mode
                    val shouldShowSearch = if (isSearchActive) {
                        // Search UI is active, keep it visible regardless of mode
                        true
                    } else {
                        // Search UI is not active, show it only if there's a query in current mode
                        if (inTitleMode) {
                            searchFilter.hasTitlesSearch
                        } else {
                            searchFilter.hasThemesSearch
                        }
                    }

                    if (shouldShowSearch) {
                        // Show search UI (without animation if already visible)
                        if (stickySearchContainer.visibility != View.VISIBLE) {
                            stickySearchContainer.visibility = View.VISIBLE
                            // Hide header search toggle button
                            searchToggleButton.visibility = View.GONE
                            // Mark as active since we're showing it
                            isSearchActive = true
                        }
                        // If there's a query, execute search to populate results
                        if (displayQuery.isNotEmpty()) {
                            // Clear list first to avoid showing stale data
                            contentAdapter.submitList(emptyList())
                            // Execute search to populate results
                            activity.window.decorView.post {
                                performSearch(displayQuery)
                            }
                        }
                    } else {
                        stickySearchContainer.visibility = View.GONE
                        // Show header search toggle button
                        searchToggleButton.visibility = View.VISIBLE
                    }

                    // Update right panel title from viewState
                    rightPanelTitle.text = FormatUtils.formatRightPanelTitle(activity, viewState)
                }
            }
        }
    }

    private fun setupCollapsibleDescription() {
        var isExpanded = false

        descriptionContainer.setOnClickListener {
            isExpanded = !isExpanded

            if (isExpanded) {
                // Expand: show all lines
                descriptionText.maxLines = Integer.MAX_VALUE
                descriptionText.ellipsize = null
                showMoreText.text = activity.getString(R.string.show_less)
                // Rotate chevron up (180 degrees)
                showMoreChevron.animate().rotation(180f).setDuration(200).start()
            } else {
                // Collapse: show 3 lines
                descriptionText.maxLines = 3
                descriptionText.ellipsize = android.text.TextUtils.TruncateAt.END
                showMoreText.text = activity.getString(R.string.show_more)
                // Rotate chevron down (0 degrees)
                showMoreChevron.animate().rotation(0f).setDuration(200).start()
            }
        }

        // Check if text is truncated and show button
        descriptionText.post {
            val layout = descriptionText.layout
            if (layout != null) {
                val lines = layout.lineCount
                if (lines > 0 && layout.getEllipsisCount(lines - 1) > 0) {
                    // Text is truncated, show the "Show more" button
                    showMoreButton.visibility = View.VISIBLE
                    showMoreText.text = activity.getString(R.string.show_more)
                    showMoreChevron.rotation = 0f
                } else {
                    // Text fits, hide button
                    showMoreButton.visibility = View.GONE
                }
            }
        }
    }

    // Search functionality

    /**
     * Activate search mode - show search input with grow animation and focus it
     */
    private fun activateSearch() {
        // Mark search as active
        isSearchActive = true

        // Hide the search toggle button in header
        searchToggleButton.visibility = View.GONE

        // Make sticky container visible first
        stickySearchContainer.visibility = View.VISIBLE

        // Animate from collapsed to expanded (slide down animation)
        stickySearchContainer.alpha = 0f
        stickySearchContainer.translationY = -stickySearchContainer.height.toFloat()
        stickySearchContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(250)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                // Request focus on search input after animation
                searchInput.requestFocus()

                // Show keyboard for text input
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager
                imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT)
            }
            .start()
    }

    /**
     * Deactivate search mode - hide sticky search container with slide up animation and clear search
     */
    private fun deactivateSearch() {
        // Mark search as inactive
        isSearchActive = false

        // Cancel any ongoing search
        cancelSearch()

        // Hide keyboard first
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE)
            as InputMethodManager
        imm.hideSoftInputFromWindow(searchInput.windowToken, 0)

        // Animate from expanded to collapsed (slide up animation)
        stickySearchContainer.animate()
            .alpha(0f)
            .translationY(-stickySearchContainer.height.toFloat())
            .setDuration(200)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                // Hide after animation completes
                stickySearchContainer.visibility = View.GONE
                // Reset for next animation
                stickySearchContainer.alpha = 1f
                stickySearchContainer.translationY = 0f
                // Show the search toggle button again in header
                searchToggleButton.visibility = View.VISIBLE
            }
            .start()

        // Clear themesQuery in ViewModel
        viewModel.updateSearchQuery("")

        // Clear the input (this won't trigger TextWatcher because viewState observer will handle it)
        isUpdatingSearchFromViewState = true
        searchInput.text.clear()
        isUpdatingSearchFromViewState = false

        // Explicitly restore the content list after clearing search
        // Post to ensure viewState has updated after clearing the query
        activity.window.decorView.post {
            val currentContent = viewModel.contentList.value
            updateContentListFromFlow(currentContent)
        }

        // Return focus to content list
        contentRecyclerView.requestFocus()
    }

    /**
     * Perform search with the given query
     * Contextual search based on current view state:
     * - In THEMES/CHANNELS state: search within themes
     * - In TITLES state: search within titles
     *
     * Cancels any ongoing search before starting a new one
     * Shows loading spinner during search
     */
    private fun performSearch(query: String) {
        // Trim whitespace from query
        val trimmedQuery = query.trim()

        if (trimmedQuery.isBlank()) {
            // If search is empty, deactivate search to restore normal view
            return
        }

        // Cancel any ongoing search, pending debounce, and pending search runnable
        currentSearchJob?.cancel()
        debounceJob?.cancel()
        debounceJob = null
        pendingSearchRunnable?.let { searchLoadingSpinner.removeCallbacks(it) }
        pendingSearchRunnable = null

        // Show loading indicator
        showSearchLoading()

        // Use postDelayed to ensure spinner is visible for minimum duration BEFORE search starts
        // This is critical for fast searches on low-memory devices (50 result limit, cache hits)
        pendingSearchRunnable = Runnable {
            currentSearchJob = activity.lifecycleScope.launch {
                try {
                    // Track when spinner was shown for minimum display duration
                    val spinnerStartTime = System.currentTimeMillis()

                    val currentState = viewModel.viewState.value

                when {
                    currentState is MediaViewModel.ViewState.Themes && currentState.theme == null -> {
                        // Search within themes - Hybrid approach: quick initial results + full results
                        // This shows ALL themes across all channels that match in theme name, title, or description

                        // Step 1: Get initial quick results (200 items)
                        val initialResults = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            if (currentState.channel != null) {
                                viewModel.repository.searchEntriesByChannel(currentState.channel, trimmedQuery, limit = 200)
                            } else {
                                viewModel.repository.searchEntries(trimmedQuery, limit = 200)
                            }
                        }

                        // Display initial results immediately (keep spinner visible)
                        val initialThemes = initialResults.distinctBy { it.theme }
                        val initialThemeItems = initialThemes.map { MediaBrowseAdapter.MediaEntryItem.Theme(it) }

                        Log.d(TAG, "Theme search (initial) for '$trimmedQuery' in channel '${currentState.channel ?: "ALL"}': " +
                                "${initialThemeItems.size} themes displayed (from ${initialResults.size} entries)")
                        contentAdapter.submitList(initialThemeItems)

                        // Step 2: Continue loading additional results in background (skip already loaded)
                        val additionalResults = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            if (currentState.channel != null) {
                                viewModel.repository.searchEntriesByChannelWithOffset(currentState.channel, trimmedQuery, limit = 4800, offset = 200)
                            } else {
                                viewModel.repository.searchEntriesWithOffset(trimmedQuery, limit = 4800, offset = 200)
                            }
                        }

                        // Combine initial and additional results
                        val searchResults = initialResults + additionalResults

                        // Update with complete results (DiffUtil handles efficiency if results are identical)
                        val uniqueThemes = searchResults.distinctBy { it.theme }
                        val themeItems = uniqueThemes.map { MediaBrowseAdapter.MediaEntryItem.Theme(it) }

                        Log.d(TAG, "Theme search (complete) for '$trimmedQuery' in channel '${currentState.channel ?: "ALL"}': " +
                                "${themeItems.size} themes found (from ${searchResults.size} total entries, ${additionalResults.size} additional)")

                        // Only update list if we found more unique themes than the initial preview
                        if (themeItems.size > initialThemeItems.size) {
                            contentAdapter.submitList(themeItems)
                        } else {
                            Log.d(TAG, "No new themes beyond initial preview (${initialThemeItems.size} themes)")
                        }

                        // Ensure spinner is visible for minimum 300ms (prevents flicker on fast searches)
                        val elapsedTime = System.currentTimeMillis() - spinnerStartTime
                        if (elapsedTime < 300) {
                            delay(300 - elapsedTime)
                        }

                        // Hide loading indicator after all results displayed
                        hideSearchLoading()
                    }

                    currentState is MediaViewModel.ViewState.Themes && currentState.theme != null -> {
                        // Search within titles - Hybrid approach: quick initial results + full results

                        // Step 1: Get initial quick results (200 items)
                        val initialResults = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            if (currentState.channel != null) {
                                viewModel.repository.searchEntriesByChannelAndTheme(
                                    currentState.channel,
                                    currentState.theme,
                                    trimmedQuery,
                                    limit = 200
                                )
                            } else {
                                viewModel.repository.searchEntriesByTheme(currentState.theme, trimmedQuery, limit = 200)
                            }
                        }

                        // Apply themesQuery filter to initial results if present (on background thread)
                        val initialFilteredResults = if (currentState.searchFilter.hasThemesSearch) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                                val themesQueryLower = currentState.searchFilter.themesQuery!!.trim().lowercase()
                                initialResults.filter { entry ->
                                    entry.theme.lowercase().contains(themesQueryLower) ||
                                    entry.title.lowercase().contains(themesQueryLower) ||
                                    entry.description.lowercase().contains(themesQueryLower)
                                }
                            }
                        } else {
                            initialResults
                        }

                        // Display initial results immediately (keep spinner visible)
                        val initialTitles = initialFilteredResults.distinctBy { it.title }
                        val initialTitleItems = initialTitles.map { MediaBrowseAdapter.MediaEntryItem.Title(it) }

                        Log.d(TAG, "Title search (initial) for '$trimmedQuery' in theme '${currentState.theme}' channel '${currentState.channel ?: "ALL"}': " +
                                "${initialTitleItems.size} titles displayed (from ${initialResults.size} entries" +
                                (if (currentState.searchFilter.hasThemesSearch) ", ${initialFilteredResults.size} after themesQuery filter" else "") + ")")
                        contentAdapter.submitList(initialTitleItems)

                        // Step 2: Continue loading additional results in background (skip already loaded)
                        val additionalResults = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            if (currentState.channel != null) {
                                viewModel.repository.searchEntriesByChannelAndThemeWithOffset(
                                    currentState.channel,
                                    currentState.theme,
                                    trimmedQuery,
                                    limit = 4800,
                                    offset = 200
                                )
                            } else {
                                viewModel.repository.searchEntriesByThemeWithOffset(currentState.theme, trimmedQuery, limit = 4800, offset = 200)
                            }
                        }

                        // Combine initial and additional results
                        val searchResults = initialResults + additionalResults

                        // Apply themesQuery as additional filter if present (cumulative filtering) on background thread
                        // This combines: theme filter + titlesQuery search + themesQuery filter
                        val filteredResults = if (currentState.searchFilter.hasThemesSearch) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                                val themesQueryLower = currentState.searchFilter.themesQuery!!.trim().lowercase()  // Trim before filtering
                                searchResults.filter { entry ->
                                    entry.theme.lowercase().contains(themesQueryLower) ||
                                    entry.title.lowercase().contains(themesQueryLower) ||
                                    entry.description.lowercase().contains(themesQueryLower)
                                }
                            }
                        } else {
                            searchResults
                        }

                        // Update with complete results
                        val uniqueTitles = filteredResults.distinctBy { it.title }
                        val titleItems = uniqueTitles.map { MediaBrowseAdapter.MediaEntryItem.Title(it) }

                        Log.d(TAG, "Title search (complete) for '$trimmedQuery' in theme '${currentState.theme}' channel '${currentState.channel ?: "ALL"}': " +
                                "${titleItems.size} titles found (from ${searchResults.size} total entries, ${additionalResults.size} additional" +
                                (if (currentState.searchFilter.hasThemesSearch) ", ${filteredResults.size} after themesQuery filter '${currentState.searchFilter.themesQuery}'" else "") + ")")

                        // Only update list if we found more unique titles than the initial preview
                        if (titleItems.size > initialTitleItems.size) {
                            contentAdapter.submitList(titleItems)
                        } else {
                            Log.d(TAG, "No new titles beyond initial preview (${initialTitleItems.size} titles)")
                        }

                        // Ensure spinner is visible for minimum 300ms (prevents flicker on fast searches)
                        val elapsedTime = System.currentTimeMillis() - spinnerStartTime
                        if (elapsedTime < 300) {
                            delay(300 - elapsedTime)
                        }

                        // Hide loading indicator after all results displayed
                        hideSearchLoading()
                    }

                    currentState is MediaViewModel.ViewState.Detail -> {
                        // Should not happen - search is only available in browse view
                        hideSearchLoading()
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "Search cancelled for query '$trimmedQuery'")
                hideSearchLoading()
                throw e // Re-throw to properly handle cancellation
            }
            }
        }
        searchLoadingSpinner.postDelayed(pendingSearchRunnable, 200) // Delay 200ms before starting search to ensure spinner is visible
    }

    /**
     * Cancel any ongoing search query
     */
    private fun cancelSearch() {
        currentSearchJob?.cancel()
        currentSearchJob = null
        debounceJob?.cancel()
        debounceJob = null
        pendingSearchRunnable?.let { searchLoadingSpinner.removeCallbacks(it) }
        pendingSearchRunnable = null
        hideSearchLoading()
    }

    /**
     * Show loading spinner, hide search button
     */
    private fun showSearchLoading() {
        searchButton.visibility = View.INVISIBLE
        searchLoadingSpinner.visibility = View.VISIBLE
        viewModel.setSearching(true)
    }

    /**
     * Hide loading spinner, show search button
     */
    private fun hideSearchLoading() {
        searchButton.visibility = View.VISIBLE
        searchLoadingSpinner.visibility = View.GONE
        viewModel.setSearching(false)
    }

    /**
     * Handle RecyclerView content item click (theme or title depending on state)
     * This is called from the adapter's click listener
     */
    private fun onContentItemClick(item: MediaBrowseAdapter.MediaEntryItem, position: Int) {
        // Cancel any ongoing search when navigating
        cancelSearch()

        when (item) {
            is MediaBrowseAdapter.MediaEntryItem.Theme -> onThemeSelected(item, position)
            is MediaBrowseAdapter.MediaEntryItem.Title -> onTitleSelected(item, position)
            is MediaBrowseAdapter.MediaEntryItem.More -> {
                // Load more themes by incrementing pagination
                val currentPart = viewModel.currentPart.value
                val nextPart = currentPart + 1
                viewModel.setCurrentPart(nextPart)
                // The themes Flow will automatically update via updateContentListFromFlow()
            }
        }
    }

    // Navigation methods

    /**
     * Show all themes (no channel filter)
     */
    fun showAllThemes(preserveThemeSelection: Boolean = false) {
        Log.i(TAG, "showAllThemes called (preserveThemeSelection: $preserveThemeSelection)")

        // Clear selection unless explicitly preserving
        if (!preserveThemeSelection) {
            viewModel.clearSelectedItem()
        }

        viewModel.navigateToThemes(null)
        // Note: navigateToThemes already sets currentPart to 0

        // Clear channel selection
        clearChannelSelection()

        // Show browse view - Flow will populate themes with selection preserved
        showBrowseView(animated = false)
    }

    /**
     * Show themes for selected channel
     */
    fun showChannelThemes(position: Int, preserveThemeSelection: Boolean = false) {
        Log.i(TAG, "showChannelThemes for position $position (preserveThemeSelection: $preserveThemeSelection)")

        // Clear selection unless explicitly preserving
        if (!preserveThemeSelection) {
            viewModel.clearSelectedItem()
        }

        val channelName = Broadcaster.getChannelName(position)
        viewModel.navigateToThemes(channelName)
        // Note: navigateToThemes already sets currentPart to 0

        // Update channel selection visual
        updateChannelSelection(position)

        // Show browse view - Flow will populate themes with selection preserved
        showBrowseView(animated = false)
    }

    /**
     * Show titles for selected theme
     */
    fun showTitleList(theme: String, preserveTitleSelection: Boolean = false) {
        Log.i(TAG, "showTitleList for theme: $theme (preserveTitleSelection: $preserveTitleSelection)")

        // When NOT preserving, clear the selection
        // This prevents showing a random title as selected when first entering from themes
        // Selection should only be shown when coming back from detail view
        if (!preserveTitleSelection) {
            viewModel.clearSelectedItem()
        }

        // Update ViewModel state - this will trigger the contentList Flow
        // The viewState observer will automatically update the search input based on queryText
        viewModel.navigateToThemes(channel = viewModel.selectedChannel, theme = theme)

        // Ensure channel selection UI is maintained when navigating to titles
        ensureChannelSelectionSynced()

        // Update UI to show titles view
        showBrowseView(animated = false)

        // The contentList Flow will automatically populate the list via updateContentListFromFlow()
        // No need to manually fetch - rely on reactive architecture
    }

    /**
     * Show detail view for selected title
     */
    fun showDetailView(title: String) {
        Log.i(TAG, "showDetailView for title: $title")

        // Update ViewModel state - this will load the media entry and navigate to Detail state
        viewModel.navigateToDetail(title)

        // Animate transition to detail view
        animateToDetailView()

        // The ViewState observer will automatically populate the detail view
        // via updateDetailViewFromFlow() in MediaActivity
    }

    /**
     * Handle back navigation
     * Returns true if navigation was handled, false if should exit app
     */
    fun onBackPressed(): Boolean {
        val currentView = viewModel.viewState.value
        val selectedChannelPosition = (if (viewModel.selectedChannel == null) -1 else Broadcaster.getChannelPosition(viewModel.selectedChannel!!))

        return when (currentView) {
            is MediaViewModel.ViewState.Detail -> {
                // Detail -> Back to titles WITH ANIMATION
                // Use navigation context (user's filter), not media entry's actual channel
                val navigationChannel = currentView.channel  // This now returns navigationChannel
                val navigationTheme = currentView.theme      // This now returns navigationTheme
                viewModel.navigateToThemes(channel = navigationChannel, theme = navigationTheme)
                ensureChannelSelectionSynced()
                showBrowseView(animated = true)
                true
            }
            is MediaViewModel.ViewState.Themes -> {
                when {
                    // Showing titles (theme != null) -> Back to themes
                    currentView.theme != null -> {
                        Log.i(TAG, "Back from TITLES: selectedChannel=${currentView.channel}, selectedTheme=${currentView.theme}")

                        // Explicitly set the theme selection before navigating back
                        // Find a MediaEntry with the theme we're coming from
                        val themeToSelect = viewModel.contentList.value.firstOrNull { it.theme == currentView.theme }
                        if (themeToSelect != null) {
                            viewModel.setSelectedItem(themeToSelect)
                            Log.d(TAG, "Set selectedItem to theme: ${currentView.theme}")
                        }

                        // Navigate back to themes first
                        if (selectedChannelPosition >= 0) {
                            Log.i(TAG, "Back from Titles → Channel Themes (position $selectedChannelPosition)")
                            showChannelThemes(selectedChannelPosition, preserveThemeSelection = true)
                        } else {
                            Log.i(TAG, "Back from Titles → All Themes")
                            showAllThemes(preserveThemeSelection = true)
                        }

                        // Ensure channel selection is visually synced
                        ensureChannelSelectionSynced()

                        // Query text is automatically restored by viewState observer
                        // No need for manual restoration anymore

                        true
                    }
                    // Showing channel themes (channel != null, theme == null) -> Back to all themes
                    currentView.channel != null -> {
                        Log.i(TAG, "Back from Channel Themes (${currentView.channel}) → All Themes")
                        showAllThemes()
                        true
                    }
                    // Showing all themes (channel == null, theme == null) -> Exit app
                    else -> {
                        Log.i(TAG, "Back from All Themes → Exit app")
                        false
                    }
                }
            }
        }
    }

    // Event handlers

    private fun onChannelSelected(position: Int) {
        Log.i(TAG, "Channel selected: position $position")
        // Cancel any ongoing search when switching channels
        cancelSearch()
        showChannelThemes(position)
    }

    private fun onThemeSelected(item: MediaBrowseAdapter.MediaEntryItem.Theme, position: Int) {
        val selectedTheme = item.theme
        Log.i(TAG, "Theme selected: '$selectedTheme' at position $position")

        // Don't cache selectedItem here - it will cause a title to be selected
        // Theme selection will be restored explicitly when navigating back from titles

        // Show titles for this theme
        showTitleList(selectedTheme)
    }

    private fun onTitleSelected(item: MediaBrowseAdapter.MediaEntryItem.Title, position: Int) {
        val selectedTitle = item.title
        Log.i(TAG, "Title selected: '$selectedTitle' at position $position")

        // Cache the selected item in viewState for back navigation
        viewModel.setSelectedItem(item.mediaEntry)

        showDetailView(selectedTitle)
    }

    // UI update methods

    private fun showBrowseView(animated: Boolean) {
        if (animated && detailView.isVisible) {
            // Only animate when transitioning FROM detail view
            animateToBrowseView()
        } else {
            // No animation when staying within browse view states
            browseView.visibility = View.VISIBLE
            detailView.visibility = View.GONE
        }

        // Request focus on content list for TV/DPAD navigation when returning from detail
        if (animated) {
            activity.window.decorView.post {
                contentRecyclerView.requestFocus()
            }
        }
    }

    private fun updateDetailView(info: Array<String>) {
        if (info.size < 11) return

        // Update channel image with drawable resource ID
        val iconRes = Broadcaster.getIconOfName(info[0])
        channelImg.setImageResource(iconRes)

        // Update text fields
        themeText.text = info[1]
        titleText.text = info[2]
        dateText.text = info[3]
        timeText.text = FormatUtils.formatTime(activity, info[4])
        durationText.text = FormatUtils.formatDuration(activity, info[5])
        sizeText.text = FormatUtils.formatFileSize(info[6])
        descriptionText.text = info[7]
    }

    /**
     * Update visual selection indicator for channel list
     */
    private fun updateChannelSelection(position: Int) {
        // Use ListView's built-in selection mechanism
        channelListView.clearChoices()
        channelListView.setItemChecked(position, true)

        // Scroll to position - use smoothScrollToPosition for better UX
        // Post to ensure layout is complete before scrolling
        channelListView.post {
            channelListView.smoothScrollToPosition(position)
        }

        // Also set selection for keyboard navigation
        channelListView.setSelection(position)

        // Force adapter refresh to update backgrounds
        broadCasterListAdapter.notifyDataSetChanged()
    }

    /**
     * Clear all channel selections
     */
    private fun clearChannelSelection() {
        channelListView.clearChoices()

        // Force adapter refresh to update backgrounds
        broadCasterListAdapter.notifyDataSetChanged()
    }

    /**
     * Ensure channel selection UI matches the current viewState
     * This is called when navigating between views to keep selection in sync
     */
    private fun ensureChannelSelectionSynced() {
        val selectedChannel = viewModel.selectedChannel
        if (selectedChannel != null) {
            val position = Broadcaster.getChannelPosition(selectedChannel)
            if (position >= 0) {
                // Only update if not already correctly selected
                if (!channelListView.isItemChecked(position)) {
                    updateChannelSelection(position)
                }
            }
        } else {
            // No channel selected, ensure nothing is highlighted
            if (channelListView.checkedItemCount > 0) {
                clearChannelSelection()
            }
        }
    }

    // Menu handling

    /**
     * Show overflow menu popup
     * Configured for TV/DPAD navigation compatibility
     */
    private fun showOverflowMenu(anchorView: View) {
        // Create PopupMenu with themed context to ensure selector works
        val themedContext = android.view.ContextThemeWrapper(activity, R.style.PopupMenuThemeOverlay)
        val popup = PopupMenu(themedContext, anchorView)
        popup.menuInflater.inflate(R.menu.menu_mv, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_check_update -> {
                    (activity as? MediaActivity)?.checkForUpdatesManually()
                    true
                }
                R.id.menu_get_filmlist -> {
                    (activity as? MediaActivity)?.startMediaListDownload()
                    true
                }
                R.id.menu_set_zeitraum -> {
                    (activity as? MediaActivity)?.showTimePeriodDialog(viewModel)
                    true
                }
                R.id.menu_start_compose -> {
                    (activity as? MediaActivity)?.startComposeActivity()
                    true
                }
                else -> false
            }
        }

        popup.setOnDismissListener {
            // Return focus to menu button after dismissal
            menuButton.requestFocus()
        }

        popup.show()
    }

    // Getters

    fun isQualityHigh(): Boolean = qualityHigh.isChecked

    // Called when media list is loaded
    fun onMediaListLoaded() {
        activity.runOnUiThread {
            // Show all themes on startup
            showAllThemes()
        }
    }

    /**
     * Update content list from unified reactive Flow
     * Handles both themes and titles based on current ViewState
     * Applies search filter to Flow data if present (for hidden/cached filters)
     * Preserves selection of previously selected items from viewState
     */
    fun updateContentListFromFlow(entries: List<com.mediathekview.android.database.MediaEntry>) {
        val currentView = viewModel.viewState.value
        if (currentView !is MediaViewModel.ViewState.Themes) {
            Log.d(TAG, "updateContentListFromFlow: Skipping - not in Themes view (currentView=${currentView::class.simpleName})")
            return
        }

        // Determine if we're showing themes or titles
        val showingThemes = currentView.theme == null

        // Get search filter from viewState
        val searchFilter = currentView.searchFilter

        // If search is active in current mode, performSearch() handles display (skip Flow)
        // Theme mode: skip if themesQuery exists (active search)
        // Title mode: skip if titlesQuery exists (active search in title mode)
        if (showingThemes && searchFilter.hasThemesSearch) {
            Log.d(TAG, "updateContentListFromFlow: Skipping - themesQuery active (performSearch handles display)")
            return
        }
        if (!showingThemes && searchFilter.hasTitlesSearch) {
            Log.d(TAG, "updateContentListFromFlow: Skipping - titlesQuery active (performSearch handles display)")
            return
        }

        // Apply themesQuery as hidden filter in title mode (when user came from theme search)
        val filteredEntries = if (!showingThemes && searchFilter.hasThemesSearch) {
            val query = searchFilter.themesQuery!!.trim().lowercase()  // Trim before filtering
            val filtered = entries.filter { entry ->
                entry.theme.lowercase().contains(query) ||
                entry.title.lowercase().contains(query) ||
                entry.description.lowercase().contains(query)
            }
            Log.d(TAG, "Applied themesQuery filter '$query' in title mode: ${entries.size} entries → ${filtered.size} after filter")
            if (filtered.isEmpty() && entries.isNotEmpty()) {
                Log.w(TAG, "WARNING: Filter resulted in empty list! Sample entry: theme='${entries.first().theme}', title='${entries.first().title}'")
            }
            filtered
        } else {
            entries
        }

        // Convert MediaEntry objects to adapter items
        val items = if (showingThemes) {
            // Showing themes - convert to Theme items
            val uniqueThemes = filteredEntries.distinctBy { it.theme }
            val themeItems: MutableList<MediaBrowseAdapter.MediaEntryItem> =
                uniqueThemes.map { MediaBrowseAdapter.MediaEntryItem.Theme(it) }.toMutableList()

            // Add "+++ more +++" item if we received exactly MAX_UI_ITEMS (1200) AND no themesQuery is active
            // This indicates there might be more themes to load
            if (uniqueThemes.size >= 1200 && !searchFilter.hasThemesSearch) {
                themeItems.add(MediaBrowseAdapter.MediaEntryItem.More)
            }

            themeItems
        } else {
            // Showing titles - convert to Title items
            val uniqueTitles = filteredEntries.distinctBy { it.title }
            uniqueTitles.map { MediaBrowseAdapter.MediaEntryItem.Title(it) }
        }

        // Get cached selected item from viewState (unified for both themes and titles)
        val cachedSelectedItem = currentView.selectedItem

        Log.d(TAG, "Updating content from Flow: ${entries.size} entries → ${filteredEntries.size} filtered → ${items.size} unique, " +
                "type=${if (showingThemes) "themes" else "titles"}, " +
                "themesQuery='${searchFilter.themesQuery}', titlesQuery='${searchFilter.titlesQuery}', " +
                "cached selection: ${cachedSelectedItem?.let { if (showingThemes) it.theme else it.title }}")

        // Find selected item by matching the cached MediaEntry
        // Context (theme vs title) determines which field to compare
        val selectedItem = if (cachedSelectedItem != null) {
            items.find { item ->
                when (item) {
                    is MediaBrowseAdapter.MediaEntryItem.Theme ->
                        item.mediaEntry.theme == cachedSelectedItem.theme
                    is MediaBrowseAdapter.MediaEntryItem.Title ->
                        item.mediaEntry.title == cachedSelectedItem.title
                    is MediaBrowseAdapter.MediaEntryItem.More ->
                        false // Never match More items for selection
                }
            }
        } else null

        // Preserve selection when updating list - DiffUtil handles animations
        contentAdapter.submitListWithSelection(items, selectedItem)

        // Scroll to selected item if exists
        if (selectedItem != null) {
            val position = items.indexOf(selectedItem)
            if (position >= 0) {
                contentRecyclerView.scrollToPosition(position)
            } else {
                contentRecyclerView.scrollToPosition(0)
            }
        } else {
            contentRecyclerView.scrollToPosition(0)
        }
    }

    /**
     * Update detail view from reactive Flow
     * Called when current media entry changes
     */
    fun updateDetailViewFromFlow(mediaEntry: com.mediathekview.android.database.MediaEntry?) {
        if (mediaEntry == null) {
            Log.w(TAG, "Cannot update detail view: media entry is null")
            return
        }

        val currentView = viewModel.viewState.value
        if (currentView is MediaViewModel.ViewState.Detail) {
            // Log all available media entry data
            Log.d(TAG, "=".repeat(80))
            Log.d(TAG, "MEDIA ENTRY DETAIL VIEW DATA:")
            Log.d(TAG, "=".repeat(80))
            Log.d(TAG, "ID: ${mediaEntry.id}")
            Log.d(TAG, "Channel: ${mediaEntry.channel}")
            Log.d(TAG, "Theme: ${mediaEntry.theme}")
            Log.d(TAG, "Title: ${mediaEntry.title}")
            Log.d(TAG, "Date: ${mediaEntry.date}")
            Log.d(TAG, "Time: ${mediaEntry.time}")
            Log.d(TAG, "Duration: ${mediaEntry.duration}")
            Log.d(TAG, "Size (MB): ${mediaEntry.sizeMB}")
            Log.d(TAG, "Description: ${mediaEntry.description}")
            Log.d(TAG, "-".repeat(80))
            Log.d(TAG, "URLs:")
            Log.d(TAG, "  Main URL: ${mediaEntry.url}")
            Log.d(TAG, "  Website: ${mediaEntry.website}")
            Log.d(TAG, "  Subtitle URL: ${mediaEntry.subtitleUrl}")
            Log.d(TAG, "  Small URL: ${mediaEntry.smallUrl}")
            Log.d(TAG, "  HD URL: ${mediaEntry.hdUrl}")
            Log.d(TAG, "-".repeat(80))
            Log.d(TAG, "Metadata:")
            Log.d(TAG, "  Timestamp: ${mediaEntry.timestamp}")
            Log.d(TAG, "  Geo restriction: ${mediaEntry.geo}")
            Log.d(TAG, "  Is new: ${mediaEntry.isNew}")
            Log.d(TAG, "=".repeat(80))

            val info = mediaEntry.toInfoArray()
            updateDetailView(info)
        }
    }

    /**
     * Restore navigation state after configuration change
     * Should be called after media list is loaded
     * Reads all state from ViewModel only
     *
     * This method only restores the visual state (view visibility, channel selection, title text).
     * The reactive Flows will handle populating the content lists automatically based on
     * the ViewModel state that was already restored.
     */
    fun restoreNavigationState() {
        val viewState = viewModel.viewState.value
        val selectedChannelPosition = (if (viewModel.selectedChannel == null) -1 else Broadcaster.getChannelPosition(viewModel.selectedChannel!!))
        val selectedTitle = viewModel.selectedTitle ?: ""
        val currentTheme = viewModel.selectedTheme ?: ""
        val infoList = viewModel.currentMediaEntry?.toInfoArray()

        Log.i(TAG, "Restoring navigation state: viewState=$viewState, channel=$selectedChannelPosition, theme=$currentTheme, title=$selectedTitle")

        when (viewState) {
            is MediaViewModel.ViewState.Themes -> {
                if (viewState.theme != null) {
                    // Restore browse titles view
                    // Just restore visual state - the currentTitles Flow will populate the content list
                    if (currentTheme.isNotEmpty()) {
                        if (selectedChannelPosition >= 0) {
                            updateChannelSelection(selectedChannelPosition)
                        }
                    }
                } else {
                    // Restore browse themes view
                    // Just restore visual state - the allThemes Flow will populate the content list
                    if (selectedChannelPosition >= 0) {
                        updateChannelSelection(selectedChannelPosition)
                    } else {
                        clearChannelSelection()
                    }
                }
                // Show browse view without animation during restore
                browseView.visibility = View.VISIBLE
                detailView.visibility = View.GONE
                browseView.alpha = 1f
                browseView.translationX = 0f
                detailView.alpha = 1f
                detailView.translationX = 0f
            }
            is MediaViewModel.ViewState.Detail -> {
                // Restore detail view using ViewModel state
                Log.d(TAG, "Attempting DETAIL restore: title='$selectedTitle', theme='$currentTheme', infoList=${infoList != null}")
                if (selectedTitle.isNotEmpty() && infoList != null && currentTheme.isNotEmpty()) {
                    Log.i(TAG, "Restoring DETAIL view successfully")
                    // Restore channel selection if needed
                    if (selectedChannelPosition >= 0) {
                        updateChannelSelection(selectedChannelPosition)
                    }
                    // Restore detail view directly without animation
                    updateDetailView(infoList)
                    browseView.visibility = View.GONE
                    detailView.visibility = View.VISIBLE
                    // Hide keyboard when showing detail view
                    hideKeyboard()
                    // Ensure views are in correct state (no animation artifacts)
                    detailView.alpha = 1f
                    detailView.translationX = 0f
                    browseView.alpha = 1f
                    browseView.translationX = 0f

                    // Set initial focus on Play button for TV/DPAD navigation
                    activity.window.decorView.post {
                        playButton?.requestFocus()
                    }
                } else {
                    Log.w(TAG, "Failed to restore DETAIL view - missing data, showing browse view")
                    // Fallback to browse view - let Flows populate
                    // Show browse view without animation during restore
                    browseView.visibility = View.VISIBLE
                    detailView.visibility = View.GONE
                    browseView.alpha = 1f
                    browseView.translationX = 0f
                    detailView.alpha = 1f
                    detailView.translationX = 0f
                }
            }
        }
    }

    // Animation methods

    /**
     * Hide the soft keyboard
     */
    private fun hideKeyboard() {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val currentFocus = activity.currentFocus
        if (currentFocus != null) {
            imm?.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        } else {
            // If no view has focus, try to hide from the search input which is most likely to have opened it
            imm?.hideSoftInputFromWindow(searchInput.windowToken, 0)
        }
    }

    /**
     * Animate transition from browse view to detail view
     * Fades out browse view while sliding left, fades in detail view while sliding from right
     */
    private fun animateToDetailView() {
        // Hide keyboard when navigating to detail view
        hideKeyboard()

        // Make detail view visible but transparent and off-screen
        detailView.visibility = View.VISIBLE
        detailView.alpha = 0f
        detailView.translationX = detailView.width.toFloat()

        // Animate browse view out (fade + slide left)
        browseView.animate()
            .alpha(0f)
            .translationX(-browseView.width.toFloat() * 0.3f)
            .setDuration(ANIMATION_DURATION)
            .withEndAction {
                browseView.visibility = View.GONE
                browseView.alpha = 1f
                browseView.translationX = 0f
            }
            .start()

        // Animate detail view in (fade + slide from right)
        detailView.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(ANIMATION_DURATION)
            .withEndAction {
                // Request focus on Play button for TV/DPAD navigation
                activity.window.decorView.post {
                    playButton?.requestFocus()
                }
            }
            .start()
    }

    /**
     * Animate transition from detail view to browse view
     * Fades out detail view while sliding right, fades in browse view while sliding from left
     */
    private fun animateToBrowseView() {
        // Make browse view visible but transparent and off-screen
        browseView.visibility = View.VISIBLE
        browseView.alpha = 0f
        browseView.translationX = -browseView.width.toFloat() * 0.3f

        // Animate detail view out (fade + slide right)
        detailView.animate()
            .alpha(0f)
            .translationX(detailView.width.toFloat())
            .setDuration(ANIMATION_DURATION)
            .withEndAction {
                detailView.visibility = View.GONE
                detailView.alpha = 1f
                detailView.translationX = 0f
            }
            .start()

        // Animate browse view in (fade + slide from left)
        browseView.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(ANIMATION_DURATION)
            .start()
    }

}

private const val TAG = "UIManager"
private const val ANIMATION_DURATION = 300L