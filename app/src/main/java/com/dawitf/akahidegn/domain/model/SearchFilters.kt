package com.dawitf.akahidegn.domain.model

data class SearchFilters(
    val maxDistance: Double = 10.0, // km
    val maxMembers: Int = 4,
    val sortBy: SortOption = SortOption.DISTANCE,
    val showOnlyActive: Boolean = true,
    val timeWindow: TimeWindow = TimeWindow.NEXT_HOUR,
    val availableSeatsOnly: Boolean = false
)

enum class TimeWindow {
    NEXT_HOUR,
    NEXT_3_HOURS,
    TODAY,
    ALL_TIME
}
