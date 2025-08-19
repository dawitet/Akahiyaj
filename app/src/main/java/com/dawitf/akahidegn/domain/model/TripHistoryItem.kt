package com.dawitf.akahidegn.domain.model

data class TripHistoryItem(
    val tripId: String,
    val groupId: String,
    val destinationName: String,
    val memberCount: Int,
    val role: String, // "CREATOR" or "MEMBER"
    val status: String, // "ACTIVE", "COMPLETED", "CANCELLED"
    val timestamp: Long = System.currentTimeMillis(),
    val pickupLocation: String? = null,
    val dropoffLocation: String? = null
)
