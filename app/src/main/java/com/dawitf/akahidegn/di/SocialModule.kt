package com.dawitf.akahidegn.di

import com.dawitf.akahidegn.features.social.RideBuddyService
import com.dawitf.akahidegn.features.social.impl.RideBuddyServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SocialModule {

    @Binds
    @Singleton
    abstract fun bindRideBuddyService(
        rideBuddyServiceImpl: RideBuddyServiceImpl
    ): RideBuddyService
}
