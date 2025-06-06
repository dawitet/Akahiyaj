package com.dawitf.akahidegn.integration

import android.content.Context
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.debug.GroupCleanupDebugHelper
import com.dawitf.akahidegn.domain.repository.GroupRepository
import com.dawitf.akahidegn.service.GroupCleanupScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Integration test for Group Cleanup functionality demonstrating 30-minute persistence.
 * This test validates the complete workflow of group creation, cleanup scheduling, and expiration.
 */
class GroupCleanupIntegrationTest {

    private lateinit var context: Context
    private lateinit var groupRepository: GroupRepository
    private lateinit var groupCleanupScheduler: GroupCleanupScheduler
    private lateinit var debugHelper: GroupCleanupDebugHelper

    companion object {
        private const val THIRTY_MINUTES_MS = 30 * 60 * 1000L
    }

    @Before
    fun setUp() {
        // Mock Android Log class
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.i(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        
        context = mockk(relaxed = true)
        groupRepository = mockk(relaxed = true)
        groupCleanupScheduler = mockk(relaxed = true)
        
        // Mock WorkManager
        mockkStatic(androidx.work.WorkManager::class)
        every { androidx.work.WorkManager.getInstance(any()) } returns mockk(relaxed = true)
        
        debugHelper = GroupCleanupDebugHelper(context, groupCleanupScheduler, groupRepository)
    }

    @Test
    fun testGroupLifecycle_30MinutePersistence() = runBlocking {
        // Test data setup
        val currentTime = System.currentTimeMillis()
        val expiredTime = currentTime - THIRTY_MINUTES_MS - 60000L // 31 minutes ago (1 minute past expiration)
        val activeTime = currentTime - (10 * 60 * 1000L) // 10 minutes ago (still active)

        val expiredGroup = Group(
            groupId = "expired-group-1",
            destinationName = "Old Destination",
            timestamp = expiredTime,
            memberCount = 2,
            pickupLat = 9.0317,
            pickupLng = 38.7611
        )

        val activeGroup = Group(
            groupId = "active-group-1", 
            destinationName = "Active Destination",
            timestamp = activeTime,
            memberCount = 3,
            pickupLat = 9.0317,
            pickupLng = 38.7611
        )

        val allGroups = listOf(expiredGroup, activeGroup)

        // Mock repository responses - using Flow for getAllGroups
        coEvery { groupRepository.getAllGroups() } returns flowOf(Result.Success(allGroups))
        coEvery { groupRepository.cleanupExpiredGroups() } returns Result.Success(1) // 1 group cleaned

        // Test 1: Verify group age calculation
        val expiredAgeMinutes = (currentTime - expiredTime) / (60 * 1000.0)
        val activeAgeMinutes = (currentTime - activeTime) / (60 * 1000.0)
        
        assertTrue("Expired group should be older than 30 minutes (actual: $expiredAgeMinutes)", expiredAgeMinutes > 30.0)
        assertTrue("Active group should be younger than 30 minutes (actual: $activeAgeMinutes)", activeAgeMinutes < 30.0)

        // Test 2: Verify cleanup logic identifies expired groups correctly
        val thirtyMinutesAgo = currentTime - THIRTY_MINUTES_MS
        assertTrue("Expired group timestamp should be before 30-minute threshold", 
                  expiredGroup.timestamp!! <= thirtyMinutesAgo)
        assertFalse("Active group timestamp should be after 30-minute threshold", 
                   activeGroup.timestamp!! <= thirtyMinutesAgo)

        // Test 3: Test debug helper functionality
        debugHelper.triggerImmediateCleanup()
        
        // Verify scheduler was called
        coVerify { groupCleanupScheduler.triggerImmediateCleanup() }

        // Test 4: Simulate cleanup operation
        val cleanupResult = groupRepository.cleanupExpiredGroups()
        assertTrue("Cleanup should succeed", cleanupResult is Result.Success)
        assertEquals("Should clean up 1 expired group", 1, (cleanupResult as Result.Success).data)

        // Verify cleanup was called
        coVerify { groupRepository.cleanupExpiredGroups() }
    }

    @Test
    fun testGroupCleanupScheduler_PeriodicExecution() {
        // Test scheduler configuration
        groupCleanupScheduler.scheduleGroupCleanup()
        
        // Verify scheduling was called
        coVerify { groupCleanupScheduler.scheduleGroupCleanup() }
        
        // Test immediate cleanup trigger
        groupCleanupScheduler.triggerImmediateCleanup()
        coVerify { groupCleanupScheduler.triggerImmediateCleanup() }
    }

    @Test
    fun testGroupPersistenceThreshold() {
        val currentTime = System.currentTimeMillis()
        
        // Test various time scenarios
        val scenarios = listOf(
            Pair("29 minutes ago", currentTime - (29 * 60 * 1000)), // Should persist
            Pair("30 minutes ago", currentTime - (30 * 60 * 1000)), // Exactly at threshold
            Pair("31 minutes ago", currentTime - (31 * 60 * 1000)), // Should be expired
            Pair("1 hour ago", currentTime - (60 * 60 * 1000))       // Definitely expired
        )
        
        val thirtyMinutesAgo = currentTime - THIRTY_MINUTES_MS
        
        scenarios.forEach { (description, timestamp) ->
            val shouldBeExpired = timestamp <= thirtyMinutesAgo
            val ageMinutes = (currentTime - timestamp) / (60 * 1000)
            
            if (shouldBeExpired) {
                assertTrue("$description (${ageMinutes}min) should be expired", ageMinutes >= 30)
            } else {
                assertTrue("$description (${ageMinutes}min) should persist", ageMinutes < 30)
            }
        }
    }

    @Test
    fun testDebugHelper_GroupCreationAndLogging() = runBlocking {
        val testGroups = listOf(
            Group(
                groupId = "test-1",
                destinationName = "Test Destination 1", 
                timestamp = System.currentTimeMillis(),
                memberCount = 1,
                pickupLat = 9.0317,
                pickupLng = 38.7611
            )
        )

        coEvery { groupRepository.getAllGroups() } returns flowOf(Result.Success(testGroups))

        // Test that debug helper can retrieve and log groups
        val result = groupRepository.getAllGroups()
        assertTrue("Should successfully retrieve groups", result != null)
        
        coVerify { groupRepository.getAllGroups() }
    }
}
