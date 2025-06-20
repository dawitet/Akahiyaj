package com.dawitf.akahidegn.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dawitf.akahidegn.domain.repository.GroupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker that periodically cleans up expired groups.
 * Groups older than 30 minutes are considered expired and will be removed.
 */
@HiltWorker
class GroupCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val groupRepository: GroupRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "GroupCleanupWorker"
        const val WORK_NAME = "group_cleanup_work"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting group cleanup task")
        
        return try {
            when (val cleanupResult = groupRepository.cleanupExpiredGroups()) {
                is com.dawitf.akahidegn.core.result.Result.Success -> {
                    val deletedCount = cleanupResult.data
                    Log.d(TAG, "Successfully cleaned up $deletedCount expired groups")
                    Result.success()
                }
                is com.dawitf.akahidegn.core.result.Result.Error -> {
                    Log.e(TAG, "Failed to cleanup expired groups: ${cleanupResult.error}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Group cleanup failed with exception", e)
            Result.retry()
        }
    }
}
