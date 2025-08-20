package com.dawitf.akahidegn.di

import android.content.Context
import com.dawitf.akahidegn.production.DatabaseOptimizationManager
import com.dawitf.akahidegn.production.ProductionAnalyticsManager
import com.dawitf.akahidegn.production.ProductionErrorHandler
import com.dawitf.akahidegn.production.ProductionNotificationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for production components
 */
@Module
@InstallIn(SingletonComponent::class)
object ProductionModule {
    
    @Provides
    @Singleton
    fun provideDatabaseOptimizationManager(): DatabaseOptimizationManager {
        return DatabaseOptimizationManager()
    }
    
    @Provides
    @Singleton
    fun provideProductionAnalyticsManager(
        @ApplicationContext context: Context
    ): ProductionAnalyticsManager {
        return ProductionAnalyticsManager(context)
    }
    
    @Provides
    @Singleton
    fun provideProductionErrorHandler(
        @ApplicationContext context: Context
    ): ProductionErrorHandler {
        return ProductionErrorHandler(context)
    }
    
    @Provides
    @Singleton
    fun provideProductionNotificationManager(
        @ApplicationContext context: Context
    ): ProductionNotificationManager {
        return ProductionNotificationManager(context)
    }
}
