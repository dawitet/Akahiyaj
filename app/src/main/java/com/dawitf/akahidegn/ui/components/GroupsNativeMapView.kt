package com.dawitf.akahidegn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dawitf.akahidegn.Group

@Composable
fun GroupsNativeMapView(
    groups: List<Group>,
    onJoinGroup: (Group) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2C2C2C) // Charcoal grey background
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🗺️ Groups Map View",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700) // Golden yellow
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFD700).copy(alpha = 0.2f) // Golden yellow with transparency
                    )
                ) {
                    Text(
                        text = "${groups.size} ቡድኖች",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFFD700) // Golden yellow
                    )
                }
            }
            
            if (groups.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFFFFD700) // Golden yellow
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No groups nearby",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFFD700) // Golden yellow
                        )
                    }
                }
            } else {
                // Groups list with location info
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groups.take(10)) { group -> // Show first 10 groups
                        GroupLocationCard(
                            group = group,
                            onJoinGroup = { onJoinGroup(group) }
                        )
                    }
                    
                    if (groups.size > 10) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFD700).copy(alpha = 0.1f) // Golden yellow with transparency
                                )
                            ) {
                                Text(
                                    text = "... and ${groups.size - 10} more groups",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFFD700) // Golden yellow
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupLocationCard(
    group: Group,
    onJoinGroup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3C3C3C) // Slightly lighter charcoal for individual cards
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Destination
            Text(
                text = group.originalDestination ?: group.destinationName ?: "Unknown destination",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFFD700) // Golden yellow
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Location info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coordinates
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFFD700) // Golden yellow
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${group.pickupLat?.toString()?.take(6) ?: "?"}, ${group.pickupLng?.toString()?.take(6) ?: "?"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFD700) // Golden yellow
                    )
                }
                
                // Member count
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFFD700) // Golden yellow
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${group.memberCount ?: 1}/${group.maxMembers ?: 4}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFD700) // Golden yellow
                    )
                }
            }
            
            // Join button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                val isFull = (group.memberCount ?: 1) >= (group.maxMembers ?: 4)
                Button(
                    onClick = onJoinGroup,
                    enabled = !isFull,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700), // Golden yellow
                        disabledContainerColor = Color(0xFF666666) // Dark grey for disabled
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isFull) "ሙሉ" else "ተቀላቀል",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2C2C2C) // Dark text on golden button
                    )
                }
            }
        }
    }
}
