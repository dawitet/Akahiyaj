package com.dawitf.akahidegn.domain.model

import androidx.compose.runtime.Stable

/**
 * Data class representing search filters for enhanced group search functionality.
 */
@Stable
data class SearchFilters(
    val destination: String = "",
    val maxDistance: Double = 10.0, // in kilometers
    
    val timeRange: TimeRange = TimeRange(),
    val maxMembers: Int? = null,
    val availableSeatsOnly: Boolean = false,
    val sortBy: SortOption = SortOption.NEAREST
) {
    
    /**
     * Checks if any filters are active (non-default values).
     */
    fun hasActiveFilters(): Boolean {
        return destination.isNotBlank() ||
                maxDistance != 10.0 ||
                
                timeRange.hasLimits() ||
                maxMembers != null ||
                availableSeatsOnly ||
                sortBy != SortOption.NEAREST
    }
    
    /**
     * Returns a copy with cleared filters.
     */
    fun clear(): SearchFilters {
        return SearchFilters()
    }
}



/**
 * Time range filter for group search (departure time).
 */
@Stable
data class TimeRange(
    val start: Long? = null, // Unix timestamp
    val end: Long? = null    // Unix timestamp
) {
    fun hasLimits(): Boolean = start != null || end != null
    
    fun isInRange(time: Long): Boolean {
        return (start == null || time >= start) && (end == null || time <= end)
    }
}

/**
 * Sort options for search results with Amharic display names.
 */
enum class SortOption(val displayNameEn: String, val displayNameAm: String) {
    NEAREST("Nearest", "በቅርብ"),
    
    DEPARTURE_TIME("Departure Time", "የመነሻ ጊዜ"),
    MOST_POPULAR("Most Popular", "በጣም ተወዳጅ"),
    NEWEST("Newest First", "አዲስ በመጀመሪያ"),
    AVAILABLE_SEATS("Available Seats", "ነፃ መቀመጫዎች")
}

/**
 * Data class for recent search history.
 */
@Stable
data class RecentSearch(
    val id: String,
    val query: String,
    val destination: String,
    val timestamp: Long,
    val usageCount: Int
)

/**
 * Data class for autocomplete suggestions.
 */
@Stable
data class AutocompleteSuggestion(
    val text: String,
    val type: SuggestionType,
    val subtitle: String? = null,
    val distance: Double? = null,
    val popularity: Int = 0
)

/**
 * Types of autocomplete suggestions.
 */
enum class SuggestionType {
    RECENT_SEARCH,
    POPULAR_DESTINATION,
    NEARBY_LOCATION,
    SAVED_LOCATION
}
