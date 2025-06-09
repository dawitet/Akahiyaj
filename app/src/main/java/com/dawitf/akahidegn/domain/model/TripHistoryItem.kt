package com.dawitf.akahidegn.domain.model

import com.dawitf.akahidegn.features.profile.PaymentMethod
import com.dawitf.akahidegn.features.profile.RideStatus
import com.dawitf.akahidegn.features.profile.RideType

/**
 * Data class representing a trip/ride history item
 */
data class TripHistoryItem(
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
