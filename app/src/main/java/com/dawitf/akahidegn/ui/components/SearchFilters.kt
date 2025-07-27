package com.dawitf.akahidegn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dawitf.akahidegn.domain.model.SearchFilters

// Removed duplicate SearchFilters data class - using the one from domain.model package

// Helper function to filter groups based on search filters
fun filterGroups(groups: List<com.dawitf.akahidegn.Group>, filters: SearchFilters): List<com.dawitf.akahidegn.Group> {
    return groups.filter { group ->
        // Destination filter
        val destinationMatch = if (filters.destination.isNotBlank()) {
            group.destinationName?.contains(filters.destination, ignoreCase = true) == true
        } else true

        // Max members filter
        val maxMembersMatch = filters.maxMembers?.let { maxMembers ->
            group.maxMembers <= maxMembers
        } ?: true

        // Available seats filter
        val availableSeatsMatch = if (filters.availableSeatsOnly) {
            group.memberCount < group.maxMembers
        } else true

        destinationMatch && maxMembersMatch && availableSeatsMatch
    }
}
