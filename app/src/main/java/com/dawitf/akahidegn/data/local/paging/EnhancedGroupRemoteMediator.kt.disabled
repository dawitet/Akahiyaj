package com.dawitf.akahidegn.data.local.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.dawitf.akahidegn.data.local.dao.EnhancedGroupDao
import com.dawitf.akahidegn.data.local.database.AkahidegnDatabase
import com.dawitf.akahidegn.data.local.entity.GroupEntityEnhanced
import com.dawitf.akahidegn.data.mapper.toEnhancedEntity
import com.dawitf.akahidegn.data.remote.service.FirebaseGroupService
import com.dawitf.akahidegn.domain.model.SearchFilters
import com.dawitf.akahidegn.util.LocationUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Remote mediator for enhanced group paging with Firebase backend synchronization.
 */
@OptIn(ExperimentalPagingApi::class)
class EnhancedGroupRemoteMediator @Inject constructor(
    private val database: AkahidegnDatabase,
    private val groupDao: EnhancedGroupDao,
    private val remoteDataSource: FirebaseGroupService,
    private val userLatitude: Double,
    private val userLongitude: Double,
    private val filters: SearchFilters
) : RemoteMediator<Int, GroupEntityEnhanced>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, GroupEntityEnhanced>
    ): MediatorResult {
        return try {
            val loadKey = when (loadType) {
                LoadType.REFRESH -> 0
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val lastItem = state.lastItemOrNull()
                    if (lastItem == null) {
                        0
                    } else {
                        // Calculate offset based on current page
                        state.pages.sumOf { it.data.size }
                    }
                }
            }

            // Fetch data from Firebase using available method
            val response = remoteDataSource.getNearbyGroups(
                latitude = userLatitude,
                longitude = userLongitude,
                radiusKm = 50.0 // 50km radius
            ).first()

            if (response.isFailure) {
                return MediatorResult.Error(
                    Exception("Failed to load groups")
                )
            }

            val groups = response.getOrNull() ?: emptyList()
            
            // Convert to enhanced entities with distance calculations
            val enhancedGroups = groups.map { group ->
                val pickupLat = group.pickupLat
                val pickupLng = group.pickupLng
                val distance = if (pickupLat != null && pickupLng != null) {
                    LocationUtils.calculateDistance(
                        userLatitude, userLongitude,
                        pickupLat, pickupLng
                    )
                } else {
                    null
                }
                
                group.toEnhancedEntity().copy(
                    distanceFromUser = distance,
                    popularityScore = calculatePopularityScore(group.memberCount, group.timestamp ?: 0L)
                )
            }

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    // Clear cache for refresh
                    // Note: Only clear groups that match current filters to avoid clearing unrelated data
                    groupDao.clearFilteredGroups(filters.hashCode().toString())
                }
                
                // Insert new groups
                groupDao.insertGroups(enhancedGroups)
            }

            MediatorResult.Success(
                endOfPaginationReached = groups.size < state.config.pageSize
            )
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    /**
     * Calculate popularity score based on member count and recency.
     */
    private fun calculatePopularityScore(memberCount: Int, timestamp: Long): Float {
        val currentTime = System.currentTimeMillis()
        val ageInHours = (currentTime - timestamp) / (1000 * 60 * 60)
        val recencyScore = maxOf(0f, 1f - (ageInHours / 24f)) // Decay over 24 hours
        val memberScore = memberCount / 4f // Normalize by max members
        return (recencyScore * 0.6f + memberScore * 0.4f).coerceIn(0f, 1f)
    }
}
