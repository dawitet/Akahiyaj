package com.dawitf.akahidegn.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dawitf.akahidegn.ui.components.EmptyStateComponent
import java.text.SimpleDateFormat
import java.util.*

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val type: NotificationType,
    val isRead: Boolean = false
)

enum class NotificationType(val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    GROUP_JOINED(Icons.Default.Group),
    GROUP_LEFT(Icons.Default.ExitToApp),
    CHAT_MESSAGE(Icons.Default.Message),
    SYSTEM(Icons.Default.Settings),
    RIDE_UPDATE(Icons.Default.DirectionsCar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit
) {
    // Sample notifications - in a real app, this would come from a repository/ViewModel
    var notifications by remember { 
        mutableStateOf(
            listOf(
                NotificationItem(
                    id = "1",
                    title = "New message in group",
                    message = "Someone sent a message in 'To Bole' group",
                    timestamp = System.currentTimeMillis() - 300000, // 5 minutes ago
                    type = NotificationType.CHAT_MESSAGE,
                    isRead = false
                ),
                NotificationItem(
                    id = "2", 
                    title = "Member joined your group",
                    message = "A new member joined 'To CMC' group",
                    timestamp = System.currentTimeMillis() - 3600000, // 1 hour ago
                    type = NotificationType.GROUP_JOINED,
                    isRead = true
                ),
                NotificationItem(
                    id = "3",
                    title = "Ride starting soon",
                    message = "Your ride to Piassa starts in 15 minutes",
                    timestamp = System.currentTimeMillis() - 7200000, // 2 hours ago
                    type = NotificationType.RIDE_UPDATE,
                    isRead = true
                )
            )
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Mark all as read
                            notifications = notifications.map { it.copy(isRead = true) }
                        }
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = "Mark all as read")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateComponent(
                    title = "No Notifications",
                    subtitle = "You're all caught up!\nNotifications about your rides and groups will appear here.",
                    icon = Icons.Default.Notifications
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationCard(
                        notification = notification,
                        onMarkAsRead = { notificationId ->
                            notifications = notifications.map { 
                                if (it.id == notificationId) it.copy(isRead = true) else it 
                            }
                        },
                        onDismiss = { notificationId ->
                            notifications = notifications.filter { it.id != notificationId }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCard(
    notification: NotificationItem,
    onMarkAsRead: (String) -> Unit,
    onDismiss: (String) -> Unit
) {
    val cardColors = if (notification.isRead) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = notification.type.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold
                        )
                        
                        Text(
                            text = notification.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = formatTimeAgo(notification.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Action buttons
                Row {
                    if (!notification.isRead) {
                        IconButton(
                            onClick = { onMarkAsRead(notification.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Done,
                                contentDescription = "Mark as read",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = { onDismiss(notification.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}