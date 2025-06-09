package com.dawitf.akahidegn.data.local.dao

import androidx.room.*
import com.dawitf.akahidegn.data.local.entity.RecentSearchEntity
import com.dawitf.akahidegn.data.local.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for search-related database operations.
 */
@Dao
interface SearchDao {
    
    // Recent Searches
    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSearches(limit: Int = 10): List<RecentSearchEntity>
    
    @Query("SELECT COUNT(*) FROM recent_searches")
    suspend fun getRecentSearchCount(): Int
    
    @Query("SELECT * FROM recent_searches WHERE query LIKE '%' || :query || '%' ORDER BY usageCount DESC, timestamp DESC LIMIT :limit")
    suspend fun searchRecentSearches(query: String, limit: Int = 5): List<RecentSearchEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSearch(search: RecentSearchEntity)
    
    @Query("UPDATE recent_searches SET usageCount = usageCount + 1, timestamp = :timestamp WHERE id = :id")
    suspend fun incrementSearchUsage(id: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM recent_searches WHERE timestamp < :cutoffTime")
    suspend fun deleteOldSearches(cutoffTime: Long)
    
    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()
    
    // Popular Destinations
    @Query("SELECT * FROM popular_destinations ORDER BY count DESC LIMIT :limit")
    suspend fun getPopularDestinations(limit: Int = 20): List<PopularDestinationEntity>
    
    @Query("SELECT * FROM popular_destinations WHERE name LIKE '%' || :query || '%' ORDER BY count DESC LIMIT :limit")
    suspend fun searchPopularDestinations(query: String, limit: Int = 10): List<PopularDestinationEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPopularDestination(destination: PopularDestinationEntity)
    
    @Query("UPDATE popular_destinations SET count = count + 1, lastUpdated = :timestamp WHERE name = :name")
    suspend fun incrementDestinationCount(name: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM popular_destinations WHERE lastUpdated < :cutoffTime")
    suspend fun deleteOldDestinations(cutoffTime: Long)
    
    // Search Cache
    @Query("SELECT * FROM search_cache WHERE cacheKey = :key AND timestamp > :validSince")
    suspend fun getCachedSearch(key: String, validSince: Long): SearchCacheEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchCache(cache: SearchCacheEntity)
    
    @Query("DELETE FROM search_cache WHERE timestamp < :cutoffTime")
    suspend fun deleteExpiredCache(cutoffTime: Long)
    
    @Query("DELETE FROM search_cache")
    suspend fun clearSearchCache()
    
    // Combined autocomplete query
    @Query("""
        SELECT query as text, 'RECENT_SEARCH' as type, destination as subtitle, 0 as distance, usageCount as popularity
        FROM recent_searches 
        WHERE query LIKE '%' || :query || '%'
        ORDER BY usageCount DESC, timestamp DESC
        LIMIT :recentLimit
    """)
    suspend fun getRecentSearchSuggestions(query: String, recentLimit: Int = 3): List<AutocompleteSuggestionRaw>
    
    @Query("""
        SELECT name as text, 'POPULAR_DESTINATION' as type, NULL as subtitle, distance, count as popularity
        FROM popular_destinations 
        WHERE name LIKE '%' || :query || '%'
        ORDER BY count DESC
        LIMIT :popularLimit
    """)
    suspend fun getPopularDestinationSuggestions(query: String, popularLimit: Int = 5): List<AutocompleteSuggestionRaw>
}

/**
 * Raw data class for autocomplete suggestions from database queries
 */
data class AutocompleteSuggestionRaw(
    val text: String,
    val type: String,
    val subtitle: String?,
    val distance: Double,
    val popularity: Int
)
