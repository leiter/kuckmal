package com.mediathekview.shared.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import java.awt.Toolkit

/**
 * Desktop implementation of platform color scheme
 * Desktop doesn't support dynamic colors, so we use the standard color schemes
 */
@Composable
actual fun getPlatformColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme {
    // Desktop doesn't support Material You dynamic colors
    // Just use the standard color schemes
    return if (darkTheme) DarkColorScheme else LightColorScheme
}

/**
 * Desktop implementation of system dark theme check
 * Attempts to detect OS dark mode setting
 */
@Composable
actual fun isSystemInDarkTheme(): Boolean {
    return try {
        // Try to detect system dark mode
        // This works on some systems but not all
        val toolkit = Toolkit.getDefaultToolkit()
        val desktopProperty = toolkit.getDesktopProperty("awt.os.theme")

        when {
            desktopProperty?.toString()?.lowercase()?.contains("dark") == true -> true
            // Check GTK theme on Linux
            System.getenv("GTK_THEME")?.lowercase()?.contains("dark") == true -> true
            // Check for common dark theme indicators
            System.getProperty("os.name")?.lowercase()?.contains("mac") == true -> {
                // macOS: try to detect appearance
                try {
                    val process = Runtime.getRuntime().exec(
                        arrayOf("defaults", "read", "-g", "AppleInterfaceStyle")
                    )
                    val result = process.inputStream.bufferedReader().readText().trim()
                    result.lowercase() == "dark"
                } catch (e: Exception) {
                    false
                }
            }
            else -> false
        }
    } catch (e: Exception) {
        // Default to light theme if detection fails
        false
    }
}
