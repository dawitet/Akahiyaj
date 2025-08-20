package com.dawitf.akahidegn.domain.model

/**
 * Sort options for filtering groups
 */
enum class SortOption(val displayName: String) {
    DISTANCE("Distance"),
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    MEMBERS_COUNT("Member Count"),
    AVAILABLE_SEATS("Available Seats");

    companion object {
        fun fromString(value: String): SortOption? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}

/**
 * Filter criteria for groups
 */
data class GroupFilter(
    val maxDistance: Int? = null,
    val minSeats: Int? = null,
    val maxSeats: Int? = null,
    val sortOption: SortOption = SortOption.DISTANCE,
    val availableSeatsOnly: Boolean = false
)
