package com.dawitf.akahidegn.service

import android.content.Context
import android.util.Log
import androidx.work.*
import com.dawitf.akahidegn.worker.GroupCleanupWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for scheduling and managing the group cleanup background task.
 */
@Singleton
class GroupCleanupScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        const val TAG = "GroupCleanupScheduler"
        private const val CLEANUP_INTERVAL_MINUTES = 30L
    }
    
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Schedules the periodic group cleanup task.
     * The task will run every 30 minutes to clean up groups older than 30 minutes.
     */
    fun scheduleGroupCleanup() {
        Log.d(TAG, "Scheduling group cleanup task")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val cleanupRequest = PeriodicWorkRequestBuilder<GroupCleanupWorker>(
            CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES,
            // Flex interval (the task can run within this window)
            10, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(GroupCleanupWorker.TAG)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            GroupCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
        
        Log.d(TAG, "Group cleanup task scheduled successfully")
    }
    
    /**
     * Cancels the scheduled group cleanup task.
     */
    fun cancelGroupCleanup() {
        Log.d(TAG, "Cancelling group cleanup task")
        workManager.cancelUniqueWork(GroupCleanupWorker.WORK_NAME)
    }
    
    /**
     * Manually triggers a group cleanup task to run immediately.
     */
    fun triggerImmediateCleanup() {
        Log.d(TAG, "Triggering immediate group cleanup")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val immediateCleanupRequest = OneTimeWorkRequestBuilder<GroupCleanupWorker>()
            .setConstraints(constraints)
            .addTag("${GroupCleanupWorker.TAG}_immediate")
            .build()
        
        workManager.enqueue(immediateCleanupRequest)
    }
    
    /**
     * Gets the current status of the group cleanup task.
     */
    fun getCleanupWorkStatus() = workManager.getWorkInfosForUniqueWorkLiveData(GroupCleanupWorker.WORK_NAME)
}
