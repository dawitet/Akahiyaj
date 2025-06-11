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
    
    /**
     * MASS DELETE ALL GROUPS - USE ONLY FOR MANUAL CLEANUP
     * This will delete ALL groups from Firebase, regardless of age
     */
    fun massDeleteAllGroups() {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "🔥 MASS DELETION: Starting to delete ALL groups from Firebase")
            Log.w(TAG, "⚠️ WARNING: This will delete ALL groups permanently!")
            
            try {
                groupRepository.getAllGroups().collect { result ->
                    when (result) {
                        is com.dawitf.akahidegn.core.result.Result.Success -> {
                            val allGroups = result.data
                            Log.d(TAG, "Found ${allGroups.size} groups to delete")
                            
                            if (allGroups.isEmpty()) {
                                Log.d(TAG, "No groups found to delete")
                                return@collect
                            }
                            
                            var deletedCount = 0
                            var failedCount = 0
                            
                            allGroups.forEach { group ->
                                val groupInfo = "${group.destinationName} (ID: ${group.groupId})"
                                Log.d(TAG, "Deleting group: $groupInfo")
                                
                                try {
                                    val deleteResult = groupRepository.deleteGroup(group.groupId ?: "")
                                    if (deleteResult.isSuccess) {
                                        deletedCount++
                                        Log.d(TAG, "✅ Successfully deleted: $groupInfo")
                                    } else {
                                        failedCount++
                                        val error = if (deleteResult is com.dawitf.akahidegn.core.result.Result.Error) {
                                            deleteResult.error.message
                                        } else {
                                            "Unknown error"
                                        }
                                        Log.e(TAG, "❌ Failed to delete $groupInfo: $error")
                                    }
                                } catch (e: Exception) {
                                    failedCount++
                                    Log.e(TAG, "❌ Exception deleting $groupInfo: ${e.message}")
                                }
                            }
                            
                            Log.d(TAG, "🔥 MASS DELETION COMPLETED!")
                            Log.d(TAG, "✅ Successfully deleted: $deletedCount groups")
                            Log.d(TAG, "❌ Failed to delete: $failedCount groups")
                            Log.d(TAG, "📊 Total processed: ${allGroups.size} groups")
                        }
                        is com.dawitf.akahidegn.core.result.Result.Error -> {
                            Log.e(TAG, "Failed to get groups for mass deletion: ${result.error.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Mass deletion failed with exception: ${e.message}", e)
            }
        }
    }
    
    /**
     * Delete only old groups (older than specified minutes) - safer option
     */
    fun deleteOldGroups(olderThanMinutes: Int = 30) {
        CoroutineScope(Dispatchers.IO).launch {
            val cutoffTime = System.currentTimeMillis() - (olderThanMinutes * 60 * 1000)
            Log.d(TAG, "🧹 Deleting groups older than $olderThanMinutes minutes (before $cutoffTime)")
            
            try {
                groupRepository.getAllGroups().collect { result ->
                    when (result) {
                        is com.dawitf.akahidegn.core.result.Result.Success -> {
                            val allGroups = result.data
                            val oldGroups = allGroups.filter { group ->
                                group.timestamp != null && group.timestamp!! <= cutoffTime
                            }
                            
                            Log.d(TAG, "Found ${oldGroups.size} old groups out of ${allGroups.size} total groups")
                            
                            oldGroups.forEach { group ->
                                val ageMinutes = if (group.timestamp != null) {
                                    (System.currentTimeMillis() - group.timestamp!!) / (60 * 1000)
                                } else {
                                    "unknown"
                                }
                                
                                Log.d(TAG, "Deleting old group: ${group.destinationName} (age: ${ageMinutes}min)")
                                groupRepository.deleteGroup(group.groupId ?: "")
                            }
                            
                            Log.d(TAG, "🧹 Finished deleting ${oldGroups.size} old groups")
                        }
                        is com.dawitf.akahidegn.core.result.Result.Error -> {
                            Log.e(TAG, "Failed to get groups for age-based deletion: ${result.error.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Age-based deletion failed: ${e.message}", e)
            }
        }
    }
}
