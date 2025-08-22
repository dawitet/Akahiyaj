package com.dawitf.akahidegn.domain.model

import android.util.Log
import androidx.compose.runtime.Stable
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import kotlin.math.*

@Stable
@IgnoreExtraProperties
data class MemberInfo(
    var name: String = "",
    var phone: String = "",
    var avatar: String = "avatar_1",
    var joinedAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "avatar_1", System.currentTimeMillis())
}

@Stable
@IgnoreExtraProperties
data class Group(
    @get:Exclude var groupId: String? = null,
    var creatorId: String? = null,
    var creatorName: String? = null,
    var creatorCloudflareId: String? = null,
    var destinationName: String? = null,
    var originalDestination: String? = null,
    var from: String? = null,
    var to: String? = null,
    var status: String? = "active",
    var pickupLat: Double? = null,
    var pickupLng: Double? = null,
    var timestamp: Long? = null,
    var expiresAt: Long? = null, // Group expires after 30 minutes
    var maxMembers: Int = 4,
    var members: HashMap<String, Boolean> = HashMap(),
    var memberDetails: HashMap<String, MemberInfo> = HashMap(),
    var memberCount: Int = 0,
    var imageUrl: String? = null
) {
    companion object {
        const val EXPIRATION_TIME_MINUTES = 30L
        const val PROXIMITY_RADIUS_METERS = 500.0
    }

    constructor() : this(
        groupId = null,
        creatorId = null,
        creatorName = null,
        creatorCloudflareId = null,
        destinationName = null,
        originalDestination = null,
        from = null,
        to = null,
        status = "active",
        pickupLat = null,
        pickupLng = null,
        timestamp = System.currentTimeMillis(),
        expiresAt = System.currentTimeMillis() + (EXPIRATION_TIME_MINUTES * 60 * 1000),
        maxMembers = 4,
        members = HashMap(),
        memberDetails = HashMap(),
        memberCount = 0,
        imageUrl = null
    )

    @Exclude
    fun isExpired(): Boolean {
        val now = System.currentTimeMillis()
        return expiresAt?.let { now > it } ?: false
    }

    @Exclude
    fun isWithinRadius(userLat: Double, userLng: Double, radiusMeters: Double = PROXIMITY_RADIUS_METERS): Boolean {
        return if (pickupLat != null && pickupLng != null) {
            calculateDistance(userLat, userLng, pickupLat!!, pickupLng!!) <= radiusMeters
        } else {
            false
        }
    }

    @Exclude
    fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    @Exclude
    fun getDistanceText(userLat: Double, userLng: Double): String {
        return if (pickupLat != null && pickupLng != null) {
            val distance = calculateDistance(userLat, userLng, pickupLat!!, pickupLng!!)
            when {
                distance < 1000 -> "${distance.toInt()}m away"
                else -> "${"%.1f".format(distance / 1000)}km away"
            }
        } else {
            "Location unknown"
        }
    }

    @Exclude
    fun getRemainingTime(): String {
        val now = System.currentTimeMillis()
        val remaining = (expiresAt ?: 0) - now
        return when {
            remaining <= 0 -> "Expired"
            remaining < 60000 -> "${(remaining / 1000).toInt()}s left"
            else -> "${(remaining / 60000).toInt()}m left"
        }
    }
}
