package com.dawitf.akahidegn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawitf.akahidegn.notifications.service.NotificationPreferencesManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val notificationPreferencesManager: NotificationPreferencesManager,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _notificationsEnabled = MutableStateFlow(notificationPreferencesManager.areNotificationsEnabled())
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _soundEnabled = MutableStateFlow(notificationPreferencesManager.isSoundEnabled())
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(notificationPreferencesManager.isVibrationEnabled())
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _suggestionSubmitting = MutableStateFlow(false)
    val suggestionSubmitting: StateFlow<Boolean> = _suggestionSubmitting.asStateFlow()

    private val _suggestionSubmitted = MutableStateFlow(false)
    val suggestionSubmitted: StateFlow<Boolean> = _suggestionSubmitted.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        notificationPreferencesManager.setNotificationsEnabled(enabled)
    }

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        notificationPreferencesManager.setSoundEnabled(enabled)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _vibrationEnabled.value = enabled
        notificationPreferencesManager.setVibrationEnabled(enabled)
    }

    fun submitSuggestion(text: String) {
        if (text.isBlank() || _suggestionSubmitting.value) return
        val uid = auth.currentUser?.uid ?: "anonymous"
        val data = hashMapOf(
            "userId" to uid,
            "text" to text.trim(),
            "createdAt" to System.currentTimeMillis()
        )
        _suggestionSubmitting.value = true
        _suggestionSubmitted.value = false
        viewModelScope.launch {
            try {
                firestore.collection("suggestions").add(data)
                _suggestionSubmitted.value = true
            } catch (_: Exception) {
                _suggestionSubmitted.value = false
            } finally {
                _suggestionSubmitting.value = false
            }
        }
    }
}
