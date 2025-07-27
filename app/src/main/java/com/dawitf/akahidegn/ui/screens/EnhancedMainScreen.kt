package com.dawitf.akahidegn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dawitf.akahidegn.ui.components.*
import com.dawitf.akahidegn.ui.viewmodels.AnimationViewModel
import kotlinx.coroutines.launch

/**
 * Enhanced Main Screen with integrated animation system
 * This shows how to integrate animations into your existing MainScreen
 */
@Composable
fun EnhancedMainScreen(
    modifier: Modifier = Modifier
) {
    val animationViewModel: AnimationViewModel = viewModel()
    val notifications by animationViewModel.notifications.collectAsState()
    val isLoading by animationViewModel.isLoading.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // State management for your existing features
    var showUserRegistrationDialog by remember { mutableStateOf(false) }
    var showGroupMembersDialog by remember { mutableStateOf(false) }
    var isLocationUpdateInProgress by remember { mutableStateOf(false) }
    var networkStatus by remember { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Your existing MainScreen content goes here
            // I'm showing how to enhance it with animations

            // Enhanced location updates with animations
            LocationUpdateSection(
                onLocationUpdateRequested = {
                    if (!isLocationUpdateInProgress) {
                        isLocationUpdateInProgress = true
                        val loadingId = animationViewModel.showLoading(
                            title = "አካባቢ እየፈለገ...",
                            subtitle = "GPS ምልክት እየተሰላ ነው..."
                        )

                        // Use coroutine scope instead of LaunchedEffect
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(3000) // Replace with your actual location logic
                            animationViewModel.hideLoading(loadingId)

                            // Show success or error based on actual result
                            val locationFound = kotlin.random.Random.nextBoolean() // Replace with actual result
                            if (locationFound) {
                                animationViewModel.showSuccess(
                                    title = "አካባቢ ተገኘ!",
                                    subtitle = "የአንተ አካባቢ በተሳካ ሁኔታ ተዘምኗል።",
                                    preset = NotificationPresets.quickSuccess("አካባቢ ተዘምኗል")
                                )
                            } else {
                                animationViewModel.showError(
                                    title = "አካባቢ አልተገኘም!",
                                    subtitle = "GPS አገልግሎት ሊሰራ አልቻለም። እባክዎ እንደገና ይሞክሩ።",
                                    onRetry = {
                                        // Retry location update
                                        isLocationUpdateInProgress = false
                                    }
                                )
                            }
                            isLocationUpdateInProgress = false
                        }
                    }
                }
            )

            // Enhanced group management with animations
            GroupManagementSection(
                onJoinGroupRequested = { groupId ->
                    val loadingId = animationViewModel.showLoading(
                        title = "ወደ ቡድን እየገባ...",
                        subtitle = "እባክዎ ይጠብቁ..."
                    )

                    // Use coroutine scope instead of LaunchedEffect
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(2000)
                        animationViewModel.hideLoading(loadingId)
                        animationViewModel.showSuccess(
                            title = "ወደ ቡድን ገብተዋል!",
                            subtitle = "በተሳካ ሁኔታ ወደ ቡድን ገብተዋል።",
                            preset = ContextPresets.FormSubmission.success
                        )
                    }
                },
                onShowGroupMembers = {
                    showGroupMembersDialog = true
                }
            )

            // User registration section with animations
            UserRegistrationSection(
                onShowRegistration = {
                    showUserRegistrationDialog = true
                }
            )

            // Network status monitoring with animations
            NetworkStatusMonitor(
                isConnected = networkStatus,
                animationViewModel = animationViewModel
            )

            // Your existing MainScreen components can be placed here
            // For example: AvailableGroupsBox, LocationHistoryBox, etc.
        }

        // Display all animations as an overlay
        AnimatedNotificationList(
            notifications = notifications,
            modifier = Modifier.fillMaxWidth()
        )

        // Enhanced dialogs with animations
        EnhancedUserRegistrationDialog(
            showDialog = showUserRegistrationDialog,
            onDismiss = { showUserRegistrationDialog = false },
            onRegistrationSuccess = {
                // Handle successful registration
                showUserRegistrationDialog = false
            }
        )

        EnhancedGroupMembersDialog(
            showDialog = showGroupMembersDialog,
            groupMembers = emptyList(), // Replace with your actual group members
            onDismiss = { showGroupMembersDialog = false },
            onMemberRemoved = { member ->
                // Handle member removal with animation feedback
            }
        )
    }
}

@Composable
private fun LocationUpdateSection(
    onLocationUpdateRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "አካባቢ ዝማኔ",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onLocationUpdateRequested,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("አካባቢ ዘምን")
            }
        }
    }
}

@Composable
private fun GroupManagementSection(
    onJoinGroupRequested: (String) -> Unit,
    onShowGroupMembers: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "ቡድን አስተዳደር",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onJoinGroupRequested("sample_group") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ወደ ቡድን ግባ")
                }
                Button(
                    onClick = onShowGroupMembers,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("አባላት ይመልከቱ")
                }
            }
        }
    }
}

@Composable
private fun UserRegistrationSection(
    onShowRegistration: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "ተጠቃሚ ምዝገባ",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onShowRegistration,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("አዲስ ተጠቃሚ ይመዝገቡ")
            }
        }
    }
}

@Composable
private fun NetworkStatusMonitor(
    isConnected: Boolean,
    animationViewModel: AnimationViewModel,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(isConnected) {
        if (isConnected) {
            animationViewModel.showNetworkConnected()
        } else {
            animationViewModel.showNetworkDisconnected()
        }
    }

    // Optional: Show current network status
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = if (isConnected) "ኢንተርኔት ግንኙነት አለ" else "ኢንተርኔት ግንኙነት የለም",
            modifier = Modifier.padding(16.dp),
            color = if (isConnected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
