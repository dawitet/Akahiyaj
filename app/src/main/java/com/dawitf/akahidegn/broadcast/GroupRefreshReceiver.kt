package com.dawitf.akahidegn.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class GroupRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Handle the broadcast message
        Log.d(TAG, "Broadcast received: ${intent.action}")
    }

    companion object {
        private const val TAG = "GroupRefreshReceiver"
    }
}
