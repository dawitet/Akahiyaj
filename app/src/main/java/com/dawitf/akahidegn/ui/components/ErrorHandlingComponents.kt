package com.dawitf.akahidegn.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Enhanced Error Handling Components
 * Provides beautiful error states with recovery options and animations
 */

enum class ErrorType {
    NETWORK,
    PERMISSION,
    SERVER,
    VALIDATION,
    UNKNOWN
}

data class ErrorInfo(
    val type: ErrorType,
    val title: String,
    val message: String,
    val actionText: String? = null,
    val secondaryActionText: String? = null,
    val canRetry: Boolean = true
)

@Composable
fun ErrorScreen(
    errorInfo: ErrorInfo,
    onRetry: () -> Unit = {},
    onSecondaryAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated error icon
            AnimatedErrorIcon(
                errorType = errorInfo.type,
                modifier = Modifier.size(120.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = errorInfo.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = errorInfo.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Primary action button
            if (errorInfo.canRetry) {
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRetry()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorInfo.actionText ?: "እንደገና ሞክር",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Secondary action button
            errorInfo.secondaryActionText?.let { text ->
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSecondaryAction()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedErrorIcon(
    errorType: ErrorType,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "error_icon")
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_rotation"
    )
    
    val (icon, color) = when (errorType) {
        ErrorType.NETWORK -> Icons.Default.SignalWifiOff to MaterialTheme.colorScheme.error
        ErrorType.PERMISSION -> Icons.Default.Lock to MaterialTheme.colorScheme.error
        ErrorType.SERVER -> Icons.Default.Cloud to MaterialTheme.colorScheme.error
        ErrorType.VALIDATION -> Icons.Default.Error to MaterialTheme.colorScheme.error
        ErrorType.UNKNOWN -> Icons.AutoMirrored.Outlined.Help to MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .size(60.dp)
                    // Apply gentle rotation animation
                    .graphicsLayer {
                        rotationZ = rotationAngle
                    }
            )
        }
    }
}

@Composable
fun InlineErrorMessage(
    message: String,
    isVisible: Boolean,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "ዝጋ",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
    
    // Auto-dismiss after 5 seconds
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(5000)
            onDismiss()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorBottomSheet(
    errorInfo: ErrorInfo,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Error icon
                Card(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (errorInfo.type) {
                                ErrorType.NETWORK -> Icons.Default.SignalWifiOff
                                ErrorType.PERMISSION -> Icons.Default.Lock
                                ErrorType.SERVER -> Icons.Default.Cloud
                                ErrorType.VALIDATION -> Icons.Default.Error
                                ErrorType.UNKNOWN -> Icons.AutoMirrored.Outlined.Help
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = errorInfo.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = errorInfo.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ዝጋ")
                    }
                    
                    if (errorInfo.canRetry) {
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRetry()
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(errorInfo.actionText ?: "እንደገና ሞክር")
                        }
                    }
                }
                
                // Add extra space for gesture navigation
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Predefined Error States for Common Scenarios
 */

object CommonErrors {
    val NetworkError = ErrorInfo(
        type = ErrorType.NETWORK,
        title = "የኢንተርኔት ግንኙነት የለም",
        message = "እባክዎን የኢንተርኔት ግንኙነትዎን ያረጋግጡ እና እንደገና ይሞክሩ።",
        actionText = "እንደገና ሞክር",
        canRetry = true
    )
    
    val ServerError = ErrorInfo(
        type = ErrorType.SERVER,
        title = "የአገልግሎት ስህተት",
        message = "በአገልግሎታችን ላይ ጊዜያዊ ችግር አለ። እባክዎን ጠብቀው እንደገና ይሞክሩ።",
        actionText = "እንደገና ሞክር",
        secondaryActionText = "ድጋፍ አግኙ",
        canRetry = true
    )
    
    val PermissionError = ErrorInfo(
        type = ErrorType.PERMISSION,
        title = "ፈቃድ ያስፈልጋል",
        message = "ይህንን ባህሪ ለመጠቀም አስፈላጊ ፈቃዶች ይፈለጋሉ።",
        actionText = "ፈቃድ ይስጡ",
        canRetry = true
    )
    
    val ValidationError = ErrorInfo(
        type = ErrorType.VALIDATION,
        title = "የመረጃ ስህተት",
        message = "እባክዎን ትክክለኛ መረጃ ያስገቡ።",
        actionText = "ማረም",
        canRetry = true
    )
}
