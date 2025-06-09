package com.dawitf.akahidegn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing recent searches for autocomplete functionality.
 */
@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey
    val id: String,
    val query: String,
    val destination: String,
    val timestamp: Long,
    val usageCount: Int
)

/**
 * Entity for storing popular destinations analytics.
 */
@Entity(tableName = "popular_destinations")
data class PopularDestinationEntity(
    @PrimaryKey
    val name: String,
    val count: Int,
    val distance: Double,
    val lastUpdated: Long
)

/**
 * Entity for caching search results temporarily.
 */
@Entity(tableName = "search_cache")
data class SearchCacheEntity(
    @PrimaryKey
    val cacheKey: String,
    val groupIds: String, // JSON array of group IDs
    val timestamp: Long,
    val filters: String   // JSON representation of SearchFilters
)

/**
 * Entity for user preferences and settings.
 */
@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey
    val key: String,
    val value: String,
    val type: String // "string", "boolean", "int", "float", "long"
)

/**
 * Entity for user analytics and usage tracking.
 */
@Entity(tableName = "user_analytics")
data class UserAnalyticsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventType: String,
    val eventData: String, // JSON data
    val timestamp: Long,
    val uploaded: Boolean = false
)

/**
 * Enhanced Group entity with additional fields for performance and features.
 */
@Entity(
    tableName = "groups_enhanced",
    indices = [
        androidx.room.Index(value = ["destinationName"]),
        androidx.room.Index(value = ["pickupLat", "pickupLng"]),
        androidx.room.Index(value = ["timestamp"]),
        androidx.room.Index(value = ["pricePerPerson"]),
        androidx.room.Index(value = ["departureTime"]),
        androidx.room.Index(value = ["memberCount"]),
        androidx.room.Index(value = ["availableSeats"])
    ]
)
data class GroupEntityEnhanced(
    @PrimaryKey
    val groupId: String,
    val creatorId: String?,
    val creatorCloudflareId: String?,
    val destinationName: String?,
    val pickupLat: Double?,
    val pickupLng: Double?,
    val timestamp: Long?,
    val maxMembers: Int = 4,
    val memberCount: Int = 0,
    val imageUrl: String?,
    
    // Enhanced fields
    val pricePerPerson: Double?,
    val departureTime: Long?,
    val availableSeats: Int,
    val description: String?,
    val contactInfo: String?,
    val vehicleType: String?,
    val route: String?, // JSON representation of route points
    val tags: String?, // JSON array of tags
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val isActive: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis(),
    
    // Cached calculation fields for performance
    val distanceFromUser: Double? = null,
    val popularityScore: Float = 0f
)
