package com.dawitf.akahidegn.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        Firebase.analytics
    }

    fun logEvent(eventName: String, params: Map<String, Any?> = emptyMap()) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Float -> putFloat(key, value)
                    null -> putString(key, "null")
                    else -> putString(key, value.toString())
                }
            }
        }
        firebaseAnalytics.logEvent(eventName, bundle)
    }

    // User Events
    fun logUserSignUp(method: String) {
        logEvent(FirebaseAnalytics.Event.SIGN_UP, mapOf(
            FirebaseAnalytics.Param.METHOD to method
        ))
    }

    fun logUserLogin(method: String) {
        logEvent(FirebaseAnalytics.Event.LOGIN, mapOf(
            FirebaseAnalytics.Param.METHOD to method
        ))
    }

    fun logProfileUpdate() {
        logEvent("profile_update")
    }

    fun trackAppStart() {
        logEvent("app_start", mapOf(
            "timestamp" to System.currentTimeMillis(),
            "platform" to "android"
        ))
    }

    // Trip Events
    fun logTripCreated(
        tripId: String,
        destination: String,
        departureTime: Long,
        maxPassengers: Int,
        pricePerSeat: Double
    ) {
        logEvent("trip_created", mapOf(
            "trip_id" to tripId,
            "destination" to destination,
            "departure_time" to departureTime,
            "max_passengers" to maxPassengers,
            "price_per_seat" to pricePerSeat
        ))
    }

    fun logTripJoined(
        tripId: String,
        destination: String,
        joinedAt: Long,
        seatsRequested: Int
    ) {
        logEvent("trip_joined", mapOf(
            "trip_id" to tripId,
            "destination" to destination,
            "joined_at" to joinedAt,
            "seats_requested" to seatsRequested
        ))
    }

    fun logTripCompleted(
        tripId: String,
        duration: Long,
        distance: Double,
        rating: Double
    ) {
        logEvent("trip_completed", mapOf(
            "trip_id" to tripId,
            "duration_minutes" to duration,
            "distance_km" to distance,
            "rating" to rating
        ))
    }

    fun logTripCancelled(
        tripId: String,
        reason: String,
        cancelledBy: String
    ) {
        logEvent("trip_cancelled", mapOf(
            "trip_id" to tripId,
            "reason" to reason,
            "cancelled_by" to cancelledBy
        ))
    }

    // Search Events
    fun logSearch(
        query: String,
        filters: Map<String, Any>,
        resultsCount: Int
    ) {
        logEvent(FirebaseAnalytics.Event.SEARCH, mapOf(
            FirebaseAnalytics.Param.SEARCH_TERM to query,
            "filters_applied" to filters.size,
            "results_count" to resultsCount
        ))
    }

    fun logFilterApplied(filterType: String, filterValue: String) {
        logEvent("filter_applied", mapOf(
            "filter_type" to filterType,
            "filter_value" to filterValue
        ))
    }

    // Notification Events
    fun logNotificationReceived(type: String, importance: String) {
        logEvent("notification_received", mapOf(
            "notification_type" to type,
            "importance" to importance
        ))
    }

    fun logNotificationClicked(type: String, actionTaken: String) {
        logEvent("notification_clicked", mapOf(
            "notification_type" to type,
            "action_taken" to actionTaken
        ))
    }

    // UI Events
    fun logScreenView(screenName: String, screenClass: String) {
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, mapOf(
            FirebaseAnalytics.Param.SCREEN_NAME to screenName,
            FirebaseAnalytics.Param.SCREEN_CLASS to screenClass
        ))
    }

    fun logButtonClick(buttonName: String, screenName: String) {
        logEvent("button_click", mapOf(
            "button_name" to buttonName,
            "screen_name" to screenName
        ))
    }

    // Achievement Events
    fun logAchievementUnlocked(
        achievementId: String,
        achievementName: String,
        category: String,
        rarity: String
    ) {
        logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, mapOf(
            FirebaseAnalytics.Param.ACHIEVEMENT_ID to achievementId,
            "achievement_name" to achievementName,
            "category" to category,
            "rarity" to rarity
        ))
    }

    // Error Events
    fun logError(
        errorType: String,
        errorMessage: String,
        screenName: String? = null
    ) {
        logEvent("app_error", mapOf(
            "error_type" to errorType,
            "error_message" to errorMessage,
            "screen_name" to (screenName ?: "unknown")
        ))
    }

    // Performance Events
    fun logPerformanceMetric(
        metricName: String,
        value: Double,
        unit: String
    ) {
        logEvent("performance_metric", mapOf(
            "metric_name" to metricName,
            "value" to value,
            "unit" to unit
        ))
    }

    // Business Intelligence Events
    fun logRevenue(
        tripId: String,
        amount: Double,
        currency: String = "ETB"
    ) {
        logEvent(FirebaseAnalytics.Event.PURCHASE, mapOf(
            FirebaseAnalytics.Param.TRANSACTION_ID to tripId,
            FirebaseAnalytics.Param.VALUE to amount,
            FirebaseAnalytics.Param.CURRENCY to currency
        ))
    }

    fun logUserRetention(daysActive: Int, totalSessions: Int) {
        logEvent("user_retention", mapOf(
            "days_active" to daysActive,
            "total_sessions" to totalSessions
        ))
    }

    // Set user properties
    fun setUserProperty(name: String, value: String) {
        firebaseAnalytics.setUserProperty(name, value)
    }

    fun setUserId(userId: String) {
        firebaseAnalytics.setUserId(userId)
    }

    fun setUserProperties(properties: Map<String, String>) {
        properties.forEach { (key, value) ->
            firebaseAnalytics.setUserProperty(key, value)
        }
    }
}
