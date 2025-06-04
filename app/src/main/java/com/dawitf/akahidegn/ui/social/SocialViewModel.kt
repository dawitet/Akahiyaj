package com.dawitf.akahidegn.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawitf.akahidegn.analytics.AnalyticsService
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.features.profile.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val userProfileService: UserProfileService,
    private val analyticsService: AnalyticsService
) : ViewModel() {

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequest>> = _friendRequests.asStateFlow()

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    init {
        loadFriends()
        loadFriendRequests()
    }

    private fun loadFriends() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                userProfileService.getFriends().collect { friends ->
                    _friends.value = friends
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun loadFriendRequests() {
        // Stub implementation
        viewModelScope.launch {
            _friendRequests.value = emptyList()
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            // Stub implementation
            analyticsService.trackEvent("friend_removed", mapOf("friend_id" to friendId))
        }
    }

    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            // Stub implementation
            analyticsService.trackEvent("friend_request_accepted", mapOf("request_id" to requestId))
        }
    }

    fun declineFriendRequest(requestId: String) {
        viewModelScope.launch {
            // Stub implementation
            analyticsService.trackEvent("friend_request_declined", mapOf("request_id" to requestId))
        }
    }

    fun sendFriendRequest(userId: String) {
        viewModelScope.launch {
            // Stub implementation
            analyticsService.trackEvent("friend_request_sent", mapOf("user_id" to userId))
        }
    }

    fun findFriendByPhone(phoneNumber: String) {
        viewModelScope.launch {
            // Stub implementation
            analyticsService.trackEvent("friend_search_by_phone", emptyMap())
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

data class SocialUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
