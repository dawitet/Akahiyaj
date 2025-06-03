package com.dawitf.akahidegn.viewmodel

import android.location.Location
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.dawitf.akahidegn.ChatMessage
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.domain.repository.ChatRepository
import com.dawitf.akahidegn.domain.repository.GroupRepository
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
    private val chatRepository: ChatRepository
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

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _selectedGroup = MutableStateFlow<Group?>(null)
    val selectedGroup: StateFlow<Group?> = _selectedGroup.asStateFlow()

    // Firebase references
    private lateinit var activeGroupsRef: DatabaseReference
    private lateinit var groupChatsRef: DatabaseReference
    
    // Current search parameters
    private var currentSearchRadiusMeters = 1000
    private var currentFirebaseUserId: String? = null
    private var chatValueEventListener: ValueEventListener? = null
    private var currentChatGroupId: String? = null

    // Advanced caching and performance optimization
    private val groupsCache = mutableMapOf<String, Group>()
    private val recentSearchesCache = mutableListOf<String>()
    private val lastSearchTimestamp = mutableMapOf<String, Long>()
    private val CACHE_EXPIRY_MS = 5 * 60 * 1000L // 5 minutes
    
    // Memory optimization: Limit cache sizes
    private val MAX_GROUPS_CACHE_SIZE = 200
    private val MAX_CHAT_MESSAGES_SIZE = 100
    private val MAX_RECENT_SEARCHES_SIZE = 20

    // Background dispatcher for heavy operations
    private val backgroundDispatcher = Dispatchers.IO
    private val computationDispatcher = Dispatchers.Default

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
     * Update current location and trigger nearby groups search if needed
     */
    fun updateLocation(location: Location?) {
        _currentLocation.value = location
        // Only trigger search if we have a location and we're not currently searching
        if (location != null && !_isLoadingGroups.value && currentFirebaseUserId != null) {
            viewModelScope.launch {
                performGroupsSearch(_searchQuery.value.trim().takeIf { it.isNotBlank() })
            }
        }
    }

    /**
     * Perform groups search with optimization and smart caching
     */
    private suspend fun performGroupsSearch(destinationFilter: String?) {
        if (_isLoadingGroups.value) {
            Log.d("MainViewModel", "Search already in progress, skipping")
            return
        }

        // Check if we have cached results for this search
        val cacheKey = "${destinationFilter ?: "all"}_${_currentLocation.value?.let { "${it.latitude}_${it.longitude}" } ?: "no_location"}"
        val lastSearchTime = lastSearchTimestamp[cacheKey] ?: 0
        val currentTime = System.currentTimeMillis()
        
        // Use cached results if recent enough (within 2 minutes)
        if (currentTime - lastSearchTime < 2 * 60 * 1000L && _groups.value.isNotEmpty()) {
            Log.d("MainViewModel", "Using cached search results for: $cacheKey")
            return
        }

        _isLoadingGroups.value = true
        Log.d("MainViewModel", "Performing optimized groups search. Filter: '$destinationFilter'")

        try {
            withContext(backgroundDispatcher) {
                activeGroupsRef.orderByChild("timestamp")
                    .limitToLast(100)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
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
     * Process groups data on background thread with optimized caching and filtering
     */
    private suspend fun processGroupsData(snapshot: DataSnapshot, destinationFilter: String?, cacheKey: String) {
        val newGroupsList = mutableListOf<Group>()
        val userLocation = _currentLocation.value

        for (groupSnapshot in snapshot.children) {
            val group = groupSnapshot.getValue(Group::class.java)
            if (group != null) {
                group.groupId = groupSnapshot.key
                
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
            Log.d("MainViewModel", "Updated groups list with ${sortedList.size} groups")
        }
    }

    /**
     * Check if group passes all filters
     */
    private fun passesFilters(group: Group, destinationFilter: String?, userLocation: Location?): Boolean {
        // Destination filter
        destinationFilter?.let { filter ->
            if (group.destinationName?.contains(filter, ignoreCase = true) != true) {
                return false
            }
        }

        // Proximity filter
        if (userLocation != null && group.pickupLat != null && group.pickupLng != null) {
            val groupLocation = Location("group").apply {
                latitude = group.pickupLat!!
                longitude = group.pickupLng!!
            }
            val distance = userLocation.distanceTo(groupLocation)
            if (distance > currentSearchRadiusMeters) {
                return false
            }
        }

        return true
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
     * Attach chat listener for real-time messages with user display name
     */
    fun attachChatListener(groupId: String, currentUserDisplayName: String) {
        if (currentChatGroupId == groupId) return // Already listening

        detachChatListener(currentChatGroupId) // Clean up previous listener
        currentChatGroupId = groupId

        chatValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch(Dispatchers.Default) {
                    val messages = mutableListOf<ChatMessage>()
                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(ChatMessage::class.java)
                        message?.let { 
                            // Set sender name if it's null but we know the current user
                            if (it.senderName.isNullOrBlank() && it.senderId == currentFirebaseUserId) {
                                it.senderName = currentUserDisplayName
                            }
                            messages.add(it)
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        _chatMessages.value = messages.sortedBy { it.timestamp }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainViewModel", "Chat listener cancelled: ${error.message}")
            }
        }

        groupChatsRef.child(groupId).child("messages")
            .addValueEventListener(chatValueEventListener!!)
    }

    /**
     * Detach chat listener with specific group ID
     */
    fun detachChatListener(groupId: String? = null) {
        val targetGroupId = groupId ?: currentChatGroupId
        chatValueEventListener?.let { listener ->
            targetGroupId?.let { id ->
                groupChatsRef.child(id).child("messages")
                    .removeEventListener(listener)
            }
        }
        if (groupId == null || groupId == currentChatGroupId) {
            chatValueEventListener = null
            currentChatGroupId = null
            _chatMessages.value = emptyList()
        }
    }

    /**
     * Send a message to a group chat
     */
    fun sendMessage(groupId: String, messageText: String, senderId: String, senderName: String) {
        if (messageText.isBlank()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val messageId = groupChatsRef.child(groupId).child("messages").push().key
                if (messageId != null) {
                    val chatMessage = ChatMessage(
                        messageId = messageId,
                        senderId = senderId,
                        senderName = senderName,
                        text = messageText.trim(),
                        timestamp = System.currentTimeMillis()
                    )
                    
                    groupChatsRef.child(groupId).child("messages").child(messageId)
                        .setValue(chatMessage)
                        .addOnSuccessListener {
                            Log.d("MainViewModel", "Message sent successfully")
                        }
                        .addOnFailureListener { error ->
                            Log.e("MainViewModel", "Failed to send message: ${error.message}")
                        }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error sending message", e)
            }
        }
    }

    /**
     * Initialize Firebase references and user ID for the ViewModel
     */
    fun initializeFirebase(activeGroupsRef: DatabaseReference, groupChatsRef: DatabaseReference, firebaseUserId: String) {
        this.activeGroupsRef = activeGroupsRef
        this.groupChatsRef = groupChatsRef
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
        
        // Limit chat messages
        val currentMessages = _chatMessages.value
        if (currentMessages.size > MAX_CHAT_MESSAGES_SIZE) {
            val limitedMessages = currentMessages.takeLast(MAX_CHAT_MESSAGES_SIZE)
            _chatMessages.value = limitedMessages
        }
        
        // Limit recent searches
        if (recentSearchesCache.size > MAX_RECENT_SEARCHES_SIZE) {
            val limitedSearches = recentSearchesCache.takeLast(MAX_RECENT_SEARCHES_SIZE)
            recentSearchesCache.clear()
            recentSearchesCache.addAll(limitedSearches)
            _recentSearches.value = limitedSearches.toList()
        }
        
        Log.d("MainViewModel", "Memory cleanup completed. Cache sizes - Groups: ${groupsCache.size}, Messages: ${_chatMessages.value.size}, Searches: ${recentSearchesCache.size}")
    }

    override fun onCleared() {
        super.onCleared()
        detachChatListener()
        groupsCache.clear()
    }
}
