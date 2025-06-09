package com.dawitf.akahidegn.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.dawitf.akahidegn.data.local.dao.EnhancedGroupDao
import com.dawitf.akahidegn.data.local.dao.SearchDao
import com.dawitf.akahidegn.data.local.dao.UserPreferencesDao
import com.dawitf.akahidegn.data.local.entity.GroupEntity
import com.dawitf.akahidegn.data.local.entity.GroupEntityEnhanced
import com.dawitf.akahidegn.data.local.entity.RecentSearchEntity
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.data.mapper.toEnhancedEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val groupDao: EnhancedGroupDao,
    private val searchDao: SearchDao,
    private val userPreferencesDao: UserPreferencesDao
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val offlineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _isOnline = MutableStateFlow(isNetworkAvailable())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    
    private val _pendingSyncActions = MutableStateFlow<List<SyncAction>>(emptyList())
    val pendingSyncActions: StateFlow<List<SyncAction>> = _pendingSyncActions.asStateFlow()
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
            syncPendingActions()
        }
        
        override fun onLost(network: Network) {
            _isOnline.value = false
        }
        
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isOnline.value = hasInternet
            if (hasInternet) {
                syncPendingActions()
            }
        }
    }
    
    init {
        registerNetworkCallback()
        // Launch coroutine for suspend function
        offlineScope.launch {
            loadPendingSyncActions()
        }
    }
    
    fun initialize() {
        // Initialize offline capabilities - mostly already done in init block
        // This method provides external interface for initialization tracking
        offlineScope.launch {
            try {
                // Ensure database is ready and sync any pending actions
                syncPendingActions()
            } catch (e: Exception) {
                // Log initialization error but don't crash
                e.printStackTrace()
            }
        }
    }
    
    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }
    
    private fun isNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    // Caching methods for offline access
    suspend fun cacheGroupsForOfflineAccess(groups: List<Group>) {
        val enhancedGroups = groups.map { group ->
            group.toEnhancedEntity()
        }
        groupDao.insertGroups(enhancedGroups)
    }
    
    suspend fun getCachedGroups(): List<GroupEntityEnhanced> {
        return groupDao.getNearbyGroups(0.0, 0.0, Double.MAX_VALUE, 100)
    }
    
    suspend fun getCachedGroupsByDestination(destination: String): List<GroupEntityEnhanced> {
        return groupDao.searchGroups(destination, 50)
    }
    
    suspend fun cacheSearchHistory(query: String, destination: String) {
        val searchEntity = RecentSearchEntity(
            id = java.util.UUID.randomUUID().toString(),
            query = query,
            destination = query, // Using query as destination for now
            timestamp = System.currentTimeMillis(),
            usageCount = 1
        )
        searchDao.insertRecentSearch(searchEntity)
    }
    
    suspend fun getCachedSearchHistory(): List<String> {
        return searchDao.getRecentSearches(10).map { it.query }
    }
    
    // Sync action management
    suspend fun addSyncAction(action: SyncAction) {
        val currentActions = _pendingSyncActions.value.toMutableList()
        currentActions.add(action)
        _pendingSyncActions.value = currentActions
        savePendingSyncActions(currentActions)
    }
    
    private fun syncPendingActions() {
        if (!_isOnline.value) return
        
        offlineScope.launch {
            val actions = _pendingSyncActions.value
            if (actions.isEmpty()) return@launch
            
            val processedActions = mutableListOf<SyncAction>()
            
            actions.forEach { action ->
                try {
                    when (action) {
                        is SyncAction.CreateGroup -> {
                            // Process group creation when back online
                            processGroupCreation(action)
                            processedActions.add(action)
                        }
                        is SyncAction.JoinGroup -> {
                            // Process group joining when back online
                            processGroupJoining(action)
                            processedActions.add(action)
                        }
                        is SyncAction.LeaveGroup -> {
                            // Process group leaving when back online
                            processGroupLeaving(action)
                            processedActions.add(action)
                        }
                        is SyncAction.UpdateProfile -> {
                            // Process profile update when back online
                            processProfileUpdate(action)
                            processedActions.add(action)
                        }
                        is SyncAction.SubmitReview -> {
                            // Process review submission when back online
                            processReviewSubmission(action)
                            processedActions.add(action)
                        }
                    }
                } catch (e: Exception) {
                    // Log error but continue with other actions
                    // Keep failed actions for retry
                }
            }
            
            // Remove successfully processed actions
            val remainingActions = actions - processedActions.toSet()
            _pendingSyncActions.value = remainingActions
            savePendingSyncActions(remainingActions)
        }
    }
    
    private suspend fun processGroupCreation(action: SyncAction.CreateGroup) {
        // Implement actual group creation logic here
        // This would typically involve calling the repository
    }
    
    private suspend fun processGroupJoining(action: SyncAction.JoinGroup) {
        // Implement actual group joining logic here
    }
    
    private suspend fun processGroupLeaving(action: SyncAction.LeaveGroup) {
        // Implement actual group leaving logic here
    }
    
    private suspend fun processProfileUpdate(action: SyncAction.UpdateProfile) {
        // Implement actual profile update logic here
    }
    
    private suspend fun processReviewSubmission(action: SyncAction.SubmitReview) {
        // Implement actual review submission logic here
    }
    
    // Offline data management
    suspend fun getOfflineContent(): OfflineContent {
        return OfflineContent(
            cachedGroups = getCachedGroups().map { it.toGroupEntity() },
            recentSearches = getCachedSearchHistory(),
            userPreferences = userPreferencesDao.getAllUserPreferences(),
            lastSyncTime = System.currentTimeMillis()
        )
    }
    
    suspend fun clearOfflineCache() {
        groupDao.deleteAllGroups()
        searchDao.clearRecentSearches()
    }
    
    suspend fun getOfflineDataSize(): Long {
        // Calculate approximate size of offline data
        val groupCount = groupDao.getGroupCount()
        val searchCount = searchDao.getRecentSearchCount().toLong()
        val preferencesCount = userPreferencesDao.getUserPreferencesCount()
        
        // Rough estimation: each group ~2KB, each search ~100B, each preference ~500B
        return (groupCount * 2048L) + (searchCount * 100L) + (preferencesCount * 500L)
    }
    
    // Persistence for sync actions
    private suspend fun savePendingSyncActions(actions: List<SyncAction>) {
        // Save to local storage (could use SharedPreferences or Room)
        // For now, we'll keep them in memory
    }
    
    private suspend fun loadPendingSyncActions() {
        // Load from local storage
        // For now, start with empty list
        _pendingSyncActions.value = emptyList()
    }
    
    fun cleanup() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
        offlineScope.cancel()
    }
}

