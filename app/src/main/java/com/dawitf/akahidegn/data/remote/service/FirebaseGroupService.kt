package com.dawitf.akahidegn.data.remote.service

import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.core.result.Result
import kotlinx.coroutines.flow.Flow

interface FirebaseGroupService {
    
    fun getAllGroups(): Flow<Result<List<Group>>>
    
    suspend fun getGroupById(groupId: String): Result<Group>
    
    fun getNearbyGroups(latitude: Double, longitude: Double, radiusKm: Double): Flow<Result<List<Group>>>
    
    suspend fun createGroup(group: Group): Result<Group>
    
    suspend fun updateGroup(group: Group): Result<Group>
    
    suspend fun deleteGroup(groupId: String): Result<Unit>
    
    suspend fun joinGroup(groupId: String, userId: String): Result<Unit>
    
    suspend fun leaveGroup(groupId: String, userId: String): Result<Unit>
    
    // Cleanup methods
    suspend fun getExpiredGroups(thresholdTimestamp: Long): Result<List<Group>>
    
    suspend fun deleteExpiredGroups(groupIds: List<String>): Result<Unit>
}
