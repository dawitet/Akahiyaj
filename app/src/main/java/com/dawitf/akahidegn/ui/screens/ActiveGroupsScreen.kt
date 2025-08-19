package com.dawitf.akahidegn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dawitf.akahidegn.ui.components.EnhancedPullToRefresh
import com.dawitf.akahidegn.ui.components.GroupCard
import com.dawitf.akahidegn.viewmodel.MainViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.dawitf.akahidegn.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import com.dawitf.akahidegn.ui.animation.shared.SharedAnimatedVisibility
import com.dawitf.akahidegn.ui.animation.shared.SharedElement
import com.dawitf.akahidegn.ui.animation.shared.SharedElementKeys
import com.dawitf.akahidegn.ui.animation.shared.AnimationType
import com.dawitf.akahidegn.ui.animation.shared.TransformType

@Composable
fun ActiveGroupsScreen(
    onOpenHistory: () -> Unit = {}
) {
    val viewModel: MainViewModel = hiltViewModel()
    val userGroups by viewModel.userGroups.collectAsState()
    val isLoading by viewModel.isLoadingGroups.collectAsState()
    val currentUserId = Firebase.auth.currentUser?.uid

    // Set userId for filtering (if not already set)
    LaunchedEffect(currentUserId) {
        viewModel.setCurrentUserId(currentUserId)
    }

    // Force refresh when screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.refreshGroups()
    }

    // Simple column structure matching Main page formatting
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(id = R.string.group_list_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Content area with official pull-to-refresh
            val refreshing = isLoading
            val pullToRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { viewModel.refreshGroups() },
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
                if (refreshing) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stringResource(id = R.string.loading_groups), style = MaterialTheme.typography.bodyLarge)
                    }
                } else if (userGroups.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.no_groups_found),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.nearby_groups_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = userGroups,
                            key = { group -> group.groupId ?: (group.destinationName ?: "group") }
                        ) { group ->
                            val isActive = (group.timestamp ?: 0L) > (System.currentTimeMillis() - (30 * 60 * 1000L))
                            val itemAlpha = if (isActive) 1f else 0.55f
                            Box(modifier = Modifier.alpha(itemAlpha)) {
                                GroupCard(
                                    group = group,
                                    userLocation = null,
                                    onClick = { /* Navigate to details */ },
                                    onJoinClick = null
                                )
                            }
                        }
                    }
                }
            }
        }

        // History FAB in bottom-right corner
        ExtendedFloatingActionButton(
            onClick = onOpenHistory,
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            text = { Text(text = stringResource(id = R.string.activity_history_title)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}
