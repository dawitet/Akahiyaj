package com.dawitf.akahidegn.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dawitf.akahidegn.domain.model.Group
import java.text.SimpleDateFormat
import java.util.*

/**
 * Bookmark and Recent Activity Components
 * Provides functionality to save favorite groups and track user activity
 */

data class BookmarkedGroup(
    val group: Group,
    val bookmarkedAt: Long = System.currentTimeMillis(),
    val notes: String? = null
)

data class RecentActivity(
    val id: String,
    val type: ActivityType,
    val groupId: String,
    val groupName: String,
    val timestamp: Long,
    val details: String? = null
)

enum class ActivityType(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    JOINED_GROUP("ቡድን ገብተዋል", Icons.Default.Person),
    LEFT_GROUP("ቡድን ወጥተዋል", Icons.Default.PersonOff),
    CREATED_GROUP("ቡድን ፈጥረዋል", Icons.Default.Add),
    BOOKMARKED("ቡድን አስቀምጠዋል", Icons.Default.Star),
    SEARCHED("ፈልገዋል", Icons.Default.Search),
    VIEWED_GROUP("ቡድን ተመልክተዋል", Icons.Default.Visibility)
}

@Composable
fun BookmarkButton(
    isBookmarked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    val scale by animateFloatAsState(
        targetValue = if (isBookmarked) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "bookmark_scale"
    )
    
    val color by animateColorAsState(
        targetValue = if (isBookmarked) MaterialTheme.colorScheme.primary 
                     else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "bookmark_color"
    )
    
    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggle()
        },
        modifier = modifier.scale(scale)
    ) {
        Icon(
            imageVector = if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
            contentDescription = if (isBookmarked) "ከመዝገብ አውጣ" else "በመዝገብ አስቀምጥ",
            tint = color
        )
    }
}

@Composable
fun BookmarkedGroupsList(
    bookmarkedGroups: List<BookmarkedGroup>,
    onGroupClick: (Group) -> Unit,
    onRemoveBookmark: (BookmarkedGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    if (bookmarkedGroups.isEmpty()) {
        EmptyBookmarksState(modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(
                items = bookmarkedGroups,
                key = { it.group.id }
            ) { bookmarkedGroup ->
                BookmarkedGroupCard(
                    bookmarkedGroup = bookmarkedGroup,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onGroupClick(bookmarkedGroup.group)
                    },
                    onRemoveBookmark = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRemoveBookmark(bookmarkedGroup)
                    }
                )
            }
        }
    }
}

@Composable
fun BookmarkedGroupCard(
    bookmarkedGroup: BookmarkedGroup,
    onClick: () -> Unit,
    onRemoveBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val group = bookmarkedGroup.group
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "ወደ ${group.destination}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "መነሻ: ${group.departureTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                BookmarkButton(
                    isBookmarked = true,
                    onToggle = onRemoveBookmark
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${group.currentMembers}/${group.maxMembers}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Text(
                    text = "ተይዞ: ${dateFormat.format(Date(bookmarkedGroup.bookmarkedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Notes if available
            bookmarkedGroup.notes?.let { notes ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyBookmarksState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(60.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "ምንም የተይዘ ቡድን የለም",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "የሚወዷቸውን ቡድኖች ይያዙ እና በቀላሉ ለመግኘት።",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
        )
    }
}

@Composable
fun RecentActivityList(
    activities: List<RecentActivity>,
    onActivityClick: (RecentActivity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (activities.isEmpty()) {
        EmptyActivityState(modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(
                items = activities,
                key = { it.id }
            ) { activity ->
                RecentActivityItem(
                    activity = activity,
                    onClick = { onActivityClick(activity) }
                )
            }
        }
    }
}

@Composable
fun RecentActivityItem(
    activity: RecentActivity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val timeFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Activity icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = activity.type.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.type.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = activity.groupName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                activity.details?.let { details ->
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Text(
                text = timeFormat.format(Date(activity.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyActivityState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(60.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "ምንም ቅርብ ተግባር የለም",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "ቡድኖችን መፈለግ፣ መግባት እና መፍጠር ሲጀምሩ እዚህ ይታያሉ።",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
        )
    }
}

@Composable
fun ActivityFilterTabs(
    selectedTypes: Set<ActivityType>,
    onTypesChange: (Set<ActivityType>) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ActivityType.values()) { type ->
            FilterChip(
                selected = type in selectedTypes,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val newTypes = if (type in selectedTypes) {
                        selectedTypes - type
                    } else {
                        selectedTypes + type
                    }
                    onTypesChange(newTypes)
                },
                label = {
                    Text(
                        text = type.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = type.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
