package com.dawitf.akahidegn.viewmodel

import android.location.Location
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
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

    // Paged groups for better performance
    val pagedGroups = groupRepository.getAllGroupsPaged()
        .cachedIn(viewModelScope)

    private val _isLoadingGroups = MutableStateFlow(false)
    val isLoadingGroups: StateFlow<Boolean> = _isLoadingGroups.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

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
     * Add a recent search with context for PreferenceManager
     */
    fun addRecentSearch(context: android.content.Context, search: String) {
        if (search.isBlank() || recentSearchesCache.contains(search)) return
        
        viewModelScope.launch(Dispatchers.IO) {
            // Use PreferenceManager directly here
            com.dawitf.akahidegn.PreferenceManager.addRecentSearch(context, search)
            
            // Update cache and UI
            recentSearchesCache.clear()
            recentSearchesCache.addAll(com.dawitf.akahidegn.PreferenceManager.getRecentSearches(context))
            
            withContext(Dispatchers.Main) {
                _recentSearches.value = recentSearchesCache.toList()
            }
        }
    }

    /**
     * Load recent searches from PreferenceManager
     */
    fun loadRecentSearches(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val searches = com.dawitf.akahidegn.PreferenceManager.getRecentSearches(context)
            recentSearchesCache.clear()
            recentSearchesCache.addAll(searches)
            
            withContext(Dispatchers.Main) {
                _recentSearches.value = searches
            }
        }
    }

    /**
     * Update current location and trigger nearby groups search
     */
    fun updateLocation(location: Location?) {
        _currentLocation.value = location
        // Trigger search when location is available
        if (location != null && currentFirebaseUserId != null) {
            viewModelScope.launch {
                performGroupsSearch(_searchQuery.value.trim().takeIf { it.isNotBlank() })
            }
        }
    }

    /**
     * Perform groups search with optimization and smart caching
     */
    private suspend fun performGroupsSearch(destinationFilter: String?) {
        Log.d("MainViewModel", "performGroupsSearch called. Filter: $destinationFilter, User ID: $currentFirebaseUserId")
        if (_isLoadingGroups.value) {
            Log.d("MainViewModel", "Search already in progress, skipping")
            return
        }

        // Check if we have cached results for this search
        val userLoc = _currentLocation.value
        val cacheKey = "${destinationFilter ?: "all"}_${userLoc?.let { "${it.latitude}_${it.longitude}" } ?: "no_location"}"
        val lastSearchTime = lastSearchTimestamp[cacheKey] ?: 0
        val currentTime = System.currentTimeMillis()
        
        // Use cached results if recent enough (within 2 minutes)
        if (currentTime - lastSearchTime < 2 * 60 * 1000L && _groups.value.isNotEmpty()) {
            Log.d("MainViewModel", "Using cached search results for: $cacheKey")
            return
        }

        _isLoadingGroups.value = true
        Log.d("MainViewModel", "Performing optimized groups search. Filter: '$destinationFilter'. User Location: ${_currentLocation.value?.latitude}, ${_currentLocation.value?.longitude}")

        try {
            withContext(backgroundDispatcher) {
                // Query by createdAt (used in toMap) instead of timestamp
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
                        }
                    })
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error during groups search", e)
            _isLoadingGroups.value = false
        }
    }

    /**
     * Process groups data on background thread with 500m location filtering
     */
    private suspend fun processGroupsData(snapshot: DataSnapshot, destinationFilter: String?, cacheKey: String) {
        val newGroupsList = mutableListOf<Group>()
        val userLocation = _currentLocation.value
        Log.d("MainViewModel", "Processing groups data. Snapshot children: ${snapshot.childrenCount}")

        for (groupSnapshot in snapshot.children) {
            val group = GroupReader.fromSnapshot(groupSnapshot)
            if (group != null) {
                // GroupId is already set by GroupReader
                
                // Use cache if available and group hasn't changed
                val cachedGroup = groupsCache[group.groupId]
                if (cachedGroup != null && cachedGroup.timestamp == group.timestamp) {
                    if (passesFilters(cachedGroup, destinationFilter, userLocation)) {
                        newGroupsList.add(cachedGroup)
                    }
                } else {
                    // Process new or updated group
                    if (passesFilters(group, destinationFilter, userLocation)) {
                        newGroupsList.add(group)
                        groupsCache[group.groupId!!] = group
                    }
                }
            }
        }

        // Sort and update UI on main thread
        val sortedList = newGroupsList.sortedByDescending { it.timestamp }
        
        // Update cache timestamp for this search
        lastSearchTimestamp[cacheKey] = System.currentTimeMillis()
        
        withContext(Dispatchers.Main) {
            _groups.value = sortedList
            _isLoadingGroups.value = false
            Log.d("MainViewModel", "Updated groups list with ${sortedList.size} groups within 500m")
        }
    }

    /**
     * Check if group passes filters - Location (500m) + expiry (30 minutes) + destination search
     */
    private fun passesFilters(group: Group, destinationFilter: String?, userLocation: Location?): Boolean {
        // Location filter - 500 meter radius
        if (userLocation != null && group.pickupLat != null && group.pickupLng != null) {
            val distance = calculateDistance(
                userLocation.latitude, userLocation.longitude,
                group.pickupLat!!, group.pickupLng!!
            )
            if (distance > searchRadiusMeters) {
                Log.d("MainViewModel", "Group '${group.originalDestination}' filtered out - ${distance.toInt()}m away")
                return false
            }
        } else if (userLocation == null) {
            // No user location available - don't show any groups
            return false
        }

        // Destination filter
        destinationFilter?.let { filter ->
            val searchableDestination = group.originalDestination ?: group.destinationName
            if (searchableDestination?.contains(filter, ignoreCase = true) != true) {
                return false
            }
        }

        // Only check if group is not expired (30 minutes)
        val thirtyMinutesAgo = System.currentTimeMillis() - (30 * 60 * 1000L)
        Log.d("MainViewModel", "Filtering group '${group.originalDestination}'. Group Timestamp: ${group.timestamp}, Thirty Minutes Ago: $thirtyMinutesAgo")
        if (group.timestamp != null && group.timestamp!! <= thirtyMinutesAgo) {
            Log.d("MainViewModel", "Group '${group.originalDestination}' filtered out - expired")
            return false
        }

        Log.d("MainViewModel", "Group '${group.originalDestination}' included - active and nearby")
        return true
    }

    /**
     * Calculate straight-line distance between two points (Euclidean distance)
     * Fast and sufficient for short distances like 500m
     */
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        
        // Convert to meters using simple approximation for short distances
        val deltaX = dLng * earthRadius * Math.cos(Math.toRadians((lat1 + lat2) / 2))
        val deltaY = dLat * earthRadius
        
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY)
    }

    /**
     * Force refresh groups (for pull-to-refresh scenarios)
     */
    fun refreshGroups() {
        groupsCache.clear() // Clear cache to force fresh data
        viewModelScope.launch {
            performGroupsSearch(_searchQuery.value.trim().takeIf { it.isNotBlank() })
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
        
        // Remove expired search timestamps
        lastSearchTimestamp.entries.removeAll { (_, timestamp) ->
            currentTime - timestamp > CACHE_EXPIRY_MS
        }
        
        // Limit groups cache size
        if (groupsCache.size > MAX_GROUPS_CACHE_SIZE) {
            val entriesToRemove = groupsCache.size - MAX_GROUPS_CACHE_SIZE
            val oldestEntries = groupsCache.entries.take(entriesToRemove)
            oldestEntries.forEach { groupsCache.remove(it.key) }
        }
        
        // Limit recent searches
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
                    // Last attempt failed
                    _errorState.value = "Failed to $operationName after $maxRetries attempts"
                    return@withContext Result.failure(e)
                }
                
                // Exponential backoff: wait longer between retries
                val delayMs = baseRetryDelayMs * (1 shl attempt) // 1s, 2s, 4s
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

    override fun onCleared() {
        super.onCleared()
        groupsCache.clear()
    }
}
