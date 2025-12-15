package cut.the.crap.shared.viewmodel

import cut.the.crap.shared.database.MediaEntry

/**
 * Loading states for the ViewModel
 */
enum class LoadingState {
    NOT_LOADED,
    LOADING,
    LOADED,
    ERROR
}

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

/**
 * Navigation state for the Compose UI
 */
sealed interface ViewState {
    val channel: String?
    val theme: String?

    data class Themes(
        override val channel: String? = null,
        override val theme: String? = null,
        val searchFilter: SearchFilter = SearchFilter(),
        val selectedItem: MediaEntry? = null
    ) : ViewState

    data class Detail(
        val mediaEntry: MediaEntry,
        val navigationChannel: String? = null,
        val navigationTheme: String? = null,
        val searchFilter: SearchFilter = SearchFilter(),
        val selectedItem: MediaEntry? = null
    ) : ViewState {
        override val channel: String? get() = navigationChannel
        override val theme: String? get() = navigationTheme ?: mediaEntry.theme
        val title: String get() = mediaEntry.title
    }
}

/**
 * Content type for database queries
 */
sealed class ContentType {
    data class Themes(val channel: String?, val date: Long, val part: Int) : ContentType()
    data class Titles(val channel: String?, val theme: String, val date: Long) : ContentType()
}

/**
 * Platform-agnostic dialog state
 * Uses action enums instead of callbacks for cross-platform compatibility
 */
sealed interface DialogState {
    val title: String
    val message: String
    val cancelable: Boolean

    /**
     * Actions that can be triggered by dialog buttons
     */
    enum class Action {
        DISMISS,
        CONFIRM,
        CANCEL,
        RETRY,
        START_DOWNLOAD,
        UPDATE,
        LATER
    }

    data class Message(
        override val title: String,
        override val message: String,
        val positiveLabel: String = "OK",
        val positiveAction: Action = Action.DISMISS,
        override val cancelable: Boolean = true
    ) : DialogState

    data class Confirmation(
        override val title: String,
        override val message: String,
        val positiveLabel: String = "OK",
        val negativeLabel: String = "Cancel",
        val positiveAction: Action = Action.CONFIRM,
        val negativeAction: Action = Action.CANCEL,
        override val cancelable: Boolean = true
    ) : DialogState

    data class Progress(
        override val title: String,
        override val message: String,
        override val cancelable: Boolean = false
    ) : DialogState

    data class Error(
        override val title: String = "Error",
        override val message: String,
        val retryLabel: String = "Retry",
        val cancelLabel: String = "Cancel",
        val retryAction: Action = Action.RETRY,
        val cancelAction: Action = Action.DISMISS,
        val canRetry: Boolean = true,
        override val cancelable: Boolean = false
    ) : DialogState

    data class SingleChoice(
        override val title: String,
        override val message: String = "",
        val items: List<String>,
        val selectedIndex: Int = -1,
        val negativeLabel: String = "Cancel",
        override val cancelable: Boolean = true
    ) : DialogState
}
