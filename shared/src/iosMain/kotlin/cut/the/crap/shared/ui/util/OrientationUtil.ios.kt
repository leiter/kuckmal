package cut.the.crap.shared.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIScreen

/**
 * iOS implementation of orientation detection
 * Uses UIScreen bounds to determine landscape vs portrait
 */
@Composable
actual fun rememberOrientation(): Orientation {
    return remember {
        val bounds = UIScreen.mainScreen.bounds
        val width = bounds.useContents { size.width }
        val height = bounds.useContents { size.height }
        if (width > height) {
            Orientation.Landscape
        } else {
            Orientation.Portrait
        }
    }
}
