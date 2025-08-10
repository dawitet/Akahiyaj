package com.dawitf.akahidegn.core.result

import com.dawitf.akahidegn.core.error.AppError

/**
 * Custom Result wrapper for API responses with loading state support
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val error: AppError) : Result<Nothing>()
    data object Loading : Result<Nothing>()
    
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error, is Loading -> null
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw Exception("Result failed with error: $error")
        is Loading -> throw Exception("Result is still loading")
    }
    
    companion object {
        fun <T> success(value: T): Result<T> = Success(value)
        fun error(error: AppError): Result<Nothing> = Error(error)
        fun failure(error: AppError): Result<Nothing> = Error(error)
        fun loading(): Result<Nothing> = Loading
    }
}

// Extension functions to convert between custom Result and Kotlin Result
fun <T> Result<T>.toKotlinResult(): kotlin.Result<T> = when (this) {
    is Result.Success -> kotlin.Result.success(data)
    is Result.Error -> kotlin.Result.failure(Exception("Error: $error"))
    is Result.Loading -> kotlin.Result.failure(Exception("Result is loading"))
}

fun <T> kotlin.Result<T>.toCustomResult(): Result<T> = when {
    isSuccess -> Result.Success(getOrThrow())
    else -> Result.Error(AppError.NetworkError.RequestFailed(exceptionOrNull()?.message ?: "Unknown error"))
}
