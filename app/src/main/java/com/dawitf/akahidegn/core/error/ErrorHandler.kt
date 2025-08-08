package com.dawitf.akahidegn.core.error

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error handler / mapper for converting AppError (and unexpected Throwables)
 * into user-facing messages, logging, and optional analytics hooks.
 */
@Singleton
class ErrorHandler @Inject constructor() {
    fun toUserMessage(error: Throwable): String = when (error) {
        is AppError.NetworkError.NoInternet -> "No internet connection."
        is AppError.NetworkError.ConnectionTimeout -> "Connection timed out. Try again."
        is AppError.NetworkError.FirebaseError -> error.message
        is AppError.AuthenticationError.SessionExpired -> error.message
        is AppError.AuthenticationError.NotAuthenticated -> "Please sign in to continue."
        is AppError.ValidationError.InvalidInput -> error.message
        is AppError.ValidationError.ResourceNotFound -> "Requested item was not found."
        is AppError.RateLimitError.TooManyRequests -> "Too many requests. Slow down."
        is AppError.LocationError.PermissionDenied -> "Location permission denied."
        is AppError.UnknownError -> error.message
        else -> error.message ?: "Unexpected error occurred."
    }

    fun log(error: Throwable, context: String? = null) {
        val tag = "AppError"
        Log.e(tag, "${context ?: "Unhandled"}: ${error::class.simpleName} -> ${error.message}", error)
    }
}
