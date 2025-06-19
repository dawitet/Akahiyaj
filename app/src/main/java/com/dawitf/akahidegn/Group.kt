package com.dawitf.akahidegn // Make sure this matches your package name

import android.util.Log
import androidx.compose.runtime.Stable
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

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
    var creatorName: String? = null, // Added creator name field
    var creatorCloudflareId: String? = null,
    var destinationName: String? = null,
    var originalDestination: String? = null, // Store original destination without time/creator info
    var pickupLat: Double? = null,
    var pickupLng: Double? = null,
    var timestamp: Long? = null,
    var maxMembers: Int = 4,
    var members: HashMap<String, Boolean> = HashMap(),
    var memberDetails: HashMap<String, MemberInfo> = HashMap(), // Store member details including phone
    var memberCount: Int = 0,
    var imageUrl: String? = null // Added missing imageUrl field
) {
    constructor() : this(
        groupId = null,
        creatorId = null,
        creatorName = null,
        creatorCloudflareId = null,
        destinationName = null,
        originalDestination = null,
        pickupLat = null,
        pickupLng = null,
        timestamp = null,
        maxMembers = 4,
        members = HashMap<String, Boolean>(),
        memberDetails = HashMap<String, MemberInfo>(),
        memberCount = 0,
        imageUrl = null
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
        map["timestamp"] = timestamp ?: System.currentTimeMillis()  // Add timestamp field for Group class
        map["createdBy"] = creatorId ?: ""  // Must match auth.uid
        
        // SIMPLIFIED MEMBERS STRUCTURE - this works with our updated rules
        map["members"] = members.filter { it.value }.keys.associateWith { true }
        
        // Store member details separately for phone access
        map["memberDetails"] = memberDetails.mapValues { (_, memberInfo) ->
            mapOf(
                "name" to memberInfo.name,
                "phone" to memberInfo.phone,
                "avatar" to memberInfo.avatar,
                "joinedAt" to memberInfo.joinedAt
            )
        }
        
        // Additional fields - these are allowed by our rules
        map["pickupLat"] = pickupLat ?: 0.0
        map["pickupLng"] = pickupLng ?: 0.0
        map["maxMembers"] = maxMembers
        map["memberCount"] = memberCount
        map["imageUrl"] = imageUrl ?: ""
        map["creatorName"] = creatorName ?: ""
        map["originalDestination"] = originalDestination ?: destinationName ?: ""
        
        // Log validation fields to help diagnose rule failures
        Log.d("FIREBASE_DEBUG", "Security validation fields: id=${map["id"]}, createdBy=${map["createdBy"]}")
        Log.d("FIREBASE_DEBUG", "Complete map being written to Firebase: $map")
        
        return map
        
        return map
    }
}