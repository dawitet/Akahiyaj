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
data class AccessibilitySettings(
    val isHighContrastEnabled: Boolean = false,
    val isLargeTextEnabled: Boolean = false,
    val isScreenReaderEnabled: Boolean = false,
    val fontSizeMultiplier: Float = 1.0f
)

val LocalAccessibilitySettings = staticCompositionLocalOf { AccessibilitySettings() }

// Color.kt should now only contain:
// val CharcoalGrey = ComposeColor(0xFF36454F)
// val MustardYellow = ComposeColor(0xFFFFDA63)
// val RoyalBlue = ComposeColor(0xFF4169E1)
// val Cream = ComposeColor(0xFFFFD700)

// Enhanced Dark Theme with high contrast support
private val DarkThemeColorScheme = darkColorScheme(
    primary = MustardYellow,
    onPrimary = CharcoalGrey,
    secondary = CharcoalGrey,
    onSecondary = MustardYellow,
    tertiary = RoyalBlue,
    onTertiary = Cream,
    background = RoyalBlue,
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
    primary = CharcoalGrey,
    onPrimary = Cream,
    secondary = MustardYellow,
    onSecondary = CharcoalGrey,
    tertiary = MustardYellow,
    onTertiary = CharcoalGrey,
    background = Cream,
    onBackground = CharcoalGrey,
    surface = Cream,
    onSurface = CharcoalGrey,
    surfaceVariant = Cream,
    outline = CharcoalGrey,
    error = AkahidegnColors.Error,
    onError = Color.White
)

// High contrast color schemes for accessibility
private val HighContrastLightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color.Black,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF000080),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF000080),
    onSecondaryContainer = Color.White,
    surface = Color.White,
    onSurface = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color.Black,
    error = Color(0xFF8B0000),
    onError = Color.White,
    outline = Color.Black
)

private val HighContrastDarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color.White,
    onPrimaryContainer = Color.Black,
    secondary = Color.Yellow,
    onSecondary = Color.Black,
    secondaryContainer = Color.Yellow,
    onSecondaryContainer = Color.Black,
    surface = Color.Black,
    onSurface = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color.White,
    error = Color.Red,
    onError = Color.Black,
    outline = Color.White
)

@Composable
fun AkahidegnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    highContrast: Boolean = false,
    accessibilitySettings: AccessibilitySettings = AccessibilitySettings(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrast && darkTheme -> HighContrastDarkColorScheme
        highContrast && !darkTheme -> HighContrastLightColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkThemeColorScheme
        else -> LightThemeColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // WindowCompat.setDecorFitsSystemWindows(window, false) // THIS SHOULD BE IN Activity's onCreate

            // The primary responsibility here is to set the icon appearance
            // based on the theme, assuming edge-to-edge is already enabled.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme

            // If you want the navigation bar to also be transparent (or have a specific color)
            // and control its icons:
            // window.navigationBarColor = android.graphics.Color.TRANSPARENT // If making transparent
            // WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
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