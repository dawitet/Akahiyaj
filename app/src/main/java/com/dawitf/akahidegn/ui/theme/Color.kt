package com.dawitf.akahidegn.ui.theme


import androidx.compose.ui.graphics.Color

// --- CORRECTED COLOR DEFINITIONS ---
val CharcoalGrey = Color(0xFF36454F)  // Dark grey for text/backgrounds
val MustardYellow = Color(0xFFFFDA63) // Yellow accent color
val RoyalBlue = Color(0xFF4169E1)     // Blue primary color
val Cream = Color(0xFFFFD700)        // Golden/cream color
val GoldenText = Color(0xFFB8860B)    // Dark golden color for text in light mode

// --- TAB-SPECIFIC GRADIENT COLORS ---
// Home Tab Colors (Misty Blue to Byzantium)
val HomeGradientTop = Color(0xFF8FA3C4)     // Misty Blue
val HomeGradientBottom = Color(0xFF6A4C7F)  // Byzantium
val HomeContentBackground = Color(0xFF4A4A4A) // Charcoal Grey
val HomeContentText = Color(0xFFFFFFFF)     // White

// Settings Tab Colors (Dark Green to Pink)
val SettingsGradientTop = Color(0xFF06402B)    // Dark Green
val SettingsGradientBottom = Color(0xFFFF8DA1) // Pink
val SettingsContentBackground = Color(0xFF4A4A4A) // Charcoal Grey
val SettingsContentText = Color(0xFFFFC30B)    // Golden Yellow
val SettingsContentColor = SettingsContentText // Alias for compatibility

// Active Groups Tab Colors (Charcoal Grey to Golden Yellow)
val ActiveGroupsGradientTop = Color(0xFF4A4A4A)    // Charcoal Grey
val ActiveGroupsGradientBottom = Color(0xFFFFC30B) // Golden Yellow
val ActiveGroupsContentBackground = Color(0xFF8FA3C4) // Misty Blue
val ActiveGroupsContentText = Color(0xFFFFFFFF)    // White
val ActiveGroupsContentColor = ActiveGroupsContentText // Alias for compatibility

// Additional aliases for compatibility
val HomeContentColor = HomeContentText