package com.dawitf.akahidegn.ui.profile.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedProfileCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = tween(300)
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300)
    )

    LaunchedEffect(Unit) {
        visible = true
    }

    Card(
        modifier = modifier
            .scale(scale)
            .alpha(alpha),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        content()
    }
}

@Composable
fun AnimatedStatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(300)
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300)
    )

    LaunchedEffect(Unit) {
        visible = true
    }

    Row(
        modifier = modifier
            .offset(y = offsetY.dp)
            .alpha(alpha)
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AnimatedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (visible) progress else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing)
    )

    LaunchedEffect(Unit) {
        visible = true
    }

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
    )
}

@Composable
fun AnimatedAchievementIcon(
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    LaunchedEffect(Unit) {
        visible = true
    }

    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = if (isUnlocked) "Unlocked" else "Locked",
        tint = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        modifier = modifier.scale(scale)
    )
}
