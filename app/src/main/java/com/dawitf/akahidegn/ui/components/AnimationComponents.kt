package com.dawitf.akahidegn.ui.components

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animation configuration data classes for better maintainability
 */

/**
 * Configuration class for animation parameters
 *
 * @property duration Animation duration in milliseconds
 * @property delay Animation delay in milliseconds
 * @property easing Animation easing function
 * @property dampingRatio Spring animation damping ratio (0.1f to 1.0f)
 * @property stiffness Spring animation stiffness
 */
data class AnimationConfig(
    val duration: Int = 800,
    val delay: Int = 200,
    val easing: Easing = FastOutSlowInEasing,
    val dampingRatio: Float = Spring.DampingRatioMediumBouncy,
    val stiffness: Float = Spring.StiffnessMedium
) {
    init {
        require(duration > 0) { "Duration must be positive" }
        require(delay >= 0) { "Delay cannot be negative" }
        require(dampingRatio in 0.1f..1.0f) { "Damping ratio must be between 0.1 and 1.0" }
    }
}

/**
 * Animation speed configuration
 *
 * @property multiplier Speed multiplier for animations (lower = faster)
 */
data class AnimationSpeed(
    val multiplier: Float
) {
    init {
        require(multiplier > 0) { "Speed multiplier must be positive" }
    }

    companion object {
        /** Slow animation speed (1.5x duration) */
        val Slow = AnimationSpeed(1.5f)
        /** Normal animation speed (1.0x duration) */
        val Normal = AnimationSpeed(1.0f)
        /** Fast animation speed (0.7x duration) */
        val Fast = AnimationSpeed(0.7f)
    }
}

/**
 * Available animation types for notifications
 */
enum class AnimationType {
    /** Success state with green colors and checkmark */
    SUCCESS,
    /** Error state with red colors and error icon */
    ERROR,
    /** Warning state with orange colors and warning icon */
    WARNING,
    /** Loading state with spinning indicator */
    LOADING,
    /** Fade animation */
    FADE
}

/**
 * Animation slide directions
 */
enum class SlideDirection {
    /** Slide from top */
    TOP,
    /** Slide from bottom */
    BOTTOM,
    /** Slide from left */
    LEFT,
    /** Slide from right */
    RIGHT
}

/**
 * Size variants for animation components
 */
enum class AnimationSize {
    /** Small size (48dp) */
    Small,
    /** Medium size (64dp) */
    Medium,
    /** Large size (96dp) */
    Large;

    val dp: androidx.compose.ui.unit.Dp
        get() = when (this) {
            Small -> 48.dp
            Medium -> 64.dp
            Large -> 96.dp
        }
}

/**
 * Enhanced Success Animation Components
 * Creates beautiful animated success indicators with smooth transitions
 * Now with accessibility support and performance optimizations
 */

