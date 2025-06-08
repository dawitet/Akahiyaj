package com.dawitf.akahidegn.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dawitf.akahidegn.Group // Import your Group data class
import com.dawitf.akahidegn.R     // Import your R file
// Enhanced UI Components
import com.dawitf.akahidegn.ui.components.glassCard
import com.dawitf.akahidegn.ui.components.gradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideGroupCard(group: Group, onClick: () -> Unit) {
    val context = LocalContext.current
    
    // Cache computed values
    val destinationText = remember(group.destinationName) {
        group.destinationName ?: "Unknown Destination"
    }
    
    val spotsText = remember(group.memberCount, group.maxMembers) {
        "${group.memberCount}/${group.maxMembers}"
    }
    
    // Calculate capacity percentage for progress indicator
    val capacityPercentage = remember(group.memberCount, group.maxMembers) {
        if (group.maxMembers > 0) group.memberCount.toFloat() / group.maxMembers.toFloat() else 0f
    }
    
    // Determine status and colors
    val (statusText, statusColor, isAlmostFull) = remember(capacityPercentage) {
        when {
            capacityPercentage >= 1.0f -> Triple("ሙሉ", Color.Red, true)
            capacityPercentage >= 0.8f -> Triple("እየሞላ", Color(0xFFFF9800), true)
            capacityPercentage >= 0.5f -> Triple("ሴቱ", Color.Green, false)
            else -> Triple("ክፍት", Color.Blue, false)
        }
    }
    
    // Animation for pulsing effect when almost full
    val infiniteTransition = rememberInfiniteTransition(label = "card_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isAlmostFull) 0.3f else 0f,
        targetValue = if (isAlmostFull) 0.7f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(160.dp)
            .height(200.dp)
            .glassCard(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background gradient for status indication
            if (isAlmostFull) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    statusColor.copy(alpha = pulseAlpha * 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header section with status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusColor.copy(alpha = 0.2f),
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    // Capacity indicator icon
                    Icon(
                        imageVector = when {
                            capacityPercentage >= 1.0f -> Icons.Default.Block
                            capacityPercentage >= 0.8f -> Icons.Default.Warning
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = "Status",
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Avatar with enhanced styling
                Box(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    LazyImageLoader(
                        imageUrl = group.imageUrl,
                        fallbackAvatarId = group.id,
                        isGroupAvatar = true,
                        contentDescription = "አንድ ሰው",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .border(
                                width = 3.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                    
                    // Online indicator
                    Surface(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd),
                        shape = CircleShape,
                        color = Color.Green,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                    ) {}
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Destination with enhanced typography
                Text(
                    text = destinationText,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Capacity progress bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = spotsText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(capacityPercentage * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = statusColor
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Animated progress bar
                    LinearProgressIndicator(
                        progress = { capacityPercentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
fun UserAvatarsOverlay(currentMembers: Int, maxMembers: Int) {
    val avatarSize = 16.dp
    val circleRadius = 45.dp
    
    // Calculate positions for user avatars around the circle
    val positions = remember(currentMembers) {
        (0 until minOf(currentMembers, 4)).map { index ->
            val angle = (index * 90.0) * (kotlin.math.PI / 180.0) // 90 degrees apart
            val x = (circleRadius.value * kotlin.math.cos(angle)).dp
            val y = (circleRadius.value * kotlin.math.sin(angle)).dp
            Pair(x, y)
        }
    }
    
    positions.forEach { (x, y) ->
        Box(
            modifier = Modifier
                .offset(x = x, y = y)
                .size(avatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
                    CircleShape
                )
        )
    }
    
    // Show "+X" indicator if there are more than 4 members
    if (currentMembers > 4) {
        Box(
            modifier = Modifier
                .offset(x = circleRadius, y = (-circleRadius))
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+${currentMembers - 4}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun UserCapacityIndicator(currentMembers: Int, maxMembers: Int, modifier: Modifier = Modifier) {
    // Cache the display max members and indicator items to prevent recreation
    val indicatorItems = remember(currentMembers, maxMembers) {
        val displayMaxMembers = maxMembers.coerceAtMost(8)
        (1..displayMaxMembers).map { i ->
            IndicatorItem(
                isFilled = i <= currentMembers,
                index = i
            )
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        indicatorItems.forEach { item ->
            IndicatorDot(isFilled = item.isFilled)
        }
    }
}

@Composable
private fun IndicatorDot(isFilled: Boolean) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(
                if (isFilled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (isFilled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                CircleShape
            )
    )
}

private data class IndicatorItem(
    val isFilled: Boolean,
    val index: Int
)