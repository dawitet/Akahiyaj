package com.dawitf.akahidegn.features.profile

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.dawitf.akahidegn.core.result.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileSyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) {
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("akahidegn_prefs", Context.MODE_PRIVATE)
    }

    suspend fun syncProfileData(): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.Error("User not authenticated")
            val uid = currentUser.uid

            val snapshot = database.reference.child("users").child(uid).get().await()
            if (snapshot.exists()) {
                val remoteName = snapshot.child("name").getValue(String::class.java)
                val remotePhone = snapshot.child("phone").getValue(String::class.java)
                val remoteAvatarUrl = snapshot.child("avatarUrl").getValue(String::class.java)

                if (!remoteName.isNullOrBlank()) {
                    sharedPreferences.edit().putString("user_name", remoteName).apply()
                }
                if (!remotePhone.isNullOrBlank()) {
                    sharedPreferences.edit().putString("user_phone", remotePhone).apply()
                }
                if (!remoteAvatarUrl.isNullOrBlank()) {
                    sharedPreferences.edit().putString("user_avatar_url", remoteAvatarUrl).apply()
                }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Profile sync failed: ${e.message}")
        }
    }

    suspend fun validateProfileConsistency(): Result<Boolean> {
        return try {
            val localName = sharedPreferences.getString("user_name", null)
            val localPhone = sharedPreferences.getString("user_phone", null)

            val currentUser = auth.currentUser ?: return Result.Error("User not authenticated")
            val uid = currentUser.uid

            val snapshot = database.reference.child("users").child(uid).get().await()
            if (snapshot.exists()) {
                val remoteName = snapshot.child("name").getValue(String::class.java)
                val remotePhone = snapshot.child("phone").getValue(String::class.java)

                val isConsistent = localName == remoteName && localPhone == remotePhone
                Result.Success(isConsistent)
            } else {
                Result.Success(false)
            }
        } catch (e: Exception) {
            Result.Error("Profile validation failed: ${e.message}")
        }
    }

    suspend fun forceSyncProfile(): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.Error("User not authenticated")
            val uid = currentUser.uid

            val localName = sharedPreferences.getString("user_name", null)
            val localPhone = sharedPreferences.getString("user_phone", null)
            val localAvatarUrl = sharedPreferences.getString("user_avatar_url", null)

            val userMap = mutableMapOf<String, Any>()
            if (!localName.isNullOrBlank()) userMap["name"] = localName
            if (!localPhone.isNullOrBlank()) userMap["phone"] = localPhone
            if (!localAvatarUrl.isNullOrBlank()) userMap["avatarUrl"] = localAvatarUrl
            userMap["lastSync"] = System.currentTimeMillis()

            database.reference.child("users").child(uid).updateChildren(userMap).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Force sync failed: ${e.message}")
        }
    }

    suspend fun updateProfileData(name: String, phone: String, avatarUrl: String?): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.Error("User not authenticated")
            val uid = currentUser.uid

            // Update locally first
            sharedPreferences.edit()
                .putString("user_name", name)
                .putString("user_phone", phone)
                .apply {
                    if (!avatarUrl.isNullOrBlank()) {
                        putString("user_avatar_url", avatarUrl)
                    }
                }
                .apply()

            // Then sync to Firebase
            val userMap = mutableMapOf<String, Any>(
                "name" to name,
                "phone" to phone,
                "lastUpdated" to System.currentTimeMillis()
            )
            if (!avatarUrl.isNullOrBlank()) {
                userMap["avatarUrl"] = avatarUrl
            }

            database.reference.child("users").child(uid).updateChildren(userMap).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Profile update failed: ${e.message}")
        }
    }
}
