package cut.the.crap.shared.ui.util

import androidx.compose.runtime.Composable

@Composable
actual fun rememberOrientation(): Orientation {
    // Desktop windows are typically landscape-oriented
    return Orientation.Landscape
}
