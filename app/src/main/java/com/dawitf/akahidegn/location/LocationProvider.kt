package com.dawitf.akahidegn.location

import android.location.Location

/**
 * A singleton provider for location data in the app
 */
object LocationProvider {
    // The last known user location, can be set by any component that has location access
    private var lastKnownLocation: Location? = null
    
    /**
     * Returns the last known user location or null if not available
     */
    fun getUserLocation(): Location? {
        return lastKnownLocation
    }
    
    /**
     * Sets the user's current location
     */
    fun setUserLocation(location: Location) {
        lastKnownLocation = location
    }
    
    /**
     * Clears the stored location
     */
    fun clearLocation() {
        lastKnownLocation = null
    }
}
