package com.mediathekview.shared.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import kotlinx.browser.window

/**
 * JS/Web implementation of platform color scheme
 * No dynamic colors on web - always returns standard schemes
 */
@Composable
actual fun getPlatformColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme {
    // Web doesn't support dynamic colors like Android Material You
    // Always use standard color schemes
    return if (darkTheme) DarkColorScheme else LightColorScheme
}

/**
 * JS/Web implementation of system dark theme check
 * Uses CSS media query to detect system preference
 */
@Composable
actual fun isSystemInDarkTheme(): Boolean {
    return try {
        // Check CSS media query for dark mode preference
        window.matchMedia("(prefers-color-scheme: dark)").matches
    } catch (e: Exception) {
        // Default to light theme if detection fails
        false
    }
}
