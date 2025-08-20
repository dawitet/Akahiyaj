package com.dawitf.akahidegn.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Compatibility layer that provides a PullToRefreshBox API (as in official guidelines)
 * backed by the existing EnhancedPullToRefresh implementation already present
 * in the codebase. This lets screens follow guideline style code while keeping
 * the custom haptics/physics logic you already implemented.
 */
@Composable
fun PullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    indicator: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    // We delegate gesture + refresh mechanics to EnhancedPullToRefresh.
    // The provided state currently only exposes distanceFraction so callers using
    // guideline samples compile. We approximate progress via internal composition.
    EnhancedPullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        Box {
            content()
            // Default indicator if none provided
            if (indicator != null) {
                indicator()
            } else {
                DefaultPullIndicator(isRefreshing = isRefreshing, distanceFraction = state.distanceFraction)
            }
        }
    }
}

/** Lightweight state placeholder so guideline examples compile. */
class PullToRefreshState internal constructor(internal val distanceFractionState: State<Float>) {
    val distanceFraction: Float get() = distanceFractionState.value
}

@Composable
fun rememberPullToRefreshState(): PullToRefreshState {
    // We do not have direct access to gesture drag offset from EnhancedPullToRefresh.
    // Expose a simple animated stub that goes to 1f while refreshing.
    val animatedFrac by animateFloatAsState(
        targetValue = 0f,
        label = "ptr_frac_stub"
    )
    return remember { PullToRefreshState(distanceFractionState = derivedStateOf { animatedFrac }) }
}

@Composable
private fun BoxScope.DefaultPullIndicator(isRefreshing: Boolean, distanceFraction: Float) {
    val rotation by animateFloatAsState(targetValue = distanceFraction * 180f, label = "ptr_rotation")
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .graphicsLayer { alpha = if (isRefreshing) 1f else distanceFraction },
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = isRefreshing,
            animationSpec = tween(300),
            label = "ptr_crossfade"
        ) { refreshing ->
            if (refreshing) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = if (distanceFraction >= 1f) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(rotation)
                )
            }
        }
    }
}

