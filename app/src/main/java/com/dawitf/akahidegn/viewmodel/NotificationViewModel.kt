package com.dawitf.akahidegn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.dawitf.akahidegn.domain.model.NotificationItem
import com.dawitf.akahidegn.notifications.service.NotificationManagerService
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationService: NotificationManagerService
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Create mock notifications for testing
                val mockNotifications = createMockNotifications()
                _notifications.value = mockNotifications.sortedByDescending { it.timestamp }
                updateUnreadCount()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun dismissNotification(notificationId: String) {
        _notifications.value = _notifications.value.filter { it.id != notificationId }
        updateUnreadCount()
    }

    fun markAsRead(notificationId: String) {
        _notifications.value = _notifications.value.map { notification ->
            if (notification.id == notificationId) {
                notification.copy(isRead = true)
            } else {
                notification
            }
        }
        updateUnreadCount()
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
        _unreadCount.value = 0
    }

    private fun updateUnreadCount() {
        _unreadCount.value = _notifications.value.count { !it.isRead }
    }

    private fun createMockNotifications(): List<NotificationItem> {
        return listOf(
            NotificationItem(
                id = "1",
                title = "አዲስ አባል ወደ ቡድን ገባ",
                message = "የሚካኤል የተሰኘ ሰው ወደ 'ቦሌ' ቡድን ገብቷል",
                timestamp = System.currentTimeMillis() - 300000, // 5 minutes ago
                isRead = false
            ),
            NotificationItem(
                id = "2",
                title = "ቡድንዎ ሞላ",
                message = "'መገናኛ' ቡድን አሁን ሙሉ ነው። ጉዞን ይጀምሩ!",
                timestamp = System.currentTimeMillis() - 900000, // 15 minutes ago
                isRead = false
            ),
            NotificationItem(
                id = "3",
                title = "አባል ቡድን ለቀቀ",
                message = "አንድ አባል 'ፒያሳ' ቡድንን ለቅቋል",
                timestamp = System.currentTimeMillis() - 1800000, // 30 minutes ago
                isRead = true
            )
        )
    }
}
