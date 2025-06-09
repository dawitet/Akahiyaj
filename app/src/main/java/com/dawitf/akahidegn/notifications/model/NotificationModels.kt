package com.dawitf.akahidegn.notifications.model

import androidx.annotation.Keep

/**
 * Different types of notifications in the app.
 */
enum class NotificationType {
    GROUP_JOIN_REQUEST,
    GROUP_MEMBER_JOINED,
    GROUP_MEMBER_LEFT,
    GROUP_CHAT_MESSAGE,
    TRIP_REMINDER,
    TRIP_STARTING_SOON,
    TRIP_CANCELLED,
    SYSTEM_ANNOUNCEMENT,
    PROMOTION
}

/**
 * Base notification data structure.
 */
@Keep
data class NotificationData(
    val type: NotificationType,
    val title: String,
    val body: String,
    val groupId: String? = null,
    val userId: String? = null,
    val messageId: String? = null,
    val imageUrl: String? = null,
    val actionUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val category: String? = null,
    val sound: String? = null,
    val data: Map<String, String> = emptyMap()
)

/**
 * Notification priority levels.
 */
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Local notification preferences.
 */
@Keep
data class NotificationPreferences(
    val groupJoinRequests: Boolean = true,
    val groupActivity: Boolean = true,
    val chatMessages: Boolean = true,
    val tripReminders: Boolean = true,
    val tripAlerts: Boolean = true,
    val systemAnnouncements: Boolean = true,
    val promotions: Boolean = false,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val quietHours: QuietHours? = null
)

/**
 * Quiet hours configuration.
 */
@Keep
data class QuietHours(
    val enabled: Boolean = false,
    val startHour: Int = 22, // 10 PM
    val startMinute: Int = 0,
    val endHour: Int = 7,    // 7 AM
    val endMinute: Int = 0
)
