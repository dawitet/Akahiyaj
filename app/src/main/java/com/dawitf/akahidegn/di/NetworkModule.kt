package com.dawitf.akahidegn.di

import com.dawitf.akahidegn.core.retry.ExponentialBackoffStrategy
import com.dawitf.akahidegn.core.retry.RetryMechanism
import com.dawitf.akahidegn.core.retry.RetryStrategy
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        val database = FirebaseDatabase.getInstance()
        database.setPersistenceEnabled(true) // Enable offline persistence
        return database
    }

    @Provides
    @Singleton
    fun provideRetryStrategy(): RetryStrategy {
        return ExponentialBackoffStrategy(
            maxRetries = 3,
            initialDelayMs = 1000,
            maxDelayMs = 30000,
            backoffMultiplier = 2.0
        )
    }

    @Provides
    @Singleton
    fun provideRetryMechanism(retryStrategy: RetryStrategy): RetryMechanism {
        return RetryMechanism(retryStrategy)
    }
}
