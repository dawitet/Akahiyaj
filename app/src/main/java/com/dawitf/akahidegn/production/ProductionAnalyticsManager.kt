package com.dawitf.akahidegn.production

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production analytics and monitoring manager
 * Comprehensive tracking for user engagement, performance, and business intelligence
 */
@Singleton
class ProductionAnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()
    private val performance = FirebasePerformance.getInstance()
    
    private val _analyticsState = MutableStateFlow(AnalyticsState())
    val analyticsState: StateFlow<AnalyticsState> = _analyticsState
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    data class AnalyticsState(
        val sessionStart: Long = System.currentTimeMillis(),
        val eventsTracked: Int = 0,
        val crashesReported: Int = 0,
        val performanceMetrics: Map<String, Double> = emptyMap()
    )
    
    /**
     * Initialize analytics for production
     */
    fun initializeAnalytics(userId: String) {
        // Set user properties
        firebaseAnalytics.setUserId(userId)
        crashlytics.setUserId(userId)
        
        // Track app start
        trackEvent("app_started", Bundle().apply {
            putLong("timestamp", System.currentTimeMillis())
            putString("version", getAppVersion())
        })
        
        // Set custom keys for crash reporting
        crashlytics.setCustomKey("user_id", userId)
        crashlytics.setCustomKey("app_version", getAppVersion())
    }
    
    /**
     * Track user engagement events
     */
    fun trackUserEngagement(action: String, details: Map<String, Any> = emptyMap()) {
        val bundle = Bundle().apply {
            putString("action", action)
            putLong("timestamp", System.currentTimeMillis())
            details.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
        
        trackEvent("user_engagement", bundle)
    }
    
    /**
     * Track ride-related events
     */
    fun trackRideEvent(event: String, groupId: String, details: Map<String, Any> = emptyMap()) {
        val bundle = Bundle().apply {
            putString("event_type", event)
            putString("group_id", groupId)
            putLong("timestamp", System.currentTimeMillis())
            details.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
        
        trackEvent("ride_activity", bundle)
        
        // Update analytics state
        scope.launch {
            _analyticsState.value = _analyticsState.value.copy(
                eventsTracked = _analyticsState.value.eventsTracked + 1
            )
        }
    }
    
    /**
     * Track app performance metrics
     */
    fun trackPerformance(operation: String, duration: Long, success: Boolean) {
        val bundle = Bundle().apply {
            putString("operation", operation)
            putLong("duration_ms", duration)
            putBoolean("success", success)
            putLong("timestamp", System.currentTimeMillis())
        }
        
        trackEvent("performance_metric", bundle)
        
        // Update performance metrics
        scope.launch {
            val currentMetrics = _analyticsState.value.performanceMetrics.toMutableMap()
            currentMetrics[operation] = duration.toDouble()
            
            _analyticsState.value = _analyticsState.value.copy(
                performanceMetrics = currentMetrics
            )
        }
    }
    
    /**
     * Track business intelligence events
     */
    fun trackBusinessEvent(event: String, revenue: Double? = null, details: Map<String, Any> = emptyMap()) {
        val bundle = Bundle().apply {
            putString("business_event", event)
            revenue?.let { putDouble("revenue", it) }
            putLong("timestamp", System.currentTimeMillis())
            details.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
        
        trackEvent("business_intelligence", bundle)
    }
    
    /**
     * Track popular routes for analytics
     */
    fun trackRoute(from: String, to: String, memberCount: Int) {
        val bundle = Bundle().apply {
            putString("route_from", from)
            putString("route_to", to)
            putInt("member_count", memberCount)
            putLong("timestamp", System.currentTimeMillis())
            putString("route_key", "$from-$to")
        }
        
        trackEvent("route_analytics", bundle)
    }
    
    /**
     * Track peak usage times
     */
    fun trackUsageTime(action: String) {
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val dayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        
        val bundle = Bundle().apply {
            putString("action", action)
            putInt("hour_of_day", currentHour)
            putInt("day_of_week", dayOfWeek)
            putLong("timestamp", System.currentTimeMillis())
        }
        
        trackEvent("usage_pattern", bundle)
    }
    
    /**
     * Report non-fatal errors
     */
    fun reportError(error: Throwable, context: String) {
        crashlytics.setCustomKey("error_context", context)
        crashlytics.recordException(error)
        
        scope.launch {
            _analyticsState.value = _analyticsState.value.copy(
                crashesReported = _analyticsState.value.crashesReported + 1
            )
        }
    }
    
    /**
     * Start performance trace
     */
    fun startTrace(traceName: String): Trace {
        return performance.newTrace(traceName).apply {
            start()
        }
    }
    
    /**
     * Stop performance trace
     */
    fun stopTrace(trace: Trace, attributes: Map<String, String> = emptyMap()) {
        attributes.forEach { (key, value) ->
            trace.putAttribute(key, value)
        }
        trace.stop()
    }
    
    /**
     * Track conversion events
     */
    fun trackConversion(event: String, value: Double) {
        val bundle = Bundle().apply {
            putString("conversion_event", event)
            putDouble("value", value)
            putLong("timestamp", System.currentTimeMillis())
        }
        
        trackEvent("conversion", bundle)
    }
    
    /**
     * Set user properties for segmentation
     */
    fun setUserProperty(name: String, value: String) {
        firebaseAnalytics.setUserProperty(name, value)
    }
    
    /**
     * Track session duration
     */
    fun trackSessionEnd() {
        val sessionDuration = System.currentTimeMillis() - _analyticsState.value.sessionStart
        
        val bundle = Bundle().apply {
            putLong("session_duration", sessionDuration)
            putInt("events_in_session", _analyticsState.value.eventsTracked)
            putLong("timestamp", System.currentTimeMillis())
        }
        
        trackEvent("session_end", bundle)
    }
    
    /**
     * Get analytics summary for monitoring
     */
    fun getAnalyticsSummary(): Map<String, Any> {
        val state = _analyticsState.value
        return mapOf(
            "session_duration" to (System.currentTimeMillis() - state.sessionStart),
            "events_tracked" to state.eventsTracked,
            "crashes_reported" to state.crashesReported,
            "performance_metrics_count" to state.performanceMetrics.size
        )
    }
    
    private fun trackEvent(eventName: String, bundle: Bundle) {
        firebaseAnalytics.logEvent(eventName, bundle)
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
