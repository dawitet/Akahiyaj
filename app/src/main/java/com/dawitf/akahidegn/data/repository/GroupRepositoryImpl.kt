package com.dawitf.akahidegn.data.repository

import androidx.paging.*
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.data.local.dao.GroupDao
import com.dawitf.akahidegn.data.mapper.toDomainModel
import com.dawitf.akahidegn.data.mapper.toEntity
import com.dawitf.akahidegn.data.mapper.toDomainModels
import com.dawitf.akahidegn.data.remote.service.GroupService
import com.dawitf.akahidegn.domain.repository.GroupRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepositoryImpl @Inject constructor(
    private val localDataSource: GroupDao,
    private val remoteDataSource: GroupService
) : GroupRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getAllGroupsPaged(): Flow<PagingData<Group>> {
        return Pager(
            PagingConfig(pageSize = 20),
        pagingSourceFactory = { localDataSource.getAllGroupsPaged() }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }
    }

    override fun getAllGroups(): Flow<Result<List<Group>>> {
        return combine(
            localDataSource.getAllGroups(),
            remoteDataSource.getAllGroups()
        ) { localGroups, remoteResult ->
            when {
                remoteResult.isSuccess -> {
                    val remoteGroups = remoteResult.getOrNull() ?: emptyList()
                    // Cache remote data locally
                    try {
                        localDataSource.insertGroups(remoteGroups.map { it.toEntity() })
                    } catch (e: Exception) {
                        // Log error but don't fail the operation
                    }
                    Result.success(remoteGroups)
                }
                localGroups.isNotEmpty() -> {
                    // Return cached data if remote fails but we have local data
                    Result.success(localGroups.toDomainModels())
                }
                else -> {
                    remoteResult // Return the remote error
                }
            }
        }
    }

    override suspend fun getGroupById(groupId: String): Result<Group> {
        // Try local first
        val localGroup = localDataSource.getGroupById(groupId)
        if (localGroup != null) {
            return Result.success(localGroup.toDomainModel())
        }

        // Fall back to remote
        val remoteResult = remoteDataSource.getGroupById(groupId)
        if (remoteResult.isSuccess) {
            val group = remoteResult.getOrNull()
            if (group != null) {
                // Cache locally
                localDataSource.insertGroup(group.toEntity())
                return Result.success(group)
            }
        }

        return remoteResult
    }

    override suspend fun createGroup(group: Group): Result<Group> {
        val result = remoteDataSource.createGroup(group)
        if (result.isSuccess) {
            val createdGroup = result.getOrNull()
            if (createdGroup != null) {
                // Cache locally
                localDataSource.insertGroup(createdGroup.toEntity())
            }
        }
        return result
    }

    override suspend fun updateGroup(group: Group): Result<Group> {
        val result = remoteDataSource.updateGroup(group)
        if (result.isSuccess) {
            // Update local cache
            localDataSource.updateGroup(group.toEntity())
        }
        return result
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        val result = remoteDataSource.deleteGroup(groupId)
        if (result.isSuccess) {
            // Remove from local cache
            localDataSource.deleteGroupById(groupId)
        }
        return result
    }

    override suspend fun joinGroup(groupId: String, userId: String): Result<Unit> {
        val result = remoteDataSource.joinGroup(groupId, userId)
        if (result.isSuccess) {
            // Update local cache
            val localGroup = localDataSource.getGroupById(groupId)
            if (localGroup != null) {
                // memberCount is now part of GroupEntity, no need for separate updateMemberCount
                // localDataSource.updateMemberCount(groupId, localGroup.memberCount + 1)
                val updatedGroup = localGroup.copy(memberCount = localGroup.memberCount + 1)
                localDataSource.updateGroup(updatedGroup)
            }
        }
        return result
    }

    override suspend fun leaveGroup(groupId: String, userId: String): Result<Unit> {
        val result = remoteDataSource.leaveGroup(groupId, userId)
        if (result.isSuccess) {
            // Update local cache
            val localGroup = localDataSource.getGroupById(groupId)
            if (localGroup != null) {
                // memberCount is now part of GroupEntity, no need for separate updateMemberCount
                // localDataSource.updateMemberCount(groupId, maxOf(0, localGroup.memberCount - 1))
                val updatedGroup = localGroup.copy(memberCount = maxOf(0, localGroup.memberCount - 1))
                localDataSource.updateGroup(updatedGroup)
            }
        }
        return result
    }

    override suspend fun syncGroups(): Result<Unit> {
        return try {
            // Clean up old cached data (older than 24 hours)
            val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            localDataSource.deleteOldGroups(oneDayAgo)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError.OperationFailed(e.message ?: "Sync failed"))
        }
    }

    override suspend fun clearCache(): Result<Unit> {
        return try {
            // This would need to be implemented in the DAO
            // localDataSource.clearAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError.OperationFailed(e.message ?: "Clear cache failed"))
        }
    }
    
    override suspend fun getExpiredGroups(thresholdTimestamp: Long): Result<List<Group>> {
        return remoteDataSource.getExpiredGroups(thresholdTimestamp)
    }
    
    override suspend fun cleanupExpiredGroups(): Result<Int> {
        return try {
            val thirtyMinutesAgo = System.currentTimeMillis() - (30 * 60 * 1000) // 30 minutes in milliseconds
            
            // Get expired groups
            val expiredGroupsResult = remoteDataSource.getExpiredGroups(thirtyMinutesAgo)
            if (expiredGroupsResult is Result.Success) {
                val expiredGroups = expiredGroupsResult.data
                if (expiredGroups.isNotEmpty()) {
                    val groupIds = expiredGroups.mapNotNull { it.groupId }
                    
                    // Delete expired groups from Firebase
                    val deleteResult = remoteDataSource.deleteExpiredGroups(groupIds)
                    if (deleteResult is Result.Success) {
                        // Also clean up from local cache
                        withContext(Dispatchers.IO) {
                            groupIds.forEach { groupId ->
                                localDataSource.deleteGroupById(groupId)
                            }
                        }
                        Result.success(groupIds.size)
                    } else {
                        Result.success(0)
                    }
                } else {
                    Result.success(0)
                }
            } else {
                Result.success(0)
            }
        } catch (e: Exception) {
            Result.failure(AppError.NetworkError.FirebaseError(e.message ?: "Failed to cleanup expired groups"))
        }
    }

    override suspend fun getCreatedGroupsCount(userId: String): Result<Int> {
        return remoteDataSource.getCreatedGroupsCount(userId)
    }

    override suspend fun getJoinedGroupsCount(userId: String): Result<Int> {
        return remoteDataSource.getJoinedGroupsCount(userId)
    }

    override suspend fun updateGroupStatus(groupId: String, status: String): Result<Unit> {
        val result = remoteDataSource.updateGroupStatus(groupId, status)
        if (result.isSuccess) {
            // Update local cache if needed, or refetch the group
            val localGroup = localDataSource.getGroupById(groupId)
            if (localGroup != null) {
                localDataSource.updateGroup(localGroup.copy(status = status))
            }
        }
        return result
    }
}