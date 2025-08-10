package com.dawitf.akahidegn.core.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages UI events using SharedFlow for one-time event delivery.
 * 
 * SharedFlow characteristics:
 * - replay = 0: Events are not replayed to new collectors
 * - extraBufferCapacity = 1: Can buffer one event if no collectors are active
 * - onBufferOverflow = DROP_OLDEST: Drop old events if buffer is full
 * 
 * This ensures events are delivered exactly once and don't persist across
 * configuration changes or navigation.
 */
@Singleton
class UiEventManager @Inject constructor() {
    
    private val _uiEvents = MutableSharedFlow<UiEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    
    /**
     * Cold flow of UI events. Each event is delivered exactly once.
     */
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()
    
    /**
     * Emits a UI event. If no collectors are active, the event is buffered.
     * If buffer is full, the oldest event is dropped.
     */
    fun emitEvent(event: UiEvent) {
        _uiEvents.tryEmit(event)
    }
    
    // Convenience methods for common events
    fun showToast(message: String) = emitEvent(UiEvent.ShowToast(message))
    fun showToastRes(messageRes: Int) = emitEvent(UiEvent.ShowToastRes(messageRes))
    fun showError(error: String) = emitEvent(UiEvent.ShowError(error))
    fun showErrorRes(errorRes: Int) = emitEvent(UiEvent.ShowErrorRes(errorRes))
    fun showSuccess(message: String) = emitEvent(UiEvent.ShowSuccess(message))
    fun showSuccessRes(messageRes: Int) = emitEvent(UiEvent.ShowSuccessRes(messageRes))
    
    fun navigateToScreen(route: String) = emitEvent(UiEvent.NavigateToScreen(route))
    fun navigateBack() = emitEvent(UiEvent.NavigateBack)
    fun navigateToGroupDetails(groupId: String) = emitEvent(UiEvent.NavigateToGroupDetails(groupId))
    fun navigateToProfile(userId: String? = null) = emitEvent(UiEvent.NavigateToProfile(userId))
    
    fun requestLocationPermission() = emitEvent(UiEvent.RequestLocationPermission)
    fun requestNotificationPermission() = emitEvent(UiEvent.RequestNotificationPermission)
    
    fun groupCreatedSuccess(groupId: String, groupName: String) = 
        emitEvent(UiEvent.GroupCreatedSuccess(groupId, groupName))
    fun groupJoinedSuccess(groupId: String, groupName: String) = 
        emitEvent(UiEvent.GroupJoinedSuccess(groupId, groupName))
    fun groupLeftSuccess(groupName: String) = 
        emitEvent(UiEvent.GroupLeftSuccess(groupName))
        
    fun signInRequired() = emitEvent(UiEvent.SignInRequired)
    fun signOutSuccess() = emitEvent(UiEvent.SignOutSuccess)
    
    fun showLoading() = emitEvent(UiEvent.ShowLoading)
    fun hideLoading() = emitEvent(UiEvent.HideLoading)
}
