package com.dawitf.akahidegn.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.dawitf.akahidegn.ACTION_ACCEPT" -> {
                Toast.makeText(context, "Action Accepted", Toast.LENGTH_SHORT).show()
                // Handle accept action
            }
            "com.dawitf.akahidegn.ACTION_DECLINE" -> {
                Toast.makeText(context, "Action Declined", Toast.LENGTH_SHORT).show()
                // Handle decline action
            }
        }
    }
}
