package com.dawitf.akahidegn.features.profile

/**
 * Data classes for user preferences
 */
data class AccessibilityPreferences(
    val screenReaderEnabled: Boolean = false,
    val highContrastMode: Boolean = false,
    val largeTextEnabled: Boolean = false,
    val voiceAnnouncementsEnabled: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true
)

data class RidePreferences(
    val maxWaitTime: Int = 15, // minutes
    val preferredVehicleType: String = "any",
    val musicPreference: String = "driver_choice",
    val smokingPolicy: String = "no_smoking",
    val petPolicy: String = "no_pets",
    val conversationLevel: String = "normal",
    val temperaturePreference: String = "comfortable"
)

enum class BadgeCategory {
    ACHIEVEMENT,
    MILESTONE,
    SPECIAL,
    SEASONAL,
    COMMUNITY
}
