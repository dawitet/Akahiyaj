@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.dawitf.akahidegn.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dawitf.akahidegn.Group
import android.location.Location
import kotlin.math.*
import com.dawitf.akahidegn.ui.animation.shared.SharedElement
import com.dawitf.akahidegn.performance.rememberDistanceCalculation
import com.dawitf.akahidegn.performance.rememberTimeFormat
import com.dawitf.akahidegn.performance.rememberDistanceFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCard(
    group: Group,
    userLocation: Location?,
    onClick: () -> Unit,
    onJoinClick: ((Group) -> Unit)? = null, // New parameter for join functionality
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    // Optimized distance calculation with caching
    val distance = rememberDistanceCalculation(
        userLat = userLocation?.latitude,
        userLng = userLocation?.longitude,
        targetLat = group.pickupLat,
        targetLng = group.pickupLng
    )

    // Optimized time formatting with caching
    val timeAgo = rememberTimeFormat(group.timestamp)

    // Optimized distance formatting with caching
    val distanceText = rememberDistanceFormat(distance)

    // Use stable callbacks to prevent recomposition
    val stableOnClick = remember { onClick }
    val stableOnJoinClick = remember { onJoinClick }

    // Use groupId as a stable shared element key
    val sharedKey = remember(group.groupId) { "groupCard-${group.groupId ?: group.destinationName}" }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .glassCard()
            .clickable { stableOnClick() }
            .then(
                if (isPressed) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    )
                } else Modifier
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        SharedElement(key = sharedKey) { sharedMod ->
            Column(
                modifier = sharedMod
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
            // Header row with destination and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.destinationName ?: "Unknown Destination",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Group status indicator
                val isFull = group.memberCount >= group.maxMembers
                val statusColor = if (isFull) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                val statusText = if (isFull) "ሞላ" else "ክፍት"

                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Info row with members, distance, and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Member count
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Members",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${group.memberCount}/${group.maxMembers}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Distance (if available)
                if (distance != null && distanceText.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Distance",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = distanceText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Time ago (always available with fallback)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Time",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeAgo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Creator info
            group.creatorName?.let { creatorName ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Creator",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "በ $creatorName የተፈጠረ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
            }
            }

            // Join button (only show if group is not full and join callback is provided)
            onJoinClick?.let { joinCallback ->
                val isFull = group.memberCount >= group.maxMembers
                if (!isFull) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { joinCallback(group) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ቡድን ተቀላቀል", // "Join Group" in Amharic
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
    }
    }
    }
}

// Helper function to calculate distance between two points
private fun calculateDistance(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val r = 6371000.0 // Earth radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}
