package com.dawitf.akahidegn.performance

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network performance optimization manager
 * Handles request batching, caching, and network-aware operations
 */
@Singleton
class NetworkOptimizationManager @Inject constructor(
    private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _networkState = MutableStateFlow(NetworkState())
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    private val requestQueue = mutableListOf<NetworkRequest>()
    private val responseCache = mutableMapOf<String, CacheEntry>()
    private val maxCacheSize = 100
    private val cacheExpirationMs = 5 * 60 * 1000L // 5 minutes
    
    private var batchProcessingActive = false

    /**
     * Start network monitoring and optimization
     */
    suspend fun startOptimization() {
        updateNetworkState()
        startBatchProcessing()
    }

    /**
     * Update current network state
     */
    private fun updateNetworkState() {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        val state = when {
            networkCapabilities == null -> NetworkState(
                isConnected = false,
                connectionType = ConnectionType.NONE,
                isMetered = false,
                signalStrength = SignalStrength.NONE
            )
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkState(
                isConnected = true,
                connectionType = ConnectionType.WIFI,
                isMetered = false,
                signalStrength = getSignalStrength(networkCapabilities)
            )
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkState(
                isConnected = true,
                connectionType = ConnectionType.CELLULAR,
                isMetered = true,
                signalStrength = getSignalStrength(networkCapabilities)
            )
            else -> NetworkState(
                isConnected = true,
                connectionType = ConnectionType.OTHER,
                isMetered = connectivityManager.isActiveNetworkMetered,
                signalStrength = SignalStrength.UNKNOWN
            )
        }
        
        _networkState.value = state
    }

    /**
     * Get signal strength from network capabilities
     */
    private fun getSignalStrength(networkCapabilities: NetworkCapabilities): SignalStrength {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (networkCapabilities.signalStrength) {
                in -50..-1 -> SignalStrength.EXCELLENT
                in -70..-51 -> SignalStrength.GOOD
                in -85..-71 -> SignalStrength.FAIR
                in -100..-86 -> SignalStrength.POOR
                else -> SignalStrength.UNKNOWN
            }
        } else {
            SignalStrength.UNKNOWN
        }
    }

    /**
     * Add request to optimization queue
     */
    fun queueRequest(request: NetworkRequest) {
        // Check cache first
        val cacheKey = generateCacheKey(request)
        val cachedEntry = responseCache[cacheKey]
        
        if (cachedEntry != null && !isCacheExpired(cachedEntry)) {
            request.onSuccess(cachedEntry.data)
            return
        }
        
        // Add to queue for batch processing
        synchronized(requestQueue) {
            requestQueue.add(request)
        }
    }

    /**
     * Start batch processing of network requests
     */
    private suspend fun startBatchProcessing() {
        if (batchProcessingActive) return
        batchProcessingActive = true
        
        while (batchProcessingActive) {
            processBatch()
            delay(getOptimalBatchDelay())
        }
    }

    /**
     * Process batch of network requests
     */
    private suspend fun processBatch() {
        val batch = synchronized(requestQueue) {
            val currentBatch = requestQueue.toList()
            requestQueue.clear()
            currentBatch
        }
        
        if (batch.isEmpty()) return
        
        val networkState = _networkState.value
        
        // Prioritize requests based on network conditions
        val prioritizedBatch = when {
            !networkState.isConnected -> {
                // Handle offline requests
                batch.filter { it.allowOffline }
            }
            networkState.isMetered -> {
                // Prioritize critical requests on metered connections
                batch.sortedBy { if (it.priority == RequestPriority.HIGH) 0 else 1 }
                    .take(getMaxRequestsForMetered())
            }
            else -> batch
        }
        
        // Execute requests
        prioritizedBatch.forEach { request ->
            try {
                executeRequest(request)
            } catch (e: Exception) {
                request.onError(e)
            }
        }
    }

    /**
     * Execute individual network request
     */
    private suspend fun executeRequest(request: NetworkRequest) {
        // Simulate network request execution
        // In a real implementation, use OkHttp, Retrofit, or similar
        
        delay(100) // Simulate network delay
        
        val response = when (request.type) {
            RequestType.GET -> "GET response for ${request.url}"
            RequestType.POST -> "POST response for ${request.url}"
            RequestType.PUT -> "PUT response for ${request.url}"
            RequestType.DELETE -> "DELETE response for ${request.url}"
        }
        
        // Cache successful responses
        val cacheKey = generateCacheKey(request)
        responseCache[cacheKey] = CacheEntry(
            data = response,
            timestamp = System.currentTimeMillis()
        )
        
        // Manage cache size
        if (responseCache.size > maxCacheSize) {
            cleanupCache()
        }
        
        request.onSuccess(response)
    }

    /**
     * Generate cache key for request
     */
    private fun generateCacheKey(request: NetworkRequest): String {
        return "${request.type}_${request.url}_${request.parameters.hashCode()}"
    }

    /**
     * Check if cache entry is expired
     */
    private fun isCacheExpired(entry: CacheEntry): Boolean {
        return System.currentTimeMillis() - entry.timestamp > cacheExpirationMs
    }

    /**
     * Clean up expired cache entries
     */
    private fun cleanupCache() {
        val currentTime = System.currentTimeMillis()
        val expiredKeys = responseCache.entries
            .filter { currentTime - it.value.timestamp > cacheExpirationMs }
            .map { it.key }
        
        expiredKeys.forEach { responseCache.remove(it) }
        
        // If still too large, remove oldest entries
        while (responseCache.size > maxCacheSize) {
            val oldestKey = responseCache.entries
                .minByOrNull { it.value.timestamp }?.key
            oldestKey?.let { responseCache.remove(it) }
        }
    }

    /**
     * Get optimal batch delay based on network conditions
     */
    private fun getOptimalBatchDelay(): Long {
        val networkState = _networkState.value
        return when {
            !networkState.isConnected -> 5000L // 5 seconds when offline
            networkState.isMetered -> 2000L // 2 seconds on metered
            networkState.signalStrength == SignalStrength.POOR -> 3000L // 3 seconds on poor signal
            else -> 1000L // 1 second on good connections
        }
    }

    /**
     * Get maximum requests for metered connections
     */
    private fun getMaxRequestsForMetered(): Int {
        return when (_networkState.value.signalStrength) {
            SignalStrength.EXCELLENT -> 10
            SignalStrength.GOOD -> 7
            SignalStrength.FAIR -> 5
            SignalStrength.POOR -> 3
            else -> 1
        }
    }

    /**
     * Clear all cached responses
     */
    fun clearCache() {
        responseCache.clear()
    }

    /**
     * Stop network optimization
     */
    fun stopOptimization() {
        batchProcessingActive = false
    }

    data class NetworkState(
        val isConnected: Boolean = false,
        val connectionType: ConnectionType = ConnectionType.NONE,
        val isMetered: Boolean = false,
        val signalStrength: SignalStrength = SignalStrength.UNKNOWN
    )

    enum class ConnectionType {
        NONE, WIFI, CELLULAR, OTHER
    }

    enum class SignalStrength {
        NONE, POOR, FAIR, GOOD, EXCELLENT, UNKNOWN
    }

    data class NetworkRequest(
        val type: RequestType,
        val url: String,
        val parameters: Map<String, Any> = emptyMap(),
        val priority: RequestPriority = RequestPriority.NORMAL,
        val allowOffline: Boolean = false,
        val onSuccess: (String) -> Unit,
        val onError: (Exception) -> Unit
    )

    enum class RequestType {
        GET, POST, PUT, DELETE
    }

    enum class RequestPriority {
        LOW, NORMAL, HIGH
    }

    private data class CacheEntry(
        val data: String,
        val timestamp: Long
    )
}
