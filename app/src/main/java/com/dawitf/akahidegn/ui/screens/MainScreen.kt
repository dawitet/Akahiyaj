package com.dawitf.akahidegn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.ui.components.RideGroupCard
import com.dawitf.akahidegn.ui.components.EmptyStateComponent
import com.dawitf.akahidegn.ui.components.SearchFilters
import com.dawitf.akahidegn.ui.components.EnhancedPullToRefresh
import com.dawitf.akahidegn.ui.components.ShimmerGroupList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    groups: List<Group>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onGroupClick: (Group) -> Unit,
    isLoading: Boolean,
    onRefreshGroups: () -> Unit,
    onCreateGroup: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    
    suspend fun handleRefresh() {
        isRefreshing = true
        onRefreshGroups()
        isRefreshing = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Ride Groups",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateGroup,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Group")
            }
        }
    ) { innerPadding ->
        EnhancedPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = { 
                coroutineScope.launch { 
                    handleRefresh() 
                }
            }
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search ride groups...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content
                if (isLoading) {
                    ShimmerGroupList()
                } else if (groups.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateComponent(
                            icon = Icons.Default.DirectionsCar,
                            title = "No Groups Available",
                            subtitle = "Create a new group or pull down to refresh",
                            actionText = "Create Group",
                            onActionClick = onCreateGroup
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(groups) { group ->
                            RideGroupCard(
                                group = group,
                                onClick = { onGroupClick(group) }
                            )
                        }
                        
                        // Bottom spacing for FAB
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}