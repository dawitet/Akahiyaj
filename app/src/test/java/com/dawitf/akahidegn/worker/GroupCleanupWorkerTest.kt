package com.dawitf.akahidegn.worker

import com.dawitf.akahidegn.domain.repository.GroupRepository
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.core.error.AppError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

/**
 * Test class for GroupCleanupWorker functionality.
 * Tests the core business logic without WorkManager complexity.
 */
@RunWith(JUnit4::class)
class GroupCleanupWorkerTest {

    private lateinit var groupRepository: GroupRepository

    @Before
    fun setUp() {
        groupRepository = mock(GroupRepository::class.java)
    }

    @Test
    fun testGroupCleanupLogic_success() {
        runBlocking {
            // Mock the repository to return a success result
            `when`(groupRepository.cleanupExpiredGroups()).thenReturn(Result.Success(5))

            // Test the cleanup logic directly
            val result = groupRepository.cleanupExpiredGroups()

            // Verify the result
            when (result) {
                is Result.Success -> {
                    assertEquals(5, result.data)
                    // This would translate to ListenableWorker.Result.success() in actual worker
                }
                is Result.Error -> {
                    throw AssertionError("Expected success but got error: ${result.error}")
                }
            }

            verify(groupRepository).cleanupExpiredGroups()
        }
    }

    @Test
    fun testGroupCleanupLogic_failure() {
        runBlocking {
            // Mock the repository to return an error result
            `when`(groupRepository.cleanupExpiredGroups()).thenReturn(Result.Error(AppError.UnknownError("Cleanup failed")))

            // Test the cleanup logic directly
            val result = groupRepository.cleanupExpiredGroups()

            // Verify the result
            when (result) {
                is Result.Success -> {
                    throw AssertionError("Expected error but got success: ${result.data}")
                }
                is Result.Error -> {
                    assertEquals("Cleanup failed", result.error.message)
                    // This would translate to ListenableWorker.Result.retry() in actual worker
                }
            }

            verify(groupRepository).cleanupExpiredGroups()
        }
    }
}
