package com.mediathekview.android.compose.screens

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mediathekview.android.compose.data.ComposeDataMapper
import com.mediathekview.android.compose.data.ComposeDataMapper.extractUniqueThemes
import com.mediathekview.android.compose.data.ComposeDataMapper.extractUniqueTitles
import com.mediathekview.android.compose.data.ComposeDataMapper.toMediaItem
import com.mediathekview.android.compose.models.Channel
import com.mediathekview.android.compose.models.ComposeViewModel
import com.mediathekview.android.compose.models.MediaItem
import com.mediathekview.android.data.MediaViewModel
import com.mediathekview.android.database.MediaEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * State holder for Compose Media screens
 * Bridges the gap between MediaViewModel and Compose UI
 */
@Composable
fun rememberComposeMediaState(
    viewModel: ComposeViewModel
): ComposeMediaState {
    return remember(viewModel) {
        ComposeMediaState(viewModel)
    }
}

class ComposeMediaState(
    private val viewModel: ComposeViewModel
) {
    // Get all available channels
    val channels: List<Channel> = ComposeDataMapper.getAllChannels()

    /**
     * Get all media entries directly from repository
     * This bypasses the filtered contentList which might be empty in certain view states
     */
    @Composable
    private fun getAllMediaEntries(): State<List<com.mediathekview.android.database.MediaEntry>> {
        // Use getEntriesFlow with empty parameters to get all entries
        val entriesFlow = remember {
            viewModel.repository.getEntriesFlow("", "")
        }
        return entriesFlow.collectAsStateWithLifecycle(emptyList())
    }

    /**
     * Get themes for display based on current navigation state
     * Uses repository's dedicated Flow methods for proper reactive updates
     * @param channelName Filter by channel name (null for all channels)
     * @param currentPart Current pagination page (0-indexed)
     * @return Flow of theme names
     */
    @Composable
    fun getThemes(channelName: String?, currentPart: Int = 0): State<List<String>> {
        // Calculate limit based on pagination (1200 items per page)
        val limit = (currentPart + 1) * 1200

        // Use the repository's dedicated themes Flow for proper reactivity
        val themesFlow = remember(channelName, limit) {
            if (channelName != null) {
                viewModel.repository.getThemesForChannelFlow(channelName, limit = limit)
            } else {
                viewModel.repository.getAllThemesFlow(limit = limit)
            }
        }

        val themes by themesFlow.collectAsStateWithLifecycle(emptyList())

        android.util.Log.d("ComposeMediaState", "getThemes(channel=$channelName, part=$currentPart, limit=$limit): ${themes.size} themes")

        return remember(themes) {
            derivedStateOf { themes }
        }
    }

    /**
     * Get titles for a specific theme
     * Uses repository's Flow for proper reactive updates
     * @param channelName Optional channel filter
     * @param theme Theme to get titles for
     * @return Flow of title names
     */
    @Composable
    fun getTitles(channelName: String?, theme: String): State<List<String>> {
        // Use repository's entries flow and extract titles
        val entriesFlow = remember(channelName, theme) {
            viewModel.repository.getEntriesFlow(channelName ?: "", theme)
        }

        val entries by entriesFlow.collectAsStateWithLifecycle(emptyList())

        return remember(entries) {
            derivedStateOf {
                val titles = entries.extractUniqueTitles()
                android.util.Log.d("ComposeMediaState", "getTitles(channel=$channelName, theme=$theme): ${titles.size} titles")
                titles
            }
        }
    }

    /**
     * Get the raw MediaEntry for a specific title
     * @param title Title to get entry for
     * @param channel Optional channel to filter by
     * @param theme Optional theme to filter by
     * @return MediaEntry or null if not found
     */
    @Composable
    fun getMediaEntry(
        title: String,
        channel: String? = null,
        theme: String? = null
    ): State<com.mediathekview.android.database.MediaEntry?> {
        val contentList by getAllMediaEntries()

        return remember(contentList, title, channel, theme) {
            derivedStateOf {
                // SIMPLIFIED APPROACH: Just find any item with matching title in the given theme
                if (theme != null) {
                    // We know the theme, so find any item with this title in this theme
                    contentList.firstOrNull { entry ->
                        entry.title == title && entry.theme == theme
                    } ?: contentList.firstOrNull { entry ->
                        // Fallback: case-insensitive match
                        entry.title.equals(title, ignoreCase = true) &&
                        entry.theme.equals(theme, ignoreCase = true)
                    }
                } else {
                    // No theme context, just find first match by title
                    contentList.firstOrNull { entry ->
                        entry.title == title
                    } ?: contentList.firstOrNull { entry ->
                        entry.title.equals(title, ignoreCase = true)
                    }
                }
            }
        }
    }

    /**
     * Get media item details for a specific title
     * @param title Title to get details for
     * @param channel Optional channel to filter by
     * @param theme Optional theme to filter by
     * @return MediaItem or null if not found
     */
    @Composable
    fun getMediaItem(
        title: String,
        channel: String? = null,
        theme: String? = null
    ): State<MediaItem?> {
        val mediaEntry by getMediaEntry(title, channel, theme)

        return remember(mediaEntry) {
            derivedStateOf {
                android.util.Log.d("ComposeMediaState", "=== MEDIA ITEM LOOKUP ===")
                android.util.Log.d("ComposeMediaState", "Looking for - Title: '$title', Channel: '$channel', Theme: '$theme'")

                val entry = mediaEntry
                if (entry != null) {
                    android.util.Log.d("ComposeMediaState", "FOUND: ${entry.title} in ${entry.theme} (${entry.channel})")
                } else {
                    android.util.Log.d("ComposeMediaState", "NOT FOUND for title: '$title', theme: '$theme'")
                }

                entry?.toMediaItem()
            }
        }
    }

    /**
     * Search for themes or titles
     * @param query Search query
     * @param searchInTitles If true, search in titles; otherwise search in themes
     * @return List of matching items
     */
    @Composable
    fun searchContent(query: String, searchInTitles: Boolean): State<List<String>> {
        val contentList by getAllMediaEntries()

        return remember(contentList, query, searchInTitles) {
            derivedStateOf {
                if (query.isBlank()) {
                    emptyList()
                } else {
                    val lowerQuery = query.lowercase()
                    if (searchInTitles) {
                        contentList
                            .filter {
                                it.title.lowercase().contains(lowerQuery) ||
                                it.description.lowercase().contains(lowerQuery)
                            }
                            .extractUniqueTitles()
                    } else {
                        contentList
                            .filter {
                                it.theme.lowercase().contains(lowerQuery) ||
                                it.title.lowercase().contains(lowerQuery) ||
                                it.description.lowercase().contains(lowerQuery)
                            }
                            .extractUniqueThemes()
                    }
                }
            }
        }
    }

    /**
     * Navigate to themes view
     */
    fun navigateToThemes(channelName: String?) {
        viewModel.navigateToThemes(channelName)
    }

    /**
     * Navigate to titles within a theme
     */
    fun navigateToTitles(channelName: String?, theme: String) {
        viewModel.navigateToThemes(channel = channelName, theme = theme)
    }

    /**
     * Navigate to media detail
     */
    fun navigateToDetail(title: String, channel: String? = null, theme: String? = null) {
        // First ensure the ViewModel is in the right state for finding the entry
        if (theme != null) {
            viewModel.navigateToThemes(channel = channel, theme = theme)
        }
        viewModel.navigateToDetail(title)
    }

    /**
     * Handle play button click with specific media entry
     */
    fun onPlayClicked(mediaEntry: com.mediathekview.android.database.MediaEntry?, isHighQuality: Boolean) {
        android.util.Log.d("ComposeMediaState", "=== PLAY CLICKED ===")
        android.util.Log.d("ComposeMediaState", "MediaEntry: ${mediaEntry?.title ?: "NULL"}")
        android.util.Log.d("ComposeMediaState", "High Quality: $isHighQuality")

        if (mediaEntry != null) {
            android.util.Log.d("ComposeMediaState", "Playing: ${mediaEntry.title} - ${mediaEntry.channel} - ${mediaEntry.theme}")
            android.util.Log.d("ComposeMediaState", "URLs - Main: ${mediaEntry.url.take(50)}...")
            // Use the new direct playback method that takes the entry as parameter
            viewModel.playVideoWithEntry(mediaEntry, isHighQuality)
        } else {
            android.util.Log.e("ComposeMediaState", "Cannot play - media entry is null")
        }
    }

    /**
     * Handle download button click with specific media entry
     */
    fun onDownloadClicked(mediaEntry: com.mediathekview.android.database.MediaEntry?, isHighQuality: Boolean) {
        if (mediaEntry != null) {
            // Set the media entry in the ViewModel state first
            viewModel.setCurrentMediaEntry(mediaEntry)
            // Then trigger download
            viewModel.onDownloadButtonClicked(isHighQuality)
        } else {
            android.util.Log.e("ComposeMediaState", "Cannot download - media entry is null")
        }
    }

    /**
     * Check if data is loaded and available
     */
    @Composable
    fun isDataLoaded(): State<Boolean> {
        val loadingState by viewModel.loadingState.collectAsStateWithLifecycle()
        val contentList by getAllMediaEntries()

        return remember(loadingState, contentList) {
            derivedStateOf {
                // Check both that loading is complete AND we have some data
                // or that we're explicitly in LOADED state (even if empty)
                loadingState == ComposeViewModel.LoadingState.LOADED || contentList.isNotEmpty()
            }
        }
    }

    /**
     * Get current loading state
     */
    @Composable
    fun getLoadingState(): State<ComposeViewModel.LoadingState> {
        return viewModel.loadingState.collectAsStateWithLifecycle()
    }
}