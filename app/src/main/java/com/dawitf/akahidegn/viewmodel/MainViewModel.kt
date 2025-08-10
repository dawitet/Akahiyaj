package com.dawitf.akahidegn.viewmodel

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.GroupReader
import com.dawitf.akahidegn.domain.repository.GroupRepository
import com.dawitf.akahidegn.notifications.service.NotificationManagerService
import com.google.firebase.auth.FirebaseAuth
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
    private val groupRepository: GroupRepository,
    private val notificationManagerService: NotificationManagerService
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

    // User groups state
    private val _userGroups = MutableStateFlow<List<Group>>(emptyList())
    val userGroups: StateFlow<List<Group>> = _userGroups.asStateFlow()

    private val _isLoadingUserGroups = MutableStateFlow(false)
    val isLoadingUserGroups: StateFlow<Boolean> = _isLoadingUserGroups.asStateFlow()

    // Firebase references
    private lateinit var activeGroupsRef: DatabaseReference
    private val auth = FirebaseAuth.getInstance()
    
    // Real-time listeners for automatic updates
    private var realTimeGroupsListener: ValueEventListener? = null
    private var lastRealTimeUpdate = 0L
    private val realTimeUpdateThrottleMs = 2000L // Throttle updates to prevent spam
    
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
        setupPeriodicRefresh()
    }

    /**
     * Setup periodic refresh to catch updates from other users
     */
    private fun setupPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30 * 1000L) // Every 30 seconds
                
                // Only refresh if:
                // 1. Firebase is initialized
                // 2. User is authenticated  
                // 3. No search query is active (for main groups list)
                // 4. Not currently loading
                if (::activeGroupsRef.isInitialized && 
                    currentFirebaseUserId != null && 
                    _searchQuery.value.isBlank() && 
                    !_isLoadingGroups.value) {
                    
                    Log.d("MainViewModel", "Performing periodic refresh")
                    performGroupsSearch(null, forceRefresh = true)
                }
            }
        }
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
    private suspend fun performGroupsSearch(destinationFilter: String?, forceRefresh: Boolean = false) {
        Log.d("MainViewModel", "performGroupsSearch called. Filter: $destinationFilter, User ID: $currentFirebaseUserId, ForceRefresh: $forceRefresh")
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
        // Skip cache if forceRefresh is true (e.g., pull-to-refresh)
        if (!forceRefresh && destinationFilter != null && currentTime - lastSearchTime < 120 * 1000L && _groups.value.isNotEmpty()) {
            Log.d("MainViewModel", "Using cached search results for: $cacheKey")
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
        
        // Clear all caches to force fresh data
        groupsCache.clear() 
        lastSearchTimestamp.clear()
        
        val currentQuery = _searchQuery.value.trim().takeIf { it.isNotBlank() }
        viewModelScope.launch {
            // If currentQuery is null (blank), this will fetch all groups and filter by location.
            // If currentQuery is not null, it will search by text, and location filtering logic in passesFilters might be conditional.
            performGroupsSearch(currentQuery, forceRefresh = true)
        }
    }

    /**
     * Select a group for chat
     */
    fun selectGroup(group: Group?) {
        _selectedGroup.value = group
    }

    /**
     * Initialize Firebase and setup real-time listeners
     */
    fun initializeFirebase(activeGroupsRef: DatabaseReference, firebaseUserId: String) {
        Log.d("MainViewModel", "initializeFirebase called with user ID: $firebaseUserId")
        this.activeGroupsRef = activeGroupsRef
        this.currentFirebaseUserId = firebaseUserId
        Log.d("MainViewModel", "Firebase initialized with user ID: $firebaseUserId")
        
        // Setup real-time listener for automatic updates
        setupRealTimeListener()
        
        // Trigger initial load if not already loading and location is available or becomes available
        if (_currentLocation.value != null && _groups.value.isEmpty() && !_isLoadingGroups.value) {
            refreshGroups()
        }
    }

    /**
     * Setup real-time listener for automatic group updates
     */
    private fun setupRealTimeListener() {
        // Remove existing listener if any
        realTimeGroupsListener?.let { listener ->
            activeGroupsRef.removeEventListener(listener)
        }
        
        realTimeGroupsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentTime = System.currentTimeMillis()
                
                // Throttle real-time updates to prevent excessive processing
                if (currentTime - lastRealTimeUpdate < realTimeUpdateThrottleMs) {
                    Log.d("MainViewModel", "Real-time update throttled")
                    return
                }
                
                lastRealTimeUpdate = currentTime
                Log.d("MainViewModel", "Real-time data changed, children count: ${snapshot.childrenCount}")
                
                viewModelScope.launch(computationDispatcher) {
                    // Only update if we're not currently performing a manual search
                    if (!_isLoadingGroups.value) {
                        Log.d("MainViewModel", "Processing real-time update")
                        val currentQuery = _searchQuery.value.trim().takeIf { it.isNotBlank() }
                        processGroupsData(snapshot, currentQuery, "realtime_${currentTime}")
                    } else {
                        Log.d("MainViewModel", "Skipping real-time update during manual refresh")
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("MainViewModel", "Real-time listener cancelled: ${error.message}")
                // Attempt to reconnect after a delay
                viewModelScope.launch {
                    kotlinx.coroutines.delay(5000L)
                    if (::activeGroupsRef.isInitialized) {
                        Log.d("MainViewModel", "Attempting to reconnect real-time listener")
                        setupRealTimeListener()
                    }
                }
            }
        }
        
        // Add the listener to Firebase
        activeGroupsRef.orderByChild("createdAt")
            .limitToLast(100)
            .addValueEventListener(realTimeGroupsListener!!)
        
        Log.d("MainViewModel", "Real-time listener setup completed")
    }

    /**
     * Remove real-time listeners when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        
        // Clean up real-time listeners
        realTimeGroupsListener?.let { listener ->
            if (::activeGroupsRef.isInitialized) {
                activeGroupsRef.removeEventListener(listener)
            }
        }
        
        // Clear cache
        groupsCache.clear()
        
        Log.d("MainViewModel", "MainViewModel cleared - real-time listeners and cache cleaned up")
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

    /**
     * Load groups created or joined by the current user
     */
    fun loadUserGroups() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d("MainViewModel", "No authenticated user, cannot load user groups")
            _userGroups.value = emptyList()
            return
        }

        if (_isLoadingUserGroups.value) {
            Log.d("MainViewModel", "User groups already loading, skipping")
            return
        }

        _isLoadingUserGroups.value = true
        Log.d("MainViewModel", "Loading groups for user: ${currentUser.uid}")

        viewModelScope.launch(backgroundDispatcher) {
            try {
                activeGroupsRef.orderByChild("timestamp")
                    .limitToLast(50) // Get recent groups
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            viewModelScope.launch(computationDispatcher) {
                                val userGroupsList = mutableListOf<Group>()
                                
                                for (childSnapshot in snapshot.children) {
                                    try {
                                        val group = GroupReader.fromSnapshot(childSnapshot)
                                        if (group != null) {
                                            // Check if user is creator or member
                                            val isCreator = group.creatorId == currentUser.uid
                                            val isMember = group.members.containsKey(currentUser.uid) && 
                                                          group.members[currentUser.uid] == true
                                            
                                            if (isCreator || isMember) {
                                                group.groupId = childSnapshot.key
                                                userGroupsList.add(group)
                                                Log.d("MainViewModel", "Added user group: ${group.destinationName}, creator: $isCreator, member: $isMember")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MainViewModel", "Error reading user group: ${e.message}")
                                    }
                                }
                                
                                // Sort by timestamp descending (newest first)
                                val sortedGroups = userGroupsList.sortedByDescending { it.timestamp ?: 0L }
                                
                                withContext(Dispatchers.Main) {
                                    _userGroups.value = sortedGroups
                                    _isLoadingUserGroups.value = false
                                    Log.d("MainViewModel", "Loaded ${sortedGroups.size} user groups")
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("MainViewModel", "Failed to load user groups: ${error.message}")
                            _isLoadingUserGroups.value = false
                            _errorState.value = "Failed to load your groups: ${error.message}"
                        }
                    })
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading user groups", e)
                _isLoadingUserGroups.value = false
                _errorState.value = "An error occurred while loading your groups"
            }
        }
    }

    /**
     * Check if the current user is the creator of a specific group
     */
    fun isGroupCreator(group: Group): Boolean {
        val currentUser = auth.currentUser
        return currentUser != null && group.creatorId == currentUser.uid
    }

    /**
     * Refresh user groups
     */
    fun refreshUserGroups() {
        loadUserGroups()
    }

    /**
     * Disband a group (creator only)
     */
    fun disbandGroup(group: Group, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onError("You must be signed in to disband a group")
            return
        }

        if (group.creatorId != currentUser.uid) {
            onError("Only the group creator can disband this group")
            return
        }

        if (group.groupId == null) {
            onError("Invalid group ID")
            return
        }

        viewModelScope.launch(backgroundDispatcher) {
            try {
                // Delete the group from Firebase
                activeGroupsRef.child(group.groupId!!).removeValue()
                    .addOnSuccessListener {
                        Log.d("MainViewModel", "Group ${group.groupId} disbanded successfully")
                        viewModelScope.launch(Dispatchers.Main) {
                            // Send notification to group members about disbandment
                            notificationManagerService.showGroupDisbandedNotification(group)
                            
                            onSuccess()
                            // Refresh user groups to remove the disbanded group
                            loadUserGroups()
                            // Also refresh main groups list
                            refreshGroups()
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("MainViewModel", "Failed to disband group: ${exception.message}")
                        viewModelScope.launch(Dispatchers.Main) {
                            onError("Failed to disband group: ${exception.message}")
                        }
                    }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error disbanding group", e)
                viewModelScope.launch(Dispatchers.Main) {
                    onError("An error occurred while disbanding the group")
                }
            }
        }
    }

    /**
     * Leave a group (member only)
     */
    fun leaveGroup(group: Group, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onError("You must be signed in to leave a group")
            return
        }

        if (group.creatorId == currentUser.uid) {
            onError("Group creators cannot leave their own group. Use disband instead.")
            return
        }

        if (group.groupId == null) {
            onError("Invalid group ID")
            return
        }

        if (!group.members.containsKey(currentUser.uid) || group.members[currentUser.uid] != true) {
            onError("You are not a member of this group")
            return
        }

        viewModelScope.launch(backgroundDispatcher) {
            try {
                val groupRef = activeGroupsRef.child(group.groupId!!)
                
                // Remove user from members
                groupRef.child("members").child(currentUser.uid).removeValue()
                    .addOnSuccessListener {
                        // Remove user from member details
                        groupRef.child("memberDetails").child(currentUser.uid).removeValue()
                            .addOnSuccessListener {
                                // Update member count
                                val newMemberCount = maxOf(0, group.memberCount - 1)
                                groupRef.child("memberCount").setValue(newMemberCount)
                                    .addOnSuccessListener {
                                        Log.d("MainViewModel", "Left group ${group.groupId} successfully")
                                        viewModelScope.launch(Dispatchers.Main) {
                                            // Send notification to group creator about user leaving
                                            val userName = auth.currentUser?.displayName ?: "Someone"
                                            notificationManagerService.showUserLeftNotification(group, userName)
                                            
                                            onSuccess()
                                            // Refresh user groups to remove the left group
                                            loadUserGroups()
                                            // Also refresh main groups list
                                            refreshGroups()
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.e("MainViewModel", "Failed to update member count: ${exception.message}")
                                        viewModelScope.launch(Dispatchers.Main) {
                                            onError("Failed to leave group: ${exception.message}")
                                        }
                                    }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("MainViewModel", "Failed to remove member details: ${exception.message}")
                                viewModelScope.launch(Dispatchers.Main) {
                                    onError("Failed to leave group: ${exception.message}")
                                }
                            }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("MainViewModel", "Failed to remove from members: ${exception.message}")
                        viewModelScope.launch(Dispatchers.Main) {
                            onError("Failed to leave group: ${exception.message}")
                        }
                    }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error leaving group", e)
                viewModelScope.launch(Dispatchers.Main) {
                    onError("An error occurred while leaving the group")
                }
            }
        }
    }

    // Removed updateLocation function, will use setUserLocationFlow instead
}
