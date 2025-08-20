package com.dawitf.akahidegn.di

import android.content.Context
import com.dawitf.akahidegn.analytics.AnalyticsManager
import com.dawitf.akahidegn.analytics.AnalyticsService
import com.dawitf.akahidegn.production.ProductionAnalyticsManager
import com.dawitf.akahidegn.performance.PerformanceMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsManager(
        @ApplicationContext context: Context
    ): AnalyticsManager {
        return AnalyticsManager(context)
    }

    @Provides
    @Singleton
    fun provideAnalyticsService(
        @ApplicationContext context: Context
    ): AnalyticsService {
        return AnalyticsService(context)
    }

    @Provides
    @Singleton
    fun providePerformanceMonitor(
        @ApplicationContext context: Context,
        analyticsService: AnalyticsService
    ): PerformanceMonitor {
        return PerformanceMonitor(context, analyticsService)
    }

    // Performance-related dependencies moved to PerformanceModule to avoid duplicates
} 