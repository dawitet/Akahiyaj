package com.dawitf.akahidegn.notifications.service

import com.dawitf.akahidegn.production.ProductionNotificationManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AkahidegnFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationManager: ProductionNotificationManager

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle different types of messages
        when (remoteMessage.data["type"]) {
            "group_update" -> handleGroupUpdateMessage(remoteMessage)
            "chat_message" -> handleChatMessage(remoteMessage)
            "trip_reminder" -> handleTripReminder(remoteMessage)
            "promotion" -> handlePromotionalMessage(remoteMessage)
            else -> handleGenericMessage(remoteMessage)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Update token in your backend if needed
        android.util.Log.d("FCM", "New FCM token: $token")
    }

    private fun handleGroupUpdateMessage(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        notificationManager.sendGroupUpdateNotification(
            groupId = data["group_id"] ?: "",
            groupDestination = data["destination"] ?: "",
            memberName = data["member_name"] ?: "",
            action = data["action"] ?: ""
        )
    }

    private fun handleChatMessage(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        notificationManager.sendChatMessageNotification(
            groupId = data["group_id"] ?: "",
            groupDestination = data["destination"] ?: "",
            senderName = data["sender_name"] ?: "",
            message = data["message"] ?: ""
        )
    }

    private fun handleTripReminder(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        notificationManager.sendTripReminderNotification(
            groupId = data["group_id"] ?: "",
            destination = data["destination"] ?: "",
            scheduledTime = data["scheduled_time"] ?: "",
            minutesBefore = data["minutes_before"]?.toIntOrNull() ?: 15
        )
    }

    private fun handlePromotionalMessage(remoteMessage: RemoteMessage) {
        notificationManager.sendPromotionalNotification(
            title = remoteMessage.notification?.title ?: "",
            message = remoteMessage.notification?.body ?: "",
            actionUrl = remoteMessage.data["action_url"]
        )
    }

    private fun handleGenericMessage(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let { notification ->
            notificationManager.sendPromotionalNotification(
                title = notification.title ?: "",
                message = notification.body ?: ""
            )
        }
    }
}