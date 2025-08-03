package com.dawitf.akahidegn.debug

import android.content.Context
import android.util.Log
import com.dawitf.akahidegn.domain.repository.GroupRepository
import com.dawitf.akahidegn.service.GroupCleanupScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug helper class for Group Cleanup functionality.
 * Provides utilities for testing and debugging group cleanup operations.
 */
class GroupCleanupDebugHelper(
    private val context: Context,
    private val groupCleanupScheduler: GroupCleanupScheduler,
    private val groupRepository: GroupRepository
) {

    companion object {
        private const val TAG = "GroupCleanupDebugHelper"
    }

    /**
     * Triggers an immediate cleanup operation for testing purposes.
     */
    fun triggerImmediateCleanup() {
        Log.d(TAG, "Debug: Triggering immediate group cleanup")
        groupCleanupScheduler.triggerImmediateCleanup()
    }

    /**
     * Logs current group information for debugging.
     */
    fun logGroupInformation() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                groupRepository.getAllGroups().collect { result ->
                    when (result) {
                        is com.dawitf.akahidegn.core.result.Result.Success<*> -> {
                            val groups = result.data as List<com.dawitf.akahidegn.Group>
                            Log.d(TAG, "Debug: Found ${groups.size} groups")
                            groups.forEach { group ->
                                val ageMinutes = (System.currentTimeMillis() - (group.timestamp ?: 0)) / (60 * 1000)
                                Log.d(TAG, "Debug: Group ${group.groupId} - Age: ${ageMinutes}min, Destination: ${group.destinationName}")
                            }
                        }
                        is com.dawitf.akahidegn.core.result.Result.Error -> {
                            Log.e(TAG, "Debug: Error retrieving groups: ${result.error.message}")
                        }
                        else -> {
                            Log.d(TAG, "Debug: Unhandled result type")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Debug: Exception while logging group information", e)
            }
        }
    }

    

    /**
     * Schedules the group cleanup service for debugging.
     */
    fun scheduleCleanup() {
        Log.d(TAG, "Debug: Scheduling group cleanup")
        groupCleanupScheduler.scheduleGroupCleanup()
    }

    /**
     * Cancels the group cleanup service for debugging.
     */
    fun cancelCleanup() {
        Log.d(TAG, "Debug: Cancelling group cleanup")
        groupCleanupScheduler.cancelGroupCleanup()
    }
}
