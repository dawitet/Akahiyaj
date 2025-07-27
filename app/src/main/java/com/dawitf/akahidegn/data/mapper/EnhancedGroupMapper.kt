package com.dawitf.akahidegn.data.mapper

import com.dawitf.akahidegn.data.local.entity.GroupEntityEnhanced
import com.dawitf.akahidegn.Group

fun Group.toEnhancedEntity(): GroupEntityEnhanced {
    return GroupEntityEnhanced(
        groupId = this.groupId ?: "",
        creatorId = this.creatorId,
        creatorCloudflareId = this.creatorCloudflareId,
        destinationName = this.destinationName,
        pickupLat = this.pickupLat,
        pickupLng = this.pickupLng,
        timestamp = this.timestamp,
        maxMembers = this.maxMembers,
        memberCount = this.memberCount,
        imageUrl = this.imageUrl,
        
        departureTime = this.timestamp,
        availableSeats = maxOf(0, this.maxMembers - this.memberCount),
        description = "",
        contactInfo = "",
        vehicleType = "",
        route = null,
        tags = null,
        rating = 0f,
        reviewCount = 0,
        isActive = true,
        lastUpdated = System.currentTimeMillis(),
        distanceFromUser = null,
        popularityScore = 0f
    )
}

fun GroupEntityEnhanced.toDomainModel(): Group {
    val membersMap = HashMap<String, Boolean>()
    // If we have members stored, convert them to the map format
    // Otherwise leave the HashMap empty
    
    return Group(
        groupId = this.groupId,
        creatorId = this.creatorId,
        creatorCloudflareId = this.creatorCloudflareId,
        destinationName = this.destinationName,
        pickupLat = this.pickupLat,
        pickupLng = this.pickupLng,
        timestamp = this.timestamp,
        maxMembers = this.maxMembers,
        members = membersMap,
        memberCount = this.memberCount,
        imageUrl = this.imageUrl
    )
}
