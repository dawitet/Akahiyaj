package com.dawitf.akahidegn.di

import com.dawitf.akahidegn.data.remote.service.GroupService
import com.dawitf.akahidegn.data.remote.service.impl.GroupServiceImpl
import com.dawitf.akahidegn.data.repository.GroupRepositoryImpl
import com.dawitf.akahidegn.data.repository.UserProfileRepositoryImpl
import com.dawitf.akahidegn.domain.repository.GroupRepository
import com.dawitf.akahidegn.domain.repository.UserProfileRepository
import com.dawitf.akahidegn.analytics.AnalyticsManager
import com.dawitf.akahidegn.localization.LocalizationManager
import com.dawitf.akahidegn.accessibility.AccessibilityManager
// Note: OfflineManager is left commented out as it appears to be empty/incomplete
// import com.dawitf.akahidegn.offline.OfflineManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGroupRepository(
        groupRepositoryImpl: GroupRepositoryImpl
    ): GroupRepository

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(
        userProfileRepositoryImpl: UserProfileRepositoryImpl
    ): UserProfileRepository

    @Binds
    @Singleton
    abstract fun bindGroupService(
        groupServiceImpl: GroupServiceImpl
    ): GroupService
}
