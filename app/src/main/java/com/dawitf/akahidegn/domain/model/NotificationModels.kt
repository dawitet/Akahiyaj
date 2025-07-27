package com.dawitf.akahidegn.domain.model

/**
 * Notification item for the notification system
 */
data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.GENERAL,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionUrl: String? = null,
    val groupId: String? = null,
    val userId: String? = null
)

/**
 * Types of notifications
 */
enum class NotificationType {
    GENERAL,
    GROUP_INVITE,
    GROUP_UPDATE,
    TRIP_REMINDER,
    USER_MESSAGE,
    SYSTEM_UPDATE
}

/**
 * Font size options for accessibility
 */
enum class FontSizeOption(val displayName: String, val scale: Float) {
    SMALL("Small", 0.85f),
    NORMAL("Normal", 1.0f),
    MEDIUM("Medium", 1.15f),
    LARGE("Large", 1.15f),
    EXTRA_LARGE("Extra Large", 1.3f)
}