sealed class SyncAction {
    data class CreateGroup(
        val destination: String,
        val maxMembers: Int,
        val departureTime: Long,
        val pricePerSeat: Double,
        val description: String?,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncAction()
    
    data class JoinGroup(
        val groupId: String,
        val userId: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncAction()
    
    data class LeaveGroup(
        val groupId: String,
        val userId: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncAction()
    
    data class UpdateProfile(
        val userId: String,
        val updates: Map<String, Any>,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncAction()
    
    data class SubmitReview(
        val revieweeId: String,
        val rating: Double,
        val comment: String,
        val tripId: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncAction()
}

data class OfflineContent(
    val cachedGroups: List<GroupEntity>,
    val recentSearches: List<String>,
    val userPreferences: List<com.dawitf.akahidegn.data.local.entity.UserPreferencesEntity>,
    val lastSyncTime: Long
)

// Extension function to convert GroupEntityEnhanced to GroupEntity
private fun GroupEntityEnhanced.toGroupEntity(): GroupEntity {
    return GroupEntity(
        id = this.groupId,
        creatorUserId = this.creatorId ?: "",
        name = "",
        description = this.description ?: "",
        destination = this.destinationName ?: "",
        date = "",
        time = "",
        latitude = this.pickupLat ?: 0.0,
        longitude = this.pickupLng ?: 0.0,
        memberCount = this.memberCount,
        maxMembers = this.maxMembers,
        isActive = this.isActive,
        createdAt = this.timestamp ?: System.currentTimeMillis(),
        updatedAt = this.lastUpdated
    )
}
