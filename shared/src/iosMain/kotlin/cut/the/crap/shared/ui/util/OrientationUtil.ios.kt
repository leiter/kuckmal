@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package cut.the.crap.shared.ui.util

import androidx.compose.runtime.*
import kotlinx.cinterop.useContents
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIDeviceOrientationDidChangeNotification
import platform.UIKit.UIScreen

/**
 * iOS implementation of orientation detection.
 * Uses NSNotificationCenter to observe device orientation changes
 * and reactively update the orientation state.
 */
@Composable
actual fun rememberOrientation(): Orientation {
    var orientation by remember { mutableStateOf(getOrientationFromDevice() ?: getOrientationFromBounds()) }

    DisposableEffect(Unit) {
        // Enable device orientation notifications
        UIDevice.currentDevice.beginGeneratingDeviceOrientationNotifications()

        // Observe orientation changes
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIDeviceOrientationDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            // Use device orientation directly - it's already updated when notification fires
            getOrientationFromDevice()?.let { newOrientation ->
                orientation = newOrientation
            }
        }

        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
            UIDevice.currentDevice.endGeneratingDeviceOrientationNotifications()
        }
    }

    return orientation
}

/**
 * Get orientation from device orientation.
 * Returns null for face up/down/unknown orientations.
 */
private fun getOrientationFromDevice(): Orientation? {
    return when (UIDevice.currentDevice.orientation) {
        UIDeviceOrientation.UIDeviceOrientationPortrait,
        UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown -> Orientation.Portrait
        UIDeviceOrientation.UIDeviceOrientationLandscapeLeft,
        UIDeviceOrientation.UIDeviceOrientationLandscapeRight -> Orientation.Landscape
        else -> null // Face up, face down, or unknown - ignore these
    }
}

/**
 * Get orientation from screen bounds (fallback for initial state).
 */
private fun getOrientationFromBounds(): Orientation {
    val bounds = UIScreen.mainScreen.bounds
    val width = bounds.useContents { size.width }
    val height = bounds.useContents { size.height }
    return if (width > height) Orientation.Landscape else Orientation.Portrait
}
