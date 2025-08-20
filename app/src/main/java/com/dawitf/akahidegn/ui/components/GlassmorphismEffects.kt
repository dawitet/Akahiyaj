package com.dawitf.akahidegn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism effect modifier that creates a frosted glass appearance
 */
fun Modifier.glassmorphism(
    blurRadius: androidx.compose.ui.unit.Dp = 10.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 12.dp,
    alpha: Float = 0.1f
): Modifier = composed {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    
    this
        .clip(RoundedCornerShape(cornerRadius))
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    surfaceColor.copy(alpha = alpha + 0.1f),
                    primaryColor.copy(alpha = alpha),
                    surfaceColor.copy(alpha = alpha)
                )
            )
        )
        .blur(radius = blurRadius)
}

/**
 * Card glassmorphism effect for elevated surfaces
 */
fun Modifier.glassCard(
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    alpha: Float = 0.15f
): Modifier = composed {
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primaryContainer
    
    this
        .clip(RoundedCornerShape(cornerRadius))
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = alpha),
                    surfaceColor.copy(alpha = alpha + 0.05f),
                    surfaceColor.copy(alpha = alpha)
                )
            )
        )
}

/**
 * Gradient background modifier with Material 3 colors
 */
fun Modifier.gradientBackground(
    isVertical: Boolean = true
): Modifier = composed {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceContainerColor = MaterialTheme.colorScheme.surfaceContainer
    
    background(
        brush = if (isVertical) {
            Brush.verticalGradient(
                colors = listOf(
                    surfaceColor,
                    surfaceContainerColor,
                    surfaceColor
                )
            )
        } else {
            Brush.horizontalGradient(
                colors = listOf(
                    surfaceColor,
                    surfaceContainerColor,
                    surfaceColor
                )
            )
        }
    )
}
