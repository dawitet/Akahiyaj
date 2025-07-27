package com.dawitf.akahidegn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Animation Preset Configurations
 * Pre-configured animation setups for common use cases
 */
object AnimationPresets {

    /**
     * Quick animation preset for fast interactions
     */
    val Quick = AnimationConfig(
        duration = 300,
        delay = 0,
        easing = FastOutSlowInEasing,
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )

    /**
     * Smooth animation preset for elegant transitions
     */
    val Smooth = AnimationConfig(
        duration = 800,
        delay = 100,
        easing = FastOutSlowInEasing,
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * Bouncy animation preset for playful interactions
     */
    val Bouncy = AnimationConfig(
        duration = 1000,
        delay = 150,
        easing = FastOutSlowInEasing,
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    /**
     * Gentle animation preset for subtle effects
     */
    val Gentle = AnimationConfig(
        duration = 1200,
        delay = 200,
        easing = LinearOutSlowInEasing,
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessVeryLow
    )

    /**
     * Dramatic animation preset for important notifications
     */
    val Dramatic = AnimationConfig(
        duration = 1500,
        delay = 300,
        easing = FastOutSlowInEasing,
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessLow
    )

    /**
     * Accessibility-friendly animation preset (reduced motion)
     */
    val Accessible = AnimationConfig(
        duration = 150,
        delay = 0,
        easing = LinearEasing,
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )
}

/**
 * Notification Type Presets
 * Complete configurations for different notification types
 */
object NotificationPresets {

    /**
     * Success notification preset
     * @property title The main title text
     * @property subtitle Optional subtitle text
     * @property animationConfig Configuration for animation parameters
     * @property animationSpeed Speed multiplier for animations
     * @property autoDismissDelay Auto-dismiss delay in milliseconds
     * @property enableSwipeToDismiss Whether swipe-to-dismiss is enabled
     * @property slideDirection Direction for slide animation
     * @property size Size variant for the animation
     */
    data class SuccessPreset(
        val title: String,
        val subtitle: String? = null,
        val animationConfig: AnimationConfig = AnimationPresets.Smooth,
        val animationSpeed: AnimationSpeed = AnimationSpeed.Normal,
        val autoDismissDelay: Long = 3000L,
        val enableSwipeToDismiss: Boolean = true,
        val slideDirection: SlideDirection = SlideDirection.TOP,
        val size: AnimationSize = AnimationSize.Medium
    )

    /**
     * Error notification preset
     * @property title The main title text
     * @property subtitle Optional subtitle text
     * @property animationConfig Configuration for animation parameters
     * @property showRetryButton Whether to show retry button
     * @property autoDismissDelay Auto-dismiss delay in milliseconds (0 = no auto-dismiss)
     */
    data class ErrorPreset(
        val title: String,
        val subtitle: String? = null,
        val animationConfig: AnimationConfig = AnimationPresets.Dramatic,
        val showRetryButton: Boolean = true,
        val autoDismissDelay: Long = 5000L
    )

    /**
     * Warning notification preset
     * @property title The main title text
     * @property subtitle Optional subtitle text
     * @property animationConfig Configuration for animation parameters
     * @property showActionButton Whether to show action button
     * @property actionText Text for the action button
     * @property autoDismissDelay Auto-dismiss delay in milliseconds
     */
    data class WarningPreset(
        val title: String,
        val subtitle: String? = null,
        val animationConfig: AnimationConfig = AnimationPresets.Bouncy,
        val showActionButton: Boolean = true,
        val actionText: String = "እሺ",
        val autoDismissDelay: Long = 4000L
    )

    /**
     * Loading notification preset
     * @property title The main title text
     * @property subtitle Optional subtitle text
     * @property animationConfig Configuration for animation parameters
     * @property showSpinner Whether to show loading spinner
     */
    data class LoadingPreset(
        val title: String,
        val subtitle: String? = null,
        val animationConfig: AnimationConfig = AnimationPresets.Gentle,
        val showSpinner: Boolean = true
    )

    /**
     * Quick success preset for common use cases
     */
    fun quickSuccess(message: String) = SuccessPreset(
        title = message,
        animationConfig = AnimationPresets.Quick,
        animationSpeed = AnimationSpeed.Fast,
        autoDismissDelay = 2000L,
        size = AnimationSize.Small
    )

    /**
     * Critical error preset for important errors
     */
    fun criticalError(message: String, details: String? = null) = ErrorPreset(
        title = message,
        subtitle = details,
        animationConfig = AnimationPresets.Dramatic,
        showRetryButton = true,
        autoDismissDelay = 0L // No auto-dismiss for critical errors
    )

    /**
     * Important warning preset
     */
    fun importantWarning(message: String, details: String? = null) = WarningPreset(
        title = message,
        subtitle = details,
        animationConfig = AnimationPresets.Bouncy,
        showActionButton = true,
        autoDismissDelay = 6000L // Longer delay for important warnings
    )

    /**
     * Simple loading preset
     */
    fun simpleLoading(message: String = "እየጠብቅ...") = LoadingPreset(
        title = message,
        animationConfig = AnimationPresets.Gentle
    )
}

// Context-based presets with proper documentation
object ContextPresets {

    /**
     * Form submission presets
     */
    object FormSubmission {
        /** Success preset for form submission */
        val success = NotificationPresets.SuccessPreset(
            title = "ተሳክቷል!",
            subtitle = "ቅጹ በተሳካ ሁኔታ ተልኳል።",
            animationConfig = AnimationPresets.Smooth,
            slideDirection = SlideDirection.TOP
        )

        /** Error preset for form submission */
        val error = NotificationPresets.ErrorPreset(
            title = "ስህተት!",
            subtitle = "ቅጹ መላክ አልተሳካም። እባክዎ እንደገና ይሞክሩ።",
            animationConfig = AnimationPresets.Dramatic,
            showRetryButton = true
        )

        /** Validation preset for form errors */
        val validation = NotificationPresets.WarningPreset(
            title = "ማስታወሻ!",
            subtitle = "እባክዎ ሁሉንም ሳጥኖች ይሙሉ።",
            animationConfig = AnimationPresets.Bouncy,
            showActionButton = false
        )
    }

    /**
     * Network operation presets
     */
    object NetworkOperations {
        /** Network connected preset */
        val connected = NotificationPresets.SuccessPreset(
            title = "ተከናውኗል!",
            subtitle = "ኢንተርኔት ግንኙነት ተመሳሳይ።",
            animationConfig = AnimationPresets.Quick,
            autoDismissDelay = 2000L,
            size = AnimationSize.Small
        )

        /** Network disconnected preset */
        val disconnected = NotificationPresets.ErrorPreset(
            title = "ግንኙነት ተቋረጠ!",
            subtitle = "ኢንተርኔት ግንኙነት የለም።",
            animationConfig = AnimationPresets.Dramatic,
            showRetryButton = false,
            autoDismissDelay = 0L // Keep visible until reconnected
        )
    }

    /**
     * Authentication presets
     */
    object Authentication {
        /** Login success preset */
        val loginSuccess = NotificationPresets.SuccessPreset(
            title = "እንኳን ደህና መጡ!",
            subtitle = "በተሳካ ሁኔታ ተመዝገቡ።",
            animationConfig = AnimationPresets.Smooth,
            slideDirection = SlideDirection.TOP
        )

        /** Login failed preset */
        val loginFailed = NotificationPresets.ErrorPreset(
            title = "መግቢያ አልተሳካም!",
            subtitle = "የተሳሳተ ኢሜይል ወይም ፓስወርድ።",
            animationConfig = AnimationPresets.Dramatic,
            showRetryButton = true
        )
    }
}

/**
 * Animation Preset Builder
 * Fluent API for creating custom animation presets
 */
class AnimationPresetBuilder {
    private var config = AnimationConfig()
    private var speed = AnimationSpeed.Normal
    private var direction = SlideDirection.TOP
    private var size = AnimationSize.Medium
    private var autoDismiss = 3000L
    private var swipeToDismiss = true

    fun duration(duration: Int) = apply {
        config = config.copy(duration = duration)
    }

    fun delay(delay: Int) = apply {
        config = config.copy(delay = delay)
    }

    fun easing(easing: Easing) = apply {
        config = config.copy(easing = easing)
    }

    fun dampingRatio(ratio: Float) = apply {
        config = config.copy(dampingRatio = ratio)
    }

    fun stiffness(stiffness: Float) = apply {
        config = config.copy(stiffness = stiffness)
    }

    fun speed(speed: AnimationSpeed) = apply {
        this.speed = speed
    }

    fun slideDirection(direction: SlideDirection) = apply {
        this.direction = direction
    }

    fun size(size: AnimationSize) = apply {
        this.size = size
    }

    fun autoDismissDelay(delay: Long) = apply {
        this.autoDismiss = delay
    }

    fun enableSwipeToDismiss(enable: Boolean) = apply {
        this.swipeToDismiss = enable
    }

    fun buildSuccessPreset(title: String, subtitle: String? = null) =
        NotificationPresets.SuccessPreset(
            title = title,
            subtitle = subtitle,
            animationConfig = config,
            animationSpeed = speed,
            slideDirection = direction,
            size = size,
            autoDismissDelay = autoDismiss,
            enableSwipeToDismiss = swipeToDismiss
        )

    fun buildErrorPreset(title: String, subtitle: String? = null) =
        NotificationPresets.ErrorPreset(
            title = title,
            subtitle = subtitle,
            animationConfig = config,
            autoDismissDelay = autoDismiss
        )

    fun buildWarningPreset(title: String, subtitle: String? = null) =
        NotificationPresets.WarningPreset(
            title = title,
            subtitle = subtitle,
            animationConfig = config,
            autoDismissDelay = autoDismiss
        )

    fun buildLoadingPreset(title: String, subtitle: String? = null) =
        NotificationPresets.LoadingPreset(
            title = title,
            subtitle = subtitle,
            animationConfig = config
        )
}

/**
 * Convenience function to create custom animation presets
 */
fun buildAnimationPreset(block: AnimationPresetBuilder.() -> Unit): AnimationPresetBuilder {
    return AnimationPresetBuilder().apply(block)
}

/**
 * Extension functions for easy preset application
 */
fun AnimationConfig.toQuick() = AnimationPresets.Quick
fun AnimationConfig.toSmooth() = AnimationPresets.Smooth
fun AnimationConfig.toBouncy() = AnimationPresets.Bouncy
fun AnimationConfig.toGentle() = AnimationPresets.Gentle
fun AnimationConfig.toDramatic() = AnimationPresets.Dramatic
fun AnimationConfig.toAccessible() = AnimationPresets.Accessible
