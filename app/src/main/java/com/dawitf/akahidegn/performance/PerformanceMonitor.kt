package com.dawitf.akahidegn.performance

import android.content.Context
import android.os.SystemClock
import com.dawitf.akahidegn.analytics.AnalyticsService
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.AddTrace
import com.google.firebase.perf.metrics.Trace
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsService: AnalyticsService
) {
    
    private val performanceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeTraces = ConcurrentHashMap<String, Trace>()
    private val startTimes = ConcurrentHashMap<String, Long>()
    
    // Screen Performance Monitoring
    fun startScreenTrace(screenName: String) {
        val traceName = "screen_$screenName"
        val trace = FirebasePerformance.getInstance().newTrace(traceName)
        trace.start()
        activeTraces[traceName] = trace
        startTimes[traceName] = SystemClock.elapsedRealtime()
    }
    
    fun stopScreenTrace(screenName: String) {
        val traceName = "screen_$screenName"
        activeTraces[traceName]?.let { trace ->
            trace.stop()
            activeTraces.remove(traceName)
            
            val startTime = startTimes.remove(traceName)
            if (startTime != null) {
                val loadTime = SystemClock.elapsedRealtime() - startTime
                analyticsService.trackScreenLoadTime(screenName, loadTime)
            }
        }
    }
    
    // Network Performance Monitoring
    fun startNetworkTrace(endpoint: String): String {
        val traceId = "${endpoint}_${System.currentTimeMillis()}"
        val trace = FirebasePerformance.getInstance().newTrace("network_$endpoint")
        trace.start()
        activeTraces[traceId] = trace
        startTimes[traceId] = SystemClock.elapsedRealtime()
        return traceId
    }
    
    fun stopNetworkTrace(traceId: String, endpoint: String, success: Boolean, responseSize: Long = 0) {
        activeTraces[traceId]?.let { trace ->
            if (responseSize > 0) {
                trace.putMetric("response_size", responseSize)
            }
            trace.putAttribute("success", success.toString())
            trace.stop()
            activeTraces.remove(traceId)
            
            val startTime = startTimes.remove(traceId)
            if (startTime != null) {
                val latency = SystemClock.elapsedRealtime() - startTime
                analyticsService.trackNetworkLatency(endpoint, latency, success)
            }
        }
    }
    
    // Memory Performance Monitoring
    fun monitorMemoryUsage() {
        performanceScope.launch {
            while (true) {
                try {
                    val runtime = Runtime.getRuntime()
                    val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                    val maxMemory = runtime.maxMemory()
                    val memoryUsagePercent = (usedMemory.toFloat() / maxMemory.toFloat() * 100).toInt()
                    
                    if (memoryUsagePercent > 80) {
                        analyticsService.trackCustomEvent("memory_warning", mapOf(
                            "memory_usage_percent" to memoryUsagePercent,
                            "used_memory_mb" to (usedMemory / 1024 / 1024),
                            "max_memory_mb" to (maxMemory / 1024 / 1024)
                        ))
                    }
                    
                    delay(30000) // Check every 30 seconds
                } catch (e: Exception) {
                    analyticsService.logError(e, "memory_monitoring")
                    delay(60000) // Wait longer if there's an error
                }
            }
        }
    }
    
    // Database Performance Monitoring
    @AddTrace(name = "database_query", enabled = true)
    suspend fun <T> measureDatabaseOperation(
        operationName: String,
        operation: suspend () -> T
    ): T {
        val startTime = SystemClock.elapsedRealtime()
        return try {
            val result = operation()
            val duration = SystemClock.elapsedRealtime() - startTime
            
            analyticsService.trackCustomEvent("database_performance", mapOf(
                "operation" to operationName,
                "duration_ms" to duration,
                "success" to true
            ))
            
            result
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            
            analyticsService.trackCustomEvent("database_performance", mapOf(
                "operation" to operationName,
                "duration_ms" to duration,
                "success" to false,
                "error" to e.javaClass.simpleName
            ))
            
            throw e
        }
    }
    
    // UI Performance Monitoring
    fun measureCompositionTime(composableName: String, block: () -> Unit) {
        val startTime = SystemClock.elapsedRealtime()
        block()
        val duration = SystemClock.elapsedRealtime() - startTime
        
        if (duration > 16) { // More than one frame at 60fps
            analyticsService.trackCustomEvent("ui_performance", mapOf(
                "composable" to composableName,
                "composition_time_ms" to duration,
                "frames_dropped" to (duration / 16).toInt()
            ))
        }
    }
    
    // Cold Start Monitoring
    fun trackColdStart() {
        performanceScope.launch {
            delay(1000) // Wait for app to settle
            
            analyticsService.trackCustomEvent("app_startup", mapOf(
                "startup_type" to "cold",
                "timestamp" to System.currentTimeMillis()
            ))
        }
    }
    
    // Crash Monitoring with Context
    fun recordBreadcrumb(message: String, category: String = "general") {
        analyticsService.logError("Breadcrumb: [$category] $message", "breadcrumb")
    }
    
    // Resource Usage Monitoring
    fun startResourceMonitoring() {
        performanceScope.launch {
            while (true) {
                try {
                    monitorCPUUsage()
                    monitorBatteryUsage()
                    delay(60000) // Check every minute
                } catch (e: Exception) {
                    analyticsService.logError(e, "resource_monitoring")
                    delay(120000) // Wait longer if there's an error
                }
            }
        }
    }
    
    private fun monitorCPUUsage() {
        // This is a simplified CPU monitoring
        // In a real app, you might use more sophisticated monitoring
        val threadCount = Thread.activeCount()
        
        if (threadCount > 50) {
            analyticsService.trackCustomEvent("performance_warning", mapOf(
                "type" to "high_thread_count",
                "thread_count" to threadCount
            ))
        }
    }
    
    private fun monitorBatteryUsage() {
        // Battery monitoring would require more complex implementation
        // This is a placeholder for battery usage tracking
        analyticsService.trackCustomEvent("battery_check", mapOf(
            "timestamp" to System.currentTimeMillis()
        ))
    }
    
    // Enhanced Performance Monitoring
    private var lastFrameTime = 0L
    private var frameDropCount = 0
    private var totalFrames = 0
    
    /**
     * Monitor frame rate and detect frame drops
     */
    fun trackFrameRate() {
        val currentTime = SystemClock.elapsedRealtime()
        if (lastFrameTime > 0) {
            val frameDuration = currentTime - lastFrameTime
            totalFrames++
            
            // Consider anything over 20ms as a dropped frame (60fps = ~16.67ms per frame)
            if (frameDuration > 20) {
                frameDropCount++
                
                // Log significant frame drops
                if (frameDuration > 50) {
                    analyticsService.trackCustomEvent("severe_frame_drop", mapOf(
                        "duration_ms" to frameDuration,
                        "drop_ratio" to (frameDropCount.toFloat() / totalFrames.toFloat())
                    ))
                }
            }
            
            // Report every 1000 frames
            if (totalFrames % 1000 == 0) {
                val dropPercentage = (frameDropCount.toFloat() / totalFrames.toFloat()) * 100
                analyticsService.trackCustomEvent("frame_performance", mapOf(
                    "total_frames" to totalFrames,
                    "dropped_frames" to frameDropCount,
                    "drop_percentage" to dropPercentage
                ))
            }
        }
        lastFrameTime = currentTime
    }
    
    /**
     * Track app startup time and report performance metrics
     */
    fun trackAppStartup(startupType: String) {
        analyticsService.trackCustomEvent("app_startup", mapOf(
            "startup_type" to startupType,
            "timestamp" to System.currentTimeMillis()
        ))
    }
    
    // Cleanup
    fun cleanup() {
        activeTraces.values.forEach { trace ->
            try {
                trace.stop()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        activeTraces.clear()
        startTimes.clear()
    }
}
