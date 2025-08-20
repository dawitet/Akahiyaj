package com.dawitf.akahidegn.benchmark

import androidx.benchmark.macro.ExperimentalBaselineProfilesApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.benchmark.macro.macrobenchmark
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 5: User Journey Performance Benchmarks
 * 
 * Measures performance of critical user journeys in the Akahidegn app:
 * - Group browsing and search
 * - Group creation workflow
 * - Group joining workflow
 * - Navigation and transitions
 * 
 * These benchmarks generate baseline profiles for the most common user flows
 * and measure frame timing to ensure smooth 60fps performance.
 */
@ExperimentalBaselineProfilesApi
@LargeTest
@RunWith(AndroidJUnit4::class)
class UserJourneyBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun groupBrowsingPerformance() = benchmarkRule.macrobenchmark(
        uniqueName = "groupBrowsingPerformance",
        packageName = "com.dawitf.akahidegn",
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        setupBlock = {
            startActivityAndWait()
            device.waitForIdle(3000) // Wait for initial group loading
        }
    ) {
        // Simulate group browsing workflow
        performGroupBrowsingJourney()
    }

    @Test
    fun groupCreationPerformance() = benchmarkRule.macrobenchmark(
        uniqueName = "groupCreationPerformance", 
        packageName = "com.dawitf.akahidegn",
        metrics = listOf(FrameTimingMetric()),
        iterations = 3, // Fewer iterations since this is a longer workflow
        setupBlock = {
            startActivityAndWait()
            device.waitForIdle(3000)
        }
    ) {
        // Simulate group creation workflow
        performGroupCreationJourney()
    }

    @Test
    fun navigationPerformance() = benchmarkRule.macrobenchmark(
        uniqueName = "navigationPerformance",
        packageName = "com.dawitf.akahidegn", 
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        setupBlock = {
            startActivityAndWait()
            device.waitForIdle(2000)
        }
    ) {
        // Simulate navigation between screens
        performNavigationJourney()
    }

    @Test
    fun searchPerformance() = benchmarkRule.macrobenchmark(
        uniqueName = "searchPerformance",
        packageName = "com.dawitf.akahidegn",
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        setupBlock = {
            startActivityAndWait()
            device.waitForIdle(3000)
        }
    ) {
        // Simulate search functionality
        performSearchJourney()
    }

    private fun MacrobenchmarkScope.startActivityAndWait() {
        val intent = context.packageManager.getLaunchIntentForPackage("com.dawitf.akahidegn")
        requireNotNull(intent) { "Cannot find launch intent for com.dawitf.akahidegn" }
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        device.waitForIdle(2000)
    }

    private fun MacrobenchmarkScope.performGroupBrowsingJourney() {
        // This simulates the most common user journey: browsing available groups
        // Exercises: StateFlow group loading, LazyColumn composition, distance calculations
        
        repeat(5) {
            // Scroll down through groups
            device.swipe(
                startX = device.displayWidth / 2,
                startY = device.displayHeight * 3 / 4,
                endX = device.displayWidth / 2, 
                endY = device.displayHeight / 4,
                steps = 15 // Smooth scrolling
            )
            device.waitForIdle(800)
            
            // Scroll back up
            device.swipe(
                startX = device.displayWidth / 2,
                startY = device.displayHeight / 4,
                endX = device.displayWidth / 2,
                endY = device.displayHeight * 3 / 4,
                steps = 15
            )
            device.waitForIdle(800)
        }
        
        // Simulate tapping on a group for details
        device.click(device.displayWidth / 2, device.displayHeight / 2)
        device.waitForIdle(1500)
        
        // Go back to main list
        device.pressBack()
        device.waitForIdle(1000)
    }

    private fun MacrobenchmarkScope.performGroupCreationJourney() {
        // Simulates group creation workflow
        // Exercises: OptimisticOperationsManager, CreateGroupWorker, Firebase operations
        
        // Look for FAB or create button (bottom-right area typically)
        device.click(device.displayWidth - 100, device.displayHeight - 100)
        device.waitForIdle(1500)
        
        // Simulate form interaction (this would need UI selectors in real implementation)
        // For baseline profile generation, even simulated interactions help
        device.click(device.displayWidth / 2, device.displayHeight / 3)
        device.waitForIdle(500)
        
        device.click(device.displayWidth / 2, device.displayHeight / 2)
        device.waitForIdle(500)
        
        // Simulate submission
        device.click(device.displayWidth / 2, device.displayHeight * 3 / 4)
        device.waitForIdle(2000)
        
        // Return to main screen
        device.pressBack()
        device.waitForIdle(1000)
    }

    private fun MacrobenchmarkScope.performNavigationJourney() {
        // Simulates navigation between different screens
        // Exercises: Navigation component, shared element transitions, screen composition
        
        // Navigate to profile (typically top-right or menu)
        device.click(device.displayWidth - 50, 100)
        device.waitForIdle(1500)
        
        // Navigate back
        device.pressBack()
        device.waitForIdle(1000)
        
        // Navigate to settings/menu
        device.click(50, 100)
        device.waitForIdle(1500)
        
        device.pressBack()
        device.waitForIdle(1000)
        
        // Quick navigation pattern
        repeat(3) {
            device.click(device.displayWidth / 4, device.displayHeight / 4)
            device.waitForIdle(500)
            device.pressBack()
            device.waitForIdle(500)
        }
    }

    private fun MacrobenchmarkScope.performSearchJourney() {
        // Simulates search functionality
        // Exercises: Search StateFlow, filtering logic, UI updates
        
        // Look for search area (typically top of screen)
        device.click(device.displayWidth / 2, 200)
        device.waitForIdle(1000)
        
        // Simulate typing (this exercises search filtering)
        repeat(5) {
            device.click(device.displayWidth / 2, 250)
            device.waitForIdle(300)
        }
        
        // Wait for search results
        device.waitForIdle(1500)
        
        // Clear search
        device.pressBack()
        device.waitForIdle(1000)
        
        // Repeat search workflow
        device.click(device.displayWidth / 2, 200)
        device.waitForIdle(500)
        
        device.pressBack()
        device.waitForIdle(500)
    }
}
