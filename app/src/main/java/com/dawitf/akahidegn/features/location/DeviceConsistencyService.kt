package com.dawitf.akahidegn.features.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.dawitf.akahidegn.core.result.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceConsistencyService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val locationManager: LocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    suspend fun getReliableLocation(): Result<Location> {
        return try {
            // Simulate location validation logic
            val lastKnownLocation = try {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (e: SecurityException) {
                null
            }

            if (lastKnownLocation != null) {
                // Validate location accuracy and freshness
                val isLocationFresh = (System.currentTimeMillis() - lastKnownLocation.time) < 300000 // 5 minutes
                val isLocationAccurate = lastKnownLocation.accuracy < 100 // Within 100 meters

                if (isLocationFresh && isLocationAccurate) {
                    Result.Success(lastKnownLocation)
                } else {
                    Result.Error("Location not reliable - stale or inaccurate")
                }
            } else {
                Result.Error("No location available")
            }
        } catch (e: Exception) {
            Result.Error("Location service error: ${e.message}")
        }
    }

    fun validateLocationConsistency(location: Location): Boolean {
        // Basic location validation
        return location.accuracy < 200 && // Within 200 meters accuracy
               location.latitude != 0.0 &&
               location.longitude != 0.0 &&
               (System.currentTimeMillis() - location.time) < 600000 // Within 10 minutes
    }
}
