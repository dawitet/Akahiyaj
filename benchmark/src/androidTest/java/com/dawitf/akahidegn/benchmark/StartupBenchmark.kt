package com.dawitf.akahidegn.benchmark

import androidx.benchmark.macro.ExperimentalBaselineProfilesApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.benchmark.macro.macrobenchmark
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 5: Enhanced Startup Benchmarks with Baseline Profile Support
 * 
 * Measures startup performance and generates baseline profiles for 
 * critical startup paths in the Akahidegn ride-sharing app.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @OptIn(ExperimentalBaselineProfilesApi::class)
    @Test
    fun startupCold() = benchmarkRule.macrobenchmark(
        uniqueName = "startupCold",
        packageName = "com.dawitf.akahidegn",
        metrics = listOf(StartupTimingMetric()),
        iterations = 5, // Increased iterations for better baseline profile data
        startupMode = StartupMode.COLD
    ) {
        pressHome()
        startActivityAndWait()
        
        // Wait for critical startup operations to complete
        // This includes StateFlow initialization, Firebase auth, and UI composition
        device.waitForIdle(3000)
        
        // Trigger initial group loading (most common first action)
        simulateInitialGroupBrowsing()
    }

    @OptIn(ExperimentalBaselineProfilesApi::class)
    @Test
    fun startupWarm() = benchmarkRule.macrobenchmark(
        uniqueName = "startupWarm",
        packageName = "com.dawitf.akahidegn",
        metrics = listOf(StartupTimingMetric()),
        iterations = 8, // More iterations for warm startup
        startupMode = StartupMode.WARM
    ) {
        pressHome()
        startActivityAndWait()
        
        // Warm startup should be faster - shorter wait
        device.waitForIdle(1500)
        
        // Quick interaction to ensure UI is responsive
        simulateQuickInteraction()
    }

    @OptIn(ExperimentalBaselineProfilesApi::class)
    @Test
    fun startupHot() = benchmarkRule.macrobenchmark(
        uniqueName = "startupHot",
        packageName = "com.dawitf.akahidegn",
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.HOT
    ) {
        pressHome()
        startActivityAndWait()
        
        // Hot startup - minimal wait time
        device.waitForIdle(500)
    }

    private fun MacrobenchmarkScope.startActivityAndWait() {
        val intent = context.packageManager.getLaunchIntentForPackage("com.dawitf.akahidegn")
        requireNotNull(intent) { "Cannot find launch intent for com.dawitf.akahidegn" }
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        device.waitForIdle(2000) // Base wait for activity start
    }

    private fun MacrobenchmarkScope.simulateInitialGroupBrowsing() {
        // Simulate the most common first user action: browsing groups
        // This helps optimize the critical path after startup
        
        // Scroll down to trigger LazyColumn composition
        device.swipe(
            startX = device.displayWidth / 2,
            startY = device.displayHeight * 3 / 4,
            endX = device.displayWidth / 2,
            endY = device.displayHeight / 4,
            steps = 10
        )
        device.waitForIdle(1000)
        
        // Scroll back up
        device.swipe(
            startX = device.displayWidth / 2,
            startY = device.displayHeight / 4,
            endX = device.displayWidth / 2,
            endY = device.displayHeight * 3 / 4,
            steps = 10
        )
        device.waitForIdle(1000)
    }

    private fun MacrobenchmarkScope.simulateQuickInteraction() {
        // Quick tap to ensure UI responsiveness after warm startup
        device.click(device.displayWidth / 2, device.displayHeight / 2)
        device.waitForIdle(500)
    }
}
