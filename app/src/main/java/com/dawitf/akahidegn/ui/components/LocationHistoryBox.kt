package com.dawitf.akahidegn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dawitf.akahidegn.Group
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationHistoryBox(
    userCreatedGroups: List<Group> = emptyList(),
    userJoinedGroups: List<Group> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Combine and sort by recency
    val allUserGroups = (userCreatedGroups + userJoinedGroups)
        .distinctBy { it.groupId }
        .sortedByDescending { it.timestamp ?: 0 }
        .take(5) // Show only last 5
    
    // Charcoal black styling
    val charcoalBlack = Color(0xFF2C2C2C)
    val charcoalLight = Color(0xFF3A3A3A)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp), // Fixed height for the box
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = charcoalBlack
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = "Location History",
                        tint = Color(0xFFFFD700), // Golden yellow
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "የቦታ ታሪክ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Text(
                    text = "${allUserGroups.size} ቡድኖች",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // History list or empty state
            if (allUserGroups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ምንም የቦታ ታሪክ የለም",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "ቡድን ይፍጠሩ ወይም ይቀላቀሉ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(allUserGroups) { group ->
                        LocationHistoryCard(
                            group = group,
                            isCreated = userCreatedGroups.any { it.groupId == group.groupId }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationHistoryCard(
    group: Group,
    isCreated: Boolean
) {
    val charcoalLight = Color(0xFF3A3A3A)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = charcoalLight
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Destination info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = group.originalDestination ?: group.destinationName ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatTime(group.timestamp ?: 0),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
            
            // Action indicator
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isCreated) Color(0xFF4CAF50) else Color(0xFF2196F3)
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = if (isCreated) "ፈጠርኩ" else "ተቀላቀልኩ",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    return try {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        when {
            diff < 60 * 1000 -> "አሁን"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}ደ በፊት"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}ሰ በፊት"
            else -> {
                val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
                formatter.format(Date(timestamp))
            }
        }
    } catch (e: Exception) {
        "?"
    }
}
