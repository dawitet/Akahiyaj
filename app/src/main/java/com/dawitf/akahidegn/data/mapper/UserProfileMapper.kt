package com.dawitf.akahidegn.data.mapper

import com.dawitf.akahidegn.data.local.entity.*
import com.dawitf.akahidegn.domain.model.*

object UserProfileMapper {
    
    fun toEntity(domain: UserProfile): UserProfileEntity {
        // Split display name into first and last name (fallback approach)
        val nameParts = domain.displayName.trim().split(" ", limit = 2)
        val firstName = nameParts.firstOrNull() ?: ""
        val lastName = if (nameParts.size > 1) nameParts[1] else ""
        
        return UserProfileEntity(
            userId = domain.userId,
            firstName = firstName,
            lastName = lastName,
            email = domain.email ?: "",
            phoneNumber = domain.phoneNumber,
            gender = null, // Not available in domain model
            dateOfBirth = null, // Not available in domain model
            bio = domain.bio,
            profileImageUrl = domain.profilePictureUrl,
            isVerified = domain.isVerified,
            verificationLevel = "BASIC", // Default value since not in domain model
            isDriver = false, // Default value since not in domain model
            totalTrips = domain.totalTrips,
            averageRating = domain.rating,
            totalRatings = domain.reviewCount,
            memberSince = domain.joinDate,
            lastActive = domain.lastActiveDate,
            isOnline = domain.isActive,
            createdAt = domain.joinDate,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    fun toDomain(entity: UserProfileEntity): UserProfile {
        return UserProfile(
            userId = entity.userId,
            displayName = "${entity.firstName} ${entity.lastName}".trim(),
            profilePictureUrl = entity.profileImageUrl,
            email = entity.email,
            phoneNumber = entity.phoneNumber,
            isVerified = entity.isVerified,
            joinDate = entity.memberSince,
            lastActiveDate = entity.lastActive,
            rating = entity.averageRating,
            reviewCount = entity.totalRatings,
            totalTrips = entity.totalTrips,
            completedTrips = entity.totalTrips, // Assume all are completed for now
            cancelledTrips = 0,
            totalPassengers = 0,
            totalDistance = 0.0,
            
            bio = entity.bio ?: "",
            preferences = UserPreferences(),
            privacySettings = PrivacySettings(),
            isActive = entity.isOnline,
            accountStatus = AccountStatus.ACTIVE
        )
    }
    
    fun preferencesToEntity(domain: UserPreferences, userId: String): UserProfilePreferencesEntity {
        return UserProfilePreferencesEntity(
            userId = userId,
            notificationsEnabled = domain.notificationSettings.notificationsEnabled,
            locationSharingEnabled = true, // Default value
            autoJoinEnabled = false, // Default value  
            preferredLanguage = domain.preferredLanguage,
            theme = "SYSTEM", // Default value
            soundEnabled = domain.notificationSettings.soundEnabled,
            vibrationEnabled = true, // Default value
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    fun preferencesFromEntity(entity: UserProfilePreferencesEntity): UserPreferences {
        return UserPreferences(
            preferredLanguage = entity.preferredLanguage,
            notificationSettings = NotificationSettings(
                notificationsEnabled = entity.notificationsEnabled,
                chatNotificationsEnabled = entity.notificationsEnabled,
                tripNotificationsEnabled = entity.notificationsEnabled,
                systemNotificationsEnabled = entity.notificationsEnabled,
                soundEnabled = entity.soundEnabled,
                vibrationEnabled = entity.vibrationEnabled
            )
        )
    }
}

// Extension functions
fun UserProfile.toUserProfileEntity(): UserProfileEntity = UserProfileMapper.toEntity(this)
fun UserProfileEntity.toDomain(): UserProfile = UserProfileMapper.toDomain(this)
fun UserPreferences.toEntity(userId: String): UserProfilePreferencesEntity = UserProfileMapper.preferencesToEntity(this, userId)
fun UserProfilePreferencesEntity.toDomainPreferences(): UserPreferences = UserProfileMapper.preferencesFromEntity(this)
