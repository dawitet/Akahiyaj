package com.dawitf.akahidegn.utils

import android.util.Log
import com.dawitf.akahidegn.BuildConfig

object AppLog {
    private val ENABLE_LOGGING = BuildConfig.DEBUG
    
    fun d(tag: String, message: String) {
        if (ENABLE_LOGGING) {
            Log.d(tag, message)
        }
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (ENABLE_LOGGING) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }
    
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (ENABLE_LOGGING) {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
    }
}
