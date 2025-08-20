package com.dawitf.akahidegn.domain.model

import androidx.annotation.Keep

/**
 * Basic user analytics model
 */
@Keep
data class BasicUserAnalytics(
    val totalTrips: Int = 0,
    val averageRating: Float = 0f,
    val totalRatings: Int = 0,
    val preferredLanguage: String = "en"
)

/**
 * User profile domain model
 */
@Keep
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val profilePictureUrl: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val isVerified: Boolean = false,
    val joinDate: Long = System.currentTimeMillis(),
    val lastActiveDate: Long = System.currentTimeMillis(),
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val totalTrips: Int = 0,
    val completedTrips: Int = 0,
    val cancelledTrips: Int = 0,
    val totalPassengers: Int = 0,
    val totalDistance: Double = 0.0,

    val bio: String = "",
    val preferences: UserPreferences = UserPreferences(),
    val privacySettings: PrivacySettings = PrivacySettings(),
    val isActive: Boolean = true,
    val accountStatus: AccountStatus = AccountStatus.ACTIVE
)

/**
 * User preferences model
 */
@Keep
data class UserPreferences(
    val preferredLanguage: String = LanguageOption.ENGLISH.code,
    val language: String = LanguageOption.ENGLISH.code, // Added for compatibility with UserProfileServiceImpl
    val theme: String = "SYSTEM", // Added for compatibility with UserProfileServiceImpl
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val notifications: NotificationPreferences = NotificationPreferences(), // Added for compatibility with UserProfileServiceImpl
    val privacy: PrivacyPreferences = PrivacyPreferences(), // Added for compatibility with UserProfileServiceImpl
    val searchRadius: Double = 25.0,
    val autoAcceptRadius: Double = 5.0,
    val maxPassengers: Int = 4,
    val vehicleInfo: String? = null
)

/**
 * Notification settings model
 */
@Keep
data class NotificationSettings(
    val notificationsEnabled: Boolean = true,
    val chatNotificationsEnabled: Boolean = true,
    val tripNotificationsEnabled: Boolean = true,
    val systemNotificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00"
)

/**
 * Privacy settings model
 */
@Keep
data class PrivacySettings(
    val locationSharingEnabled: Boolean = true,
    val profileVisibilityEnabled: Boolean = true,
    val phoneNumberVisible: Boolean = false,
    val emailVisible: Boolean = false
)

/**
 * Account status enum
 */
enum class AccountStatus {
    ACTIVE,
    SUSPENDED,
    INACTIVE,
    BANNED
}

/**
 * User review model
 */
@Keep
data class UserReview(
    val reviewId: String = "",
    val reviewerId: String = "",
    val reviewerName: String = "",
    val targetUserId: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val tripId: String? = null
)

/**
 * User analytics model
 */
@Keep
data class UserAnalytics(
    val userId: String = "",
    val totalTrips: Int = 0,
    val completedTrips: Int = 0,
    val cancelledTrips: Int = 0,
    val averageRating: Float = 0f,
    val totalDistance: Double = 0.0,

    val totalSpent: Double = 0.0,
    val totalTimeSaved: Double = 0.0,
    val carbonSaved: Double = 0.0,
    val joinDate: Long = System.currentTimeMillis(),
    val lastActive: Long = System.currentTimeMillis(),
    val totalAppUsage: Long = 0L,
    val averageSessionLength: Long = 0L,
    val searchCount: Int = 0,
    val groupCreationCount: Int = 0,
    val groupJoinCount: Int = 0,
    val messagesSent: Int = 0,
    val favoriteDestinations: List<String> = emptyList(),
    val peakUsageHours: List<Int> = emptyList(),
    val mostActiveDay: String = "",
    val conversionRate: Float = 0f,
    val referralCount: Int = 0,
    val reportsMade: Int = 0,
    val reportsReceived: Int = 0,
    val searchRadius: Double = 10.0,
    val autoAcceptRadius: Double = 5.0,
    val maxPassengers: Int = 4,
    val vehicleInfo: String = ""
)
