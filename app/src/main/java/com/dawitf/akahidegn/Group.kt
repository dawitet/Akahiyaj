package com.dawitf.akahidegn // Make sure this matches your package name

import android.util.Log
import androidx.compose.runtime.Stable
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@Stable
@IgnoreExtraProperties
data class Group(
    @get:Exclude var groupId: String? = null,
    var creatorId: String? = null,
    var creatorCloudflareId: String? = null,
    var destinationName: String? = null,
    var pickupLat: Double? = null,
    var pickupLng: Double? = null,
    var timestamp: Long? = null,
    var maxMembers: Int = 4,
    var members: HashMap<String, Boolean> = HashMap(),
    var memberCount: Int = 0,
    var imageUrl: String? = null // Added missing imageUrl field
) {
    constructor() : this(
        null, null, null, null,
        null, null, null, 4,
        HashMap(), 0, null
    )

    @Exclude
    fun toMap(): Map<String, Any?> {
        // Initialize a map with only the fields allowed by the security rules
        val map = HashMap<String, Any?>()
        
        // Debug the Group object values with extra verbosity for rule debugging
        Log.d("GROUP_DEBUG", "Converting Group to Map: id=${groupId}, creator=${creatorId}, destination=${destinationName}")
        
        // SECURITY RULES REQUIRED FIELDS - these must match exactly what worked in CLI tests
        // These fields are required by the .validate rule
        map["id"] = groupId ?: ""  // Must match the $groupId in the path
        map["from"] = "Current Location"  // Must be non-empty string
        map["to"] = destinationName ?: ""  // Must be non-empty string
        map["departureTime"] = timestamp?.toString() ?: System.currentTimeMillis().toString()  // Must be non-empty string
        map["availableSeats"] = (maxMembers - memberCount).coerceIn(1, 8)  // Must be between 1 and 8
        map["pricePerPerson"] = 0  // Must be >= 0
        map["createdAt"] = timestamp ?: System.currentTimeMillis()  // Must be <= now
        map["createdBy"] = creatorId ?: ""  // Must match auth.uid
        
        // SIMPLIFIED MEMBERS STRUCTURE - this works with our updated rules
        map["members"] = members.filter { it.value }.keys.associateWith { true }
        
        // Additional fields - these are allowed by our rules
        map["pickupLat"] = pickupLat ?: 0.0
        map["pickupLng"] = pickupLng ?: 0.0
        map["maxMembers"] = maxMembers
        map["memberCount"] = memberCount
        map["imageUrl"] = imageUrl ?: ""
        
        // Log validation fields to help diagnose rule failures
        Log.d("FIREBASE_DEBUG", "Security validation fields: id=${map["id"]}, createdBy=${map["createdBy"]}")
        Log.d("FIREBASE_DEBUG", "Complete map being written to Firebase: $map")
        
        return map
        
        return map
    }
}