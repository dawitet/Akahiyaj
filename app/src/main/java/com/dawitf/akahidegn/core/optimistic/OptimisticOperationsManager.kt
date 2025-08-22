package com.dawitf.akahidegn.core.optimistic

import android.util.Log
import com.dawitf.akahidegn.domain.model.Group
import com.dawitf.akahidegn.core.event.UiEventManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages optimistic operations for immediate UI updates with eventual server consistency.
 * 
 * Key Features:
 * - Immediate UI updates before server confirmation
 * - Automatic rollback on operation failure
 * - Operation queuing and retry mechanism
 * - Integration with UI event system for user feedback
 * 
 * Flow:
 * 1. User action triggers optimistic operation
 * 2. UI updates immediately with optimistic data
 * 3. Server operation executes in background
 * 4. On success: Confirm optimistic changes
 * 5. On failure: Rollback UI and show error
 */
@Singleton
class OptimisticOperationsManager @Inject constructor(
    private val uiEventManager: UiEventManager
) {
    companion object {
        private const val TAG = "OptimisticOperations"
        private const val OPERATION_TIMEOUT_MS = 30_000L // 30 seconds
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Track active operations
    private val _activeOperations = MutableStateFlow<Map<String, OptimisticOperationState>>(emptyMap())
    val activeOperations: StateFlow<Map<String, OptimisticOperationState>> = _activeOperations.asStateFlow()
    
    // Track optimistic groups for UI display
    private val _optimisticGroups = MutableStateFlow<Map<String, Group>>(emptyMap())
    val optimisticGroups: StateFlow<Map<String, Group>> = _optimisticGroups.asStateFlow()
    
    // Track groups pending removal (for leave/delete operations)
    private val _pendingRemovals = MutableStateFlow<Set<String>>(emptySet())
    val pendingRemovals: StateFlow<Set<String>> = _pendingRemovals.asStateFlow()
    
    /**
     * Execute an optimistic operation with immediate UI update
     */
    fun executeOptimistically(
        operation: OptimisticOperation,
        serverOperation: suspend () -> Unit
    ) {
        Log.d(TAG, "Starting optimistic operation: ${operation.operationId}")
        
        // 1. Apply optimistic changes to UI immediately
        applyOptimisticChanges(operation)
        
        // 2. Mark operation as pending
        updateOperationState(OptimisticOperationState.Pending(operation))
        
        // 3. Execute server operation in background
        scope.launch {
            try {
                serverOperation()
                handleOperationSuccess(operation)
            } catch (e: Exception) {
                handleOperationFailure(operation, e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Apply immediate UI changes for optimistic operations
     */
    private fun applyOptimisticChanges(operation: OptimisticOperation) {
        when (operation) {
            is OptimisticOperation.CreateGroup -> {
                // Add optimistic group to UI immediately
                _optimisticGroups.value = _optimisticGroups.value + (operation.optimisticGroup.groupId!! to operation.optimisticGroup)
                Log.d(TAG, "Applied optimistic group creation: ${operation.optimisticGroup.groupId}")
            }
            
            is OptimisticOperation.JoinGroup -> {
                // Update group member count optimistically
                val currentGroups = _optimisticGroups.value.toMutableMap()
                currentGroups[operation.groupId]?.let { group ->
                    val updatedMembers = HashMap(group.members)
                    updatedMembers[operation.userId] = true
                    val updatedGroup = group.copy(
                        memberCount = group.memberCount + 1,
                        members = updatedMembers
                    )
                    currentGroups[operation.groupId] = updatedGroup
                    _optimisticGroups.value = currentGroups
                }
                Log.d(TAG, "Applied optimistic group join: ${operation.groupId}")
            }
            
            is OptimisticOperation.LeaveGroup -> {
                // Mark group for pending removal or update member count
                _pendingRemovals.value = _pendingRemovals.value + operation.groupId
                
                val currentGroups = _optimisticGroups.value.toMutableMap()
                currentGroups[operation.groupId]?.let { group ->
                    val updatedMembers = HashMap(group.members)
                    updatedMembers.remove(operation.userId)
                    val updatedGroup = group.copy(
                        memberCount = maxOf(0, group.memberCount - 1),
                        members = updatedMembers
                    )
                    currentGroups[operation.groupId] = updatedGroup
                    _optimisticGroups.value = currentGroups
                }
                Log.d(TAG, "Applied optimistic group leave: ${operation.groupId}")
            }
            
            is OptimisticOperation.UpdateGroup -> {
                // Update group data optimistically
                _optimisticGroups.value = _optimisticGroups.value + (operation.groupId to operation.updatedGroup)
                Log.d(TAG, "Applied optimistic group update: ${operation.groupId}")
            }
            
            is OptimisticOperation.DeleteGroup -> {
                // Remove group from UI immediately
                _optimisticGroups.value = _optimisticGroups.value - operation.groupId
                _pendingRemovals.value = _pendingRemovals.value + operation.groupId
                Log.d(TAG, "Applied optimistic group deletion: ${operation.groupId}")
            }
        }
    }
    
    /**
     * Handle successful operation completion
     */
    private fun handleOperationSuccess(operation: OptimisticOperation) {
        Log.d(TAG, "Operation succeeded: ${operation.operationId}")
        
        // Update operation state to success
        updateOperationState(OptimisticOperationState.Success(operation))
        
        // Clean up pending removals
        when (operation) {
            is OptimisticOperation.LeaveGroup -> {
                _pendingRemovals.value = _pendingRemovals.value - operation.groupId
            }
            is OptimisticOperation.DeleteGroup -> {
                _pendingRemovals.value = _pendingRemovals.value - operation.groupId
            }
            else -> { /* No cleanup needed */ }
        }
        
        // Send success event
        sendSuccessEvent(operation)
        
        // Remove operation from active list after a delay
        scope.launch {
            kotlinx.coroutines.delay(2000) // Keep success state for 2 seconds
            removeOperation(operation.operationId)
        }
    }
    
    /**
     * Handle operation failure and rollback optimistic changes
     */
    private fun handleOperationFailure(operation: OptimisticOperation, error: String) {
        Log.w(TAG, "Operation failed: ${operation.operationId}, error: $error")
        
        // Rollback optimistic changes
        rollbackOptimisticChanges(operation)
        
        // Update operation state to failed
        updateOperationState(OptimisticOperationState.Failed(operation, error, shouldRetry = true))
        
        // Send error event
        uiEventManager.showError("Operation failed: $error")
        
        // Remove operation from active list after a delay
        scope.launch {
            kotlinx.coroutines.delay(5000) // Keep error state for 5 seconds
            removeOperation(operation.operationId)
        }
    }
    
    /**
     * Rollback optimistic changes when operation fails
     */
    private fun rollbackOptimisticChanges(operation: OptimisticOperation) {
        when (operation) {
            is OptimisticOperation.CreateGroup -> {
                // Remove optimistic group from UI
                _optimisticGroups.value = _optimisticGroups.value - operation.optimisticGroup.groupId!!
                Log.d(TAG, "Rolled back optimistic group creation: ${operation.optimisticGroup.groupId}")
            }
            
            is OptimisticOperation.JoinGroup -> {
                // Revert member count change
                val currentGroups = _optimisticGroups.value.toMutableMap()
                currentGroups[operation.groupId]?.let { group ->
                    val revertedMembers = HashMap(group.members)
                    revertedMembers.remove(operation.userId)
                    val revertedGroup = group.copy(
                        memberCount = maxOf(0, group.memberCount - 1),
                        members = revertedMembers
                    )
                    currentGroups[operation.groupId] = revertedGroup
                    _optimisticGroups.value = currentGroups
                }
                Log.d(TAG, "Rolled back optimistic group join: ${operation.groupId}")
            }
            
            is OptimisticOperation.LeaveGroup -> {
                // Remove from pending removals and revert member count
                _pendingRemovals.value = _pendingRemovals.value - operation.groupId
                
                val currentGroups = _optimisticGroups.value.toMutableMap()
                currentGroups[operation.groupId]?.let { group ->
                    val revertedMembers = HashMap(group.members)
                    revertedMembers[operation.userId] = true
                    val revertedGroup = group.copy(
                        memberCount = group.memberCount + 1,
                        members = revertedMembers
                    )
                    currentGroups[operation.groupId] = revertedGroup
                    _optimisticGroups.value = currentGroups
                }
                Log.d(TAG, "Rolled back optimistic group leave: ${operation.groupId}")
            }
            
            is OptimisticOperation.DeleteGroup -> {
                // Remove from pending removals (would need original group data to fully restore)
                _pendingRemovals.value = _pendingRemovals.value - operation.groupId
                Log.d(TAG, "Rolled back optimistic group deletion: ${operation.groupId}")
            }
            
            is OptimisticOperation.UpdateGroup -> {
                // Would need original group data to properly rollback
                Log.d(TAG, "Rolled back optimistic group update: ${operation.groupId}")
            }
        }
    }
    
    /**
     * Send appropriate success event based on operation type
     */
    private fun sendSuccessEvent(operation: OptimisticOperation) {
        when (operation) {
            is OptimisticOperation.CreateGroup -> {
                uiEventManager.groupCreatedSuccess(operation.optimisticGroup.groupId!!, operation.optimisticGroup.destinationName ?: "Group")
            }
            is OptimisticOperation.JoinGroup -> {
                uiEventManager.groupJoinedSuccess(operation.groupId, "Group")
            }
            is OptimisticOperation.LeaveGroup -> {
                uiEventManager.groupLeftSuccess("Group")
            }
            is OptimisticOperation.UpdateGroup -> {
                uiEventManager.showSuccess("Group updated successfully")
            }
            is OptimisticOperation.DeleteGroup -> {
                uiEventManager.showSuccess("Group deleted successfully")
            }
        }
    }
    
    /**
     * Mark operation as successful (called by WorkManager workers)
     */
    fun markOperationSuccess(operationId: String) {
        Log.d(TAG, "WorkManager operation succeeded: $operationId")
        
        val operationState = _activeOperations.value[operationId]
        if (operationState is OptimisticOperationState.Pending) {
            handleOperationSuccess(operationState.operation)
        } else {
            Log.w(TAG, "No pending operation found for success: $operationId")
        }
    }
    
    /**
     * Mark operation as failed (called by WorkManager workers)
     */
    fun markOperationFailed(operationId: String) {
        Log.d(TAG, "WorkManager operation failed: $operationId")
        
        val operationState = _activeOperations.value[operationId]
        if (operationState is OptimisticOperationState.Pending) {
            handleOperationFailure(operationState.operation, "Operation failed after retries")
        } else {
            Log.w(TAG, "No pending operation found for failure: $operationId")
        }
    }
    
    /**
     * Update operation state
     */
    private fun updateOperationState(state: OptimisticOperationState) {
        _activeOperations.value = _activeOperations.value + (state.operation.operationId to state)
    }
    
    /**
     * Remove operation from tracking
     */
    private fun removeOperation(operationId: String) {
        _activeOperations.value = _activeOperations.value - operationId
    }
    
    /**
     * Clear all optimistic data (useful for refresh scenarios)
     */
    fun clearOptimisticData() {
        _optimisticGroups.value = emptyMap()
        _pendingRemovals.value = emptySet()
        _activeOperations.value = emptyMap()
        Log.d(TAG, "Cleared all optimistic data")
    }
    
    /**
     * Check if a group is currently in an optimistic state
     */
    fun isGroupOptimistic(groupId: String): Boolean {
        return _optimisticGroups.value.containsKey(groupId) || _pendingRemovals.value.contains(groupId)
    }
    
    /**
     * Get optimistic group data if available
     */
    fun getOptimisticGroup(groupId: String): Group? {
        return _optimisticGroups.value[groupId]
    }
}
