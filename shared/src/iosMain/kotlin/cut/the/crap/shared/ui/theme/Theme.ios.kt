package cut.the.crap.shared.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle

/**
 * iOS implementation of platform color scheme
 * iOS doesn't support dynamic colors like Android 12+, so we always return standard schemes
 */
@Composable
actual fun getPlatformColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme {
    // iOS doesn't have dynamic color support like Android Material You
    // Always return standard color schemes
    return if (darkTheme) DarkColorScheme else LightColorScheme
}

/**
 * iOS implementation of system dark theme check
 * Uses UITraitCollection to detect current interface style
 */
@Composable
actual fun isSystemInDarkTheme(): Boolean {
    return remember {
        val traitCollection = UIScreen.mainScreen.traitCollection
        traitCollection.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark
    }
}
