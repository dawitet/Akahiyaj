package com.dawitf.akahidegn.fixed

import android.util.Log
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

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
    var imageUrl: String? = null
) {
    constructor() : this(
        null, null, null, null,
        null, null, null, 4,
        HashMap(), 0, null
    )

    @Exclude
    fun toMap(): Map<String, Any?> {
        val map = HashMap<String, Any?>()
        
        map["id"] = groupId ?: ""
        map["from"] = "Current Location"
        map["to"] = destinationName ?: ""
        map["departureTime"] = timestamp?.toString() ?: System.currentTimeMillis().toString()
        map["availableSeats"] = (maxMembers - memberCount).coerceIn(1, 8)
        map["pricePerPerson"] = 0
        map["createdAt"] = timestamp ?: System.currentTimeMillis()
        map["createdBy"] = creatorId ?: ""
        
        map["members"] = members.filter { it.value }.keys.associateWith { true }
        
        map["pickupLat"] = pickupLat ?: 0.0
        map["pickupLng"] = pickupLng ?: 0.0
        map["maxMembers"] = maxMembers
        map["memberCount"] = memberCount
        map["imageUrl"] = imageUrl ?: ""
        
        return map
    }
}
