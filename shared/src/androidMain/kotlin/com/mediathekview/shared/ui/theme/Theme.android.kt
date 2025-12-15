package com.mediathekview.shared.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme as androidIsSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation of platform color scheme
 * Supports Material You dynamic colors on Android 12+ (API 31+)
 */
@Composable
actual fun getPlatformColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme {
    return when {
        // Use dynamic colors on Android 12+ if enabled
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        // Fall back to standard color schemes
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
}

/**
 * Android implementation of system dark theme check
 * Uses the standard AndroidX Compose function
 */
@Composable
actual fun isSystemInDarkTheme(): Boolean = androidIsSystemInDarkTheme()
