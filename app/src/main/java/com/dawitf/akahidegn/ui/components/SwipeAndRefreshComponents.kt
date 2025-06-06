package com.dawitf.akahidegn.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.pow
import kotlinx.coroutines.delay

/**
 * Enhanced Pull-to-Refresh and Swipe Gesture Components
 * Modern refresh functionality with haptic feedback and smooth animations
 */

@Composable
fun EnhancedPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    refreshThreshold: Dp = 80.dp,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val refreshThresholdPx = with(density) { refreshThreshold.toPx() }
    
    var pullOffset by remember { mutableFloatStateOf(0f) }
    var isTriggered by remember { mutableStateOf(false) }
    
    val pullProgress = (pullOffset / refreshThresholdPx).coerceIn(0f, 1f)
    val refreshState = when {
        isRefreshing -> RefreshState.Refreshing
        pullProgress >= 1f -> RefreshState.ReadyToRefresh
        pullProgress > 0f -> RefreshState.Pulling
        else -> RefreshState.Idle
    }
    
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.Drag && available.y < 0 && pullOffset > 0) {
                    val consumed = pullOffset.coerceAtMost(-available.y)
                    pullOffset -= consumed
                    return Offset(0f, -consumed)
                }
                return Offset.Zero
            }
            
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.Drag && available.y > 0) {
                    val newOffset = (pullOffset + available.y).coerceAtMost(refreshThresholdPx * 1.5f)
                    
                    // Trigger haptic feedback when crossing threshold
                    if (pullOffset < refreshThresholdPx && newOffset >= refreshThresholdPx) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isTriggered = true
                    }
                    
                    pullOffset = newOffset
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
            
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffset > refreshThresholdPx && !isRefreshing) {
                    onRefresh()
                }
                
                // Animate back to 0
                if (pullOffset > 0) {
                    val targetOffset = if (isRefreshing) refreshThresholdPx * 0.6f else 0f
                    animate(
                        initialValue = pullOffset,
                        targetValue = targetOffset,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) { value, _ ->
                        pullOffset = value
                    }
                }
                
                isTriggered = false
                return super.onPreFling(available)
            }
        }
    }
    
    // Reset pull offset when refreshing stops
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && pullOffset > 0) {
            animate(
                initialValue = pullOffset,
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) { value, _ ->
                pullOffset = value
            }
        }
    }
    
    Box(modifier = modifier.nestedScroll(nestedScrollConnection)) {
        // Main content
        Box(
            modifier = Modifier.graphicsLayer {
                translationY = pullOffset
            }
        ) {
            content()
        }
        
        // Pull-to-refresh indicator
        PullToRefreshIndicator(
            refreshState = refreshState,
            pullProgress = pullProgress,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    translationY = pullOffset - refreshThresholdPx
                    alpha = pullProgress
                }
        )
    }
}

@Composable
private fun PullToRefreshIndicator(
    refreshState: RefreshState,
    pullProgress: Float,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = pullProgress * 180f,
        animationSpec = tween(durationMillis = 100),
        label = "indicator_rotation"
    )
    
    Card(
        modifier = modifier.size(56.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (refreshState) {
                RefreshState.Idle -> {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.rotate(rotation)
                    )
                }
                RefreshState.Pulling -> {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.rotate(rotation)
                    )
                }
                RefreshState.ReadyToRefresh -> {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                RefreshState.Refreshing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private enum class RefreshState {
    Idle, Pulling, ReadyToRefresh, Refreshing
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableCard(
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    leftAction: SwipeAction? = null,
    rightAction: SwipeAction? = null,
    swipeThreshold: Float = 0.3f,
    content: @Composable BoxScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isSwipeTriggered by remember { mutableStateOf(false) }
    
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "swipe_offset"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    offsetX = (offsetX + delta).coerceIn(-200f, 200f)
                    
                    // Trigger haptic feedback when crossing threshold
                    val threshold = 100f
                    if (!isSwipeTriggered && abs(offsetX) > threshold) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isSwipeTriggered = true
                    } else if (isSwipeTriggered && abs(offsetX) < threshold) {
                        isSwipeTriggered = false
                    }
                },
                onDragStopped = { velocity ->
                    val threshold = 100f
                    
                    when {
                        offsetX > threshold && onSwipeRight != null -> {
                            onSwipeRight()
                        }
                        offsetX < -threshold && onSwipeLeft != null -> {
                            onSwipeLeft()
                        }
                    }
                    
                    // Reset position
                    offsetX = 0f
                    isSwipeTriggered = false
                }
            )
    ) {
        // Background actions
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left action
            leftAction?.let { action ->
                SwipeActionButton(
                    action = action,
                    isVisible = animatedOffsetX > 50f,
                    modifier = Modifier.alpha(
                        (animatedOffsetX / 100f).coerceIn(0f, 1f)
                    )
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Right action
            rightAction?.let { action ->
                SwipeActionButton(
                    action = action,
                    isVisible = animatedOffsetX < -50f,
                    modifier = Modifier.alpha(
                        (-animatedOffsetX / 100f).coerceIn(0f, 1f)
                    )
                )
            }
        }
        
        // Main content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = animatedOffsetX
                },
            content = content
        )
    }
}

@Composable
private fun SwipeActionButton(
    action: SwipeAction,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically() + androidx.compose.animation.fadeIn(),
        exit = slideOutVertically() + androidx.compose.animation.fadeOut(),
        modifier = modifier
    ) {
        Card(
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = action.backgroundColor
            ),
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.label,
                    tint = action.iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

data class SwipeAction(
    val icon: ImageVector,
    val label: String,
    val backgroundColor: Color,
    val iconColor: Color = Color.White
)

@Composable
fun QuickActionsMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    actions: List<QuickAction>,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + androidx.compose.animation.fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + androidx.compose.animation.fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                actions.forEachIndexed { index, action ->
                    QuickActionItem(
                        action = action,
                        onClick = {
                            action.onClick()
                            onDismiss()
                        },
                        animationDelay = index * 50
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    action: QuickAction,
    onClick: () -> Unit,
    animationDelay: Int = 0,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }
    
    SlideInCard(
        visible = isVisible,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = action.iconColor,
                modifier = Modifier.size(24.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                action.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (action.showArrow) {
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

data class QuickAction(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val iconColor: Color = Color.Unspecified,
    val showArrow: Boolean = true,
    val onClick: () -> Unit
)
