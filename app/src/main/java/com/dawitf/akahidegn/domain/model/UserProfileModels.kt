package com.dawitf.akahidegn.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Enhanced user profile model with additional features.
 */
data class UserProfile(
    val userId: String,
    val displayName: String,
    val profilePictureUrl: String?,
    val email: String?,
    val phoneNumber: String?,
    val isVerified: Boolean = false,
    val joinDate: Long,
    val lastActiveDate: Long,
    
    // Rating and reviews
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    
    // Trip statistics
    val totalTrips: Int = 0,
    val completedTrips: Int = 0,
    val cancelledTrips: Int = 0,
    val totalPassengers: Int = 0,
    val totalDistance: Double = 0.0, // in kilometers
    val totalEarnings: Double = 0.0, // in currency
    
    // Profile settings
    val bio: String = "",
    val preferences: UserPreferences = UserPreferences(),
    val privacySettings: PrivacySettings = PrivacySettings(),
    
    // Status
    val isActive: Boolean = true,
    val accountStatus: AccountStatus = AccountStatus.ACTIVE
)

/**
 * User preferences for customization.
 */
data class UserPreferences(
    val preferredLanguage: String = "system",
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val searchRadius: Double = 25.0, // in kilometers
    val autoAcceptRadius: Double = 5.0, // in kilometers
    val maxPassengers: Int = 4,
    val vehicleInfo: VehicleInfo? = null
)

/**
 * Vehicle information for drivers.
 */
data class VehicleInfo(
    val make: String,
    val model: String,
    val year: Int,
    val color: String,
    val licensePlate: String,
    val seatCount: Int = 4,
    val isVerified: Boolean = false
)

/**
 * Privacy settings for user profile.
 */
data class PrivacySettings(
    val showEmail: Boolean = false,
    val showPhoneNumber: Boolean = false,
    val showLastActive: Boolean = true,
    val showTripHistory: Boolean = true,
    val allowLocationSharing: Boolean = true,
    val allowReviews: Boolean = true
)

/**
 * Account status enumeration.
 */
enum class AccountStatus {
    ACTIVE,
    SUSPENDED,
    BANNED,
    PENDING_VERIFICATION,
    DEACTIVATED
}

/**
 * User review model.
 */
data class UserReview(
    val reviewId: String,
    val reviewerId: String,
    val reviewerName: String,
    val reviewerProfilePicture: String?,
    val targetUserId: String,
    val tripId: String?,
    val rating: Float, // 1.0 to 5.0
    val comment: String,
    val timestamp: Long,
    val isVerified: Boolean = false, // Verified trip-based review
    val helpfulCount: Int = 0,
    val reportCount: Int = 0,
    
    // Review categories
    val punctuality: Float? = null,
    val communication: Float? = null,
    val vehicleCondition: Float? = null,
    val safety: Float? = null,
    val overall: Float = rating
)

/**
 * Trip history model.
 */
data class TripHistory(
    val tripId: String,
    val groupId: String?,
    val destination: String,
    val origin: String,
    val date: Long,
    val status: String, // completed, cancelled, in_progress, pending
    val role: String, // driver, passenger, co_passenger
    val passengerCount: Int,
    val price: Double,
    val distance: Double,
    val duration: Long, // in minutes
    val rating: Float? = null,
    val wasRated: Boolean = false,
    val notes: String = ""
)

/**
 * User achievement model.
 */
data class Achievement(
    val achievementId: String,
    val title: String,
    val description: String,
    val emoji: String,
    val color: Color,
    val category: AchievementCategory,
    val unlockedDate: Long,
    val rarity: AchievementRarity = AchievementRarity.COMMON,
    val progress: Int = 100, // Percentage completed
    val maxProgress: Int = 100
)

/**
 * User analytics data.
 */
data class UserAnalytics(
    val userId: String,
    val totalAppUsage: Long, // in milliseconds
    val averageSessionLength: Long, // in milliseconds
    val searchCount: Int,
    val groupCreationCount: Int,
    val groupJoinCount: Int,
    val messagesSent: Int,
    val favoriteDestinations: List<String>,
    val peakUsageHours: List<Int>, // 0-23 hours
    val mostActiveDay: String, // Monday, Tuesday, etc.
    val conversionRate: Float, // searches to actual trips
    val referralCount: Int,
    val reportsMade: Int,
    val reportsReceived: Int
)