/**
 * A comprehensive success animation card with customizable behavior
 *
 * @param isVisible Whether the animation should be visible
 * @param title Main title text to display
 * @param subtitle Optional subtitle text
 * @param onDismiss Callback when the card is dismissed
 * @param modifier Modifier for styling
 * @param animationConfig Configuration for animation parameters
 * @param animationSpeed Speed configuration for animations
 * @param autoDismissDelay Delay before auto-dismissing (milliseconds)
 * @param enableSwipeToDismiss Whether to enable swipe-to-dismiss gesture
 * @param enableSoundEffects Whether to enable sound effects (placeholder)
 * @param slideDirection Direction for slide animation
 * @param size Size variant for the animation
 * @param customIcon Optional custom icon to display instead of checkmark
 * @param shape Shape configuration for the card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuccessAnimationCard(
    isVisible: Boolean,
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
    animationConfig: AnimationConfig = AnimationConfig(),
    animationSpeed: AnimationSpeed = AnimationSpeed.Normal,
    autoDismissDelay: Long = 3000L,
    enableSwipeToDismiss: Boolean = true,
    enableSoundEffects: Boolean = false,
    slideDirection: SlideDirection = SlideDirection.TOP,
    size: AnimationSize = AnimationSize.Medium,
    customIcon: (@Composable () -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp)
) {
    val haptic = LocalHapticFeedback.current
    val accessibilityManager = LocalAccessibilityManager.current

    // Error handling for invalid parameters
    if (autoDismissDelay < 0) {
        LaunchedEffect(Unit) {
            throw IllegalArgumentException("Auto-dismiss delay cannot be negative")
        }
        return
    }

    // Performance optimization: Use derivedStateOf for expensive calculations
    val adjustedDuration by remember {
        derivedStateOf {
            try {
                (animationConfig.duration / animationSpeed.multiplier).toInt()
            } catch (e: Exception) {
                800 // fallback duration
            }
        }
    }

    // Accessibility: Check if reduced motion is preferred
    val isReducedMotionEnabled = false // Simplified for now - can be enhanced later

    val slideEnterAnimation = remember(slideDirection, isReducedMotionEnabled) {
        try {
            when (slideDirection) {
                SlideDirection.TOP -> slideInVertically(
                    initialOffsetY = { if (isReducedMotionEnabled) 0 else -it },
                    animationSpec = if (isReducedMotionEnabled) tween(100) else spring(
                        dampingRatio = animationConfig.dampingRatio,
                        stiffness = animationConfig.stiffness
                    )
                )
                SlideDirection.BOTTOM -> slideInVertically(
                    initialOffsetY = { if (isReducedMotionEnabled) 0 else it },
                    animationSpec = if (isReducedMotionEnabled) tween(100) else spring(
                        dampingRatio = animationConfig.dampingRatio,
                        stiffness = animationConfig.stiffness
                    )
                )
                SlideDirection.LEFT -> slideInHorizontally(
                    initialOffsetX = { if (isReducedMotionEnabled) 0 else -it },
                    animationSpec = if (isReducedMotionEnabled) tween(100) else spring(
                        dampingRatio = animationConfig.dampingRatio,
                        stiffness = animationConfig.stiffness
                    )
                )
                SlideDirection.RIGHT -> slideInHorizontally(
                    initialOffsetX = { if (isReducedMotionEnabled) 0 else it },
                    animationSpec = if (isReducedMotionEnabled) tween(100) else spring(
                        dampingRatio = animationConfig.dampingRatio,
                        stiffness = animationConfig.stiffness
                    )
                )
            }
        } catch (e: Exception) {
            // Fallback to simple fade in
            fadeIn()
        }
    }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var isDismissed by remember { mutableStateOf(false) }

    // Undo functionality state
    var showUndoOption by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isVisible && !isDismissed,
        enter = slideEnterAnimation + fadeIn(),
        exit = slideOutVertically(
            animationSpec = tween(300)
        ) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .offset(x = offsetX.dp)
                .then(
                    if (enableSwipeToDismiss) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    try {
                                        if (kotlin.math.abs(offsetX) > 100) {
                                            showUndoOption = true
                                            isDismissed = true
                                        } else {
                                            offsetX = 0f
                                        }
                                    } catch (e: Exception) {
                                        // Reset on error
                                        offsetX = 0f
                                    }
                                }
                            ) { _, dragAmount ->
                                offsetX += dragAmount.x
                            }
                        }
                    } else Modifier
                )
                .semantics {
                    contentDescription = "Success notification: $title"
                    role = Role.Button
                    if (enableSwipeToDismiss) {
                        customActions = listOf(
                            CustomAccessibilityAction("Dismiss") {
                                onDismiss()
                                true
                            }
                        )
                    }
                },
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Custom icon or animated checkmark
                if (customIcon != null) {
                    Box(
                        modifier = Modifier.size(size.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        customIcon()
                    }
                } else {
                    AnimatedCheckmark(
                        isVisible = isVisible,
                        size = size.dp,
                        color = MaterialTheme.colorScheme.primary,
                        animationConfig = animationConfig,
                        animationSpeed = animationSpeed
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics {
                        contentDescription = "Success title: $title"
                    }
                )
                
                subtitle?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics {
                            contentDescription = "Success subtitle: $it"
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                FilledTonalButton(
                    onClick = {
                        try {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss()
                        } catch (e: Exception) {
                            // Fallback without haptic feedback
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Dismiss success notification"
                        }
                ) {
                    Text("እሺ")
                }
            }
        }
    }
    
    // Undo functionality
    if (showUndoOption) {
        UndoSnackbar(
            message = "Notification dismissed",
            onUndo = {
                showUndoOption = false
                isDismissed = false
                offsetX = 0f
            },
            onDismiss = {
                showUndoOption = false
                onDismiss()
            }
        )
    }

    // Auto-dismiss with configurable delay
    LaunchedEffect(isVisible) {
        if (isVisible && !isDismissed) {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(autoDismissDelay)
                if (!isDismissed) {
                    onDismiss()
                }
            } catch (e: Exception) {
                // Fallback without haptic feedback
                delay(autoDismissDelay)
                if (!isDismissed) {
                    onDismiss()
                }
            }
        }
    }
}

/**
 * Undo functionality snackbar
 *
 * @param message Message to display
 * @param onUndo Callback when undo is pressed
 * @param onDismiss Callback when dismissed
 */
