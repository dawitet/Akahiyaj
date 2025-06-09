package com.dawitf.akahidegn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String?,
    val gender: String?,
    val dateOfBirth: String?,
    val bio: String?,
    val profileImageUrl: String?,
    val isVerified: Boolean = false,
    val verificationLevel: String = "NONE",
    val isDriver: Boolean = false,
    val totalTrips: Int = 0,
    val averageRating: Float = 0f,
    val totalRatings: Int = 0,
    val memberSince: Long = System.currentTimeMillis(),
    val lastActive: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile_preferences")
data class UserProfilePreferencesEntity(
    @PrimaryKey val userId: String,
    val notificationsEnabled: Boolean = true,
    val locationSharingEnabled: Boolean = true,
    val autoJoinEnabled: Boolean = false,
    val preferredLanguage: String = "ENGLISH",
    val theme: String = "SYSTEM",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_achievements")
data class UserAchievementEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val achievementId: String,
    val title: String,
    val description: String,
    val iconUrl: String,
    val category: String,
    val rarity: String,
    val points: Int,
    val unlockedAt: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val isPrimary: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payment_methods")
data class PaymentMethodEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val provider: String,
    val accountNumber: String,
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
