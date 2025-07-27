package com.dawitf.akahidegn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawitf.akahidegn.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing animation states and notification queue
 * Provides centralized state management for all animation components
 */
class AnimationViewModel : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    /** StateFlow of current notifications */
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    /** StateFlow indicating if any loading notifications are active */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _animationSettings = MutableStateFlow(AnimationSettings())
    /** StateFlow of current animation settings */
    val animationSettings: StateFlow<AnimationSettings> = _animationSettings.asStateFlow()

    private val _analyticsEvents = MutableStateFlow<List<AnimationAnalyticsEvent>>(emptyList())
    /** StateFlow of analytics events */
    val analyticsEvents: StateFlow<List<AnimationAnalyticsEvent>> = _analyticsEvents.asStateFlow()

    private val animationController = AnimationController()

    /**
     * Show a success notification
     */
    fun showSuccess(
        title: String,
        subtitle: String? = null,
        preset: NotificationPresets.SuccessPreset = NotificationPresets.quickSuccess(title),
        trackAnalytics: Boolean = true
    ) {
        val id = generateId()
        val notification = NotificationItem(
            id = id,
            type = AnimationType.SUCCESS,
            title = title,
            subtitle = subtitle,
            isVisible = true,
            onDismiss = { dismissNotification(id) }
        )

        addNotification(notification)

        if (trackAnalytics) {
            trackAnimationEvent(
                AnimationAnalyticsEvent.NotificationShown(
                    type = AnimationType.SUCCESS,
                    title = title,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        // Auto-dismiss after preset delay
        viewModelScope.launch {
            delay(preset.autoDismissDelay)
            dismissNotification(id)
        }
    }

    /**
     * Show an error notification
     */
    fun showError(
        title: String,
        subtitle: String? = null,
        preset: NotificationPresets.ErrorPreset = NotificationPresets.criticalError(title, subtitle),
        onRetry: (() -> Unit)? = null,
        trackAnalytics: Boolean = true
    ) {
        val id = generateId()
        val notification = NotificationItem(
            id = id,
            type = AnimationType.ERROR,
            title = title,
            subtitle = subtitle,
            isVisible = true,
            onDismiss = { dismissNotification(id) },
            onRetry = onRetry
        )

        addNotification(notification)

        if (trackAnalytics) {
            trackAnimationEvent(
                AnimationAnalyticsEvent.NotificationShown(
                    type = AnimationType.ERROR,
                    title = title,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        // Auto-dismiss only if delay is set
        if (preset.autoDismissDelay > 0) {
            viewModelScope.launch {
                delay(preset.autoDismissDelay)
                dismissNotification(id)
            }
        }
    }

    /**
     * Show a loading notification
     */
    fun showLoading(
        title: String,
        subtitle: String? = null,
        trackAnalytics: Boolean = true
    ): String {
        val id = generateId()
        val notification = NotificationItem(
            id = id,
            type = AnimationType.LOADING,
            title = title,
            subtitle = subtitle,
            isVisible = true,
            onDismiss = { dismissNotification(id) }
        )

        addNotification(notification)
        _isLoading.value = true

        if (trackAnalytics) {
            trackAnimationEvent(
                AnimationAnalyticsEvent.NotificationShown(
                    type = AnimationType.LOADING,
                    title = title,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        return id
    }

    /**
     * Hide loading notification
     */
    fun hideLoading(loadingId: String? = null) {
        if (loadingId != null) {
            dismissNotification(loadingId)
        } else {
            // Dismiss all loading notifications
            val loadingNotifications = _notifications.value.filter { it.type == AnimationType.LOADING }
            loadingNotifications.forEach { dismissNotification(it.id) }
        }

        // Check if any loading notifications remain
        val hasLoading = _notifications.value.any { it.type == AnimationType.LOADING && it.isVisible }
        _isLoading.value = hasLoading
    }

    /**
     * Dismiss a specific notification
     */
    fun dismissNotification(notificationId: String) {
        val currentNotifications = _notifications.value.toMutableList()
        val index = currentNotifications.indexOfFirst { it.id == notificationId }

        if (index != -1) {
            currentNotifications[index] = currentNotifications[index].copy(isVisible = false)
            _notifications.value = currentNotifications

            // Remove from list after animation completes
            viewModelScope.launch {
                delay(500) // Wait for exit animation
                val updatedNotifications = _notifications.value.toMutableList()
                updatedNotifications.removeAll { it.id == notificationId }
                _notifications.value = updatedNotifications
            }

            trackAnimationEvent(
                AnimationAnalyticsEvent.NotificationDismissed(
                    notificationId = notificationId,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Get animation controller
     */
    fun getAnimationController(): AnimationController = animationController

    // Private helper methods

    private fun addNotification(notification: NotificationItem) {
        val currentNotifications = _notifications.value.toMutableList()
        currentNotifications.add(notification)
        _notifications.value = currentNotifications
    }

    private fun generateId(): String = "notification_${System.currentTimeMillis()}_${(0..9999).random()}"

    private fun trackAnimationEvent(event: AnimationAnalyticsEvent) {
        val currentEvents = _analyticsEvents.value.toMutableList()
        currentEvents.add(event)
        _analyticsEvents.value = currentEvents
    }

    // Convenience methods for common operations

    /**
     * Show form submission success
     */
    fun showFormSubmissionSuccess() {
        showSuccess(
            title = "ተሳክቷል!",
            subtitle = "ቅጹ በተሳካ ሁኔታ ተልኳል።",
            preset = ContextPresets.FormSubmission.success
        )
    }

    /**
     * Show form submission error
     */
    fun showFormSubmissionError(onRetry: () -> Unit) {
        showError(
            title = "ስህተት!",
            subtitle = "ቅጹ መላክ አልተሳካም። እባክዎ እንደገና ይሞክሩ።",
            preset = ContextPresets.FormSubmission.error,
            onRetry = onRetry
        )
    }

    /**
     * Show network connection success
     */
    fun showNetworkConnected() {
        showSuccess(
            title = "ተከናውኗል!",
            subtitle = "ኢንተርኔት ግንኙነት ተመለሰ።",
            preset = ContextPresets.NetworkOperations.connected
        )
    }

    /**
     * Show network disconnection
     */
    fun showNetworkDisconnected() {
        showError(
            title = "ግንኙነት ተቋረጠ!",
            subtitle = "ኢንተርኔት ግንኙነት የለም።",
            preset = ContextPresets.NetworkOperations.disconnected
        )
    }

    /**
     * Show login success
     */
    fun showLoginSuccess() {
        showSuccess(
            title = "እንኳን ደህና መጡ!",
            subtitle = "በተሳካ ሁ��ታ ተመዝገቡ።",
            preset = ContextPresets.Authentication.loginSuccess
        )
    }
}

/**
 * Animation settings data class
 * @property enableAnimations Whether animations are enabled
 * @property enableSoundEffects Whether sound effects are enabled
 * @property enableHapticFeedback Whether haptic feedback is enabled
 * @property enableReducedMotion Whether reduced motion is enabled for accessibility
 * @property animationSpeed Default animation speed
 * @property defaultAnimationPreset Default animation preset
 * @property maxNotificationsVisible Maximum notifications visible at once
 * @property enableAnalytics Whether analytics tracking is enabled
 */
data class AnimationSettings(
    val enableAnimations: Boolean = true,
    val enableSoundEffects: Boolean = false,
    val enableHapticFeedback: Boolean = true,
    val enableReducedMotion: Boolean = false,
    val animationSpeed: AnimationSpeed = AnimationSpeed.Normal,
    val defaultAnimationPreset: AnimationConfig = AnimationPresets.Smooth,
    val maxNotificationsVisible: Int = 3,
    val enableAnalytics: Boolean = true
)

/**
 * Animation analytics events
 */
sealed class AnimationAnalyticsEvent {
    /** Notification shown event */
    data class NotificationShown(
        val type: AnimationType,
        val title: String,
        val timestamp: Long
    ) : AnimationAnalyticsEvent()

    /** Notification dismissed event */
    data class NotificationDismissed(
        val notificationId: String,
        val timestamp: Long
    ) : AnimationAnalyticsEvent()
}

/**
 * Extension functions for easier ViewModel usage
 */
fun AnimationViewModel.showQuickSuccess(message: String) {
    showSuccess(
        title = message,
        preset = NotificationPresets.quickSuccess(message)
    )
}