@Composable
fun UndoSnackbar(
    message: String,
    onUndo: () -> Unit,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3000) // Auto-dismiss after 3 seconds
        isVisible = false
        onDismiss()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.inverseSurface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.weight(1f)
                )

                TextButton(
                    onClick = {
                        isVisible = false
                        onUndo()
                    }
                ) {
                    Text(
                        "UNDO",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Enhanced animated checkmark with comprehensive documentation
 *
 * @param isVisible Whether the checkmark animation should be visible
 * @param size Size of the checkmark indicator
 * @param color Color of the checkmark and circle
 * @param strokeWidth Width of the stroke for drawing
 * @param animationConfig Configuration for animation parameters
 * @param animationSpeed Speed configuration for animations
 */
@Composable
fun AnimatedCheckmark(
    isVisible: Boolean,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Float = 8f,
    animationConfig: AnimationConfig = AnimationConfig(),
    animationSpeed: AnimationSpeed = AnimationSpeed.Normal
) {
    val adjustedDuration = remember {
        try {
            (animationConfig.duration / animationSpeed.multiplier).toInt()
        } catch (e: Exception) {
            800 // fallback duration
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = adjustedDuration,
            delayMillis = animationConfig.delay,
            easing = animationConfig.easing
        ),
        label = "checkmark_progress"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = animationConfig.dampingRatio,
            stiffness = animationConfig.stiffness
        ),
        label = "checkmark_scale"
    )
    
    // Performance optimization: Remember Path object to avoid recreation
    val checkmarkPath = remember { Path() }

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .semantics {
                contentDescription = "Animated checkmark indicator"
                role = Role.Image
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(size)
        ) {
            try {
                val canvasSize = size.toPx()
                val strokeWidthPx = strokeWidth

                // Draw circle background
                drawCircle(
                    color = color.copy(alpha = 0.2f),
                    radius = canvasSize / 2 - strokeWidthPx / 2,
                    style = Stroke(strokeWidthPx)
                )

                // Draw animated circle progress
                val sweepAngle = 360f * progress
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                )

                // Draw checkmark when circle is complete
                if (progress > 0.7f) {
                    val checkProgress = (progress - 0.7f) / 0.3f
                    drawCheckmark(
                        color = color,
                        progress = checkProgress,
                        strokeWidth = strokeWidthPx * 0.8f,
                        path = checkmarkPath
                    )
                }
            } catch (e: Exception) {
                // Fallback: Draw simple circle
                drawCircle(
                    color = color,
                    radius = size.toPx() / 2 - strokeWidth / 2,
                    style = Stroke(strokeWidth)
                )
            }
        }
    }
}

/**
 * Error Animation Card - New addition
 */
