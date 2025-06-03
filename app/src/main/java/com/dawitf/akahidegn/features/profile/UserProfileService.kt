package com.dawitf.akahidegn.features.profile

import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.core.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Service for managing user profiles and ride statistics
 */
interface UserProfileService {
    
    // Profile management
    suspend fun getUserProfile(): Result<UserProfile>
    suspend fun updateUserProfile(profile: UserProfileUpdate): Result<Unit>
    suspend fun uploadProfilePhoto(photoUri: String): Result<String>
    
    // Ride statistics
    fun getRideStats(): Flow<RideStatistics>
    fun getRideHistory(): Flow<List<RideHistoryItem>>
    suspend fun getRideDetails(rideId: String): Result<RideDetails>
    
    // Achievement and badges
    fun getUserAchievements(): Flow<List<Achievement>>
    fun getBadgeProgress(): Flow<List<BadgeProgress>>
    
    // Preferences
    suspend fun updateUserPreferences(preferences: UserPreferences): Result<Unit>
    suspend fun getUserPreferences(): Result<UserPreferences>
    
    // Social features
    fun getFriends(): Flow<List<Friend>>
    suspend fun sendFriendRequest(userId: String): Result<Unit>
    suspend fun acceptFriendRequest(requestId: String): Result<Unit>
    
    // Referral system
    suspend fun generateReferralCode(): Result<String>
    fun getReferralStats(): Flow<ReferralStats>
    
    // Carbon footprint tracking
    fun getCarbonFootprintData(): Flow<CarbonFootprintData>
}

data class UserProfile(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val profilePhotoUrl: String? = null,
    val dateOfBirth: Long? = null,
    val gender: Gender? = null,
    val joinDate: Long,
    val isVerified: Boolean = false,
    val verificationLevel: VerificationLevel = VerificationLevel.BASIC,
    val rating: Float = 0f,
    val totalRating: Int = 0,
    val emergencyContact: EmergencyContact? = null,
    val homeAddress: Address? = null,
    val workAddress: Address? = null,
    val preferredPaymentMethod: PaymentMethod? = null,
    val languages: List<String> = emptyList(),
    val bio: String? = null,
    val interests: List<String> = emptyList()
)

data class UserProfileUpdate(
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val dateOfBirth: Long? = null,
    val gender: Gender? = null,
    val emergencyContact: EmergencyContact? = null,
    val homeAddress: Address? = null,
    val workAddress: Address? = null,
    val preferredPaymentMethod: PaymentMethod? = null,
    val languages: List<String>? = null,
    val bio: String? = null,
    val interests: List<String>? = null
)

enum class Gender {
    MALE, FEMALE, NON_BINARY, PREFER_NOT_TO_SAY
}

enum class VerificationLevel {
    BASIC, PHONE_VERIFIED, EMAIL_VERIFIED, ID_VERIFIED, FULL_VERIFIED
}

data class EmergencyContact(
    val name: String,
    val phoneNumber: String,
    val relationship: String
)

data class Address(
    val street: String,
    val city: String,
    val state: String,
    val country: String,
    val postalCode: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isDefault: Boolean = false
)

enum class PaymentMethod {
    CASH, CREDIT_CARD, DEBIT_CARD, MOBILE_PAYMENT, WALLET
}

data class RideStatistics(
    val totalRides: Int,
    val totalDistance: Double, // in kilometers
    val totalSpent: Double,
    val averageRating: Float,
    val totalTimeSaved: Int, // in minutes
    val carbonSaved: Double, // in kg CO2
    val favoriteDestination: String? = null,
    val mostUsedRoute: String? = null,
    val ridingStreak: Int = 0, // consecutive days with rides
    val totalRidingTime: Int = 0, // in minutes
    val averageWaitTime: Int = 0, // in minutes
    val cancelledRides: Int = 0,
    val completedRides: Int = 0,
    val monthlyRides: Map<String, Int> = emptyMap(), // month -> ride count
    val ridesByTimeOfDay: Map<String, Int> = emptyMap(), // hour -> ride count
    val preferredRideTypes: Map<String, Int> = emptyMap() // ride type -> count
)

data class RideHistoryItem(
    val id: String,
    val date: Long,
    val from: String,
    val to: String,
    val distance: Double,
    val duration: Int, // minutes
    val fare: Double,
    val status: RideStatus,
    val driverName: String? = null,
    val driverRating: Float? = null,
    val vehicleInfo: String? = null,
    val paymentMethod: PaymentMethod,
    val rideType: RideType = RideType.REGULAR,
    val groupSize: Int = 1,
    val carbonSaved: Double = 0.0
)

enum class RideStatus {
    COMPLETED, CANCELLED, IN_PROGRESS, SCHEDULED
}

enum class RideType {
    REGULAR, SHARED, PREMIUM, EXPRESS, SCHEDULED
}

data class RideDetails(
    val id: String,
    val date: Long,
    val pickupLocation: LocationInfo,
    val dropoffLocation: LocationInfo,
    val distance: Double,
    val duration: Int,
    val fare: FareBreakdown,
    val status: RideStatus,
    val driver: DriverInfo? = null,
    val vehicle: VehicleInfo? = null,
    val route: List<RoutePoint> = emptyList(),
    val timeline: List<RideTimelineEvent> = emptyList(),
    val rating: RideRating? = null,
    val receipt: RideReceipt? = null
)

data class LocationInfo(
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long? = null,
    val landmark: String? = null
)

data class FareBreakdown(
    val baseFare: Double,
    val distanceFare: Double,
    val timeFare: Double,
    val surcharge: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val tip: Double = 0.0,
    val totalFare: Double
)

