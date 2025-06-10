package com.dawitf.akahidegn.di

import android.content.Context
import com.dawitf.akahidegn.debug.GroupCleanupDebugHelper
import com.dawitf.akahidegn.service.GroupCleanupScheduler
import com.dawitf.akahidegn.domain.repository.GroupRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GroupCleanupModule {

    @Provides
    @Singleton
    fun provideGroupCleanupDebugHelper(
        @ApplicationContext context: Context,
        groupCleanupScheduler: GroupCleanupScheduler,
        groupRepository: GroupRepository
    ): GroupCleanupDebugHelper {
        return GroupCleanupDebugHelper(context, groupCleanupScheduler, groupRepository)
    }
} 