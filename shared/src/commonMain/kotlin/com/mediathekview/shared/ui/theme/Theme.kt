package com.mediathekview.shared.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Shared color schemes for MediathekView theme
 * These are the fallback schemes when platform-specific dynamic colors are not available
 */
val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = TertiaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = OnPrimaryDark,
    onSecondary = OnSecondaryDark,
    onTertiary = OnTertiaryDark,
    onBackground = OnBackgroundDark,
    onSurface = OnSurfaceDark
)

val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    secondary = SecondaryLight,
    tertiary = TertiaryLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = OnPrimaryLight,
    onSecondary = OnSecondaryLight,
    onTertiary = OnTertiaryLight,
    onBackground = OnBackgroundLight,
    onSurface = OnSurfaceLight
)

/**
 * Platform-specific color scheme provider
 * On Android 12+, this can return dynamic colors based on wallpaper
 * On other platforms, returns the standard light/dark scheme
 *
 * @param darkTheme Whether to use dark theme
 * @param dynamicColor Whether to use platform dynamic colors (Android 12+ only)
 * @return The appropriate ColorScheme for the platform
 */
@Composable
expect fun getPlatformColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme

/**
 * Shared MediathekView theme composable
 * Uses platform-specific color scheme with shared typography
 *
 * @param darkTheme Whether to use dark theme (defaults to system setting)
 * @param dynamicColor Whether to use platform dynamic colors when available
 * @param content The content to display with this theme
 */
@Composable
fun MediathekViewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = getPlatformColorScheme(darkTheme, dynamicColor)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SharedTypography,
        content = content
    )
}

/**
 * Platform-specific check for system dark theme
 * Returns true if the system is in dark mode
 */
@Composable
expect fun isSystemInDarkTheme(): Boolean
