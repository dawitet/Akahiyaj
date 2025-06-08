package com.dawitf.akahidegn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.dawitf.akahidegn.Group
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

/**
 * Performance and Memory Management Components
 * Optimizes the app for smooth performance and efficient memory usage
 */

/**
 * Memory-efficient group list state management
 */
@Composable
fun rememberOptimizedGroupState(
    groups: List<Group>
): OptimizedGroupState {
    val groupState = remember { OptimizedGroupState() }
    
    LaunchedEffect(groups) {
        groupState.updateGroups(groups)
    }
    
    return groupState
}

class OptimizedGroupState {
    private var _visibleGroups by mutableStateOf<List<Group>>(emptyList())
    private var _allGroups by mutableStateOf<List<Group>>(emptyList())
    private val pageSize = 20
    private var currentPage = 0
    
    val visibleGroups: List<Group> get() = _visibleGroups
    val hasMoreGroups: Boolean get() = _visibleGroups.size < _allGroups.size
    
    fun updateGroups(groups: List<Group>) {
        _allGroups = groups
        currentPage = 0
        _visibleGroups = groups.take(pageSize)
    }
    
    fun loadMore() {
        if (hasMoreGroups) {
            currentPage++
            val startIndex = currentPage * pageSize
            val endIndex = minOf(startIndex + pageSize, _allGroups.size)
            _visibleGroups = _allGroups.take(endIndex)
        }
    }
    
    fun refresh() {
        currentPage = 0
        _visibleGroups = _allGroups.take(pageSize)
    }
}

/**
 * Smart scroll state that automatically loads more content
 */
@Composable
fun rememberAutoLoadingScrollState(
    onLoadMore: () -> Unit,
    loadThreshold: Int = 3
): LazyListState {
    val scrollState = rememberLazyListState()
    
    LaunchedEffect(scrollState) {
        snapshotFlow {
            val layoutInfo = scrollState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            
            lastVisibleItemIndex > (totalItemsNumber - loadThreshold)
        }
            .distinctUntilChanged()
            .drop(1) // Skip initial emission
            .collect { shouldLoadMore ->
                if (shouldLoadMore) {
                    onLoadMore()
                }
            }
    }
    
    return scrollState
}

/**
 * Lifecycle-aware component state management
 */
@Composable
fun <T> rememberLifecycleAwareState(
    initialValue: T,
    onPause: (T) -> Unit = {},
    onResume: (T) -> Unit = {}
): MutableState<T> {
    val state = remember { mutableStateOf(initialValue) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> onPause(state.value)
                Lifecycle.Event.ON_RESUME -> onResume(state.value)
                else -> {}
            }
        }
        
        lifecycle.addObserver(observer)
        
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
    
    return state
}

/**
 * Image loading optimization with caching
 */
@Composable
fun rememberImageCacheState(): ImageCacheState {
    return remember { ImageCacheState() }
}

class ImageCacheState {
    private val cache = mutableMapOf<String, Any>()
    private val maxCacheSize = 50
    
    fun getCachedImage(url: String): Any? {
        return cache[url]
    }
    
    fun cacheImage(url: String, image: Any) {
        if (cache.size >= maxCacheSize) {
            // Remove oldest entry (simple LRU implementation)
            val oldestKey = cache.keys.first()
            cache.remove(oldestKey)
        }
        cache[url] = image
    }
    
    fun clearCache() {
        cache.clear()
    }
}

/**
 * Animation performance optimization
 */
@Composable
fun rememberOptimizedAnimationSpec(): AnimationSpec<Float> {
    return remember {
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }
}

/**
 * Memory usage monitoring (for debugging)
 */
@Composable
fun MemoryMonitor(
    enabled: Boolean = false,
    onMemoryInfo: (MemoryInfo) -> Unit = {}
) {
    if (!enabled) return
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1000) // Check every second
        
        val runtime = Runtime.getRuntime()
        val memoryInfo = MemoryInfo(
            totalMemory = runtime.totalMemory(),
            freeMemory = runtime.freeMemory(),
            maxMemory = runtime.maxMemory(),
            usedMemory = runtime.totalMemory() - runtime.freeMemory()
        )
        
        onMemoryInfo(memoryInfo)
    }
}

