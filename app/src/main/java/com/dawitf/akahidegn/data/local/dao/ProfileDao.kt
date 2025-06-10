package com.dawitf.akahidegn.data.local.dao

import androidx.room.*
import com.dawitf.akahidegn.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM ride_statistics WHERE userId = :userId")
    fun getRideStatistics(userId: String): Flow<RideStatisticsEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRideStatistics(statistics: RideStatisticsEntity)
    
    @Query("SELECT * FROM achievements WHERE userId = :userId")
    fun getAchievements(userId: String): Flow<List<AchievementEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)
    
    @Query("SELECT * FROM carbon_footprint WHERE userId = :userId")
    fun getCarbonFootprint(userId: String): Flow<CarbonFootprintEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarbonFootprint(footprint: CarbonFootprintEntity)
    
    @Query("SELECT * FROM rides WHERE userId = :userId ORDER BY date DESC")
    fun getRides(userId: String): Flow<List<RideEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRides(rides: List<RideEntity>)
    
    @Query("DELETE FROM ride_statistics WHERE userId = :userId")
    suspend fun deleteRideStatistics(userId: String)
    
    @Query("DELETE FROM achievements WHERE userId = :userId")
    suspend fun deleteAchievements(userId: String)
    
    @Query("DELETE FROM carbon_footprint WHERE userId = :userId")
    suspend fun deleteCarbonFootprint(userId: String)
    
    @Query("DELETE FROM rides WHERE userId = :userId")
    suspend fun deleteRides(userId: String)
} 