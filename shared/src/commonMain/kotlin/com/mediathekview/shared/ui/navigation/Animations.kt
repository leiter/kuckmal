package com.mediathekview.shared.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/**
 * Animation specifications for navigation transitions
 * KMP-compatible - uses Compose Multiplatform animation APIs
 */
object NavigationAnimations {
    const val ANIMATION_DURATION = 300

    // Forward navigation (going to detail)
    val slideInFromRight: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(ANIMATION_DURATION)
    ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))

    val slideOutToLeft: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth / 3 },
        animationSpec = tween(ANIMATION_DURATION)
    ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))

    // Backward navigation (returning from detail)
    val slideInFromLeft: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth / 3 },
        animationSpec = tween(ANIMATION_DURATION)
    ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))

    val slideOutToRight: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(ANIMATION_DURATION)
    ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))

    // Simple fade transitions
    val fadeInTransition: EnterTransition = fadeIn(animationSpec = tween(ANIMATION_DURATION))
    val fadeOutTransition: ExitTransition = fadeOut(animationSpec = tween(ANIMATION_DURATION))
}
