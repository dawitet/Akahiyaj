package com.dawitf.akahidegn.data.repository

import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.data.remote.service.GroupService
import com.dawitf.akahidegn.domain.repository.GroupRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepositoryImpl @Inject constructor(
    private val remoteDataSource: GroupService
) : GroupRepository {

    // Repository scope for StateFlow - survives ViewModel lifecycle
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // StateFlow that provides lifecycle-aware group updates
    // This is the reactive foundation that replaces manual Firebase listeners
    private val _groupsStateFlow: StateFlow<Result<List<Group>>> = 
        remoteDataSource.getAllGroups()
            .onStart { 
                // Emit loading state immediately
                emit(Result.loading()) 
            }
            .catch { throwable ->
                // Handle any upstream errors gracefully
                val appError = when (throwable) {
                    is com.google.firebase.database.DatabaseException -> 
                        AppError.NetworkError.FirebaseError(throwable.message ?: "Firebase error")
                    else -> 
                        AppError.NetworkError.UnknownNetworkError(throwable.message ?: "Unknown error")
                }
                emit(Result.failure(appError))
            }
            .flowOn(Dispatchers.IO) // Ensure Firebase operations are off main thread
            .stateIn(
                scope = repositoryScope,
                started = SharingStarted.WhileSubscribed(
                    stopTimeoutMillis = 5_000, // Stop listening 5 seconds after last subscriber
                    replayExpirationMillis = 0 // Always replay the latest value
                ),
                initialValue = Result.loading()
            )

    override fun getAllGroups(): Flow<Result<List<Group>>> {
        // Return the StateFlow as Flow for the interface contract
        // This provides automatic lifecycle management and memory leak prevention
        return _groupsStateFlow
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