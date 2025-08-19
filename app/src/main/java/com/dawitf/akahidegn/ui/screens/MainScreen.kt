package com.dawitf.akahidegn.ui.screens

import android.location.Location
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.ui.components.GroupCard
import com.dawitf.akahidegn.domain.model.SearchFilters
import com.dawitf.akahidegn.ui.components.EnhancedSearchBar
import com.dawitf.akahidegn.ui.components.NoSearchResults
import com.dawitf.akahidegn.ui.components.EnhancedPullToRefresh
import com.dawitf.akahidegn.ui.components.HomeTabLayout
import com.dawitf.akahidegn.R
import com.dawitf.akahidegn.ui.animation.shared.SharedElement
import com.dawitf.akahidegn.ui.animation.shared.SharedElementKeys
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.derivedStateOf
import com.dawitf.akahidegn.performance.rememberOptimizedListState
import com.dawitf.akahidegn.ui.components.ColorfulBlobsBackground
import androidx.compose.ui.res.painterResource
import com.dawitf.akahidegn.ui.components.FullWidthBannerAd
import com.dawitf.akahidegn.ui.components.gradientBackground
import com.dawitf.akahidegn.ui.animation.shared.SharedAnimatedVisibility
import com.dawitf.akahidegn.ui.animation.shared.AnimationType
import com.dawitf.akahidegn.ui.animation.shared.TransformType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    mainGroups: List<Group>,
    activeGroups: List<Group>,
    historyGroups: List<Group>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onGroupClick: (Group) -> Unit,
    onJoinGroup: (Group) -> Unit, // New parameter for optimistic join operations
    isLoading: Boolean,
    onRefreshGroups: () -> Unit,
    onCreateGroup: () -> Unit,
    userLocation: Location?
) {
    // Optimize list state for better performance
    // Only show mainGroups, no tabs or history/active switching
    val optimizedGroups = rememberOptimizedListState(
        list = mainGroups,
        keySelector = { it.groupId ?: it.hashCode() }
    )
    
    // Create stable callbacks to prevent unnecessary recompositions
    val stableOnGroupClick = remember { onGroupClick }
    val stableOnJoinGroup = remember { onJoinGroup }
    val stableOnSearchQueryChange = remember { onSearchQueryChange }
    val stableOnRefreshGroups = remember { onRefreshGroups }
    val stableOnCreateGroup = remember { onCreateGroup }
    
    // Memoized empty state check to prevent recomposition
    val isEmpty = remember(optimizedGroups, searchQuery) {
        derivedStateOf {
            optimizedGroups.isEmpty() && searchQuery.isNotEmpty()
        }
    }.value
    
    // No tabs or animated balls, just a simple column with Create Group FAB
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Home",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp)
            )
            EnhancedSearchBar(
                query = searchQuery,
                onQueryChange = stableOnSearchQueryChange,
                onSearchSubmit = { /* ViewModel handles search logic based on query changes */ }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Content area with official pull-to-refresh
            val refreshing = isLoading
            val pullToRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = stableOnRefreshGroups,
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = refreshing,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            ) {
                if (refreshing && optimizedGroups.isEmpty() && searchQuery.isBlank()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (isEmpty) {
                    NoSearchResults(query = searchQuery, onClearSearch = { stableOnSearchQueryChange("") })
                } else if (optimizedGroups.isEmpty() && !refreshing) {
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
        }

        // Create Group FAB in bottom-right corner
        ExtendedFloatingActionButton(
            onClick = stableOnCreateGroup,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(text = stringResource(id = R.string.create_group_button)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

