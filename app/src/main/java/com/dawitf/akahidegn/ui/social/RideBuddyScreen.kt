package com.dawitf.akahidegn.ui.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dawitf.akahidegn.R
import com.dawitf.akahidegn.features.social.RideBuddyService.*
import java.text.SimpleDateFormat
import java.util.*
// Enhanced UI Components
import com.dawitf.akahidegn.ui.components.SwipeToActionCard
import com.dawitf.akahidegn.ui.components.SwipeAction
import com.dawitf.akahidegn.ui.components.LongPressContextMenu
import com.dawitf.akahidegn.ui.components.ContextMenuItem
import com.dawitf.akahidegn.ui.components.InteractiveStatusCard
import com.dawitf.akahidegn.ui.components.ShimmerGroupList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideBuddyScreen(
    onNavigateBack: () -> Unit,
    viewModel: RideBuddyViewModel = viewModel()
) {
    val buddies by viewModel.rideBuddies.collectAsState()
    val pendingInvitations by viewModel.pendingInvitations.collectAsState()
    val sentInvitations by viewModel.sentInvitations.collectAsState()
    val regularGroups by viewModel.regularGroups.collectAsState()
    val buddyStats by viewModel.buddyStats.collectAsState()
    val suggestions by viewModel.buddySuggestions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    
    val tabs = listOf("Buddies", "Invitations", "Groups", "Suggestions")
    
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Ride Buddies",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showInviteDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Invite Buddy")
                }
                IconButton(onClick = { showCreateGroupDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create Group")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        
        // Stats Card
        BuddyStatsCard(
            stats = buddyStats,
            modifier = Modifier.padding(16.dp)
        )
        
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
        
        // Content based on selected tab
        if (isLoading) {
            ShimmerGroupList(itemCount = 5)
        } else {
            when (selectedTab) {
                0 -> BuddiesTab(
                    buddies = buddies,
                    onRemoveBuddy = { viewModel.removeBuddy(it) },
                    onRateBuddy = { buddyId, rating -> viewModel.rateBuddy(buddyId, rating) }
                )
                1 -> InvitationsTab(
                    pendingInvitations = pendingInvitations,
                    sentInvitations = sentInvitations,
                    onRespondToInvitation = { invitationId, accept ->
                        viewModel.respondToInvitation(invitationId, accept)
                    }
                )
                2 -> RegularGroupsTab(
                    regularGroups = regularGroups,
                    onLeaveGroup = { viewModel.leaveRegularGroup(it) }
                )
                3 -> SuggestionsTab(
                    suggestions = suggestions,
                    onInviteBuddy = { suggestion ->
                        viewModel.sendInvitation(
                            suggestion.userId,
                            suggestion.displayName,
                            "Let's become ride buddies! We've ridden together ${suggestion.ridesTogether} times."
                        )
                    }
                )
            }
        }
    }
    
    // Dialogs
    if (showInviteDialog) {
        InviteBuddyDialog(
            onDismiss = { showInviteDialog = false },
            onInvite = { userId, userName, message ->
                viewModel.sendInvitation(userId, userName, message)
                showInviteDialog = false
            }
        )
    }
    
    if (showCreateGroupDialog) {
        CreateRegularGroupDialog(
            buddies = buddies,
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { groupName, selectedBuddies, route, times, description ->
                viewModel.createRegularGroup(groupName, selectedBuddies, route, times, description)
                showCreateGroupDialog = false
            }
        )
    }
}

