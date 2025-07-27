package com.dawitf.akahidegn.analytics

import android.content.Context
import android.util.Log
import com.dawitf.akahidegn.ui.components.AnimationType
import com.dawitf.akahidegn.ui.viewmodels.AnimationAnalyticsEvent
import com.dawitf.akahidegn.ui.viewmodels.AnimationSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Analytics manager for tracking animation interactions and performance
 * Provides insights into user behavior and animation effectiveness
 */
class AnimationAnalyticsManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _analyticsEvents = MutableSharedFlow<AnimationAnalyticsEvent>()
    /** SharedFlow of analytics events */
    val analyticsEvents: SharedFlow<AnimationAnalyticsEvent> = _analyticsEvents.asSharedFlow()

    private val _performanceMetrics = MutableSharedFlow<AnimationPerformanceMetric>()
    /** SharedFlow of performance metrics */
    val performanceMetrics: SharedFlow<AnimationPerformanceMetric> = _performanceMetrics.asSharedFlow()

    // Analytics data storage
    private val eventHistory = mutableListOf<AnimationAnalyticsEvent>()
    private val performanceHistory = mutableListOf<AnimationPerformanceMetric>()

    /**
     * Track animation event
     */
    fun trackAnimationEvent(event: AnimationAnalyticsEvent) {
        scope.launch {
            eventHistory.add(event)
            _analyticsEvents.emit(event)

            // Log to console in debug mode
            logEvent(event)
        }
    }

    /**
     * Track animation performance
     */
    fun trackAnimationPerformance(metric: AnimationPerformanceMetric) {
        scope.launch {
            performanceHistory.add(metric)
            _performanceMetrics.emit(metric)

            // Log performance metric
            logPerformanceMetric(metric)
        }
    }

    /**
     * Track animation completion time
     */
    fun trackAnimationDuration(
        animationType: AnimationType,
        component: String,
        duration: Long,
        success: Boolean
    ) {
        val metric = AnimationPerformanceMetric.AnimationDuration(
            animationType = animationType,
            component = component,
            duration = duration,
            success = success,
            timestamp = System.currentTimeMillis()
        )

        trackAnimationPerformance(metric)
    }

    /**
     * Get analytics summary
     */
    fun getAnalyticsSummary(): AnimationAnalyticsSummary {
        val events = eventHistory.toList()
        val metrics = performanceHistory.toList()

        return AnimationAnalyticsSummary(
            totalNotifications = events.filterIsInstance<AnimationAnalyticsEvent.NotificationShown>().size,
            notificationsByType = events.filterIsInstance<AnimationAnalyticsEvent.NotificationShown>()
                .groupBy { it.type }
                .mapValues { it.value.size },
            averageAnimationDuration = metrics.filterIsInstance<AnimationPerformanceMetric.AnimationDuration>()
                .map { it.duration }
                .average()
                .takeIf { !it.isNaN() } ?: 0.0,
            successfulAnimations = metrics.filterIsInstance<AnimationPerformanceMetric.AnimationDuration>()
                .count { it.success },
            failedAnimations = metrics.filterIsInstance<AnimationPerformanceMetric.AnimationDuration>()
                .count { !it.success },
            totalEventCount = events.size,
            dateRange = if (events.isNotEmpty()) {
                val timestamps = events.map {
                    when (it) {
                        is AnimationAnalyticsEvent.NotificationShown -> it.timestamp
                        is AnimationAnalyticsEvent.NotificationDismissed -> it.timestamp
                    }
                }
                timestamps.minOrNull()!! to timestamps.maxOrNull()!!
            } else {
                0L to 0L
            }
        )
    }

    /**
     * Export analytics data
     */
    fun exportAnalyticsData(): String {
        val summary = getAnalyticsSummary()

        return buildString {
            appendLine("Animation Analytics Report")
            appendLine("=".repeat(50))
            appendLine("Total Notifications: ${summary.totalNotifications}")
            appendLine("Notifications by Type:")
            summary.notificationsByType.forEach { (type, count) ->
                appendLine("  $type: $count")
            }
            appendLine("Average Animation Duration: ${summary.averageAnimationDuration}ms")
            appendLine("Successful Animations: ${summary.successfulAnimations}")
            appendLine("Failed Animations: ${summary.failedAnimations}")
            appendLine("Total Events: ${summary.totalEventCount}")
            appendLine("Date Range: ${formatTimestamp(summary.dateRange.first)} - ${formatTimestamp(summary.dateRange.second)}")
        }
    }

    // Private helper methods

    private fun logEvent(event: AnimationAnalyticsEvent) {
        when (event) {
            is AnimationAnalyticsEvent.NotificationShown -> {
                Log.d("AnimationAnalytics", "Notification shown: ${event.type} - ${event.title}")
            }
            is AnimationAnalyticsEvent.NotificationDismissed -> {
                Log.d("AnimationAnalytics", "Notification dismissed: ${event.notificationId}")
            }
        }
    }

    private fun logPerformanceMetric(metric: AnimationPerformanceMetric) {
        when (metric) {
            is AnimationPerformanceMetric.AnimationDuration -> {
                Log.d("AnimationPerformance",
                    "${metric.component} ${metric.animationType} took ${metric.duration}ms (success: ${metric.success})")
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) return "N/A"
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    }
}

