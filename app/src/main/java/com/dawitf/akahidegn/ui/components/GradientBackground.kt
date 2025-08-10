package com.dawitf.akahidegn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun FrostedGlassTabHeader(
    topColor: Color,
    bottomColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blob_animation")
    
    // Animate blob positions
    val blob1X by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1X"
    )
    
    val blob2X by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2X"
    )
    
    val blob1Y by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1Y"
    )
    
    val blob2Y by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2Y"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor.copy(alpha = 0.9f), bottomColor.copy(alpha = 0.9f))
                )
            )
    ) {
        // Animated color blobs behind frosted glass
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            // Blob 1
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(
                        x = (blob1X * 250).dp,
                        y = (blob1Y * 40).dp
                    )
                    .background(
                        color = topColor.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .blur(20.dp)
            )
            
            // Blob 2
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .offset(
                        x = (blob2X * 200).dp,
                        y = (blob2Y * 20).dp
                    )
                    .background(
                        color = bottomColor.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .blur(25.dp)
            )
            
            // Frosted glass overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.White.copy(alpha = 0.1f)
                    )
                    .blur(2.dp)
            )
        }
        
        // Content on top
        content()
    }
}

@Composable
fun TabContentArea(
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        content()
    }
}

@Composable
fun HomeTabLayout(
    modifier: Modifier = Modifier,
    headerContent: @Composable BoxScope.() -> Unit,
    mainContent: @Composable BoxScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        FrostedGlassTabHeader(
            topColor = com.dawitf.akahidegn.ui.theme.HomeGradientTop,
            bottomColor = com.dawitf.akahidegn.ui.theme.HomeGradientBottom,
            content = headerContent
        )
        
        TabContentArea(
            backgroundColor = com.dawitf.akahidegn.ui.theme.HomeContentBackground,
            textColor = com.dawitf.akahidegn.ui.theme.HomeContentText,
            modifier = Modifier.weight(1f),
            content = mainContent
        )
    }
}

@Composable
fun SettingsTabLayout(
    modifier: Modifier = Modifier,
    headerContent: @Composable BoxScope.() -> Unit,
    mainContent: @Composable BoxScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        FrostedGlassTabHeader(
            topColor = com.dawitf.akahidegn.ui.theme.SettingsGradientTop,
            bottomColor = com.dawitf.akahidegn.ui.theme.SettingsGradientBottom,
            content = headerContent
        )
        
        TabContentArea(
            backgroundColor = com.dawitf.akahidegn.ui.theme.SettingsContentBackground,
            textColor = com.dawitf.akahidegn.ui.theme.SettingsContentText,
            modifier = Modifier.weight(1f),
            content = mainContent
        )
    }
}

@Composable
fun ActiveGroupsTabLayout(
    modifier: Modifier = Modifier,
    headerContent: @Composable BoxScope.() -> Unit,
    mainContent: @Composable BoxScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        FrostedGlassTabHeader(
            topColor = com.dawitf.akahidegn.ui.theme.ActiveGroupsGradientTop,
            bottomColor = com.dawitf.akahidegn.ui.theme.ActiveGroupsGradientBottom,
            content = headerContent
        )
        
        TabContentArea(
            backgroundColor = com.dawitf.akahidegn.ui.theme.ActiveGroupsContentBackground,
            textColor = com.dawitf.akahidegn.ui.theme.ActiveGroupsContentText,
            modifier = Modifier.weight(1f),
            content = mainContent
        )
    }
}
