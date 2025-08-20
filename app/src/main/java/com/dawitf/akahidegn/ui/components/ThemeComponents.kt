package com.dawitf.akahidegn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

@Composable
fun ThemeToggleCard(
    currentTheme: ThemeMode,
    onThemeChanged: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Theme Preference",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOption(
                    theme = ThemeMode.LIGHT,
                    currentTheme = currentTheme,
                    onThemeSelected = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onThemeChanged(it)
                    },
                    icon = Icons.Filled.Brightness6,
                    label = "Light",
                    modifier = Modifier.weight(1f)
                )
                
                ThemeOption(
                    theme = ThemeMode.DARK,
                    currentTheme = currentTheme,
                    onThemeSelected = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onThemeChanged(it)
                    },
                    icon = Icons.Filled.Brightness2,
                    label = "Dark",
                    modifier = Modifier.weight(1f)
                )
                
                ThemeOption(
                    theme = ThemeMode.SYSTEM,
                    currentTheme = currentTheme,
                    onThemeSelected = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onThemeChanged(it)
                    },
                    icon = Icons.Filled.Settings,
                    label = "System",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    theme: ThemeMode,
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val isSelected = currentTheme == theme
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(300),
        label = "background"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(300),
        label = "content"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(200),
        label = "scale"
    )
    
    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onThemeSelected(theme) }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "$label theme",
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun AnimatedThemeToggle(
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val rotation by animateFloatAsState(
        targetValue = if (isDarkTheme) 180f else 0f,
        animationSpec = tween(500),
        label = "rotation"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isDarkTheme) {
            Color(0xFF1A1A1A)
        } else {
            Color(0xFFFFF9C4)
        },
        animationSpec = tween(500),
        label = "background"
    )
    
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isDarkTheme) Icons.Filled.Brightness2 else Icons.Filled.Brightness6,
            contentDescription = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme",
            tint = if (isDarkTheme) Color.White else Color(0xFF1A1A1A),
            modifier = Modifier
                .size(24.dp)
                .rotate(rotation)
        )
    }
}
