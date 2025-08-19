package com.dawitf.akahidegn.domain.model

data class SearchFilters(
    val maxDistance: Double = 10.0, // km
    val maxMembers: Int = 4,
    val sortBy: SortBy = SortBy.DISTANCE,
    val showOnlyActive: Boolean = true,
    val timeWindow: TimeWindow = TimeWindow.NEXT_HOUR
)

enum class SortBy {
    DISTANCE,
    TIME_CREATED,
    MEMBER_COUNT
}

enum class TimeWindow {
    NEXT_HOUR,
    NEXT_3_HOURS,
    TODAY,
    ALL_TIME
}
