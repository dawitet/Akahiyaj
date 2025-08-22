package com.dawitf.akahidegn.di

import android.content.Context
import android.content.SharedPreferences
import com.dawitf.akahidegn.analytics.AnalyticsService
import com.dawitf.akahidegn.features.auth.EnhancedAuthService
import com.dawitf.akahidegn.features.location.DeviceConsistencyService
import com.dawitf.akahidegn.features.profile.ProfileSyncService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
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
        @ApplicationContext context: Context,
        auth: FirebaseAuth,
        database: FirebaseDatabase,
        firestore: FirebaseFirestore
    ): ProfileSyncService {
        return ProfileSyncService(context, auth, firestore)
    }

    @Provides
    @Singleton
    fun provideEnhancedAuthService(
        @ApplicationContext context: Context,
        auth: FirebaseAuth
    ): EnhancedAuthService {
        return EnhancedAuthService(context, auth)
    }

    @Provides
    @Singleton
    fun provideDeviceConsistencyService(
        @ApplicationContext context: Context
    ): DeviceConsistencyService {
        return DeviceConsistencyService(context)
    }
}
