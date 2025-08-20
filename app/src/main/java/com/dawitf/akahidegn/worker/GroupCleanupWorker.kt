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
            groupRepository.getExpiredGroups(System.currentTimeMillis() - 30 * 60 * 1000).let { result ->
                return if (result is com.dawitf.akahidegn.core.result.Result.Success<*>) {
                    val groups = result.data as List<com.dawitf.akahidegn.Group>
                    groups.forEach { group ->
                        group.groupId?.let { groupId ->
                            groupRepository.deleteGroup(groupId)
                        }
                    }
                    Log.d(TAG, "Successfully cleaned up ${groups.size} expired groups")
                    Result.success()
                } else if (result is com.dawitf.akahidegn.core.result.Result.Error) {
                    Log.e(TAG, "Failed to cleanup expired groups: ${result.error}")
                    Result.retry()
                } else {
                    // Should not happen, but handle for exhaustiveness
                    Log.e(TAG, "Unexpected result type from getExpiredGroups")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Group cleanup failed with exception", e)
            Result.retry()
        }
    }
}
