package com.dawitf.akahidegn.ui.viewmodels

import com.dawitf.akahidegn.ui.components.AnimationType

/**
 * Sealed class representing different animation analytics events
 */
sealed class AnimationAnalyticsEvent {
    abstract val timestamp: Long
    
    data class NotificationShown(
        override val timestamp: Long,
        val type: String,
        val title: String,
        val notificationId: String
    ) : AnimationAnalyticsEvent()
    
    data class NotificationDismissed(
        override val timestamp: Long,
        val notificationId: String,
        val duration: Long
    ) : AnimationAnalyticsEvent()
    
    data class AnimationStarted(
        override val timestamp: Long,
        val animationType: AnimationType,
        val duration: Long
    ) : AnimationAnalyticsEvent()
    
    data class AnimationCompleted(
        override val timestamp: Long,
        val animationType: AnimationType,
        val actualDuration: Long
    ) : AnimationAnalyticsEvent()
    
    data class UserInteraction(
        override val timestamp: Long,
        val interactionType: String,
        val elementId: String
    ) : AnimationAnalyticsEvent()
}
