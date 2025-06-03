package com.dawitf.akahidegn.features.driver

import android.location.Location
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.Group
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Service for managing driver mode functionality
 */
interface DriverModeService {
    
    // Driver status management
    suspend fun enableDriverMode(driverDetails: DriverDetails): Result<Unit>
    suspend fun disableDriverMode(): Result<Unit>
    fun isDriverModeEnabled(): StateFlow<Boolean>
    
    // Ride request management
    fun getIncomingRideRequests(): Flow<List<RideRequest>>
    suspend fun acceptRideRequest(requestId: String): Result<RideAcceptance>
    suspend fun rejectRideRequest(requestId: String, reason: String): Result<Unit>
    
    // Active ride management
    fun getActiveRide(): StateFlow<ActiveRide?>
    suspend fun startRide(rideId: String): Result<Unit>
    suspend fun updateRideProgress(location: Location, status: RideStatus): Result<Unit>
    suspend fun completeRide(rideId: String, fareDetails: FareDetails): Result<RideCompletion>
    suspend fun cancelRide(rideId: String, reason: String): Result<Unit>
    
    // Driver statistics and earnings
    fun getDriverStats(): Flow<DriverStats>
    fun getDailyEarnings(): Flow<EarningsData>
    fun getWeeklyEarnings(): Flow<EarningsData>
    
    // Vehicle and availability management
    suspend fun updateVehicleInfo(vehicleInfo: VehicleInfo): Result<Unit>
    suspend fun setAvailabilityZone(zone: AvailabilityZone): Result<Unit>
    suspend fun updateDriverLocation(location: Location): Result<Unit>
    
    // Rating and feedback
    suspend fun ratePassenger(rideId: String, rating: Float, feedback: String?): Result<Unit>
    fun getDriverRating(): Flow<DriverRating>
    
    // Emergency and safety
    suspend fun triggerEmergencyAlert(): Result<Unit>
    suspend fun reportSafetyIncident(incident: SafetyIncident): Result<Unit>
}

data class DriverDetails(
    val licenseNumber: String,
    val licenseExpiryDate: Long,
    val vehicleInfo: VehicleInfo,
    val emergencyContact: EmergencyContact,
    val preferredAreas: List<String> = emptyList(),
    val workingHours: WorkingHours? = null
)

data class VehicleInfo(
    val make: String,
    val model: String,
    val year: Int,
    val color: String,
    val plateNumber: String,
    val capacity: Int,
    val fuelType: FuelType = FuelType.GASOLINE,
    val insuranceValid: Boolean = true,
    val registrationValid: Boolean = true
)

enum class FuelType {
    GASOLINE, DIESEL, ELECTRIC, HYBRID
}

data class EmergencyContact(
    val name: String,
    val phoneNumber: String,
    val relationship: String
)

data class WorkingHours(
    val startTime: String, // HH:mm format
    val endTime: String,   // HH:mm format
    val workingDays: List<Int> // 1-7 for Mon-Sun
)

data class RideRequest(
    val id: String,
    val passengerId: String,
    val passengerName: String,
    val passengerRating: Float,
    val pickupLocation: LocationData,
    val destination: LocationData,
    val estimatedDistance: Double,
    val estimatedDuration: Int, // minutes
    val suggestedFare: Double,
    val requestTime: Long,
    val expiresAt: Long,
    val specialRequests: List<String> = emptyList(),
    val passengerCount: Int = 1
)

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val landmark: String? = null
)

data class RideAcceptance(
    val rideId: String,
    val estimatedArrival: Int, // minutes
    val driverLocation: LocationData,
    val passengerContact: String
)

data class ActiveRide(
    val id: String,
    val passengerId: String,
    val passengerName: String,
    val passengerPhone: String,
    val pickupLocation: LocationData,
    val destination: LocationData,
    val status: RideStatus,
    val startTime: Long?,
    val estimatedCompletion: Long?,
    val fare: Double,
    val distance: Double = 0.0,
    val duration: Int = 0 // minutes
)

enum class RideStatus {
    ACCEPTED,
    HEADING_TO_PICKUP,
    ARRIVED_AT_PICKUP,
    PASSENGER_PICKED_UP,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

data class FareDetails(
    val baseFare: Double,
    val distanceFare: Double,
    val timeFare: Double,
    val additionalCharges: Double = 0.0,
    val discount: Double = 0.0,
    val totalFare: Double,
    val paymentMethod: PaymentMethod,
    val tip: Double = 0.0
)

enum class PaymentMethod {
    CASH, CARD, MOBILE_PAYMENT, WALLET
}

data class RideCompletion(
    val rideId: String,
    val completionTime: Long,
    val finalFare: Double,
    val distance: Double,
    val duration: Int,
    val earningsAfterCommission: Double
)

data class DriverStats(
    val totalRides: Int,
    val totalEarnings: Double,
    val averageRating: Float,
    val completionRate: Float,
    val acceptanceRate: Float,
    val totalHoursOnline: Int,
    val totalDistance: Double,
    val currentStreak: Int, // consecutive days active
    val badgesEarned: List<DriverBadge>
)

data class DriverBadge(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String,
    val earnedDate: Long
)

data class EarningsData(
    val period: String,
    val totalEarnings: Double,
    val commission: Double,
    val netEarnings: Double,
    val totalRides: Int,
    val averageEarningsPerRide: Double,
    val peakHourEarnings: Double,
    val bonuses: Double = 0.0,
    val tips: Double = 0.0
)

data class AvailabilityZone(
    val centerLatitude: Double,
    val centerLongitude: Double,
    val radiusKm: Double,
    val name: String
)

data class DriverRating(
    val averageRating: Float,
    val totalRatings: Int,
    val ratingDistribution: Map<Int, Int>, // star -> count
    val recentFeedback: List<PassengerFeedback>
)

data class PassengerFeedback(
    val rating: Float,
    val comment: String?,
    val date: Long,
    val rideId: String
)

data class SafetyIncident(
    val type: IncidentType,
    val description: String,
    val location: LocationData,
    val timestamp: Long,
    val severity: IncidentSeverity,
    val involvedPassengerId: String? = null
)

enum class IncidentType {
    HARASSMENT,
    UNSAFE_BEHAVIOR,
    PROPERTY_DAMAGE,
    MEDICAL_EMERGENCY,
    ACCIDENT,
    THEFT,
    OTHER
}

enum class IncidentSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}
