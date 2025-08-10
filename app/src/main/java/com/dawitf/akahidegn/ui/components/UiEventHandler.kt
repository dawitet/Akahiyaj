package com.dawitf.akahidegn.ui.components

import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.dawitf.akahidegn.core.event.UiEvent
import kotlinx.coroutines.flow.SharedFlow

/**
 * Handles UI events in a lifecycle-aware manner.
 * 
 * This composable collects UI events and converts them to UI actions:
 * - Shows toasts and snackbars
 * - Handles navigation
 * - Manages permissions
 * - Triggers other UI side effects
 * 
 * @param uiEvents SharedFlow of UI events to handle
 * @param navController Navigation controller for routing
 * @param snackbarHostState Host state for showing snackbars
 * @param onLocationPermissionRequested Callback for location permission request
 * @param onNotificationPermissionRequested Callback for notification permission request
 */
@Composable
fun UiEventHandler(
    uiEvents: SharedFlow<UiEvent>,
    navController: NavController,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onLocationPermissionRequested: () -> Unit = {},
    onNotificationPermissionRequested: () -> Unit = {}
) {
    val context = LocalContext.current
    
    LaunchedEffect(uiEvents) {
        uiEvents.collect { event ->
            when (event) {
                // Toast Events
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is UiEvent.ShowToastRes -> {
                    Toast.makeText(context, context.getString(event.messageRes), Toast.LENGTH_SHORT).show()
                }
                
                // Snackbar Events
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel
                    )
                }
                is UiEvent.ShowSnackbarRes -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(event.messageRes),
                        actionLabel = event.actionLabelRes?.let { context.getString(it) }
                    )
                }
                
                // Navigation Events
                is UiEvent.NavigateToScreen -> {
                    navController.navigate(event.route)
                }
                is UiEvent.NavigateBack -> {
                    navController.popBackStack()
                }
                is UiEvent.NavigateToGroupDetails -> {
                    navController.navigate("group_details/${event.groupId}")
                }
                is UiEvent.NavigateToProfile -> {
                    val route = if (event.userId != null) {
                        "profile/${event.userId}"
                    } else {
                        "profile"
                    }
                    navController.navigate(route)
                }
                
                // Error Events
                is UiEvent.ShowError -> {
                    Toast.makeText(context, "Error: ${event.error}", Toast.LENGTH_LONG).show()
                }
                is UiEvent.ShowErrorRes -> {
                    Toast.makeText(context, "Error: ${context.getString(event.errorRes)}", Toast.LENGTH_LONG).show()
                }
                
                // Success Events
                is UiEvent.ShowSuccess -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is UiEvent.ShowSuccessRes -> {
                    Toast.makeText(context, context.getString(event.messageRes), Toast.LENGTH_SHORT).show()
                }
                
                // Permission Events
                is UiEvent.RequestLocationPermission -> {
                    onLocationPermissionRequested()
                }
                is UiEvent.RequestNotificationPermission -> {
                    onNotificationPermissionRequested()
                }
                
                // Group-specific Events
                is UiEvent.GroupCreatedSuccess -> {
                    Toast.makeText(
                        context, 
                        "Successfully created group: ${event.groupName}", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is UiEvent.GroupJoinedSuccess -> {
                    Toast.makeText(
                        context, 
                        "Successfully joined group: ${event.groupName}", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is UiEvent.GroupLeftSuccess -> {
                    Toast.makeText(
                        context, 
                        "Left group: ${event.groupName}", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                // Authentication Events
                is UiEvent.SignInRequired -> {
                    Toast.makeText(context, "Please sign in to continue", Toast.LENGTH_LONG).show()
                    // Could navigate to sign-in screen here
                }
                is UiEvent.SignOutSuccess -> {
                    Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                }
                
                // Loading Events
                is UiEvent.ShowLoading -> {
                    // Loading is typically handled by UI state, but could show global loading indicator
                }
                is UiEvent.HideLoading -> {
                    // Hide global loading indicator
                }
            }
        }
    }
}
