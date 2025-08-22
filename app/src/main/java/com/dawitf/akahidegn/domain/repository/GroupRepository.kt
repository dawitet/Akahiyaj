package com.dawitf.akahidegn.domain.repository
import com.dawitf.akahidegn.domain.model.Group
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.domain.model.MemberInfo
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    
    fun getAllGroups(): Flow<Result<List<Group>>>
    
    fun getGroupsWithinRadius(userLat: Double, userLng: Double, radiusMeters: Double = 500.0): Flow<Result<List<Group>>>

    suspend fun getGroupById(groupId: String): Result<Group>
    
    suspend fun createGroupOptimistic(group: Group): Result<Group>

    suspend fun joinGroupOptimistic(groupId: String, userId: String, userInfo: MemberInfo): Result<Unit>

    suspend fun updateGroup(group: Group): Result<Group>
    
    suspend fun deleteGroup(groupId: String): Result<Unit>
    
    suspend fun leaveGroup(groupId: String, userId: String): Result<Unit>
    
    suspend fun syncGroups(): Result<Unit>
    
    suspend fun clearCache(): Result<Unit>
    
    suspend fun cleanupExpiredGroups(): Result<Unit>

    // Cleanup methods
    suspend fun getExpiredGroups(thresholdTimestamp: Long): Result<List<Group>>

    suspend fun refreshGroups(): Result<Unit>
}
