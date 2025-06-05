package com.dawitf.akahidegn.worker

import androidx.work.DelegatingWorkerFactory
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import android.content.Context
import com.dawitf.akahidegn.domain.repository.GroupRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HiltWorkerFactory @Inject constructor(
    private val groupRepository: GroupRepository
) : WorkerFactory() {
    
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            GroupCleanupWorker::class.java.name ->
                GroupCleanupWorker(appContext, workerParameters, groupRepository)
            else -> null
        }
    }
}
