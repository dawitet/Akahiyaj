package com.dawitf.akahidegn.activityhistory

import com.dawitf.akahidegn.domain.model.TripHistoryItem
import com.dawitf.akahidegn.core.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityHistoryRepository @Inject constructor() {
    private val _historyItems = MutableStateFlow<List<TripHistoryItem>>(emptyList())
    val historyItems: Flow<List<TripHistoryItem>> = _historyItems.asStateFlow()

    suspend fun add(item: TripHistoryItem): Result<Unit> {
        return try {
            val currentItems = _historyItems.value.toMutableList()
            currentItems.add(0, item) // Add to beginning of list
            _historyItems.value = currentItems
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to add history item: ${e.message}")
        }
    }

    suspend fun getAll(): Result<List<TripHistoryItem>> {
        return try {
            Result.Success(_historyItems.value)
        } catch (e: Exception) {
            Result.Error("Failed to get history items: ${e.message}")
        }
    }

    suspend fun getByGroupId(groupId: String): Result<TripHistoryItem?> {
        return try {
            val item = _historyItems.value.find { it.groupId == groupId }
            Result.Success(item)
        } catch (e: Exception) {
            Result.Error("Failed to get history item: ${e.message}")
        }
    }

    suspend fun clear(): Result<Unit> {
        return try {
            _historyItems.value = emptyList()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to clear history: ${e.message}")
        }
    }
}
