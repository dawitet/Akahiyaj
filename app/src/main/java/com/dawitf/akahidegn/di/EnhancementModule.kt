package com.dawitf.akahidegn.di

import android.content.Context
import android.content.SharedPreferences
import com.dawitf.akahidegn.analytics.AnalyticsService
import com.dawitf.akahidegn.features.auth.EnhancedAuthService
import com.dawitf.akahidegn.features.location.DeviceConsistencyService
import com.dawitf.akahidegn.features.profile.ProfileSyncService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EnhancementModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("akahidegn_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideProfileSyncService(
        auth: FirebaseAuth,
        database: FirebaseDatabase,
        sharedPreferences: SharedPreferences,
        analyticsService: AnalyticsService
    ): ProfileSyncService {
        return ProfileSyncService(auth, database, sharedPreferences, analyticsService)
    }

    @Provides
    @Singleton
    fun provideEnhancedAuthService(
        @ApplicationContext context: Context,
        auth: FirebaseAuth,
        analyticsService: AnalyticsService
    ): EnhancedAuthService {
        return EnhancedAuthService(context, auth, analyticsService)
    }

    @Provides
    @Singleton
    fun provideDeviceConsistencyService(
        @ApplicationContext context: Context,
        auth: FirebaseAuth,
        database: FirebaseDatabase,
        analyticsService: AnalyticsService
    ): DeviceConsistencyService {
        return DeviceConsistencyService(context, auth, database, analyticsService)
    }
}
