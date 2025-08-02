package com.dawitf.akahidegn.ui.screens


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.ui.components.SearchFilters
import com.dawitf.akahidegn.ui.components.*
import com.dawitf.akahidegn.ui.components.GroupFilterType
import com.dawitf.akahidegn.ui.components.GroupSortOption
import com.dawitf.akahidegn.ui.components.EnhancedSearchBar
import com.dawitf.akahidegn.ui.components.FilterChips
import com.dawitf.akahidegn.ui.components.SearchResultsHeader
import com.dawitf.akahidegn.ui.components.SortingBottomSheet
import com.dawitf.akahidegn.ui.components.NoSearchResults

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
    onNavigateToNotifications: () -> Unit,
    onNavigateToActiveGroups: () -> Unit
) {
    var showSortingBottomSheet by remember { mutableStateOf(false) }
    var currentSortOption by remember { mutableStateOf(GroupSortOption.NEAREST) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Akahidegn") },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = onNavigateToActiveGroups) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Active Groups")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateGroup) {
                Icon(Icons.Default.Add, contentDescription = "Create Group")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            EnhancedSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onSearchSubmit = { /* Handled by ViewModel's debouncing */ }
            )
            Spacer(modifier = Modifier.height(8.dp))
            FilterChips(
                selectedFilter = GroupFilterType.ALL, // You'll need to manage this state
                onFilterChange = { /* Handle filter change */ }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SearchResultsHeader(
                resultsCount = groups.size,
                query = searchQuery,
                sortOption = currentSortOption,
                onSortClick = { showSortingBottomSheet = true }
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth())
            } else if (groups.isEmpty() && searchQuery.isNotEmpty()) {
                NoSearchResults(query = searchQuery, onClearSearch = { onSearchQueryChange("") })
            } else if (groups.isEmpty()) {
                Text("No groups available. Create one!", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groups) { group ->
                        // Replace with your actual GroupCard or GroupListItem Composable
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGroupClick(group) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = group.to ?: "Unknown Destination", style = MaterialTheme.typography.titleMedium)
                                Text(text = "Members: ${group.memberCount}/${group.maxMembers}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSortingBottomSheet) {
        SortingBottomSheet(
            isVisible = showSortingBottomSheet,
            currentSort = currentSortOption,
            onSortChange = { newSort ->
                currentSortOption = newSort
                // Trigger sorting in ViewModel or update UI directly
            },
            onDismiss = { showSortingBottomSheet = false }
        )
    }
}