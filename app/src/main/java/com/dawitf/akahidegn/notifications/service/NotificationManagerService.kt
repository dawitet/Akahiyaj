package com.dawitf.akahidegn.notifications.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.dawitf.akahidegn.MainActivity
import com.dawitf.akahidegn.R
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.notifications.service.NotificationPreferencesManager
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class NotificationManagerService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: NotificationPreferencesManager
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    companion object {
        private const val CHANNEL_ID = "group_events"
        private const val CHANNEL_NAME = "Group Events"
        private const val CHANNEL_DESCRIPTION = "Notifications for group activities"

        // Notification IDs
        private const val NOTIFICATION_ID_USER_JOINED = 1001
        private const val NOTIFICATION_ID_USER_LEFT = 1002
        private const val NOTIFICATION_ID_GROUP_FULL = 1003
        private const val NOTIFICATION_ID_GROUP_DISBANDED = 1004
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showUserJoinedNotification(group: Group, userName: String) {
        if (!preferencesManager.areNotificationsEnabled()) return

        val title = "👋 New Member Joined!"
        val message = "$userName joined your group to ${group.destinationName}"

        showNotification(
            NOTIFICATION_ID_USER_JOINED,
            title,
            message,
            group
        )

        playNotificationEffects()
    }

    fun showUserLeftNotification(group: Group, userName: String) {
        if (!preferencesManager.areNotificationsEnabled()) return

        val title = "👋 Member Left"
        val message = "$userName left the group to ${group.destinationName}"

        showNotification(
            NOTIFICATION_ID_USER_LEFT,
            title,
            message,
            group
        )

        playNotificationEffects()
    }

    fun showGroupFullNotification(group: Group) {
        if (!preferencesManager.areNotificationsEnabled()) return

        val title = "🚗 Group Full!"
        val message = "Your group to ${group.destinationName} is now full (${group.maxMembers}/${group.maxMembers})"

        showNotification(
            NOTIFICATION_ID_GROUP_FULL,
            title,
            message,
            group
        )

        playNotificationEffects()
    }

    fun showGroupDisbandedNotification(group: Group) {
        if (!preferencesManager.areNotificationsEnabled()) return

        val title = "⚠️ Group Disbanded"
        val message = "The group to ${group.destinationName} has been disbanded"

        showNotification(
            NOTIFICATION_ID_GROUP_DISBANDED,
            title,
            message,
            group
        )

        playNotificationEffects()
    }

    private fun showNotification(
        notificationId: Int,
        title: String,
        message: String,
        group: Group
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("group_id", group.groupId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        // Add sound if enabled
        if (preferencesManager.isSoundEnabled()) {
            notificationBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun playNotificationEffects() {
        // Play vibration if enabled
        if (preferencesManager.isVibrationEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(300)
            }
        }
    }

    fun playSuccessVibration() {
        if (preferencesManager.isVibrationEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 100, 50, 100)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val pattern = longArrayOf(0, 100, 50, 100)
                vibrator.vibrate(pattern, -1)
            }
        }
    }
}
