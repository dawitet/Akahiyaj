package com.dawitf.akahidegn.ui.animation.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

// Official SharedTransition wrappers keeping the same public API as before.

private val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedElementsRoot(content: @Composable () -> Unit) {
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            content()
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedElement(
    key: Any,
    screenKey: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit
) {
    val sharedScope = LocalSharedTransitionScope.current
    if (sharedScope == null) {
        // Fallback if not inside SharedElementsRoot
        content(modifier)
        return
    }

    // For now, just render content without shared element animation
    // until rememberSharedContentState is available in stable Compose
    content(modifier)
}
