package com.dawitf.akahidegn.data.repository

import androidx.paging.*
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.data.local.dao.GroupDao
import com.dawitf.akahidegn.data.mapper.toDomainModel
import com.dawitf.akahidegn.data.mapper.toEntity
import com.dawitf.akahidegn.data.mapper.toDomainModels
import com.dawitf.akahidegn.data.remote.service.FirebaseGroupService
import com.dawitf.akahidegn.domain.repository.GroupRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepositoryImpl @Inject constructor(
    private val localDataSource: GroupDao,
    private val remoteDataSource: FirebaseGroupService
) : GroupRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getAllGroupsPaged(): Flow<PagingData<Group>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5
            ),
            remoteMediator = GroupRemoteMediator(localDataSource, remoteDataSource),
            pagingSourceFactory = { localDataSource.getAllGroupsPaged() }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }
    }

    override fun getAllGroups(): Flow<com.dawitf.akahidegn.core.result.Result<List<Group>>> {
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
                    com.dawitf.akahidegn.core.result.Result.success(remoteGroups)
                }
                localGroups.isNotEmpty() -> {
                    // Return cached data if remote fails but we have local data
                    com.dawitf.akahidegn.core.result.Result.success(localGroups.toDomainModels())
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

    override fun getNearbyGroups(latitude: Double, longitude: Double, radiusKm: Double): Flow<Result<List<Group>>> {
        val radiusSquared = (radiusKm * 1000) * (radiusKm * 1000) // Convert to meters and square

        return combine(
            localDataSource.getNearbyGroups(latitude, longitude, radiusSquared),
            remoteDataSource.getNearbyGroups(latitude, longitude, radiusKm)
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
                    Result.success(localGroups.map { it.toDomainModel() })
                }
                else -> {
                    remoteResult // Return the remote error
                }
            }
        }
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
                localDataSource.updateMemberCount(groupId, localGroup.memberCount + 1)
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
                localDataSource.updateMemberCount(groupId, maxOf(0, localGroup.memberCount - 1))
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
}

// Simple RemoteMediator for demonstration
@OptIn(ExperimentalPagingApi::class)
class GroupRemoteMediator(
    private val localDataSource: GroupDao,
    private val remoteDataSource: FirebaseGroupService
) : RemoteMediator<Int, com.dawitf.akahidegn.data.local.entity.GroupEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, com.dawitf.akahidegn.data.local.entity.GroupEntity>
    ): MediatorResult {
        return try {
            // For now, just return success. In a real implementation,
            // you would load data from remote and cache it locally
            MediatorResult.Success(endOfPaginationReached = false)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
