package com.dawitf.akahidegn.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.location.Location
import com.dawitf.akahidegn.Group
import kotlin.math.*

@Composable
fun GroupsMapView(
    groups: List<Group>,
    userLocation: Location?,
    onGroupClick: (Group) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = "📍 Nearby Groups",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopStart)
            )

            // Map area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp)
            ) {
                if (userLocation != null && groups.isNotEmpty()) {
                    GroupsMap(
                        groups = groups,
                        userLocation = userLocation,
                        onGroupClick = onGroupClick,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (userLocation == null) "Location not available" else "No groups nearby",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupsMap(
    groups: List<Group>,
    userLocation: Location,
    onGroupClick: (Group) -> Unit,
    modifier: Modifier = Modifier
) {
    val mapCenter = Offset(150f, 150f) // Center of the map view

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f

        // Draw background grid
        drawGrid(centerX, centerY)

        // Draw user location pin (center)
        drawUserPin(centerX, centerY)

        // Draw group pins around user
        groups.forEachIndexed { index, group ->
            val position = calculateGroupPosition(
                userLocation = userLocation,
                group = group,
                centerX = centerX,
                centerY = centerY,
                index = index,
                totalGroups = groups.size
            )

            drawGroupPin(
                position = position,
                group = group,
                onGroupClick = onGroupClick
            )
        }
    }
}

private fun DrawScope.drawGrid(centerX: Float, centerY: Float) {
    val gridColor = Color.Gray.copy(alpha = 0.2f)
    val gridSize = 30f

    // Draw horizontal lines
    for (i in -5..5) {
        val y = centerY + i * gridSize
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
    }

    // Draw vertical lines
    for (i in -5..5) {
        val x = centerX + i * gridSize
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawUserPin(centerX: Float, centerY: Float) {
    // Draw user pin (blue circle)
    drawCircle(
        color = Color.Blue,
        radius = 12f,
        center = Offset(centerX, centerY)
    )

    // Draw inner circle
    drawCircle(
        color = Color.White,
        radius = 8f,
        center = Offset(centerX, centerY)
    )

    // Draw user icon (simplified)
    drawCircle(
        color = Color.Blue,
        radius = 4f,
        center = Offset(centerX, centerY)
    )
}

private fun DrawScope.drawGroupPin(
    position: Offset,
    group: Group,
    onGroupClick: (Group) -> Unit
) {
    // Draw group pin background
    drawCircle(
        color = Color.Red,
        radius = 10f,
        center = position
    )

    // Draw inner circle
    drawCircle(
        color = Color.White,
        radius = 7f,
        center = position
    )

    // Draw group indicator
    drawCircle(
        color = Color.Red,
        radius = 3f,
        center = position
    )
}

private fun calculateGroupPosition(
    userLocation: Location,
    group: Group,
    centerX: Float,
    centerY: Float,
    index: Int,
    totalGroups: Int
): Offset {
    // If group has real coordinates, use them
    if (group.pickupLat != null && group.pickupLng != null) {
        val deltaLat = group.pickupLat!! - userLocation.latitude
        val deltaLng = group.pickupLng!! - userLocation.longitude

        // Convert lat/lng difference to screen coordinates
        val scale = 1000f // Adjust this to control zoom level
        val x = centerX + (deltaLng * scale).toFloat()
        val y = centerY - (deltaLat * scale).toFloat() // Negative because screen Y is inverted

        return Offset(x, y)
    } else {
        // If no coordinates, arrange groups in a circle around user
        val radius = 80f
        val angle = (2 * PI * index / totalGroups).toFloat()
        val x = centerX + radius * cos(angle)
        val y = centerY + radius * sin(angle)

        return Offset(x, y)
    }
}

@Composable
fun GroupPinOverlay(
    groups: List<Group>,
    userLocation: Location?,
    onGroupClick: (Group) -> Unit,
    modifier: Modifier = Modifier
) {
    if (userLocation == null || groups.isEmpty()) return

    Box(modifier = modifier.fillMaxSize()) {
        groups.forEachIndexed { index, group ->
            val position = remember(group, userLocation) {
                calculatePinPosition(userLocation, group, index, groups.size)
            }

            GroupPinItem(
                group = group,
                position = position,
                onClick = { onGroupClick(group) },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun GroupPinItem(
    group: Group,
    position: Offset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .offset(
                x = position.x.dp,
                y = position.y.dp
            )
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Destination name above pin
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = group.destinationName ?: "Unknown",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Pin icon
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Group location",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Member count
        Text(
            text = "${group.memberCount}/${group.maxMembers}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

private fun calculatePinPosition(
    userLocation: Location,
    group: Group,
    index: Int,
    totalGroups: Int
): Offset {
    // If group has real coordinates, use them
    if (group.pickupLat != null && group.pickupLng != null) {
        val deltaLat = group.pickupLat!! - userLocation.latitude
        val deltaLng = group.pickupLng!! - userLocation.longitude

        // Convert to screen coordinates (simplified)
        val scale = 100f // Adjust for desired zoom level
        val x = (deltaLng * scale).toFloat()
        val y = -(deltaLat * scale).toFloat() // Negative because screen Y is inverted

        return Offset(x, y)
    } else {
        // Arrange in circle if no coordinates
        val radius = 80f
        val angle = (2 * PI * index / totalGroups).toFloat()
        val x = radius * cos(angle)
        val y = radius * sin(angle)

        return Offset(x, y)
    }
}
