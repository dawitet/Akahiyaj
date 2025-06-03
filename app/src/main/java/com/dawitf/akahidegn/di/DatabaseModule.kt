package com.dawitf.akahidegn.di

import android.content.Context
import androidx.room.Room
import com.dawitf.akahidegn.data.local.dao.ChatMessageDao
import com.dawitf.akahidegn.data.local.dao.GroupDao
import com.dawitf.akahidegn.data.local.database.AkahidegnDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAkahidegnDatabase(@ApplicationContext context: Context): AkahidegnDatabase {
        return Room.databaseBuilder(
            context,
            AkahidegnDatabase::class.java,
            AkahidegnDatabase.DATABASE_NAME
        )
        .addMigrations(AkahidegnDatabase.MIGRATION_1_2)
        .fallbackToDestructiveMigration() // Only for development
        .build()
    }

    @Provides
    fun provideGroupDao(database: AkahidegnDatabase): GroupDao {
        return database.groupDao()
    }

    @Provides
    fun provideChatMessageDao(database: AkahidegnDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }
}
