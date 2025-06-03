package com.dawitf.akahidegn.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dawitf.akahidegn.data.local.dao.ChatMessageDao
import com.dawitf.akahidegn.data.local.dao.GroupDao
import com.dawitf.akahidegn.data.local.entity.ChatMessageEntity
import com.dawitf.akahidegn.data.local.entity.GroupEntity

@Database(
    entities = [GroupEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AkahidegnDatabase : RoomDatabase() {
    
    abstract fun groupDao(): GroupDao
    abstract fun chatMessageDao(): ChatMessageDao
    
    companion object {
        const val DATABASE_NAME = "akahidegn_database"
        
        // Migration from version 1 to 2 (for future use)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add migration logic here when needed
            }
        }
    }
}
