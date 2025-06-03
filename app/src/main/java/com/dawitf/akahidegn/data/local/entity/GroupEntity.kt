package com.dawitf.akahidegn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val destination: String,
    val date: String,
    val time: String,
    val creatorUserId: String,
    val latitude: Double,
    val longitude: Double,
    val memberCount: Int = 0,
    val maxMembers: Int = 4,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
