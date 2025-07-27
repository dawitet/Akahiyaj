package com.dawitf.akahidegn.features.profile

import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.core.result.Result

import com.dawitf.akahidegn.domain.model.UserProfile
import com.dawitf.akahidegn.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Service for managing user profiles and ride statistics
 */
interface UserProfileService {
    
    // Profile management
    suspend fun getUserProfile(): Result<UserProfile>
    suspend fun updateUserProfile(profile: UserProfileUpdate): Result<Unit>
    suspend fun uploadProfilePhoto(photoUri: String): Result<String>
    
    
    
    // Preferences
    suspend fun updateUserPreferences(preferences: UserPreferences): Result<Unit>
    suspend fun getUserPreferences(): Result<UserPreferences>
    
    // Social features - simplified for Ethiopian market
    fun getFriends(): Flow<List<Friend>>
    suspend fun addFriend(userId: String): Result<Unit>
    suspend fun removeFriend(userId: String): Result<Unit>

    // Cache management
    suspend fun clearCache(): Result<Unit>
    suspend fun syncProfile(): Result<Unit>
}

// Supporting data classes for this service
data class UserProfileUpdate(
    val displayName: String? = null,
    val bio: String? = null,
    val phoneNumber: String? = null
)



data class Friend(
    val userId: String,
    val name: String,
    val profilePicture: String?,
    val totalRides: Int,
    val rating: Float
)
