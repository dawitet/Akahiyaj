package com.dawitf.akahidegn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawitf.akahidegn.domain.repository.UserProfileRepository
import com.dawitf.akahidegn.domain.model.UserProfile
import com.dawitf.akahidegn.features.profile.RideStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _rideStats = MutableStateFlow<RideStatistics?>(null)
    val rideStats: StateFlow<RideStatistics?> = _rideStats.asStateFlow()

    init {
        loadUserProfile()
        loadRideStats()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Note: This needs to be updated to get current user ID
                // For now, using a placeholder. In real implementation, 
                // get the current user ID from authentication service
                val currentUserId = "placeholder_user_id"
                userProfileRepository.observeUserProfile(currentUserId).collect { profile ->
                    _userProfile.value = profile
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                userProfileRepository.updateUserProfile(profile)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadRideStats() {
        viewModelScope.launch {
            try {
                // Note: getRideStats method is not available in simplified repository
                // This would need to be implemented or called from the service layer
                // For now, we'll use a default empty stats
                _rideStats.value = RideStatistics(
                    totalRides = 0,
                    totalDistance = 0.0,
                    totalSpent = 0.0,
                    averageRating = 0.0f,
                    totalTimeSaved = 0L,
                    carbonSaved = 0.0
                )
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
