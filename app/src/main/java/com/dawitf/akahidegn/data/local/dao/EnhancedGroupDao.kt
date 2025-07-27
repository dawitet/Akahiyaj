package com.dawitf.akahidegn.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.dawitf.akahidegn.data.local.entity.GroupEntityEnhanced
import com.dawitf.akahidegn.data.local.entity.UserPreferencesEntity
import com.dawitf.akahidegn.domain.model.PopularDestination
import kotlinx.coroutines.flow.Flow

// Query result data classes for Room
data class PopularDestinationCount(
    val destinationName: String,
    val count: Int
)

data class LocationPopularDestination(
    val destinationName: String,
    val count: Int,
    val distance: Double
)

@Dao
interface EnhancedGroupDao {
    
    // Basic CRUD operations
    @Query("SELECT * FROM groups_enhanced WHERE isActive = 1 ORDER BY timestamp DESC")
    fun getAllGroupsPaged(): PagingSource<Int, GroupEntityEnhanced>
    
    @Query("SELECT * FROM groups_enhanced WHERE groupId = :groupId")
    suspend fun getGroupById(groupId: String): GroupEntityEnhanced?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntityEnhanced)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<GroupEntityEnhanced>)
    
    @Update
    suspend fun updateGroup(group: GroupEntityEnhanced)
    
    @Query("DELETE FROM groups_enhanced WHERE groupId = :groupId")
    suspend fun deleteGroupById(groupId: String)
    
    // Enhanced search and filtering
    @Query("""
        SELECT * FROM groups_enhanced 
        WHERE isActive = 1 
        AND (:destination = '' OR destinationName LIKE '%' || :destination || '%')

        AND (:minTime IS NULL OR departureTime >= :minTime)
        AND (:maxTime IS NULL OR departureTime <= :maxTime)
        AND (:maxMemberCount IS NULL OR maxMembers <= :maxMemberCount)
        AND (:availableSeatsOnly = 0 OR availableSeats > 0)
        ORDER BY 
        CASE WHEN :sortBy = 'NEAREST' THEN distanceFromUser END ASC,
        
        CASE WHEN :sortBy = 'DEPARTURE_TIME' THEN departureTime END ASC,
        CASE WHEN :sortBy = 'MOST_POPULAR' THEN popularityScore END DESC,
        CASE WHEN :sortBy = 'NEWEST' THEN timestamp END DESC,
        CASE WHEN :sortBy = 'AVAILABLE_SEATS' THEN availableSeats END DESC
    """)
    fun getFilteredGroups(
        destination: String,

        minTime: Long?,
        maxTime: Long?,
        maxMemberCount: Int?,
        availableSeatsOnly: Boolean,
        sortBy: String
    ): PagingSource<Int, GroupEntityEnhanced>
    
    // Location-based queries
    @Query("""
        SELECT * FROM groups_enhanced 
        WHERE isActive = 1 
        AND ((pickupLat - :userLat) * (pickupLat - :userLat) + 
             (pickupLng - :userLng) * (pickupLng - :userLng)) <= :maxDistanceSquared
        ORDER BY ((pickupLat - :userLat) * (pickupLat - :userLat) + 
                  (pickupLng - :userLng) * (pickupLng - :userLng)) ASC
        LIMIT :limit
    """)
    suspend fun getNearbyGroups(
        userLat: Double, 
        userLng: Double, 
        maxDistanceSquared: Double,
        limit: Int = 20
    ): List<GroupEntityEnhanced>
    
    // Popular destinations
    @Query("""
        SELECT destinationName, COUNT(*) as count
        FROM groups_enhanced 
        WHERE isActive = 1 AND destinationName IS NOT NULL
        GROUP BY destinationName 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    suspend fun getPopularDestinations(limit: Int = 10): List<PopularDestinationCount>
    
    // Analytics queries
    @Query("SELECT COUNT(*) FROM groups_enhanced WHERE creatorId = :userId AND isActive = 1")
    suspend fun getUserActiveGroupCount(userId: String): Int
    
    @Query("SELECT AVG(rating) FROM groups_enhanced WHERE creatorId = :userId AND rating > 0")
    suspend fun getUserAverageRating(userId: String): Float?
    
    @Query("""
        SELECT * FROM groups_enhanced 
        WHERE creatorId = :userId 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    suspend fun getUserGroups(userId: String, limit: Int = 50): List<GroupEntityEnhanced>
    
    // Performance optimization
    @Query("UPDATE groups_enhanced SET distanceFromUser = :distance WHERE groupId = :groupId")
    suspend fun updateGroupDistance(groupId: String, distance: Double)
    
    @Query("UPDATE groups_enhanced SET popularityScore = :score WHERE groupId = :groupId")
    suspend fun updatePopularityScore(groupId: String, score: Float)
    
    @Query("""
        UPDATE groups_enhanced 
        SET availableSeats = (maxMembers - memberCount) 
        WHERE groupId = :groupId
    """)
    suspend fun updateAvailableSeats(groupId: String)
    
    @Query("UPDATE groups_enhanced SET lastUpdated = :timestamp WHERE groupId = :groupId")
    suspend fun updateLastUpdated(groupId: String, timestamp: Long = System.currentTimeMillis())
    
    // Batch operations for performance
    @Query("DELETE FROM groups_enhanced WHERE lastUpdated < :cutoffTime")
    suspend fun deleteStaleGroups(cutoffTime: Long)
    
    @Query("UPDATE groups_enhanced SET isActive = 0 WHERE timestamp < :cutoffTime")
    suspend fun deactivateOldGroups(cutoffTime: Long)
    
    // Search text matching
    @Query("""
        SELECT * FROM groups_enhanced 
        WHERE isActive = 1 
        AND (destinationName LIKE '%' || :query || '%' 
             OR description LIKE '%' || :query || '%'
             OR tags LIKE '%' || :query || '%')
        ORDER BY 
        CASE 
            WHEN destinationName LIKE :query || '%' THEN 1
            WHEN destinationName LIKE '%' || :query || '%' THEN 2
            WHEN description LIKE '%' || :query || '%' THEN 3
            ELSE 4
        END,
        popularityScore DESC
        LIMIT :limit
    """)
    suspend fun searchGroups(query: String, limit: Int = 20): List<GroupEntityEnhanced>

    // Additional search methods needed by repository
    @Query("""
        SELECT DISTINCT destinationName FROM groups_enhanced 
        WHERE destinationName LIKE '%' || :query || '%' 
        AND destinationName IS NOT NULL 
        AND isActive = 1
        LIMIT :limit
    """)
    suspend fun getDestinationSuggestions(query: String, limit: Int): List<String>
    
    @Query("""
        DELETE FROM groups_enhanced 
        WHERE groupId IN (
            SELECT groupId FROM search_cache 
            WHERE cacheKey LIKE '%' || :filterHash || '%'
        )
    """)
    suspend fun clearFilteredGroups(filterHash: String)

    // Enhanced search with all filters
    @Query("""
        SELECT * FROM groups_enhanced 
        WHERE isActive = 1 
        AND (:destination IS NULL OR destinationName LIKE '%' || :destination || '%')

        AND (:startTime IS NULL OR departureTime >= :startTime)
        AND (:endTime IS NULL OR departureTime <= :endTime)
        AND (:maxMembers IS NULL OR maxMembers <= :maxMembers)
        AND (:availableSeatsOnly = 0 OR availableSeats > 0)
        AND ((pickupLat - :latitude) * (pickupLat - :latitude) + 
             (pickupLng - :longitude) * (pickupLng - :longitude)) <= :maxDistance
        ORDER BY timestamp DESC
    """)
    suspend fun searchGroups(
        destination: String?,
        latitude: Double,
        longitude: Double,
        maxDistance: Double,

        startTime: Long?,
        endTime: Long?,
        maxMembers: Int?,
        availableSeatsOnly: Boolean
    ): List<GroupEntityEnhanced>

    // Popular destinations with location filtering
    @Query("""
        SELECT destinationName, COUNT(*) as count, 
        AVG((pickupLat - :latitude) * (pickupLat - :latitude) + 
            (pickupLng - :longitude) * (pickupLng - :longitude)) as distance
        FROM groups_enhanced 
        WHERE isActive = 1 AND destinationName IS NOT NULL
        AND ((pickupLat - :latitude) * (pickupLat - :latitude) + 
             (pickupLng - :longitude) * (pickupLng - :longitude)) <= 100 -- ~10km radius
        GROUP BY destinationName 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    suspend fun getPopularDestinations(
        latitude: Double, 
        longitude: Double, 
        limit: Int
    ): List<LocationPopularDestination>

    // Missing methods required by OfflineManager
    @Query("DELETE FROM groups_enhanced")
    suspend fun deleteAllGroups()
    
    @Query("SELECT COUNT(*) FROM groups_enhanced")
    suspend fun getGroupCount(): Int
    
    @Query("SELECT COUNT(*) FROM recent_searches")
    suspend fun getRecentSearchCount(): Int
    
    @Query("SELECT COUNT(*) FROM user_preferences")
    suspend fun getUserPreferencesCount(): Int
    
    @Query("SELECT * FROM user_preferences")
    suspend fun getAllUserPreferences(): List<UserPreferencesEntity>
    
}
