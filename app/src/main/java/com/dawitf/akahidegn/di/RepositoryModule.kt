package com.dawitf.akahidegn.di

import com.dawitf.akahidegn.data.remote.service.FirebaseChatService
import com.dawitf.akahidegn.data.remote.service.FirebaseGroupService
import com.dawitf.akahidegn.data.remote.service.impl.FirebaseChatServiceImpl
import com.dawitf.akahidegn.data.remote.service.impl.FirebaseGroupServiceImpl
import com.dawitf.akahidegn.data.repository.ChatRepositoryImpl
import com.dawitf.akahidegn.data.repository.GroupRepositoryImpl
import com.dawitf.akahidegn.data.repository.UserProfileRepositoryImpl
import com.dawitf.akahidegn.domain.repository.ChatRepository
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
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindFirebaseGroupService(
        firebaseGroupServiceImpl: FirebaseGroupServiceImpl
    ): FirebaseGroupService

    @Binds
    @Singleton
    abstract fun bindFirebaseChatService(
        firebaseChatServiceImpl: FirebaseChatServiceImpl
    ): FirebaseChatService

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(
        userProfileRepositoryImpl: UserProfileRepositoryImpl
    ): UserProfileRepository

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

    @Binds
    @Singleton
    abstract fun bindOfflineManager(
        offlineManagerImpl: OfflineManagerImpl
    ): OfflineManager

    @Binds
    @Singleton
    abstract fun bindAccessibilityManager(
        accessibilityManagerImpl: AccessibilityManagerImpl
    ): AccessibilityManager
    */
}
