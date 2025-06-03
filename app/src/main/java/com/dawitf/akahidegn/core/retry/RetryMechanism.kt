package com.dawitf.akahidegn.core.retry

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow

/**
 * Retry mechanism with exponential backoff
 */
interface BackoffStrategy {
    suspend fun calculateDelay(attempt: Int): Long
}

/**
 * Retry strategy interface for dependency injection
 */
interface RetryStrategy {
    val maxRetries: Int
    val initialDelayMs: Long
    val maxDelayMs: Long
    val backoffMultiplier: Double
}

class ExponentialBackoffStrategy(
    override val maxRetries: Int = 3,
    override val initialDelayMs: Long = 1000L,
    override val maxDelayMs: Long = 30000L,
    override val backoffMultiplier: Double = 2.0
) : RetryStrategy

class ExponentialBackoff(
    private val initialDelay: Long = 1000L,
    private val maxDelay: Long = 30000L,
    private val multiplier: Double = 2.0
) : BackoffStrategy {
    override suspend fun calculateDelay(attempt: Int): Long {
        val delay = (initialDelay * multiplier.pow(attempt.toDouble())).toLong()
        return min(delay, maxDelay)
    }
}

class LinearBackoff(
    private val delay: Long = 1000L
) : BackoffStrategy {
    override suspend fun calculateDelay(attempt: Int): Long = delay
}

/**
 * Retry mechanism class for dependency injection
 */
class RetryMechanism(private val strategy: RetryStrategy) {
    
    suspend fun <T> withRetry(
        shouldRetry: (Throwable) -> Boolean = { true },
        block: suspend () -> T
    ): T {
        return withRetry(
            maxAttempts = strategy.maxRetries,
            backoffStrategy = ExponentialBackoff(
                initialDelay = strategy.initialDelayMs,
                maxDelay = strategy.maxDelayMs,
                multiplier = strategy.backoffMultiplier
            ),
            shouldRetry = shouldRetry,
            block = block
        )
    }
}

/**
 * Retry function with configurable strategy
 */
suspend fun <T> withRetry(
    maxAttempts: Int = 3,
    backoffStrategy: BackoffStrategy = ExponentialBackoff(),
    shouldRetry: (Throwable) -> Boolean = { true },
    block: suspend () -> T
): T {
    var lastException: Throwable? = null
    
    repeat(maxAttempts) { attempt ->
        try {
            return block()
        } catch (e: Throwable) {
            lastException = e
            
            if (attempt == maxAttempts - 1 || !shouldRetry(e)) {
                throw e
            }
            
            val delayTime = backoffStrategy.calculateDelay(attempt)
            delay(delayTime)
        }
    }
    
    throw lastException ?: RuntimeException("Retry failed without exception")
}
