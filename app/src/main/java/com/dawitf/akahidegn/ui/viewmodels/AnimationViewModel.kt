package com.dawitf.akahidegn.ui.viewmodels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Simple notification data class
 */
data class AnimationNotification(
    val id: String = "",
    val message: String = "",
    val type: String = "info", // success, error, warning, info
    val duration: Long = 3000L
)

@HiltViewModel
class AnimationViewModel @Inject constructor() : ViewModel() {

    private val _notifications = MutableStateFlow<List<AnimationNotification>>(emptyList())
    val notifications: StateFlow<List<AnimationNotification>> = _notifications.asStateFlow()

    fun showQuickSuccess(message: String) {
        addNotification(AnimationNotification(
            id = System.currentTimeMillis().toString(),
            message = message,
            type = "success"
        ))
    }

    fun showSuccess(message: String) {
        showQuickSuccess(message)
    }

    fun showError(message: String) {
        addNotification(AnimationNotification(
            id = System.currentTimeMillis().toString(),
            message = message,
            type = "error"
        ))
    }

    fun showLoading(message: String = "Loading...") {
        addNotification(AnimationNotification(
            id = "loading",
            message = message,
            type = "loading",
            duration = Long.MAX_VALUE // Keep until explicitly removed
        ))
    }

    fun hideLoading() {
        _notifications.value = _notifications.value.filter { it.id != "loading" }
    }

    fun showFormSubmissionSuccess(message: String) {
        showSuccess("Form submitted: $message")
    }

    fun showFormSubmissionError(message: String) {
        showError("Form error: $message")
    }

    fun showNetworkConnected() {
        showSuccess("Network connected")
    }

    fun showNetworkDisconnected() {
        showError("Network disconnected")
    }

    private fun addNotification(notification: AnimationNotification) {
        _notifications.value = _notifications.value + notification
        
        // Auto-remove after duration (except for loading)
        if (notification.duration != Long.MAX_VALUE) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(notification.duration)
                _notifications.value = _notifications.value.filter { it.id != notification.id }
            }
        }
    }

    fun runGroupJoinSequence(groupName: String) {
        viewModelScope.launch {
            // Play group join animation sequence
            android.util.Log.d("AnimationViewModel", "Group join sequence for: $groupName")
        }
    }
}

// Extension function for showQuickSuccess
fun AnimationViewModel.showQuickSuccess(message: String) {
    showQuickSuccess(message)
}
