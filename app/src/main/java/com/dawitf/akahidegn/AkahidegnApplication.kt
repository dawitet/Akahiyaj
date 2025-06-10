package com.dawitf.akahidegn

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dawitf.akahidegn.service.GroupCleanupScheduler
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
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
        
        // Initialize Firebase Database persistence first
        initializeFirebaseDatabase()
        
        // Initialize Firebase Authentication
        initializeFirebaseAuth()
        
        // Schedule group cleanup task
        initializeGroupCleanup()
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
