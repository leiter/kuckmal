package cut.the.crap.shared.ui.navigation

/**
 * Navigation routes - simplified to just two screens
 * KMP-compatible - no platform-specific dependencies
 */
sealed class Screen(val route: String) {
    // Overview screen (handles all browsing: themes, channels, titles)
    data object Overview : Screen("overview")

    // Detail screen - uses URL encoding for parameters
    data class Detail(
        val title: String,
        val channel: String? = null,
        val theme: String? = null
    ) : Screen("detail/{title}?channel={channel}&theme={theme}") {
        companion object {
            const val ROUTE_PATTERN = "detail/{title}?channel={channel}&theme={theme}"

            fun createRoute(
                title: String,
                channel: String? = null,
                theme: String? = null
            ): String {
                val encodedTitle = urlEncode(title)
                val encodedChannel = channel?.let { urlEncode(it) } ?: ""
                val encodedTheme = theme?.let { urlEncode(it) } ?: ""
                return "detail/$encodedTitle?channel=$encodedChannel&theme=$encodedTheme"
            }
        }
    }
}

/**
 * Overview screen state - tracks what data is being displayed
 */
sealed class OverviewState {
    // Showing all themes (no channel filter)
    data object AllThemes : OverviewState()

    // Showing themes for a specific channel
    data class ChannelThemes(val channelName: String) : OverviewState()

    // Showing titles within a theme
    data class ThemeTitles(val channelName: String?, val themeName: String) : OverviewState()
}

/**
 * Simple URL encoding for navigation parameters
 * Encodes special characters that could break navigation routes
 */
fun urlEncode(value: String): String {
    return buildString {
        for (char in value) {
            when (char) {
                ' ' -> append("%20")
                '/' -> append("%2F")
                '?' -> append("%3F")
                '&' -> append("%26")
                '=' -> append("%3D")
                '#' -> append("%23")
                '%' -> append("%25")
                else -> append(char)
            }
        }
    }
}

/**
 * Simple URL decoding for navigation parameters
 */
fun urlDecode(value: String): String {
    return value
        .replace("%20", " ")
        .replace("%2F", "/")
        .replace("%3F", "?")
        .replace("%26", "&")
        .replace("%3D", "=")
        .replace("%23", "#")
        .replace("%25", "%")
}
