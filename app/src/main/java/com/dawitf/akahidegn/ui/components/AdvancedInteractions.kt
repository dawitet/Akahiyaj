package com.dawitf.akahidegn.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Advanced Interaction Components
 * Provides swipe gestures, long-press menus, and micro-interactions
 */

data class SwipeAction(
    val icon: ImageVector,
    val label: String,
    val backgroundColor: Color,
    val iconColor: Color = Color.White
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableGroupCard(
    content: @Composable () -> Unit,
    leftAction: SwipeAction? = null,
    rightAction: SwipeAction? = null,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    var offsetX by remember { mutableStateOf(0f) }
    var showLeftAction by remember { mutableStateOf(false) }
    var showRightAction by remember { mutableStateOf(false) }
    
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "swipe_offset"
    )
    
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Background actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left action
            if (leftAction != null && showLeftAction) {
                SwipeActionButton(
                    action = leftAction,
                    modifier = Modifier
                        .width(80.dp)
                        .fillMaxHeight()
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Right action
            if (rightAction != null && showRightAction) {
                SwipeActionButton(
                    action = rightAction,
                    modifier = Modifier
                        .width(80.dp)
                        .fillMaxHeight()
                )
            }
        }
        
        // Main content
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            when {
                                offsetX > 80 && leftAction != null && onSwipeLeft != null -> {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSwipeLeft()
                                }
                                offsetX < -80 && rightAction != null && onSwipeRight != null -> {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSwipeRight()
                                }
                            }
                            offsetX = 0f
                            showLeftAction = false
                            showRightAction = false
                        }
                    ) { change, _ ->
                        offsetX += change.position.x
                        showLeftAction = offsetX > 20 && leftAction != null
                        showRightAction = offsetX < -20 && rightAction != null
                        
                        if ((showLeftAction || showRightAction) && 
                            kotlin.math.abs(change.position.x) > 10) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                }
                .combinedClickable(
                    onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress?.invoke()
                    },
                    onClick = { /* Handled by content */ }
                )
        ) {
            content()
        }
    }
}

@Composable
private fun SwipeActionButton(
    action: SwipeAction,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = action.backgroundColor.copy(alpha = 0.9f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = action.iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelSmall,
                color = action.iconColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LongPressContextMenu(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    options: List<ContextMenuOption>,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .width(200.dp)
                .glassCard(alpha = 0.2f),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                options.forEach { option ->
                    ContextMenuItem(
                        option = option,
                        onClick = {
                            option.action()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

data class ContextMenuOption(
    val icon: ImageVector,
    val label: String,
    val action: () -> Unit,
    val isDestructive: Boolean = false
)

@Composable
private fun ContextMenuItem(
    option: ContextMenuOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    Surface(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = option.label,
                tint = if (option.isDestructive) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            
            Text(
                text = option.label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (option.isDestructive) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Bouncy button with micro-interactions
 */
@Composable
fun BouncyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "button_scale"
    )
    
    Button(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .scale(scale)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isPressed = true },
                    onDragEnd = { isPressed = false }
                ) { _, _ -> }
            },
        enabled = enabled,
        colors = colors,
        content = content
    )
}
