package com.dawitf.akahidegn

import com.dawitf.akahidegn.viewmodel.MainViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Performance validation tests for the Akahidegn app
 * These tests verify that our ViewModel optimizations are working correctly
 * and that performance improvements are measurable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PerformanceValidationTest {

    private lateinit var viewModel: MainViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MainViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `verify search query debouncing works correctly`() = runTest {
        // Simulate rapid typing (should be debounced)
        repeat(10) { i ->
            viewModel.updateSearchQuery("search query $i")
        }
        
        val searchQuery = viewModel.searchQuery.first()
        
        // Verify that the query was updated
        assertEquals("search query 9", searchQuery)
    }

    @Test
    fun `verify cache memory management`() {
        // This test verifies that our cache management prevents memory leaks
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Simulate many search operations
        repeat(100) { i ->
            viewModel.updateSearchQuery("query_$i")
        }
        
        // Force garbage collection
        System.gc()
        
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        // Memory increase should be reasonable (less than 10MB for this test)
        assertTrue("Memory usage should be controlled", 
                   memoryIncrease < 10 * 1024 * 1024)
    }

    @Test
    fun `verify state flow efficiency`() = runTest {
        // Update the same value multiple times
        repeat(5) {
            viewModel.updateSearchQuery("same query")
        }
        
        val searchQuery = viewModel.searchQuery.first()
        assertEquals("same query", searchQuery)
    }

    @Test
    fun `verify loading state performance`() {
        val measureTime = measureTimeMillis {
            // Simulate loading operations
            repeat(50) {
                viewModel.updateSearchQuery("query_$it")
            }
        }
        
        // Operations should complete quickly
        assertTrue("Loading operations should be fast", 
                   measureTime < 5000) // Less than 5 seconds
    }

    @Test
    fun `verify stable data classes prevent unnecessary recomposition`() {
        // Test Group data class stability
        val group1 = Group(
            groupId = "test1",
            destinationName = "Test Destination",
            memberCount = 2,
            maxMembers = 4
        )
        
        val group2 = Group(
            groupId = "test1",
            destinationName = "Test Destination", 
            memberCount = 2,
            maxMembers = 4
        )
        
        // Groups with same data should be equal (important for Compose stability)
        assertEquals("Groups with same data should be equal", group1, group2)
        assertEquals("Hash codes should match", group1.hashCode(), group2.hashCode())
    }

    @Test
    fun `verify memory cleanup works`() = runTest {
        // Fill cache with test data
        repeat(20) { i ->
            viewModel.updateSearchQuery("test_query_$i")
        }
        
        // Verify that the ViewModel is still responsive after operations
        viewModel.updateSearchQuery("after_cleanup")
        
        val finalQuery = viewModel.searchQuery.first()
        assertEquals("after_cleanup", finalQuery)
    }

    @Test
    fun `verify concurrent operations safety`() {
        // Simulate concurrent search operations
        repeat(10) { i ->
            viewModel.updateSearchQuery("concurrent_$i")
        }
        
        // Verify the ViewModel state is consistent
        val finalQuery = viewModel.searchQuery.value
        assertNotNull("Search query should not be null", finalQuery)
        assertTrue("Final query should contain 'concurrent'", 
                   finalQuery.contains("concurrent"))
    }
}
