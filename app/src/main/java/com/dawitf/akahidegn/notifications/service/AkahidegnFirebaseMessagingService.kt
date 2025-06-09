package com.dawitf.akahidegn.notifications.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dawitf.akahidegn.MainActivity
import com.dawitf.akahidegn.R
import com.dawitf.akahidegn.PreferenceManager
import com.dawitf.akahidegn.notifications.NotificationActionReceiver
import com.dawitf.akahidegn.notifications.model.NotificationData
import com.dawitf.akahidegn.notifications.model.NotificationPriority
import com.dawitf.akahidegn.notifications.model.NotificationType
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import javax.inject.Inject

@AndroidEntryPoint
class AkahidegnFirebaseMessagingService : FirebaseMessagingService() {
    
    // TODO: Uncomment when these manager classes are implemented
    /*
    @Inject
    lateinit var notificationManager: NotificationManagerService
    
    @Inject
    lateinit var preferencesManager: NotificationPreferencesManager
    */
    
    companion object {
        const val CHANNEL_ID_DEFAULT = "default_channel"
        const val CHANNEL_ID_CHAT = "chat_channel"
        const val CHANNEL_ID_TRIPS = "trips_channel"
        const val CHANNEL_ID_SYSTEM = "system_channel"
        const val REQUEST_CODE_DEFAULT = 1001
        const val REQUEST_CODE_CHAT = 1002
        const val REQUEST_CODE_TRIPS = 1003
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        // Parse notification data
        val notificationData = parseRemoteMessage(remoteMessage)
        
        // Check if notifications are enabled for this type
        CoroutineScope(Dispatchers.IO).launch {
            val preferences = PreferenceManager.getNotificationPreferences(this@AkahidegnFirebaseMessagingService)
            if (shouldShowNotification(notificationData, preferences)) {
                showNotification(notificationData)
            }
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // Send token to server
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // In a real app, you would send this to your backend
                // FirebaseTokenService.updateToken(token)
            } catch (e: Exception) {
                // Handle token update failure
            }
        }
    }
    
    private fun parseRemoteMessage(remoteMessage: RemoteMessage): NotificationData {
        val data = remoteMessage.data
        val notification = remoteMessage.notification
        
        return NotificationData(
            type = data["type"]?.let { 
                try { NotificationType.valueOf(it) } 
                catch (e: Exception) { NotificationType.SYSTEM_ANNOUNCEMENT }
            } ?: NotificationType.SYSTEM_ANNOUNCEMENT,
            title = notification?.title ?: data["title"] ?: "አካሂያጅ",
            body = notification?.body ?: data["body"] ?: "",
            groupId = data["groupId"],
            userId = data["userId"],
            messageId = data["messageId"],
            imageUrl = notification?.imageUrl?.toString() ?: data["imageUrl"],
            actionUrl = data["actionUrl"],
            timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis(),
            priority = data["priority"]?.let {
                try { NotificationPriority.valueOf(it) }
                catch (e: Exception) { NotificationPriority.NORMAL }
            } ?: NotificationPriority.NORMAL,
            category = data["category"],
            sound = data["sound"],
            data = data
        )
    }
    
    private suspend fun shouldShowNotification(
        notificationData: NotificationData,
        preferences: com.dawitf.akahidegn.domain.model.NotificationSettings
    ): Boolean {
        if (!preferences.notificationsEnabled) return false
        
        return when (notificationData.type) {
            NotificationType.GROUP_JOIN_REQUEST, 
            NotificationType.GROUP_MEMBER_JOINED,
            NotificationType.GROUP_MEMBER_LEFT -> preferences.chatNotificationsEnabled
            NotificationType.GROUP_CHAT_MESSAGE -> preferences.chatNotificationsEnabled
            NotificationType.TRIP_REMINDER,
            NotificationType.TRIP_STARTING_SOON,
            NotificationType.TRIP_CANCELLED -> preferences.tripNotificationsEnabled
            NotificationType.SYSTEM_ANNOUNCEMENT,
            NotificationType.PROMOTION -> preferences.systemNotificationsEnabled
        } && !isInQuietHours(preferences)
    }
    
    private fun isInQuietHours(preferences: com.dawitf.akahidegn.domain.model.NotificationSettings): Boolean {
        if (!preferences.quietHoursEnabled) return false
        
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)
        val currentTimeMinutes = currentHour * 60 + currentMinute
        
        val startHourMinutes = parseTimeString(preferences.quietHoursStart)
        val endHourMinutes = parseTimeString(preferences.quietHoursEnd)
        
        return if (startHourMinutes <= endHourMinutes) {
            currentTimeMinutes in startHourMinutes..endHourMinutes
        } else {
            currentTimeMinutes >= startHourMinutes || currentTimeMinutes <= endHourMinutes
        }
    }
    
    private fun parseTimeString(timeString: String): Int {
        val parts = timeString.split(":")
        if (parts.size == 2) {
            val hour = parts[0].toIntOrNull() ?: 0
            val minute = parts[1].toIntOrNull() ?: 0
            return hour * 60 + minute
        }
        return 0
    }
    
    private suspend fun showNotification(notificationData: NotificationData) {
        withContext(Dispatchers.Main) {
            val channelId = getChannelId(notificationData.type)
            val intent = createNotificationIntent(notificationData)
            val pendingIntent = PendingIntent.getActivity(
                this@AkahidegnFirebaseMessagingService,
                getRequestCode(notificationData.type),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val builder = NotificationCompat.Builder(this@AkahidegnFirebaseMessagingService, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(notificationData.title)
                .setContentText(notificationData.body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(getNotificationPriority(notificationData.priority))
                .setCategory(getNotificationCategory(notificationData.type))
            
            // Add sound if enabled
            val preferences = PreferenceManager.getNotificationPreferences(this@AkahidegnFirebaseMessagingService)
            if (preferences.soundEnabled) {
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            }
            
            // Add vibration if enabled
            if (preferences.vibrationEnabled) {
                builder.setVibrate(longArrayOf(0, 250, 250, 250))
            }
            
            // Load large icon if image URL is provided
            notificationData.imageUrl?.let { imageUrl ->
                loadImageFromUrl(imageUrl)?.let { bitmap ->
                    builder.setLargeIcon(bitmap)
                    builder.setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .bigLargeIcon(null as Bitmap?)
                    )
                }
            }
            
            // Add action buttons based on notification type
            addNotificationActions(builder, notificationData)
            
            val notificationId = notificationData.hashCode()
            
            try {
                NotificationManagerCompat.from(this@AkahidegnFirebaseMessagingService)
                    .notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                // Handle missing notification permission
            }
        }
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_DEFAULT,
                    "ዋና ማሳወቂያዎች",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "ዋና የመተግበሪያ ማሳወቂያዎች"
                },
                
                NotificationChannel(
                    CHANNEL_ID_CHAT,
                    "የውይይት ማሳወቂያዎች",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "የቡድን ውይይት ማሳወቂያዎች"
                },
                
                NotificationChannel(
                    CHANNEL_ID_TRIPS,
                    "የጉዞ ማሳወቂያዎች",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "የጉዞ ሪሚንደሮች እና ማሳወቂያዎች"
                },
                
                NotificationChannel(
                    CHANNEL_ID_SYSTEM,
                    "የስርዓት ማሳወቂያዎች",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "የስርዓት አዘጋጆች እና ማሳወቂያዎች"
                }
            )
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
    
    private fun getChannelId(type: NotificationType): String {
        return when (type) {
            NotificationType.GROUP_CHAT_MESSAGE -> CHANNEL_ID_CHAT
            NotificationType.TRIP_REMINDER,
            NotificationType.TRIP_STARTING_SOON,
            NotificationType.TRIP_CANCELLED -> CHANNEL_ID_TRIPS
            NotificationType.SYSTEM_ANNOUNCEMENT,
            NotificationType.PROMOTION -> CHANNEL_ID_SYSTEM
            else -> CHANNEL_ID_DEFAULT
        }
    }
    
    private fun getRequestCode(type: NotificationType): Int {
        return when (type) {
            NotificationType.GROUP_CHAT_MESSAGE -> REQUEST_CODE_CHAT
            NotificationType.TRIP_REMINDER,
            NotificationType.TRIP_STARTING_SOON,
            NotificationType.TRIP_CANCELLED -> REQUEST_CODE_TRIPS
            else -> REQUEST_CODE_DEFAULT
        }
    }
    
    private fun createNotificationIntent(notificationData: NotificationData): Intent {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        
        // Add extra data based on notification type
        when (notificationData.type) {
            NotificationType.GROUP_JOIN_REQUEST,
            NotificationType.GROUP_MEMBER_JOINED,
            NotificationType.GROUP_MEMBER_LEFT,
            NotificationType.GROUP_CHAT_MESSAGE -> {
                notificationData.groupId?.let { groupId ->
                    intent.putExtra("groupId", groupId)
                    intent.putExtra("action", "open_group")
                }
            }
            NotificationType.TRIP_REMINDER,
            NotificationType.TRIP_STARTING_SOON,
            NotificationType.TRIP_CANCELLED -> {
                notificationData.groupId?.let { groupId ->
                    intent.putExtra("groupId", groupId)
                    intent.putExtra("action", "open_trip")
                }
            }
            else -> {
                notificationData.actionUrl?.let { url ->
                    intent.putExtra("actionUrl", url)
                }
            }
        }
        
        return intent
    }
    
    private fun getNotificationPriority(priority: NotificationPriority): Int {
        return when (priority) {
            NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
            NotificationPriority.NORMAL -> NotificationCompat.PRIORITY_DEFAULT
            NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            NotificationPriority.URGENT -> NotificationCompat.PRIORITY_MAX
        }
    }
    
    private fun getNotificationCategory(type: NotificationType): String {
        return when (type) {
            NotificationType.GROUP_CHAT_MESSAGE -> NotificationCompat.CATEGORY_MESSAGE
            NotificationType.GROUP_JOIN_REQUEST -> NotificationCompat.CATEGORY_SOCIAL
            NotificationType.TRIP_REMINDER,
            NotificationType.TRIP_STARTING_SOON -> NotificationCompat.CATEGORY_REMINDER
            NotificationType.TRIP_CANCELLED -> NotificationCompat.CATEGORY_EVENT
            else -> NotificationCompat.CATEGORY_RECOMMENDATION
        }
    }
    
    private fun addNotificationActions(
        builder: NotificationCompat.Builder,
        notificationData: NotificationData
    ) {
        when (notificationData.type) {
            NotificationType.GROUP_JOIN_REQUEST -> {
                // Add accept/decline actions
                val acceptIntent = createActionIntent("accept_join_request", notificationData)
                val declineIntent = createActionIntent("decline_join_request", notificationData)
                
                builder.addAction(R.drawable.ic_check, "ተቀበል", acceptIntent)
                builder.addAction(R.drawable.ic_close, "አትቀበል", declineIntent)
            }
            NotificationType.GROUP_CHAT_MESSAGE -> {
                // Add reply action
                val replyIntent = createActionIntent("reply_message", notificationData)
                builder.addAction(R.drawable.ic_reply, "ምላሽ ስጥ", replyIntent)
            }
            NotificationType.TRIP_STARTING_SOON -> {
                // Add "I'm ready" action
                val readyIntent = createActionIntent("trip_ready", notificationData)
                builder.addAction(R.drawable.ic_check, "ዝግጁ ነኝ", readyIntent)
            }
            else -> {
                // No specific actions
            }
        }
    }
    
    private fun createActionIntent(action: String, notificationData: NotificationData): PendingIntent {
        val intent = Intent(this, NotificationActionReceiver::class.java)
        intent.putExtra("action", action)
        intent.putExtra("notificationData", notificationData.toString()) // In real app, use proper serialization
        
        return PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private suspend fun loadImageFromUrl(url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection()
                connection.doInput = true
                connection.connect()
                val inputStream = connection.getInputStream()
                BitmapFactory.decodeStream(inputStream)
            } catch (e: IOException) {
                null
            }
        }
    }
}
