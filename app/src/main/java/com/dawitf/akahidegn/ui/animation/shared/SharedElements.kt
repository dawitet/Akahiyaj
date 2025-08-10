@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.dawitf.akahidegn.ui.animation.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.ArcMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

// Shared transition constants
object SharedElementKeys {
    const val FROSTED_GLASS_CARD = "frosted_glass_card"
    const val TAB_HEADER = "tab_header"
    const val CONTENT_AREA = "content_area"
    const val NAVIGATION_BAR = "navigation_bar"
    const val PROFILE_BUTTON = "profile_button"
    const val HISTORY_BUTTON = "history_button"
    const val PROFILE_SCREEN = "profile_screen"
    const val HISTORY_SCREEN = "history_screen"
}

// CompositionLocal for sharing the SharedTransitionScope
private val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
private val LocalAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Root composable that provides SharedTransitionLayout
 */
@Composable
fun SharedElementsRoot(content: @Composable () -> Unit) {
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            content()
        }
    }
}

/**
 * Custom bounds transforms for different types of shared elements
 */
object SharedBoundsTransforms {
    /**
     * Default bounds transform with smooth motion
     */
    val Default = BoundsTransform { _, _ ->
        tween(durationMillis = 500, easing = FastOutSlowInEasing)
    }
    
    /**
     * Smooth bounds transform for text elements
     */
    val Text = BoundsTransform { _, _ ->
        tween(durationMillis = 600, easing = FastOutSlowInEasing)
    }
    
    /**
     * Fast bounds transform for quick transitions
     */
    val Fast = BoundsTransform { _, _ ->
        tween(durationMillis = 300, easing = FastOutSlowInEasing)
    }
    
    /**
     * Slow bounds transform for dramatic transitions
     */
    val Dramatic = BoundsTransform { _, _ ->
        tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    }
}

/**
 * Helper composable for shared elements
 */
@Composable
fun SharedElement(
    key: String,
    modifier: Modifier = Modifier,
    boundsTransform: BoundsTransform = SharedBoundsTransforms.Default,
    content: @Composable (Modifier) -> Unit
) {
    val sharedScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    
    if (sharedScope == null || animatedVisibilityScope == null) {
        // Fallback if not inside SharedElementsRoot or AnimatedVisibility
        content(modifier)
        return
    }
    
    with(sharedScope) {
        content(
            modifier.sharedElement(
                state = rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = boundsTransform
            )
        )
    }
}

/**
 * Helper composable for shared bounds (for containers)
 */
@Composable
fun SharedBounds(
    key: String,
    modifier: Modifier = Modifier,
    boundsTransform: BoundsTransform = SharedBoundsTransforms.Default,
    content: @Composable (Modifier) -> Unit
) {
    val sharedScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    
    if (sharedScope == null || animatedVisibilityScope == null) {
        // Fallback if not inside SharedElementsRoot or AnimatedVisibility
        content(modifier)
        return
    }
    
    with(sharedScope) {
        content(
            modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = boundsTransform
            )
        )
    }
}

/**
 * Wrapper for AnimatedVisibility that provides the scope
 */
@Composable
fun SharedAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: androidx.compose.animation.EnterTransition = fadeIn(),
    exit: androidx.compose.animation.ExitTransition = fadeOut(),
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit
    ) {
        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
            content()
        }
    }
}

/**
 * Modifier extension for rendering in shared transition overlay
 */
@Composable
fun Modifier.renderInSharedTransitionScopeOverlay(
    zIndexInOverlay: Float = 0f
): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    return if (sharedScope != null) {
        with(sharedScope) {
            this@renderInSharedTransitionScopeOverlay.renderInSharedTransitionScopeOverlay(
                zIndexInOverlay = zIndexInOverlay
            )
        }
    } else {
        this
    }
}
