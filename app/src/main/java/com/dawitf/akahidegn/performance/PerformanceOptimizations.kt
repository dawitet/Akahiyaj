package com.dawitf.akahidegn.performance

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * LRU Cache for expensive calculations to avoid recomputation
 */
object PerformanceCache {
    
    // Distance calculation cache - key: "lat1,lng1,lat2,lng2", value: distance in meters
    private val distanceCache = LruCache<String, Double>(200)
    
    // String formatting cache for timestamps, distances, etc.
    private val formatCache = LruCache<String, String>(100)
    
    // Search result cache for filtered groups
    private val searchResultCache = LruCache<String, List<String>>(50)
    
    // Image size calculation cache
    private val imageSizeCache = LruCache<String, Pair<Int, Int>>(100)
    
    fun cacheDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double, distance: Double) {
        val key = "${lat1},${lng1},${lat2},${lng2}"
        distanceCache.put(key, distance)
    }
    
    fun getCachedDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double? {
        val key = "${lat1},${lng1},${lat2},${lng2}"
        return distanceCache.get(key)
    }
    
    fun cacheFormat(key: String, formatted: String) {
        formatCache.put(key, formatted)
    }
    
    fun getCachedFormat(key: String): String? {
        return formatCache.get(key)
    }
    
    fun cacheSearchResult(query: String, result: List<String>) {
        searchResultCache.put(query, result)
    }
    
    fun getCachedSearchResult(query: String): List<String>? {
        return searchResultCache.get(query)
    }
    
    fun cacheImageSize(url: String, width: Int, height: Int) {
        imageSizeCache.put(url, Pair(width, height))
    }
    
    fun getCachedImageSize(url: String): Pair<Int, Int>? {
        return imageSizeCache.get(url)
    }
    
    fun clearAll() {
        distanceCache.evictAll()
        formatCache.evictAll()
        searchResultCache.evictAll()
        imageSizeCache.evictAll()
    }
}

/**
 * Optimized distance calculation with caching
 */
@Composable
fun rememberDistanceCalculation(
    userLat: Double?,
    userLng: Double?,
    targetLat: Double?,
    targetLng: Double?
): Double? {
    return remember(userLat, userLng, targetLat, targetLng) {
        if (userLat != null && userLng != null && targetLat != null && targetLng != null) {
            // Check cache first
            PerformanceCache.getCachedDistance(userLat, userLng, targetLat, targetLng) ?: run {
                // Calculate and cache
                val distance = calculateHaversineDistance(userLat, userLng, targetLat, targetLng)
                PerformanceCache.cacheDistance(userLat, userLng, targetLat, targetLng, distance)
                distance
            }
        } else null
    }
}

/**
 * Fast distance calculation using Haversine formula
 */
private fun calculateHaversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val R = 6371000.0 // Earth's radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return R * c
}

/**
 * Optimized time formatting with caching
 */
@Composable
fun rememberTimeFormat(timestamp: Long?): String {
    return remember(timestamp) {
        timestamp?.let { ts ->
            val key = "time_$ts"
            PerformanceCache.getCachedFormat(key) ?: run {
                val minutes = (System.currentTimeMillis() - ts) / (60 * 1000)
                val formatted = when {
                    minutes < 1 -> "Just now"
                    minutes < 60 -> "${minutes}m ago"
                    else -> "${minutes / 60}h ago"
                }
                PerformanceCache.cacheFormat(key, formatted)
                formatted
            }
        } ?: "Unknown"
    }
}

/**
 * Optimized distance formatting with caching
 */
@Composable
fun rememberDistanceFormat(distance: Double?): String {
    return remember(distance) {
        distance?.let { dist ->
            val key = "dist_${dist.toInt()}"
            PerformanceCache.getCachedFormat(key) ?: run {
                val formatted = when {
                    dist < 1000 -> "${dist.toInt()}m"
                    else -> String.format("%.1fkm", dist / 1000)
                }
                PerformanceCache.cacheFormat(key, formatted)
                formatted
            }
        } ?: ""
    }
}

/**
 * Memoized list filtering using derivedStateOf
 */
@Composable
fun <T> rememberFilteredList(
    list: List<T>,
    predicate: (T) -> Boolean
): List<T> {
    return remember(list) {
        derivedStateOf {
            list.filter(predicate)
        }
    }.value
}

/**
 * Memoized list sorting using derivedStateOf
 */
@Composable
fun <T> rememberSortedList(
    list: List<T>,
    comparator: Comparator<T>
): List<T> {
    return remember(list, comparator) {
        derivedStateOf {
            list.sortedWith(comparator)
        }
    }.value
}

/**
 * Optimized Dp to Px conversion with caching
 */
@Composable
fun rememberDpToPx(dp: Dp): Float {
    val density = LocalDensity.current
    return remember(dp, density.density) {
        with(density) { dp.toPx() }
    }
}

/**
 * Memory-optimized state holder for complex objects
 */
@Stable
class MemoizedState<T>(
    initialValue: T,
    private val keySelector: (T) -> Any
) {
    private var _value by mutableStateOf(initialValue)
    private var _key by mutableStateOf(keySelector(initialValue))
    
    var value: T
        get() = _value
        set(newValue) {
            val newKey = keySelector(newValue)
            if (newKey != _key) {
                _value = newValue
                _key = newKey
            }
        }
}

/**
 * Create a memoized state that only updates when the key changes
 */
@Composable
fun <T> rememberMemoizedState(
    value: T,
    keySelector: (T) -> Any
): T {
    val memoizedState = remember { MemoizedState(value, keySelector) }
    memoizedState.value = value
    return memoizedState.value
}

/**
 * Background task executor for expensive operations
 */
object BackgroundTaskExecutor {
    private val scope = CoroutineScope(Dispatchers.Default)
    
    fun <T> executeInBackground(
        task: suspend () -> T,
        onResult: (T) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        scope.launch {
            try {
                val result = task()
                withContext(Dispatchers.Main) {
                    onResult(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }
}

/**
 * Stable reference for lambda functions to prevent recomposition
 */
@Composable
fun <T> rememberStableCallback(callback: T): T where T : Function<*> {
    return remember { callback }
}

/**
 * Optimized list state management with automatic memoization
 */
@Composable
fun <T> rememberOptimizedListState(
    list: List<T>,
    keySelector: (T) -> Any = { it.hashCode() }
): List<T> {
    return remember(list.size, list.map(keySelector)) {
        list
    }
}
