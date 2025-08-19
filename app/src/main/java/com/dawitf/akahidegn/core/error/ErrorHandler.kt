package com.dawitf.akahidegn.core.error

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ErrorHandler"
    }

    fun log(throwable: Throwable, tag: String = "ERROR") {
        Log.e(TAG, "[$tag] ${throwable.message}", throwable)

        // In a production app, you might want to send this to a crash reporting service
        // like Firebase Crashlytics, Sentry, etc.
    }

    fun toUserMessage(throwable: Throwable): String {
        return when {
            throwable.message?.contains("network", ignoreCase = true) == true ->
                "Network error. Please check your connection and try again."
            throwable.message?.contains("permission", ignoreCase = true) == true ->
                "Permission denied. Please check your app permissions."
            throwable.message?.contains("auth", ignoreCase = true) == true ->
                "Authentication error. Please sign in again."
            throwable.message?.contains("timeout", ignoreCase = true) == true ->
                "Request timed out. Please try again."
            else ->
                "Something went wrong. Please try again."
        }
    }

    fun handleError(throwable: Throwable, tag: String = "ERROR", showToUser: Boolean = false): String {
        log(throwable, tag)
        return if (showToUser) toUserMessage(throwable) else throwable.message ?: "Unknown error"
    }
}
