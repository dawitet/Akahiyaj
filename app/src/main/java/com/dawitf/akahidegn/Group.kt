package com.dawitf.akahidegn // Make sure this matches your package name

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
        return mapOf(
            "creatorId" to creatorId,
            "creatorCloudflareId" to creatorCloudflareId,
            "destinationName" to destinationName,
            "pickupLat" to pickupLat,
            "pickupLng" to pickupLng,
            "timestamp" to timestamp,
            "maxMembers" to maxMembers,
            "members" to members,
            "memberCount" to memberCount,
            "imageUrl" to imageUrl
        )
    }
}