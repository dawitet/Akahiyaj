package com.dawitf.akahidegn.data.repository

import com.dawitf.akahidegn.domain.model.Group
import com.dawitf.akahidegn.domain.model.MemberInfo
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.core.result.success
import com.dawitf.akahidegn.core.result.failure
import com.dawitf.akahidegn.core.result.loading
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.data.remote.service.GroupService
import com.dawitf.akahidegn.domain.repository.GroupRepository
import com.dawitf.akahidegn.domain.service.LocationService
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepositoryImpl @Inject constructor(
    private val remoteDataSource: GroupService,
    private val locationService: LocationService
) : GroupRepository {

    // Repository scope for StateFlow - survives ViewModel lifecycle
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Optimistic state for immediate UI updates
    private val _optimisticGroups = MutableStateFlow<List<Group>>(emptyList())

    // StateFlow that provides lifecycle-aware group updates
    private val _groupsStateFlow: StateFlow<Result<List<Group>>> =
        combine(
            remoteDataSource.getAllGroups(),
            _optimisticGroups
        ) { remoteResult, optimisticGroups ->
            when (remoteResult) {
                is Result.Success -> {
                    val filteredGroups = remoteResult.data.filter { !it.isExpired() }
                    // Merge optimistic groups with remote groups (optimistic takes precedence)
                    val mergedGroups = mergeOptimisticWithRemote(optimisticGroups, filteredGroups)
                    success(mergedGroups)
                }
                is Result.Error -> remoteResult
                is Result.Loading -> {
                    if (optimisticGroups.isNotEmpty()) {
                        success(optimisticGroups)
                    } else {
                        remoteResult
                    }
                }
            }
        }
            .onStart {
                emit(loading())
            }
            .catch { throwable ->
                val appError = when (throwable) {
                    is com.google.firebase.database.DatabaseException -> 
                        AppError.NetworkError.FirebaseError(throwable.message ?: "Firebase error")
                    else -> 
                        AppError.NetworkError.UnknownNetworkError(throwable.message ?: "Unknown error")
                }
                emit(failure(appError.message ?: "Unknown error"))
            }
            .flowOn(Dispatchers.IO)
            .stateIn(
                scope = repositoryScope,
                started = SharingStarted.WhileSubscribed(
                    stopTimeoutMillis = 5_000,
                    replayExpirationMillis = 0
                ),
                initialValue = loading()
            )

    init {
        // Start automatic cleanup of expired groups every 5 minutes
        repositoryScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5 * 60 * 1000) // 5 minutes
                cleanupExpiredGroups()
            }
        }
    }

    override fun getAllGroups(): Flow<Result<List<Group>>> {
        return _groupsStateFlow
    }

    override fun getGroupsWithinRadius(userLat: Double, userLng: Double, radiusMeters: Double): Flow<Result<List<Group>>> {
        return _groupsStateFlow.map { result ->
            when (result) {
                is Result.Success -> {
                    val filteredGroups = result.data.filter { group ->
                        group.isWithinRadius(userLat, userLng, radiusMeters) && !group.isExpired()
                    }
                    success(filteredGroups)
                }
                else -> result
            }
        }
    }

    override suspend fun getGroupById(groupId: String): Result<Group> {
        return remoteDataSource.getGroupById(groupId)
    }

    override suspend fun createGroupOptimistic(group: Group): Result<Group> {
        // Check location permission first
        if (!locationService.hasLocationPermission.value) {
            return failure("Location permission required to create groups")
        }

        // Get current location
        val currentLocation = locationService.getCurrentLocationSync()
            ?: return failure("Unable to get current location")

        // Create group with location and expiration
        val groupWithLocation = group.copy(
            pickupLat = currentLocation.latitude,
            pickupLng = currentLocation.longitude,
            timestamp = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (30 * 60 * 1000), // 30 minutes
            status = "active"
        )

        // Optimistic UI: Add to local state immediately
        val currentOptimistic = _optimisticGroups.value.toMutableList()
        currentOptimistic.add(groupWithLocation)
        _optimisticGroups.value = currentOptimistic

        // Try to create remotely
        return try {
            val result = remoteDataSource.createGroup(groupWithLocation)
            if (result is Result.Error) {
                // Remove from optimistic state if remote creation failed
                _optimisticGroups.value = _optimisticGroups.value.filter { it.groupId != groupWithLocation.groupId }
            }
            result
        } catch (e: Exception) {
            // Remove from optimistic state on error
            _optimisticGroups.value = _optimisticGroups.value.filter { it.groupId != groupWithLocation.groupId }
            failure("Failed to create group: ${e.message}")
        }
    }

    override suspend fun joinGroupOptimistic(groupId: String, userId: String, userInfo: MemberInfo): Result<Unit> {
        // Check location permission first
        if (!locationService.hasLocationPermission.value) {
            return failure("Location permission required to join groups")
        }

        // Get current location to verify user is within range
        val currentLocation = locationService.getCurrentLocationSync()
            ?: return failure("Unable to get current location")

        // Find the group and check if user is within range
        val currentState = _groupsStateFlow.value
        if (currentState is Result.Success) {
            val group = currentState.data.find { it.groupId == groupId }
            if (group != null && !group.isWithinRadius(currentLocation.latitude, currentLocation.longitude)) {
                return failure("You must be within 500 meters of the group to join")
            }
        }

        // Optimistic UI: Update local state immediately
        val currentOptimistic = _optimisticGroups.value.toMutableList()
        val groupIndex = currentOptimistic.indexOfFirst { it.groupId == groupId }
        if (groupIndex != -1) {
            val updatedGroup = currentOptimistic[groupIndex].copy(
                members = currentOptimistic[groupIndex].members.apply { put(userId, true) },
                memberDetails = currentOptimistic[groupIndex].memberDetails.apply { put(userId, userInfo) },
                memberCount = currentOptimistic[groupIndex].memberCount + 1
            )
            currentOptimistic[groupIndex] = updatedGroup
            _optimisticGroups.value = currentOptimistic
        }

        // Try to join remotely
        return try {
            val result = remoteDataSource.joinGroup(groupId, userId, userInfo)
            if (result is Result.Error) {
                // Revert optimistic state if remote join failed
                revertOptimisticJoin(groupId, userId)
            }
            result
        } catch (e: Exception) {
            revertOptimisticJoin(groupId, userId)
            failure("Failed to join group: ${e.message}")
        }
    }

    private fun revertOptimisticJoin(groupId: String, userId: String) {
        val currentOptimistic = _optimisticGroups.value.toMutableList()
        val groupIndex = currentOptimistic.indexOfFirst { it.groupId == groupId }
        if (groupIndex != -1) {
            val updatedGroup = currentOptimistic[groupIndex].copy(
                members = currentOptimistic[groupIndex].members.apply { remove(userId) },
                memberDetails = currentOptimistic[groupIndex].memberDetails.apply { remove(userId) },
                memberCount = maxOf(0, currentOptimistic[groupIndex].memberCount - 1)
            )
            currentOptimistic[groupIndex] = updatedGroup
            _optimisticGroups.value = currentOptimistic
        }
    }

    private fun mergeOptimisticWithRemote(optimistic: List<Group>, remote: List<Group>): List<Group> {
        val optimisticIds = optimistic.map { it.groupId }.toSet()
        val mergedList = mutableListOf<Group>()

        // Add optimistic groups first (they take precedence)
        mergedList.addAll(optimistic)

        // Add remote groups that aren't in optimistic list
        remote.forEach { remoteGroup ->
            if (remoteGroup.groupId !in optimisticIds) {
                mergedList.add(remoteGroup)
            }
        }

        return mergedList
    }

    override suspend fun updateGroup(group: Group): Result<Group> {
        return remoteDataSource.updateGroup(group)
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        return remoteDataSource.deleteGroup(groupId)
    }

    override suspend fun leaveGroup(groupId: String, userId: String): Result<Unit> {
        return remoteDataSource.leaveGroup(groupId, userId)
    }

    override suspend fun syncGroups(): Result<Unit> {
        return success(Unit)
    }

    override suspend fun clearCache(): Result<Unit> {
        _optimisticGroups.value = emptyList()
        return success(Unit)
    }

    override suspend fun cleanupExpiredGroups(): Result<Unit> {
        return try {
            val currentTime = System.currentTimeMillis()
            remoteDataSource.getExpiredGroups(currentTime)
            success(Unit)
        } catch (e: Exception) {
            failure("Failed to cleanup expired groups: ${e.message}")
        }
    }

    override suspend fun refreshGroups(): Result<Unit> {
        // For now, a simple implementation can just return success.
        // You can add logic here later to force a remote data fetch if needed.
        return success(Unit)
    }

    override suspend fun getExpiredGroups(thresholdTimestamp: Long): Result<List<Group>> {
        return remoteDataSource.getExpiredGroups(thresholdTimestamp)
    }
}