package com.dawitf.akahidegn.ui.screens

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.ui.components.GroupCard
import com.dawitf.akahidegn.domain.model.SearchFilters
import com.dawitf.akahidegn.ui.components.EnhancedSearchBar
import com.dawitf.akahidegn.ui.components.NoSearchResults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.dawitf.akahidegn.R

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
    userLocation: Location?,
    onOpenProfile: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            onRefreshGroups()
        }
    }

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            pullToRefreshState.endRefresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.group_list_title)) },
                actions = {
                    IconButton(onClick = onOpenHistory) { Icon(Icons.Default.History, contentDescription = stringResource(id = R.string.activity_history_title)) }
                    IconButton(onClick = onOpenProfile) { Icon(Icons.Default.Person, contentDescription = stringResource(id = R.string.profile)) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateGroup) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.create_group_button))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(pullToRefreshState.nestedScrollConnection) // Attach the nested scroll
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
                        CircularProgressIndicator()
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
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(groups, key = { group -> group.groupId ?: group.hashCode() }) { group ->
                            GroupCard(group = group, onClick = { onGroupClick(group) }, userLocation = userLocation)
                        }
                    }
                }
            }

            // The PullToRefreshContainer is now an overlay aligned to the top center
            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