data class MemoryInfo(
    val totalMemory: Long,
    val freeMemory: Long,
    val maxMemory: Long,
    val usedMemory: Long
) {
    val usagePercentage: Float get() = (usedMemory.toFloat() / maxMemory.toFloat()) * 100f
}

/**
 * Debounced state for search optimization
 */
@Composable
fun <T> rememberDebouncedState(
    value: T,
    delayMillis: Long = 300L
): State<T> {
    val debouncedValue = remember { mutableStateOf(value) }
    
    LaunchedEffect(value) {
        kotlinx.coroutines.delay(delayMillis)
        debouncedValue.value = value
    }
    
    return debouncedValue
}

/**
 * Smart recomposition optimization
 */
@Composable
fun <T> rememberStableState(
    value: T,
    areEqual: (T, T) -> Boolean = { a, b -> a == b }
): State<T> {
    val state = remember { mutableStateOf(value) }
    
    LaunchedEffect(value) {
        if (!areEqual(state.value, value)) {
            state.value = value
        }
    }
    
    return state
}

/**
 * Performance metrics collection
 */
class PerformanceMetrics {
    private val frameRenderTimes = mutableListOf<Long>()
    private val maxSamples = 100
    
    fun recordFrameTime(startTime: Long, endTime: Long) {
        val renderTime = endTime - startTime
        
        if (frameRenderTimes.size >= maxSamples) {
            frameRenderTimes.removeAt(0)
        }
        frameRenderTimes.add(renderTime)
    }
    
    fun getAverageFrameTime(): Double {
        return if (frameRenderTimes.isNotEmpty()) {
            frameRenderTimes.average()
        } else 0.0
    }
    
    fun getFPS(): Double {
        val avgFrameTime = getAverageFrameTime()
        return if (avgFrameTime > 0) {
            1000.0 / avgFrameTime
        } else 0.0
    }
    
    fun reset() {
        frameRenderTimes.clear()
    }
}

@Composable
fun rememberPerformanceMetrics(): PerformanceMetrics {
    return remember { PerformanceMetrics() }
}

/**
 * Resource cleanup utilities
 */
@Composable
fun ResourceCleanup(
    onCleanup: () -> Unit
) {
    DisposableEffect(Unit) {
        onDispose {
            onCleanup()
        }
    }
}

/**
 * Advanced performance utilities for memory and resource optimization
 */

/**
 * Optimized collection utilities for better memory management
 */
class OptimizedCollections {
    companion object {
        /**
         * Create a memory-efficient list with limited capacity
         */
        fun <T> createBoundedList(maxSize: Int): MutableList<T> {
            return object : ArrayList<T>() {
                override fun add(element: T): Boolean {
                    if (size >= maxSize) {
                        removeAt(0) // Remove oldest element
                    }
                    return super.add(element)
                }
            }
        }
        
        /**
         * Create a LRU cache with automatic cleanup
         */
        fun <K, V> createLRUCache(maxSize: Int): MutableMap<K, V> {
            return object : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
                    return size > maxSize
                }
            }
        }
    }
}

/**
 * Resource cleanup helper for preventing memory leaks
 */
object ResourceCleanupHelper {
    /**
     * Execute cleanup tasks safely
     */
    fun safeCleanup(vararg cleanupTasks: () -> Unit) {
        cleanupTasks.forEach { task ->
            try {
                task()
            } catch (e: Exception) {
                // Log error but don't crash the app
                // In production, this would be logged to analytics
            }
        }
    }
    
    /**
     * Clean up collections efficiently
     */
    fun <T> cleanupCollection(collection: MutableCollection<T>, 
                             shouldKeep: (T) -> Boolean = { false }) {
        val iterator = collection.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (!shouldKeep(item)) {
                iterator.remove()
            }
        }
    }
}
