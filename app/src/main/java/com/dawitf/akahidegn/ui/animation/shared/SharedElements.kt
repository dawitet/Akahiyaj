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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// Shared transition constants with glassmorphism and advanced transitions
object SharedElementKeys {
    const val FROSTED_GLASS_CARD = "frosted_glass_card"
    const val TAB_HEADER = "tab_header"
    const val CONTENT_AREA = "content_area"
    const val NAVIGATION_BAR = "navigation_bar"
    const val PROFILE_BUTTON = "profile_button"
    const val HISTORY_BUTTON = "history_button"
    const val CREATE_BUTTON = "create_button"
    const val PROFILE_SCREEN = "profile_screen"
    const val HISTORY_SCREEN = "history_screen"
    const val GROUP_CARD = "group_card"
    const val GROUP_DETAIL = "group_detail"
    const val SETTINGS_BACKGROUND = "settings_background"
    const val GLASSMORPHIC_OVERLAY = "glassmorphic_overlay"
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
 * Transform types for shared elements
 */
enum class TransformType {
    DEFAULT, ELEGANT_ARC, GLASS_MORPH, SNAPPY, LUXURIOUS, DRAMATIC
}

/**
 * Advanced bounds transforms inspired by modern UI patterns
 */
object SharedBoundsTransforms {
    /**
     * Default smooth transform with spring physics
     */
    val Default = BoundsTransform { _, _ ->
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }
    
    /**
     * Elegant arc-based transform for card/screen transitions
     */
    val ElegantArc = BoundsTransform { initialBounds, targetBounds ->
        tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        )
    }
    
    /**
     * Glass morphing transform for glassmorphism effects
     */
    val GlassMorph = BoundsTransform { _, _ ->
        tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        )
    }
    
    /**
     * Fast snappy transform for button interactions
     */
    val Snappy = BoundsTransform { _, _ ->
        spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh
        )
    }
    
    /**
     * Smooth text transform with extended duration
     */
    val Text = BoundsTransform { _, _ ->
        tween(durationMillis = 700, easing = FastOutSlowInEasing)
    }
    
    /**
     * Luxurious transform for premium UI elements
     */
    val Luxurious = BoundsTransform { initialBounds, targetBounds ->
        tween(
            durationMillis = 1200,
            easing = FastOutSlowInEasing
        )
    }
    
    /**
     * Dramatic transform for impressive transitions
     */
    val Dramatic = BoundsTransform { _, _ ->
        tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    }
    
    /**
     * Get transform by type
     */
    fun getTransform(type: TransformType): BoundsTransform = when(type) {
        TransformType.DEFAULT -> Default
        TransformType.ELEGANT_ARC -> ElegantArc
        TransformType.GLASS_MORPH -> GlassMorph
        TransformType.SNAPPY -> Snappy
        TransformType.LUXURIOUS -> Luxurious
        TransformType.DRAMATIC -> Dramatic
    }
}

/**
 * Glassmorphism effect composable with shared element support
 */
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    blur: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.25f),
            Color.White.copy(alpha = 0.1f)
        )
    )
    
    Box(
        modifier = modifier.clip(shape)
    ) {
        // Background blur simulation
        if (blur) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.White.copy(alpha = 0.1f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        )
                    )
            )
        }
        
        // Glass surface
        Card(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.5f),
                            Color.White.copy(alpha = 0.1f),
                            Color.White.copy(alpha = 0.8f)
                        )
                    ),
                    shape = shape
                ),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 12.dp
            )
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(24.dp),
                content = content
            )
        }
    }
}

// Public wrappers (SharedElement/SharedBounds) intentionally hide experimental types from call sites.

/**
 * Helper composable for shared elements
 */
@Composable
private fun InternalSharedElement(
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
 * Enhanced SharedElement with preset transform options
 */
@Composable
fun SharedElement(
    key: String,
    modifier: Modifier = Modifier,
    transform: TransformType = TransformType.DEFAULT,
    content: @Composable (Modifier) -> Unit
) {
    InternalSharedElement(
        key = key, 
        modifier = modifier, 
        boundsTransform = SharedBoundsTransforms.getTransform(transform), 
        content = content
    )
}

/**
 * Enhanced SharedBounds with preset transform options
 */
@Composable
fun SharedBounds(
    key: String,
    modifier: Modifier = Modifier,
    transform: TransformType = TransformType.ELEGANT_ARC,
    content: @Composable (Modifier) -> Unit
) {
    InternalSharedBounds(
        key = key, 
        modifier = modifier, 
        boundsTransform = SharedBoundsTransforms.getTransform(transform), 
        content = content
    )
}

/**
 * Helper composable for shared bounds (for containers)
 */
@Composable
private fun InternalSharedBounds(
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
 * Enhanced SharedAnimatedVisibility with sophisticated enter/exit animations
 */
@Composable
fun SharedAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    animationType: AnimationType = AnimationType.FadeSlide,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val sharedScope = LocalSharedTransitionScope.current
    
    if (sharedScope == null) {
        // Fallback without shared transitions
        AnimatedVisibility(
            visible = visible,
            modifier = modifier,
            enter = animationType.enterTransition,
            exit = animationType.exitTransition,
            content = content
        )
        return
    }
    
    with(sharedScope) {
        AnimatedVisibility(
            visible = visible,
            modifier = modifier,
            enter = animationType.enterTransition,
            exit = animationType.exitTransition
        ) {
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                content()
            }
        }
    }
}

/**
 * Animation types for different UI scenarios
 */
enum class AnimationType(
    val enterTransition: androidx.compose.animation.EnterTransition,
    val exitTransition: androidx.compose.animation.ExitTransition
) {
    // Elegant fade with slide for general use
    FadeSlide(
        enterTransition = fadeIn(tween(600)) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        exitTransition = fadeOut(tween(400)) + slideOutVertically(
            targetOffsetY = { -it / 3 },
            animationSpec = tween(400)
        )
    ),
    
    // Scale with fade for buttons and cards
    ScaleFade(
        enterTransition = scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(tween(500)),
        exitTransition = scaleOut(
            targetScale = 0.9f,
            animationSpec = tween(300)
        ) + fadeOut(tween(300))
    ),
    
    // Gentle fade for subtle elements
    Fade(
        enterTransition = fadeIn(tween(800)),
        exitTransition = fadeOut(tween(600))
    ),
    
    // Dramatic slide for major screen changes
    DramaticSlide(
        enterTransition = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(tween(800)),
        exitTransition = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(600)
        ) + fadeOut(tween(400))
    )
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
