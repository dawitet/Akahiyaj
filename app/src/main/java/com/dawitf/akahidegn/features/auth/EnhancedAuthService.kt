package com.dawitf.akahidegn.features.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.dawitf.akahidegn.core.result.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnhancedAuthService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth
) {
    suspend fun validateToken(): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser ?: return Result.Error("No authenticated user")
            currentUser.getIdToken(false).await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error("Token validation failed: ${e.message}")
        }
    }

    suspend fun refreshToken(): Result<String> {
        return try {
            val currentUser = auth.currentUser ?: return Result.Error("No authenticated user")
            val tokenResult = currentUser.getIdToken(true).await()
            Result.Success(tokenResult.token ?: "")
        } catch (e: Exception) {
            Result.Error("Token refresh failed: ${e.message}")
        }
    }

    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Sign out failed: ${e.message}")
        }
    }
}
