package com.dawitf.akahidegn.viewmodel

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.GroupReader
import com.dawitf.akahidegn.domain.repository.GroupRepository
import com.google.firebase.database.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for managing app state and repository operations off the main thread
 * This addresses performance issues by:
 * 1. Moving repository operations to background threads
 * 2. Debouncing search queries to prevent excessive API calls
 * 3. Using StateFlow for efficient state management
 * 4. Caching recent searches in memory
 * 5. Using Paging for efficient data loading
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {

    // State flows for UI state
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _isLoadingGroups = MutableStateFlow(false)
    val isLoadingGroups: StateFlow<Boolean> = _isLoadingGroups.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    // _currentLocation is updated by setUserLocationFlow from MainActivity
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _selectedGroup = MutableStateFlow<Group?>(null)
    val selectedGroup: StateFlow<Group?> = _selectedGroup.asStateFlow()

    // Firebase references
    private lateinit var activeGroupsRef: DatabaseReference
    
    // Current search parameters
    // Search configuration - 500 meter radius for location-based filtering
    private val searchRadiusMeters = 500.0
    private val maxRetries = 3
    private var currentFirebaseUserId: String? = null

    // Advanced caching and performance optimization
    private val groupsCache = mutableMapOf<String, Group>()
    private val recentSearchesCache = mutableListOf<String>()
    private val lastSearchTimestamp = mutableMapOf<String, Long>()
    private val CACHE_EXPIRY_MS = 5 * 60 * 1000L // 5 minutes
    
    // Memory optimization: Limit cache sizes
    private val MAX_GROUPS_CACHE_SIZE = 200
    private val MAX_RECENT_SEARCHES_SIZE = 20

    // Background dispatcher for heavy operations
    private val backgroundDispatcher = Dispatchers.IO
    private val computationDispatcher = Dispatchers.Default

    // Enhanced error handling and resilience
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()
        
    private var retryCount = 0
    private val baseRetryDelayMs = 1000L
    
    init {
        setupSearchQueryDebouncing()
        setupMemoryCleanup()
    }

    fun setUserLocationFlow(locationFlow: StateFlow<Location?>) {
        viewModelScope.launch {
            // *** FIX 2: REMOVE THE REDUNDANT .distinctUntilChanged() ***
            locationFlow.collect { newLocation ->
                val oldLocation = _currentLocation.value
                _currentLocation.value = newLocation
                Log.d("MainViewModel", "User location updated in ViewModel: $newLocation")
                // and no active text search is happening.
                if (_searchQuery.value.isBlank()) {
                    val distanceChange = oldLocation?.distanceTo(newLocation ?: Location("")) ?: Float.MAX_VALUE
                    // Refresh if new location is known, or if it became unknown,
                    // or if it moved by more than searchRadiusMeters/2 (heuristic)
                    if (newLocation != null || oldLocation != null || distanceChange > searchRadiusMeters / 2) {
                         Log.d("MainViewModel", "Location changed, refreshing groups for non-search scenario.")
                         performGroupsSearch(null) // Pass null to get all groups, filtering will happen
                    }
                }
                 // If a search query is active, the debounced search will pick up the new location.
            }
        }
    }
    
    /**
     * Setup debounced search to prevent excessive Firebase calls
     */
    private fun setupSearchQueryDebouncing() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Wait 300ms after user stops typing
                .distinctUntilChanged() // Only proceed if the query actually changed
                .filter { currentFirebaseUserId != null } // Only search if authenticated
                .collect { query ->
                    Log.d("MainViewModel", "Debounced search triggered for: '$query'")
                    performGroupsSearch(query.trim().takeIf { it.isNotBlank() })
                }
        }
    }

    /**
     * Update search query - automatically triggers debounced search
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Perform groups search with optimization and smart caching
     */
    private suspend fun performGroupsSearch(destinationFilter: String?) {
        Log.d("MainViewModel", "performGroupsSearch called. Filter: $destinationFilter, User ID: $currentFirebaseUserId")
        if (!::activeGroupsRef.isInitialized || currentFirebaseUserId == null) {
            Log.w("MainViewModel", "Firebase not initialized, skipping search.")
            _isLoadingGroups.value = false // Ensure loading state is reset
            _groups.value = emptyList() // Clear groups if not initialized
            return
        }

        if (_isLoadingGroups.value) {
            Log.d("MainViewModel", "Search already in progress, skipping")
            return
        }

        val userLoc = _currentLocation.value
        val cacheKey = "${destinationFilter ?: "all"}_${userLoc?.let { "${it.latitude}_${it.longitude}" } ?: "no_location"}"
        val lastSearchTime = lastSearchTimestamp[cacheKey] ?: 0
        val currentTime = System.currentTimeMillis()
        
        // Use cached results if recent enough (within 2 minutes)
        // AND if the current groups list is not empty (implying a successful previous fetch for this state)
        // If destinationFilter is null (swipe refresh or location change for "all nearby"), we might want to bypass cache more aggressively
        // or ensure cache is invalidated properly. The current refreshGroups() clears groupsCache.
        if (destinationFilter != null && currentTime - lastSearchTime < 120 * 1000L && _groups.value.isNotEmpty()) {
            Log.d("MainViewModel", "Using cached search results for: $cacheKey")
            // Potentially re-apply filter if userLocation changed but filter text is same
            // For simplicity, current refreshGroups clears cache, forcing fresh data.
            return 
        }

        _isLoadingGroups.value = true
        Log.d("MainViewModel", "Performing optimized groups search. Filter: '$destinationFilter'. User Location: ${_currentLocation.value?.latitude}, ${_currentLocation.value?.longitude}")

        try {
            withContext(backgroundDispatcher) {
                activeGroupsRef.orderByChild("createdAt")
                    .limitToLast(100)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            Log.d("MainViewModel", "Firebase snapshot received. Children count: ${snapshot.childrenCount}")
                            viewModelScope.launch(computationDispatcher) {
                                processGroupsData(snapshot, destinationFilter, cacheKey)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("MainViewModel", "Groups search failed: ${error.message}")
                            _isLoadingGroups.value = false
                            _errorState.value = "Failed to load groups: ${error.message}"
                        }
                    })
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error during groups search", e)
            _isLoadingGroups.value = false
            _errorState.value = "An error occurred while searching for groups."
        }
    }

    /**
     * Process groups data on background thread with 500m location filtering
     */
    private suspend fun processGroupsData(snapshot: DataSnapshot, destinationFilter: String?, cacheKey: String) {
        val newGroupsList = mutableListOf<Group>()
        val userLocationForFilter = _currentLocation.value // Use the latest location
        Log.d("MainViewModel", "Processing groups data. Snapshot children: ${snapshot.childrenCount}, UserLocation for filter: $userLocationForFilter")

        for (groupSnapshot in snapshot.children) {
            val group = GroupReader.fromSnapshot(groupSnapshot)
            if (group != null) {
                val cachedGroup = groupsCache[group.groupId]
                if (cachedGroup != null && cachedGroup.timestamp == group.timestamp && destinationFilter != null) { // Only use cache for specific text searches
                    if (passesFilters(cachedGroup, destinationFilter, userLocationForFilter)) {
                        newGroupsList.add(cachedGroup)
                    }
                } else {
                    if (passesFilters(group, destinationFilter, userLocationForFilter)) {
                        newGroupsList.add(group)
                        if (group.groupId != null) { // Ensure groupId is not null before caching
                           groupsCache[group.groupId!!] = group
                        }
                    }
                }
            }
        }
        
        val sortedList = newGroupsList.sortedByDescending { it.timestamp }
        lastSearchTimestamp[cacheKey] = System.currentTimeMillis()
        
        withContext(Dispatchers.Main) {
            _groups.value = sortedList
            _isLoadingGroups.value = false
            Log.d("MainViewModel", "Updated groups list with ${sortedList.size} groups that pass filters.")
        }
    }

    /**
     * Check if group passes filters - Location (500m) + expiry (30 minutes) + destination search
     */
    private fun passesFilters(group: Group, destinationFilter: String?, userLocation: Location?): Boolean {
        // Active status check (if your Group model has a status field)
        // if (group.status != "active") return false 

        val thirtyMinutesAgo = System.currentTimeMillis() - (30 * 60 * 1000L)
        val timestamp = group.timestamp
        if (timestamp != null && timestamp <= thirtyMinutesAgo) {
            return false
        }

        // Location filter - 500 meter radius
        // This filter is only applied if there is NO destinationFilter.
        // If there IS a destinationFilter, we show all matching groups regardless of distance.
        // For the main screen (no search text), destinationFilter will be null.
        if (destinationFilter == null) { // Only apply distance filter if no specific destination is searched
            if (userLocation == null) {
                 Log.d("MainViewModel", "Group '${group.originalDestination}' filtered out - user location unknown for proximity check.")
                return false // No user location, so no group is "nearby"
            }
            if (group.pickupLat != null && group.pickupLng != null) {
                val distance = calculateDistance(
                    userLocation.latitude, userLocation.longitude,
                    group.pickupLat!!, group.pickupLng!!
                )
                if (distance > searchRadiusMeters) {
                    // Log.d("MainViewModel", "Group '${group.originalDestination}' filtered out by distance - ${distance.toInt()}m away")
                    return false
                }
            } else {
                // Log.d("MainViewModel", "Group '${group.originalDestination}' filtered out - group has no location for proximity check.")
                return false // Group has no location, cannot be considered "nearby"
            }
        }


        // Destination text filter (applies if destinationFilter is not null)
        destinationFilter?.let { filter ->
            val searchableDestination = group.originalDestination ?: group.destinationName
            if (searchableDestination?.contains(filter, ignoreCase = true) != true) {
                // Log.d("MainViewModel", "Group '${group.originalDestination}' filtered out by text filter: '$filter'")
                return false
            }
        }
        
        // Log.d("MainViewModel", "Group '${group.originalDestination}' included. Filter: $destinationFilter, UserLoc: ${userLocation != null}")
        return true
    }

    /**
     * Calculate straight-line distance between two points (Euclidean distance)
     * Fast and sufficient for short distances like 500m
     */
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        // Using Android Location's built-in distanceTo for accuracy and simplicity
        val startPoint = Location("start")
        startPoint.latitude = lat1
        startPoint.longitude = lng1

        val endPoint = Location("end")
        endPoint.latitude = lat2
        endPoint.longitude = lng2
        
        return startPoint.distanceTo(endPoint).toDouble() // distanceTo returns float in meters
    }

    /**
     * Force refresh groups (for pull-to-refresh scenarios or initial load without search text)
     */
    fun refreshGroups() {
        Log.d("MainViewModel", "refreshGroups called, clearing cache and performing search with current query: '${_searchQuery.value}' or null if blank")
        groupsCache.clear() 
        val currentQuery = _searchQuery.value.trim().takeIf { it.isNotBlank() }
        viewModelScope.launch {
            // If currentQuery is null (blank), this will fetch all groups and filter by location.
            // If currentQuery is not null, it will search by text, and location filtering logic in passesFilters might be conditional.
            performGroupsSearch(currentQuery)
        }
    }

    /**
     * Select a group for chat
     */
    fun selectGroup(group: Group?) {
        _selectedGroup.value = group
    }

    /**
     * Initialize Firebase references and user ID for the ViewModel
     */
    fun initializeFirebase(activeGroupsRef: DatabaseReference, firebaseUserId: String) {
        Log.d("MainViewModel", "initializeFirebase called with user ID: $firebaseUserId")
        this.activeGroupsRef = activeGroupsRef
        this.currentFirebaseUserId = firebaseUserId
        Log.d("MainViewModel", "Firebase initialized with user ID: $firebaseUserId")
        // Trigger initial load if not already loading and location is available or becomes available
        if (_currentLocation.value != null && _groups.value.isEmpty() && !_isLoadingGroups.value) {
            refreshGroups()
        }
    }

    /**
     * Setup periodic memory cleanup to prevent memory leaks
     */
    private fun setupMemoryCleanup() {
        viewModelScope.launch(backgroundDispatcher) {
            while (true) {
                kotlinx.coroutines.delay(10 * 60 * 1000L) // Every 10 minutes
                cleanupMemory()
            }
        }
    }
    
    /**
     * Clean up expired cache entries and limit memory usage
     */
    private suspend fun cleanupMemory() = withContext(computationDispatcher) {
        val currentTime = System.currentTimeMillis()
        
        lastSearchTimestamp.entries.removeAll { (_, timestamp) ->
            currentTime - timestamp > CACHE_EXPIRY_MS
        }
        
        if (groupsCache.size > MAX_GROUPS_CACHE_SIZE) {
            val entriesToRemove = groupsCache.size - MAX_GROUPS_CACHE_SIZE
            // Evict oldest entries based on insertion order (less ideal than LRU but simpler here)
            val keysToRemove = groupsCache.keys.take(entriesToRemove)
            keysToRemove.forEach { groupsCache.remove(it) }
        }
        
        if (recentSearchesCache.size > MAX_RECENT_SEARCHES_SIZE) {
            val limitedSearches = recentSearchesCache.takeLast(MAX_RECENT_SEARCHES_SIZE)
            recentSearchesCache.clear()
            recentSearchesCache.addAll(limitedSearches)
            _recentSearches.value = limitedSearches.toList()
        }
        
        Log.d("MainViewModel", "Memory cleanup completed. Cache sizes - Groups: ${groupsCache.size}, Searches: ${recentSearchesCache.size}")
    }

    /**
     * Execute operations with retry logic and exponential backoff
     */
    private suspend fun <T> executeWithRetry(
        operationName: String,
        operation: suspend () -> T
    ): Result<T> = withContext(backgroundDispatcher) {
        repeat(maxRetries) { attempt ->
            try {
                val result = operation()
                if (attempt > 0) {
                    Log.i("MainViewModel", "$operationName succeeded after $attempt retries")
                }
                retryCount = 0 // Reset on success
                return@withContext Result.success(result)
            } catch (e: Exception) {
                Log.w("MainViewModel", "$operationName failed on attempt ${attempt + 1}: ${e.message}")
                
                if (attempt == maxRetries - 1) {
                    _errorState.value = "Failed to $operationName after $maxRetries attempts"
                    return@withContext Result.failure(e)
                }
                
                val delayMs = baseRetryDelayMs * (1 shl attempt) 
                kotlinx.coroutines.delay(delayMs)
            }
        }
        Result.failure(Exception("Unexpected retry loop exit"))
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _errorState.value = null
    }

    // Removed updateLocation function, will use setUserLocationFlow instead

    override fun onCleared() {
        super.onCleared()
        groupsCache.clear() // Clear cache when ViewModel is destroyed
        Log.d("MainViewModel", "MainViewModel cleared.")
    }
}
