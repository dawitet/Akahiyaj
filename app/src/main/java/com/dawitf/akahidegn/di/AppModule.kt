package com.dawitf.akahidegn.di

import android.content.Context
import com.dawitf.akahidegn.core.error.ErrorHandler
import com.dawitf.akahidegn.core.notification.NotificationManagerService
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
    fun provideErrorHandler(@ApplicationContext context: Context): ErrorHandler {
        return ErrorHandler(context)
    }

    @Provides
    @Singleton
    fun provideNotificationManagerService(@ApplicationContext context: Context): NotificationManagerService {
        return NotificationManagerService(context)
    }
}
