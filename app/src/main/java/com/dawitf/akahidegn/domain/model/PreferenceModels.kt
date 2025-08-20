package com.dawitf.akahidegn.domain.model

/**
 * User notification preferences
 */
data class NotificationPreferences(
    val rideUpdates: Boolean = true,
    val promotions: Boolean = true,
    val accountUpdates: Boolean = true,
    val friendRequests: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00"
)

/**
 * User privacy preferences
 */
data class PrivacyPreferences(
    val showProfilePhoto: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val shareLocationWhileRiding: Boolean = true,
    val shareRideHistory: Boolean = false,
    val shareContactInfo: Boolean = false
)
