package com.dawitf.akahidegn.data.repository

import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.data.remote.service.GroupService
import com.dawitf.akahidegn.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepositoryImpl @Inject constructor(
    private val remoteDataSource: GroupService
) : GroupRepository {

    override fun getAllGroups(): Flow<Result<List<Group>>> {
        return remoteDataSource.getAllGroups()
    }

    override suspend fun getGroupById(groupId: String): Result<Group> {
        return remoteDataSource.getGroupById(groupId)
    }

    override suspend fun createGroup(group: Group): Result<Group> {
        return remoteDataSource.createGroup(group)
    }

    override suspend fun updateGroup(group: Group): Result<Group> {
        return remoteDataSource.updateGroup(group)
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        return remoteDataSource.deleteGroup(groupId)
    }

    override suspend fun joinGroup(groupId: String, userId: String): Result<Unit> {
        return remoteDataSource.joinGroup(groupId, userId)
    }

    override suspend fun leaveGroup(groupId: String, userId: String): Result<Unit> {
        return remoteDataSource.leaveGroup(groupId, userId)
    }

    override suspend fun syncGroups(): Result<Unit> {
        // No-op since we are not caching
        return Result.success(Unit)
    }

    override suspend fun clearCache(): Result<Unit> {
        // No-op since we are not caching
        return Result.success(Unit)
    }

    override suspend fun getExpiredGroups(thresholdTimestamp: Long): Result<List<Group>> {
        return remoteDataSource.getExpiredGroups(thresholdTimestamp)
    }
}