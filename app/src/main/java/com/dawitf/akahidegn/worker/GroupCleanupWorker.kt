package com.dawitf.akahidegn.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dawitf.akahidegn.domain.repository.GroupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.dawitf.akahidegn.domain.model.Group

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
                return when (result) {
                    is com.dawitf.akahidegn.core.result.Result.Success<*> -> {
                        val data = result.data
                        if (data is List<*> && data.all { it is Group }) {
                            val groups = data.filterIsInstance<Group>()
                            groups.forEach { group ->
                                group.groupId?.let { groupId ->
                                    groupRepository.deleteGroup(groupId)
                                }
                            }
                            Log.d(TAG, "Successfully cleaned up ${groups.size} expired groups")
                            Result.success()
                        } else {
                            Log.w(TAG, "Unexpected data type from getExpiredGroups")
                            Result.failure()
                        }
                    }
                    is com.dawitf.akahidegn.core.result.Result.Error -> {
                        Log.e(TAG, "Failed to cleanup expired groups: ${result.error}")
                        Result.retry()
                    }
                    else -> {
                        Log.e(TAG, "Unexpected result type from getExpiredGroups")
                        Result.failure()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Group cleanup failed with exception", e)
            Result.retry()
        }
    }
}
