@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.dawitf.akahidegn.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.R
import android.content.Intent
import android.net.Uri
import com.dawitf.akahidegn.ui.animation.shared.SharedElement

data class GroupMember(
    val id: String,
    val name: String,
    val phone: String,
    val avatar: String? = null,
    val isCreator: Boolean = false
)

@Composable
fun GroupMembersDialog(
    group: Group,
    members: List<GroupMember>,
    currentUserId: String,
    onDismiss: () -> Unit,
    onLeaveGroup: (groupId: String, userId: String) -> Unit,
    onJoinGroup: ((groupId: String, userId: String, userName: String) -> Unit)? = null // New join functionality
) {
    val context = LocalContext.current
    
    // Check if current user has joined this group
    val userHasJoined = group.members.containsKey(currentUserId)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Group Members",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Group destination with shared element from the list card
                val sharedKey = remember(group.groupId) { "groupCard-${group.groupId ?: group.destinationName}" }
                SharedElement(key = sharedKey) { sharedMod ->
                    Text(
                        text = "Destination: ${group.destinationName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = sharedMod.padding(vertical = 8.dp)
                    )
                }
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
                // Members count
                Text(
                    text = "${members.size} member${if (members.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Members list
                if (members.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f), // Use weight instead of fillMaxSize to leave space for buttons
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No members yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Use weight instead of fillMaxSize to leave space for buttons
                    ) {
                        items(members) { member ->
                            MemberCard(
                                member = member,
                                showPhoneNumber = userHasJoined,
                                onPhoneClick = { phoneNumber ->
                                    // Create intent to dial phone number
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:$phoneNumber")
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
                
                // Action buttons
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!userHasJoined) {
                        // Join Group Button (only show if user hasn't joined and callback is provided)
                        onJoinGroup?.let { joinCallback ->
                            Button(
                                onClick = { 
                                    joinCallback(group.groupId!!, currentUserId, "User") // TODO: Get actual user name
                                    onDismiss() // Close dialog after joining
                                },
                                modifier = Modifier.weight(1f),
                                enabled = group.memberCount < group.maxMembers
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Join Group")
                            }
                        }
                    } else {
                        // Leave Group Button (only show if user has joined)
                        OutlinedButton(
                            onClick = { 
                                onLeaveGroup(group.groupId!!, currentUserId)
                                onDismiss() // Close dialog after leaving
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Leave Group")
                        }
                    }
                    
                    // Dismiss Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberCard(
    member: GroupMember,
    showPhoneNumber: Boolean = false,
    onPhoneClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (member.isCreator) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                val avatarRes = getAvatarResource(member.avatar)
                if (avatarRes != null) {
                    Image(
                        painter = painterResource(id = avatarRes),
                        contentDescription = "User avatar",
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    // Fallback icon
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Default avatar",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Member info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (member.isCreator) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Group creator",
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFFFFD700) // Gold color for star
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (showPhoneNumber) member.phone else "***-***-****",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (member.isCreator) {
                    Text(
                        text = "Group Creator",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Phone button (only show if user has joined)
            if (showPhoneNumber) {
                IconButton(
                    onClick = { onPhoneClick(member.phone) },
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Call ${member.name}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                // Show placeholder for locked phone access
                IconButton(
                    onClick = { /* Do nothing - phone locked */ },
                    enabled = false,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Join group to see phone",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

private fun getAvatarResource(avatarId: String?): Int? {
    return when (avatarId) {
        "avatar_1" -> R.drawable.user_avatar_1
        "avatar_2" -> R.drawable.user_avatar_2
        "avatar_3" -> R.drawable.user_avatar_3
        "avatar_4" -> R.drawable.user_avatar_4
        "avatar_5" -> R.drawable.user_avatar_5
        "avatar_6" -> R.drawable.default_avatar  // For anonymous users
        null -> R.drawable.default_avatar        // Fallback for null avatars
        else -> R.drawable.default_avatar        // Fallback for unknown avatars
    }
}
