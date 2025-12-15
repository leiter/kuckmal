package com.mediathekview.android.compose.ui.theme

import androidx.compose.runtime.Composable
// Re-export the shared theme for backward compatibility
import com.mediathekview.shared.ui.theme.MediathekViewTheme as SharedMediathekViewTheme

/**
 * MediathekView theme wrapper for the app module
 * Delegates to the shared module's cross-platform theme implementation
 *
 * @param darkTheme Whether to use dark theme (defaults to system setting)
 * @param dynamicColor Whether to use platform dynamic colors (Android 12+)
 * @param content The content to display with this theme
 */
@Composable
fun MediathekViewTheme(
    darkTheme: Boolean = com.mediathekview.shared.ui.theme.isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    SharedMediathekViewTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
