package com.dawitf.akahidegn.domain.repository

import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.core.optimistic.OptimisticOperation
import com.dawitf.akahidegn.core.optimistic.OptimisticOperationsManager
import com.dawitf.akahidegn.core.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository decorator that adds optimistic UI capabilities to group operations.
 * 
 * This class wraps the base GroupRepository and provides optimistic operations
 * that update the UI immediately while executing server operations in the background.
 * 
 * Key Features:
 * - Immediate UI updates for better perceived performance
 * - Automatic rollback on operation failure
 * - Seamless integration with existing StateFlow architecture
 * - Combines server data with optimistic state for UI consumption
 */
@Singleton
class OptimisticGroupRepository @Inject constructor(
    private val baseRepository: GroupRepository,
    private val optimisticManager: OptimisticOperationsManager
) {
    
    /**
     * Get all groups with optimistic operations applied.
     * Combines server data with optimistic state for immediate UI updates.
     */
    fun getAllGroupsWithOptimistic(): Flow<Result<List<Group>>> {
        return combine(
            baseRepository.getAllGroups(),
            optimisticManager.optimisticGroups,
            optimisticManager.pendingRemovals
        ) { serverResult, optimisticGroups, pendingRemovals ->
            when (serverResult) {
                is Result.Success -> {
                    // Merge server data with optimistic state
                    val mergedGroups = mergeGroupsWithOptimistic(
                        serverGroups = serverResult.data,
                        optimisticGroups = optimisticGroups,
                        pendingRemovals = pendingRemovals
                    )
                    Result.success(mergedGroups)
                }
                is Result.Error -> serverResult
                is Result.Loading -> serverResult
            }
        }
    }
    
    /**
     * Create a group with optimistic UI update
     */
    suspend fun createGroupOptimistically(group: Group, userId: String) {
        val operation = OptimisticOperation.CreateGroup(
            optimisticGroup = group,
            userId = userId
        )
        
        optimisticManager.executeOptimistically(operation) {
            // Execute actual server operation
            when (val result = baseRepository.createGroup(group)) {
                is Result.Success -> {
                    // Success - operation manager will handle UI updates
                }
                is Result.Error -> {
                    throw Exception(result.error.message)
                }
                is Result.Loading -> {
                    // Should not happen for suspend functions
                    throw Exception("Unexpected loading state")
                }
            }
        }
    }
    
    /**
     * Join a group with optimistic UI update
     */
    suspend fun joinGroupOptimistically(groupId: String, userId: String, userName: String) {
        val operation = OptimisticOperation.JoinGroup(
            groupId = groupId,
            userId = userId,
            userName = userName
        )
        
        optimisticManager.executeOptimistically(operation) {
            // Execute actual server operation
            when (val result = baseRepository.joinGroup(groupId, userId)) {
                is Result.Success -> {
                    // Success - operation manager will handle UI updates
                }
                is Result.Error -> {
                    throw Exception(result.error.message)
                }
                is Result.Loading -> {
                    throw Exception("Unexpected loading state")
                }
            }
        }
    }
    
    /**
     * Leave a group with optimistic UI update
     */
    suspend fun leaveGroupOptimistically(groupId: String, userId: String) {
        val operation = OptimisticOperation.LeaveGroup(
            groupId = groupId,
            userId = userId
        )
        
        optimisticManager.executeOptimistically(operation) {
            // Execute actual server operation
            when (val result = baseRepository.leaveGroup(groupId, userId)) {
                is Result.Success -> {
                    // Success - operation manager will handle UI updates
                }
                is Result.Error -> {
                    throw Exception(result.error.message)
                }
                is Result.Loading -> {
                    throw Exception("Unexpected loading state")
                }
            }
        }
    }
    
    /**
     * Update a group with optimistic UI update
     */
    suspend fun updateGroupOptimistically(group: Group) {
        val operation = OptimisticOperation.UpdateGroup(
            groupId = group.groupId!!,
            updatedGroup = group
        )
        
        optimisticManager.executeOptimistically(operation) {
            // Execute actual server operation
            when (val result = baseRepository.updateGroup(group)) {
                is Result.Success -> {
                    // Success - operation manager will handle UI updates
                }
                is Result.Error -> {
                    throw Exception(result.error.message)
                }
                is Result.Loading -> {
                    throw Exception("Unexpected loading state")
                }
            }
        }
    }
    
    /**
     * Delete a group with optimistic UI update
     */
    suspend fun deleteGroupOptimistically(groupId: String, userId: String) {
        val operation = OptimisticOperation.DeleteGroup(
            groupId = groupId,
            userId = userId
        )
        
        optimisticManager.executeOptimistically(operation) {
            // Execute actual server operation
            when (val result = baseRepository.deleteGroup(groupId)) {
                is Result.Success -> {
                    // Success - operation manager will handle UI updates
                }
                is Result.Error -> {
                    throw Exception(result.error.message)
                }
                is Result.Loading -> {
                    throw Exception("Unexpected loading state")
                }
            }
        }
    }
    
    /**
     * Clear all optimistic data (useful for refresh operations)
     */
    fun clearOptimisticData() {
        optimisticManager.clearOptimisticData()
    }
    
    /**
     * Check if a group is currently in an optimistic state
     */
    fun isGroupOptimistic(groupId: String): Boolean {
        return optimisticManager.isGroupOptimistic(groupId)
    }
    
    /**
     * Merge server groups with optimistic state for UI display
     */
    private fun mergeGroupsWithOptimistic(
        serverGroups: List<Group>,
        optimisticGroups: Map<String, Group>,
        pendingRemovals: Set<String>
    ): List<Group> {
        // Start with server groups
        val mergedGroups = mutableMapOf<String, Group>()
        
        // Add server groups (excluding ones pending removal)
        serverGroups.forEach { group ->
            if (!pendingRemovals.contains(group.groupId!!)) {
                mergedGroups[group.groupId!!] = group
            }
        }
        
        // Apply optimistic updates (these take precedence)
        optimisticGroups.forEach { (groupId, optimisticGroup) ->
            if (!pendingRemovals.contains(groupId)) {
                mergedGroups[groupId] = optimisticGroup
            }
        }
        
        return mergedGroups.values.toList()
    }
    
    // Delegate non-optimistic operations to base repository
    suspend fun getGroupById(groupId: String): Result<Group> = baseRepository.getGroupById(groupId)
    suspend fun syncGroups(): Result<Unit> = baseRepository.syncGroups()
    suspend fun clearCache(): Result<Unit> = baseRepository.clearCache()
    suspend fun getExpiredGroups(thresholdTimestamp: Long): Result<List<Group>> = 
        baseRepository.getExpiredGroups(thresholdTimestamp)
}
