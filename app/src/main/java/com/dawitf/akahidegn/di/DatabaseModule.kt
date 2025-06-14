package com.dawitf.akahidegn.di

import android.content.Context
import com.dawitf.akahidegn.data.local.dao.GroupDao
import com.dawitf.akahidegn.data.local.dao.UserPreferencesDao
import com.dawitf.akahidegn.data.local.database.AkahidegnDatabase
import com.dawitf.akahidegn.production.DatabaseOptimizationManager
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
    fun provideAkahidegnDatabase(
        @ApplicationContext context: Context
    ): AkahidegnDatabase {
        return AkahidegnDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideGroupDao(database: AkahidegnDatabase): GroupDao {
        return database.groupDao()
    }

    @Provides
    @Singleton
    fun provideUserPreferencesDao(database: AkahidegnDatabase): com.dawitf.akahidegn.data.local.dao.UserPreferencesDao {
        return database.userPreferencesDao()
    }

    // DatabaseOptimizationManager moved to ProductionModule to avoid duplicates
}