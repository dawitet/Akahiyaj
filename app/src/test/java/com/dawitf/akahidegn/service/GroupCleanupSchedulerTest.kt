package com.dawitf.akahidegn.service

import android.content.Context
import androidx.work.WorkManager
import com.dawitf.akahidegn.worker.GroupCleanupWorker
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Simple test for GroupCleanupScheduler to verify basic functionality.
 */
class GroupCleanupSchedulerTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var groupCleanupScheduler: GroupCleanupScheduler

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        workManager = mockk(relaxed = true)
        
        // Mock WorkManager.getInstance()
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(context) } returns workManager
        
        groupCleanupScheduler = GroupCleanupScheduler(context)
    }

    @Test
    fun testGroupCleanupScheduler_creation() {
        // Verify that the scheduler can be created successfully
        assertNotNull("GroupCleanupScheduler should be created", groupCleanupScheduler)
    }

    @Test
    fun testGroupCleanupScheduler_constants() {
        // Verify that necessary constants are available
        assertEquals("group_cleanup_work", GroupCleanupWorker.WORK_NAME)
        assertEquals("GroupCleanupWorker", GroupCleanupWorker.TAG)
    }
}
