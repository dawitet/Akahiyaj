package com.dawitf.akahidegn.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences"
)

/**
 * Available theme modes.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM // Follow system setting
}

/**
 * Available language options.
 */
enum class AppLanguage {
    ENGLISH,
    AMHARIC,
    SYSTEM // Follow system language
}

/**
 * Theme configuration data class.
 */
data class ThemeConfiguration(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val useDynamicColors: Boolean = true, // Material You dynamic colors on Android 12+
    val useHighContrast: Boolean = false, // Accessibility feature
    val fontSize: FontSize = FontSize.MEDIUM
)

/**
 * Font size options for accessibility.
 */
enum class FontSize(val scaleFactor: Float) {
    SMALL(0.85f),
    MEDIUM(1.0f),
    LARGE(1.15f),
    EXTRA_LARGE(1.3f),
    NORMAL(1.0f) // Added NORMAL size
}

@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val dataStore = context.themeDataStore
    
    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
        val USE_HIGH_CONTRAST = booleanPreferencesKey("use_high_contrast")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val CHAT_NOTIFICATIONS_ENABLED = booleanPreferencesKey("chat_notifications_enabled")
        val TRIP_NOTIFICATIONS_ENABLED = booleanPreferencesKey("trip_notifications_enabled")
        val SYSTEM_NOTIFICATIONS_ENABLED = booleanPreferencesKey("system_notifications_enabled")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        val LOCATION_DATA_ENABLED = booleanPreferencesKey("location_data_enabled")
    }
    
    val themeConfigurationFlow: Flow<ThemeConfiguration> = dataStore.data.map { preferences ->
        ThemeConfiguration(
            themeMode = preferences[THEME_MODE]?.let { 
                try { ThemeMode.valueOf(it) } 
                catch (e: Exception) { ThemeMode.SYSTEM }
            } ?: ThemeMode.SYSTEM,
            language = preferences[APP_LANGUAGE]?.let {
                try { AppLanguage.valueOf(it) }
                catch (e: Exception) { AppLanguage.SYSTEM }
            } ?: AppLanguage.SYSTEM,
            useDynamicColors = preferences[USE_DYNAMIC_COLORS] ?: true,
            useHighContrast = preferences[USE_HIGH_CONTRAST] ?: false,
            fontSize = preferences[FONT_SIZE]?.let {
                try { FontSize.valueOf(it) }
                catch (e: Exception) { FontSize.MEDIUM }
            } ?: FontSize.MEDIUM
        )
    }
    
    // Individual Flow properties for SettingsViewModel
    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        preferences[THEME_MODE]?.let { 
            try { ThemeMode.valueOf(it) } 
            catch (e: Exception) { ThemeMode.SYSTEM }
        } ?: ThemeMode.SYSTEM
    }
    
    val language: Flow<com.dawitf.akahidegn.domain.model.LanguageOption> = dataStore.data.map { preferences ->
        preferences[APP_LANGUAGE]?.let {
            try { 
                when (AppLanguage.valueOf(it)) {
                    AppLanguage.ENGLISH -> com.dawitf.akahidegn.domain.model.LanguageOption.ENGLISH
                    AppLanguage.AMHARIC -> com.dawitf.akahidegn.domain.model.LanguageOption.AMHARIC
                    AppLanguage.SYSTEM -> com.dawitf.akahidegn.domain.model.LanguageOption.SYSTEM
                }
            }
            catch (e: Exception) { com.dawitf.akahidegn.domain.model.LanguageOption.SYSTEM }
        } ?: com.dawitf.akahidegn.domain.model.LanguageOption.SYSTEM
    }
    
    val useDynamicColors: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[USE_DYNAMIC_COLORS] ?: true
    }
    
    val useHighContrast: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[USE_HIGH_CONTRAST] ?: false
    }
    
    val fontSize: Flow<com.dawitf.akahidegn.domain.model.FontSizeOption> = dataStore.data.map { preferences ->
        preferences[FONT_SIZE]?.let {
            try { 
                when (FontSize.valueOf(it)) {
                    FontSize.SMALL -> com.dawitf.akahidegn.domain.model.FontSizeOption.SMALL
                    FontSize.NORMAL -> com.dawitf.akahidegn.domain.model.FontSizeOption.NORMAL
                    FontSize.MEDIUM -> com.dawitf.akahidegn.domain.model.FontSizeOption.MEDIUM
                    FontSize.LARGE -> com.dawitf.akahidegn.domain.model.FontSizeOption.LARGE
                    FontSize.EXTRA_LARGE -> com.dawitf.akahidegn.domain.model.FontSizeOption.EXTRA_LARGE
                }
            }
            catch (e: Exception) { com.dawitf.akahidegn.domain.model.FontSizeOption.MEDIUM }
        } ?: com.dawitf.akahidegn.domain.model.FontSizeOption.MEDIUM
    }
    
    // Notification preferences
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED] ?: true
    }
    
    val chatNotificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[CHAT_NOTIFICATIONS_ENABLED] ?: true
    }
    
    val tripNotificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[TRIP_NOTIFICATIONS_ENABLED] ?: true
    }
    
    val systemNotificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SYSTEM_NOTIFICATIONS_ENABLED] ?: true
    }
    
    val quietHoursEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[QUIET_HOURS_ENABLED] ?: false
    }
    
    val analyticsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ANALYTICS_ENABLED] ?: true
    }
    
    val locationDataEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[LOCATION_DATA_ENABLED] ?: true
    }
    
    suspend fun getThemeConfiguration(): ThemeConfiguration {
        return themeConfigurationFlow.first()
    }
    
    suspend fun updateThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = themeMode.name
        }
    }
    
    suspend fun updateLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[APP_LANGUAGE] = language.name
        }
    }
    
    suspend fun updateDynamicColors(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_DYNAMIC_COLORS] = enabled
        }
    }
    
    suspend fun updateHighContrast(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_HIGH_CONTRAST] = enabled
        }
    }
    
    suspend fun updateFontSize(fontSize: FontSize) {
        dataStore.edit { preferences ->
            preferences[FONT_SIZE] = fontSize.name
        }
    }
    
    suspend fun updateThemeConfiguration(configuration: ThemeConfiguration) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = configuration.themeMode.name
            preferences[APP_LANGUAGE] = configuration.language.name
            preferences[USE_DYNAMIC_COLORS] = configuration.useDynamicColors
            preferences[USE_HIGH_CONTRAST] = configuration.useHighContrast
            preferences[FONT_SIZE] = configuration.fontSize.name
        }
    }
    
    suspend fun setThemeMode(themeMode: ThemeMode) {
        updateThemeMode(themeMode)
    }
    
    suspend fun setLanguage(language: com.dawitf.akahidegn.domain.model.LanguageOption) {
        val appLanguage = when (language) {
            com.dawitf.akahidegn.domain.model.LanguageOption.ENGLISH -> AppLanguage.ENGLISH
            com.dawitf.akahidegn.domain.model.LanguageOption.AMHARIC -> AppLanguage.AMHARIC
            com.dawitf.akahidegn.domain.model.LanguageOption.OROMO -> AppLanguage.AMHARIC // Map to closest
            com.dawitf.akahidegn.domain.model.LanguageOption.SYSTEM -> AppLanguage.SYSTEM
        }
        updateLanguage(appLanguage)
    }
    
    suspend fun setUseDynamicColors(enabled: Boolean) {
        updateDynamicColors(enabled)
    }
    
    suspend fun setUseHighContrast(enabled: Boolean) {
        updateHighContrast(enabled)
    }
    
    suspend fun setFontSize(fontSize: com.dawitf.akahidegn.domain.model.FontSizeOption) {
        val fontSizeEnum = when (fontSize) {
            com.dawitf.akahidegn.domain.model.FontSizeOption.SMALL -> FontSize.SMALL
            com.dawitf.akahidegn.domain.model.FontSizeOption.MEDIUM -> FontSize.MEDIUM
            com.dawitf.akahidegn.domain.model.FontSizeOption.LARGE -> FontSize.LARGE
            com.dawitf.akahidegn.domain.model.FontSizeOption.EXTRA_LARGE -> FontSize.EXTRA_LARGE
            com.dawitf.akahidegn.domain.model.FontSizeOption.NORMAL -> FontSize.NORMAL // Added missing NORMAL branch
        }
        updateFontSize(fontSizeEnum)
    }
    
    // Notification preference setters
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    suspend fun setChatNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CHAT_NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    suspend fun setTripNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[TRIP_NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    suspend fun setSystemNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SYSTEM_NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[QUIET_HOURS_ENABLED] = enabled
        }
    }
    
    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ANALYTICS_ENABLED] = enabled
        }
    }
    
    suspend fun setLocationDataEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[LOCATION_DATA_ENABLED] = enabled
        }
    }
}
