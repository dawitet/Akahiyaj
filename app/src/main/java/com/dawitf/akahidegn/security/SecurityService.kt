package com.dawitf.akahidegn.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dawitf.akahidegn.analytics.AnalyticsService
import com.dawitf.akahidegn.core.error.AppError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(name = "security_settings")

@Singleton
class SecurityService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsService: AnalyticsService
) {
    
    private val dataStore = context.securityDataStore
    private val rateLimitMap = ConcurrentHashMap<String, RateLimitInfo>()
    
    data class RateLimitInfo(
        val lastRequestTime: Long,
        val requestCount: Int,
        val windowStartTime: Long
    )
    
    // Input Validation
    fun validateGroupName(name: String): Result<String> {
        return when {
            name.isBlank() -> Result.failure(AppError.ValidationError.InvalidInput("Group name cannot be empty"))
            name.length < 3 -> Result.failure(AppError.ValidationError.InvalidInput("Group name must be at least 3 characters"))
            name.length > 50 -> Result.failure(AppError.ValidationError.InvalidInput("Group name cannot exceed 50 characters"))
            !name.matches(Regex("^[a-zA-Z0-9\\s\\u1200-\\u137F\\u1380-\\u139F\\u2D80-\\u2DDF]+$")) -> 
                Result.failure(AppError.ValidationError.InvalidInput("Group name contains invalid characters"))
            containsProfanity(name) -> Result.failure(AppError.ValidationError.InvalidInput("Group name contains inappropriate content"))
            else -> Result.success(sanitizeInput(name))
        }
    }
    
    fun validateDestination(destination: String): Result<String> {
        return when {
            destination.isBlank() -> Result.failure(AppError.ValidationError.InvalidInput("Destination cannot be empty"))
            destination.length < 3 -> Result.failure(AppError.ValidationError.InvalidInput("Destination must be at least 3 characters"))
            destination.length > 100 -> Result.failure(AppError.ValidationError.InvalidInput("Destination cannot exceed 100 characters"))
            !destination.matches(Regex("^[a-zA-Z0-9\\s\\u1200-\\u137F\\u1380-\\u139F\\u2D80-\\u2DDF.,'-]+$")) -> 
                Result.failure(AppError.ValidationError.InvalidInput("Destination contains invalid characters"))
            else -> Result.success(sanitizeInput(destination))
        }
    }
    
    fun validateChatMessage(message: String): Result<String> {
        return when {
            message.isBlank() -> Result.failure(AppError.ValidationError.InvalidInput("Message cannot be empty"))
            message.length > 500 -> Result.failure(AppError.ValidationError.InvalidInput("Message cannot exceed 500 characters"))
            containsProfanity(message) -> Result.failure(AppError.ValidationError.InvalidInput("Message contains inappropriate content"))
            containsSpam(message) -> Result.failure(AppError.ValidationError.InvalidInput("Message appears to be spam"))
            else -> Result.success(sanitizeInput(message))
        }
    }
    
    fun validateUserName(name: String): Result<String> {
        return when {
            name.isBlank() -> Result.failure(AppError.ValidationError.InvalidInput("Name cannot be empty"))
            name.length < 2 -> Result.failure(AppError.ValidationError.InvalidInput("Name must be at least 2 characters"))
            name.length > 30 -> Result.failure(AppError.ValidationError.InvalidInput("Name cannot exceed 30 characters"))
            !name.matches(Regex("^[a-zA-Z\\s\\u1200-\\u137F\\u1380-\\u139F\\u2D80-\\u2DDF]+$")) -> 
                Result.failure(AppError.ValidationError.InvalidInput("Name contains invalid characters"))
            containsProfanity(name) -> Result.failure(AppError.ValidationError.InvalidInput("Name contains inappropriate content"))
            else -> Result.success(sanitizeInput(name))
        }
    }
    
    // Generic input validation method
    fun validateInput(input: String, type: String): Boolean {
        return when (type) {
            "phone" -> input.matches(Regex("^\\+?[1-9]\\d{1,14}$"))
            "license" -> input.matches(Regex("^[A-Z0-9]{5,20}$"))
            "email" -> input.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
            "text" -> input.isNotBlank() && input.length <= 500 && !containsProfanity(input)
            else -> input.isNotBlank() && input.length <= 100
        }
    }
    
    // Rate Limiting
    suspend fun checkRateLimit(userId: String, action: String, maxRequests: Int = 10, windowMs: Long = 60000): Result<Unit> {
        val key = "${userId}_$action"
        val currentTime = System.currentTimeMillis()
        
        val rateLimitInfo = rateLimitMap[key]
        
        return if (rateLimitInfo == null) {
            // First request
            rateLimitMap[key] = RateLimitInfo(currentTime, 1, currentTime)
            Result.success(Unit)
        } else {
            // Check if we're in a new window
            if (currentTime - rateLimitInfo.windowStartTime > windowMs) {
                // New window, reset count
                rateLimitMap[key] = RateLimitInfo(currentTime, 1, currentTime)
                Result.success(Unit)
            } else if (rateLimitInfo.requestCount >= maxRequests) {
                // Rate limit exceeded
                analyticsService.trackCustomEvent("rate_limit_exceeded", mapOf(
                    "user_id" to userId,
                    "action" to action,
                    "request_count" to rateLimitInfo.requestCount,
                    "window_start" to rateLimitInfo.windowStartTime
                ))
                Result.failure(AppError.RateLimitError.TooManyRequests("Too many requests. Please try again later."))
            } else {
                // Within limits, increment count
                rateLimitMap[key] = rateLimitInfo.copy(
                    lastRequestTime = currentTime,
                    requestCount = rateLimitInfo.requestCount + 1
                )
                Result.success(Unit)
            }
        }
    }
    
    // Encryption for sensitive data
    fun encryptSensitiveData(data: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(data.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            analyticsService.logError(e, "encryption_failed")
            data // Return original if encryption fails
        }
    }
    
    // Secure storage for sensitive preferences
    suspend fun storeSecurePreference(key: String, value: Long) {
        try {
            val prefKey = longPreferencesKey(key)
            dataStore.edit { preferences ->
                preferences[prefKey] = value
            }
        } catch (e: Exception) {
            analyticsService.logError(e, "secure_storage_write")
        }
    }
    
    suspend fun getSecurePreference(key: String, defaultValue: Long = 0): Long {
        return try {
            val prefKey = longPreferencesKey(key)
            dataStore.data.map { preferences ->
                preferences[prefKey] ?: defaultValue
            }.first()
        } catch (e: Exception) {
            analyticsService.logError(e, "secure_storage_read")
            defaultValue
        }
    }
    
    // Session Security
    suspend fun validateSession(userId: String): Result<Unit> {
        val lastActivity = getSecurePreference("last_activity_$userId")
        val currentTime = System.currentTimeMillis()
        val sessionTimeout = 24 * 60 * 60 * 1000L // 24 hours
        
        return if (currentTime - lastActivity > sessionTimeout) {
            analyticsService.trackCustomEvent("session_expired", mapOf(
                "user_id" to userId,
                "last_activity" to lastActivity,
                "timeout_hours" to 24
            ))
            Result.failure(AppError.AuthenticationError.SessionExpired("Session has expired. Please sign in again."))
        } else {
            storeSecurePreference("last_activity_$userId", currentTime)
            Result.success(Unit)
        }
    }
    
    // Helper functions
    private fun sanitizeInput(input: String): String {
        return input.trim()
            .replace(Regex("\\s+"), " ") // Replace multiple spaces with single space
            .replace(Regex("[\\r\\n\\t]"), "") // Remove line breaks and tabs
    }
    
    private fun containsProfanity(text: String): Boolean {
        // Basic profanity filter - in production, use a more comprehensive solution
        val profanityWords = listOf(
            // Add appropriate profanity words for your target languages
            // This is a simplified example
        )
        
        val lowerText = text.lowercase()
        return profanityWords.any { lowerText.contains(it) }
    }
    
    private fun containsSpam(text: String): Boolean {
        // Basic spam detection
        val spamIndicators = listOf(
            Regex("(http|https)://[^\\s]+"), // URLs
            Regex("\\b\\d{3}-\\d{3}-\\d{4}\\b"), // Phone numbers
            Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), // Email addresses
            Regex("\\b(buy|sell|cheap|free|discount)\\b", RegexOption.IGNORE_CASE), // Commercial terms
        )
        
        // Check for repeated characters
        if (Regex("(.)\\1{4,}").containsMatchIn(text)) return true
        
        // Check for spam indicators
        val spamCount = spamIndicators.count { it.containsMatchIn(text) }
        return spamCount > 1 // If multiple spam indicators are found
    }
    
    // Cleanup old rate limit entries
    fun cleanupRateLimitData() {
        val currentTime = System.currentTimeMillis()
        val expiredThreshold = 60 * 60 * 1000L // 1 hour
        
        rateLimitMap.entries.removeAll { (_, rateLimitInfo) ->
            currentTime - rateLimitInfo.windowStartTime > expiredThreshold
        }
    }
}
