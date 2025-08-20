package com.dawitf.akahidegn.performance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.WorkerThread
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced image caching system with memory optimization
 * Provides efficient image loading, caching, and memory management
 */
@Singleton
class ImageCacheManager @Inject constructor(
    private val context: Context
) {
    private val memoryCache = mutableMapOf<String, ImageBitmap>()
    private val cacheDir = File(context.cacheDir, "images")
    private val maxMemoryCacheSize = 50 // Maximum number of images in memory
    
    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    /**
     * Load image with caching and lazy loading
     */
    @Composable
    fun LazyImage(
        url: String,
        contentDescription: String? = null,
        placeholder: ImageBitmap? = null
    ): ImageBitmap? {
        var imageBitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
        var isLoading by remember(url) { mutableStateOf(true) }

        LaunchedEffect(url) {
            try {
                imageBitmap = loadImage(url)
            } catch (e: Exception) {
                // Handle error - keep placeholder or null
            } finally {
                isLoading = false
            }
        }

        return when {
            isLoading -> placeholder
            else -> imageBitmap
        }
    }

    /**
     * Load image from cache or network
     */
    suspend fun loadImage(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
        val cacheKey = generateCacheKey(url)
        
        // Check memory cache first
        memoryCache[cacheKey]?.let { return@withContext it }
        
        // Check disk cache
        val cachedFile = File(cacheDir, cacheKey)
        if (cachedFile.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(cachedFile.absolutePath)
                val imageBitmap = bitmap.asImageBitmap()
                
                // Add to memory cache
                addToMemoryCache(cacheKey, imageBitmap)
                return@withContext imageBitmap
            } catch (e: Exception) {
                // Delete corrupted cache file
                cachedFile.delete()
            }
        }
        
        // Load from network (placeholder implementation)
        // In a real app, you'd use a network library like OkHttp or Retrofit
        try {
            val bitmap = loadFromNetwork(url)
            val imageBitmap = bitmap.asImageBitmap()
            
            // Cache to disk
            saveToDiskCache(cacheKey, bitmap)
            
            // Add to memory cache
            addToMemoryCache(cacheKey, imageBitmap)
            
            return@withContext imageBitmap
        } catch (e: Exception) {
            return@withContext null
        }
    }

    /**
     * Add image to memory cache with size management
     */
    private fun addToMemoryCache(key: String, image: ImageBitmap) {
        // Remove oldest items if cache is full
        while (memoryCache.size >= maxMemoryCacheSize) {
            val oldestKey = memoryCache.keys.first()
            memoryCache.remove(oldestKey)
        }
        
        memoryCache[key] = image
    }

    /**
     * Save bitmap to disk cache
     */
    @WorkerThread
    private fun saveToDiskCache(key: String, bitmap: Bitmap) {
        try {
            val file = File(cacheDir, key)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
        } catch (e: IOException) {
            // Handle save error
        }
    }

    /**
     * Generate cache key from URL
     */
    private fun generateCacheKey(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(url.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Placeholder for network loading
     * In a real implementation, replace with actual network loading
     */
    @WorkerThread
    private suspend fun loadFromNetwork(url: String): Bitmap {
        // Placeholder implementation
        // In a real app, use OkHttp, Retrofit, or similar
        throw NotImplementedError("Network loading not implemented in this example")
    }

    /**
     * Clear all caches
     */
    fun clearCache() {
        memoryCache.clear()
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Get cache size information
     */
    fun getCacheInfo(): CacheInfo {
        val memoryCacheSize = memoryCache.size
        val diskCacheFiles = cacheDir.listFiles()?.size ?: 0
        val diskCacheSize = cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        
        return CacheInfo(
            memoryCacheSize = memoryCacheSize,
            diskCacheFiles = diskCacheFiles,
            diskCacheSizeBytes = diskCacheSize
        )
    }

    data class CacheInfo(
        val memoryCacheSize: Int,
        val diskCacheFiles: Int,
        val diskCacheSizeBytes: Long
    )
}
