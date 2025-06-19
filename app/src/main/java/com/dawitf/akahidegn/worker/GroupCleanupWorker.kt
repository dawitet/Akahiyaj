package com.dawitf.akahidegn.worker

import android.content.Context
import com.dawitf.akahidegn.utils.AppLog
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

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        AppLog.d(TAG, "Starting group cleanup task")
        
        return try {
            when (val cleanupResult = groupRepository.cleanupExpiredGroups()) {
                is com.dawitf.akahidegn.core.result.Result.Success -> {
                    val deletedCount = cleanupResult.data
                    AppLog.d(TAG, "Successfully cleaned up $deletedCount expired groups")
                    androidx.work.ListenableWorker.Result.success()
                }
                is com.dawitf.akahidegn.core.result.Result.Error -> {
                    AppLog.e(TAG, "Failed to cleanup expired groups: ${cleanupResult.error}")
                    androidx.work.ListenableWorker.Result.retry()
                }
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Group cleanup failed with exception", e)
            androidx.work.ListenableWorker.Result.retry()
        }
    }
}
