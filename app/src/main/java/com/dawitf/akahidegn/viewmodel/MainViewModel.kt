package com.dawitf.akahidegn.viewmodel

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.domain.repository.GroupRepository
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.core.event.UiEventManager
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

/**
 * Refactored ViewModel using StateFlow for reactive data streams
 * 
 * PHASE 1: Repository StateFlow Migration
 * - Replaces manual Firebase listener management with reactive StateFlow
 * - Provides lifecycle-aware data observation
 * - Eliminates memory leaks from manual listener cleanup
 * - Centralized error handling and loading states
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val uiEventManager: UiEventManager
) : ViewModel() {


    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Location state
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    // Recent searches cache
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    // Selected group state
    private val _selectedGroup = MutableStateFlow<Group?>(null)
    val selectedGroup: StateFlow<Group?> = _selectedGroup.asStateFlow()

    // Raw groups from repository (StateFlow from Phase 1)
    private val rawGroupsResult: StateFlow<Result<List<Group>>> = groupRepository
        .getAllGroups()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Result.Loading
        )

    // Loading state derived from raw groups result
    val isLoadingGroups: StateFlow<Boolean> = rawGroupsResult
        .map { it is Result.Loading }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = true
        )

    // Error state derived from raw groups result  
    val errorState: StateFlow<String?> = rawGroupsResult
        .map { result ->
            when (result) {
                is Result.Error -> result.error.toString()
                else -> null
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    // UI Events flow for one-time events (toasts, navigation, etc.)
    val uiEvents = uiEventManager.uiEvents


    // Track current user ID for filtering
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()
    fun setCurrentUserId(userId: String?) { _currentUserId.value = userId }

    // Main tab: groups within 500m radius (default)
    val mainGroups: StateFlow<List<Group>> = combine(
        rawGroupsResult,
        searchQuery.debounce(300),
        currentLocation
    ) { result: Result<List<Group>>, query: String, location: Location? ->
        when (result) {
            is Result.Success -> {
                val allGroups = result.data
                if (allGroups.isEmpty()) return@combine emptyList()
                allGroups.filter { group ->
                    // Only groups within 500m and not expired
                    passesOptimizedFilters(group, "", location)
                }.sortedByDescending { it.timestamp }
            }
            else -> emptyList()
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = emptyList()
    )

    // Active tab: groups user has joined or created and are still active
    val activeGroups: StateFlow<List<Group>> = combine(
        rawGroupsResult,
        searchQuery.debounce(300),
        currentLocation,
        currentUserId
    ) { result: Result<List<Group>>, query: String, location: Location?, userId: String? ->
        when (result) {
            is Result.Success -> {
                val allGroups = result.data
                if (allGroups.isEmpty() || userId.isNullOrBlank()) return@combine emptyList()
                allGroups.filter { group ->
                    // User is creator or member, and group is not expired
                    val ts = group.timestamp
                    (group.creatorId == userId || group.memberDetails.containsKey(userId)) &&
                    (ts == null || (ts is Long && ts > System.currentTimeMillis() - (30 * 60 * 1000L)))
                }.sortedByDescending { it.timestamp }
            }
            else -> emptyList()
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = emptyList()
    )

    // History tab: groups user has created (regardless of expiry)
    val historyGroups: StateFlow<List<Group>> = combine(
        rawGroupsResult,
        currentUserId
    ) { result: Result<List<Group>>, userId: String? ->
        when (result) {
            is Result.Success -> {
                val allGroups = result.data
                if (allGroups.isEmpty() || userId.isNullOrBlank()) return@combine emptyList()
                allGroups.filter { group ->
                    group.creatorId == userId
                }.sortedByDescending { it.timestamp }
            }
            else -> emptyList()
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = emptyList()
    )

    // Groups tab: all groups the user created or joined (active and inactive), sorted by recency
    val userGroups: StateFlow<List<Group>> = combine(
        rawGroupsResult,
        currentUserId
    ) { result: Result<List<Group>>, userId: String? ->
        when (result) {
            is Result.Success -> {
                val allGroups = result.data
                if (allGroups.isEmpty() || userId.isNullOrBlank()) return@combine emptyList()
                allGroups.filter { group ->
                    group.creatorId == userId || group.memberDetails.containsKey(userId)
                }.sortedByDescending { it.timestamp }
            }
            else -> emptyList()
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = emptyList()
    )


    // Search configuration
    private val searchRadiusMeters = 500.0
    private val maxRecentSearches = 20

    // Location updates - now clean and simple with StateFlow
    fun setUserLocationFlow(locationFlow: StateFlow<Location?>) {
        viewModelScope.launch {
            locationFlow.collect { newLocation ->
                _currentLocation.value = newLocation
                Log.d("MainViewModel", "User location updated: $newLocation")
                // StateFlow reactive chains automatically handle location changes
                // No manual refresh needed - the combine flow handles it
            }
        }
    }

    /**
     * Update search query - triggers reactive filtering automatically
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        // Add to recent searches if not empty and not already present
        if (query.isNotBlank() && !_recentSearches.value.contains(query)) {
            val updated = (_recentSearches.value + query).takeLast(maxRecentSearches)
            _recentSearches.value = updated
        }
    }

    /**
     * Clear search query and results
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }

    /**
     * Select a group for detailed view
     */
    fun selectGroup(group: Group?) {
        _selectedGroup.value = group
    }

    /**
     * Clear selected group
     */
    fun clearSelectedGroup() {
        _selectedGroup.value = null
    }

    /**
     * Load a single group by its ID into selectedGroup state
     */
    fun loadGroupById(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (val result = groupRepository.getGroupById(groupId)) {
                    is com.dawitf.akahidegn.core.result.Result.Success -> {
                        _selectedGroup.value = result.data
                    }
                    is com.dawitf.akahidegn.core.result.Result.Error -> {
                        Log.w("MainViewModel", "Failed to load group $groupId: ${result.error}")
                        uiEventManager.showError("Failed to load group details")
                    }
                    is com.dawitf.akahidegn.core.result.Result.Loading -> {
                        // no-op
                    }
                }
            } catch (t: Throwable) {
                Log.e("MainViewModel", "Exception loading group $groupId", t)
                uiEventManager.showError("Failed to load group details")
            }
        }
    }

    /**
     * Optimized filter function with caching for performance
     * This is used by the reactive filtering chain
     */
    private fun passesOptimizedFilters(group: Group, queryFilter: String, userLocation: Location?): Boolean {
        // Check expiry first (fastest check)
        val thirtyMinutesAgo = System.currentTimeMillis() - (30 * 60 * 1000L)
        val timestamp = group.timestamp
        if (timestamp != null && timestamp <= thirtyMinutesAgo) {
            return false
        }

        // Text filter check
        if (queryFilter.isNotBlank()) {
            val searchableDestination = group.originalDestination ?: group.destinationName
            if (searchableDestination?.contains(queryFilter, ignoreCase = true) != true) {
                return false
            }
        }

        // Location filter (only for nearby/no-search queries)
        if (queryFilter.isBlank()) {
            if (userLocation == null || group.pickupLat == null || group.pickupLng == null) {
                return false
            }
            
            // Use cached distance if available
            val distance = PerformanceCache.getCachedDistance(
                userLocation.latitude, userLocation.longitude,
                group.pickupLat!!, group.pickupLng!!
            ) ?: run {
                val calculated = calculateDistance(
                    userLocation.latitude, userLocation.longitude,
                    group.pickupLat!!, group.pickupLng!!
                )
                PerformanceCache.cacheDistance(
                    userLocation.latitude, userLocation.longitude,
                    group.pickupLat!!, group.pickupLng!!, calculated
                )
                calculated
            }
            
            if (distance > searchRadiusMeters) {
                return false
            }
        }

        return true
    }

    /**
     * Force refresh groups - now simply clears caches since StateFlow handles updates automatically
     */
    fun refreshGroups() {
        Log.d("MainViewModel", "Refreshing groups - clearing caches and optimistic data")
        PerformanceCache.clearAll()
        // groupRepository.clearOptimisticData() // Optimistic functionality temporarily disabled
        // StateFlow automatically provides fresh data - no manual refresh needed!
    }

    // ============ OPTIMISTIC UI OPERATIONS (Phase 2) ============
    
    /**
     * Create a group with optimistic UI update for instant feedback
     */
    fun createGroup(group: Group, userId: String) {
        viewModelScope.launch {
            groupRepository.createGroup(group)
        }
    }
    
    /**
     * Join a group with optimistic UI update for instant feedback
     */
    fun joinGroup(groupId: String, userId: String, userName: String) {
        viewModelScope.launch {
            groupRepository.joinGroup(groupId, userId)
        }
    }
    
    /**
     * Leave a group with optimistic UI update for instant feedback
     */
    fun leaveGroup(groupId: String, userId: String) {
        viewModelScope.launch {
            groupRepository.leaveGroup(groupId, userId)
        }
    }
    
    /**
     * Update a group with optimistic UI update for instant feedback
     */
    fun updateGroup(group: Group) {
        viewModelScope.launch {
            groupRepository.updateGroup(group)
        }
    }
    
    /**
     * Delete a group with optimistic UI update for instant feedback
     */
    fun deleteGroup(groupId: String, userId: String) {
        viewModelScope.launch {
            groupRepository.deleteGroup(groupId)
        }
    }
    
    /**
     * Check if a group is currently in an optimistic state (useful for UI indicators)
     */
    fun isGroupOptimistic(groupId: String): Boolean {
        return false // Optimistic functionality temporarily disabled
    }

    // UI Event convenience methods
    fun showToast(message: String) = uiEventManager.showToast(message)
    fun showError(error: String) = uiEventManager.showError(error)
    fun showSuccess(message: String) = uiEventManager.showSuccess(message)
    fun navigateToGroupDetails(groupId: String) = uiEventManager.navigateToGroupDetails(groupId)
    fun requestLocationPermission() = uiEventManager.requestLocationPermission()
    
    fun onGroupCreated(groupId: String, groupName: String) {
        uiEventManager.groupCreatedSuccess(groupId, groupName)
        refreshGroups() // Refresh the group list
    }
    
    fun onGroupJoined(groupId: String, groupName: String) {
        uiEventManager.groupJoinedSuccess(groupId, groupName)
        refreshGroups() // Refresh the group list
    }
    
    fun onGroupLeft(groupName: String) {
        uiEventManager.groupLeftSuccess(groupName)
        refreshGroups() // Refresh the group list
    }

    // ========== UI-Friendly Optimistic Operations ==========
    
    /**
     * Join a group with optimistic UI update for instant feedback
     */
    fun joinGroup(group: Group, userId: String, userName: String) {
        joinGroup(group.groupId!!, userId, userName)
    }
    
    /**
     * Calculate straight-line distance between two points using Android Location
     * Fast and accurate for short distances like 500m radius filtering
     */
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val startPoint = Location("start").apply {
            latitude = lat1
            longitude = lng1
        }
        val endPoint = Location("end").apply {
            latitude = lat2
            longitude = lng2
        }
        return startPoint.distanceTo(endPoint).toDouble() // Returns distance in meters
    }
}
