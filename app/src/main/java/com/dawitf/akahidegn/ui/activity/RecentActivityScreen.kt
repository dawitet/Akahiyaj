package com.dawitf.akahidegn.ui.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dawitf.akahidegn.ui.components.RecentActivity
import com.dawitf.akahidegn.ui.components.ActivityType
import com.dawitf.akahidegn.ui.components.ShimmerGroupList
import com.dawitf.akahidegn.ui.components.GlassmorphismCard
import com.dawitf.akahidegn.ui.components.BilingualText
import com.dawitf.akahidegn.ui.components.EmptyStateComponent
import com.dawitf.akahidegn.features.bookmark.BookmarkManager
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentActivityScreen(
    onNavigateBack: () -> Unit,
    onClearActivity: () -> Unit = {}
) {
    val context = LocalContext.current
    val recentActivities by BookmarkManager.getRecentActivity(context).collectAsState(initial = emptyList())
    
    var showConfirmClearDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recent Activity") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                    }
                },
                actions = {
                    IconButton(onClick = { showConfirmClearDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all activity")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (recentActivities.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateComponent(
                        title = "No Recent Activity",
                        subtitle = "Your recent interactions and ride activities will appear here.",
                        icon = Icons.Default.History
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentActivities.groupBy { 
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        sdf.format(Date(it.timestamp)) 
                    }.toList()) { (date, activities) ->
                        // Date header
                        Text(
                            text = formatDateHeader(date),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        // Activities for this date
                        activities.forEach { activity ->
                            ActivityItem(activity = activity)
                        }
                    }
                }
            }
        }
        
        // Confirmation dialog for clearing history
        if (showConfirmClearDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmClearDialog = false },
                title = { Text("Clear Activity History") },
                text = { Text("Are you sure you want to clear all your recent activity? This cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmClearDialog = false
                            onClearActivity()
                        }
                    ) {
                        Text("Clear All")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmClearDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ActivityItem(activity: RecentActivity) {
    GlassmorphismCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = activity.type.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                BilingualText(
                    englishText = getActivityTitle(activity),
                    amharicText = activity.type.label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = activity.groupName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatActivityTime(activity.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helper functions
private fun getActivityTitle(activity: RecentActivity): String {
    return when (activity.type) {
        ActivityType.JOINED_GROUP -> "Joined ride group"
        ActivityType.LEFT_GROUP -> "Left ride group"
        ActivityType.CREATED_GROUP -> "Created a new group"
        ActivityType.BOOKMARKED -> "Saved as favorite"
        ActivityType.SEARCHED -> "Searched for"
        ActivityType.VIEWED_GROUP -> "Viewed group details"
    }
}

private fun formatDateHeader(dateStr: String): String {
    val now = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
        Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
    )
    
    return when (dateStr) {
        now -> "Today"
        yesterday -> "Yesterday"
        else -> {
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date!!)
            } catch (e: Exception) {
                dateStr
            }
        }
    }
}

private fun formatActivityTime(timestamp: Long): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
}
