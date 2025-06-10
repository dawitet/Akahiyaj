package com.dawitf.akahidegn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.dawitf.akahidegn.data.local.entity.DateConverter
import java.util.Date

@Entity(tableName = "ride_statistics")
@TypeConverters(DateConverter::class)
data class RideStatisticsEntity(
    @PrimaryKey val userId: String,
    val totalRides: Int,
    val completedRides: Int,
    val cancelledRides: Int,
    val totalDistance: Double,
    val totalDuration: Long,
    val averageRating: Float,
    val totalEarnings: Double,
    val totalPassengers: Int,
    val favoriteRoutes: List<String>,
    val recentRides: List<String>,
    val lastUpdated: Date = Date()
)

@Entity(tableName = "achievements")
@TypeConverters(DateConverter::class)
data class AchievementEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val iconUrl: String,
    val category: String,
    val rarity: String,
    val points: Int,
    val progress: Int,
    val maxProgress: Int,
    val isUnlocked: Boolean,
    val unlockedAt: Date?,
    val lastUpdated: Date = Date()
)

@Entity(tableName = "carbon_footprint")
@TypeConverters(DateConverter::class)
data class CarbonFootprintEntity(
    @PrimaryKey val userId: String,
    val totalCarbonSaved: Double,
    val monthlyCarbonSaved: Double,
    val weeklyCarbonSaved: Double,
    val dailyCarbonSaved: Double,
    val totalRides: Int,
    val totalDistance: Double,
    val carbonSavedPerRide: Double,
    val carbonSavedPerKm: Double,
    val monthlyHistory: List<Double>,
    val lastUpdated: Date = Date()
)

@Entity(tableName = "rides")
@TypeConverters(DateConverter::class)
data class RideEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val startLocation: String,
    val endLocation: String,
    val distance: Double,
    val duration: Long,
    val date: Date,
    val rating: Float?,
    val earnings: Double,
    val passengers: Int,
    val lastUpdated: Date = Date()
) 