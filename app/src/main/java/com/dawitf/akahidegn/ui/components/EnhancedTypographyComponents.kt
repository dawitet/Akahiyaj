package com.dawitf.akahidegn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import kotlin.math.pow
import com.dawitf.akahidegn.R
import com.dawitf.akahidegn.ui.theme.AmharicFontFamily

/**
 * Enhanced Typography Components with Amharic/Ethiopic Font Support
 * Provides dynamic text sizing and improved accessibility
 */

@Composable
fun rememberDynamicTextSize(): Float {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    return remember(configuration.screenWidthDp, configuration.fontScale) {
        when {
            configuration.screenWidthDp < 360 -> 0.9f  // Small screens
            configuration.screenWidthDp > 600 -> 1.1f  // Large screens/tablets
            else -> 1.0f  // Normal screens
        } * configuration.fontScale
    }
}

@Composable
fun AdaptiveText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
    isAmharic: Boolean = false,
    fontSizeMultiplier: Float = 1f
) {
    val dynamicScale = rememberDynamicTextSize()
    val finalFontSize = style.fontSize * dynamicScale * fontSizeMultiplier
    
    val adaptedStyle = if (isAmharic) {
        style.copy(
            fontFamily = AmharicFontFamily,
            fontSize = finalFontSize * 1.1f, // Slightly larger for Amharic text
            lineHeight = finalFontSize * 1.4f // Better line spacing for Amharic
        )
    } else {
        style.copy(fontSize = finalFontSize)
    }
    
    Text(
        text = text,
        modifier = modifier,
        style = adaptedStyle,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign
    )
}

@Composable
fun BilingualText(
    englishText: String,
    amharicText: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    showBoth: Boolean = true,
    primaryLanguage: String = "en" // "en" or "am"
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (primaryLanguage == "en" || !showBoth) {
            AdaptiveText(
                text = englishText,
                style = style,
                color = color,
                isAmharic = false
            )
        }
        
        if (showBoth && primaryLanguage == "en") {
            AdaptiveText(
                text = amharicText,
                style = style.copy(
                    fontSize = style.fontSize * 0.9f,
                    fontWeight = FontWeight.Normal
                ),
                color = color.copy(alpha = 0.8f),
                isAmharic = true
            )
        } else if (primaryLanguage == "am") {
            AdaptiveText(
                text = amharicText,
                style = style,
                color = color,
                isAmharic = true
            )
            
            if (showBoth) {
                AdaptiveText(
                    text = englishText,
                    style = style.copy(
                        fontSize = style.fontSize * 0.9f,
                        fontWeight = FontWeight.Normal
                    ),
                    color = color.copy(alpha = 0.8f),
                    isAmharic = false
                )
            }
        }
    }
}

@Composable
fun EnhancedTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    isAmharic: Boolean = false,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AdaptiveText(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = titleColor,
            isAmharic = isAmharic,
            fontSizeMultiplier = 1.1f
        )
        
        subtitle?.let {
            AdaptiveText(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor,
                isAmharic = isAmharic
            )
        }
    }
}

@Composable
fun AccessibleTextCard(
    title: String,
    content: String,
    modifier: Modifier = Modifier,
    isAmharic: Boolean = false,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    cornerRadius: Dp = 12.dp
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdaptiveText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                isAmharic = isAmharic,
                fontSizeMultiplier = 1.05f
            )
            
            AdaptiveText(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.9f),
                isAmharic = isAmharic
            )
        }
    }
}

@Composable
fun DynamicSizeText(
    text: String,
    modifier: Modifier = Modifier,
    minSize: Float = 12f,
    maxSize: Float = 18f,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    isAmharic: Boolean = false
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    
    val calculatedSize = remember(screenWidth, configuration.fontScale) {
        val baseSize = when {
            screenWidth < 360 -> minSize
            screenWidth > 600 -> maxSize
            else -> (minSize + maxSize) / 2
        }
        (baseSize * configuration.fontScale).coerceIn(minSize, maxSize)
    }
    
    val adaptedStyle = if (isAmharic) {
        style.copy(
            fontFamily = AmharicFontFamily,
            fontSize = calculatedSize.sp * 1.1f,
            lineHeight = calculatedSize.sp * 1.4f
        )
    } else {
        style.copy(fontSize = calculatedSize.sp)
    }
    
    Text(
        text = text,
        modifier = modifier,
        style = adaptedStyle,
        color = color
    )
}

@Composable
fun TextWithIcon(
    text: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    isAmharic: Boolean = false,
    iconPosition: IconPosition = IconPosition.Leading,
    spacing: Dp = 8.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        if (iconPosition == IconPosition.Leading) {
            icon()
        }
        
        AdaptiveText(
            text = text,
            style = style,
            color = color,
            isAmharic = isAmharic,
            modifier = Modifier.weight(1f)
        )
        
        if (iconPosition == IconPosition.Trailing) {
            icon()
        }
    }
}

enum class IconPosition {
    Leading, Trailing
}

@Composable
fun HighContrastText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    isAmharic: Boolean = false,
    highContrast: Boolean = false
) {
    val textColor = if (highContrast) {
        if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
            Color.Black
        } else {
            Color.White
        }
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    val backgroundColor = if (highContrast) {
        if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
            Color.White
        } else {
            Color.Black
        }
    } else {
        Color.Transparent
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .then(
                if (highContrast) {
                    Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                } else {
                    Modifier
                }
            )
    ) {
        AdaptiveText(
            text = text,
            style = style.copy(
                fontWeight = if (highContrast) FontWeight.Bold else style.fontWeight
            ),
            color = textColor,
            isAmharic = isAmharic
        )
    }
}

// Extension function to calculate luminance
private fun Color.luminance(): Float {
    val r = if (red <= 0.03928f) red / 12.92f else ((red + 0.055) / 1.055).pow(2.4).toFloat()
    val g = if (green <= 0.03928f) green / 12.92f else ((green + 0.055) / 1.055).pow(2.4).toFloat()
    val b = if (blue <= 0.03928f) blue / 12.92f else ((blue + 0.055) / 1.055).pow(2.4).toFloat()
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
