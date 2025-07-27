package com.dawitf.akahidegn.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupsMapViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadAllGroups()
    }

    fun loadAllGroups() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            groupRepository.getAllGroups()
                .catch { exception ->
                    _error.value = exception.message ?: "Unknown error occurred"
                    _isLoading.value = false
                }
                .collect { result ->
                    _isLoading.value = false
                    when (result) {
                        is com.dawitf.akahidegn.core.result.Result.Success -> {
                            _groups.value = result.data
                        }
                        is com.dawitf.akahidegn.core.result.Result.Error -> {
                            _error.value = result.error.toString()
                        }
                    }
                }
        }
    }

    fun loadNearbyGroups(latitude: Double, longitude: Double, radiusKm: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            groupRepository.getNearbyGroups(latitude, longitude, radiusKm)
                .catch { exception ->
                    _error.value = exception.message ?: "Unknown error occurred"
                    _isLoading.value = false
                }
                .collect { result ->
                    _isLoading.value = false
                    when (result) {
                        is com.dawitf.akahidegn.core.result.Result.Success -> {
                            _groups.value = result.data
                        }
                        is com.dawitf.akahidegn.core.result.Result.Error -> {
                            _error.value = result.error.toString()
                        }
                    }
                }
        }
    }

    fun refreshGroups() {
        loadAllGroups()
    }

    fun clearError() {
        _error.value = null
    }
}
