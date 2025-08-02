package com.dawitf.akahidegn.di

import com.dawitf.akahidegn.data.remote.service.GroupService
import com.dawitf.akahidegn.data.remote.service.impl.GroupServiceImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return Firebase.database
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideGroupService(
        database: FirebaseDatabase,
        auth: FirebaseAuth
    ): GroupService {
        return GroupServiceImpl(database, auth)
    }
}