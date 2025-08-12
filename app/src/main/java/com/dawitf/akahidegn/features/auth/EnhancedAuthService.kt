package com.dawitf.akahidegn.features.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.dawitf.akahidegn.analytics.AnalyticsService
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.core.result.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import kotlinx.coroutines.withTimeout

/**
 * Enhanced authentication service with comprehensive error handling and retry mechanisms
 * Addresses network connectivity issues, authentication timeouts, and device-specific problems
 */
@Singleton
class EnhancedAuthService @Inject constructor(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val analyticsService: AnalyticsService
) {
    
    companion object {
        private const val TAG = "EnhancedAuthService"
        private const val AUTH_TIMEOUT_MS = 30_000L
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2_000L
        private const val NETWORK_CHECK_TIMEOUT_MS = 5_000L
    }

    /**
     * Signs in with Google account with comprehensive error handling and retry logic
     */
    suspend fun signInWithGoogle(idToken: String): Result<String> {
        return try {
            // Check network connectivity first
            val networkCheck = checkNetworkConnectivity()
            if (!networkCheck) {
                return Result.Error(AppError.NetworkError.NoConnection)
            }

            var lastException: Exception? = null
            var attempt = 0

            while (attempt < MAX_RETRY_ATTEMPTS) {
                attempt++
                
                try {
                    Log.d(TAG, "Google sign-in attempt $attempt")
                    
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    
                    val authResult = withTimeout(AUTH_TIMEOUT_MS) {
                        auth.signInWithCredential(credential).await()
                    }
                    
                    val user = authResult.user
                    if (user != null) {
                        Log.d(TAG, "Google sign-in successful for user: ${user.uid}")
                        
                        analyticsService.trackEvent("google_signin_success", mapOf(
                            "user_id" to user.uid,
                            "attempts" to attempt,
                            "provider" to "google"
                        ))
                        
                        // Verify the user's email if available
                        user.email?.let { email ->
                            if (!user.isEmailVerified && shouldVerifyEmail(email)) {
                                sendEmailVerification()
                            }
                        }
                        
                        return Result.Success(user.uid)
                    } else {
                        throw IllegalStateException("Authentication succeeded but user is null")
                    }
                    
                } catch (e: Exception) {
                    lastException = e
                    Log.w(TAG, "Google sign-in attempt $attempt failed", e)
                    
                    val errorType = categorizeAuthError(e)
                    
                    // Don't retry for certain error types
                    if (!shouldRetryForError(errorType)) {
                        return Result.Error(errorType)
                    }
                    
                    // Wait before retrying
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        delay(RETRY_DELAY_MS * attempt) // Exponential backoff
                    }
                }
            }
            
            // All attempts failed
            analyticsService.trackEvent("google_signin_failed", mapOf(
                "attempts" to attempt,
                "error" to (lastException?.message ?: "unknown"),
                "error_type" to (lastException?.javaClass?.simpleName ?: "unknown")
            ))
            
            Result.Error(categorizeAuthError(lastException))
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Google sign-in", e)
            Result.Error(AppError.AuthenticationError.AuthenticationFailed(e.message ?: "Unexpected authentication error"))
        }
    }

    /**
     * Signs out with proper cleanup and error handling
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            
            analyticsService.trackEvent("user_signed_out", emptyMap())
            Log.d(TAG, "User signed out successfully")
            
            Result.Success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during sign out", e)
            analyticsService.trackEvent("signout_error", mapOf(
                "error" to e.message.orEmpty()
            ))
            Result.Error(AppError.AuthenticationError.AuthenticationFailed("Sign out failed: ${e.message}"))
        }
    }

    /**
     * Sends email verification with retry logic
     */
    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = auth.currentUser 
                ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)

            if (user.isEmailVerified) {
                return Result.Success(Unit)
            }

            var attempt = 0
            var lastException: Exception? = null

            while (attempt < MAX_RETRY_ATTEMPTS) {
                attempt++
                
                try {
                    withTimeout(AUTH_TIMEOUT_MS) {
                        user.sendEmailVerification().await()
                    }
                    
                    analyticsService.trackEvent("email_verification_sent", mapOf(
                        "user_id" to user.uid,
                        "attempts" to attempt
                    ))
                    
                    return Result.Success(Unit)
                    
                } catch (e: Exception) {
                    lastException = e
                    Log.w(TAG, "Email verification attempt $attempt failed", e)
                    
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        delay(RETRY_DELAY_MS * attempt)
                    }
                }
            }
            
            Result.Error(AppError.NetworkError.RequestFailed("Failed to send verification email: ${lastException?.message}"))
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error sending email verification", e)
            Result.Error(AppError.NetworkError.RequestFailed("Email verification failed: ${e.message}"))
        }
    }

    /**
     * Refreshes the current user's authentication token
     */
    suspend fun refreshAuthToken(): Result<String> {
        return try {
            val user = auth.currentUser 
                ?: return Result.Error(AppError.AuthenticationError.NotAuthenticated)

            val tokenResult = withTimeout(AUTH_TIMEOUT_MS) {
                user.getIdToken(true).await()
            }
            
            val token = tokenResult.token
            if (token != null) {
                analyticsService.trackEvent("auth_token_refreshed", mapOf(
                    "user_id" to user.uid
                ))
                Result.Success(token)
            } else {
                Result.Error(AppError.AuthenticationError.TokenRefreshFailed)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh auth token", e)
            analyticsService.trackEvent("auth_token_refresh_failed", mapOf(
                "error" to e.message.orEmpty()
            ))
            Result.Error(AppError.AuthenticationError.TokenRefreshFailed)
        }
    }

    /**
     * Checks if the current user session is valid
     */
    suspend fun validateUserSession(): Result<Boolean> {
        return try {
            val user = auth.currentUser
            if (user == null) {
                return Result.Success(false)
            }

            // Try to refresh the token to validate the session
            val tokenResult = refreshAuthToken()
            return when (tokenResult) {
                is Result.Success -> {
                    analyticsService.trackEvent("user_session_validated", mapOf(
                        "user_id" to user.uid
                    ))
                    Result.Success(true)
                }
                is Result.Error -> Result.Success(false)
                is Result.Loading -> Result.Success(false) // Consider as invalid if still loading
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Session validation failed", e)
            Result.Success(false)
        }
    }

    /**
     * Handles authentication state changes with proper error recovery
     */
    fun handleAuthStateChange(isAuthenticated: Boolean, userId: String?) {
        try {
            if (isAuthenticated && userId != null) {
                analyticsService.trackEvent("auth_state_authenticated", mapOf(
                    "user_id" to userId
                ))
            } else {
                analyticsService.trackEvent("auth_state_unauthenticated", emptyMap())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error handling auth state change", e)
        }
    }

    /**
     * Checks network connectivity with timeout
     */
    private suspend fun checkNetworkConnectivity(): Boolean {
        return try {
            withTimeout(NETWORK_CHECK_TIMEOUT_MS) {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = connectivityManager.activeNetwork ?: return@withTimeout false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return@withTimeout false
                
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network connectivity check failed", e)
            false
        }
    }

    /**
     * Categorizes authentication errors for appropriate handling
     */
    private fun categorizeAuthError(exception: Exception?): AppError {
        return when (exception) {
            is FirebaseAuthException -> {
                when (exception.errorCode) {
                    "ERROR_NETWORK_REQUEST_FAILED" -> AppError.NetworkError.NoConnection
                    "ERROR_TOO_MANY_REQUESTS" -> AppError.AuthenticationError.TooManyRequests
                    "ERROR_USER_DISABLED" -> AppError.AuthenticationError.UserDisabled
                    "ERROR_INVALID_CREDENTIAL" -> AppError.AuthenticationError.InvalidCredentials
                    "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> AppError.AuthenticationError.AccountExistsWithDifferentCredential
                    "ERROR_CREDENTIAL_ALREADY_IN_USE" -> AppError.AuthenticationError.CredentialAlreadyInUse
                    else -> AppError.AuthenticationError.AuthenticationFailed(exception.message ?: "Authentication failed")
                }
            }
            is java.net.SocketTimeoutException -> AppError.NetworkError.Timeout
            is java.net.UnknownHostException -> AppError.NetworkError.NoConnection
            is java.net.ConnectException -> AppError.NetworkError.NoConnection
            else -> AppError.AuthenticationError.AuthenticationFailed(exception?.message ?: "Unknown authentication error")
        }
    }

    /**
     * Determines if an error type should trigger a retry
     */
    private fun shouldRetryForError(error: AppError): Boolean {
        return when (error) {
            is AppError.NetworkError.Timeout -> true
            is AppError.NetworkError.RequestFailed -> true
            is AppError.AuthenticationError.TooManyRequests -> false
            is AppError.AuthenticationError.UserDisabled -> false
            is AppError.AuthenticationError.InvalidCredentials -> false
            is AppError.AuthenticationError.AccountExistsWithDifferentCredential -> false
            is AppError.AuthenticationError.CredentialAlreadyInUse -> false
            else -> true
        }
    }

    /**
     * Determines if email verification should be sent for this email
     */
    private fun shouldVerifyEmail(email: String): Boolean {
        // Skip verification for test emails or known development accounts
        val testDomains = listOf("test.com", "example.com", "localhost")
        return !testDomains.any { email.endsWith(it) }
    }
}