@Composable
fun ErrorAnimationCard(
    isVisible: Boolean,
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit = {},
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    animationConfig: AnimationConfig = AnimationConfig()
) {
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = animationConfig.dampingRatio,
                stiffness = animationConfig.stiffness
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated error icon
                AnimatedErrorIcon(
                    isVisible = isVisible,
                    size = 64.dp,
                    color = MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                subtitle?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onRetry != null) {
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRetry()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ሞክር")
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("እሺ")
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedErrorIcon(
    isVisible: Boolean,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.error
) {
    val rotation by animateFloatAsState(
        targetValue = if (isVisible) 360f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        ),
        label = "error_rotation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "error_scale"
    )
    
    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error indicator",
            tint = color,
            modifier = Modifier.size(size)
        )
    }
}

/**
 * Warning Animation Card - New addition
 */
@Composable
fun WarningAnimationCard(
    isVisible: Boolean,
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit = {},
    onAction: (() -> Unit)? = null,
    actionText: String = "እሺ",
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(300)
        ) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0) // Warning orange background
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated warning icon
                AnimatedWarningIcon(
                    isVisible = isVisible,
                    size = 64.dp,
                    color = Color(0xFFFF9800) // Warning orange
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF5D4037),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                subtitle?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5D4037).copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onAction != null) {
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAction()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(actionText)
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("እሺ")
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedWarningIcon(
    isVisible: Boolean,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    color: Color = Color(0xFFFF9800)
) {
    val pulseScale by animateFloatAsState(
        targetValue = if (isVisible) 1.1f else 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "warning_pulse"
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(pulseScale),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Warning indicator",
            tint = color,
            modifier = Modifier.size(size)
        )
    }
}

/**
 * Loading Animation Card - New addition
 */
