package com.dawitf.akahidegn.features.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import com.dawitf.akahidegn.analytics.AnalyticsService
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.core.result.Result
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.math.abs

/**
 * Service to handle device consistency issues including location timing and accuracy
 * Addresses GPS drift, device clock synchronization, and location update reliability
 */
@Singleton
class DeviceConsistencyService @Inject constructor(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
    private val analyticsService: AnalyticsService
) {
    
    companion object {
        private const val TAG = "DeviceConsistencyService"
        private const val LOCATION_UPDATE_TIMEOUT_MS = 15_000L
        private const val MAX_LOCATION_AGE_MS = 60_000L // 1 minute
        private const val LOCATION_ACCURACY_THRESHOLD = 100f // meters
        private const val MIN_DISTANCE_CHANGE = 10f // meters
        private const val TIME_SYNC_THRESHOLD_MS = 5_000L // 5 seconds
        private const val RETRY_DELAY_MS = 2_000L
        private const val MAX_RETRIES = 3
    }

    /**
     * Gets a reliable and consistent location with validation
     */
    suspend fun getReliableLocation(): Result<Location> {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            
            // Check if location services are enabled
            if (!isLocationEnabled(locationManager)) {
                return Result.Error(AppError.LocationError.LocationDisabled)
            }

            var attempts = 0
            var bestLocation: Location? = null

            while (attempts < MAX_RETRIES) {
                attempts++
                
                try {
                    val location = withTimeout(LOCATION_UPDATE_TIMEOUT_MS) {
                        getCurrentLocationWithRetry(locationManager)
                    }
                    
                    if (location != null && isLocationReliable(location)) {
                        bestLocation = selectBestLocation(bestLocation, location)
                        
                        // If we have a good enough location, use it
                        if (bestLocation?.accuracy != null && bestLocation.accuracy <= LOCATION_ACCURACY_THRESHOLD) {
                            break
                        }
                    }
                    
                    if (attempts < MAX_RETRIES) {
                        delay(RETRY_DELAY_MS)
                    }
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Location attempt $attempts failed", e)
                    if (attempts < MAX_RETRIES) {
                        delay(RETRY_DELAY_MS)
                    }
                }
            }

            if (bestLocation != null) {
                // Validate location against known issues
                val validatedLocation = validateLocationConsistency(bestLocation)
                
                analyticsService.trackEvent("reliable_location_obtained", mapOf(
                    "accuracy" to bestLocation.accuracy as Any,
                    "attempts" to attempts as Any,
                    "age_ms" to (System.currentTimeMillis() - bestLocation.time) as Any,
                    "provider" to (bestLocation.provider ?: "unknown") as Any
                ))
                
                Result.Success(validatedLocation)
            } else {
                analyticsService.trackEvent("reliable_location_failed", mapOf(
                    "attempts" to attempts,
                    "reason" to "no_reliable_location_obtained"
                ))
                Result.Error(AppError.LocationError.LocationUnavailable)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get reliable location", e)
            analyticsService.trackEvent("reliable_location_error", mapOf(
                "error" to e.message.orEmpty()
            ))
            Result.Error(AppError.LocationError.LocationUnavailable)
        }
    }

    /**
     * Synchronizes device time with server time to prevent timing issues
     */
    suspend fun synchronizeDeviceTime(): Result<Long> {
        return try {
            val deviceTime = System.currentTimeMillis()
            val serverTimeRef = database.reference.child(".info/serverTimeOffset")
            val snapshot = serverTimeRef.get().await()
            
            val serverTimeOffset = snapshot.getValue(Long::class.java) ?: 0L
            val estimatedServerTime = deviceTime + serverTimeOffset
            
            val timeDifference = abs(deviceTime - estimatedServerTime)
            
            if (timeDifference > TIME_SYNC_THRESHOLD_MS) {
                Log.w(TAG, "Significant time difference detected: ${timeDifference}ms")
                analyticsService.trackEvent("time_sync_warning", mapOf(
                    "device_time" to deviceTime,
                    "server_time" to estimatedServerTime,
                    "difference_ms" to timeDifference
                ))
            }

            analyticsService.trackEvent("time_sync_completed", mapOf(
                "server_offset" to serverTimeOffset,
                "time_difference" to timeDifference
            ))

            Result.Success(estimatedServerTime)
            
        } catch (e: Exception) {
            Log.e(TAG, "Time synchronization failed", e)
            Result.Error(AppError.NetworkError.RequestFailed("Time sync failed: ${e.message}"))
        }
    }

    /**
     * Updates location with consistency checks and validation
     */
    suspend fun updateLocationWithConsistency(
        groupId: String,
        location: Location
    ): Result<Unit> {
        return try {
            val currentUser = auth.currentUser 
                ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)

            // Validate location before updating
            val validatedLocation = validateLocationConsistency(location)
            
            // Get synchronized time
            val serverTimeResult = synchronizeDeviceTime()
            val updateTime = when (serverTimeResult) {
                is Result.Success -> serverTimeResult.data
                is Result.Error -> System.currentTimeMillis() // Fallback to device time
                is Result.Loading -> System.currentTimeMillis() // Fallback while loading
            }

            // Check for rapid location changes (potential GPS jumping)
            val isLocationChangeValid = validateLocationChange(groupId, validatedLocation)
            if (!isLocationChangeValid) {
                Log.w(TAG, "Suspicious location change detected, skipping update")
                analyticsService.trackEvent("location_update_rejected", mapOf(
                    "reason" to "suspicious_location_change",
                    "group_id" to groupId
                ))
                return Result.Error(AppError.LocationError.InvalidLocation)
            }

            // Update Firebase with validated data
            val locationRef = database.reference
                .child("groups")
                .child(groupId)
                .child("members")
                .child(currentUser.uid)

            val locationData = mapOf(
                "latitude" to validatedLocation.latitude,
                "longitude" to validatedLocation.longitude,
                "accuracy" to validatedLocation.accuracy,
                "timestamp" to updateTime,
                "provider" to validatedLocation.provider,
                "deviceTime" to System.currentTimeMillis(),
                "bootTime" to SystemClock.elapsedRealtime()
            )

            locationRef.updateChildren(locationData).await()

            Log.d(TAG, "Location updated successfully with consistency checks")
            analyticsService.trackEvent("location_updated_with_consistency", mapOf(
                "group_id" to groupId as Any,
                "accuracy" to validatedLocation.accuracy as Any,
                "provider" to (validatedLocation.provider ?: "unknown") as Any,
                "time_diff" to abs(updateTime - System.currentTimeMillis()) as Any
            ))

            Result.Success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update location with consistency", e)
            Result.Error(AppError.NetworkError.RequestFailed("Location update failed: ${e.message}"))
        }
    }

    /**
     * Validates location consistency and filters out obvious errors
     */
    private fun validateLocationConsistency(location: Location): Location {
        // Create a copy to modify if needed
        val validatedLocation = Location(location)
        
        // Check for obviously wrong coordinates (like null island 0,0)
        if (abs(location.latitude) < 0.001 && abs(location.longitude) < 0.001) {
            Log.w(TAG, "Null Island coordinates detected, marking as invalid")
            validatedLocation.accuracy = Float.MAX_VALUE
        }
        
        // Check for impossible accuracy values
        if (location.accuracy <= 0 || location.accuracy > 10000) {
            Log.w(TAG, "Invalid accuracy detected: ${location.accuracy}")
            validatedLocation.accuracy = LOCATION_ACCURACY_THRESHOLD * 2
        }
        
        // Check location age
        val locationAge = System.currentTimeMillis() - location.time
        if (locationAge > MAX_LOCATION_AGE_MS) {
            Log.w(TAG, "Old location detected, age: ${locationAge}ms")
        }
        
        return validatedLocation
    }

    /**
     * Validates if a location change is reasonable (not GPS jumping)
     */
    private suspend fun validateLocationChange(groupId: String, newLocation: Location): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false
            
            val lastLocationRef = database.reference
                .child("groups")
                .child(groupId)
                .child("members")
                .child(currentUser.uid)
            
            val snapshot = lastLocationRef.get().await()
            
            if (snapshot.exists()) {
                val lastLat = snapshot.child("latitude").getValue(Double::class.java) ?: return true
                val lastLon = snapshot.child("longitude").getValue(Double::class.java) ?: return true
                val lastTimestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: return true
                
                val lastLocation = Location("last").apply {
                    latitude = lastLat
                    longitude = lastLon
                    time = lastTimestamp
                }
                
                val distance = newLocation.distanceTo(lastLocation)
                val timeDiff = newLocation.time - lastLocation.time
                
                // If moved more than 1km in less than 10 seconds, it's suspicious
                if (distance > 1000 && timeDiff < 10_000) {
                    Log.w(TAG, "Suspicious location jump: ${distance}m in ${timeDiff}ms")
                    return false
                }
                
                // If distance is very small, don't update unless significant time has passed
                if (distance < MIN_DISTANCE_CHANGE && timeDiff < 30_000) {
                    return false
                }
            }
            
            true
            
        } catch (e: Exception) {
            Log.w(TAG, "Location validation failed, allowing update", e)
            true // Allow update if validation fails
        }
    }

    /**
     * Selects the best location from two options
     */
    private fun selectBestLocation(location1: Location?, location2: Location?): Location? {
        if (location1 == null) return location2
        if (location2 == null) return location1
        
        // Prefer more recent location
        if (location2.time - location1.time > 30_000) { // 30 seconds
            return location2
        }
        
        // Prefer more accurate location
        if (location2.accuracy < location1.accuracy) {
            return location2
        }
        
        return location1
    }

    /**
     * Checks if location is reliable based on various criteria
     */
    private fun isLocationReliable(location: Location): Boolean {
        // Check accuracy
        if (location.accuracy > LOCATION_ACCURACY_THRESHOLD * 2) {
            return false
        }
        
        // Check age
        val age = System.currentTimeMillis() - location.time
        if (age > MAX_LOCATION_AGE_MS) {
            return false
        }
        
        // Check for valid coordinates
        if (abs(location.latitude) > 90 || abs(location.longitude) > 180) {
            return false
        }
        
        return true
    }

    /**
     * Gets current location with built-in retry mechanism
     */
    private suspend fun getCurrentLocationWithRetry(locationManager: LocationManager): Location? {
        // This is a simplified version - in real implementation, you'd use
        // LocationManager.getCurrentLocation() or FusedLocationProviderClient
        // For now, return the last known location with some validation
        
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null
        
        for (provider in providers) {
            try {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null && isLocationReliable(location)) {
                    bestLocation = selectBestLocation(bestLocation, location)
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "No permission for provider: $provider")
            }
        }
        
        return bestLocation
    }

    /**
     * Checks if location services are enabled
     */
    private fun isLocationEnabled(locationManager: LocationManager): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}
