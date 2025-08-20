package com.dawitf.akahidegn.core.event

/**
 * Sealed class representing one-time UI events that should be consumed once
 * and not replayed when the UI is recreated (e.g., configuration changes).
 * 
 * Uses SharedFlow instead of StateFlow to prevent event replay.
 */
sealed interface UiEvent {
    
    // Toast and Snackbar Events
    data class ShowToast(val message: String) : UiEvent
    data class ShowToastRes(val messageRes: Int) : UiEvent
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : UiEvent
    data class ShowSnackbarRes(val messageRes: Int, val actionLabelRes: Int? = null) : UiEvent
    
    // Navigation Events
    data class NavigateToScreen(val route: String) : UiEvent
    data object NavigateBack : UiEvent
    data class NavigateToGroupDetails(val groupId: String) : UiEvent
    data class NavigateToProfile(val userId: String? = null) : UiEvent
    
    // Error Events
    data class ShowError(val error: String) : UiEvent
    data class ShowErrorRes(val errorRes: Int) : UiEvent
    
    // Success Events
    data class ShowSuccess(val message: String) : UiEvent
    data class ShowSuccessRes(val messageRes: Int) : UiEvent
    
    // Permission Events
    data object RequestLocationPermission : UiEvent
    data object RequestNotificationPermission : UiEvent
    
    // Group-specific Events
    data class GroupCreatedSuccess(val groupId: String, val groupName: String) : UiEvent
    data class GroupJoinedSuccess(val groupId: String, val groupName: String) : UiEvent
    data class GroupLeftSuccess(val groupName: String) : UiEvent
    
    // Authentication Events
    data object SignInRequired : UiEvent
    data object SignOutSuccess : UiEvent
    
    // Loading Events
    data object ShowLoading : UiEvent
    data object HideLoading : UiEvent
}
