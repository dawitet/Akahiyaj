package com.dawitf.akahidegn.data.local.dao

import androidx.room.*
import com.dawitf.akahidegn.data.local.entity.UserPreferencesEntity
import com.dawitf.akahidegn.data.local.entity.UserAnalyticsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for user preferences and settings.
 */
@Dao
interface UserPreferencesDao {
    
    @Query("SELECT * FROM user_preferences WHERE key = :key")
    suspend fun getPreference(key: String): UserPreferencesEntity?
    
    @Query("SELECT * FROM user_preferences")
    fun getAllPreferences(): Flow<List<UserPreferencesEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(preference: UserPreferencesEntity)
    
    @Query("DELETE FROM user_preferences WHERE key = :key")
    suspend fun deletePreference(key: String)
    
    @Query("DELETE FROM user_preferences")
    suspend fun clearAllPreferences()
    
    // Convenience methods for common preference types
    suspend fun setStringPreference(key: String, value: String) {
        setPreference(UserPreferencesEntity(key, value, "string"))
    }
    
    suspend fun setBooleanPreference(key: String, value: Boolean) {
        setPreference(UserPreferencesEntity(key, value.toString(), "boolean"))
    }
    
    suspend fun setIntPreference(key: String, value: Int) {
        setPreference(UserPreferencesEntity(key, value.toString(), "int"))
    }
    
    suspend fun setFloatPreference(key: String, value: Float) {
        setPreference(UserPreferencesEntity(key, value.toString(), "float"))
    }
    
    suspend fun setLongPreference(key: String, value: Long) {
        setPreference(UserPreferencesEntity(key, value.toString(), "long"))
    }
    
    // Notification settings convenience methods
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        setBooleanPreference("notifications_enabled", enabled)
    }
    
    suspend fun setChatNotificationsEnabled(enabled: Boolean) {
        setBooleanPreference("chat_notifications_enabled", enabled)
    }
    
    suspend fun setTripNotificationsEnabled(enabled: Boolean) {
        setBooleanPreference("trip_notifications_enabled", enabled)
    }
    
    suspend fun setSystemNotificationsEnabled(enabled: Boolean) {
        setBooleanPreference("system_notifications_enabled", enabled)
    }
    
    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        setBooleanPreference("quiet_hours_enabled", enabled)
    }
    
    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        setBooleanPreference("analytics_enabled", enabled)
    }
    
    suspend fun setLocationDataEnabled(enabled: Boolean) {
        setBooleanPreference("location_data_enabled", enabled)
    }
    
    // Additional methods needed by other components
    suspend fun getAllUserPreferences(): List<UserPreferencesEntity> {
        // This is a suspend function version of getAllPreferences for one-time reads
        val preferences = getAllPreferences()
        // For now, return empty list as this would need a different implementation
        return emptyList()
    }
    
    suspend fun getRecentSearchCount(): Int {
        // This method should be in SearchDao, returning 0 for now
        return 0
    }
    
    suspend fun getUserPreferencesCount(): Int {
        return getAllUserPreferences().size
    }
}

/**
 * DAO for user analytics and usage tracking.
 */
@Dao
interface UserAnalyticsDao {
    
    @Insert
    suspend fun insertAnalyticsEvent(event: UserAnalyticsEntity)
    
    @Query("SELECT * FROM user_analytics WHERE uploaded = 0 ORDER BY timestamp ASC")
    suspend fun getUnuploadedEvents(): List<UserAnalyticsEntity>
    
    @Query("UPDATE user_analytics SET uploaded = 1 WHERE id IN (:eventIds)")
    suspend fun markEventsAsUploaded(eventIds: List<Long>)
    
    @Query("DELETE FROM user_analytics WHERE uploaded = 1 AND timestamp < :cutoffTime")
    suspend fun deleteOldUploadedEvents(cutoffTime: Long)
    
    @Query("SELECT COUNT(*) FROM user_analytics WHERE eventType = :eventType AND timestamp > :since")
    suspend fun getEventCount(eventType: String, since: Long): Int
    
    @Query("SELECT * FROM user_analytics WHERE eventType = :eventType ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getEventsByType(eventType: String, limit: Int = 100): List<UserAnalyticsEntity>
    
    @Query("DELETE FROM user_analytics WHERE timestamp < :cutoffTime")
    suspend fun deleteOldEvents(cutoffTime: Long)
    
    @Query("DELETE FROM user_analytics")
    suspend fun clearAllAnalytics()
}
