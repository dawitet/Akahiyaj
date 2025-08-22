package com.dawitf.akahidegn.ui.components

import com.dawitf.akahidegn.domain.model.Group
import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension properties for Group model to provide compatibility with UI components
 */

// Provide missing properties for UI compatibility
val Group.id: String get() = groupId ?: ""
val Group.name: String get() = "ቡድን ${groupId?.take(6) ?: "ኣዲስ"}"
val Group.destination: String get() = destinationName ?: "ያልተወሰነ መድረሻ"
val Group.currentMembers: Int get() = memberCount
val Group.departureTime: String get() {
    return if (timestamp != null) {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        format.format(Date(timestamp!!))
    } else {
        "ያልተወሰነ ጊዜ"
    }
}

// Mock properties for advanced features (can be extended with real data later)

val Group.rating: Float get() = 4.2f // Mock rating
val Group.isPremium: Boolean get() = false // Mock premium status
val Group.isWomenOnly: Boolean get() = false // Mock women only status
val Group.meetingPoint: String get() = "መሰብሰቢያ ቦታ" // Mock meeting point

// Utility functions
fun Group.hasAvailableSpots(): Boolean = memberCount < maxMembers
fun Group.isDepartingSoon(): Boolean {
    return timestamp?.let { 
        val now = System.currentTimeMillis()
        val timeDiff = it - now
        timeDiff in 0..1800000 // Departing within 30 minutes
    } ?: false
}

fun Group.getCapacityPercentage(): Float {
    return if (maxMembers > 0) {
        memberCount.toFloat() / maxMembers.toFloat()
    } else 0f
}
