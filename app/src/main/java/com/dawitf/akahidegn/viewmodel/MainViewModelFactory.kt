package com.dawitf.akahidegn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dawitf.akahidegn.domain.repository.GroupRepository
import com.dawitf.akahidegn.core.event.UiEventManager
import javax.inject.Inject

class MainViewModelFactory @Inject constructor(
    private val groupRepository: GroupRepository,
    private val uiEventManager: UiEventManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(groupRepository, uiEventManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
} 