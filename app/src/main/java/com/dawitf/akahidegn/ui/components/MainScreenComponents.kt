package com.dawitf.akahidegn.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dawitf.akahidegn.Group // Import your Group data class
import com.dawitf.akahidegn.R     // Import your R file
// Enhanced UI Components
import com.dawitf.akahidegn.ui.components.GlassmorphismCard
import com.dawitf.akahidegn.ui.components.BouncyButton
import com.dawitf.akahidegn.ui.components.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideGroupCard(group: Group, onClick: () -> Unit) {
    val context = LocalContext.current
    // Cache the image request to prevent recreation on recomposition
    val imageRequest = remember(group.imageUrl) {
        ImageRequest.Builder(context)
            .data(group.imageUrl ?: R.drawable.ic_default_group_image)
            .crossfade(true)
            .size(300, 200) // Set specific size to optimize loading
            .build()
    }
    
    // Cache the destination name to prevent unnecessary string operations
    val destinationText = remember(group.destinationName) {
        group.destinationName ?: "Unknown Destination"
    }
    
    // Cache the spots available text
    val spotsText = remember(group.memberCount, group.maxMembers) {
        "${group.memberCount}/${group.maxMembers} spots"
    }

    Card(
        onClick = onClick,
        modifier = Modifier.width(140.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular image with overlapping user avatars
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageRequest,
                    placeholder = painterResource(R.drawable.ic_default_group_image),
                    error = painterResource(R.drawable.ic_default_group_image),
                    contentDescription = destinationText,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(
                            BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            CircleShape
                        )
                )
                
                // User avatars positioned around the circle
                UserAvatarsOverlay(
                    currentMembers = group.memberCount,
                    maxMembers = group.maxMembers
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = destinationText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = spotsText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
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