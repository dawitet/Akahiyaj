package com.dawitf.akahidegn.di

import android.content.Context
import com.dawitf.akahidegn.performance.ImageCacheManager
import com.dawitf.akahidegn.performance.NetworkOptimizationManager
import com.dawitf.akahidegn.performance.PerformanceManager
import com.dawitf.akahidegn.performance.PerformanceMonitor
import com.dawitf.akahidegn.analytics.AnalyticsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PerformanceModule {

    @Provides
    @Singleton
    fun providePerformanceManager(
        @ApplicationContext context: Context
    ): PerformanceManager {
        return PerformanceManager(context)
    }

    @Provides
    @Singleton
    fun provideImageCacheManager(
        @ApplicationContext context: Context
    ): ImageCacheManager {
        return ImageCacheManager(context)
    }

    @Provides
    @Singleton
    fun provideNetworkOptimizationManager(
        @ApplicationContext context: Context
    ): NetworkOptimizationManager {
        return NetworkOptimizationManager(context)
    }

    @Provides
    @Singleton
    fun providePerformanceMonitor(
        @ApplicationContext context: Context,
        analyticsService: AnalyticsService
    ): PerformanceMonitor {
        return PerformanceMonitor(context, analyticsService)
    }
}
