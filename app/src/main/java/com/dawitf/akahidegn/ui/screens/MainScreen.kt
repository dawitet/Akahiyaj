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
import com.dawitf.akahidegn.ui.components.HomeTabLayout
import com.dawitf.akahidegn.R
import com.dawitf.akahidegn.ui.animation.shared.SharedElement
import com.dawitf.akahidegn.ui.animation.shared.SharedElementKeys
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.derivedStateOf
import com.dawitf.akahidegn.performance.rememberOptimizedListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    groups: List<Group>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onGroupClick: (Group) -> Unit,
    onJoinGroup: (Group) -> Unit, // New parameter for optimistic join operations
    isLoading: Boolean,
    onRefreshGroups: () -> Unit,
    onCreateGroup: () -> Unit,
    userLocation: Location?,
    onOpenProfile: () -> Unit,
    onOpenHistory: () -> Unit
) {
    // Optimize list state for better performance
    val optimizedGroups = rememberOptimizedListState(
        list = groups,
        keySelector = { it.groupId ?: it.hashCode() }
    )
    
    // Create stable callbacks to prevent unnecessary recompositions
    val stableOnGroupClick = remember { onGroupClick }
    val stableOnJoinGroup = remember { onJoinGroup }
    val stableOnSearchQueryChange = remember { onSearchQueryChange }
    val stableOnRefreshGroups = remember { onRefreshGroups }
    val stableOnCreateGroup = remember { onCreateGroup }
    val stableOnOpenProfile = remember { onOpenProfile }
    val stableOnOpenHistory = remember { onOpenHistory }
    
    // Memoized empty state check to prevent recomposition
    val isEmpty = remember(optimizedGroups, searchQuery) {
        derivedStateOf {
            optimizedGroups.isEmpty() && searchQuery.isNotEmpty()
        }
    }.value
    
    HomeTabLayout(
        headerContent = {
            Text(
                text = "Home",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.Center)
            )
        },
        mainContent = {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                EnhancedSearchBar(
                    query = searchQuery,
                    onQueryChange = stableOnSearchQueryChange,
                    onSearchSubmit = { /* ViewModel handles search logic based on query changes */ }
                )
                Spacer(modifier = Modifier.height(8.dp))

                // The content area
                if (isLoading && optimizedGroups.isEmpty() && searchQuery.isBlank()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (isEmpty) {
                    NoSearchResults(query = searchQuery, onClearSearch = { stableOnSearchQueryChange("") })
                } else if (optimizedGroups.isEmpty() && !isLoading) {
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
                        items(optimizedGroups, key = { group -> group.groupId ?: group.hashCode() }) { group ->
                            GroupCard(
                                group = group, 
                                onClick = { stableOnGroupClick(group) }, 
                                onJoinClick = { stableOnJoinGroup(group) },
                                userLocation = userLocation
                            )
                        }
                    }
                }
            }

            // Multiple Floating Action Buttons positioned on top of the content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Profile button with shared element
                    SharedElement(key = SharedElementKeys.PROFILE_BUTTON) { sharedMod ->
                        FloatingActionButton(
                            onClick = stableOnOpenProfile,
                            modifier = sharedMod,
                            containerColor = MaterialTheme.colorScheme.secondary
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        }
                    }
                    
                    // History button with shared element
                    SharedElement(key = SharedElementKeys.HISTORY_BUTTON) { sharedMod ->
                        FloatingActionButton(
                            onClick = stableOnOpenHistory,
                            modifier = sharedMod,
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                    }
                    
                    // Create group button
                    FloatingActionButton(onClick = stableOnCreateGroup) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.create_group_button))
                    }
                }
            }
        }
    )
}

