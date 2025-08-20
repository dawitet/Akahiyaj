package com.dawitf.akahidegn.di

import android.content.Context
import com.dawitf.akahidegn.core.event.UiEventManager
import com.dawitf.akahidegn.core.optimistic.OptimisticOperationsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ErrorHandlingModule {

    @Provides
    @Singleton
    fun provideUiEventManager(): UiEventManager {
        return UiEventManager()
    }

    @Provides
    @Singleton
    fun provideOptimisticOperationsManager(
        uiEventManager: UiEventManager
    ): OptimisticOperationsManager {
        return OptimisticOperationsManager(uiEventManager)
    }

    // Production-related dependencies moved to ProductionModule to avoid duplicates
} 