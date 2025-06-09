package com.dawitf.akahidegn.ui.theme

// No longer import android.graphics.Color here for statusBarColor
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// High contrast color schemes for accessibility
val HighContrastDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF000000),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF)
)

val HighContrastLightColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFFFFF),
    onPrimaryContainer = Color(0xFF000000),
    secondary = Color(0xFF000000),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000)
)

// Theme mode setting is now in com.dawitf.akahidegn.ui.components.ThemeMode

// Enhanced color palette
object AkahidegnColors {
    // Primary colors - keeping original colors but enhanced
    val Primary = RoyalBlue
    val PrimaryVariant = Color(0xFF2F4F8F)
    val Secondary = MustardYellow
    val SecondaryVariant = Color(0xFFDCC700)
    
    // Semantic colors
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFF9800)
    val Error = Color(0xFFF44336)
    val Info = Color(0xFF2196F3)
    
    // Ride status colors
    val RideActive = Color(0xFF4CAF50)
    val RideCompleted = Color(0xFF9E9E9E)
    val RideCancelled = Color(0xFFF44336)
    val RidePending = Color(0xFFFF9800)
    
    // Driver mode colors
    val DriverOnline = Color(0xFF4CAF50)
    val DriverOffline = Color(0xFF9E9E9E)
    val DriverBusy = Color(0xFFFF9800)
}

// Accessibility settings
// Using the new AccessibilitySettings from util package
val LocalAccessibilitySettings = staticCompositionLocalOf { com.dawitf.akahidegn.util.AccessibilitySettings() }

// Color.kt should now only contain:
// val CharcoalGrey = ComposeColor(0xFF36454F)
// val MustardYellow = ComposeColor(0xFFFFDA63)
// val RoyalBlue = ComposeColor(0xFF4169E1)
// val Cream = ComposeColor(0xFFFFD700)

// Enhanced Dark Theme with high contrast support
private val DarkThemeColorScheme = darkColorScheme(
    primary = CharcoalGrey,
    onPrimary = Cream,
    secondary = MustardYellow,
    onSecondary = CharcoalGrey,
    tertiary = MustardYellow,
    onTertiary = CharcoalGrey,
    background = CharcoalGrey,
    onBackground = Cream,
    surface = CharcoalGrey,
    onSurface = Cream,
    surfaceVariant = CharcoalGrey,
    outline = MustardYellow,
    error = AkahidegnColors.Error,
    onError = Color.White
)

// Enhanced Light Theme
private val LightThemeColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    secondary = MustardYellow,
    onSecondary = CharcoalGrey,
    tertiary = CharcoalGrey,
    onTertiary = Cream,
    background = Cream,
    onBackground = GoldenText,  // Changed to golden color for text
    surface = Cream,
    onSurface = GoldenText,     // Changed to golden color for text
    surfaceVariant = Cream,
    onSurfaceVariant = GoldenText, // Changed to golden color for text
    outline = RoyalBlue,
    error = AkahidegnColors.Error,
    onError = Color.White
)

// High contrast color schemes imported from EnhancedTheme
// Note: These are defined in EnhancedTheme.kt to avoid duplication

@Composable
fun AkahidegnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeMode: com.dawitf.akahidegn.ui.components.ThemeMode = com.dawitf.akahidegn.ui.components.ThemeMode.SYSTEM,
    accessibilitySettings: com.dawitf.akahidegn.util.AccessibilitySettings = com.dawitf.akahidegn.util.AccessibilitySettings(),
    content: @Composable () -> Unit
) {
    // Determine if dark theme should be used based on themeMode
    val effectiveDarkTheme = when (themeMode) {
        com.dawitf.akahidegn.ui.components.ThemeMode.LIGHT -> false
        com.dawitf.akahidegn.ui.components.ThemeMode.DARK -> true
        com.dawitf.akahidegn.ui.components.ThemeMode.SYSTEM -> darkTheme
    }
    
    val colorScheme = when {
        accessibilitySettings.enableHighContrast && effectiveDarkTheme -> HighContrastDarkColorScheme
        accessibilitySettings.enableHighContrast && !effectiveDarkTheme -> HighContrastLightColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (effectiveDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        effectiveDarkTheme -> DarkThemeColorScheme
        else -> LightThemeColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            if (activity != null) {
                val window = activity.window
                // WindowCompat.setDecorFitsSystemWindows(window, false) // THIS SHOULD BE IN Activity's onCreate

                // The primary responsibility here is to set the icon appearance
                // based on the theme, assuming edge-to-edge is already enabled.
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !effectiveDarkTheme

                // If you want the navigation bar to also be transparent (or have a specific color)
                // and control its icons:
                // window.navigationBarColor = android.graphics.Color.TRANSPARENT // If making transparent
                // WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalAccessibilitySettings provides accessibilitySettings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography, // Assumes Typography.kt is set up
            shapes = Shapes,         // Assumes Shapes.kt is set up
            content = content
        )
    }
}