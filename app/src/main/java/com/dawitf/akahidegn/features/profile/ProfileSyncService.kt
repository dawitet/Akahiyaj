package com.dawitf.akahidegn.features.profile

import android.content.SharedPreferences
import com.dawitf.akahidegn.analytics.AnalyticsService
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.core.result.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

/**
 * Service to handle data synchronization between SharedPreferences and Firebase
 * Ensures profile data consistency across local storage and remote database
 */
@Singleton
class ProfileSyncService @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
    private val sharedPreferences: SharedPreferences,
    private val analyticsService: AnalyticsService
) {
    
    companion object {
        private const val TAG = "ProfileSyncService"
        private const val LAST_SYNC_KEY = "last_profile_sync"
        private const val SYNC_INTERVAL_MS = 300_000L // 5 minutes
    }

    /**
     * Synchronizes profile data between SharedPreferences and Firebase
     * Prioritizes Firebase data as the source of truth
     */
    suspend fun syncProfileData(): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(TAG, "No authenticated user for profile sync")
                return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            }

            val userId = currentUser.uid
            Log.d(TAG, "Starting profile sync for user: $userId")

            // Check if sync is needed (throttle to prevent excessive syncing)
            val lastSync = sharedPreferences.getLong(LAST_SYNC_KEY, 0)
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastSync < SYNC_INTERVAL_MS) {
                Log.d(TAG, "Profile sync skipped - too recent")
                return Result.Success(Unit)
            }

            // Fetch latest data from Firebase
            val userRef = database.reference.child("users").child(userId)
            val snapshot = userRef.get().await()

            if (snapshot.exists()) {
                // Update SharedPreferences with Firebase data
                val editor = sharedPreferences.edit()
                
                snapshot.child("name").getValue(String::class.java)?.let { name ->
                    editor.putString("user_name", name)
                    Log.d(TAG, "Synced user name: $name")
                }
                
                snapshot.child("phone").getValue(String::class.java)?.let { phone ->
                    editor.putString("user_phone", phone)
                    Log.d(TAG, "Synced user phone: $phone")
                }
                
                snapshot.child("avatarUrl").getValue(String::class.java)?.let { avatarUrl ->
                    editor.putString("user_avatar_url", avatarUrl)
                    Log.d(TAG, "Synced user avatar URL")
                } ?: run {
                    // Fallback to avatar field if avatarUrl doesn't exist
                    snapshot.child("avatar").getValue(String::class.java)?.let { avatar ->
                        editor.putString("user_avatar", avatar)
                        Log.d(TAG, "Synced user avatar: $avatar")
                    }
                }
                
                snapshot.child("email").getValue(String::class.java)?.let { email ->
                    editor.putString("user_email", email)
                    Log.d(TAG, "Synced user email")
                }
                
                // Update sync timestamp
                editor.putLong(LAST_SYNC_KEY, currentTime)
                editor.apply()
                
                Log.d(TAG, "Profile sync completed successfully")
                analyticsService.trackEvent("profile_sync_success", mapOf(
                    "user_id" to userId,
                    "sync_timestamp" to currentTime
                ))
                
            } else {
                // User doesn't exist in Firebase, need to create profile
                Log.w(TAG, "User profile not found in Firebase, creating from local data")
                createFirebaseProfileFromLocal(userId)
            }

            Result.Success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Profile sync failed", e)
            analyticsService.trackEvent("profile_sync_error", mapOf(
                "error" to e.message.orEmpty()
            ))
            Result.Error(AppError.NetworkError.RequestFailed("Profile sync failed: ${e.message}"))
        }
    }

    /**
     * Creates Firebase profile from SharedPreferences data
     */
    private suspend fun createFirebaseProfileFromLocal(userId: String) {
        try {
            val userName = sharedPreferences.getString("user_name", null)
            val userPhone = sharedPreferences.getString("user_phone", null)
            val userEmail = auth.currentUser?.email
            val userAvatarUrl = sharedPreferences.getString("user_avatar_url", null)
            val userAvatar = sharedPreferences.getString("user_avatar", "avatar_1")

            if (!userName.isNullOrBlank() && !userPhone.isNullOrBlank()) {
                val userMap = mutableMapOf<String, Any>(
                    "name" to userName,
                    "phone" to userPhone,
                    "email" to (userEmail ?: ""),
                    "registrationTime" to System.currentTimeMillis(),
                    "lastActive" to System.currentTimeMillis()
                )

                if (userAvatarUrl != null) {
                    userMap["avatarUrl"] = userAvatarUrl
                } else {
                    userMap["avatar"] = userAvatar!!
                }

                val userRef = database.reference.child("users").child(userId)
                userRef.setValue(userMap).await()
                
                Log.d(TAG, "Created Firebase profile from local data")
                analyticsService.trackEvent("profile_created_from_local", mapOf(
                    "user_id" to userId
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Firebase profile from local data", e)
        }
    }

    /**
     * Forces immediate profile synchronization
     */
    suspend fun forceSyncProfile(): Result<Unit> {
        // Reset sync timestamp to force sync
        sharedPreferences.edit().putLong(LAST_SYNC_KEY, 0).apply()
        return syncProfileData()
    }

    /**
     * Updates both SharedPreferences and Firebase with new profile data
     */
    suspend fun updateProfileData(
        name: String?,
        phone: String?,
        avatarUrl: String?
    ): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.Error(AppError.AuthenticationError.NotAuthenticated)
            }

            val userId = currentUser.uid
            val editor = sharedPreferences.edit()
            val firebaseUpdates = mutableMapOf<String, Any?>()

            // Update local and prepare Firebase updates
            name?.let { 
                editor.putString("user_name", it)
                firebaseUpdates["name"] = it
            }
            
            phone?.let { 
                editor.putString("user_phone", it)
                firebaseUpdates["phone"] = it
            }
            
            avatarUrl?.let { 
                editor.putString("user_avatar_url", it)
                firebaseUpdates["avatarUrl"] = it
                editor.remove("user_avatar") // Remove old avatar field
            }

            // Apply local changes
            editor.putLong("user_last_updated", System.currentTimeMillis())
            editor.apply()

            // Apply Firebase changes
            if (firebaseUpdates.isNotEmpty()) {
                firebaseUpdates["lastActive"] = System.currentTimeMillis()
                val userRef = database.reference.child("users").child(userId)
                userRef.updateChildren(firebaseUpdates).await()
            }

            Log.d(TAG, "Profile data updated successfully")
            analyticsService.trackEvent("profile_data_updated", mapOf(
                "fields_updated" to firebaseUpdates.keys.joinToString(",")
            ))

            Result.Success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile data", e)
            Result.Error(AppError.NetworkError.RequestFailed("Failed to update profile: ${e.message}"))
        }
    }

    /**
     * Validates profile data consistency
     */
    suspend fun validateProfileConsistency(): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser ?: return Result.Success(false)
            
            val localName = sharedPreferences.getString("user_name", null)
            val localPhone = sharedPreferences.getString("user_phone", null)
            
            val userRef = database.reference.child("users").child(currentUser.uid)
            val snapshot = userRef.get().await()
            
            if (snapshot.exists()) {
                val remoteName = snapshot.child("name").getValue(String::class.java)
                val remotePhone = snapshot.child("phone").getValue(String::class.java)
                
                val isConsistent = localName == remoteName && localPhone == remotePhone
                
                Log.d(TAG, "Profile consistency check: $isConsistent")
                analyticsService.trackEvent("profile_consistency_check", mapOf(
                    "is_consistent" to isConsistent,
                    "has_local_data" to (!localName.isNullOrBlank() && !localPhone.isNullOrBlank()),
                    "has_remote_data" to (!remoteName.isNullOrBlank() && !remotePhone.isNullOrBlank())
                ))
                
                Result.Success(isConsistent)
            } else {
                Result.Success(false)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Profile consistency validation failed", e)
            Result.Success(false)
        }
    }
}
