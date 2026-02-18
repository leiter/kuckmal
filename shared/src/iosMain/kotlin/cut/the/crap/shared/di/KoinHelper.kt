package cut.the.crap.shared.di

import cut.the.crap.shared.viewmodel.SharedViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin

/**
 * Helper object for iOS Koin initialization
 * Called from Swift: KoinHelperKt.doInitKoin()
 */
fun doInitKoin() {
    startKoin {
        modules(iosModule)
    }
    // Initialize video downloader callbacks
    initializeVideoDownloader()
}

/**
 * Helper class to access Koin dependencies from iOS
 * Usage in Kotlin: KoinHelper().getSharedViewModel()
 */
class KoinHelper : KoinComponent {
    fun getSharedViewModel(): SharedViewModel = get()
}

/**
 * Deep link handler for iOS
 * Called from Swift: DeepLinkHandlerKt.handleDeepLink(url)
 *
 * Supported URLs:
 * - kuckmal://play?channel=ARD&theme=Tagesschau&title=VideoTitle
 * - kuckmal://browse?channel=ZDF
 * - kuckmal://search?q=tatort
 */
fun handleDeepLink(urlString: String): Boolean {
    val helper = KoinHelper()
    val viewModel = helper.getSharedViewModel()

    // Parse URL components
    val url = parseUrl(urlString) ?: return false
    if (url.scheme != "kuckmal") return false

    return when (url.host) {
        "play" -> {
            val channel = url.queryParams["channel"]
            val theme = url.queryParams["theme"]
            val title = url.queryParams["title"] ?: return false
            // First navigate to the correct context, then to detail
            if (channel != null || theme != null) {
                viewModel.navigateToThemes(channel, theme)
            }
            viewModel.navigateToDetail(title)
            true
        }
        "browse" -> {
            val channel = url.queryParams["channel"]
            val theme = url.queryParams["theme"]
            viewModel.navigateToThemes(channel, theme)
            true
        }
        "search" -> {
            // Search is handled via navigateToThemes with searchFilter set in UI layer
            // Deep link can navigate to themes view where user can initiate search
            viewModel.navigateToThemes()
            true
        }
        else -> false
    }
}

/**
 * Simple URL parsing for deep links
 */
private data class ParsedUrl(
    val scheme: String,
    val host: String?,
    val queryParams: Map<String, String>
)

private fun parseUrl(urlString: String): ParsedUrl? {
    // Parse scheme
    val schemeEnd = urlString.indexOf("://")
    if (schemeEnd < 0) return null

    val scheme = urlString.substring(0, schemeEnd)
    val rest = urlString.substring(schemeEnd + 3)

    // Parse host and query
    val queryStart = rest.indexOf('?')
    val host: String?
    val queryString: String?

    if (queryStart >= 0) {
        host = rest.substring(0, queryStart).takeIf { it.isNotEmpty() }
        queryString = rest.substring(queryStart + 1)
    } else {
        host = rest.takeIf { it.isNotEmpty() }
        queryString = null
    }

    // Parse query parameters
    val queryParams = mutableMapOf<String, String>()
    queryString?.split('&')?.forEach { param ->
        val equalsIndex = param.indexOf('=')
        if (equalsIndex > 0) {
            val key = param.substring(0, equalsIndex)
            val value = param.substring(equalsIndex + 1)
                .replace("%20", " ")
                .replace("+", " ")
            queryParams[key] = value
        }
    }

    return ParsedUrl(scheme, host, queryParams)
}
