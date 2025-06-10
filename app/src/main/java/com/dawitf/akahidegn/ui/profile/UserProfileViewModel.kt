package com.dawitf.akahidegn.ui.profile

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
class ProfileFeatureViewModel @Inject constructor(
    private val userProfileService: UserProfileService,
    private val analyticsService: AnalyticsService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()
    
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()
    
    val rideStats: StateFlow<RideStatistics?> = userProfileService.getRideStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val friends: StateFlow<List<Friend>> = userProfileService.getFriends()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Referral system removed for simplification
    // val referralStats: StateFlow<ReferralStats?> = userProfileService.getReferralStats()
    
    init {
        loadUserProfile()
        trackProfileView()
    }
    
    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            when (val result = userProfileService.getUserProfile()) {
                is Result.Success -> {
                    _userProfile.value = result.data
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error.message
                    )
                }
            }
        }
    }
    
    fun updateProfile(profileUpdate: UserProfileUpdate) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            when (val result = userProfileService.updateUserProfile(profileUpdate)) {
                is Result.Success -> {
                    loadUserProfile() // Reload profile after update
                    analyticsService.trackEvent("profile_updated", mapOf(
                        "fields_updated" to profileUpdate.toString()
                    ))
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error.message
                    )
                }
            }
        }
    }
    
    fun uploadProfilePhoto(photoUri: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            when (val result = userProfileService.uploadProfilePhoto(photoUri)) {
                is Result.Success -> {
                    loadUserProfile() // Reload to get updated photo URL
                    analyticsService.trackEvent("profile_photo_uploaded", emptyMap())
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error.message
                    )
                }
            }
        }
    }
    
    fun updateUserPreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            when (val result = userProfileService.updateUserPreferences(preferences)) {
                is Result.Success -> {
                    analyticsService.trackEvent("preferences_updated", mapOf(
                        "theme" to preferences.theme,
                        "language" to preferences.language
                    ))
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.error.message
                    )
                }
            }
        }
    }
    
    // Referral code generation removed for simplification
    /*
    fun generateReferralCode() {
        viewModelScope.launch {
            when (val result = userProfileService.generateReferralCode()) {
                is Result.Success -> {
                    analyticsService.trackEvent("referral_code_generated", emptyMap())
                    _uiState.value = _uiState.value.copy(
                        message = "Referral code generated: ${result.data}"
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.error.message
                    )
                }
            }
        }
    }
    */
    
    fun sendFriendRequest(userId: String) {
        viewModelScope.launch {
            when (val result = userProfileService.sendFriendRequest(userId)) {
                is Result.Success -> {
                    analyticsService.trackEvent("friend_request_sent", emptyMap())
                    _uiState.value = _uiState.value.copy(
                        message = "Friend request sent!"
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.error.message
                    )
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
    
    private fun trackProfileView() {
        analyticsService.trackEvent("profile_viewed", emptyMap())
    }
}

data class UserProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