/**
 * Animation performance metrics
 */
sealed class AnimationPerformanceMetric {
    /** Abstract timestamp property */
    abstract val timestamp: Long

    /**
     * Animation duration metric
     * @property animationType Type of animation
     * @property component Component name
     * @property duration Duration in milliseconds
     * @property success Whether animation completed successfully
     * @property timestamp Timestamp when metric was recorded
     */
    data class AnimationDuration(
        val animationType: AnimationType,
        val component: String,
        val duration: Long,
        val success: Boolean,
        override val timestamp: Long
    ) : AnimationPerformanceMetric()
}

/**
 * Analytics summary data class
 * @property totalNotifications Total number of notifications shown
 * @property notificationsByType Count of notifications by type
 * @property averageAnimationDuration Average animation duration in milliseconds
 * @property successfulAnimations Count of successful animations
 * @property failedAnimations Count of failed animations
 * @property totalEventCount Total number of events tracked
 * @property dateRange Date range of tracked events (start to end timestamps)
 */
data class AnimationAnalyticsSummary(
    val totalNotifications: Int,
    val notificationsByType: Map<AnimationType, Int>,
    val averageAnimationDuration: Double,
    val successfulAnimations: Int,
    val failedAnimations: Int,
    val totalEventCount: Int,
    val dateRange: Pair<Long, Long>
)

/**
 * Extension functions for easy analytics tracking
 */
fun AnimationAnalyticsManager.trackSuccessShown(title: String) {
    trackAnimationEvent(
        AnimationAnalyticsEvent.NotificationShown(
            type = AnimationType.SUCCESS,
            title = title,
            timestamp = System.currentTimeMillis()
        )
    )
}

fun AnimationAnalyticsManager.trackErrorShown(title: String) {
    trackAnimationEvent(
        AnimationAnalyticsEvent.NotificationShown(
            type = AnimationType.ERROR,
            title = title,
            timestamp = System.currentTimeMillis()
        )
    )
}

fun AnimationAnalyticsManager.trackWarningShown(title: String) {
    trackAnimationEvent(
        AnimationAnalyticsEvent.NotificationShown(
            type = AnimationType.WARNING,
            title = title,
            timestamp = System.currentTimeMillis()
        )
    )
}

fun AnimationAnalyticsManager.trackLoadingShown(title: String) {
    trackAnimationEvent(
        AnimationAnalyticsEvent.NotificationShown(
            type = AnimationType.LOADING,
            title = title,
            timestamp = System.currentTimeMillis()
        )
    )
}
