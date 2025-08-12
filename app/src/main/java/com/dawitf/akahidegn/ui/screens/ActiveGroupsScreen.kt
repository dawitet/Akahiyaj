package com.dawitf.akahidegn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dawitf.akahidegn.ui.components.ActiveGroupsTabLayout
import com.dawitf.akahidegn.ui.components.GroupCard
import com.dawitf.akahidegn.viewmodel.MainViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun ActiveGroupsScreen() {
    val viewModel: MainViewModel = hiltViewModel()
    val activeGroups by viewModel.activeGroups.collectAsState()
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

    ActiveGroupsTabLayout(
        headerContent = {
            Text(
                text = "Active Groups",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.Center)
            )
        },
        mainContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Loading your active groups...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else if (activeGroups.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "You haven't joined any groups yet.",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Join a group from the main screen to see it here!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = activeGroups,
                            key = { group -> group.groupId ?: (group.destinationName ?: "group") }
                        ) { group ->
                            GroupCard(
                                group = group,
                                userLocation = null, // Don't show distance in active groups
                                onClick = { /* Handle group details */ },
                                onJoinClick = null // Don't show join button for active groups
                            )
                        }
                    }
                }
            }
        }
    )
}
