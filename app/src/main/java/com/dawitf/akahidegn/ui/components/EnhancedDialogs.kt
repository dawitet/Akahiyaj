package com.dawitf.akahidegn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dawitf.akahidegn.ui.viewmodels.AnimationViewModel
import kotlinx.coroutines.launch

/**
 * Enhanced versions of your existing dialog components with animation integration
 */

/**
 * Enhanced User Registration Dialog with smooth animations
 * Replaces/enhances your existing UserRegistrationDialog.kt
 */
@Composable
fun EnhancedUserRegistrationDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onRegistrationSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animationViewModel: AnimationViewModel = viewModel()
    val notifications by animationViewModel.notifications.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var userName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("አዲስ ተጠቃሚ ምዝገባ") },
            text = {
                Column {
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("ስም") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("ስልክ ቁጥር") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Display animations within dialog
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (userName.isBlank() || phoneNumber.isBlank()) {
                            // Show validation error with animation
                            animationViewModel.showError(
                                "ማስታወሻ! እባክዎ ሁሉንም ሳጥኖች ይሙሉ።"
                            )
                            return@Button
                        }

                        isRegistering = true
                        val loadingId = animationViewModel.showLoading(
                            "እየሰራ... ተጠቃሚ እየተመዘገበ ነው..."
                        )

                        // Use coroutine scope instead of LaunchedEffect
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(2000)
                            animationViewModel.hideLoading()

                            // Simulate success or failure
                            val success = kotlin.random.Random.nextBoolean()
                            if (success) {
                                animationViewModel.showFormSubmissionSuccess("መለያ በተሳካ ሁኔታ ተፈጥሯል")
                                onRegistrationSuccess()
                                onDismiss()
                            } else {
                                animationViewModel.showFormSubmissionError("መለያ ለመፍጠር አልተቻለም")
                                isRegistering = false
                            }
                        }
                    },
                    enabled = !isRegistering
                ) {
                    Text("ተመዝገብ")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("ተወው")
                }
            }
        )
    }
}

/**
 * Enhanced Group Members Dialog with animations
 * Enhances your existing GroupMembersDialog.kt
 */
@Composable
fun EnhancedGroupMembersDialog(
    showDialog: Boolean,
    groupMembers: List<GroupMember>,
    onDismiss: () -> Unit,
    onMemberRemoved: (GroupMember) -> Unit,
    modifier: Modifier = Modifier
) {
    val animationViewModel: AnimationViewModel = viewModel()
    val notifications by animationViewModel.notifications.collectAsState()

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("የቡድን አባላት") },
            text = {
                Column {
                    groupMembers.forEach { member ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(member.name)
                                    Text(
                                        text = member.phone ?: "No phone number",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        // Show confirmation with animation
                                        animationViewModel.showSuccess(
                                            "አባል ተወገደ! ${member.name} ከቡድን ተወግዷል።"
                                        )
                                        onMemberRemoved(member)
                                    }
                                ) {
                                    Text("አስወግድ", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    // Display animations within dialog
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
            },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("እሺ")
                }
            }
        )
    }
}

/**
 * Enhanced Success with Leave Group Dialog
 * Enhances your existing SuccessWithLeaveGroupDialog.kt
 */
@Composable
fun EnhancedSuccessWithLeaveGroupDialog(
    showDialog: Boolean,
    groupName: String,
    onDismiss: () -> Unit,
    onLeaveGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animationViewModel: AnimationViewModel = viewModel()

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("ከቡድን መውጣት") },
            text = {
                Column {
                    Text("ከ '$groupName' ቡድን መውጣት ይፈልጋሉ?")
                    Text(
                        "ይህ ተግባር መልሰው ማድረግ አይችሉም።",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Show animated confirmation
                        animationViewModel.showSuccess(
                            "ከቡድን ወጥተዋል! '$groupName' ቡድን በተሳካ ሁኔታ ወጥተዋል።"
                        )
                        onLeaveGroup()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("ውጣ")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("ተው")
                }
            }
        )
    }
}
