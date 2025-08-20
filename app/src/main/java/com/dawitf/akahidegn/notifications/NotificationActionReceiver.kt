package com.dawitf.akahidegn.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.dawitf.akahidegn.ACTION_ACCEPT" -> {
                // Handle accept action
                Log.d("NotificationAction", "User accepted invitation")
            }
            "com.dawitf.akahidegn.ACTION_DECLINE" -> {
                // Handle decline action
                Log.d("NotificationAction", "User declined invitation")
            }
        }
    }
}
