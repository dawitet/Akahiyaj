package com.dawitf.akahidegn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.dawitf.akahidegn.ui.viewmodels.AnimationViewModel

/**
 * Integration examples showing how to use the new animation system
 * with existing Akahidegn app components
 */

/**
 * Enhanced User Registration Dialog with animations
 * Integrates with your existing UserRegistrationDialog.kt
 */
@Composable
fun EnhancedUserRegistrationFlow(
    onRegistrationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animationViewModel: AnimationViewModel = viewModel()
    val notifications by animationViewModel.notifications.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var isRegistering by remember { mutableStateOf(false) }
    var registrationStep by remember { mutableStateOf(1) }

    Column(modifier = modifier.fillMaxSize()) {
        // Your existing registration form goes here
        UserRegistrationForm(
            currentStep = registrationStep,
            onStepComplete = { step ->
                when (step) {
                    1 -> {
                        // Show step completion success
                        animationViewModel.showSuccess(
                            "የመጀመሪያ ደረጃ ተጠናቀቀ! እባክዎ ቀጣዩን ደረጃ ይቀጥሉ።"
                        )
                        registrationStep = 2
                    }
                    2 -> {
                        isRegistering = true
                        val loadingId = animationViewModel.showLoading(
                            "እየሰራ... እባክዎ ይጠብቁ..."
                        )

                        // Use coroutine scope instead of LaunchedEffect
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(3000)
                            animationViewModel.hideLoading()

                            // Show final success
                            animationViewModel.showSuccess(
                                "እንኳን ደህና መጡ! መለያዎ በተሳካ ሁኔታ ተፈጥሯል።"
                            )

                            onRegistrationComplete()
                        }
                    }
                }
            },
            onError = { errorMessage ->
                animationViewModel.showError(
                    "ስህተት! $errorMessage"
                )
                // Reset registration step
                registrationStep = 1
                isRegistering = false
            }
        )

        // Display animations
        AnimatedNotificationList(
            notifications = notifications.map { notification ->
                NotificationItem(
                    id = notification.id,
                    type = when (notification.type) {
                        "success" -> AnimationType.SUCCESS
                        "error" -> AnimationType.ERROR
                        "warning" -> AnimationType.WARNING
                        "loading" -> AnimationType.LOADING
                        else -> AnimationType.SUCCESS
                    },
                    title = notification.message,
                    subtitle = null
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Enhanced Group Management with animations
 * Integrates with your existing group components
 */
@Composable
fun EnhancedGroupManagement(
    onGroupJoined: (groupId: String) -> Unit,
    onGroupLeft: (groupId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val animationViewModel: AnimationViewModel = viewModel()
    val notifications by animationViewModel.notifications.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        // Group actions with animated feedback
        GroupActionButtons(
            onJoinGroup = { groupId ->
                val loadingId = animationViewModel.showLoading(
                    "ወደ ቡድን እየገባ... እባክዎ ይጠብቁ..."
                )

                // Use coroutine scope instead of LaunchedEffect
                coroutineScope.launch {
                    kotlinx.coroutines.delay(2000)
                    animationViewModel.hideLoading()

                    // Show success
                    animationViewModel.showSuccess(
                        "ተሳክቷል! ወደ ቡድን በተሳካ ሁኔታ ገብተዋል።"
                    )

                    onGroupJoined(groupId)
                }
            },
            onLeaveGroup = { groupId ->
                animationViewModel.showSuccess(
                    "ከቡድን ወጥተዋል! ከቡድን በተሳካ ሁኔታ ወጥተዋል።"
                )
                onGroupLeft(groupId)
            }
        )

        // Display animations
        AnimatedNotificationList(
            notifications = notifications.map { notification ->
                NotificationItem(
                    id = notification.id,
                    type = when (notification.type) {
                        "success" -> AnimationType.SUCCESS
                        "error" -> AnimationType.ERROR
                        "warning" -> AnimationType.WARNING
                        "loading" -> AnimationType.LOADING
                        else -> AnimationType.SUCCESS
                    },
                    title = notification.message,
                    subtitle = null
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Enhanced Network Status with animations
 * Perfect for your location-based features
 */
@Composable
fun EnhancedNetworkStatus(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val animationViewModel: AnimationViewModel = viewModel()

    LaunchedEffect(isConnected) {
        if (isConnected) {
            animationViewModel.showNetworkConnected()
        } else {
            animationViewModel.showNetworkDisconnected()
        }
    }

    // Your existing network status UI
    Box(modifier = modifier) {
        // Network status content
    }
}

/**
 * Enhanced Location Updates with animations
 * Integrates with your location features
 */
@Composable
fun EnhancedLocationUpdates(
    onLocationUpdate: (location: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val animationViewModel: AnimationViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        Button(
            onClick = {
                val loadingId = animationViewModel.showLoading(
                    "አካባቢ እየፈለገ... GPS እየስራ ነው..."
                )

                // Use coroutine scope instead of LaunchedEffect
                coroutineScope.launch {
                    kotlinx.coroutines.delay(3000)
                    animationViewModel.hideLoading()

                    // Simulate success or error
                    val success = kotlin.random.Random.nextBoolean()
                    if (success) {
                        animationViewModel.showSuccess(
                            "አካባቢ ተገኘ! የአንተ አካባቢ በተሳካ ሁኔታ ተዘምኗል።"
                        )
                        onLocationUpdate("አዲስ አበባ")
                    } else {
                        animationViewModel.showError(
                            "አካባቢ አልተገኘም! GPS አገልግሎት አይሰራም። እባክዎ እንደገና ይሞክሩ።"
                        )
                        // Retry location logic can be added elsewhere
                    }
                }
            }
        ) {
            Text("አካባቢ ፈልግ")
        }
    }
}

/**
 * Helper composable for registration form
 */
@Composable
private fun UserRegistrationForm(
    currentStep: Int,
    onStepComplete: (Int) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        when (currentStep) {
            1 -> {
                Text("ደረጃ 1: መሰረታዊ መረጃ")
                // Your existing form fields
                Button(
                    onClick = { onStepComplete(1) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ቀጣይ")
                }
            }
            2 -> {
                Text("ደረጃ 2: አረጋገጥ")
                // Your existing verification form
                Button(
                    onClick = { onStepComplete(2) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ጨርስ")
                }
            }
        }
    }
}

/**
 * Helper composable for group actions
 */
@Composable
private fun GroupActionButtons(
    onJoinGroup: (String) -> Unit,
    onLeaveGroup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { onJoinGroup("test_group_id") },
            modifier = Modifier.weight(1f)
        ) {
            Text("ወደ ቡድን ግባ")
        }

        Button(
            onClick = { onLeaveGroup("test_group_id") },
            modifier = Modifier.weight(1f)
        ) {
            Text("ከቡድን ውጣ")
        }
    }
}
