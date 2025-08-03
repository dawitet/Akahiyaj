package com.dawitf.akahidegn.domain.model

data class LatLng(val latitude: Double, val longitude: Double)

data class SearchFilters(
    val destination: String = "",
    val pickupLocation: LatLng? = null,
    val timeRange: ClosedRange<Long> = 0L..Long.MAX_VALUE,
    val maxMembers: Int? = null,
    val availableSeatsOnly: Boolean = false,
    val sortBy: SortOption = SortOption.NEAREST,
    val maxDistance: Double = Double.MAX_VALUE
) {
    fun hasActiveFilters(): Boolean {
        return destination.isNotBlank() ||
                pickupLocation != null ||
                timeRange.start > 0L ||
                timeRange.endInclusive < Long.MAX_VALUE ||
                maxMembers != null ||
                availableSeatsOnly ||
                maxDistance < Double.MAX_VALUE
    }
}

enum class SortOption {
    NEAREST,
    DEPARTURE_TIME,
    MOST_POPULAR,
    NEWEST,
    AVAILABLE_SEATS
}
