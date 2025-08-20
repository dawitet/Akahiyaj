package com.dawitf.akahidegn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawitf.akahidegn.activityhistory.ActivityHistoryRepository
import com.dawitf.akahidegn.domain.model.TripHistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityHistoryViewModel @Inject constructor(
    private val repository: ActivityHistoryRepository
) : ViewModel() {

    val uiState: StateFlow<ActivityHistoryUiState> = repository.historyItems
        .map { historyItems -> ActivityHistoryUiState(history = historyItems, isEmpty = historyItems.isEmpty()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActivityHistoryUiState())

    fun clear() {
        viewModelScope.launch { repository.clear() }
    }
}

data class ActivityHistoryUiState(
    val history: List<TripHistoryItem> = emptyList(),
    val isEmpty: Boolean = true
)
