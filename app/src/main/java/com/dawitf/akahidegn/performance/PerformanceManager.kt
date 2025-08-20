package com.dawitf.akahidegn.performance

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance monitoring and optimization manager
 * Tracks memory usage, performance metrics, and provides optimization suggestions
 */
@Singleton
class PerformanceManager @Inject constructor(
    private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    private val _memoryInfo = MutableStateFlow(MemoryInfo())
    val memoryInfo: StateFlow<MemoryInfo> = _memoryInfo.asStateFlow()
    
    private var isMonitoring = false

    /**
     * Start continuous performance monitoring
     */
    suspend fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        
        while (isMonitoring) {
            updatePerformanceMetrics()
            updateMemoryInfo()
            delay(5000) // Update every 5 seconds
        }
    }

    /**
     * Stop performance monitoring
     */
    fun stopMonitoring() {
        isMonitoring = false
    }

    /**
     * Update performance metrics
     */
    private fun updatePerformanceMetrics() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryUsagePercent = (usedMemory.toDouble() / maxMemory.toDouble() * 100).toFloat()
        
        val currentTime = System.currentTimeMillis()
        val metrics = _performanceMetrics.value
        
        val newMetrics = metrics.copy(
            memoryUsagePercent = memoryUsagePercent,
            usedMemoryMB = (usedMemory / (1024 * 1024)).toInt(),
            maxMemoryMB = (maxMemory / (1024 * 1024)).toInt(),
            lastUpdateTime = currentTime,
            isMemoryPressureHigh = memoryUsagePercent > 85f
        )
        
        _performanceMetrics.value = newMetrics
    }

    /**
     * Update system memory information
     */
    private fun updateMemoryInfo() {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val info = MemoryInfo(
            availableMemoryMB = (memInfo.availMem / (1024 * 1024)).toInt(),
            totalMemoryMB = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                (memInfo.totalMem / (1024 * 1024)).toInt()
            } else {
                0 // Not available on older versions
            },
            isLowMemory = memInfo.lowMemory,
            memoryThreshold = (memInfo.threshold / (1024 * 1024)).toInt()
        )
        
        _memoryInfo.value = info
    }

    /**
     * Force garbage collection and memory optimization
     */
    fun optimizeMemory() {
        System.gc()
        updatePerformanceMetrics()
        updateMemoryInfo()
    }

    /**
     * Get performance optimization suggestions
     */
    fun getOptimizationSuggestions(): List<OptimizationSuggestion> {
        val suggestions = mutableListOf<OptimizationSuggestion>()
        val metrics = _performanceMetrics.value
        val memInfo = _memoryInfo.value
        
        if (metrics.memoryUsagePercent > 80f) {
            suggestions.add(
                OptimizationSuggestion(
                    type = OptimizationType.MEMORY,
                    severity = SuggestionSeverity.HIGH,
                    title = "High Memory Usage",
                    description = "App is using ${metrics.memoryUsagePercent.toInt()}% of available memory",
                    action = "Clear image cache or reduce concurrent operations"
                )
            )
        }
        
        if (memInfo.isLowMemory) {
            suggestions.add(
                OptimizationSuggestion(
                    type = OptimizationType.SYSTEM,
                    severity = SuggestionSeverity.CRITICAL,
                    title = "System Low Memory",
                    description = "Device is running low on memory",
                    action = "Close other apps or restart device"
                )
            )
        }
        
        if (metrics.usedMemoryMB > 200) {
            suggestions.add(
                OptimizationSuggestion(
                    type = OptimizationType.CACHE,
                    severity = SuggestionSeverity.MEDIUM,
                    title = "Large Memory Footprint",
                    description = "App is using ${metrics.usedMemoryMB}MB of memory",
                    action = "Clear caches to free up memory"
                )
            )
        }
        
        return suggestions
    }

    /**
     * Composable for monitoring performance in real-time
     */
    @Composable
    fun PerformanceMonitor(
        content: @Composable (PerformanceMetrics, MemoryInfo) -> Unit
    ) {
        val metrics by performanceMetrics.collectAsState()
        val memInfo by memoryInfo.collectAsState()
        
        LaunchedEffect(Unit) {
            startMonitoring()
        }
        
        DisposableEffect(Unit) {
            onDispose {
                stopMonitoring()
            }
        }
        
        content(metrics, memInfo)
    }

    data class PerformanceMetrics(
        val memoryUsagePercent: Float = 0f,
        val usedMemoryMB: Int = 0,
        val maxMemoryMB: Int = 0,
        val lastUpdateTime: Long = 0L,
        val isMemoryPressureHigh: Boolean = false
    )

    data class MemoryInfo(
        val availableMemoryMB: Int = 0,
        val totalMemoryMB: Int = 0,
        val isLowMemory: Boolean = false,
        val memoryThreshold: Int = 0
    )

    data class OptimizationSuggestion(
        val type: OptimizationType,
        val severity: SuggestionSeverity,
        val title: String,
        val description: String,
        val action: String
    )

    enum class OptimizationType {
        MEMORY, CACHE, NETWORK, SYSTEM, UI
    }

    enum class SuggestionSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
