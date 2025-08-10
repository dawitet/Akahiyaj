package com.dawitf.akahidegn.ui.screens

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.ui.components.ActiveGroupCard
import com.dawitf.akahidegn.ui.components.ActiveGroupsTabLayout

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun ActiveGroupsScreen(
    userGroups: List<Group>,
    isLoading: Boolean,
    userLocation: Location?,
    onRefresh: () -> Unit,
    onGroupClick: (Group) -> Unit,
    onDisbandGroup: (Group) -> Unit,
    onLeaveGroup: (Group) -> Unit,
    isGroupCreator: (Group) -> Boolean
) {
    var refreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            refreshing = true
            onRefresh()
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

    ActiveGroupsTabLayout(
        headerContent = {
            // Header content with title and icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.GroupWork,
                    contentDescription = "Active Groups",
                    tint = com.dawitf.akahidegn.ui.theme.ActiveGroupsContentText,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "የእርስዎ ቡድኖች",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = com.dawitf.akahidegn.ui.theme.ActiveGroupsContentText
                )
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
                    // Description text
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = com.dawitf.akahidegn.ui.theme.ActiveGroupsContentText.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = "እዚህ የፈጠሯቸውን እና የተቀላቀሉባቸውን ቡድኖች ያዩ። 👑 የሚያሳየው የፈጠሯቸውን ቡድኖች ነው።",
                            style = MaterialTheme.typography.bodyMedium,
                            color = com.dawitf.akahidegn.ui.theme.ActiveGroupsContentText,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    // Content area
                    if (isLoading && userGroups.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = com.dawitf.akahidegn.ui.theme.ActiveGroupsContentText)
                        }
                    } else if (userGroups.isEmpty()) {
                        // Empty state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupWork,
                                contentDescription = "No active groups",
                                modifier = Modifier.size(64.dp),
                                tint = com.dawitf.akahidegn.ui.theme.ActiveGroupsContentText.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "ለዛሬ ምንም ቡድን የለዎትም።",
                                style = MaterialTheme.typography.headlineSmall,
                                color = com.dawitf.akahidegn.ui.theme.ActiveGroupsContentText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ወደ ተጨማሪ ቡድኖች ትብ ሄደው አዲስ ቡድን ፈጥረው ወይም ሌላ ቡድን ተቀላቀሉ።",
                                style = MaterialTheme.typography.bodyLarge,
                                color = com.dawitf.akahidegn.ui.theme.ActiveGroupsContentText.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        // Active groups list
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(userGroups, key = { group -> group.groupId ?: group.hashCode() }) { group ->
                                ActiveGroupCard(
                                    group = group,
                                    userLocation = userLocation,
                                    onClick = { onGroupClick(group) },
                                    onDisbandGroup = { onDisbandGroup(group) },
                                    onLeaveGroup = { onLeaveGroup(group) },
                                    isCreator = isGroupCreator(group)
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
            }
        }
    )
}
