package com.dawitf.akahidegn.ui.screens

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.domain.model.SearchFilters
import com.dawitf.akahidegn.ui.components.*
import com.dawitf.akahidegn.ui.viewmodels.AnimationViewModel

/**
 * Main Screen
 * This is the main screen that displays groups and handles user interactions
 */
@Composable
fun MainScreen(
    mainGroups: List<com.dawitf.akahidegn.Group>,
    activeGroups: List<Group>,
    historyGroups: List<Group>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onGroupClick: (Group) -> Unit,
    onJoinGroup: (Group) -> Unit,
    isLoading: Boolean,
    onRefreshGroups: () -> Unit,
    onCreateGroup: () -> Unit,
    userLocation: Location?,
    modifier: Modifier = Modifier
) {
    val animationViewModel: AnimationViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
        // Search Bar
        SearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Filter Section
        FilterChips(
            selectedFilters = selectedFilters,
            onFiltersChange = onFiltersChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Create Group Button
        Button(
            onClick = onCreateGroup,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create New Group")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Groups List
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (mainGroups.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No groups found")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRefreshGroups) {
                        Text("Refresh")
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = mainGroups,
                    key = { group -> group.groupId ?: group.hashCode() }
                ) { group ->
                    GroupCard(
                        group = group,
                        userLocation = userLocation,
                        onClick = { onGroupClick(group) },
                        onJoinClick = { onJoinGroup(group) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        label = { Text("Search destinations...") },
        modifier = modifier
    )
}

@Composable
private fun FilterChips(
    selectedFilters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    modifier: Modifier = Modifier
) {
    // Simple filter implementation
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilters.showOnlyActive,
            onClick = {
                onFiltersChange(selectedFilters.copy(showOnlyActive = !selectedFilters.showOnlyActive))
            },
            label = { Text("Active Only") }
        )

        FilterChip(
            selected = selectedFilters.maxDistance <= 5.0,
            onClick = {
                val newDistance = if (selectedFilters.maxDistance <= 5.0) 10.0 else 5.0
                onFiltersChange(selectedFilters.copy(maxDistance = newDistance))
            },
            label = { Text("Nearby") }
        )
    }
}
