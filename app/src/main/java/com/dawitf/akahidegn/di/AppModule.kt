package com.dawitf.akahidegn.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.dawitf.akahidegn.core.error.ErrorHandler
import com.dawitf.akahidegn.core.notification.NotificationManagerService
import com.dawitf.akahidegn.activityhistory.ActivityHistoryRepository
import com.dawitf.akahidegn.features.profile.ProfileSyncService
import com.dawitf.akahidegn.features.auth.EnhancedAuthService
import com.dawitf.akahidegn.features.location.DeviceConsistencyService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase = FirebaseDatabase.getInstance()

    @Provides
    @Singleton
    fun provideErrorHandler(@ApplicationContext context: Context): ErrorHandler {
        return ErrorHandler(context)
    }

    @Provides
    @Singleton
    fun provideNotificationManagerService(@ApplicationContext context: Context): NotificationManagerService {
        return NotificationManagerService(context)
    }

    @Provides
    @Singleton
    fun provideActivityHistoryRepository(): ActivityHistoryRepository {
        return ActivityHistoryRepository()
    }

    @Provides
    @Singleton
    fun provideProfileSyncService(
        @ApplicationContext context: Context,
        auth: FirebaseAuth,
        database: FirebaseDatabase
    ): ProfileSyncService {
        return ProfileSyncService(context, auth, database)
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
    fun provideDeviceConsistencyService(@ApplicationContext context: Context): DeviceConsistencyService {
        return DeviceConsistencyService(context)
    }
}
