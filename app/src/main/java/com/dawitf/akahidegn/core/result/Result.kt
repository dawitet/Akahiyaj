package com.dawitf.akahidegn.core.result

/**
 * A generic wrapper for handling success, error, and loading states
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

/**
 * Extension functions for Result handling
 */
inline fun <T> Result<T>.onSuccess(action: (value: T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (error: String) -> Unit): Result<T> {
    if (this is Result.Error) action(error)
    return this
}

inline fun <T> Result<T>.onLoading(action: () -> Unit): Result<T> {
    if (this is Result.Loading) action()
    return this
}

/**
 * Helper functions for creating Results
 */
fun <T> success(data: T): Result<T> = Result.Success(data)
fun failure(message: String): Result<Nothing> = Result.Error(message)
fun error(message: String): Result<Nothing> = Result.Error(message)
fun loading(): Result<Nothing> = Result.Loading

/**
 * Extension properties for easier access
 */
val <T> Result<T>.isSuccess: Boolean get() = this is Result.Success
val <T> Result<T>.isError: Boolean get() = this is Result.Error
val <T> Result<T>.isLoading: Boolean get() = this is Result.Loading

fun <T> Result<T>.getOrNull(): T? = if (this is Result.Success) data else null
