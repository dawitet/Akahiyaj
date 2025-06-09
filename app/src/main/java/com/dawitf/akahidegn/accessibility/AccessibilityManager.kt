package com.dawitf.akahidegn.accessibility

import android.content.Context
import android.graphics.Typeface
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dawitf.akahidegn.data.datastore.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val highContrastKey = booleanPreferencesKey("high_contrast_enabled")
    private val largeTextKey = booleanPreferencesKey("large_text_enabled")
    private val voiceGuidanceKey = booleanPreferencesKey("voice_guidance_enabled")
    private val textScaleKey = floatPreferencesKey("text_scale_factor")
    private val colorBlindModeKey = stringPreferencesKey("color_blind_mode")
    private val reducedMotionKey = booleanPreferencesKey("reduced_motion_enabled")
    private val screenReaderKey = booleanPreferencesKey("screen_reader_optimized")
    
    val accessibilitySettings: Flow<AccessibilitySettings> = context.dataStore.data.map { preferences ->
        AccessibilitySettings(
            highContrastEnabled = preferences[highContrastKey] ?: false,
            largeTextEnabled = preferences[largeTextKey] ?: false,
            voiceGuidanceEnabled = preferences[voiceGuidanceKey] ?: false,
            textScaleFactor = preferences[textScaleKey] ?: 1.0f,
            colorBlindMode = ColorBlindMode.valueOf(preferences[colorBlindModeKey] ?: "NONE"),
            reducedMotionEnabled = preferences[reducedMotionKey] ?: false,
            screenReaderOptimized = preferences[screenReaderKey] ?: isScreenReaderEnabled()
        )
    }
    
    suspend fun setHighContrastEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[highContrastKey] = enabled
        }
    }
    
    suspend fun setLargeTextEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[largeTextKey] = enabled
        }
    }
    
    suspend fun setVoiceGuidanceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[voiceGuidanceKey] = enabled
        }
    }
    
    suspend fun setTextScaleFactor(factor: Float) {
        context.dataStore.edit { preferences ->
            preferences[textScaleKey] = factor.coerceIn(0.8f, 3.0f)
        }
    }
    
    suspend fun setColorBlindMode(mode: ColorBlindMode) {
        context.dataStore.edit { preferences ->
            preferences[colorBlindModeKey] = mode.name
        }
    }
    
    suspend fun setReducedMotionEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[reducedMotionKey] = enabled
        }
    }
    
    suspend fun setScreenReaderOptimized(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[screenReaderKey] = enabled
        }
    }
    
    fun isScreenReaderEnabled(): Boolean {
        return try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_SPEAK_PASSWORD,
                0
            ) == 1 || Settings.Secure.getInt(
                context.contentResolver,
                "accessibility_enabled",
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }
    
    fun getSystemFontScale(): Float {
        return context.resources.configuration.fontScale
    }
    
    fun isSystemDarkModeEnabled(): Boolean {
        return (context.resources.configuration.uiMode and 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}

data class AccessibilitySettings(
    val highContrastEnabled: Boolean = false,
    val largeTextEnabled: Boolean = false,
    val voiceGuidanceEnabled: Boolean = false,
    val textScaleFactor: Float = 1.0f,
    val colorBlindMode: ColorBlindMode = ColorBlindMode.NONE,
    val reducedMotionEnabled: Boolean = false,
    val screenReaderOptimized: Boolean = false
)

enum class ColorBlindMode {
    NONE,
    PROTANOPIA,
    DEUTERANOPIA,
    TRITANOPIA,
    MONOCHROMACY
}

@Composable
fun AccessibilityTheme(
    settings: AccessibilitySettings,
    content: @Composable () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    
    // Adjust colors based on accessibility settings
    val colors = if (settings.highContrastEnabled) {
        if (isDarkTheme) {
            getHighContrastDarkColors()
        } else {
            getHighContrastLightColors()
        }
    } else {
        if (settings.colorBlindMode != ColorBlindMode.NONE) {
            getColorBlindFriendlyColors(settings.colorBlindMode, isDarkTheme)
        } else {
            MaterialTheme.colorScheme
        }
    }
    
    // Adjust typography based on accessibility settings
    val typography = MaterialTheme.typography.copy(
        headlineLarge = MaterialTheme.typography.headlineLarge.copy(
            fontSize = (MaterialTheme.typography.headlineLarge.fontSize.value * settings.textScaleFactor).sp,
            fontWeight = if (settings.largeTextEnabled) FontWeight.Bold else FontWeight.Normal
        ),
        headlineMedium = MaterialTheme.typography.headlineMedium.copy(
            fontSize = (MaterialTheme.typography.headlineMedium.fontSize.value * settings.textScaleFactor).sp,
            fontWeight = if (settings.largeTextEnabled) FontWeight.Bold else FontWeight.Normal
        ),
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(
            fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * settings.textScaleFactor).sp,
            fontWeight = if (settings.largeTextEnabled) FontWeight.Medium else FontWeight.Normal
        ),
        bodyMedium = MaterialTheme.typography.bodyMedium.copy(
            fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * settings.textScaleFactor).sp,
            fontWeight = if (settings.largeTextEnabled) FontWeight.Medium else FontWeight.Normal
        ),
        labelLarge = MaterialTheme.typography.labelLarge.copy(
            fontSize = (MaterialTheme.typography.labelLarge.fontSize.value * settings.textScaleFactor).sp,
            fontWeight = if (settings.largeTextEnabled) FontWeight.Medium else FontWeight.Normal
        )
    )
    
    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        content = content
    )
}

