package com.dawitf.akahidegn.data.local.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.dawitf.akahidegn.data.local.dao.EnhancedGroupDao
import com.dawitf.akahidegn.data.local.entity.GroupEntityEnhanced
import com.dawitf.akahidegn.domain.model.SearchFilters
import com.dawitf.akahidegn.util.LocationUtils
import javax.inject.Inject

/**
 * PagingSource for filtered group results with local caching.
 */
class FilteredGroupPagingSource @Inject constructor(
    private val dao: EnhancedGroupDao,
    private val userLat: Double,
    private val userLng: Double,
    private val filters: SearchFilters
) : PagingSource<Int, GroupEntityEnhanced>() {
    
    companion object {
        private const val STARTING_PAGE_INDEX = 0
        private const val PAGE_SIZE = 20
    }
    
    override fun getRefreshKey(state: PagingState<Int, GroupEntityEnhanced>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
    
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GroupEntityEnhanced> {
        return try {
            val page = params.key ?: STARTING_PAGE_INDEX
            val offset = page * PAGE_SIZE
            
            // Get filtered results from database
            val groups = getFilteredGroups(params.loadSize, offset)
            
            // Apply additional filtering and sorting that couldn't be done in SQL
            val processedGroups = processGroups(groups)
            
            LoadResult.Page(
                data = processedGroups,
                prevKey = if (page == STARTING_PAGE_INDEX) null else page - 1,
                nextKey = if (processedGroups.isEmpty()) null else page + 1
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }
    
    private suspend fun getFilteredGroups(loadSize: Int, offset: Int): List<GroupEntityEnhanced> {
        // Convert filters to database query parameters
        val destination = filters.destination.trim()
        
        val minTime = filters.timeRange.start
        val maxTime = filters.timeRange.end
        val maxMemberCount = filters.maxMembers
        val availableSeatsOnly = filters.availableSeatsOnly
        val sortBy = filters.sortBy.name
        
        // For now, we'll use a simple approach to get filtered results
        // In a real implementation, you might want to use a more sophisticated approach
        // with custom SQL queries or Room's support for complex filtering
        
        return if (filters.hasActiveFilters()) {
            // Use the filtered query from the DAO
            dao.getFilteredGroups(
                destination = destination,
                minTime = minTime,
                maxTime = maxTime,
                maxMemberCount = maxMemberCount,
                availableSeatsOnly = availableSeatsOnly,
                sortBy = sortBy
            ).let { pagingSource ->
                // Note: This is a simplified approach. In production, you'd want to
                // properly implement the paging with the PagingSource
                emptyList() // Placeholder - the actual implementation would use the PagingSource
            }
        } else {
            // Get all groups if no filters
            dao.getAllGroupsPaged().let { emptyList() } // Placeholder
        }
    }
    
    private suspend fun processGroups(groups: List<GroupEntityEnhanced>): List<GroupEntityEnhanced> {
        var processedGroups = groups
        
        // Calculate distances as userLat and userLng are always non-null Double
        processedGroups = processedGroups.map { group ->
            val distance = if (group.pickupLat != null && group.pickupLng != null) {
                LocationUtils.calculateDistance(
                    userLat, userLng,
                    group.pickupLat, group.pickupLng
                )
            } else null
            
            group.copy(distanceFromUser = distance)
        }
        
        // Apply distance filter if specified
        if (filters.maxDistance < Double.MAX_VALUE) {
            processedGroups = processedGroups.filter { group ->
                group.distanceFromUser?.let { it <= filters.maxDistance } ?: false
            }
        }
        
        // Apply custom sorting if needed
        processedGroups = when (filters.sortBy) {
            com.dawitf.akahidegn.domain.model.SortOption.NEAREST -> {
                processedGroups.sortedBy { it.distanceFromUser ?: Double.MAX_VALUE }
            }
            
            com.dawitf.akahidegn.domain.model.SortOption.DEPARTURE_TIME -> {
                processedGroups.sortedBy { it.departureTime ?: Long.MAX_VALUE }
            }
            com.dawitf.akahidegn.domain.model.SortOption.MOST_POPULAR -> {
                processedGroups.sortedByDescending { it.popularityScore }
            }
            com.dawitf.akahidegn.domain.model.SortOption.NEWEST -> {
                processedGroups.sortedByDescending { it.timestamp ?: 0L }
            }
            com.dawitf.akahidegn.domain.model.SortOption.AVAILABLE_SEATS -> {
                processedGroups.sortedByDescending { it.availableSeats }
            }
        }
        
        return processedGroups
    }
}
