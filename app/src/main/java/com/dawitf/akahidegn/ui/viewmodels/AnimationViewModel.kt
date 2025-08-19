package com.dawitf.akahidegn.ui.viewmodels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnimationViewModel @Inject constructor() : ViewModel() {

    fun showQuickSuccess(message: String) {
        // Trigger quick success animation/feedback
        viewModelScope.launch {
            // This would typically trigger some UI feedback
            // For now, we'll just log the success
            android.util.Log.d("AnimationViewModel", "Quick success: $message")
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
