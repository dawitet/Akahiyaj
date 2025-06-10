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
    
    // Achievement and badges - REMOVED for simplicity
    // fun getUserAchievements(): Flow<List<Achievement>>
    // fun getBadgeProgress(): Flow<List<BadgeProgress>>
    
    // Preferences
    suspend fun updateUserPreferences(preferences: UserPreferences): Result<Unit>
    suspend fun getUserPreferences(): Result<UserPreferences>
    
    // Social features - simplified for Ethiopian market
    fun getFriends(): Flow<List<Friend>>
    suspend fun sendFriendRequest(userId: String): Result<Unit>
    
    // Referral system - REMOVED for simplicity
    // suspend fun generateReferralCode(): Result<String>
    // fun getReferralStats(): Flow<ReferralStats>
    
    // Carbon footprint tracking - REMOVED for simplicity
    // fun getCarbonFootprintData(): Flow<CarbonFootprintData>
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
    val totalTimeSaved: Long, // in minutes - changed from Int to Long for consistency
    val carbonSaved: Double // in kg CO2 - simplified, removed all complex carbon tracking fields
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

// Achievement system removed for simplicity - not needed for basic Ethiopian ride-sharing

// Badge system removed for simplicity - not needed for basic Ethiopian ride-sharing

data class UserPreferences(
    val notifications: NotificationPreferences = NotificationPreferences(),
    val privacy: PrivacyPreferences = PrivacyPreferences(),
    val language: String = "en",
    val theme: String = "system",
    val units: UnitPreferences = UnitPreferences()
)

data class NotificationPreferences(
    val rideUpdates: Boolean = true,
    val promotions: Boolean = true,
    val news: Boolean = true,
    val sound: Boolean = true,
    val vibration: Boolean = true
)

data class PrivacyPreferences(
    val shareLocation: Boolean = true,
    val shareProfile: Boolean = true,
    val shareRideHistory: Boolean = false
)

data class UnitPreferences(
    val distance: String = "km",
    val currency: String = "USD",
    val temperature: String = "C"
)

data class Friend(
    val id: String,
    val name: String,
    val photoUrl: String? = null
)

// Referral system removed for simplicity - not needed for basic Ethiopian ride-sharing

// Removed carbon footprint tracking - not relevant for Ethiopian market
