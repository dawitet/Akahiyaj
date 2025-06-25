package com.dawitf.akahidegn.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.dawitf.akahidegn.BuildConfig
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import java.security.MessageDigest

/**
 * Utility class to help with test device configuration, particularly for Samsung devices.
 */
object TestDeviceHelper {

    private const val TAG = "TestDeviceHelper"

    /**
     * Initializes the test device configuration for AdMob testing on Samsung devices.
     * Call this method in your Application class.
     */
    fun initializeTestDevices(context: Context) {
        try {
            Log.d(TAG, "Initializing test devices for AdMob...")
            
            // Hardcoded test device IDs for Samsung compatibility
            val testDeviceIds = mutableListOf<String>(
                "B3EEABB8EE11C2BE770B684D95219ECB" // Generic Samsung device hash - emulator
            )

            // Add the current device ID if running on a Samsung device
            if (isSamsungDevice()) {
                Log.d(TAG, "Samsung device detected, adding device ID to test devices")
                val deviceId = getDeviceId(context)
                Log.d(TAG, "Current device ID: $deviceId")
                if (!testDeviceIds.contains(deviceId)) {
                    testDeviceIds.add(deviceId)
                }
            } else {
                Log.d(TAG, "Non-Samsung device detected: ${Build.MANUFACTURER}")
                val deviceId = getDeviceId(context)
                Log.d(TAG, "Current device ID: $deviceId")
                if (!testDeviceIds.contains(deviceId)) {
                    testDeviceIds.add(deviceId)
                }
            }

            // Configure AdMob with the test device IDs
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            MobileAds.setRequestConfiguration(configuration)

            Log.d(TAG, "Test devices configured: ${testDeviceIds.joinToString()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize test devices", e)
        }
    }
    
    /**
     * Log device information for debugging
     */
    fun logDeviceInfo() {
        Log.d(TAG, "=== Device Information ===")
        Log.d(TAG, "Manufacturer: ${Build.MANUFACTURER}")
        Log.d(TAG, "Model: ${Build.MODEL}")
        Log.d(TAG, "Brand: ${Build.BRAND}")
        Log.d(TAG, "Device: ${Build.DEVICE}")
        Log.d(TAG, "Product: ${Build.PRODUCT}")
        Log.d(TAG, "Board: ${Build.BOARD}")
        Log.d(TAG, "Is Samsung: ${isSamsungDevice()}")
        Log.d(TAG, "==========================")
    }

    /**
     * Check if the current device is a Samsung device
     */
    fun isSamsungDevice(): Boolean {
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }

    /**
     * Gets the unique device ID for AdMob testing
     */
    private fun getDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return md5(androidId).uppercase()
    }

    /**
     * Generates an MD5 hash for the device ID
     */
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
