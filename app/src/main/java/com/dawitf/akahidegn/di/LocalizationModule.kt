package com.dawitf.akahidegn.di

import android.content.Context
import com.dawitf.akahidegn.localization.LocalizationManager
import com.dawitf.akahidegn.accessibility.AccessibilityManager
// Removed OfflineManager import - offline functionality simplified
import com.dawitf.akahidegn.ui.theme.ThemeManager
import com.dawitf.akahidegn.data.local.dao.EnhancedGroupDao
import com.dawitf.akahidegn.data.local.dao.SearchDao
import com.dawitf.akahidegn.data.local.dao.UserPreferencesDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalizationModule {

    @Provides
    @Singleton
    fun provideLocalizationManager(
        @ApplicationContext context: Context
    ): LocalizationManager {
        return LocalizationManager(context)
    }

    @Provides
    @Singleton
    fun provideAccessibilityManager(
        @ApplicationContext context: Context
    ): AccessibilityManager {
        return AccessibilityManager(context)
    }

    // Offline manager removed for simplicity - not practical for ride-sharing
    // @Provides
    // @Singleton
    // fun provideOfflineManager(...): OfflineManager = ...

    @Provides
    @Singleton
    fun provideThemeManager(
        @ApplicationContext context: Context
    ): ThemeManager {
        return ThemeManager(context)
    }
} 