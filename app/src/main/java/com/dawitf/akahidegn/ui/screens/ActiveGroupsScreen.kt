package com.dawitf.akahidegn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dawitf.akahidegn.R
import com.dawitf.akahidegn.ui.components.GroupCard
import com.dawitf.akahidegn.domain.model.Group
import com.dawitf.akahidegn.ui.components.EnhancedPullToRefresh
import com.dawitf.akahidegn.viewmodel.MainViewModel
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth

@Composable
fun ActiveGroupsScreen(
    onOpenHistory: () -> Unit = {}
) {
    val viewModel: MainViewModel = hiltViewModel()
    val userGroups: List<Group> by viewModel.userGroups.collectAsState()
    val isLoading by viewModel.isLoadingGroups.collectAsState()
    val currentUserId = Firebase.auth.currentUser?.uid

    LaunchedEffect(currentUserId) { viewModel.setCurrentUserId(currentUserId) }
    LaunchedEffect(Unit) { viewModel.refreshGroups() }

    EnhancedPullToRefresh(
        isRefreshing = isLoading,
        onRefresh = { viewModel.refreshGroups() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(id = R.string.group_list_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoading && userGroups.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                userGroups.isEmpty() -> {
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
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = userGroups,
                            key = { group -> group.groupId ?: (group.destinationName ?: "group") }
                        ) { group ->
                            val isActive = !group.isExpired()
                            val itemAlpha = if (isActive) 1f else 0.55f
                            Box(modifier = Modifier.alpha(itemAlpha)) {
                                GroupCard(
                                    group = group,
                                    userLocation = null,
                                    onClick = { /* TODO: navigate to details */ },
                                    onJoinClick = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // FAB positioned independently outside the pull-to-refresh area
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        ExtendedFloatingActionButton(
            onClick = onOpenHistory,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            text = { Text(stringResource(id = R.string.view_history)) }
        )
    }
}
