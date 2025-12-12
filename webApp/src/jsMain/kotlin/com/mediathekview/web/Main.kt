package com.mediathekview.web

import androidx.compose.runtime.*
import com.mediathekview.web.repository.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.w3c.dom.events.KeyboardEvent

/**
 * Navigation state for the app
 */
sealed class NavigationState {
    data object ChannelList : NavigationState()
    data class ThemeList(val channel: String) : NavigationState()
    data class TitleList(val channel: String, val theme: String) : NavigationState()
    data class DetailView(val channel: String, val theme: String, val title: String) : NavigationState()
    data class SearchResults(val query: String) : NavigationState()
    data class FilteredBrowse(
        val dateFilter: DateFilter = DateFilter.ALL,
        val durationFilter: DurationFilter = DurationFilter.ALL,
        val channel: String? = null
    ) : NavigationState()
}

/**
 * Global repository instance
 */
private val repository: MediaRepository = MockMediaRepository()

fun main() {
    renderComposable(rootElementId = "root") {
        MediathekViewApp()
    }
}

@Composable
fun MediathekViewApp() {
    var navigationState by remember { mutableStateOf<NavigationState>(NavigationState.ChannelList) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedChannelIndex by remember { mutableStateOf(-1) }
    val coroutineScope = rememberCoroutineScope()

    // Keyboard navigation
    DisposableEffect(Unit) {
        val handler: (org.w3c.dom.events.Event) -> Unit = { event ->
            val keyEvent = event as KeyboardEvent
            when (keyEvent.key) {
                "Escape", "Backspace" -> {
                    // Don't intercept backspace when in search input
                    val target = event.target
                    if (target != null && js("target.tagName === 'INPUT'") as Boolean) {
                        // Let input handle backspace
                    } else {
                        event.preventDefault()
                        navigationState = when (val state = navigationState) {
                            is NavigationState.DetailView -> NavigationState.TitleList(state.channel, state.theme)
                            is NavigationState.TitleList -> NavigationState.ThemeList(state.channel)
                            is NavigationState.ThemeList -> NavigationState.ChannelList
                            is NavigationState.SearchResults -> NavigationState.ChannelList
                            else -> navigationState
                        }
                    }
                }
            }
        }
        document.addEventListener("keydown", handler)
        onDispose {
            document.removeEventListener("keydown", handler)
        }
    }

    Style(AppStyleSheet)

    Div({ classes(AppStyleSheet.container) }) {
        // Header
        Header({ classes(AppStyleSheet.header) }) {
            Div({ classes(AppStyleSheet.headerLeft) }) {
                H1({
                    classes(AppStyleSheet.logo)
                    onClick {
                        navigationState = NavigationState.ChannelList
                        searchQuery = ""
                    }
                    style { property("cursor", "pointer") }
                }) { Text("MediathekView") }
            }
            Div({ classes(AppStyleSheet.headerCenter) }) {
                Breadcrumb(navigationState) { newState ->
                    navigationState = newState
                }
            }
            Div({ classes(AppStyleSheet.headerRight) }) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {
                        if (searchQuery.isNotBlank()) {
                            navigationState = NavigationState.SearchResults(searchQuery)
                        }
                    }
                )
            }
        }

        // Main content
        Div({ classes(AppStyleSheet.mainContent) }) {
            // Channel sidebar (always visible)
            Aside({ classes(AppStyleSheet.sidebar) }) {
                H3({ classes(AppStyleSheet.sidebarTitle) }) { Text("Sender") }
                Div({ classes(AppStyleSheet.channelList) }) {
                    Broadcaster.channelList.forEachIndexed { index, broadcaster ->
                        val isSelected = when (val state = navigationState) {
                            is NavigationState.ThemeList -> state.channel == broadcaster.name
                            is NavigationState.TitleList -> state.channel == broadcaster.name
                            is NavigationState.DetailView -> state.channel == broadcaster.name
                            else -> false
                        }
                        ChannelButton(
                            broadcaster = broadcaster,
                            isSelected = isSelected,
                            isFocused = selectedChannelIndex == index,
                            onClick = {
                                selectedChannelIndex = index
                                navigationState = NavigationState.ThemeList(broadcaster.name)
                            }
                        )
                    }
                }
            }

            // Content area
            Main({ classes(AppStyleSheet.content) }) {
                when (val state = navigationState) {
                    is NavigationState.ChannelList -> {
                        WelcomeView(
                            onBrowseClick = {
                                navigationState = NavigationState.FilteredBrowse()
                            }
                        )
                    }
                    is NavigationState.ThemeList -> {
                        ThemeListView(
                            channel = state.channel,
                            coroutineScope = coroutineScope,
                            onThemeClick = { theme ->
                                navigationState = NavigationState.TitleList(state.channel, theme)
                            }
                        )
                    }
                    is NavigationState.TitleList -> {
                        TitleListView(
                            channel = state.channel,
                            theme = state.theme,
                            coroutineScope = coroutineScope,
                            onTitleClick = { title ->
                                navigationState = NavigationState.DetailView(state.channel, state.theme, title)
                            }
                        )
                    }
                    is NavigationState.DetailView -> {
                        DetailViewAsync(
                            channel = state.channel,
                            theme = state.theme,
                            title = state.title,
                            coroutineScope = coroutineScope,
                            onBack = {
                                navigationState = NavigationState.TitleList(state.channel, state.theme)
                            }
                        )
                    }
                    is NavigationState.SearchResults -> {
                        SearchResultsView(
                            query = state.query,
                            coroutineScope = coroutineScope,
                            onItemClick = { item ->
                                navigationState = NavigationState.DetailView(item.channel, item.theme, item.title)
                            }
                        )
                    }
                    is NavigationState.FilteredBrowse -> {
                        FilteredBrowseView(
                            dateFilter = state.dateFilter,
                            durationFilter = state.durationFilter,
                            channel = state.channel,
                            coroutineScope = coroutineScope,
                            onFilterChange = { date, duration, ch ->
                                navigationState = NavigationState.FilteredBrowse(date, duration, ch)
                            },
                            onItemClick = { item ->
                                navigationState = NavigationState.DetailView(item.channel, item.theme, item.title)
                            }
                        )
                    }
                }
            }
        }

        // Footer
        Footer({ classes(AppStyleSheet.footer) }) {
            Text("MediathekView Web - Powered by Kotlin/JS & Compose HTML")
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit) {
    Div({ classes(AppStyleSheet.searchContainer) }) {
        TextInput(query) {
            onInput { event -> onQueryChange(event.value) }
            onKeyDown { event ->
                if (event.key == "Enter") {
                    onSearch()
                }
            }
            attr("placeholder", "Suchen...")
            classes(AppStyleSheet.searchInput)
        }
        Button({
            onClick { onSearch() }
            classes(AppStyleSheet.searchButton)
        }) {
            Text("Suchen")
        }
    }
}

@Composable
fun Breadcrumb(state: NavigationState, onNavigate: (NavigationState) -> Unit) {
    Div({ classes(AppStyleSheet.breadcrumb) }) {
        Span({
            classes(AppStyleSheet.breadcrumbItem)
            onClick { onNavigate(NavigationState.ChannelList) }
        }) { Text("Start") }

        when (state) {
            is NavigationState.SearchResults -> {
                Span({ classes(AppStyleSheet.breadcrumbSeparator) }) { Text(" > ") }
                Span({ classes(AppStyleSheet.breadcrumbItem, AppStyleSheet.breadcrumbActive) }) {
                    Text("Suche: \"${state.query}\"")
                }
            }
            is NavigationState.FilteredBrowse -> {
                Span({ classes(AppStyleSheet.breadcrumbSeparator) }) { Text(" > ") }
                Span({ classes(AppStyleSheet.breadcrumbItem, AppStyleSheet.breadcrumbActive) }) {
                    Text("Durchsuchen")
                }
            }
            is NavigationState.ThemeList -> {
                Span({ classes(AppStyleSheet.breadcrumbSeparator) }) { Text(" > ") }
                Span({ classes(AppStyleSheet.breadcrumbItem, AppStyleSheet.breadcrumbActive) }) {
                    Text(state.channel)
                }
            }
            is NavigationState.TitleList -> {
                Span({ classes(AppStyleSheet.breadcrumbSeparator) }) { Text(" > ") }
                Span({
                    classes(AppStyleSheet.breadcrumbItem)
                    onClick { onNavigate(NavigationState.ThemeList(state.channel)) }
                }) { Text(state.channel) }
                Span({ classes(AppStyleSheet.breadcrumbSeparator) }) { Text(" > ") }
                Span({ classes(AppStyleSheet.breadcrumbItem, AppStyleSheet.breadcrumbActive) }) {
                    Text(state.theme)
                }
            }
            is NavigationState.DetailView -> {
                Span({ classes(AppStyleSheet.breadcrumbSeparator) }) { Text(" > ") }
                Span({
                    classes(AppStyleSheet.breadcrumbItem)
                    onClick { onNavigate(NavigationState.ThemeList(state.channel)) }
                }) { Text(state.channel) }
                Span({ classes(AppStyleSheet.breadcrumbSeparator) }) { Text(" > ") }
                Span({
                    classes(AppStyleSheet.breadcrumbItem)
                    onClick { onNavigate(NavigationState.TitleList(state.channel, state.theme)) }
                }) { Text(state.theme) }
                Span({ classes(AppStyleSheet.breadcrumbSeparator) }) { Text(" > ") }
                Span({ classes(AppStyleSheet.breadcrumbItem, AppStyleSheet.breadcrumbActive) }) {
                    Text(state.title.take(30) + if (state.title.length > 30) "..." else "")
                }
            }
            else -> {}
        }
    }
}

@Composable
fun ChannelButton(
    broadcaster: Broadcaster,
    isSelected: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit
) {
    val bgColor = "#${(broadcaster.brandColor and 0xFFFFFF).toString(16).padStart(6, '0')}"

    Button({
        onClick { onClick() }
        if (isFocused) {
            attr("autofocus", "true")
        }
        style {
            backgroundColor(if (isSelected) Color(bgColor) else Color.transparent)
            color(if (isSelected) Color.white else Color(bgColor))
            border(2.px, LineStyle.Solid, Color(bgColor))
            padding(10.px, 16.px)
            marginBottom(6.px)
            width(100.percent)
            property("cursor", "pointer")
            borderRadius(6.px)
            textAlign("left")
            fontSize(14.px)
            fontWeight(if (isSelected) "bold" else "normal")
            property("transition", "all 0.2s ease")
            if (isFocused) {
                property("outline", "3px solid #FFD700")
                property("outline-offset", "2px")
            }
        }
    }) {
        Text(broadcaster.abbreviation.ifEmpty { broadcaster.name })
    }
}

@Composable
fun WelcomeView(onBrowseClick: () -> Unit = {}) {
    Div({ classes(AppStyleSheet.welcomeContainer) }) {
        H2({ classes(AppStyleSheet.welcomeTitle) }) { Text("Willkommen bei MediathekView") }
        P({ classes(AppStyleSheet.welcomeText) }) {
            Text("Bitte waehlen Sie einen Sender aus der Liste links, um Sendungen zu durchsuchen.")
        }

        // Browse button
        Button({
            onClick { onBrowseClick() }
            classes(AppStyleSheet.browseButton)
        }) {
            Text("Alle Sendungen durchsuchen")
        }

        Div({ classes(AppStyleSheet.welcomeHint) }) {
            P { Text("Navigation:") }
            Ul {
                Li { Text("Klicken Sie auf einen Sender, um Themen anzuzeigen") }
                Li { Text("Klicken Sie auf ein Thema, um Titel anzuzeigen") }
                Li { Text("Klicken Sie auf einen Titel fuer Details") }
                Li { Text("Druecken Sie ESC zum Zurueckgehen") }
                Li { Text("Nutzen Sie die Suche oben rechts") }
            }
        }
    }
}

@Composable
fun LoadingSpinner() {
    Div({ classes(AppStyleSheet.loadingContainer) }) {
        Div({ classes(AppStyleSheet.spinner) })
        Span({ style { marginLeft(12.px); color(Color("#888")) } }) { Text("Laden...") }
    }
}

@Composable
fun ThemeListView(
    channel: String,
    coroutineScope: CoroutineScope,
    onThemeClick: (String) -> Unit
) {
    var themes by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var totalCount by remember { mutableStateOf(0) }
    var hasMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(0) }

    val channelColor = Broadcaster.getBrandColorOfName(channel)
    val colorHex = "#${(channelColor and 0xFFFFFF).toString(16).padStart(6, '0')}"

    // Load initial data
    LaunchedEffect(channel) {
        isLoading = true
        themes = emptyList()
        currentPage = 0
        val result = repository.getThemes(channel, page = 0, pageSize = 20)
        themes = result.items
        totalCount = result.totalCount
        hasMore = result.hasMore
        currentPage = 0
        isLoading = false
    }

    fun loadMore() {
        coroutineScope.launch {
            isLoading = true
            val result = repository.getThemes(channel, page = currentPage + 1, pageSize = 20)
            themes = themes + result.items
            hasMore = result.hasMore
            currentPage++
            isLoading = false
        }
    }

    Div({ classes(AppStyleSheet.listContainer) }) {
        H2({
            style {
                color(Color(colorHex))
                marginBottom(8.px)
            }
        }) { Text("Themen von $channel") }

        if (totalCount > 0) {
            P({ classes(AppStyleSheet.resultCount) }) {
                Text("${themes.size} von $totalCount Themen")
            }
        }

        if (themes.isEmpty() && isLoading) {
            LoadingSpinner()
        } else {
            Div({ classes(AppStyleSheet.listGrid) }) {
                themes.forEach { theme ->
                    Div({
                        classes(AppStyleSheet.listItem)
                        onClick { onThemeClick(theme) }
                        style {
                            property("border-left", "4px solid $colorHex")
                        }
                        attr("tabindex", "0")
                        onKeyDown { event ->
                            if (event.key == "Enter") onThemeClick(theme)
                        }
                    }) {
                        Span({ classes(AppStyleSheet.listItemTitle) }) { Text(theme) }
                    }
                }
            }

            // Pagination
            if (hasMore || isLoading) {
                Div({ classes(AppStyleSheet.paginationContainer) }) {
                    if (isLoading) {
                        LoadingSpinner()
                    } else if (hasMore) {
                        Button({
                            onClick { loadMore() }
                            classes(AppStyleSheet.loadMoreButton)
                        }) {
                            Text("Mehr laden")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TitleListView(
    channel: String,
    theme: String,
    coroutineScope: CoroutineScope,
    onTitleClick: (String) -> Unit
) {
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var totalCount by remember { mutableStateOf(0) }
    var hasMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(0) }

    val channelColor = Broadcaster.getBrandColorOfName(channel)
    val colorHex = "#${(channelColor and 0xFFFFFF).toString(16).padStart(6, '0')}"

    // Load initial data
    LaunchedEffect(channel, theme) {
        isLoading = true
        items = emptyList()
        currentPage = 0
        val result = repository.getTitles(channel, theme, page = 0, pageSize = 20)
        items = result.items
        totalCount = result.totalCount
        hasMore = result.hasMore
        currentPage = 0
        isLoading = false
    }

    fun loadMore() {
        coroutineScope.launch {
            isLoading = true
            val result = repository.getTitles(channel, theme, page = currentPage + 1, pageSize = 20)
            items = items + result.items
            hasMore = result.hasMore
            currentPage++
            isLoading = false
        }
    }

    Div({ classes(AppStyleSheet.listContainer) }) {
        H2({
            style {
                color(Color(colorHex))
                marginBottom(8.px)
            }
        }) { Text(theme) }
        P({
            style {
                color(Color("#888"))
                marginBottom(8.px)
            }
        }) { Text("Sender: $channel") }

        if (totalCount > 0) {
            P({ classes(AppStyleSheet.resultCount) }) {
                Text("${items.size} von $totalCount Sendungen")
            }
        }

        if (items.isEmpty() && isLoading) {
            LoadingSpinner()
        } else {
            Div({ classes(AppStyleSheet.listGrid) }) {
                items.forEach { item ->
                    Div({
                        classes(AppStyleSheet.listItem)
                        onClick { onTitleClick(item.title) }
                        style {
                            property("border-left", "4px solid $colorHex")
                        }
                        attr("tabindex", "0")
                        onKeyDown { event ->
                            if (event.key == "Enter") onTitleClick(item.title)
                        }
                    }) {
                        Span({ classes(AppStyleSheet.listItemTitle) }) { Text(item.title) }
                        Span({ classes(AppStyleSheet.listItemMeta) }) {
                            Text("${item.date} - ${item.duration}")
                        }
                    }
                }
            }

            // Pagination
            if (hasMore || isLoading) {
                Div({ classes(AppStyleSheet.paginationContainer) }) {
                    if (isLoading) {
                        LoadingSpinner()
                    } else if (hasMore) {
                        Button({
                            onClick { loadMore() }
                            classes(AppStyleSheet.loadMoreButton)
                        }) {
                            Text("Mehr laden")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultsView(
    query: String,
    coroutineScope: CoroutineScope,
    onItemClick: (MediaItem) -> Unit
) {
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var totalCount by remember { mutableStateOf(0) }
    var hasMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(0) }

    // Load initial data
    LaunchedEffect(query) {
        isLoading = true
        items = emptyList()
        currentPage = 0
        val result = repository.search(query, page = 0, pageSize = 20)
        items = result.items
        totalCount = result.totalCount
        hasMore = result.hasMore
        currentPage = 0
        isLoading = false
    }

    fun loadMore() {
        coroutineScope.launch {
            isLoading = true
            val result = repository.search(query, page = currentPage + 1, pageSize = 20)
            items = items + result.items
            hasMore = result.hasMore
            currentPage++
            isLoading = false
        }
    }

    Div({ classes(AppStyleSheet.listContainer) }) {
        H2({
            style {
                color(Color("#81B4D2"))
                marginBottom(8.px)
            }
        }) { Text("Suchergebnisse fuer \"$query\"") }

        if (totalCount > 0) {
            P({ classes(AppStyleSheet.resultCount) }) {
                Text("${items.size} von $totalCount Ergebnissen")
            }
        }

        if (items.isEmpty() && isLoading) {
            LoadingSpinner()
        } else if (items.isEmpty()) {
            Div({ classes(AppStyleSheet.emptyState) }) {
                P { Text("Keine Ergebnisse gefunden.") }
            }
        } else {
            Div({ classes(AppStyleSheet.listGrid) }) {
                items.forEach { item ->
                    val channelColor = Broadcaster.getBrandColorOfName(item.channel)
                    val colorHex = "#${(channelColor and 0xFFFFFF).toString(16).padStart(6, '0')}"

                    Div({
                        classes(AppStyleSheet.listItem)
                        onClick { onItemClick(item) }
                        style {
                            property("border-left", "4px solid $colorHex")
                        }
                        attr("tabindex", "0")
                        onKeyDown { event ->
                            if (event.key == "Enter") onItemClick(item)
                        }
                    }) {
                        Div({ classes(AppStyleSheet.searchResultHeader) }) {
                            Span({
                                classes(AppStyleSheet.channelTag)
                                style { backgroundColor(Color(colorHex)) }
                            }) { Text(Broadcaster.getAbbreviationOfName(item.channel)) }
                            Span({ classes(AppStyleSheet.listItemMeta) }) { Text(item.theme) }
                        }
                        Span({ classes(AppStyleSheet.listItemTitle) }) { Text(item.title) }
                        Span({ classes(AppStyleSheet.listItemMeta) }) {
                            Text("${item.date} - ${item.duration}")
                        }
                    }
                }
            }

            // Pagination
            if (hasMore || isLoading) {
                Div({ classes(AppStyleSheet.paginationContainer) }) {
                    if (isLoading) {
                        LoadingSpinner()
                    } else if (hasMore) {
                        Button({
                            onClick { loadMore() }
                            classes(AppStyleSheet.loadMoreButton)
                        }) {
                            Text("Mehr laden")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilteredBrowseView(
    dateFilter: DateFilter,
    durationFilter: DurationFilter,
    channel: String?,
    coroutineScope: CoroutineScope,
    onFilterChange: (DateFilter, DurationFilter, String?) -> Unit,
    onItemClick: (MediaItem) -> Unit
) {
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var totalCount by remember { mutableStateOf(0) }
    var hasMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(0) }

    // Load initial data when filters change
    LaunchedEffect(dateFilter, durationFilter, channel) {
        isLoading = true
        items = emptyList()
        currentPage = 0
        val result = repository.getFilteredItems(
            dateFilter = dateFilter,
            durationFilter = durationFilter,
            channel = channel,
            page = 0,
            pageSize = 20
        )
        items = result.items
        totalCount = result.totalCount
        hasMore = result.hasMore
        currentPage = 0
        isLoading = false
    }

    fun loadMore() {
        coroutineScope.launch {
            isLoading = true
            val result = repository.getFilteredItems(
                dateFilter = dateFilter,
                durationFilter = durationFilter,
                channel = channel,
                page = currentPage + 1,
                pageSize = 20
            )
            items = items + result.items
            hasMore = result.hasMore
            currentPage++
            isLoading = false
        }
    }

    Div({ classes(AppStyleSheet.listContainer) }) {
        H2({
            style {
                color(Color("#81B4D2"))
                marginBottom(16.px)
            }
        }) { Text("Sendungen durchsuchen") }

        // Filter toolbar
        Div({ classes(AppStyleSheet.filterToolbar) }) {
            // Date filter
            Div({ classes(AppStyleSheet.filterGroup) }) {
                Span({ classes(AppStyleSheet.filterLabel) }) { Text("Zeitraum:") }
                Div({ classes(AppStyleSheet.filterButtons) }) {
                    FilterButton("Alle", dateFilter == DateFilter.ALL) {
                        onFilterChange(DateFilter.ALL, durationFilter, channel)
                    }
                    FilterButton("Heute", dateFilter == DateFilter.TODAY) {
                        onFilterChange(DateFilter.TODAY, durationFilter, channel)
                    }
                    FilterButton("Woche", dateFilter == DateFilter.LAST_WEEK) {
                        onFilterChange(DateFilter.LAST_WEEK, durationFilter, channel)
                    }
                    FilterButton("Monat", dateFilter == DateFilter.LAST_MONTH) {
                        onFilterChange(DateFilter.LAST_MONTH, durationFilter, channel)
                    }
                }
            }

            // Duration filter
            Div({ classes(AppStyleSheet.filterGroup) }) {
                Span({ classes(AppStyleSheet.filterLabel) }) { Text("Dauer:") }
                Div({ classes(AppStyleSheet.filterButtons) }) {
                    FilterButton("Alle", durationFilter == DurationFilter.ALL) {
                        onFilterChange(dateFilter, DurationFilter.ALL, channel)
                    }
                    FilterButton("<15 Min", durationFilter == DurationFilter.SHORT) {
                        onFilterChange(dateFilter, DurationFilter.SHORT, channel)
                    }
                    FilterButton("15-60 Min", durationFilter == DurationFilter.MEDIUM) {
                        onFilterChange(dateFilter, DurationFilter.MEDIUM, channel)
                    }
                    FilterButton(">60 Min", durationFilter == DurationFilter.LONG) {
                        onFilterChange(dateFilter, DurationFilter.LONG, channel)
                    }
                }
            }

            // Channel filter
            Div({ classes(AppStyleSheet.filterGroup) }) {
                Span({ classes(AppStyleSheet.filterLabel) }) { Text("Sender:") }
                Select({
                    onChange { event ->
                        val selectedChannel = event.value?.takeIf { it.isNotEmpty() }
                        onFilterChange(dateFilter, durationFilter, selectedChannel)
                    }
                    classes(AppStyleSheet.filterSelect)
                }) {
                    Option("", { if (channel == null) attr("selected", "selected") }) { Text("Alle Sender") }
                    Broadcaster.channelList.forEach { broadcaster ->
                        Option(broadcaster.name, {
                            if (channel == broadcaster.name) attr("selected", "selected")
                        }) {
                            Text(broadcaster.name)
                        }
                    }
                }
            }
        }

        // Results count
        if (totalCount > 0) {
            P({ classes(AppStyleSheet.resultCount) }) {
                Text("${items.size} von $totalCount Sendungen")
            }
        }

        // Results
        if (items.isEmpty() && isLoading) {
            LoadingSpinner()
        } else if (items.isEmpty()) {
            Div({ classes(AppStyleSheet.emptyState) }) {
                P { Text("Keine Sendungen mit diesen Filtern gefunden.") }
            }
        } else {
            Div({ classes(AppStyleSheet.listGrid) }) {
                items.forEach { item ->
                    val channelColor = Broadcaster.getBrandColorOfName(item.channel)
                    val colorHex = "#${(channelColor and 0xFFFFFF).toString(16).padStart(6, '0')}"

                    Div({
                        classes(AppStyleSheet.listItem)
                        onClick { onItemClick(item) }
                        style {
                            property("border-left", "4px solid $colorHex")
                        }
                        attr("tabindex", "0")
                        onKeyDown { event ->
                            if (event.key == "Enter") onItemClick(item)
                        }
                    }) {
                        Div({ classes(AppStyleSheet.searchResultHeader) }) {
                            Span({
                                classes(AppStyleSheet.channelTag)
                                style { backgroundColor(Color(colorHex)) }
                            }) { Text(Broadcaster.getAbbreviationOfName(item.channel)) }
                            Span({ classes(AppStyleSheet.listItemMeta) }) { Text(item.theme) }
                        }
                        Span({ classes(AppStyleSheet.listItemTitle) }) { Text(item.title) }
                        Span({ classes(AppStyleSheet.listItemMeta) }) {
                            Text("${item.date} - ${item.duration}")
                        }
                    }
                }
            }

            // Pagination
            if (hasMore || isLoading) {
                Div({ classes(AppStyleSheet.paginationContainer) }) {
                    if (isLoading) {
                        LoadingSpinner()
                    } else if (hasMore) {
                        Button({
                            onClick { loadMore() }
                            classes(AppStyleSheet.loadMoreButton)
                        }) {
                            Text("Mehr laden")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterButton(label: String, isActive: Boolean, onClick: () -> Unit) {
    Button({
        onClick { onClick() }
        classes(AppStyleSheet.filterButton)
        if (isActive) classes(AppStyleSheet.filterButtonActive)
    }) {
        Text(label)
    }
}

@Composable
fun DetailViewAsync(
    channel: String,
    theme: String,
    title: String,
    coroutineScope: CoroutineScope,
    onBack: () -> Unit
) {
    var mediaItem by remember { mutableStateOf<MediaItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(channel, theme, title) {
        isLoading = true
        mediaItem = repository.getMediaItem(channel, theme, title)
        isLoading = false
    }

    if (isLoading) {
        LoadingSpinner()
    } else if (mediaItem != null) {
        DetailView(mediaItem!!, onBack)
    } else {
        Div({ classes(AppStyleSheet.emptyState) }) {
            P { Text("Sendung nicht gefunden.") }
            Button({
                onClick { onBack() }
                classes(AppStyleSheet.backButton)
            }) { Text("< Zurueck") }
        }
    }
}

@Composable
fun DetailView(mediaItem: MediaItem, onBack: () -> Unit) {
    val channelColor = Broadcaster.getBrandColorOfName(mediaItem.channel)
    val colorHex = "#${(channelColor and 0xFFFFFF).toString(16).padStart(6, '0')}"

    Div({ classes(AppStyleSheet.detailContainer) }) {
        // Back button
        Button({
            onClick { onBack() }
            classes(AppStyleSheet.backButton)
        }) {
            Text("< Zurueck")
        }

        // Channel badge
        Div({
            classes(AppStyleSheet.channelBadge)
            style {
                backgroundColor(Color(colorHex))
            }
        }) {
            Text(Broadcaster.getAbbreviationOfName(mediaItem.channel))
        }

        // Theme
        H3({
            style {
                color(Color("#81B4D2"))
                marginTop(16.px)
                marginBottom(8.px)
            }
        }) { Text(mediaItem.theme) }

        // Title
        H2({
            style {
                color(Color.white)
                marginBottom(16.px)
            }
        }) { Text(mediaItem.title) }

        // Metadata grid
        Div({ classes(AppStyleSheet.metadataGrid) }) {
            MetadataRow("Datum", mediaItem.date)
            MetadataRow("Zeit", mediaItem.time)
            MetadataRow("Dauer", mediaItem.duration)
            MetadataRow("Groesse", mediaItem.size)
        }

        // Description
        Div({ classes(AppStyleSheet.descriptionCard) }) {
            H4({
                style {
                    color(Color("#81B4D2"))
                    marginBottom(8.px)
                }
            }) { Text("Beschreibung") }
            P({
                style { color(Color.white) }
            }) { Text(mediaItem.description) }
        }

        // URLs (if available)
        if (mediaItem.url.isNotEmpty()) {
            Div({ classes(AppStyleSheet.urlSection) }) {
                H4({
                    style {
                        color(Color("#81B4D2"))
                        marginBottom(8.px)
                    }
                }) { Text("Links") }
                Div({ classes(AppStyleSheet.urlList) }) {
                    A(href = mediaItem.url, {
                        attr("target", "_blank")
                        classes(AppStyleSheet.urlLink)
                    }) { Text("Video (Standard)") }
                    if (mediaItem.hdUrl.isNotEmpty()) {
                        A(href = mediaItem.hdUrl, {
                            attr("target", "_blank")
                            classes(AppStyleSheet.urlLink)
                        }) { Text("Video (HD)") }
                    }
                }
            }
        }

        // Action buttons
        Div({ classes(AppStyleSheet.actionButtons) }) {
            Button({
                classes(AppStyleSheet.playButton)
                onClick {
                    if (mediaItem.url.isNotEmpty()) {
                        window.open(mediaItem.url, "_blank")
                    } else {
                        window.alert("Keine Video-URL verfuegbar")
                    }
                }
            }) {
                Text("Abspielen")
            }
            Button({
                classes(AppStyleSheet.downloadButton)
                onClick {
                    if (mediaItem.url.isNotEmpty()) {
                        window.open(mediaItem.url, "_blank")
                    } else {
                        window.alert("Keine Download-URL verfuegbar")
                    }
                }
            }) {
                Text("Herunterladen")
            }
        }
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Div({ classes(AppStyleSheet.metadataRow) }) {
        Span({ classes(AppStyleSheet.metadataLabel) }) { Text(label) }
        Span({ classes(AppStyleSheet.metadataValue) }) { Text(value) }
    }
}

// ============================================
// Stylesheet
// ============================================

object AppStyleSheet : StyleSheet() {
    val container by style {
        fontFamily("Arial", "Helvetica", "sans-serif")
        property("min-height", "100vh")
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        backgroundColor(Color("#0a0a0a"))
        color(Color.white)
    }

    val header by style {
        backgroundColor(Color("#1a1a1a"))
        padding(12.px, 24.px)
        display(DisplayStyle.Flex)
        justifyContent(JustifyContent.SpaceBetween)
        alignItems(AlignItems.Center)
        property("border-bottom", "1px solid #333")
    }

    val headerLeft by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
    }

    val headerCenter by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        property("flex", "1")
        justifyContent(JustifyContent.Center)
    }

    val headerRight by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
    }

    val logo by style {
        fontSize(24.px)
        fontWeight("bold")
        color(Color("#81B4D2"))
        margin(0.px)
    }

    val breadcrumb by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        fontSize(14.px)
    }

    val breadcrumbItem by style {
        color(Color("#888"))
        property("cursor", "pointer")
        padding(4.px, 8.px)
        borderRadius(4.px)
        property("transition", "background-color 0.2s")
        self + hover style {
            backgroundColor(Color("#333"))
        }
    }

    val breadcrumbActive by style {
        color(Color.white)
        fontWeight("bold")
    }

    val breadcrumbSeparator by style {
        color(Color("#555"))
        padding(0.px, 4.px)
    }

    val searchContainer by style {
        display(DisplayStyle.Flex)
        gap(8.px)
    }

    val searchInput by style {
        padding(10.px, 16.px)
        fontSize(14.px)
        borderRadius(6.px, 0.px, 0.px, 6.px)
        border(1.px, LineStyle.Solid, Color("#444"))
        backgroundColor(Color("#2a2a2a"))
        color(Color.white)
        width(200.px)
        property("outline", "none")
        self + focus style {
            property("border-color", "#81B4D2")
            property("box-shadow", "0 0 0 2px rgba(129, 180, 210, 0.2)")
        }
    }

    val searchButton by style {
        padding(10.px, 16.px)
        fontSize(14.px)
        borderRadius(0.px, 6.px, 6.px, 0.px)
        border(1.px, LineStyle.Solid, Color("#0088AA"))
        backgroundColor(Color("#0088AA"))
        color(Color.white)
        property("cursor", "pointer")
        property("transition", "background-color 0.2s")
        self + hover style {
            backgroundColor(Color("#00AACC"))
        }
    }

    val mainContent by style {
        display(DisplayStyle.Flex)
        property("flex", "1")
        property("overflow", "hidden")
    }

    val sidebar by style {
        width(220.px)
        backgroundColor(Color("#151515"))
        padding(16.px)
        property("overflow-y", "auto")
        property("border-right", "1px solid #333")
    }

    val sidebarTitle by style {
        fontSize(14.px)
        color(Color("#888"))
        property("text-transform", "uppercase")
        marginBottom(12.px)
        property("letter-spacing", "1px")
    }

    val channelList by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
    }

    val content by style {
        property("flex", "1")
        padding(24.px)
        property("overflow-y", "auto")
    }

    val welcomeContainer by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
        property("min-height", "60vh")
        textAlign("center")
    }

    val welcomeTitle by style {
        fontSize(32.px)
        color(Color("#81B4D2"))
        marginBottom(16.px)
    }

    val welcomeText by style {
        fontSize(18.px)
        color(Color("#aaa"))
        marginBottom(32.px)
    }

    val welcomeHint by style {
        backgroundColor(Color("#1a1a1a"))
        padding(24.px)
        borderRadius(8.px)
        textAlign("left")
        color(Color("#888"))
    }

    val listContainer by style {
        property("max-width", "1200px")
    }

    val listGrid by style {
        display(DisplayStyle.Grid)
        property("grid-template-columns", "repeat(auto-fill, minmax(300px, 1fr))")
        gap(12.px)
    }

    val listItem by style {
        backgroundColor(Color("#1a1a1a"))
        padding(16.px)
        borderRadius(8.px)
        property("cursor", "pointer")
        property("transition", "all 0.2s ease")
        self + hover style {
            backgroundColor(Color("#252525"))
            property("transform", "translateX(4px)")
        }
        self + focus style {
            property("outline", "3px solid #FFD700")
            property("outline-offset", "2px")
        }
    }

    val listItemTitle by style {
        display(DisplayStyle.Block)
        fontSize(16.px)
        fontWeight("bold")
        marginBottom(4.px)
        color(Color.white)
    }

    val listItemMeta by style {
        display(DisplayStyle.Block)
        fontSize(12.px)
        color(Color("#888"))
    }

    val resultCount by style {
        color(Color("#888"))
        fontSize(14.px)
        marginBottom(16.px)
    }

    val loadingContainer by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
        padding(32.px)
    }

    val spinner by style {
        width(24.px)
        height(24.px)
        border(3.px, LineStyle.Solid, Color("#333"))
        property("border-top-color", "#81B4D2")
        borderRadius(50.percent)
        property("animation", "spin 1s linear infinite")
    }

    val paginationContainer by style {
        display(DisplayStyle.Flex)
        justifyContent(JustifyContent.Center)
        marginTop(24.px)
    }

    val loadMoreButton by style {
        backgroundColor(Color("#2a2a2a"))
        color(Color("#81B4D2"))
        border(1.px, LineStyle.Solid, Color("#444"))
        padding(12.px, 32.px)
        borderRadius(6.px)
        fontSize(14.px)
        property("cursor", "pointer")
        property("transition", "all 0.2s ease")
        self + hover style {
            backgroundColor(Color("#333"))
            property("border-color", "#81B4D2")
        }
        self + focus style {
            property("outline", "3px solid #FFD700")
            property("outline-offset", "2px")
        }
    }

    val emptyState by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
        padding(48.px)
        color(Color("#888"))
    }

    val browseButton by style {
        backgroundColor(Color("#0088AA"))
        color(Color.white)
        border(0.px)
        padding(16.px, 48.px)
        borderRadius(8.px)
        fontSize(18.px)
        fontWeight("bold")
        property("cursor", "pointer")
        property("transition", "background-color 0.2s")
        marginBottom(32.px)
        self + hover style {
            backgroundColor(Color("#00AACC"))
        }
        self + focus style {
            property("outline", "3px solid #FFD700")
            property("outline-offset", "2px")
        }
    }

    val filterToolbar by style {
        display(DisplayStyle.Flex)
        flexWrap(FlexWrap.Wrap)
        gap(24.px)
        padding(16.px)
        backgroundColor(Color("#1a1a1a"))
        borderRadius(8.px)
        marginBottom(24.px)
    }

    val filterGroup by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        gap(8.px)
    }

    val filterLabel by style {
        color(Color("#888"))
        fontSize(14.px)
        property("white-space", "nowrap")
    }

    val filterButtons by style {
        display(DisplayStyle.Flex)
        gap(4.px)
    }

    val filterButton by style {
        backgroundColor(Color("#2a2a2a"))
        color(Color("#aaa"))
        border(1.px, LineStyle.Solid, Color("#444"))
        padding(6.px, 12.px)
        borderRadius(4.px)
        fontSize(12.px)
        property("cursor", "pointer")
        property("transition", "all 0.2s ease")
        self + hover style {
            backgroundColor(Color("#333"))
            color(Color.white)
        }
        self + focus style {
            property("outline", "2px solid #FFD700")
            property("outline-offset", "1px")
        }
    }

    val filterButtonActive by style {
        backgroundColor(Color("#0088AA"))
        color(Color.white)
        property("border-color", "#0088AA")
    }

    val filterSelect by style {
        backgroundColor(Color("#2a2a2a"))
        color(Color.white)
        border(1.px, LineStyle.Solid, Color("#444"))
        padding(6.px, 12.px)
        borderRadius(4.px)
        fontSize(12.px)
        property("cursor", "pointer")
        property("outline", "none")
        self + focus style {
            property("border-color", "#81B4D2")
        }
    }

    val searchResultHeader by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        gap(8.px)
        marginBottom(8.px)
    }

    val channelTag by style {
        display(DisplayStyle.InlineBlock)
        padding(2.px, 8.px)
        borderRadius(4.px)
        fontSize(11.px)
        fontWeight("bold")
        color(Color.white)
    }

    val detailContainer by style {
        property("max-width", "800px")
    }

    val backButton by style {
        backgroundColor(Color.transparent)
        color(Color("#81B4D2"))
        border(0.px)
        padding(8.px, 0.px)
        fontSize(14.px)
        property("cursor", "pointer")
        marginBottom(16.px)
        self + hover style {
            textDecoration("underline")
        }
    }

    val channelBadge by style {
        display(DisplayStyle.InlineBlock)
        padding(16.px, 32.px)
        borderRadius(8.px)
        fontSize(24.px)
        fontWeight("bold")
        color(Color.white)
    }

    val metadataGrid by style {
        display(DisplayStyle.Grid)
        property("grid-template-columns", "repeat(2, 1fr)")
        gap(12.px)
        marginBottom(24.px)
    }

    val metadataRow by style {
        display(DisplayStyle.Flex)
        gap(8.px)
    }

    val metadataLabel by style {
        color(Color("#888"))
        width(80.px)
    }

    val metadataValue by style {
        color(Color.white)
    }

    val descriptionCard by style {
        backgroundColor(Color("#1a1a1a"))
        padding(16.px)
        borderRadius(8.px)
        marginBottom(24.px)
    }

    val urlSection by style {
        marginBottom(24.px)
    }

    val urlList by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        gap(8.px)
    }

    val urlLink by style {
        color(Color("#81B4D2"))
        textDecoration("none")
        self + hover style {
            textDecoration("underline")
        }
    }

    val actionButtons by style {
        display(DisplayStyle.Flex)
        gap(12.px)
    }

    val playButton by style {
        backgroundColor(Color("#0088AA"))
        color(Color.white)
        border(0.px)
        padding(12.px, 32.px)
        borderRadius(6.px)
        fontSize(16.px)
        fontWeight("bold")
        property("cursor", "pointer")
        property("transition", "background-color 0.2s")
        self + hover style {
            backgroundColor(Color("#00AACC"))
        }
        self + focus style {
            property("outline", "3px solid #FFD700")
            property("outline-offset", "2px")
        }
    }

    val downloadButton by style {
        backgroundColor(Color("#3A4A5A"))
        color(Color.white)
        border(0.px)
        padding(12.px, 32.px)
        borderRadius(6.px)
        fontSize(16.px)
        fontWeight("bold")
        property("cursor", "pointer")
        property("transition", "background-color 0.2s")
        self + hover style {
            backgroundColor(Color("#4A5A6A"))
        }
        self + focus style {
            property("outline", "3px solid #FFD700")
            property("outline-offset", "2px")
        }
    }

    val footer by style {
        backgroundColor(Color("#1a1a1a"))
        padding(12.px)
        textAlign("center")
        fontSize(12.px)
        color(Color("#666"))
        property("border-top", "1px solid #333")
    }

    // Keyframes injection via init block
    init {
        // Add spinner animation via raw CSS
        val styleElement = document.createElement("style")
        styleElement.textContent = """
            @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
            }
        """.trimIndent()
        document.head?.appendChild(styleElement)
    }
}
