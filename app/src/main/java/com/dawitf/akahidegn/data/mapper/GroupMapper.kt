package com.dawitf.akahidegn.data.mapper

import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.data.local.entity.GroupEntity

fun GroupEntity.toDomainModel(): Group {
    return Group(
        groupId = id,
        creatorId = creatorUserId,
        creatorCloudflareId = "", // placeholder or from entity if stored
        destinationName = destination,
        pickupLat = latitude,
        pickupLng = longitude,
        timestamp = null, // or entity.createdAt
        maxMembers = maxMembers,
        members = HashMap(), // needs actual mapping if stored locally
        memberCount = memberCount,
        imageUrl = "",
        // Additional fields default
    )
}

fun Group.toEntity(): GroupEntity {
    return GroupEntity(
        id = groupId ?: "",
        name = "",
        description = "",
        destination = destinationName ?: "",
        date = "",
        time = "",
        creatorUserId = creatorId ?: "",
        latitude = pickupLat ?: 0.0,
        longitude = pickupLng ?: 0.0,
        memberCount = memberCount,
        maxMembers = maxMembers,
        isActive = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

// Extension for mapping list of entities to domain models
fun List<GroupEntity>.toDomainModels(): List<Group> = map { it.toDomainModel() }

fun List<Group>.toEntities(): List<GroupEntity> {
    return map { it.toEntity() }
}
