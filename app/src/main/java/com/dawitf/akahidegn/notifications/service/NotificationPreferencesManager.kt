package com.dawitf.akahidegn.notifications.service

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class NotificationPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"

        // Default values
        private const val DEFAULT_NOTIFICATIONS_ENABLED = true
        private const val DEFAULT_SOUND_ENABLED = true
        private const val DEFAULT_VIBRATION_ENABLED = true
    }
    
    fun areNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, DEFAULT_NOTIFICATIONS_ENABLED)
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
            .apply()
    }
    
    fun isSoundEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND_ENABLED)
    }
    
    fun setSoundEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_SOUND_ENABLED, enabled)
            .apply()
    }
    
    fun isVibrationEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_VIBRATION_ENABLED, DEFAULT_VIBRATION_ENABLED)
    }
    
    fun setVibrationEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_VIBRATION_ENABLED, enabled)
            .apply()
    }
}