data class DriverInfo(
    val id: String,
    val name: String,
    val rating: Float,
    val totalTrips: Int,
    val photoUrl: String? = null,
    val phoneNumber: String? = null
)

data class VehicleInfo(
    val make: String,
    val model: String,
    val year: Int,
    val color: String,
    val plateNumber: String,
    val capacity: Int
)

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

data class RideTimelineEvent(
    val type: TimelineEventType,
    val timestamp: Long,
    val description: String,
    val location: LocationInfo? = null
)

enum class TimelineEventType {
    RIDE_REQUESTED,
    DRIVER_ASSIGNED,
    DRIVER_ARRIVED,
    RIDE_STARTED,
    RIDE_COMPLETED,
    RIDE_CANCELLED
}

data class RideRating(
    val driverRating: Float,
    val vehicleRating: Float,
    val overallRating: Float,
    val comment: String? = null,
    val categories: Map<String, Float> = emptyMap() // e.g., "punctuality" -> 4.5
)

data class RideReceipt(
    val receiptNumber: String,
    val issueDate: Long,
    val taxInformation: String? = null,
    val companyInfo: String? = null
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconUrl: String,
    val unlockedDate: Long? = null,
    val progress: Float = 0f,
    val maxProgress: Float = 1f,
    val category: AchievementCategory,
    val points: Int = 0,
    val isUnlocked: Boolean = false
)

enum class AchievementCategory {
    RIDES, DISTANCE, SAVINGS, SOCIAL, ECO_FRIENDLY, STREAKS, SPECIAL
}

data class BadgeProgress(
    val badgeId: String,
    val title: String,
    val description: String,
    val iconUrl: String,
    val currentProgress: Int,
    val targetProgress: Int,
    val progressPercentage: Float,
    val isCompleted: Boolean = false,
    val category: BadgeCategory,
    val reward: String? = null
)

enum class BadgeCategory {
    BEGINNER, FREQUENT_RIDER, ECO_WARRIOR, SOCIAL_BUTTERFLY, EXPLORER, PREMIUM
}

data class UserPreferences(
    val notifications: NotificationPreferences,
    val privacy: PrivacyPreferences,
    val accessibility: AccessibilityPreferences,
    val ridePreferences: RidePreferences,
    val theme: ThemePreference = ThemePreference.AUTO,
    val language: String = "en",
    val currency: String = "USD"
)

data class NotificationPreferences(
    val pushNotifications: Boolean = true,
    val emailNotifications: Boolean = true,
    val smsNotifications: Boolean = false,
    val rideUpdates: Boolean = true,
    val promotions: Boolean = true,
    val socialUpdates: Boolean = true,
    val securityAlerts: Boolean = true,
    val weeklyDigest: Boolean = true
)

data class PrivacyPreferences(
    val shareLocationWithFriends: Boolean = false,
    val shareRideHistory: Boolean = false,
    val allowFriendRequests: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val dataCollection: Boolean = true,
    val analyticsOptOut: Boolean = false
)

data class AccessibilityPreferences(
    val fontSize: FontSize = FontSize.MEDIUM,
    val highContrast: Boolean = false,
    val voiceAssistance: Boolean = false,
    val largeButtons: Boolean = false,
    val screenReader: Boolean = false,
    val colorBlindSupport: Boolean = false
)

enum class FontSize {
    SMALL, MEDIUM, LARGE, EXTRA_LARGE
}

data class RidePreferences(
    val defaultRideType: RideType = RideType.REGULAR,
    val allowSharedRides: Boolean = true,
    val maxWaitTime: Int = 10, // minutes
    val preferredPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val musicPreference: Boolean = true,
    val temperaturePreference: TemperaturePreference = TemperaturePreference.MODERATE,
    val conversationPreference: ConversationPreference = ConversationPreference.OPTIONAL,
    val petFriendly: Boolean = false,
    val childSeatRequired: Boolean = false
)

enum class TemperaturePreference {
    COOL, MODERATE, WARM
}

enum class ConversationPreference {
    NONE, OPTIONAL, PREFERRED
}

enum class ThemePreference {
    LIGHT, DARK, AUTO
}

data class Friend(
    val id: String,
    val name: String,
    val profilePhotoUrl: String? = null,
    val joinDate: Long,
    val mutualFriends: Int = 0,
    val totalRides: Int = 0,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null,
    val favoriteDestinations: List<String> = emptyList()
)

data class ReferralStats(
    val referralCode: String,
    val totalReferrals: Int,
    val successfulReferrals: Int,
    val totalEarnings: Double,
    val pendingRewards: Double,
    val recentReferrals: List<RecentReferral> = emptyList()
)

data class RecentReferral(
    val friendName: String,
    val joinDate: Long,
    val status: ReferralStatus,
    val reward: Double
)

enum class ReferralStatus {
    PENDING, COMPLETED, EXPIRED
}

data class CarbonFootprintData(
    val totalCarbonSaved: Double, // kg CO2
    val carbonSavedThisMonth: Double,
    val carbonSavedThisYear: Double,
    val equivalentTrees: Int, // trees planted equivalent
    val comparisonData: CarbonComparison,
    val monthlyTrend: List<MonthlyCarbonData> = emptyList(),
    val tips: List<EcoTip> = emptyList()
)

data class CarbonComparison(
    val vsPrivateCar: Double, // percentage reduction
    val vsPublicTransport: Double,
    val vsAverageUser: Double
)

data class MonthlyCarbonData(
    val month: String,
    val carbonSaved: Double,
    val ridesCount: Int
)

data class EcoTip(
    val id: String,
    val title: String,
    val description: String,
    val impact: String, // e.g., "Save 2kg CO2"
    val iconUrl: String? = null
)
