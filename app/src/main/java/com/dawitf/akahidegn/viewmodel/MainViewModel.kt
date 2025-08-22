package com.dawitf.akahidegn.viewmodel

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawitf.akahidegn.domain.model.Group
import com.dawitf.akahidegn.domain.repository.GroupRepository
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.core.event.UiEventManager
import com.dawitf.akahidegn.domain.model.MemberInfo
import com.dawitf.akahidegn.performance.PerformanceCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.app.Application
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.dawitf.akahidegn.worker.operations.JoinGroupWorker
import java.util.UUID


/**
 * Enhanced ViewModel with location-based filtering and optimistic UI
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application,
    private val groupRepository: GroupRepository,
    private val uiEventManager: UiEventManager
) : ViewModel() {

    // Location state
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Current user ID state
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    // Loading states
    private val _isLoadingGroups = MutableStateFlow(false)
    val isLoadingGroups: StateFlow<Boolean> = _isLoadingGroups.asStateFlow()

    // Optimistic UI state for pending operations
    private val _pendingGroups = MutableStateFlow<List<Group>>(emptyList())
    private val _pendingJoins = MutableStateFlow<Set<String>>(emptySet())

    // Base groups from repository
    private val baseGroups = groupRepository.getAllGroups()
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading
        )

    // Filtered groups with location-based proximity and expiration checks
    private val filteredGroups = combine(
        baseGroups,
        _currentLocation,
        _searchQuery.debounce(300)
    ) { groupsResult, location, query ->
        when (groupsResult) {
            is Result.Loading -> {
                _isLoadingGroups.value = true
                emptyList()
            }
            is Result.Error -> {
                _isLoadingGroups.value = false
                Log.e("MainViewModel", "Error loading groups: ${groupsResult.error}")
                emptyList()
            }
            is Result.Success -> {
                _isLoadingGroups.value = false
                val allGroups = groupsResult.data

                // Filter by location (500m radius) and expiration (30 minutes)
                allGroups.filter { group ->
                    // Check if group is expired
                    if (group.isExpired()) {
                        Log.d("MainViewModel", "Filtering out expired group: ${group.groupId}")
                        return@filter false
                    }

                    // Check location proximity if user location is available
                    if (location != null && group.pickupLat != null && group.pickupLng != null) {
                        val isWithinRadius = group.isWithinRadius(location.latitude, location.longitude)
                        if (!isWithinRadius) {
                            Log.d("MainViewModel", "Filtering out distant group: ${group.groupId} (${group.getDistanceText(location.latitude, location.longitude)})")
                            return@filter false
                        }
                    }

                    // Search filter
                    if (query.isNotBlank()) {
                        val matchesSearch = group.destinationName?.contains(query, ignoreCase = true) == true ||
                                          group.originalDestination?.contains(query, ignoreCase = true) == true ||
                                          group.to?.contains(query, ignoreCase = true) == true
                        if (!matchesSearch) {
                            return@filter false
                        }
                    }

                    true
                }.sortedWith(compareBy<Group> {
                    // Sort by distance if location available, otherwise by creation time
                    if (location != null && it.pickupLat != null && it.pickupLng != null) {
                        it.calculateDistance(location.latitude, location.longitude, it.pickupLat!!, it.pickupLng!!)
                    } else {
                        -(it.timestamp ?: 0L) // Newest first if no location
                    }
                }.thenByDescending { it.timestamp })
            }
        }
    }.flowOn(Dispatchers.Default)

    // Main groups with optimistic UI updates
    val mainGroups: StateFlow<List<Group>> = combine(
        filteredGroups,
        _pendingGroups
    ) { filtered, pending ->
        // Merge filtered groups with pending optimistic updates
        val mergedGroups = mutableListOf<Group>()
        mergedGroups.addAll(pending) // Show pending groups first
        mergedGroups.addAll(filtered.filter { group ->
            // Avoid duplicates with pending groups
            pending.none { it.groupId == group.groupId }
        })
        mergedGroups
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // User's groups (groups they've joined or created)
    val userGroups: StateFlow<List<Group>> = combine(
        baseGroups,
        _currentUserId
    ) { groupsResult, userId ->
        when (groupsResult) {
            is Result.Success -> {
                if (userId != null) {
                    groupsResult.data.filter { group ->
                        group.members.containsKey(userId) && group.members[userId] == true
                    }
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setCurrentUserId(userId: String?) {
        _currentUserId.value = userId
    }

    fun setUserLocationFlow(locationFlow: StateFlow<Location?>) {
        viewModelScope.launch {
            locationFlow.collect { location ->
                _currentLocation.value = location
                if (location != null) {
                    Log.d("MainViewModel", "Location updated: ${location.latitude}, ${location.longitude}")
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Optimistic UI: Create group immediately, sync to Firebase in background
    fun createGroup(group: Group, currentUserId: String) {
        viewModelScope.launch {
            // Add to optimistic state immediately
            val currentPending = _pendingGroups.value.toMutableList()
            currentPending.add(0, group) // Add at top
            _pendingGroups.value = currentPending

            Log.d("MainViewModel", "Added group to optimistic UI: ${group.destinationName}")

            try {
                // Save to Firebase in background
                when (val result = groupRepository.createGroupOptimistic(group)) {
                    is Result.Success -> {
                        Log.d("MainViewModel", "Group saved to Firebase successfully")
                        // Remove from pending since it will appear in the main stream
                        removePendingGroup(group.groupId)
                    }
                    is Result.Error -> {
                        Log.e("MainViewModel", "Failed to save group to Firebase: ${result.error}")
                        // Remove from optimistic UI on failure
                        removePendingGroup(group.groupId)
                        uiEventManager.showError("Failed to create group: ${result.error}")
                    }
                    is Result.Loading -> {
                        // Keep in pending state
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Exception creating group", e)
                removePendingGroup(group.groupId)
                uiEventManager.showError("Failed to create group: ${e.message}")
            }
        }
    }

    // Optimistic UI: Join group immediately, sync to Firebase in background
    fun joinGroup(group: Group, currentUserId: String, userName: String) {
        val groupId = group.groupId ?: return

        viewModelScope.launch {
            // Add to optimistic state immediately
            val currentPending = _pendingJoins.value.toMutableSet()
            currentPending.add(groupId)
            _pendingJoins.value = currentPending

            Log.d("MainViewModel", "Added group join to optimistic UI: ${group.destinationName}")

            // --- THIS IS THE NEW PART THAT REPLACES THE OLD TRY-CATCH BLOCK ---

            // 1. Create a unique ID for this specific operation to track it.
            val operationId = UUID.randomUUID().toString()

            // 2. Prepare the data to send to the worker.
            //    This matches the KEYs you defined in JoinGroupWorker.
            val workData = workDataOf(
                JoinGroupWorker.KEY_GROUP_ID to groupId,
                JoinGroupWorker.KEY_USER_ID to currentUserId,
                JoinGroupWorker.KEY_OPERATION_ID to operationId,
                JoinGroupWorker.KEY_USER_NAME to userName // The crucial piece you identified!
            )

            // 3. Build the request to run our worker one time in the background.
            val joinGroupWorkRequest = OneTimeWorkRequestBuilder<JoinGroupWorker>()
                .setInputData(workData)
                .build()

            // 4. Give the request to the system to run.
            //    We use the application context we injected in Step 1.
            WorkManager.getInstance(application.applicationContext).enqueue(joinGroupWorkRequest)

            // The old `try-catch` block that called groupRepository.joinGroupOptimistic is now gone.
            // The worker is now responsible for that task.
        }
    }

    fun refreshGroups() {
        viewModelScope.launch {
            _isLoadingGroups.value = true
            try {
                groupRepository.refreshGroups()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error refreshing groups", e)
                uiEventManager.showError("Failed to refresh groups: ${e.message}")
            } finally {
                _isLoadingGroups.value = false
            }
        }
    }

    private fun removePendingGroup(groupId: String?) {
        if (groupId != null) {
            val currentPending = _pendingGroups.value.toMutableList()
            currentPending.removeAll { it.groupId == groupId }
            _pendingGroups.value = currentPending
        }
    }

    private fun removePendingJoin(groupId: String) {
        val currentPending = _pendingJoins.value.toMutableSet()
        currentPending.remove(groupId)
        _pendingJoins.value = currentPending
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("MainViewModel", "ViewModel cleared")
    }
}
