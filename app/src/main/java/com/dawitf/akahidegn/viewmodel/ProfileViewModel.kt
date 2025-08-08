package com.dawitf.akahidegn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.domain.model.TripHistoryItem
import com.dawitf.akahidegn.domain.model.UserProfile
import com.dawitf.akahidegn.domain.model.UserReview
import com.dawitf.akahidegn.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun load(userId: String) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val profileResult = userProfileRepository.getUserProfile(userId)
            val tripsResult = userProfileRepository.getUserTripHistory(userId)
            val reviewsResult = userProfileRepository.getUserReviews(userId)
            when {
                profileResult is Result.Success &&
                        tripsResult is Result.Success &&
                        reviewsResult is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profile = profileResult.data,
                        tripHistory = tripsResult.data.take(10),
                        recentReviews = reviewsResult.data.take(3)
                    )
                }
                else -> {
                    val firstError = listOf(profileResult, tripsResult, reviewsResult)
                        .filterIsInstance<Result.Error>()
                        .firstOrNull()
                    val errMsg = firstError?.error?.message ?: "Failed to load profile"
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errMsg)
                }
            }
        }
    }

    fun refresh() {
        _uiState.value.profile?.let { load(it.userId) }
    }
}

data class ProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val profile: UserProfile? = null,
    val tripHistory: List<TripHistoryItem> = emptyList(),
    val recentReviews: List<UserReview> = emptyList()
)
