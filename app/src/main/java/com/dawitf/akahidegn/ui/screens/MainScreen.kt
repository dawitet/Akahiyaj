package com.dawitf.akahidegn.ui.screens

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.ui.components.GroupCard
import com.dawitf.akahidegn.domain.model.SearchFilters
import com.dawitf.akahidegn.ui.components.EnhancedSearchBar
import com.dawitf.akahidegn.ui.components.NoSearchResults
import com.dawitf.akahidegn.ui.components.HomeTabLayout
import com.dawitf.akahidegn.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun MainScreen(
    groups: List<Group>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onGroupClick: (Group) -> Unit,
    onJoinGroup: (Group) -> Unit, // New parameter for join functionality
    isLoading: Boolean,
    onRefreshGroups: () -> Unit,
    onCreateGroup: () -> Unit,
    userLocation: Location?,
    onOpenProfile: () -> Unit,
    onOpenHistory: () -> Unit
) {
    var refreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            refreshing = true
            onRefreshGroups()
        }
    )

    // Reset refreshing state when loading completes
    LaunchedEffect(isLoading) {
        if (!isLoading) refreshing = false
    }

    // Timeout for refreshing state to prevent getting stuck
    LaunchedEffect(refreshing) {
        if (refreshing) {
            kotlinx.coroutines.delay(10000L) // 10 second timeout
            refreshing = false
        }
    }

    HomeTabLayout(
        headerContent = {
            // Header content with title and action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.group_list_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = com.dawitf.akahidegn.ui.theme.HomeContentText
                )
                
                Row {
                    IconButton(onClick = onOpenHistory) { 
                        Icon(
                            Icons.Default.History, 
                            contentDescription = stringResource(id = R.string.activity_history_title),
                            tint = com.dawitf.akahidegn.ui.theme.HomeContentText
                        ) 
                    }
                    IconButton(onClick = onOpenProfile) { 
                        Icon(
                            Icons.Default.Person, 
                            contentDescription = stringResource(id = R.string.profile),
                            tint = com.dawitf.akahidegn.ui.theme.HomeContentText
                        ) 
                    }
                }
            }
        },
        mainContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullToRefreshState)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    EnhancedSearchBar(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onSearchSubmit = { /* ViewModel handles search logic based on query changes */ }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // The content area
                    if (isLoading && groups.isEmpty() && searchQuery.isBlank()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = com.dawitf.akahidegn.ui.theme.HomeContentText)
                        }
                    } else if (groups.isEmpty() && searchQuery.isNotEmpty()) {
                        NoSearchResults(query = searchQuery, onClearSearch = { onSearchQueryChange("") })
                    } else if (groups.isEmpty() && !isLoading) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                stringResource(id = R.string.empty_groups_message),
                                style = MaterialTheme.typography.bodyLarge,
                                color = com.dawitf.akahidegn.ui.theme.HomeContentText
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(groups, key = { group -> group.groupId ?: group.hashCode() }) { group ->
                                GroupCard(
                                    group = group, 
                                    onClick = { onGroupClick(group) }, 
                                    onJoinClick = { onJoinGroup(it) },
                                    userLocation = userLocation
                                )
                            }
                        }
                    }
                }

                // The PullRefreshIndicator is an overlay aligned to the top center
                PullRefreshIndicator(
                    refreshing = refreshing,
                    state = pullToRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                
                // Floating Action Button
                FloatingActionButton(
                    onClick = onCreateGroup,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = com.dawitf.akahidegn.ui.theme.HomeContentText,
                    contentColor = com.dawitf.akahidegn.ui.theme.HomeContentBackground
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.create_group_button))
                }
            }
        }
    )
}

