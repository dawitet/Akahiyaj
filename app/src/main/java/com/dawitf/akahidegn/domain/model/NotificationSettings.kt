package com.dawitf.akahidegn.domain.model

/**
 * Data class representing user notification preferences
 */
data class NotificationSettings(
    val notificationsEnabled: Boolean = true,
    val chatNotificationsEnabled: Boolean = true,
    val tripNotificationsEnabled: Boolean = true,
    val systemNotificationsEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00",
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val showInLockScreen: Boolean = true,
    val groupByType: Boolean = true
)
