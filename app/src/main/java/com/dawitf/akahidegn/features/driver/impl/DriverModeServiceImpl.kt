package com.dawitf.akahidegn.features.driver.impl

import android.location.Location
import com.dawitf.akahidegn.analytics.AnalyticsService
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.features.driver.*
import com.dawitf.akahidegn.security.SecurityService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriverModeServiceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val analyticsService: AnalyticsService,
    private val securityService: SecurityService
) : DriverModeService {
    
    companion object {
        private const val DRIVERS_COLLECTION = "drivers"
        private const val RIDE_REQUESTS_COLLECTION = "rideRequests"
        private const val ACTIVE_RIDES_COLLECTION = "activeRides"
        private const val DRIVER_STATS_COLLECTION = "driverStats"
        private const val EARNINGS_COLLECTION = "earnings"
        private const val SAFETY_INCIDENTS_COLLECTION = "safetyIncidents"
        
        private const val MAX_REQUEST_RADIUS_KM = 10.0
        private const val REQUEST_TIMEOUT_MINUTES = 5
    }
    
    private val _isDriverModeEnabled = MutableStateFlow(false)
    private val _activeRide = MutableStateFlow<ActiveRide?>(null)
    
    private var rideRequestsListener: ListenerRegistration? = null
    private var activeRideListener: ListenerRegistration? = null
    
    override suspend fun enableDriverMode(driverDetails: DriverDetails): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            // Validate driver details
            val validationResult = validateDriverDetails(driverDetails)
            if (validationResult is Result.Error) {
                return validationResult
            }
            
            // Update driver profile in Firestore
            val driverData = mapOf(
                "userId" to userId,
                "isActive" to true,
                "licenseNumber" to driverDetails.licenseNumber,
                "licenseExpiryDate" to driverDetails.licenseExpiryDate,
                "vehicleInfo" to driverDetails.vehicleInfo,
                "emergencyContact" to driverDetails.emergencyContact,
                "preferredAreas" to driverDetails.preferredAreas,
                "workingHours" to driverDetails.workingHours,
                "lastUpdated" to System.currentTimeMillis(),
                "status" to "AVAILABLE"
            )
            
            firestore.collection(DRIVERS_COLLECTION)
                .document(userId)
                .set(driverData)
                .await()
            
            _isDriverModeEnabled.value = true
            startListeningForRideRequests()
            
            analyticsService.trackEvent("driver_mode_enabled", mapOf(
                "vehicle_type" to driverDetails.vehicleInfo.make,
                "capacity" to driverDetails.vehicleInfo.capacity.toString()
            ))
            
            Result.Success(Unit)
        } catch (e: Exception) {
            analyticsService.logError(e, "driver_mode_enable_failed")
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to enable driver mode"))
        }
    }
    
    override suspend fun disableDriverMode(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            // Check if driver has active ride
            if (_activeRide.value != null) {
                return Result.Error(AppError.ValidationError.OperationNotAllowed("Cannot disable driver mode with active ride"))
            }
            
            // Update driver status
            firestore.collection(DRIVERS_COLLECTION)
                .document(userId)
                .update(mapOf(
                    "isActive" to false,
                    "status" to "OFFLINE",
                    "lastUpdated" to System.currentTimeMillis()
                ))
                .await()
            
            _isDriverModeEnabled.value = false
            stopListeningForRideRequests()
            
            analyticsService.trackEvent("driver_mode_disabled", emptyMap())
            
            Result.Success(Unit)
        } catch (e: Exception) {
            analyticsService.logError(e, "driver_mode_disable_failed")
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to disable driver mode"))
        }
    }
    
    override fun isDriverModeEnabled(): StateFlow<Boolean> = _isDriverModeEnabled.asStateFlow()
    
    override fun getIncomingRideRequests(): Flow<List<RideRequest>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            close()
            return@callbackFlow
        }
        
        val listener = firestore.collection(RIDE_REQUESTS_COLLECTION)
            .whereEqualTo("status", "PENDING")
            .whereEqualTo("assignedDriverId", null)
            .orderBy("requestTime", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        // Safe extraction with proper null checks
                        val specialRequestsData = doc.get("specialRequests")
                        val specialRequestsList = when (specialRequestsData) {
                            is List<*> -> specialRequestsData.filterIsInstance<String>()
                            else -> emptyList()
                        }

                        // Safe extraction of location data
                        val pickupLocationData = doc.get("pickupLocation")
                        val pickupLocation = when (pickupLocationData) {
                            is Map<*, *> -> {
                                val lat = (pickupLocationData["latitude"] as? Number)?.toDouble() ?: 0.0
                                val lng = (pickupLocationData["longitude"] as? Number)?.toDouble() ?: 0.0
                                val address = pickupLocationData["address"] as? String ?: ""
                                LocationData(lat, lng, address)
                            }
                            else -> LocationData(0.0, 0.0, "")
                        }

                        val destinationData = doc.get("destination")
                        val destination = when (destinationData) {
                            is Map<*, *> -> {
                                val lat = (destinationData["latitude"] as? Number)?.toDouble() ?: 0.0
                                val lng = (destinationData["longitude"] as? Number)?.toDouble() ?: 0.0
                                val address = destinationData["address"] as? String ?: ""
                                LocationData(lat, lng, address)
                            }
                            else -> LocationData(0.0, 0.0, "")
                        }

                        RideRequest(
                            id = doc.id,
                            passengerId = doc.getString("passengerId") ?: "",
                            passengerName = doc.getString("passengerName") ?: "",
                            passengerRating = doc.getDouble("passengerRating")?.toFloat() ?: 0f,
                            pickupLocation = pickupLocation,
                            destination = destination,
                            estimatedDistance = doc.getDouble("estimatedDistance") ?: 0.0,
                            estimatedDuration = doc.getLong("estimatedDuration")?.toInt() ?: 0,
                            suggestedFare = doc.getDouble("suggestedFare") ?: 0.0,
                            requestTime = doc.getLong("requestTime") ?: 0L,
                            expiresAt = doc.getLong("expiresAt") ?: 0L,
                            specialRequests = specialRequestsList,
                            passengerCount = doc.getLong("passengerCount")?.toInt() ?: 1
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(requests)
            }
        
        awaitClose { listener.remove() }
    }
    
    override suspend fun acceptRideRequest(requestId: String): Result<RideAcceptance> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            // Check rate limiting
            val rateLimitResult = securityService.checkRateLimit(userId, "accept_ride", 3, 60000)
            if (rateLimitResult.isFailure) {
                return Result.Error(AppError.RateLimitError.TooManyRequests("Too many ride acceptance attempts"))
            }
            
            // Get request details
            val requestDoc = firestore.collection(RIDE_REQUESTS_COLLECTION)
                .document(requestId)
                .get()
                .await()
            
            if (!requestDoc.exists()) {
                return Result.Error(AppError.ValidationError.ResourceNotFound("Ride request not found"))
            }
            
            val currentStatus = requestDoc.getString("status")
            if (currentStatus != "PENDING") {
                return Result.Error(AppError.ValidationError.OperationNotAllowed("Ride request is no longer available"))
            }
            
            // Update request with driver assignment
            val rideData = mapOf(
                "assignedDriverId" to userId,
                "status" to "ACCEPTED",
                "acceptedAt" to System.currentTimeMillis()
            )
            
            firestore.collection(RIDE_REQUESTS_COLLECTION)
                .document(requestId)
                .update(rideData)
                .await()
            
            // Create active ride
            val activeRideData = mapOf(
                "id" to requestId,
                "driverId" to userId,
                "passengerId" to requestDoc.getString("passengerId"),
                "passengerName" to requestDoc.getString("passengerName"),
                "passengerPhone" to requestDoc.getString("passengerPhone"),
                "pickupLocation" to requestDoc.get("pickupLocation"),
                "destination" to requestDoc.get("destination"),
                "status" to RideStatus.ACCEPTED.name,
                "acceptedAt" to System.currentTimeMillis(),
                "fare" to requestDoc.getDouble("suggestedFare")
            )
            
            firestore.collection(ACTIVE_RIDES_COLLECTION)
                .document(requestId)
                .set(activeRideData)
                .await()
            
            val acceptance = RideAcceptance(
                rideId = requestId,
                estimatedArrival = calculateEstimatedArrival(requestDoc),
                driverLocation = getCurrentDriverLocation(userId),
                passengerContact = requestDoc.getString("passengerPhone") ?: ""
            )
            
            analyticsService.trackEvent("ride_request_accepted", mapOf(
                "ride_id" to requestId,
                "estimated_arrival" to acceptance.estimatedArrival.toString()
            ))
            
            Result.Success(acceptance)
        } catch (e: Exception) {
            analyticsService.logError(e, "ride_accept_failed")
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to accept ride"))
        }
    }
    
    override suspend fun rejectRideRequest(requestId: String, reason: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            // Log rejection for analytics
            firestore.collection("ride_rejections")
                .add(mapOf(
                    "requestId" to requestId,
                    "driverId" to userId,
                    "reason" to reason,
                    "timestamp" to System.currentTimeMillis()
                ))
                .await()
            
            analyticsService.trackEvent("ride_request_rejected", mapOf(
                "reason" to reason
            ))
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to reject ride"))
        }
    }
    
    override fun getActiveRide(): StateFlow<ActiveRide?> = _activeRide.asStateFlow()
    
    override suspend fun startRide(rideId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            firestore.collection(ACTIVE_RIDES_COLLECTION)
                .document(rideId)
                .update(mapOf(
                    "status" to RideStatus.IN_PROGRESS.name,
                    "startTime" to System.currentTimeMillis()
                ))
                .await()
            
            analyticsService.trackEvent("ride_started", mapOf("ride_id" to rideId))
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to start ride"))
        }
    }
    
    override suspend fun updateRideProgress(location: Location, status: RideStatus): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            val activeRide = _activeRide.value ?: return Result.Error(AppError.ValidationError.OperationNotAllowed("No active ride"))
            
            firestore.collection(ACTIVE_RIDES_COLLECTION)
                .document(activeRide.id)
                .update(mapOf(
                    "status" to status.name,
                    "currentLocation" to mapOf(
                        "latitude" to location.latitude,
                        "longitude" to location.longitude
                    ),
                    "lastUpdated" to System.currentTimeMillis()
                ))
                .await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to update ride progress"))
        }
    }
    
    override suspend fun completeRide(rideId: String, fareDetails: FareDetails): Result<RideCompletion> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            val completionTime = System.currentTimeMillis()
            val commission = fareDetails.totalFare * 0.15 // 15% commission
            val earnings = fareDetails.totalFare - commission
            
            // Update ride status
            firestore.collection(ACTIVE_RIDES_COLLECTION)
                .document(rideId)
                .update(mapOf(
                    "status" to RideStatus.COMPLETED.name,
                    "completedAt" to completionTime,
                    "fareDetails" to fareDetails,
                    "driverEarnings" to earnings
                ))
                .await()
            
            // Update driver earnings
            updateDriverEarnings(userId, earnings, fareDetails)
            
            _activeRide.value = null
            
            val completion = RideCompletion(
                rideId = rideId,
                completionTime = completionTime,
                finalFare = fareDetails.totalFare,
                distance = 0.0, // Would be calculated from GPS tracking
                duration = 0, // Would be calculated from start/end times
                earningsAfterCommission = earnings
            )
            
            analyticsService.trackEvent("ride_completed", mapOf(
                "ride_id" to rideId,
                "fare" to fareDetails.totalFare.toString(),
                "earnings" to earnings.toString()
            ))
            
            Result.Success(completion)
        } catch (e: Exception) {
            analyticsService.logError(e, "ride_complete_failed")
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to complete ride"))
        }
    }
    
    override suspend fun cancelRide(rideId: String, reason: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            firestore.collection(ACTIVE_RIDES_COLLECTION)
                .document(rideId)
                .update(mapOf(
                    "status" to RideStatus.CANCELLED.name,
                    "cancelledAt" to System.currentTimeMillis(),
                    "cancellationReason" to reason,
                    "cancelledBy" to "DRIVER"
                ))
                .await()
            
            _activeRide.value = null
            
            analyticsService.trackEvent("ride_cancelled_by_driver", mapOf(
                "ride_id" to rideId,
                "reason" to reason
            ))
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to cancel ride"))
        }
    }
    
    override fun getDriverStats(): Flow<DriverStats> = flow {
        val userId = auth.currentUser?.uid ?: return@flow
        
        try {
            val statsDoc = firestore.collection(DRIVER_STATS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (statsDoc.exists()) {
                val stats = DriverStats(
                    totalRides = statsDoc.getLong("totalRides")?.toInt() ?: 0,
                    totalEarnings = statsDoc.getDouble("totalEarnings") ?: 0.0,
                    averageRating = statsDoc.getDouble("averageRating")?.toFloat() ?: 0f,
                    completionRate = statsDoc.getDouble("completionRate")?.toFloat() ?: 0f,
                    acceptanceRate = statsDoc.getDouble("acceptanceRate")?.toFloat() ?: 0f,
                    totalHoursOnline = statsDoc.getLong("totalHoursOnline")?.toInt() ?: 0,
                    totalDistance = statsDoc.getDouble("totalDistance") ?: 0.0,
                    currentStreak = statsDoc.getLong("currentStreak")?.toInt() ?: 0,
                    badgesEarned = emptyList() // Would be populated from separate collection
                )
                emit(stats)
            }
        } catch (e: Exception) {
            analyticsService.logError(e, "get_driver_stats_failed")
        }
    }
    
    override fun getDailyEarnings(): Flow<EarningsData> = getEarningsForPeriod("daily")
    
    override fun getWeeklyEarnings(): Flow<EarningsData> = getEarningsForPeriod("weekly")
    
    override suspend fun updateVehicleInfo(vehicleInfo: VehicleInfo): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            firestore.collection(DRIVERS_COLLECTION)
                .document(userId)
                .update("vehicleInfo", vehicleInfo)
                .await()
            
            analyticsService.trackEvent("vehicle_info_updated", mapOf(
                "make" to vehicleInfo.make,
                "model" to vehicleInfo.model
            ))
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to update vehicle info"))
        }
    }
    
    override suspend fun setAvailabilityZone(zone: AvailabilityZone): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            firestore.collection(DRIVERS_COLLECTION)
                .document(userId)
                .update("availabilityZone", zone)
                .await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to set availability zone"))
        }
    }
    
    override suspend fun updateDriverLocation(location: Location): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            firestore.collection(DRIVERS_COLLECTION)
                .document(userId)
                .update(mapOf(
                    "currentLocation" to mapOf(
                        "latitude" to location.latitude,
                        "longitude" to location.longitude,
                        "timestamp" to System.currentTimeMillis()
                    )
                ))
                .await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to update location"))
        }
    }
    
    override suspend fun ratePassenger(rideId: String, rating: Float, feedback: String?): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            firestore.collection("passenger_ratings")
                .add(mapOf(
                    "rideId" to rideId,
                    "driverId" to userId,
                    "rating" to rating,
                    "feedback" to feedback,
                    "timestamp" to System.currentTimeMillis()
                ))
                .await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to rate passenger"))
        }
    }
    
    override fun getDriverRating(): Flow<DriverRating> = flow {
        val userId = auth.currentUser?.uid ?: return@flow
        
        try {
            val ratingsSnapshot = firestore.collection("driver_ratings")
                .whereEqualTo("driverId", userId)
                .get()
                .await()
            
            val ratings = ratingsSnapshot.documents.mapNotNull { doc ->
                doc.getDouble("rating")?.toFloat()
            }
            
            if (ratings.isNotEmpty()) {
                val averageRating = ratings.average().toFloat()
                val ratingDistribution = ratings.groupBy { it.toInt() }
                    .mapValues { it.value.size }
                
                val driverRating = DriverRating(
                    averageRating = averageRating,
                    totalRatings = ratings.size,
                    ratingDistribution = ratingDistribution,
                    recentFeedback = emptyList() // Would be populated from recent ratings
                )
                emit(driverRating)
            }
        } catch (e: Exception) {
            analyticsService.logError(e, "get_driver_rating_failed")
        }
    }
    
    override suspend fun triggerEmergencyAlert(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            firestore.collection("emergency_alerts")
                .add(mapOf(
                    "driverId" to userId,
                    "timestamp" to System.currentTimeMillis(),
                    "location" to getCurrentDriverLocation(userId),
                    "status" to "ACTIVE"
                ))
                .await()
            
            analyticsService.trackEvent("emergency_alert_triggered", emptyMap())
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to trigger emergency alert"))
        }
    }
    
    override suspend fun reportSafetyIncident(incident: SafetyIncident): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            
            firestore.collection(SAFETY_INCIDENTS_COLLECTION)
                .add(mapOf(
                    "reporterId" to userId,
                    "type" to incident.type.name,
                    "description" to incident.description,
                    "location" to incident.location,
                    "timestamp" to incident.timestamp,
                    "severity" to incident.severity.name,
                    "involvedPassengerId" to incident.involvedPassengerId
                ))
                .await()
            
            analyticsService.trackEvent("safety_incident_reported", mapOf(
                "type" to incident.type.name,
                "severity" to incident.severity.name
            ))
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to report incident"))
        }
    }
    
    // Private helper methods
    
    private suspend fun validateDriverDetails(details: DriverDetails): Result<Unit> {
        // Validate license number format
        if (!securityService.validateInput(details.licenseNumber, "license")) {
            return Result.Error(AppError.ValidationError.InvalidInput("Invalid license number format"))
        }
        
        // Check license expiry
        if (details.licenseExpiryDate < System.currentTimeMillis()) {
            return Result.Error(AppError.ValidationError.InvalidInput("License has expired"))
        }
        
        // Validate vehicle info
        if (details.vehicleInfo.capacity < 1 || details.vehicleInfo.capacity > 8) {
            return Result.Error(AppError.ValidationError.InvalidInput("Invalid vehicle capacity"))
        }
        
        return Result.Success(Unit)
    }
    
    private fun startListeningForRideRequests() {
        val userId = auth.currentUser?.uid ?: return
        
        rideRequestsListener = firestore.collection(ACTIVE_RIDES_COLLECTION)
            .whereEqualTo("driverId", userId)
            .whereIn("status", listOf(RideStatus.ACCEPTED.name, RideStatus.IN_PROGRESS.name))
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                
                val activeRide = snapshot?.documents?.firstOrNull()?.let { doc ->
                    ActiveRide(
                        id = doc.id,
                        passengerId = doc.getString("passengerId") ?: "",
                        passengerName = doc.getString("passengerName") ?: "",
                        passengerPhone = doc.getString("passengerPhone") ?: "",
                        pickupLocation = doc.get("pickupLocation") as? LocationData ?: LocationData(0.0, 0.0, ""),
                        destination = doc.get("destination") as? LocationData ?: LocationData(0.0, 0.0, ""),
                        status = RideStatus.valueOf(doc.getString("status") ?: "ACCEPTED"),
                        startTime = doc.getLong("startTime"),
                        estimatedCompletion = doc.getLong("estimatedCompletion"),
                        fare = doc.getDouble("fare") ?: 0.0
                    )
                }
                
                _activeRide.value = activeRide
            }
    }
    
    private fun stopListeningForRideRequests() {
        rideRequestsListener?.remove()
        activeRideListener?.remove()
    }
    
    private fun calculateEstimatedArrival(requestDoc: com.google.firebase.firestore.DocumentSnapshot): Int {
        // Would use actual location and routing API
        return 10 // placeholder: 10 minutes
    }
    
    private suspend fun getCurrentDriverLocation(userId: String): LocationData {
        // Would get from location service
        return LocationData(0.0, 0.0, "Current Location")
    }
    
    private suspend fun updateDriverEarnings(userId: String, earnings: Double, fareDetails: FareDetails) {
        try {
            val earningsDoc = firestore.collection(EARNINGS_COLLECTION)
                .document("${userId}_${getCurrentDateString()}")
            
            earningsDoc.get().await().let { doc ->
                if (doc.exists()) {
                    val currentEarnings = doc.getDouble("totalEarnings") ?: 0.0
                    val currentRides = doc.getLong("totalRides") ?: 0L
                    
                    earningsDoc.update(mapOf(
                        "totalEarnings" to (currentEarnings + earnings),
                        "totalRides" to (currentRides + 1),
                        "lastUpdated" to System.currentTimeMillis()
                    )).await()
                } else {
                    earningsDoc.set(mapOf(
                        "driverId" to userId,
                        "date" to getCurrentDateString(),
                        "totalEarnings" to earnings,
                        "totalRides" to 1,
                        "lastUpdated" to System.currentTimeMillis()
                    )).await()
                }
            }
        } catch (e: Exception) {
            analyticsService.logError(e, "update_earnings_failed")
        }
    }
    
    private fun getEarningsForPeriod(period: String): Flow<EarningsData> = flow {
        val userId = auth.currentUser?.uid ?: return@flow
        
        try {
            val query = when (period) {
                "daily" -> firestore.collection(EARNINGS_COLLECTION)
                    .whereEqualTo("driverId", userId)
                    .whereEqualTo("date", getCurrentDateString())
                "weekly" -> firestore.collection(EARNINGS_COLLECTION)
                    .whereEqualTo("driverId", userId)
                    .whereGreaterThanOrEqualTo("date", getWeekStartDate())
                else -> return@flow
            }
            
            val snapshot = query.get().await()
            val totalEarnings = snapshot.documents.sumOf { it.getDouble("totalEarnings") ?: 0.0 }
            val totalRides = snapshot.documents.sumOf { it.getLong("totalRides") ?: 0L }.toInt()
            val commission = totalEarnings * 0.15
            
            val earningsData = EarningsData(
                period = period,
                totalEarnings = totalEarnings,
                commission = commission,
                netEarnings = totalEarnings - commission,
                totalRides = totalRides,
                averageEarningsPerRide = if (totalRides > 0) totalEarnings / totalRides else 0.0,
                peakHourEarnings = 0.0, // Would calculate from hourly breakdown
                bonuses = 0.0,
                tips = 0.0
            )
            
            emit(earningsData)
        } catch (e: Exception) {
            analyticsService.logError(e, "get_earnings_failed")
        }
    }
    
    private fun getCurrentDateString(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
    
    private fun getWeekStartDate(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(calendar.time)
    }
}