@Composable
fun getHighContrastLightColors() = MaterialTheme.colorScheme.copy(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color(0xFF333333),
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    outline = Color.Black,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color.Black
)

@Composable
fun getHighContrastDarkColors() = MaterialTheme.colorScheme.copy(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color(0xFFCCCCCC),
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    outline = Color.White,
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color.White
)

@Composable
fun getColorBlindFriendlyColors(mode: ColorBlindMode, isDark: Boolean) = when (mode) {
    ColorBlindMode.PROTANOPIA -> if (isDark) {
        MaterialTheme.colorScheme.copy(
            primary = Color(0xFF0077BE),
            secondary = Color(0xFFFFA500),
            tertiary = Color(0xFF9400D3)
        )
    } else {
        MaterialTheme.colorScheme.copy(
            primary = Color(0xFF0066CC),
            secondary = Color(0xFFFF8C00),
            tertiary = Color(0xFF8A2BE2)
        )
    }
    
    ColorBlindMode.DEUTERANOPIA -> if (isDark) {
        MaterialTheme.colorScheme.copy(
            primary = Color(0xFF0099CC),
            secondary = Color(0xFFFFAA00),
            tertiary = Color(0xFFCC3366)
        )
    } else {
        MaterialTheme.colorScheme.copy(
            primary = Color(0xFF0088BB),
            secondary = Color(0xFFFF9900),
            tertiary = Color(0xFFCC0066)
        )
    }
    
    ColorBlindMode.TRITANOPIA -> if (isDark) {
        MaterialTheme.colorScheme.copy(
            primary = Color(0xFF0088AA),
            secondary = Color(0xFFFF6600),
            tertiary = Color(0xFFAA0088)
        )
    } else {
        MaterialTheme.colorScheme.copy(
            primary = Color(0xFF006699),
            secondary = Color(0xFFFF5500),
            tertiary = Color(0xFF990077)
        )
    }
    
    ColorBlindMode.MONOCHROMACY -> if (isDark) {
        MaterialTheme.colorScheme.copy(
            primary = Color(0xFFCCCCCC),
            secondary = Color(0xFF999999),
            tertiary = Color(0xFF666666)
        )
    } else {
        MaterialTheme.colorScheme.copy(
            primary = Color(0xFF333333),
            secondary = Color(0xFF666666),
            tertiary = Color(0xFF999999)
        )
    }
    
    ColorBlindMode.NONE -> MaterialTheme.colorScheme
}

@Composable
fun rememberAccessibilitySettings(): AccessibilitySettings {
    val context = LocalContext.current
    val accessibilityManager = remember { AccessibilityManager(context) }
    val settings by accessibilityManager.accessibilitySettings.collectAsState(
        initial = AccessibilitySettings()
    )
    return settings
}
