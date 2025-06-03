package com.dawitf.akahidegn.di

import com.dawitf.akahidegn.data.remote.service.FirebaseChatService
import com.dawitf.akahidegn.data.remote.service.FirebaseGroupService
import com.dawitf.akahidegn.data.remote.service.impl.FirebaseChatServiceImpl
import com.dawitf.akahidegn.data.remote.service.impl.FirebaseGroupServiceImpl
import com.dawitf.akahidegn.data.repository.ChatRepositoryImpl
import com.dawitf.akahidegn.data.repository.GroupRepositoryImpl
import com.dawitf.akahidegn.domain.repository.ChatRepository
import com.dawitf.akahidegn.domain.repository.GroupRepository
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
}
