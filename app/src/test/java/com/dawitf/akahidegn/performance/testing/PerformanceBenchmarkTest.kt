package com.dawitf.akahidegn.performance.testing

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dawitf.akahidegn.performance.ImageCacheManager
import com.dawitf.akahidegn.performance.NetworkOptimizationManager
import com.dawitf.akahidegn.performance.PerformanceManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Performance benchmarking tests for critical app operations
 * Measures and validates performance metrics for key functionality
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PerformanceBenchmarkTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val benchmarkRule = BenchmarkRule()

    @Inject
    lateinit var imageCacheManager: ImageCacheManager

    @Inject
    lateinit var networkOptimizationManager: NetworkOptimizationManager

    @Inject
    lateinit var performanceManager: PerformanceManager

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun benchmarkImageCachePerformance() {
        benchmarkRule.measureRepeated {
            runBlocking {
                // Test image cache performance
                val testUrls = (1..10).map { "https://example.com/image$it.jpg" }
                
                runWithTimingDisabled {
                    // Setup phase - clear cache
                    imageCacheManager.clearCache()
                }
                
                // Benchmark image loading and caching
                testUrls.forEach { url ->
                    imageCacheManager.loadImage(url)
                }
            }
        }
    }

    @Test
    fun benchmarkNetworkOptimizationBatching() {
        benchmarkRule.measureRepeated {
            runBlocking {
                runWithTimingDisabled {
                    // Setup - start optimization
                    networkOptimizationManager.startOptimization()
                }
                
                // Benchmark network request batching
                repeat(20) { index ->
                    networkOptimizationManager.queueRequest(
                        NetworkOptimizationManager.NetworkRequest(
                            type = NetworkOptimizationManager.RequestType.GET,
                            url = "https://api.example.com/data$index",
                            onSuccess = { },
                            onError = { }
                        )
                    )
                }
                
                runWithTimingDisabled {
                    networkOptimizationManager.stopOptimization()
                }
            }
        }
    }

    @Test
    fun benchmarkMemoryUsageMonitoring() {
        benchmarkRule.measureRepeated {
            runBlocking {
                // Benchmark performance monitoring overhead
                repeat(100) {
                    performanceManager.getOptimizationSuggestions()
                }
            }
        }
    }

    @Test
    fun benchmarkCacheCleanupPerformance() {
        benchmarkRule.measureRepeated {
            runBlocking {
                runWithTimingDisabled {
                    // Fill cache with test data
                    repeat(1000) { index ->
                        imageCacheManager.loadImage("https://test.com/img$index.jpg")
                    }
                }
                
                // Benchmark cache cleanup
                imageCacheManager.clearCache()
            }
        }
    }

    @Test
    fun benchmarkNetworkStateUpdates() {
        benchmarkRule.measureRepeated {
            runBlocking {
                // Benchmark network state monitoring performance
                repeat(50) {
                    networkOptimizationManager.startOptimization()
                    networkOptimizationManager.stopOptimization()
                }
            }
        }
    }
}
