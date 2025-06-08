package com.dawitf.akahidegn.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.dawitf.akahidegn.R

/**
 * Enhanced Typography Components for Amharic Text
 * Provides optimized text rendering for Ethiopian text
 */

// Noto Sans Ethiopic font family for better Amharic rendering
// Note: Font files need to be added to res/font/ directory
val notoSansEthiopic = FontFamily.Default

@Composable
fun AmharicTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = notoSansEthiopic,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            lineHeight = 32.sp
        ),
        color = color,
        textAlign = textAlign,
        modifier = modifier
    )
}

@Composable
fun AmharicHeadline(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = notoSansEthiopic,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
            lineHeight = 28.sp
        ),
        color = color,
        textAlign = textAlign,
        modifier = modifier
    )
}

@Composable
fun AmharicBody(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = notoSansEthiopic,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.2.sp,
            lineHeight = 24.sp
        ),
        color = color,
        textAlign = textAlign,
        modifier = modifier
    )
}

@Composable
fun AmharicBodySmall(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = notoSansEthiopic,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.1.sp,
            lineHeight = 20.sp
        ),
        color = color,
        textAlign = textAlign,
        modifier = modifier
    )
}

@Composable
fun AmharicLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontFamily = notoSansEthiopic,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp,
            lineHeight = 18.sp
        ),
        color = color,
        textAlign = textAlign,
        modifier = modifier
    )
}

@Composable
fun AmharicButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onPrimary
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontFamily = notoSansEthiopic,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
        ),
        color = color,
        modifier = modifier
    )
}
