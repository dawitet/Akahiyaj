package com.dawitf.akahidegn.di

import com.dawitf.akahidegn.features.profile.UserProfileService
import com.dawitf.akahidegn.features.profile.impl.UserProfileServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {

    @Binds
    @Singleton
    abstract fun bindUserProfileService(
        userProfileServiceImpl: UserProfileServiceImpl
    ): UserProfileService
}
