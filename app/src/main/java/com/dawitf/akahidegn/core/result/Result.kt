package com.dawitf.akahidegn.core.result

import com.dawitf.akahidegn.core.error.AppError

/**
 * Custom Result wrapper for API responses
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val error: AppError) : Result<Nothing>()
    
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Error
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw Exception("Result failed with error: $error")
    }
    
    companion object {
        fun <T> success(value: T): Result<T> = Success(value)
        fun error(error: AppError): Result<Nothing> = Error(error)
        fun failure(error: AppError): Result<Nothing> = Error(error)
    }
}

// Extension functions to convert between custom Result and Kotlin Result
fun <T> Result<T>.toKotlinResult(): kotlin.Result<T> = when (this) {
    is Result.Success -> kotlin.Result.success(data)
    is Result.Error -> kotlin.Result.failure(Exception("Error: $error"))
}

fun <T> kotlin.Result<T>.toCustomResult(): Result<T> = when {
    isSuccess -> Result.Success(getOrThrow())
    else -> Result.Error(AppError.NetworkError.RequestFailed(exceptionOrNull()?.message ?: "Unknown error"))
}
