package com.dawitf.akahidegn.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()
    
    // User Events
    fun trackUserSignIn(method: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
    }
    
    fun trackUserSignUp(method: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)
    }
    
    // Ride-sharing Events
    fun trackGroupCreated(groupId: String, destination: String) {
        val bundle = Bundle().apply {
            putString("group_id", groupId)
            putString("destination", destination)
            putString("event_type", "group_created")
        }
        firebaseAnalytics.logEvent("ride_action", bundle)
    }
    
    fun trackGroupJoined(groupId: String, memberCount: Int) {
        val bundle = Bundle().apply {
            putString("group_id", groupId)
            putInt("member_count", memberCount)
            putString("event_type", "group_joined")
        }
        firebaseAnalytics.logEvent("ride_action", bundle)
    }
    
    fun trackGroupLeft(groupId: String, memberCount: Int) {
        val bundle = Bundle().apply {
            putString("group_id", groupId)
            putInt("member_count", memberCount)
            putString("event_type", "group_left")
        }
        firebaseAnalytics.logEvent("ride_action", bundle)
    }
    
    fun trackRideCompleted(groupId: String, duration: Long, distance: Float) {
        val bundle = Bundle().apply {
            putString("group_id", groupId)
            putLong("duration_ms", duration)
            putFloat("distance_km", distance)
            putString("event_type", "ride_completed")
        }
        firebaseAnalytics.logEvent("ride_action", bundle)
    }
    
    // Chat Events
    fun trackMessageSent(groupId: String, messageLength: Int) {
        val bundle = Bundle().apply {
            putString("group_id", groupId)
            putInt("message_length", messageLength)
        }
        firebaseAnalytics.logEvent("message_sent", bundle)
    }
    
    fun trackChatOpened(groupId: String) {
        val bundle = Bundle().apply {
            putString("group_id", groupId)
        }
        firebaseAnalytics.logEvent("chat_opened", bundle)
    }
    
    // Search and Discovery
    fun trackSearchPerformed(query: String, resultCount: Int) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SEARCH_TERM, query)
            putInt("result_count", resultCount)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle)
    }
    
    fun trackLocationPermissionGranted() {
        val bundle = Bundle().apply {
            putString("permission_type", "location")
            putString("permission_status", "granted")
        }
        firebaseAnalytics.logEvent("permission_changed", bundle)
    }
    
    fun trackLocationPermissionDenied() {
        val bundle = Bundle().apply {
            putString("permission_type", "location")
            putString("permission_status", "denied")
        }
        firebaseAnalytics.logEvent("permission_changed", bundle)
    }
    
    // Performance Events
    fun trackScreenLoadTime(screenName: String, loadTimeMs: Long) {
        val bundle = Bundle().apply {
            putString("screen_name", screenName)
            putLong("load_time_ms", loadTimeMs)
        }
        firebaseAnalytics.logEvent("screen_performance", bundle)
    }
    
    fun trackNetworkLatency(endpoint: String, latencyMs: Long, success: Boolean) {
        val bundle = Bundle().apply {
            putString("endpoint", endpoint)
            putLong("latency_ms", latencyMs)
            putBoolean("success", success)
        }
        firebaseAnalytics.logEvent("network_performance", bundle)
    }
    
    // Ad Events
    fun trackAdShown(adType: String, placement: String) {
        val bundle = Bundle().apply {
            putString("ad_type", adType)
            putString("placement", placement)
        }
        firebaseAnalytics.logEvent("ad_impression", bundle)
    }
    
    fun trackAdClicked(adType: String, placement: String) {
        val bundle = Bundle().apply {
            putString("ad_type", adType)
            putString("placement", placement)
        }
        firebaseAnalytics.logEvent("ad_click", bundle)
    }
    
    fun trackAdRewarded(rewardType: String, rewardAmount: Int) {
        val bundle = Bundle().apply {
            putString("reward_type", rewardType)
            putInt("reward_amount", rewardAmount)
        }
        firebaseAnalytics.logEvent("ad_reward", bundle)
    }
    
    // User Properties
    fun setUserProperty(name: String, value: String) {
        firebaseAnalytics.setUserProperty(name, value)
    }
    
    fun setUserId(userId: String) {
        firebaseAnalytics.setUserId(userId)
    }
    
    // Error Tracking
    fun logError(error: Throwable, context: String) {
        crashlytics.recordException(error)
        crashlytics.setCustomKey("error_context", context)
    }
    
    fun logError(message: String, context: String) {
        crashlytics.log("$context: $message")
    }
    
    fun setUserIdentifier(userId: String) {
        crashlytics.setUserId(userId)
    }
    
    // Custom Events
    fun trackEvent(eventName: String, parameters: Map<String, Any> = emptyMap()) {
        trackCustomEvent(eventName, parameters)
    }
    
    fun trackCustomEvent(eventName: String, parameters: Map<String, Any>) {
        val bundle = Bundle()
        parameters.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Float -> bundle.putFloat(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        firebaseAnalytics.logEvent(eventName, bundle)
    }
}
