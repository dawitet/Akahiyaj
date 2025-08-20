package com.dawitf.akahidegn.ui.theme


import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    // Shapes for small components like Buttons, Chips, TextFields
    small = RoundedCornerShape(8.dp),             // Example: 8dp rounded corners

    // Shapes for medium components like Cards
    medium = RoundedCornerShape(12.dp),            // Example: 12dp rounded corners

    // Shapes for large components like Modal bottom sheets, Navigation drawers
    large = RoundedCornerShape(16.dp),             // Example: 16dp rounded corners

    // You can also define an extraSmall and extraLarge if needed by specific components
    extraSmall = RoundedCornerShape(4.dp),         // Example: 4dp
    extraLarge = RoundedCornerShape(24.dp)         // Example: 24dp
)