package com.dawitf.akahidegn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawitf.akahidegn.notifications.service.NotificationPreferencesManager
import com.dawitf.akahidegn.ui.theme.*
import com.dawitf.akahidegn.domain.model.LanguageOption
import com.dawitf.akahidegn.domain.model.FontSizeOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing app settings including theme, notifications, and accessibility preferences.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeManager: ThemeManager,
    private val notificationPreferences: NotificationPreferencesManager
) : ViewModel() {

    // Combine all settings into a single UI state
    val uiState: StateFlow<SettingsUiState> = combine(
        themeManager.themeMode,
        themeManager.language,
        themeManager.useDynamicColors,
        themeManager.useHighContrast,
        themeManager.fontSize,
        themeManager.notificationsEnabled,
        themeManager.chatNotificationsEnabled,
        themeManager.tripNotificationsEnabled,
        themeManager.systemNotificationsEnabled,
        themeManager.quietHoursEnabled,
        themeManager.analyticsEnabled,
        themeManager.locationDataEnabled
    ) { values ->
        SettingsUiState(
            themeMode = values[0] as ThemeMode,
            language = values[1] as LanguageOption,
            useDynamicColors = values[2] as Boolean,
            useHighContrast = values[3] as Boolean,
            fontSize = values[4] as FontSizeOption,
            notificationsEnabled = values[5] as Boolean,
            chatNotificationsEnabled = values[6] as Boolean,
            tripNotificationsEnabled = values[7] as Boolean,
            systemNotificationsEnabled = values[8] as Boolean,
            quietHoursEnabled = values[9] as Boolean,
            analyticsEnabled = values[10] as Boolean,
            locationDataEnabled = values[11] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    // Theme Settings
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeManager.setThemeMode(mode)
        }
    }

    fun setLanguage(language: LanguageOption) {
        viewModelScope.launch {
            themeManager.setLanguage(language)
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            themeManager.setUseDynamicColors(enabled)
        }
    }

    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch {
            themeManager.setUseHighContrast(enabled)
        }
    }

    fun setFontSize(size: FontSizeOption) {
        viewModelScope.launch {
            themeManager.setFontSize(size)
        }
    }

    // Notification Settings
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setNotificationsEnabled(enabled)
        }
    }

    fun setChatNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setChatNotificationsEnabled(enabled)
        }
    }

    fun setTripNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setTripNotificationsEnabled(enabled)
        }
    }

    fun setSystemNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setSystemNotificationsEnabled(enabled)
        }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setQuietHoursEnabled(enabled)
        }
    }

    // Privacy Settings
    fun setAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setAnalyticsEnabled(enabled)
        }
    }

    fun setLocationDataEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setLocationDataEnabled(enabled)
        }
    }

    // Reset all settings to defaults
    fun resetToDefaults() {
        viewModelScope.launch {
            themeManager.setThemeMode(ThemeMode.SYSTEM)
            themeManager.setLanguage(LanguageOption.SYSTEM)
            themeManager.setUseDynamicColors(true)
            themeManager.setUseHighContrast(false)
            themeManager.setFontSize(FontSizeOption.MEDIUM)
            
            notificationPreferences.setNotificationsEnabled(true)
            notificationPreferences.setChatNotificationsEnabled(true)
            notificationPreferences.setTripNotificationsEnabled(true)
            notificationPreferences.setSystemNotificationsEnabled(true)
            notificationPreferences.setQuietHoursEnabled(false)
            notificationPreferences.setAnalyticsEnabled(true)
            notificationPreferences.setLocationDataEnabled(true)
        }
    }
}

/**
 * UI state for the settings screen.
 */
data class SettingsUiState(
    // Theme settings
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: LanguageOption = LanguageOption.SYSTEM,
    val useDynamicColors: Boolean = true,
    val useHighContrast: Boolean = false,
    val fontSize: FontSizeOption =            FontSizeOption.MEDIUM,
    
    // Notification settings
    val notificationsEnabled: Boolean = true,
    val chatNotificationsEnabled: Boolean = true,
    val tripNotificationsEnabled: Boolean = true,
    val systemNotificationsEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    
    // Privacy settings
    val analyticsEnabled: Boolean = true,
    val locationDataEnabled: Boolean = true,
    
    // Loading state
    val isLoading: Boolean = false,
    val error: String? = null
)
