package com.dawitf.akahidegn.di

import com.dawitf.akahidegn.data.remote.service.FirebaseGroupService
import com.dawitf.akahidegn.data.remote.service.impl.FirebaseGroupServiceImpl
import com.dawitf.akahidegn.data.repository.GroupRepositoryImpl
import com.dawitf.akahidegn.data.repository.UserProfileRepositoryImpl
import com.dawitf.akahidegn.domain.repository.GroupRepository
import com.dawitf.akahidegn.domain.repository.UserProfileRepository
// TODO: Uncomment when manager classes are implemented
// import com.dawitf.akahidegn.analytics.AnalyticsManager
// import com.dawitf.akahidegn.analytics.FirebaseAnalyticsManager
// import com.dawitf.akahidegn.localization.LocalizationManager
// import com.dawitf.akahidegn.localization.LocalizationManagerImpl
// import com.dawitf.akahidegn.offline.OfflineManager
// import com.dawitf.akahidegn.offline.OfflineManagerImpl
// import com.dawitf.akahidegn.accessibility.AccessibilityManager
// import com.dawitf.akahidegn.accessibility.AccessibilityManagerImpl
import dagger.Binds
import dagger.Module
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
    abstract fun bindFirebaseGroupService(
        firebaseGroupServiceImpl: FirebaseGroupServiceImpl
    ): FirebaseGroupService

    // TODO: Uncomment when these manager classes are implemented
    /*
    @Binds
    @Singleton
    abstract fun bindAnalyticsManager(
        firebaseAnalyticsManager: FirebaseAnalyticsManager
    ): AnalyticsManager

    @Binds
    @Singleton
    abstract fun bindLocalizationManager(
        localizationManagerImpl: LocalizationManagerImpl
    ): LocalizationManager

    // Offline manager removed for simplicity - not practical for ride-sharing
    // @Binds
    // @Singleton
    // abstract fun bindOfflineManager(...): OfflineManager

    @Binds
    @Singleton
    abstract fun bindAccessibilityManager(
        accessibilityManagerImpl: AccessibilityManagerImpl
    ): AccessibilityManager
    */
}
