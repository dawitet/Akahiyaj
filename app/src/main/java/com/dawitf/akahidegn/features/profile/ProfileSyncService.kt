package com.dawitf.akahidegn.features.profile

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.dawitf.akahidegn.core.result.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileSyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("akahidegn_prefs", Context.MODE_PRIVATE)
    }

    suspend fun syncProfileData(): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.Error("User not authenticated")
            val uid = currentUser.uid

            val snapshot = firestore.collection("users").document(uid).get().await()
            if (snapshot.exists()) {
                val remoteName = snapshot.getString("name")
                val remotePhone = snapshot.getString("phone")
                val remoteAvatarUrl = snapshot.getString("avatarUrl")

                sharedPreferences.edit {
                    if (!remoteName.isNullOrBlank()) {
                        putString("user_name", remoteName)
                    }
                    if (!remotePhone.isNullOrBlank()) {
                        putString("user_phone", remotePhone)
                    }
                    if (!remoteAvatarUrl.isNullOrBlank()) {
                        putString("user_avatar_url", remoteAvatarUrl)
                    }
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

            val snapshot = firestore.collection("users").document(uid).get().await()
            if (snapshot.exists()) {
                val remoteName = snapshot.getString("name")
                val remotePhone = snapshot.getString("phone")

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

            firestore.collection("users").document(uid).set(userMap, com.google.firebase.firestore.SetOptions.merge()).await()
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
            sharedPreferences.edit {
                putString("user_name", name)
                putString("user_phone", phone)
                if (!avatarUrl.isNullOrBlank()) putString("user_avatar_url", avatarUrl)
            }

            // Then sync to Firestore (permanent storage)
            val userMap = mutableMapOf<String, Any>(
                "name" to name,
                "phone" to phone,
                "lastUpdated" to System.currentTimeMillis()
            )
            if (!avatarUrl.isNullOrBlank()) {
                userMap["avatarUrl"] = avatarUrl
            }

            firestore.collection("users").document(uid).set(userMap, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Profile update failed: ${e.message}")
        }
    }
}
