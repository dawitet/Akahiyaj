package com.dawitf.akahidegn.core.optimistic

import com.dawitf.akahidegn.Group
import java.util.UUID

/**
 * Represents different types of optimistic operations that can be performed
 * on groups. These operations update the UI immediately before server confirmation.
 */
sealed interface OptimisticOperation {
    val operationId: String
    val timestamp: Long
    
    data class CreateGroup(
        override val operationId: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        val optimisticGroup: Group,
        val userId: String
    ) : OptimisticOperation
    
    data class JoinGroup(
        override val operationId: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        val groupId: String,
        val userId: String,
        val userName: String
    ) : OptimisticOperation
    
    data class LeaveGroup(
        override val operationId: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        val groupId: String,
        val userId: String
    ) : OptimisticOperation
    
    data class UpdateGroup(
        override val operationId: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        val groupId: String,
        val updatedGroup: Group
    ) : OptimisticOperation
    
    data class DeleteGroup(
        override val operationId: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        val groupId: String,
        val userId: String
    ) : OptimisticOperation
}

/**
 * Represents the current state of an optimistic operation
 */
sealed interface OptimisticOperationState {
    val operation: OptimisticOperation
    
    data class Pending(
        override val operation: OptimisticOperation
    ) : OptimisticOperationState
    
    data class Success(
        override val operation: OptimisticOperation,
        val serverData: Any? = null
    ) : OptimisticOperationState
    
    data class Failed(
        override val operation: OptimisticOperation,
        val error: String,
        val shouldRetry: Boolean = false
    ) : OptimisticOperationState
}
