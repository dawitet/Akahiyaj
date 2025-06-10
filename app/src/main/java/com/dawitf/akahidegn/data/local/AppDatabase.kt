package com.dawitf.akahidegn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dawitf.akahidegn.data.local.converter.ListConverter
import com.dawitf.akahidegn.data.local.dao.ProfileDao
import com.dawitf.akahidegn.data.local.entity.*

@Database(
    entities = [
        RideStatisticsEntity::class,
        AchievementEntity::class,
        CarbonFootprintEntity::class,
        RideEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(ListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
} 