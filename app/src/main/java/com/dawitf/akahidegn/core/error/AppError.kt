package com.dawitf.akahidegn.core.error

/**
 * Unified error handling for the application
 */
sealed class AppError : Exception() {
    
    sealed class NetworkError : AppError() {
        object ConnectionTimeout : NetworkError()
        object NoInternet : NetworkError()
        object NoConnection : NetworkError()
        object Timeout : NetworkError()
        data class FirebaseError(override val message: String) : NetworkError()
        data class DataParsingError(override val message: String) : NetworkError()
        data class UnknownNetworkError(override val message: String) : NetworkError()
        data class RequestFailed(override val message: String) : NetworkError()
    }
    
    sealed class AuthenticationError : AppError() {
        object UserNotFound : AuthenticationError()
        object InvalidCredentials : AuthenticationError()
        object NotAuthenticated : AuthenticationError()
        object SessionExpired : AuthenticationError() {
            override val message = "Session has expired. Please sign in again."
        }
        object TooManyRequests : AuthenticationError()
        object UserDisabled : AuthenticationError()
        object AccountExistsWithDifferentCredential : AuthenticationError()
        object CredentialAlreadyInUse : AuthenticationError()
        object TokenRefreshFailed : AuthenticationError()
        data class AuthenticationFailed(override val message: String) : AuthenticationError()
    }
    
    sealed class ValidationError : AppError() {
        data class InvalidInput(override val message: String) : ValidationError()
        data class NotFound(override val message: String) : ValidationError()
        data class InvalidFormat(override val message: String) : ValidationError()
        data class OperationNotAllowed(override val message: String) : ValidationError()
        data class ResourceNotFound(override val message: String) : ValidationError()
    }
    
    sealed class DatabaseError : AppError() {
        data class OperationFailed(override val message: String) : DatabaseError()
        object DatabaseCorrupted : DatabaseError()
        data class MigrationFailed(override val message: String) : DatabaseError()
    }
    
    sealed class RateLimitError : AppError() {
        data class TooManyRequests(override val message: String) : RateLimitError()
        data class QuotaExceeded(override val message: String) : RateLimitError()
    }
    
    sealed class LocationError : AppError() {
        object PermissionDenied : LocationError()
        object ServiceUnavailable : LocationError()
        object ProviderDisabled : LocationError()
        object LocationDisabled : LocationError()
        object LocationUnavailable : LocationError()
        object InvalidLocation : LocationError()
        data class LocationFailed(override val message: String) : LocationError()
    }
    
    data class UnknownError(override val message: String) : AppError()
}

/**
 * Type alias for better readability
 */
typealias AppResult<T> = Result<T>

/**
 * Extension functions for Result handling
 */
inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (isSuccess) action(getOrThrow())
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (Throwable) -> Unit): AppResult<T> {
    if (isFailure) action(exceptionOrNull()!!)
    return this
}

/**
 * Safe result creation
 */
inline fun <T> safeCall(action: () -> T): AppResult<T> {
    return try {
        Result.success(action())
    } catch (e: Exception) {
        Result.failure(
            when (e) {
                is java.net.UnknownHostException -> AppError.NetworkError.NoInternet
                is java.net.SocketTimeoutException -> AppError.NetworkError.ConnectionTimeout
                is SecurityException -> AppError.LocationError.PermissionDenied
                else -> AppError.UnknownError(e.message ?: "Unknown error occurred")
            }
        )
    }
}
