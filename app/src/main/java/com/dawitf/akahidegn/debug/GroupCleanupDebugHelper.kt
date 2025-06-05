package com.dawitf.akahidegn.debug

import android.content.Context
import android.util.Log
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.domain.repository.GroupRepository
import com.dawitf.akahidegn.service.GroupCleanupScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug helper for testing group cleanup functionality.
 */
@Singleton
class GroupCleanupDebugHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val groupCleanupScheduler: GroupCleanupScheduler,
    private val groupRepository: GroupRepository
) {
    
    companion object {
        const val TAG = "GroupCleanupDebug"
    }
    
    /**
     * Trigger an immediate group cleanup for testing purposes.
     */
    fun triggerImmediateCleanup() {
        Log.d(TAG, "Triggering immediate group cleanup for testing")
        groupCleanupScheduler.triggerImmediateCleanup()
        Log.d(TAG, "Immediate group cleanup triggered")
    }
    
    /**
     * Log current timestamp for debugging group expiration.
     */
    fun logCurrentTimestamp() {
        val currentTime = System.currentTimeMillis()
        val thirtyMinutesAgo = currentTime - (30 * 60 * 1000)
        Log.d(TAG, "Current timestamp: $currentTime")
        Log.d(TAG, "30 minutes ago timestamp: $thirtyMinutesAgo")
        Log.d(TAG, "Groups created before $thirtyMinutesAgo should be expired")
    }
    
    /**
     * Create a test group and log its details for debugging.
     */
    fun createTestGroup(name: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val currentTime = System.currentTimeMillis()
            val testGroup = Group(
                destinationName = name,
                timestamp = currentTime,
                memberCount = 1,
                pickupLat = 9.0317,
                pickupLng = 38.7612
            )
            
            Log.d(TAG, "Creating test group '$name' with timestamp: $currentTime")
            val result = groupRepository.createGroup(testGroup)
            if (result.isSuccess) {
                Log.d(TAG, "Test group '$name' created successfully")
            } else {
                val errorMessage = if (result is com.dawitf.akahidegn.core.result.Result.Error) {
                    result.error.message
                } else {
                    "Unknown error"
                }
                Log.e(TAG, "Failed to create test group '$name': $errorMessage")
            }
        }
    }
    
    /**
     * List all groups with their timestamps for debugging.
     */
    fun logAllGroupsWithTimestamps() {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "=== Current Groups in Database ===")
            val currentTime = System.currentTimeMillis()
            val thirtyMinutesAgo = currentTime - (30 * 60 * 1000)
            
            groupRepository.getAllGroups().collect { result ->
                if (result.isSuccess) {
                    val groups = result.getOrNull() ?: emptyList()
                    Log.d(TAG, "Total groups found: ${groups.size}")
                    
                    groups.forEach { group ->
                        val age = if (group.timestamp != null) {
                            val ageMinutes = (currentTime - group.timestamp!!) / (60 * 1000)
                            "${ageMinutes} minutes"
                        } else {
                            "unknown age"
                        }
                        
                        val status = if (group.timestamp != null && group.timestamp!! <= thirtyMinutesAgo) {
                            "SHOULD BE EXPIRED"
                        } else {
                            "active"
                        }
                        
                        Log.d(TAG, "Group: ${group.destinationName}, ID: ${group.groupId}, Timestamp: ${group.timestamp}, Age: $age, Status: $status")
                    }
                } else {
                    Log.e(TAG, "Failed to get groups: ${(result as? Result.Error)?.error?.message ?: "Unknown error"}")
                }
            }
        }
    }
}
