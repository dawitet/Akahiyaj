package com.dawitf.akahidegn

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dawitf.akahidegn.service.GroupCleanupScheduler
import com.dawitf.akahidegn.util.TestDeviceHelper
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AkahidegnApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var groupCleanupScheduler: GroupCleanupScheduler
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Log device information for debugging
            TestDeviceHelper.logDeviceInfo()

            // Enable Samsung test device configuration
            TestDeviceHelper.initializeTestDevices(this)
            
            // Initialize Mobile Ads SDK
            initializeMobileAds()

            // Initialize Firebase Database persistence first
            initializeFirebaseDatabase()
            
            // Initialize Firebase Authentication
            initializeFirebaseAuth()
            
            // Schedule group cleanup task
            initializeGroupCleanup()
            
            Log.d("AkahidegnApp", "Application initialized successfully")
        } catch (e: Exception) {
            Log.e("AkahidegnApp", "Error during application initialization", e)
            // Don't crash the app, continue with basic functionality
        }
    }
    
    private fun initializeMobileAds() {
        try {
            MobileAds.initialize(this) { initializationStatus ->
                Log.d("APP_INIT", "Mobile Ads SDK initialized: $initializationStatus")
                
                // Log initialization status for each ad network
                initializationStatus.adapterStatusMap.forEach { (adapterClass, status) ->
                    Log.d("APP_INIT", "Adapter $adapterClass: ${status.initializationState} - ${status.description}")
                }
            }
            Log.d("APP_INIT", "Mobile Ads SDK initialization started")
        } catch (e: Exception) {
            Log.e("APP_INIT", "Mobile Ads SDK initialization failed: ${e.message}")
        }
    }
    
    private fun initializeFirebaseDatabase() {
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
            Log.d("APP_INIT", "Firebase Database persistence enabled")
        } catch (e: Exception) {
            Log.w("APP_INIT", "Firebase persistence setup failed or already enabled: ${e.message}")
        }
    }
    
    private fun initializeFirebaseAuth() {
        try {
            Firebase.auth
            Log.d("APP_INIT", "Firebase Authentication initialized")
        } catch (e: Exception) {
            Log.e("APP_INIT", "Firebase Authentication initialization failed: ${e.message}")
        }
    }
    
    private fun initializeGroupCleanup() {
        try {
            groupCleanupScheduler.scheduleGroupCleanup()
            Log.d("APP_INIT", "Group cleanup scheduler initialized")
        } catch (e: Exception) {
            Log.e("APP_INIT", "Group cleanup scheduler initialization failed: ${e.message}")
        }
    }
}
