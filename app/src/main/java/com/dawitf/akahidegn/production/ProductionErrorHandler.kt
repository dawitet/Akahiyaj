package com.dawitf.akahidegn.production

import android.content.Context
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production error handling and crash reporting system
 * Comprehensive error tracking, recovery, and monitoring
 */
@Singleton
class ProductionErrorHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val crashlytics = FirebaseCrashlytics.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _errorState = MutableStateFlow(ErrorState())
    val errorState: StateFlow<ErrorState> = _errorState
    
    data class ErrorState(
        val totalErrors: Int = 0,
        val criticalErrors: Int = 0,
        val recoveredErrors: Int = 0,
        val lastError: String? = null,
        val errorCategories: Map<String, Int> = emptyMap()
    )
    
    enum class ErrorSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    enum class ErrorCategory {
        NETWORK, DATABASE, UI, LOCATION, AUTHENTICATION, PAYMENT, UNKNOWN
    }
    
    /**
     * Handle errors with comprehensive tracking and recovery
     */
    fun handleError(
        error: Throwable,
        context: String,
        severity: ErrorSeverity = ErrorSeverity.MEDIUM,
        category: ErrorCategory = ErrorCategory.UNKNOWN,
        recoveryAction: (() -> Unit)? = null
    ) {
        // Log error locally
        Log.e("ProductionError", "$context: ${error.message}", error)
        
        // Update error state
        updateErrorState(severity, category, error.message ?: "Unknown error")
        
        // Set crash reporting context
        crashlytics.setCustomKey("error_context", context)
        crashlytics.setCustomKey("error_severity", severity.name)
        crashlytics.setCustomKey("error_category", category.name)
        crashlytics.setCustomKey("timestamp", System.currentTimeMillis())
        
        // Report based on severity
        when (severity) {
            ErrorSeverity.CRITICAL -> {
                crashlytics.recordException(error)
                // Additional critical error handling
                handleCriticalError(error, context)
            }
            ErrorSeverity.HIGH -> {
                crashlytics.recordException(error)
            }
            ErrorSeverity.MEDIUM -> {
                crashlytics.recordException(error)
            }
            ErrorSeverity.LOW -> {
                // Log only for low severity
                crashlytics.log("Low severity error: $context - ${error.message}")
            }
        }
        
        // Attempt recovery if provided
        recoveryAction?.let {
            try {
                it.invoke()
                markErrorRecovered()
            } catch (recoveryError: Exception) {
                // Recovery failed, escalate
                handleError(
                    recoveryError,
                    "Recovery failed for: $context",
                    ErrorSeverity.HIGH,
                    ErrorCategory.UNKNOWN
                )
            }
        }
    }
    
    /**
     * Handle network-specific errors
     */
    fun handleNetworkError(
        error: Throwable,
        operation: String,
        retryAction: (() -> Unit)? = null
    ) {
        crashlytics.setCustomKey("network_operation", operation)
        crashlytics.setCustomKey("network_error_type", error.javaClass.simpleName)
        
        handleError(
            error,
            "Network error during: $operation",
            ErrorSeverity.MEDIUM,
            ErrorCategory.NETWORK,
            retryAction
        )
    }
    
    /**
     * Handle database-specific errors
     */
    fun handleDatabaseError(
        error: Throwable,
        operation: String,
        fallbackAction: (() -> Unit)? = null
    ) {
        crashlytics.setCustomKey("database_operation", operation)
        crashlytics.setCustomKey("database_error_type", error.javaClass.simpleName)
        
        handleError(
            error,
            "Database error during: $operation",
            ErrorSeverity.HIGH,
            ErrorCategory.DATABASE,
            fallbackAction
        )
    }
    
    /**
     * Handle UI-specific errors
     */
    fun handleUIError(
        error: Throwable,
        screen: String,
        component: String? = null
    ) {
        crashlytics.setCustomKey("ui_screen", screen)
        component?.let { crashlytics.setCustomKey("ui_component", it) }
        
        handleError(
            error,
            "UI error on screen: $screen${component?.let { " component: $it" } ?: ""}",
            ErrorSeverity.LOW,
            ErrorCategory.UI
        )
    }
    
    /**
     * Handle location service errors
     */
    fun handleLocationError(
        error: Throwable,
        operation: String,
        fallbackToCache: (() -> Unit)? = null
    ) {
        crashlytics.setCustomKey("location_operation", operation)
        
        handleError(
            error,
            "Location error during: $operation",
            ErrorSeverity.MEDIUM,
            ErrorCategory.LOCATION,
            fallbackToCache
        )
    }
    
    /**
     * Handle authentication errors
     */
    fun handleAuthenticationError(
        error: Throwable,
        operation: String,
        retryAuth: (() -> Unit)? = null
    ) {
        crashlytics.setCustomKey("auth_operation", operation)
        
        handleError(
            error,
            "Authentication error during: $operation",
            ErrorSeverity.HIGH,
            ErrorCategory.AUTHENTICATION,
            retryAuth
        )
    }
    
    /**
     * Handle critical errors with immediate action
     */
    private fun handleCriticalError(error: Throwable, context: String) {
        // Send immediate crash report
        crashlytics.sendUnsentReports()
        
        // Log critical error separately
        Log.wtf("CriticalError", "CRITICAL: $context", error)
        
        // Could trigger immediate notification to development team
        // notifyDevelopmentTeam(error, context)
    }
    
    /**
     * Update error tracking state
     */
    private fun updateErrorState(severity: ErrorSeverity, category: ErrorCategory, message: String) {
        scope.launch {
            val currentState = _errorState.value
            val newCategories = currentState.errorCategories.toMutableMap()
            newCategories[category.name] = (newCategories[category.name] ?: 0) + 1
            
            _errorState.value = currentState.copy(
                totalErrors = currentState.totalErrors + 1,
                criticalErrors = if (severity == ErrorSeverity.CRITICAL) {
                    currentState.criticalErrors + 1
                } else currentState.criticalErrors,
                lastError = message,
                errorCategories = newCategories
            )
        }
    }
    
    /**
     * Mark error as recovered
     */
    private fun markErrorRecovered() {
        scope.launch {
            _errorState.value = _errorState.value.copy(
                recoveredErrors = _errorState.value.recoveredErrors + 1
            )
        }
    }
    
    /**
     * Get error statistics for monitoring
     */
    fun getErrorStatistics(): Map<String, Any> {
        val state = _errorState.value
        return mapOf(
            "total_errors" to state.totalErrors,
            "critical_errors" to state.criticalErrors,
            "recovered_errors" to state.recoveredErrors,
            "recovery_rate" to if (state.totalErrors > 0) {
                (state.recoveredErrors.toDouble() / state.totalErrors) * 100
            } else 0.0,
            "error_categories" to state.errorCategories,
            "last_error" to (state.lastError ?: "None")
        )
    }
    
    /**
     * Reset error statistics (for testing or new sessions)
     */
    fun resetErrorStatistics() {
        scope.launch {
            _errorState.value = ErrorState()
        }
    }
    
    /**
     * Set user context for crash reporting
     */
    fun setUserContext(userId: String, additionalInfo: Map<String, String> = emptyMap()) {
        crashlytics.setUserId(userId)
        additionalInfo.forEach { (key, value) ->
            crashlytics.setCustomKey(key, value)
        }
    }
    
    /**
     * Log custom breadcrumbs for debugging
     */
    fun logBreadcrumb(message: String, category: String = "general") {
        crashlytics.log("[$category] $message")
    }
    
    /**
     * Force send unsent crash reports (for testing)
     */
    fun forceSendCrashReports() {
        crashlytics.sendUnsentReports()
    }
    
    /**
     * Enable/disable crash collection (for GDPR compliance)
     */
    fun setCrashCollectionEnabled(enabled: Boolean) {
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }
}
