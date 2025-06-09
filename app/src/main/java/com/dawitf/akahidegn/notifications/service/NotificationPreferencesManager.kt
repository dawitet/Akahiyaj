package com.dawitf.akahidegn.notifications.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.dawitf.akahidegn.notifications.model.NotificationPreferences
import com.dawitf.akahidegn.notifications.model.QuietHours
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notificationPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_preferences"
)

@Singleton
class NotificationPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val dataStore = context.notificationPreferencesDataStore
    
    companion object {
        val GROUP_JOIN_REQUESTS = booleanPreferencesKey("group_join_requests")
        val GROUP_ACTIVITY = booleanPreferencesKey("group_activity")
        val CHAT_MESSAGES = booleanPreferencesKey("chat_messages")
        val TRIP_REMINDERS = booleanPreferencesKey("trip_reminders")
        val TRIP_ALERTS = booleanPreferencesKey("trip_alerts")
        val SYSTEM_ANNOUNCEMENTS = booleanPreferencesKey("system_announcements")
        val PROMOTIONS = booleanPreferencesKey("promotions")
        val SOUND = booleanPreferencesKey("sound")
        val VIBRATION = booleanPreferencesKey("vibration")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_HOURS_START_HOUR = intPreferencesKey("quiet_hours_start_hour")
        val QUIET_HOURS_START_MINUTE = intPreferencesKey("quiet_hours_start_minute")
        val QUIET_HOURS_END_HOUR = intPreferencesKey("quiet_hours_end_hour")
        val QUIET_HOURS_END_MINUTE = intPreferencesKey("quiet_hours_end_minute")
    }
    
    val notificationPreferencesFlow: Flow<NotificationPreferences> = dataStore.data.map { preferences ->
        NotificationPreferences(
            groupJoinRequests = preferences[GROUP_JOIN_REQUESTS] ?: true,
            groupActivity = preferences[GROUP_ACTIVITY] ?: true,
            chatMessages = preferences[CHAT_MESSAGES] ?: true,
            tripReminders = preferences[TRIP_REMINDERS] ?: true,
            tripAlerts = preferences[TRIP_ALERTS] ?: true,
            systemAnnouncements = preferences[SYSTEM_ANNOUNCEMENTS] ?: true,
            promotions = preferences[PROMOTIONS] ?: false,
            sound = preferences[SOUND] ?: true,
            vibration = preferences[VIBRATION] ?: true,
            quietHours = QuietHours(
                enabled = preferences[QUIET_HOURS_ENABLED] ?: false,
                startHour = preferences[QUIET_HOURS_START_HOUR] ?: 22,
                startMinute = preferences[QUIET_HOURS_START_MINUTE] ?: 0,
                endHour = preferences[QUIET_HOURS_END_HOUR] ?: 7,
                endMinute = preferences[QUIET_HOURS_END_MINUTE] ?: 0
            )
        )
    }
    
    suspend fun getNotificationPreferences(): NotificationPreferences {
        return notificationPreferencesFlow.first()
    }
    
    suspend fun updateNotificationPreferences(preferences: NotificationPreferences) {
        dataStore.edit { settings ->
            settings[GROUP_JOIN_REQUESTS] = preferences.groupJoinRequests
            settings[GROUP_ACTIVITY] = preferences.groupActivity
            settings[CHAT_MESSAGES] = preferences.chatMessages
            settings[TRIP_REMINDERS] = preferences.tripReminders
            settings[TRIP_ALERTS] = preferences.tripAlerts
            settings[SYSTEM_ANNOUNCEMENTS] = preferences.systemAnnouncements
            settings[PROMOTIONS] = preferences.promotions
            settings[SOUND] = preferences.sound
            settings[VIBRATION] = preferences.vibration
            
            preferences.quietHours?.let { quietHours ->
                settings[QUIET_HOURS_ENABLED] = quietHours.enabled
                settings[QUIET_HOURS_START_HOUR] = quietHours.startHour
                settings[QUIET_HOURS_START_MINUTE] = quietHours.startMinute
                settings[QUIET_HOURS_END_HOUR] = quietHours.endHour
                settings[QUIET_HOURS_END_MINUTE] = quietHours.endMinute
            }
        }
    }
    
    suspend fun updateGroupJoinRequests(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[GROUP_JOIN_REQUESTS] = enabled
        }
    }
    
    suspend fun updateGroupActivity(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[GROUP_ACTIVITY] = enabled
        }
    }
    
    suspend fun updateChatMessages(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[CHAT_MESSAGES] = enabled
        }
    }
    
    suspend fun updateTripReminders(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[TRIP_REMINDERS] = enabled
        }
    }
    
    suspend fun updateTripAlerts(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[TRIP_ALERTS] = enabled
        }
    }
    
    suspend fun updateSystemAnnouncements(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[SYSTEM_ANNOUNCEMENTS] = enabled
        }
    }
    
    suspend fun updatePromotions(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[PROMOTIONS] = enabled
        }
    }
    
    suspend fun updateSound(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[SOUND] = enabled
        }
    }
    
    suspend fun updateVibration(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[VIBRATION] = enabled
        }
    }
    
    suspend fun updateQuietHours(quietHours: QuietHours) {
        dataStore.edit { settings ->
            settings[QUIET_HOURS_ENABLED] = quietHours.enabled
            settings[QUIET_HOURS_START_HOUR] = quietHours.startHour
            settings[QUIET_HOURS_START_MINUTE] = quietHours.startMinute
            settings[QUIET_HOURS_END_HOUR] = quietHours.endHour
            settings[QUIET_HOURS_END_MINUTE] = quietHours.endMinute
        }
    }
    
    // Additional wrapper methods for SettingsViewModel compatibility
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        // Enable/disable all notifications
        updateGroupJoinRequests(enabled)
        updateGroupActivity(enabled)
        updateChatMessages(enabled)
        updateTripReminders(enabled)
        updateTripAlerts(enabled)
        updateSystemAnnouncements(enabled)
    }
    
    suspend fun setChatNotificationsEnabled(enabled: Boolean) {
        updateChatMessages(enabled)
    }
    
    suspend fun setTripNotificationsEnabled(enabled: Boolean) {
        updateTripReminders(enabled)
        updateTripAlerts(enabled)
    }
    
    suspend fun setSystemNotificationsEnabled(enabled: Boolean) {
        updateSystemAnnouncements(enabled)
    }
    
    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[QUIET_HOURS_ENABLED] = enabled
        }
    }
    
    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        // This could be handled by a separate analytics preferences manager
        // For now, just update promotions as a placeholder
        updatePromotions(enabled)
    }
    
    suspend fun setLocationDataEnabled(enabled: Boolean) {
        // This could be handled by a separate location preferences manager
        // For now, just a placeholder implementation
        dataStore.edit { settings ->
            settings[booleanPreferencesKey("location_data_enabled")] = enabled
        }
    }
}
