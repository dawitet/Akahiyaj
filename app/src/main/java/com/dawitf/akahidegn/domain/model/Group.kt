package com.dawitf.akahidegn.domain.model

data class Group(
    val groupId: String? = null,
    val creatorId: String = "",
    val creatorName: String = "",
    val destinationName: String? = null,
    val originalDestination: String? = null,
    val from: String? = null,
    val to: String? = null,
    val status: String = "active",
    val pickupLat: Double = 0.0,
    val pickupLng: Double = 0.0,
    val timestamp: Long? = null,
    val maxMembers: Int = 4,
    val members: Map<String, Boolean> = emptyMap(),
    val memberDetails: Map<String, MemberInfo> = emptyMap(),
    val memberCount: Int = 0,
    val imageUrl: String? = null
)

data class MemberInfo(
    val name: String = "",
    val phone: String = "",
    val avatar: String = "",
    val joinedAt: Long = System.currentTimeMillis()
)
