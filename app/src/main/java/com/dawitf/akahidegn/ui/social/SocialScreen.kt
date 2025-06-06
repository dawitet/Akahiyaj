package com.dawitf.akahidegn.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dawitf.akahidegn.features.profile.Friend
import com.dawitf.akahidegn.ui.theme.AkahidegnColors
import java.text.SimpleDateFormat
import java.util.*
// Enhanced UI Components
import com.dawitf.akahidegn.ui.components.AdvancedSearchBar
import com.dawitf.akahidegn.ui.components.FilterChipRow
import com.dawitf.akahidegn.ui.components.FilterOption
import com.dawitf.akahidegn.ui.components.AnimatedPressableCard
import com.dawitf.akahidegn.ui.components.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    onNavigateBack: () -> Unit = {},
    onFriendProfile: (String) -> Unit = {},
    onAddFriend: () -> Unit = {},
    onLeaderboard: () -> Unit = {},
    viewModel: SocialViewModel = hiltViewModel()
) {
    val friends by viewModel.friends.collectAsState()
    val friendRequests by viewModel.friendRequests.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Friends", "Requests", "Discover")
    var searchQuery by remember { mutableStateOf("") }
    var activeFilters by remember { mutableStateOf(setOf<String>()) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { Text("Social") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = onAddFriend) {
                    Icon(Icons.Default.Add, contentDescription = "Add Friend")
                }
                IconButton(onClick = onLeaderboard) {
                    Icon(Icons.Default.Star, contentDescription = "Leaderboard")
                }
            }
        )
        
        // Enhanced search bar
        AdvancedSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { /* Handle search */ },
            placeholder = "Search friends...",
            isLoading = false,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Filter chips for friends
        if (selectedTab == 0) {
            FilterChipRow(
                filters = listOf(
                    FilterOption(id = "online", label = "Online", isSelected = "online" in activeFilters),
                    FilterOption(id = "nearby", label = "Nearby", isSelected = "nearby" in activeFilters),
                    FilterOption(id = "frequent", label = "Frequent Riders", isSelected = "frequent" in activeFilters),
                    FilterOption(id = "recent", label = "Recent", isSelected = "recent" in activeFilters)
                ),
                onFilterToggle = { filter ->
                    activeFilters = if (filter.id in activeFilters) {
                        activeFilters - filter.id
                    } else {
                        activeFilters + filter.id
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        
        // Tab Row
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        // Content based on selected tab
        when (selectedTab) {
            0 -> FriendsTab(
                friends = friends,
                onFriendProfile = onFriendProfile,
                onRemoveFriend = viewModel::removeFriend
            )
            1 -> RequestsTab(
                requests = friendRequests,
                onAcceptRequest = viewModel::acceptFriendRequest,
                onDeclineRequest = viewModel::declineFriendRequest
            )
            2 -> DiscoverTab(
                onSendRequest = viewModel::sendFriendRequest,
                onFindByPhone = viewModel::findFriendByPhone
            )
        }
        
        // Error/Message handling
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // Show snackbar or handle error
                viewModel.clearError()
            }
        }
        
        uiState.message?.let { message ->
            LaunchedEffect(message) {
                // Show success message
                viewModel.clearMessage()
            }
        }
    }
}

@Composable
private fun FriendsTab(
    friends: List<Friend>,
    onFriendProfile: (String) -> Unit,
    onRemoveFriend: (String) -> Unit
) {
    if (friends.isEmpty()) {
        EmptyFriendsState()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(friends) { friend ->
                FriendCard(
                    friend = friend,
                    onFriendProfile = onFriendProfile,
                    onRemoveFriend = onRemoveFriend
                )
            }
        }
    }
}

@Composable
private fun FriendCard(
    friend: Friend,
    onFriendProfile: (String) -> Unit,
    onRemoveFriend: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    AnimatedPressableCard(
        onClick = { onFriendProfile(friend.id) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Photo
            AsyncImage(
                model = friend.profilePhotoUrl ?: "",
                contentDescription = "Profile photo",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                fallback = androidx.compose.ui.res.painterResource(
                    android.R.drawable.ic_menu_gallery
                )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "${friend.totalRides} rides • ${friend.mutualFriends} mutual friends",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Online status
                if (friend.isOnline) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AkahidegnColors.Success)
                        )
                        Text(
                            text = "Online",
                            style = MaterialTheme.typography.labelSmall,
                            color = AkahidegnColors.Success
                        )
                    }
                } else {
                    friend.lastSeen?.let { lastSeen ->
                        Text(
                            text = "Last seen ${formatLastSeen(lastSeen)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Actions menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("View Profile") },
                        onClick = {
                            onFriendProfile(friend.id)
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove Friend") },
                        onClick = {
                            onRemoveFriend(friend.id)
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestsTab(
    requests: List<FriendRequest>,
    onAcceptRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit
) {
    if (requests.isEmpty()) {
        EmptyRequestsState()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(requests) { request ->
                FriendRequestCard(
                    request = request,
                    onAccept = onAcceptRequest,
                    onDecline = onDeclineRequest
                )
            }
        }
    }
}

@Composable
private fun FriendRequestCard(
    request: FriendRequest,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = request.senderPhotoUrl ?: "",
                    contentDescription = "Profile photo",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    fallback = androidx.compose.ui.res.painterResource(
                        android.R.drawable.ic_menu_gallery
                    )
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.senderName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Wants to be friends",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTimestamp(request.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onDecline(request.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Decline")
                }
                
                Button(
                    onClick = { onAccept(request.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Accept")
                }
            }
        }
    }
}

@Composable
private fun DiscoverTab(
    onSendRequest: (String) -> Unit,
    onFindByPhone: (String) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Find by phone number
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Find Friends by Phone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { onFindByPhone(phoneNumber) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = phoneNumber.isNotBlank()
                ) {
                    Text("Search")
                }
            }
        }
        
        // Send request by user ID
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Send Friend Request",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("User ID") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { onSendRequest(userId) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = userId.isNotBlank()
                ) {
                    Text("Send Request")
                }
            }
        }
        
        // Suggested friends (placeholder)
        SuggestedFriendsSection()
    }
}

@Composable
private fun SuggestedFriendsSection() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Suggested Friends",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "We'll suggest friends based on your contacts and mutual connections.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = { /* TODO: Implement contact sync */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Phone, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync Contacts")
            }
        }
    }
}

@Composable
private fun EmptyFriendsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "No friends",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Friends Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = "Add friends to share rides and compete on leaderboards!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyRequestsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "No requests",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Friend Requests",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = "Friend requests will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

data class FriendRequest(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderPhotoUrl: String?,
    val timestamp: Long,
    val status: String = "PENDING"
)

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatLastSeen(lastSeen: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - lastSeen
    
    return when {
        diff < 60_000 -> "just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}