@Composable
fun LoadingAnimationCard(
    isVisible: Boolean,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated loading spinner
                AnimatedLoadingSpinner(
                    isVisible = isVisible,
                    size = 64.dp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                subtitle?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedLoadingSpinner(
    isVisible: Boolean,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Float = 8f
) {
    val rotation by animateFloatAsState(
        targetValue = if (isVisible) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_rotation"
    )

    Box(
        modifier = Modifier
            .size(size)
            .rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(size)
        ) {
            val canvasSize = size.toPx()
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

/**
 * LazyColumn support for multiple animated items
 */
@Composable
fun AnimatedNotificationList(
    notifications: List<NotificationItem>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(
            items = notifications,
            key = { it.id }
        ) { notification ->
            AnimatedVisibility(
                visible = notification.isVisible,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                when (notification.type) {
                    AnimationType.SUCCESS -> SuccessAnimationCard(
                        isVisible = notification.isVisible,
                        title = notification.title,
                        subtitle = notification.subtitle,
                        onDismiss = notification.onDismiss
                    )
                    AnimationType.ERROR -> ErrorAnimationCard(
                        isVisible = notification.isVisible,
                        title = notification.title,
                        subtitle = notification.subtitle,
                        onDismiss = notification.onDismiss,
                        onRetry = notification.onRetry
                    )
                    AnimationType.WARNING -> WarningAnimationCard(
                        isVisible = notification.isVisible,
                        title = notification.title,
                        subtitle = notification.subtitle,
                        onDismiss = notification.onDismiss,
                        onAction = notification.onAction
                    )
                    AnimationType.LOADING -> LoadingAnimationCard(
                        isVisible = notification.isVisible,
                        title = notification.title,
                        subtitle = notification.subtitle
                    )
                    AnimationType.FADE -> SuccessAnimationCard(
                        isVisible = notification.isVisible,
                        title = notification.title,
                        subtitle = notification.subtitle,
                        onDismiss = notification.onDismiss
                    )
                }
            }
        }
    }
}

data class NotificationItem(
    val id: String,
    val type: AnimationType,
    val title: String,
    val subtitle: String? = null,
    val isVisible: Boolean = true,
    val onDismiss: () -> Unit = {},
    val onRetry: (() -> Unit)? = null,
    val onAction: (() -> Unit)? = null
)

/**
 * Preview Composables for Development
 */
@Preview(showBackground = true)
@Composable
fun SuccessAnimationCardPreview() {
    MaterialTheme {
        SuccessAnimationCard(
            isVisible = true,
            title = "Success!",
            subtitle = "Your action was completed successfully.",
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorAnimationCardPreview() {
    MaterialTheme {
        ErrorAnimationCard(
            isVisible = true,
            title = "Error!",
            subtitle = "Something went wrong. Please try again.",
            onDismiss = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WarningAnimationCardPreview() {
    MaterialTheme {
        WarningAnimationCard(
            isVisible = true,
            title = "Warning!",
            subtitle = "Please review your action before continuing.",
            onDismiss = {},
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingAnimationCardPreview() {
    MaterialTheme {
        LoadingAnimationCard(
            isVisible = true,
            title = "Loading...",
            subtitle = "Please wait while we process your request."
        )
    }
}

/**
 * Pause/Resume functionality for animations
 */
@Composable
fun rememberAnimationController(): AnimationController {
    return remember { AnimationController() }
}

/**
 * Animation controller for pause/resume functionality
 */
class AnimationController {
    private var _isPaused by mutableStateOf(false)
    val isPaused: Boolean get() = _isPaused

    fun pause() {
        _isPaused = true
    }

    fun resume() {
        _isPaused = false
    }

    fun toggle() {
        _isPaused = !_isPaused
    }
}

/**
 * Sound effects manager (placeholder for future implementation)
 */
object SoundEffectsManager {
    private var mediaPlayer: MediaPlayer? = null
    private var isInitialized = false

    fun initialize(context: Context) {
        isInitialized = true
    }

    fun playSuccessSound(context: Context? = null) {
        context?.let {
            // Using placeholder resource ID until actual sound resources are added
            // playSound(it, R.raw.sound_success)
            playSound(it, android.R.drawable.ic_media_play) // Temporary placeholder
        }
    }

    fun playErrorSound(context: Context? = null) {
        context?.let {
            // Using placeholder resource ID until actual sound resources are added
            // playSound(it, R.raw.sound_error)
            playSound(it, android.R.drawable.ic_media_play) // Temporary placeholder
        }
    }

    fun playWarningSound(context: Context? = null) {
        context?.let {
            // Using placeholder resource ID until actual sound resources are added
            // playSound(it, R.raw.sound_warning)
            playSound(it, android.R.drawable.ic_media_play) // Temporary placeholder
        }
    }

    private fun playSound(context: Context, resourceId: Int) {
        try {
            // Release previous instance if exists
            mediaPlayer?.release()

            // Create and configure new MediaPlayer
            mediaPlayer = MediaPlayer.create(context, resourceId).apply {
                setOnCompletionListener { mp ->
                    mp.release()
                    mediaPlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("SoundEffectsManager", "Error playing sound", e)
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

// Performance optimization: Use cached Path object
private fun DrawScope.drawCheckmark(
    color: Color,
    progress: Float,
    strokeWidth: Float,
    path: Path
) {
    try {
        path.reset()
        val centerX = size.width / 2
        val centerY = size.height / 2
        val checkSize = size.width * 0.3f

        // Define checkmark points
        val startX = centerX - checkSize * 0.5f
        val startY = centerY
        val middleX = centerX - checkSize * 0.1f
        val middleY = centerY + checkSize * 0.3f
        val endX = centerX + checkSize * 0.5f
        val endY = centerY - checkSize * 0.3f

        path.moveTo(startX, startY)

        if (progress <= 0.5f) {
            // First half of checkmark
            val currentProgress = progress * 2f
            val currentX = startX + (middleX - startX) * currentProgress
            val currentY = startY + (middleY - startY) * currentProgress
            path.lineTo(currentX, currentY)
        } else {
            // Complete first half and draw second half
            path.lineTo(middleX, middleY)
            val secondProgress = (progress - 0.5f) * 2f
            val currentX = middleX + (endX - middleX) * secondProgress
            val currentY = middleY + (endY - middleY) * secondProgress
            path.lineTo(currentX, currentY)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    } catch (e: Exception) {
        // Fallback: Draw simple checkmark
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.5f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.8f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
