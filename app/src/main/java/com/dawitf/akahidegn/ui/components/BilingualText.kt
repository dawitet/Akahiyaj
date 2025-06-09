package com.dawitf.akahidegn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * BilingualText component for displaying text in both English and Amharic
 * Used throughout the app for bilingual support
 */
@Composable
fun BilingualText(
    englishText: String,
    amharicText: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign = TextAlign.Start,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    arrangement: Arrangement.Vertical = Arrangement.spacedBy(2.dp)
) {
    Column(
        modifier = modifier,
        verticalArrangement = arrangement,
        horizontalAlignment = when (textAlign) {
            TextAlign.Center -> Alignment.CenterHorizontally
            TextAlign.End -> Alignment.End
            else -> Alignment.Start
        }
    ) {
        // English text
        Text(
            text = englishText,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign,
            style = MaterialTheme.typography.bodyMedium
        )
        
        // Amharic text with enhanced styling
        AmharicText(
            text = amharicText,
            color = color.copy(alpha = 0.8f),
            fontSize = (fontSize.value * 0.9f).sp,
            fontWeight = fontWeight,
            textAlign = textAlign
        )
    }
}

/**
 * AmharicText component for displaying Amharic text with optimized styling
 */
@Composable
fun AmharicText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign,
        style = MaterialTheme.typography.bodyMedium.copy(
            // Enhanced styling for Amharic text
            letterSpacing = 0.1.sp,
            lineHeight = (fontSize.value * 1.2f).sp
        )
    )
}

@Composable
fun BilingualText(
    englishText: String,
    amharicText: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign = TextAlign.Start
) {
    BilingualText(
        englishText = englishText,
        amharicText = amharicText,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        fontSize = style.fontSize,
        fontWeight = style.fontWeight ?: FontWeight.Normal
    )
}
