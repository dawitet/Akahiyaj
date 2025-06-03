package com.dawitf.akahidegn.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawitf.akahidegn.analytics.AnalyticsService
import com.dawitf.akahidegn.features.social.RideBuddyService
import com.dawitf.akahidegn.features.social.RideBuddyService.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RideBuddyViewModel @Inject constructor(
    private val rideBuddyService: RideBuddyService,
    private val analyticsService: AnalyticsService
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _buddyStats = MutableStateFlow(BuddyStats(0, 0, 0f, null, 0, 0))
    val buddyStats: StateFlow<BuddyStats> = _buddyStats.asStateFlow()
    
    private val _buddySuggestions = MutableStateFlow<List<RideBuddySuggestion>>(emptyList())
    val buddySuggestions: StateFlow<List<RideBuddySuggestion>> = _buddySuggestions.asStateFlow()
    
    // Flow-based data from service
    val rideBuddies: StateFlow<List<RideBuddy>> = rideBuddyService.getRideBuddies()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val pendingInvitations: StateFlow<List<RideBuddyInvitation>> = rideBuddyService.getPendingInvitations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val sentInvitations: StateFlow<List<RideBuddyInvitation>> = rideBuddyService.getSentInvitations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val regularGroups: StateFlow<List<RegularGroup>> = rideBuddyService.getRegularGroups()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    init {
        loadData()
    }
    
    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Load stats and suggestions
                loadBuddyStats()
                loadBuddySuggestions()
                
                analyticsService.trackEvent("buddy_screen_viewed", emptyMap())
                
            } catch (e: Exception) {
                _error.value = "Failed to load buddy data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun loadBuddyStats() {
        try {
            val stats = rideBuddyService.getBuddyStats()
            _buddyStats.value = stats
        } catch (e: Exception) {
            // Log error but don't fail the entire load
            _error.value = "Failed to load buddy stats"
        }
    }
    
    private suspend fun loadBuddySuggestions() {
        try {
            val suggestions = rideBuddyService.getBuddySuggestions()
            _buddySuggestions.value = suggestions
        } catch (e: Exception) {
            // Log error but don't fail the entire load
        }
    }
    
    fun sendInvitation(toUserId: String, toUserName: String, message: String, groupId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            
            rideBuddyService.sendBuddyInvitation(toUserId, toUserName, message, groupId)
                .onSuccess {
                    analyticsService.trackEvent("buddy_invitation_sent_from_ui", mapOf(
                        "invitation_id" to it,
                        "has_message" to message.isNotBlank(),
                        "from_group" to (groupId != null)
                    ))
                }
                .onFailure {
                    _error.value = "Failed to send invitation: ${it.message}"
                }
            
            _isLoading.value = false
        }
    }
    
    fun respondToInvitation(invitationId: String, accept: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            
            rideBuddyService.respondToBuddyInvitation(invitationId, accept)
                .onSuccess {
                    analyticsService.trackEvent("buddy_invitation_responded_from_ui", mapOf(
                        "invitation_id" to invitationId,
                        "accepted" to accept
                    ))
                    // Refresh suggestions if a new buddy was added
                    if (accept) {
                        loadBuddySuggestions()
                        loadBuddyStats()
                    }
                }
                .onFailure {
                    _error.value = "Failed to respond to invitation: ${it.message}"
                }
            
            _isLoading.value = false
        }
    }
    
    fun removeBuddy(buddyUserId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            rideBuddyService.removeBuddy(buddyUserId)
                .onSuccess {
                    analyticsService.trackEvent("buddy_removed_from_ui", mapOf(
                        "buddy_user_id" to buddyUserId
                    ))
                    // Refresh stats after removal
                    loadBuddyStats()
                }
                .onFailure {
                    _error.value = "Failed to remove buddy: ${it.message}"
                }
            
            _isLoading.value = false
        }
    }
    
    fun rateBuddy(buddyUserId: String, rating: Float) {
        viewModelScope.launch {
            rideBuddyService.updateBuddyRating(buddyUserId, rating)
                .onSuccess {
                    analyticsService.trackEvent("buddy_rated_from_ui", mapOf(
                        "buddy_user_id" to buddyUserId,
                        "rating" to rating
                    ))
                    // Refresh stats after rating update
                    loadBuddyStats()
                }
                .onFailure {
                    _error.value = "Failed to rate buddy: ${it.message}"
                }
        }
    }
    
    fun createRegularGroup(
        groupName: String,
        memberIds: List<String>,
        commonRoute: String,
        preferredTimes: List<String>,
        description: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            rideBuddyService.createRegularGroup(groupName, memberIds, commonRoute, preferredTimes, description)
                .onSuccess { groupId ->
                    analyticsService.trackEvent("regular_group_created_from_ui", mapOf(
                        "group_id" to groupId,
                        "member_count" to memberIds.size,
                        "has_description" to description.isNotBlank(),
                        "has_preferred_times" to preferredTimes.isNotEmpty()
                    ))
                    // Refresh stats
                    loadBuddyStats()
                }
                .onFailure {
                    _error.value = "Failed to create regular group: ${it.message}"
                }
            
            _isLoading.value = false
        }
    }
    
    fun joinRegularGroup(groupId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            rideBuddyService.joinRegularGroup(groupId)
                .onSuccess {
                    analyticsService.trackEvent("regular_group_joined_from_ui", mapOf(
                        "group_id" to groupId
                    ))
                    loadBuddyStats()
                }
                .onFailure {
                    _error.value = "Failed to join regular group: ${it.message}"
                }
            
            _isLoading.value = false
        }
    }
    
    fun leaveRegularGroup(groupId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            rideBuddyService.leaveRegularGroup(groupId)
                .onSuccess {
                    analyticsService.trackEvent("regular_group_left_from_ui", mapOf(
                        "group_id" to groupId
                    ))
                    loadBuddyStats()
                }
                .onFailure {
                    _error.value = "Failed to leave regular group: ${it.message}"
                }
            
            _isLoading.value = false
        }
    }
    
    fun recordRideWithUser(otherUserId: String, otherUserName: String, groupId: String, rating: Float? = null) {
        viewModelScope.launch {
            rideBuddyService.recordRideWithUser(otherUserId, otherUserName, groupId, rating)
                .onSuccess {
                    // Refresh suggestions and stats after recording a ride
                    loadBuddySuggestions()
                    loadBuddyStats()
                }
        }
    }
    
    fun updateNotificationPreferences(
        enableInvitations: Boolean,
        enableGroupUpdates: Boolean,
        enableRideReminders: Boolean
    ) {
        viewModelScope.launch {
            rideBuddyService.updateNotificationPreferences(
                enableInvitations,
                enableGroupUpdates,
                enableRideReminders
            ).onSuccess {
                analyticsService.trackEvent("buddy_notification_preferences_updated", mapOf(
                    "enable_invitations" to enableInvitations,
                    "enable_group_updates" to enableGroupUpdates,
                    "enable_ride_reminders" to enableRideReminders
                ))
            }
        }
    }
    
    fun searchPotentialBuddies(query: String) {
        viewModelScope.launch {
            try {
                val results = rideBuddyService.searchPotentialBuddies(query)
                // Handle search results - could be a separate state flow if needed
                analyticsService.trackEvent("buddy_search_performed", mapOf(
                    "query" to query,
                    "results_count" to results.size
                ))
            } catch (e: Exception) {
                _error.value = "Search failed: ${e.message}"
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    // Helper function to get frequent ride partners for quick actions
    fun getFrequentRidePartners(limit: Int = 5) {
        viewModelScope.launch {
            try {
                val frequentPartners = rideBuddyService.getFrequentRidePartners(limit)
                // Could emit to a separate state flow if needed for UI shortcuts
            } catch (e: Exception) {
                // Handle error silently as this is supplementary data
            }
        }
    }
    
    // Function to suggest buddy invitations after a ride
    fun suggestBuddyInvitationAfterRide(otherUserId: String, otherUserName: String, groupId: String) {
        viewModelScope.launch {
            // Check if they should be suggested as a buddy
            val suggestions = rideBuddyService.getBuddySuggestions()
            val shouldSuggest = suggestions.any { it.userId == otherUserId && it.confidence > 0.6f }
            
            if (shouldSuggest) {
                analyticsService.trackEvent("buddy_invitation_suggested_after_ride", mapOf(
                    "other_user_id" to otherUserId,
                    "group_id" to groupId
                ))
                // Could trigger a UI suggestion dialog
            }
        }
    }
    
    // Analytics helper for tracking buddy feature usage
    fun trackBuddyFeatureUsage(feature: String, details: Map<String, Any> = emptyMap()) {
        viewModelScope.launch {
            analyticsService.trackEvent("buddy_feature_used", details + mapOf("feature" to feature))
        }
    }
}
