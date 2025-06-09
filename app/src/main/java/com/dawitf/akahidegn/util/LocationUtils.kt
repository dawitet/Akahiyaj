package com.dawitf.akahidegn.util

import kotlin.math.*

/**
 * Utility class for location-related calculations.
 */
object LocationUtils {
    
    private const val EARTH_RADIUS_KM = 6371.0
    
    /**
     * Calculate distance between two points using Haversine formula.
     * @param lat1 Latitude of first point
     * @param lng1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lng2 Longitude of second point
     * @return Distance in kilometers
     */
    fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return EARTH_RADIUS_KM * c
    }
    
    /**
     * Calculate the squared distance for efficient comparison (avoids sqrt calculation).
     * @param lat1 Latitude of first point
     * @param lng1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lng2 Longitude of second point
     * @return Squared distance (not in real units, only for comparison)
     */
    fun calculateDistanceSquared(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = lat2 - lat1
        val dLng = lng2 - lng1
        return dLat * dLat + dLng * dLng
    }
    
    /**
     * Convert kilometers to the squared distance units used in database queries.
     */
    fun kilometersToDistanceSquared(km: Double): Double {
        // Approximate conversion: 1 degree ≈ 111 km
        val degrees = km / 111.0
        return degrees * degrees
    }
    
    /**
     * Check if a point is within a circular area.
     */
    fun isWithinRadius(
        centerLat: Double, centerLng: Double,
        pointLat: Double, pointLng: Double,
        radiusKm: Double
    ): Boolean {
        val distance = calculateDistance(centerLat, centerLng, pointLat, pointLng)
        return distance <= radiusKm
    }
    
    /**
     * Format distance for display.
     */
    fun formatDistance(distanceKm: Double): String {
        return when {
            distanceKm < 1.0 -> "${(distanceKm * 1000).roundToInt()}m"
            distanceKm < 10.0 -> "${"%.1f".format(distanceKm)}km"
            else -> "${distanceKm.roundToInt()}km"
        }
    }
    
    /**
     * Get bearing between two points.
     */
    fun getBearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLng = Math.toRadians(lng2 - lng1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        
        val y = sin(dLng) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLng)
        
        return Math.toDegrees(atan2(y, x))
    }
    
    /**
     * Get compass direction from bearing.
     */
    fun getCompassDirection(bearing: Double): String {
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((bearing + 22.5) / 45.0).toInt() % 8
        return directions[index]
    }
}
