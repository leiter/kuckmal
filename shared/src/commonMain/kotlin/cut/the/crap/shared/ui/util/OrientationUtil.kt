package cut.the.crap.shared.ui.util

import androidx.compose.runtime.Composable

/**
 * Enum representing device orientation
 */
enum class Orientation {
    Portrait,
    Landscape
}

/**
 * Returns the current device orientation.
 * Platform-specific implementations detect actual device orientation.
 */
@Composable
expect fun rememberOrientation(): Orientation
