package com.dawitf.akahidegn.features.profile.impl

import android.net.Uri
import android.util.Log
import com.dawitf.akahidegn.analytics.AnalyticsService
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.core.result.Result

import com.dawitf.akahidegn.domain.model.UserProfile
import com.dawitf.akahidegn.domain.model.UserPreferences
import com.dawitf.akahidegn.domain.model.NotificationPreferences
import com.dawitf.akahidegn.domain.model.PrivacyPreferences
import com.dawitf.akahidegn.features.profile.UserProfileService
import com.dawitf.akahidegn.features.profile.UserProfileUpdate
import com.dawitf.akahidegn.features.profile.ProfileSyncService

import com.dawitf.akahidegn.features.profile.Friend
import com.dawitf.akahidegn.security.SecurityService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UserProfileService that uses Firebase for data storage
 * and retrieval of user profiles, ride statistics, and preferences.
 */
@Singleton
class UserProfileServiceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    private val analyticsService: AnalyticsService,
    private val securityService: SecurityService,
    private val profileSyncService: ProfileSyncService
) : UserProfileService {
    
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val RIDES_COLLECTION = "rides"
        private const val FRIENDS_COLLECTION = "friends"
        private const val USER_PREFERENCES_COLLECTION = "userPreferences"
    }
    
    /**
     * Retrieves the current user's profile from Firebase
     * First attempts to sync data to ensure consistency
     */
    override suspend fun getUserProfile(): Result<UserProfile> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated.message ?: "User not authenticated")
            
            // Attempt to sync profile data first
            profileSyncService.syncProfileData()
            
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (!userDoc.exists()) {
                return Result.Error(AppError.ValidationError.ResourceNotFound("User profile not found").message)
            }
            
            // Get first and last name to combine into displayName
            val firstName = userDoc.getString("firstName") ?: ""
            val lastName = userDoc.getString("lastName") ?: ""
            val displayName = "$firstName $lastName".trim()

            val profile = UserProfile(
                userId = userId,
                displayName = displayName,
                profilePictureUrl = userDoc.getString("profilePhotoUrl"),
                email = userDoc.getString("email") ?: "",
                phoneNumber = userDoc.getString("phoneNumber") ?: "",
                isVerified = userDoc.getBoolean("isVerified") ?: false,
                joinDate = userDoc.getLong("joinDate") ?: System.currentTimeMillis(),
                lastActiveDate = userDoc.getLong("lastActiveDate") ?: System.currentTimeMillis(),
                rating = userDoc.getDouble("rating")?.toFloat() ?: 0f,
                reviewCount = userDoc.getLong("totalRating")?.toInt() ?: 0,
                totalTrips = userDoc.getLong("totalTrips")?.toInt() ?: 0,
                completedTrips = userDoc.getLong("completedTrips")?.toInt() ?: 0,
                cancelledTrips = userDoc.getLong("cancelledTrips")?.toInt() ?: 0,
                totalPassengers = userDoc.getLong("totalPassengers")?.toInt() ?: 0,
                totalDistance = userDoc.getDouble("totalDistance") ?: 0.0,
                
                bio = userDoc.getString("bio") ?: ""
                // Using default values for the remaining fields
            )
            
            Result.Success(profile)
        } catch (e: Exception) {
            analyticsService.logError(e, "get_user_profile_failed")
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to get user profile").message)
        }
    }
    
    /**
     * Updates the user profile with new information
     * Also updates local data through ProfileSyncService
     */
    override suspend fun updateUserProfile(profile: UserProfileUpdate): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated.message ?: "User not authenticated")
            
            // Validate input
            profile.phoneNumber?.let { phone ->
                if (!securityService.validateInput(phone, "phone")) {
                    return Result.Error(AppError.ValidationError.InvalidInput("Invalid phone number format").message)
                }
            }
            
            val updateData = mutableMapOf<String, Any?>()
            profile.displayName?.let {
                // Split displayName into firstName and lastName
                val parts = it.split(" ", limit = 2)
                updateData["firstName"] = parts[0]
                if (parts.size > 1) {
                    updateData["lastName"] = parts[1]
                }
            }
            profile.phoneNumber?.let { updateData["phoneNumber"] = it }
            profile.bio?.let { updateData["bio"] = it }

            updateData["lastUpdated"] = System.currentTimeMillis()
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updateData)
                .await()
            
            // Also update through ProfileSyncService to maintain consistency
            profileSyncService.updateProfileData(
                name = profile.displayName ?: "",
                phone = profile.phoneNumber ?: "",
                avatarUrl = null // We don't update avatar here
            )
            
            analyticsService.trackEvent("user_profile_updated", mapOf(
                "fields_updated" to updateData.keys.joinToString(",")
            ))
            
            Result.Success(Unit)
        } catch (e: Exception) {
            analyticsService.logError(e, "update_user_profile_failed")
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to update profile").message)
        }
    }
    
    /**
     * Uploads a profile photo and updates the user's profile with the new URL
     */
    override suspend fun uploadProfilePhoto(photoUri: String): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated.message ?: "User not authenticated")
            
            val photoRef = storage.reference
                .child("profile_photos")
                .child("$userId.jpg")
            
            photoRef.putFile(Uri.parse(photoUri)).await()
            val downloadUrl = photoRef.downloadUrl.await().toString()
            
            // Update user profile with photo URL
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("profilePhotoUrl", downloadUrl)
                .await()
            
            analyticsService.trackEvent("profile_photo_uploaded", emptyMap())
            
            Result.Success(downloadUrl)
        } catch (e: Exception) {
            analyticsService.logError(e, "upload_profile_photo_failed")
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to upload photo").message)
        }
    }
    
    
    
    /**
     * Updates user preferences
     */
    override suspend fun updateUserPreferences(preferences: UserPreferences): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated.message ?: "User not authenticated")
            
            firestore.collection(USER_PREFERENCES_COLLECTION)
                .document(userId)
                .set(preferences)
                .await()
            
            analyticsService.trackEvent("user_preferences_updated", mapOf(
                "theme" to preferences.theme,
                "language" to preferences.language
            ))
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to update preferences").message)
        }
    }
    
    /**
     * Gets user preferences
     */
    override suspend fun getUserPreferences(): Result<UserPreferences> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated.message ?: "User not authenticated")
            
            val prefsDoc = firestore.collection(USER_PREFERENCES_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (prefsDoc.exists()) {
                val preferences = prefsDoc.toObject(UserPreferences::class.java)
                    ?: createDefaultPreferences()
                Result.Success(preferences)
            } else {
                Result.Success(createDefaultPreferences())
            }
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to get preferences").message)
        }
    }
    
    /**
     * Gets the user's friends list
     */
    override fun getFriends(): Flow<List<Friend>> = flow {
        val userId = auth.currentUser?.uid ?: return@flow
        
        try {
            val friendsSnapshot = firestore.collection(FRIENDS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .await()
            
            val friends = friendsSnapshot.documents.mapNotNull { doc ->
                try {
                    Friend(
                        userId = doc.getString("friendId") ?: "",
                        name = doc.getString("friendName") ?: "",
                        profilePicture = doc.getString("profilePhotoUrl"),
                        totalRides = doc.getLong("totalRides")?.toInt() ?: 0,
                        rating = doc.getDouble("rating")?.toFloat() ?: 0f
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            emit(friends)
        } catch (e: Exception) {
            analyticsService.logError(e, "get_friends_failed")
        }
    }
    
    /**
     * Adds a friend by sending a friend request
     */
    override suspend fun addFriend(userId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated.message ?: "User not authenticated")
            
            if (currentUserId == userId) {
                return Result.Error(AppError.ValidationError.InvalidInput("Cannot send friend request to yourself").message)
            }
            
            firestore.collection("friend_requests")
                .add(mapOf(
                    "fromUserId" to currentUserId,
                    "toUserId" to userId,
                    "status" to "PENDING",
                    "timestamp" to System.currentTimeMillis()
                ))
                .await()
            
            analyticsService.trackEvent("friend_request_sent", emptyMap())
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to send friend request").message)
        }
    }
    
    /**
     * Removes a friend connection
     */
    override suspend fun removeFriend(userId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated.message ?: "User not authenticated")
            
            // Find friendship documents in both directions
            val friendshipQuery1 = firestore.collection(FRIENDS_COLLECTION)
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("friendId", userId)
                .get()
                .await()

            val friendshipQuery2 = firestore.collection(FRIENDS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("friendId", currentUserId)
                .get()
                .await()

            // Delete all matching documents
            for (doc in friendshipQuery1.documents + friendshipQuery2.documents) {
                firestore.collection(FRIENDS_COLLECTION).document(doc.id).delete().await()
            }

            analyticsService.trackEvent("friend_removed", emptyMap())

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to remove friend").message)
        }
    }

    /**
     * Clears cached user data
     */
    override suspend fun clearCache(): Result<Unit> {
        return try {
            // Implementation would depend on your caching strategy
            // For example, if using Room database:
            // userPreferencesDao.clearAll()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to clear cache").message)
        }
    }

    /**
     * Syncs user profile from remote to local storage
     * Now delegates to ProfileSyncService for comprehensive synchronization
     */
    override suspend fun syncProfile(): Result<Unit> {
        return try {
            // Use ProfileSyncService for comprehensive sync
            val syncResult = profileSyncService.forceSyncProfile()
            
            // Also perform validation check
            val validationResult = profileSyncService.validateProfileConsistency()
            when (validationResult) {
                is Result.Success -> {
                    if (!validationResult.data) {
                        analyticsService.trackEvent("profile_sync_inconsistency_detected", emptyMap())
                        // Attempt another sync to fix inconsistency
                        profileSyncService.forceSyncProfile()
                    }
                }
                is Result.Error -> {
                    analyticsService.logError(Exception("Profile validation failed"), "profile_validation_failed")
                }
                is Result.Loading -> {
                    // Validation still in progress, but continue with sync result
                    Log.d("UserProfileService", "Profile validation still in progress")
                }
            }

            analyticsService.trackEvent("profile_synced", emptyMap())
            syncResult
            
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.RequestFailed(e.message ?: "Failed to sync profile").message)
        }
    }

    /**
     * Creates default preferences for a new user
     */
    private fun createDefaultPreferences(): UserPreferences {
        return UserPreferences(
            language = "en",
            theme = "SYSTEM",
            notifications = NotificationPreferences(),
            privacy = PrivacyPreferences()
        )
    }
}
