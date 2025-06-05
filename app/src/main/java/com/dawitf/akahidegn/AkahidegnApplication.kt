package com.dawitf.akahidegn

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.dawitf.akahidegn.service.GroupCleanupScheduler
import com.google.firebase.database.FirebaseDatabase
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
        
        // Enable Firebase Database persistence
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
            Log.d("APP_INIT", "Firebase Database persistence enabled")
        } catch (e: Exception) {
            Log.w("APP_INIT", "Firebase persistence setup failed or already enabled: ${e.message}")
        }
        
        // Schedule group cleanup task
        try {
            groupCleanupScheduler.scheduleGroupCleanup()
            Log.d("APP_INIT", "Group cleanup scheduler initialized")
        } catch (e: Exception) {
            Log.e("APP_INIT", "Failed to initialize group cleanup scheduler: ${e.message}")
        }
    }
}
