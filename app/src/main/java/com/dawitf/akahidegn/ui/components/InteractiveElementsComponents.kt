package com.dawitf.akahidegn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

/**
 * Interactive Elements Components
 * Long-press context menus, enhanced FABs, and interactive widgets
 */

data class ContextMenuItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val color: Color = Color.Unspecified,
    val onClick: () -> Unit
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LongPressContextMenu(
    items: List<ContextMenuItem>,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    
    Box(modifier = modifier) {
        Box(
            modifier = Modifier.combinedClickable(
                onClick = { /* Regular click handled by parent */ },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMenu = true
                }
            )
        ) {
            content()
        }
        
        if (showMenu) {
            ContextMenuDialog(
                items = items,
                onDismiss = {
                    showMenu = false
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun ContextMenuDialog(
    items: List<ContextMenuItem>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        SlideInCard(
            visible = true,
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                    )
                    
                    items.forEachIndexed { index, item ->
                        ContextMenuItemComponent(
                            item = item,
                            onDismiss = onDismiss,
                            animationDelay = index * 50
                        )
                        
                        if (index < items.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContextMenuItemComponent(
    item: ContextMenuItem,
    onDismiss: () -> Unit,
    animationDelay: Int = 0
) {
    val haptic = LocalHapticFeedback.current
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }
    
    SlideInCard(
        visible = isVisible,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        item.onClick()
                        onDismiss()
                    }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = if (item.color != Color.Unspecified) item.color else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                item.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Add,
    text: String? = null,
    expanded: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val haptic = LocalHapticFeedback.current
    
    val fabScale by animateFloatAsState(
        targetValue = if (expanded) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fab_scale"
    )
    
    val fabColor by animateColorAsState(
        targetValue = containerColor,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fab_color"
    )
    
    FloatingActionButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.scale(fabScale),
        containerColor = fabColor,
        contentColor = contentColor,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = if (expanded) 12.dp else 6.dp,
            hoveredElevation = 16.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (text != null) 16.dp else 0.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text ?: "Action",
                modifier = Modifier.size(24.dp)
            )
            
            text?.let {
                androidx.compose.animation.AnimatedVisibility(
                    visible = expanded,
                    enter = androidx.compose.animation.slideInHorizontally() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutHorizontally() + androidx.compose.animation.fadeOut()
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiFAB(
    mainIcon: ImageVector = Icons.Filled.Add,
    isExpanded: Boolean,
    onMainClick: () -> Unit,
    onToggle: () -> Unit,
    actions: List<FABAction>,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Box(modifier = modifier) {
        // Background overlay when expanded
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .combinedClickable(onClick = onToggle)
                    .zIndex(1f)
            )
        }
        
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.zIndex(2f)
        ) {
            // Sub-actions
            actions.forEachIndexed { index, action ->
                var isItemVisible by remember { mutableStateOf(false) }
                
                LaunchedEffect(isExpanded) {
                    if (isExpanded) {
                        delay((index * 50).toLong())
                        isItemVisible = true
                    } else {
                        isItemVisible = false
                    }
                }
                
                androidx.compose.animation.AnimatedVisibility(
                    visible = isExpanded && isItemVisible,
                    enter = androidx.compose.animation.slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically(
                        targetOffsetY = { it / 2 }
                    ) + androidx.compose.animation.fadeOut()
                ) {
                    FABActionItem(
                        action = action,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            action.onClick()
                            onToggle()
                        }
                    )
                }
            }
            
            // Main FAB
            FloatingActionButtonAnimated(
                onClick = if (actions.isEmpty()) onMainClick else onToggle,
                icon = if (isExpanded) Icons.Filled.Close else mainIcon,
                contentDescription = if (isExpanded) "Close menu" else "Open menu"
            )
        }
    }
}

@Composable
private fun FABActionItem(
    action: FABAction,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Label
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = action.label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        
        // Mini FAB
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
            containerColor = action.backgroundColor,
            contentColor = action.contentColor
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

data class FABAction(
    val icon: ImageVector,
    val label: String,
    val backgroundColor: Color = Color.Unspecified,
    val contentColor: Color = Color.Unspecified,
    val onClick: () -> Unit
)

@Composable
fun SwipeToActionCard(
    onSwipeToDelete: (() -> Unit)? = null,
    onSwipeToJoin: (() -> Unit)? = null,
    onSwipeToBookmark: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val actions = mutableListOf<SwipeAction>()
    
    onSwipeToDelete?.let {
        actions.add(
            SwipeAction(
                icon = Icons.Filled.Delete,
                label = "Delete",
                backgroundColor = Color.Red,
                iconColor = Color.White
            )
        )
    }
    
    onSwipeToJoin?.let {
        actions.add(
            SwipeAction(
                icon = Icons.Filled.Add,
                label = "Join",
                backgroundColor = Color.Green,
                iconColor = Color.White
            )
        )
    }
    
    onSwipeToBookmark?.let {
        actions.add(
            SwipeAction(
                icon = Icons.Filled.BookmarkAdd,
                label = "Bookmark",
                backgroundColor = Color.Blue,
                iconColor = Color.White
            )
        )
    }
    
    SwipeableCard(
        onSwipeLeft = onSwipeToDelete,
        onSwipeRight = onSwipeToJoin,
        leftAction = actions.getOrNull(0),
        rightAction = actions.getOrNull(1),
        modifier = modifier,
        content = content
    )
}

@Composable
fun InteractiveStatusCard(
    title: String,
    status: String,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    contextMenuItems: List<ContextMenuItem> = emptyList()
) {
    val haptic = LocalHapticFeedback.current
    var showContextMenu by remember { mutableStateOf(false) }
    
    val cardColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_color"
    )
    
    LongPressContextMenu(
        items = contextMenuItems,
        onDismiss = { showContextMenu = false },
        modifier = modifier
    ) {
        AnimatedPressableCard(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    
                    LiveActivityIndicator(
                        isActive = isActive,
                        activityText = if (isActive) "LIVE" else "INACTIVE"
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarkButton(
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val haptic = LocalHapticFeedback.current
    
    val iconColor by animateColorAsState(
        targetValue = if (isBookmarked) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "bookmark_color"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isBookmarked) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bookmark_scale"
    )
    
    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggleBookmark()
        },
        modifier = modifier.scale(scale)
    ) {
        Icon(
            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
            tint = iconColor,
            modifier = Modifier.size(size)
        )
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val haptic = LocalHapticFeedback.current
    
    BouncyButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        icon = icon,
        text = text,
        containerColor = backgroundColor,
        contentColor = contentColor,
        modifier = modifier
    )
}
