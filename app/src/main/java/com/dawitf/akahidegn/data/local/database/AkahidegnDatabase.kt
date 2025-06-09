package com.dawitf.akahidegn.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dawitf.akahidegn.data.local.dao.*
import com.dawitf.akahidegn.data.local.entity.*

@Database(
    entities = [
        GroupEntity::class, 
        ChatMessageEntity::class,
        GroupEntityEnhanced::class,
        RecentSearchEntity::class,
        PopularDestinationEntity::class,
        SearchCacheEntity::class,
        UserPreferencesEntity::class,
        UserAnalyticsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AkahidegnDatabase : RoomDatabase() {
    
    abstract fun groupDao(): GroupDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun enhancedGroupDao(): EnhancedGroupDao
    abstract fun searchDao(): SearchDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun userAnalyticsDao(): UserAnalyticsDao
    
    companion object {
        const val DATABASE_NAME = "akahidegn_database"
        
        // Migration from version 1 to 2 - Add enhanced tables
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create enhanced groups table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `groups_enhanced` (
                        `groupId` TEXT NOT NULL,
                        `creatorId` TEXT,
                        `creatorCloudflareId` TEXT,
                        `destinationName` TEXT,
                        `pickupLat` REAL,
                        `pickupLng` REAL,
                        `timestamp` INTEGER,
                        `maxMembers` INTEGER NOT NULL DEFAULT 4,
                        `memberCount` INTEGER NOT NULL DEFAULT 0,
                        `imageUrl` TEXT,
                        `pricePerPerson` REAL,
                        `departureTime` INTEGER,
                        `availableSeats` INTEGER NOT NULL,
                        `description` TEXT,
                        `contactInfo` TEXT,
                        `vehicleType` TEXT,
                        `route` TEXT,
                        `tags` TEXT,
                        `rating` REAL NOT NULL DEFAULT 0,
                        `reviewCount` INTEGER NOT NULL DEFAULT 0,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `lastUpdated` INTEGER NOT NULL DEFAULT 0,
                        `distanceFromUser` REAL,
                        `popularityScore` REAL NOT NULL DEFAULT 0,
                        PRIMARY KEY(`groupId`)
                    )
                """)
                
                // Create indices for enhanced groups table
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_groups_enhanced_destinationName` ON `groups_enhanced` (`destinationName`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_groups_enhanced_pickupLat_pickupLng` ON `groups_enhanced` (`pickupLat`, `pickupLng`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_groups_enhanced_timestamp` ON `groups_enhanced` (`timestamp`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_groups_enhanced_pricePerPerson` ON `groups_enhanced` (`pricePerPerson`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_groups_enhanced_departureTime` ON `groups_enhanced` (`departureTime`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_groups_enhanced_memberCount` ON `groups_enhanced` (`memberCount`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_groups_enhanced_availableSeats` ON `groups_enhanced` (`availableSeats`)")
                
                // Create recent searches table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recent_searches` (
                        `id` TEXT NOT NULL,
                        `query` TEXT NOT NULL,
                        `destination` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `usageCount` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """)
                
                // Create popular destinations table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `popular_destinations` (
                        `name` TEXT NOT NULL,
                        `count` INTEGER NOT NULL,
                        `distance` REAL NOT NULL,
                        `lastUpdated` INTEGER NOT NULL,
                        PRIMARY KEY(`name`)
                    )
                """)
                
                // Create search cache table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `search_cache` (
                        `cacheKey` TEXT NOT NULL,
                        `groupIds` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `filters` TEXT NOT NULL,
                        PRIMARY KEY(`cacheKey`)
                    )
                """)
                
                // Create user preferences table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_preferences` (
                        `key` TEXT NOT NULL,
                        `value` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        PRIMARY KEY(`key`)
                    )
                """)
                
                // Create user analytics table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_analytics` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `eventData` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `uploaded` INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }
    }
}