@Composable
private fun BuddyStatsCard(
    stats: BuddyStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Your Buddy Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Buddies",
                    value = stats.totalBuddies.toString(),
                    icon = Icons.Default.Person
                )
                StatItem(
                    label = "Rides Together",
                    value = stats.totalRidesWithBuddies.toString(),
                    icon = Icons.Default.LocationOn
                )
                StatItem(
                    label = "Avg Rating",
                    value = if (stats.averageBuddyRating > 0) "%.1f".format(stats.averageBuddyRating) else "N/A",
                    icon = Icons.Default.Star
                )
                StatItem(
                    label = "Groups",
                    value = stats.regularGroupsCount.toString(),
                    icon = Icons.Default.Person
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun BuddiesTab(
    buddies: List<RideBuddy>,
    onRemoveBuddy: (String) -> Unit,
    onRateBuddy: (String, Float) -> Unit
) {
    if (buddies.isEmpty()) {
        EmptyState(
            message = "No ride buddies yet",
            description = "Invite people you frequently ride with to become your buddies!"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(buddies) { buddy ->
                BuddyCard(
                    buddy = buddy,
                    onRemove = { onRemoveBuddy(buddy.userId) },
                    onRate = { rating -> onRateBuddy(buddy.userId, rating) }
                )
            }
        }
    }
}

@Composable
private fun BuddyCard(
    buddy: RideBuddy,
    onRemove: () -> Unit,
    onRate: (Float) -> Unit
) {
    var showRatingDialog by remember { mutableStateOf(false) }
    
    SwipeToActionCard(
        onLeftSwipe = onRemove,
        onRightSwipe = { showRatingDialog = true },
        leftAction = SwipeAction(
            icon = Icons.Default.Delete,
            backgroundColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            text = "Remove"
        ),
        rightAction = SwipeAction(
            icon = Icons.Default.Star,
            backgroundColor = Color(0xFFFFD700),
            contentColor = Color.Black,
            text = "Rate"
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(buddy.profileImageUrl ?: "https://via.placeholder.com/64")
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Buddy Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = buddy.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "${buddy.totalRidesTogether} rides together",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                if (buddy.averageRating > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%.1f".format(buddy.averageRating),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                if (buddy.lastRideDate > 0) {
                    val lastRideText = formatLastRideDate(buddy.lastRideDate)
                    Text(
                        text = "Last ride: $lastRideText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            
            // Enhanced context menu with long press
            LongPressContextMenu(
                items = listOf(
                    ContextMenuItem(
                        title = "Rate Buddy",
                        icon = Icons.Default.Star,
                        onClick = { showRatingDialog = true }
                    ),
                    ContextMenuItem(
                        title = "View Profile",
                        icon = Icons.Default.Person,
                        onClick = { /* Navigate to profile */ }
                    ),
                    ContextMenuItem(
                        title = "Remove Buddy",
                        icon = Icons.Default.Delete,
                        color = MaterialTheme.colorScheme.error,
                        onClick = onRemove
                    )
                )
            ) {
                // Context menu trigger content
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
        }
    }
    
    if (showRatingDialog) {
        RatingDialog(
            buddyName = buddy.displayName,
            currentRating = buddy.averageRating,
            onDismiss = { showRatingDialog = false },
            onRate = { rating ->
                onRate(rating)
                showRatingDialog = false
            }
        )
    }
}

@Composable
private fun InvitationsTab(
    pendingInvitations: List<RideBuddyInvitation>,
    sentInvitations: List<RideBuddyInvitation>,
    onRespondToInvitation: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (pendingInvitations.isNotEmpty()) {
            item {
                Text(
                    text = "Pending Invitations (${pendingInvitations.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(pendingInvitations) { invitation ->
                InvitationCard(
                    invitation = invitation,
                    isPending = true,
                    onAccept = { onRespondToInvitation(invitation.invitationId, true) },
                    onDecline = { onRespondToInvitation(invitation.invitationId, false) }
                )
            }
        }
        
        if (sentInvitations.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sent Invitations (${sentInvitations.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(sentInvitations) { invitation ->
                InvitationCard(
                    invitation = invitation,
                    isPending = false
                )
            }
        }
        
        if (pendingInvitations.isEmpty() && sentInvitations.isEmpty()) {
            item {
                EmptyState(
                    message = "No invitations",
                    description = "Send invitations to people you'd like to ride with regularly!"
                )
            }
        }
    }
}

@Composable
private fun InvitationCard(
    invitation: RideBuddyInvitation,
    isPending: Boolean,
    onAccept: (() -> Unit)? = null,
    onDecline: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPending) MaterialTheme.colorScheme.secondaryContainer 
                           else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPending) "From: ${invitation.fromUserName}" 
                               else "To: ${invitation.toUserName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = formatInvitationDate(invitation.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                StatusChip(status = invitation.status)
            }
            
            if (invitation.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = invitation.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
            
            if (isPending && invitation.status == InvitationStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onDecline?.invoke() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Decline")
                    }
                    
                    Button(
                        onClick = { onAccept?.invoke() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Accept")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: InvitationStatus) {
    val (color, text) = when (status) {
        InvitationStatus.PENDING -> MaterialTheme.colorScheme.warning to "Pending"
        InvitationStatus.ACCEPTED -> MaterialTheme.colorScheme.primary to "Accepted"
        InvitationStatus.DECLINED -> MaterialTheme.colorScheme.error to "Declined"
        InvitationStatus.EXPIRED -> MaterialTheme.colorScheme.outline to "Expired"
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RegularGroupsTab(
    regularGroups: List<RegularGroup>,
    onLeaveGroup: (String) -> Unit
) {
    if (regularGroups.isEmpty()) {
        EmptyState(
            message = "No regular groups",
            description = "Create groups with your frequent ride partners for consistent commuting!"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(regularGroups) { group ->
                RegularGroupCard(
                    group = group,
                    onLeave = { onLeaveGroup(group.groupId) }
                )
            }
        }
    }
}

@Composable
private fun RegularGroupCard(
    group: RegularGroup,
    onLeave: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.groupName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = group.commonRoute,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Text(
                        text = "${group.members.size} members • ${group.totalTrips} trips",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Leave Group") },
                            onClick = {
                                showMenu = false
                                onLeave()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ExitToApp, contentDescription = null)
                            }
                        )
                    }
                }
            }
            
            if (group.preferredTimes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Preferred times: ${group.preferredTimes.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            if (group.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = group.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun SuggestionsTab(
    suggestions: List<RideBuddySuggestion>,
    onInviteBuddy: (RideBuddySuggestion) -> Unit
) {
    if (suggestions.isEmpty()) {
        EmptyState(
            message = "No suggestions",
            description = "Start riding with people to get buddy suggestions based on your ride history!"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestions) { suggestion ->
                SuggestionCard(
                    suggestion = suggestion,
                    onInvite = { onInviteBuddy(suggestion) }
                )
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: RideBuddySuggestion,
    onInvite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestion.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = suggestion.suggestionReason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
                
                // Confidence indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = if (index < (suggestion.confidence * 5).toInt()) Icons.Default.Star else Icons.Outlined.Star,
                            contentDescription = null,
                            tint = if (index < (suggestion.confidence * 5).toInt()) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Match: ${(suggestion.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
            
            Button(
                onClick = onInvite,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Invite")
            }
        }
    }
}

@Composable
private fun EmptyState(
    message: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

// Helper dialogs and functions
@Composable
private fun InviteBuddyDialog(
    onDismiss: () -> Unit,
    onInvite: (String, String, String) -> Unit
) {
    var userId by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite Ride Buddy") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("User ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("User Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (userId.isNotBlank() && userName.isNotBlank()) {
                        onInvite(userId, userName, message)
                    }
                },
                enabled = userId.isNotBlank() && userName.isNotBlank()
            ) {
                Text("Send Invitation")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CreateRegularGroupDialog(
    buddies: List<RideBuddy>,
    onDismiss: () -> Unit,
    onCreate: (String, List<String>, String, List<String>, String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var route by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedBuddies by remember { mutableStateOf(setOf<String>()) }
    var timeInput by remember { mutableStateOf("") }
    var preferredTimes by remember { mutableStateOf(listOf<String>()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Regular Group") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = route,
                        onValueChange = { route = it },
                        label = { Text("Common Route") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    Text("Select Buddies:", style = MaterialTheme.typography.titleSmall)
                }
                
                items(buddies) { buddy ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = buddy.userId in selectedBuddies,
                            onCheckedChange = { checked ->
                                selectedBuddies = if (checked) {
                                    selectedBuddies + buddy.userId
                                } else {
                                    selectedBuddies - buddy.userId
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(buddy.displayName)
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank() && route.isNotBlank() && selectedBuddies.isNotEmpty()) {
                        onCreate(groupName, selectedBuddies.toList(), route, preferredTimes, description)
                    }
                },
                enabled = groupName.isNotBlank() && route.isNotBlank() && selectedBuddies.isNotEmpty()
            ) {
                Text("Create Group")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RatingDialog(
    buddyName: String,
    currentRating: Float,
    onDismiss: () -> Unit,
    onRate: (Float) -> Unit
) {
    var rating by remember { mutableStateOf(currentRating.takeIf { it > 0 } ?: 5f) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate $buddyName") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("How was your ride experience with $buddyName?")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(5) { index ->
                        IconButton(
                            onClick = { rating = (index + 1).toFloat() }
                        ) {
                            Icon(
                                imageVector = if (index < rating.toInt()) Icons.Default.Star else Icons.Outlined.Star,
                                contentDescription = null,
                                tint = if (index < rating.toInt()) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                
                Text(
                    text = when (rating.toInt()) {
                        1 -> "Poor"
                        2 -> "Fair"
                        3 -> "Good"
                        4 -> "Very Good"
                        5 -> "Excellent"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onRate(rating) }) {
                Text("Rate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Utility functions
private fun formatLastRideDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val days = diff / (24 * 60 * 60 * 1000)
    
    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 7 -> "$days days ago"
        days < 30 -> "${days / 7} weeks ago"
        else -> "${days / 30} months ago"
    }
}

private fun formatInvitationDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

// Color extension for warning color
private val ColorScheme.warning: Color
    get() = Color(0xFFFF9800)
