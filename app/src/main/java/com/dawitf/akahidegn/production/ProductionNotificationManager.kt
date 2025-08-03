package com.dawitf.akahidegn.production

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dawitf.akahidegn.MainActivity
import com.dawitf.akahidegn.R
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production push notification system
 * Handles group notifications, chat messages, trip reminders, and user engagement
 */
@Singleton
class ProductionNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        const val CHANNEL_GROUP_UPDATES = "group_updates"
        const val CHANNEL_CHAT_MESSAGES = "chat_messages"
        const val CHANNEL_TRIP_REMINDERS = "trip_reminders"
        const val CHANNEL_PROMOTIONS = "promotions"
        
        const val NOTIFICATION_GROUP_UPDATE = 1001
        const val NOTIFICATION_CHAT_MESSAGE = 1002
        const val NOTIFICATION_TRIP_REMINDER = 1003
        const val NOTIFICATION_PROMOTION = 1004
    }
    
    private val firebaseMessaging = FirebaseMessaging.getInstance()
    private val notificationManager = NotificationManagerCompat.from(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _notificationState = MutableStateFlow(NotificationState())
    val notificationState: StateFlow<NotificationState> = _notificationState
    
    data class NotificationState(
        val fcmToken: String? = null,
        val subscriptions: Set<String> = emptySet(),
        val notificationsSent: Int = 0,
        val isEnabled: Boolean = true
    )
    
    init {
        createNotificationChannels()
        initializeFCM()
    }
    
    /**
     * Initialize FCM and get token
     */
    private fun initializeFCM() {
        scope.launch {
            try {
                val token = firebaseMessaging.token.await()
                _notificationState.value = _notificationState.value.copy(fcmToken = token)
                
                // Subscribe to default topics
                subscribeToTopic("general_updates")
                subscribeToTopic("app_announcements")
            } catch (e: Exception) {
                // Handle FCM initialization error
                android.util.Log.e("FCM", "Failed to get FCM token", e)
            }
        }
    }
    
    /**
     * Create notification channels for Android O+
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_GROUP_UPDATES,
                    "የቡድን ዝመናዎች",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "ከቡድን መቀላቀል እና መለቀቅ ጋር የተገኙ ማስታወቂያዎች"
                },
                
                NotificationChannel(
                    CHANNEL_CHAT_MESSAGES,
                    "የቻት መልዕክቶች",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "ከቡድን ቻት የመጡ መልዕክቶች"
                },
                
                NotificationChannel(
                    CHANNEL_TRIP_REMINDERS,
                    "የጉዞ ማስታወሻዎች",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "የጉዞ ጊዜ ማስታወሻዎች"
                },
                
                NotificationChannel(
                    CHANNEL_PROMOTIONS,
                    "ማስታወቂያዎች",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "አዳዲስ ባህሪያት እና ማስታወቂያዎች"
                }
            )
            
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { channel ->
                systemNotificationManager.createNotificationChannel(channel)
            }
        }
    }
    
    /**
     * Send group join/leave notification
     */
    fun sendGroupUpdateNotification(
        groupId: String,
        groupDestination: String,
        memberName: String,
        action: String // "joined" or "left"
    ) {
        val actionText = if (action == "joined") "ተቀላቅሏል" else "ሄዷል"
        val title = "የቡድን ዝመና"
        val message = "$memberName ወደ $groupDestination ቡድን $actionText"
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("group_id", groupId)
            putExtra("action", "open_group")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            NOTIFICATION_GROUP_UPDATE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_GROUP_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_GROUP_UPDATE, notification)
        incrementNotificationCount()
    }
    
    /**
     * Send chat message notification
     */
    fun sendChatMessageNotification(
        groupId: String,
        groupDestination: String,
        senderName: String,
        message: String
    ) {
        val title = "የአዲስ መልዕክት"
        val content = "$senderName: $message"
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("group_id", groupId)
            putExtra("action", "open_chat")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_CHAT_MESSAGE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_CHAT_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$title - $groupDestination")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .build()
        
        notificationManager.notify(NOTIFICATION_CHAT_MESSAGE, notification)
        incrementNotificationCount()
    }
    
    /**
     * Send trip reminder notification
     */
    fun sendTripReminderNotification(
        groupId: String,
        destination: String,
        scheduledTime: String,
        minutesBefore: Int
    ) {
        val title = "የጉዞ ማስታወሻ"
        val message = "ወደ $destination ያለዎት ጉዞ በ$minutesBefore ደቂቃዎች ውስጥ ይጀምራል"
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("group_id", groupId)
            putExtra("action", "open_group")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_TRIP_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_TRIP_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
        
        notificationManager.notify(NOTIFICATION_TRIP_REMINDER, notification)
        incrementNotificationCount()
    }
    
    /**
     * Send promotional notification
     */
    fun sendPromotionalNotification(
        title: String,
        message: String,
        actionUrl: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            actionUrl?.let { putExtra("action_url", it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_PROMOTION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_PROMOTIONS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_PROMOTION, notification)
        incrementNotificationCount()
    }
    
    /**
     * Subscribe to FCM topic
     */
    suspend fun subscribeToTopic(topic: String): Boolean {
        return try {
            firebaseMessaging.subscribeToTopic(topic).await()
            val currentSubscriptions = _notificationState.value.subscriptions.toMutableSet()
            currentSubscriptions.add(topic)
            _notificationState.value = _notificationState.value.copy(subscriptions = currentSubscriptions)
            true
        } catch (e: Exception) {
            android.util.Log.e("FCM", "Failed to subscribe to topic: $topic", e)
            false
        }
    }
    
    /**
     * Unsubscribe from FCM topic
     */
    suspend fun unsubscribeFromTopic(topic: String): Boolean {
        return try {
            firebaseMessaging.unsubscribeFromTopic(topic).await()
            val currentSubscriptions = _notificationState.value.subscriptions.toMutableSet()
            currentSubscriptions.remove(topic)
            _notificationState.value = _notificationState.value.copy(subscriptions = currentSubscriptions)
            true
        } catch (e: Exception) {
            android.util.Log.e("FCM", "Failed to unsubscribe from topic: $topic", e)
            false
        }
    }
    
    /**
     * Enable/disable notifications
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationState.value = _notificationState.value.copy(isEnabled = enabled)
    }
    
    /**
     * Get FCM token
     */
    fun getFCMToken(): String? {
        return _notificationState.value.fcmToken
    }
    
    /**
     * Clear all notifications
     */
    fun clearAllNotifications() {
        notificationManager.cancelAll()
    }
    
    private fun incrementNotificationCount() {
        scope.launch {
            _notificationState.value = _notificationState.value.copy(
                notificationsSent = _notificationState.value.notificationsSent + 1
            )
        }
    }
    
    /**
     * Get notification statistics
     */
    fun getNotificationStats(): Map<String, Any> {
        val state = _notificationState.value
        return mapOf(
            "fcm_token" to (state.fcmToken ?: "Not available"),
            "subscriptions" to state.subscriptions.toList(),
            "notifications_sent" to state.notificationsSent,
            "notifications_enabled" to state.isEnabled
        )
    }
}
