package com.dawitf.akahidegn.viewmodel

import androidx.lifecycle.ViewModel
import com.dawitf.akahidegn.notifications.service.NotificationPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val notificationPreferencesManager: NotificationPreferencesManager
) : ViewModel() {

    private val _notificationsEnabled = MutableStateFlow(notificationPreferencesManager.areNotificationsEnabled())
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _soundEnabled = MutableStateFlow(notificationPreferencesManager.isSoundEnabled())
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(notificationPreferencesManager.isVibrationEnabled())
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

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
}
