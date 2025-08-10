package com.dawitf.akahidegn.worker.operations

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dawitf.akahidegn.Group
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.domain.repository.GroupRepository
import com.dawitf.akahidegn.core.optimistic.OptimisticOperationsManager
import com.dawitf.akahidegn.core.event.UiEventManager
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@HiltWorker
class CreateGroupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val groupRepository: GroupRepository,
    private val optimisticOperationsManager: OptimisticOperationsManager,
    private val uiEventManager: UiEventManager
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_GROUP_ID = "group_id"
        const val KEY_CREATOR_ID = "creator_id"
        const val KEY_CREATOR_NAME = "creator_name"
        const val KEY_DESTINATION_NAME = "destination_name"
        const val KEY_OPERATION_ID = "operation_id"
    }

    override suspend fun doWork(): Result {
        val operationId = inputData.getString(KEY_OPERATION_ID) ?: return Result.failure()
        
        return withContext(Dispatchers.IO) {
            try {
                // Extract group data from input
                val groupId = inputData.getString(KEY_GROUP_ID) ?: return@withContext Result.failure()
                val creatorId = inputData.getString(KEY_CREATOR_ID) ?: return@withContext Result.failure()
                val creatorName = inputData.getString(KEY_CREATOR_NAME) ?: return@withContext Result.failure()
                val destinationName = inputData.getString(KEY_DESTINATION_NAME) ?: return@withContext Result.failure()

                // Create group object
                val group = Group(
                    groupId = groupId,
                    creatorId = creatorId,
                    creatorName = creatorName,
                    destinationName = destinationName,
                    timestamp = System.currentTimeMillis()
                )

                // Perform the actual repository operation
                val result = groupRepository.createGroup(group)
                
                when (result) {
                    is com.dawitf.akahidegn.core.result.Result.Success -> {
                        // Mark operation as successful and update UI
                        optimisticOperationsManager.markOperationSuccess(operationId)
                        uiEventManager.showSuccess("Group '${result.data.destinationName}' created successfully!")
                        Result.success()
                    }
                    is com.dawitf.akahidegn.core.result.Result.Error -> {
                        // Apply your sophisticated error handling approach here
                        val intelligentError = mapToIntelligentError(result.error)
                        
                        // Mark operation as failed with specific error context
                        optimisticOperationsManager.markOperationFailed(operationId)
                        
                        // Emit context-aware user message based on error type
                        uiEventManager.showError(intelligentError.userMessage)
                        
                        // Log detailed error for debugging (preserving full context)
                        logOperationError("CreateGroupWorker", result.error, intelligentError)
                        
                        // Return intelligent WorkManager result based on error type
                        if (intelligentError.shouldRetry) {
                            Result.retry()
                        } else {
                            Result.failure()
                        }
                    }
                    is com.dawitf.akahidegn.core.result.Result.Loading -> {
                        // Should not happen in workers, but handle gracefully
                        Result.retry()
                    }
                }
            } catch (e: Exception) {
                // Final safety net for unexpected errors
                val intelligentError = mapExceptionToIntelligentError(e)
                optimisticOperationsManager.markOperationFailed(operationId)
                uiEventManager.showError(intelligentError.userMessage)
                logOperationError("CreateGroupWorker", e, intelligentError)
                Result.failure()
            }
        }
    }
    
    /**
     * The core intelligence you requested: Map generic AppError to specific, actionable insights
     * This addresses your concern about losing operational intelligence
     */
    private data class IntelligentError(
        val userMessage: String,
        val shouldRetry: Boolean,
        val errorCategory: String,
        val debugContext: String
    )
    
    private fun mapToIntelligentError(appError: AppError): IntelligentError {
        return when (appError) {
            // Network errors - RETRYABLE with user-friendly messaging
            is AppError.NetworkError.ConnectionTimeout -> IntelligentError(
                userMessage = "Connection timed out. Please check your internet and try again.",
                shouldRetry = true,
                errorCategory = "NETWORK_TIMEOUT",
                debugContext = "User can retry, connection issue likely temporary"
            )
            is AppError.NetworkError.NoInternet -> IntelligentError(
                userMessage = "Please check your internet connection and try again.",
                shouldRetry = true,
                errorCategory = "NO_INTERNET", 
                debugContext = "User needs to fix connectivity before retry"
            )
            is AppError.NetworkError.FirebaseError -> IntelligentError(
                userMessage = "Service temporarily unavailable. Please try again in a moment.",
                shouldRetry = true,
                errorCategory = "SERVER_UNAVAILABLE",
                debugContext = "Firebase/server issue, automatic retry appropriate"
            )
            
            // Authentication errors - NOT RETRYABLE, require user action
            is AppError.AuthenticationError.NotAuthenticated -> IntelligentError(
                userMessage = "Please sign in to create groups.",
                shouldRetry = false,
                errorCategory = "AUTH_REQUIRED",
                debugContext = "User must authenticate, no point in automatic retry"
            )
            is AppError.AuthenticationError.SessionExpired -> IntelligentError(
                userMessage = "Your session has expired. Please sign in again.",
                shouldRetry = false,
                errorCategory = "SESSION_EXPIRED",
                debugContext = "User must re-authenticate, automatic retry will fail"
            )
            is AppError.AuthenticationError.InvalidCredentials -> IntelligentError(
                userMessage = "You don't have permission to create groups.",
                shouldRetry = false,
                errorCategory = "PERMISSION_DENIED",
                debugContext = "Permanent permission issue, retry will always fail"
            )
            
            // Validation errors - NOT RETRYABLE, user must fix data
            is AppError.ValidationError.InvalidInput -> IntelligentError(
                userMessage = "Please check your group information and try again.",
                shouldRetry = false,
                errorCategory = "INVALID_INPUT",
                debugContext = "User input validation failed, need UI correction"
            )
            is AppError.ValidationError.OperationNotAllowed -> IntelligentError(
                userMessage = "A group with this information already exists.",
                shouldRetry = false,
                errorCategory = "DUPLICATE_GROUP",
                debugContext = "Business logic validation, user needs different data"
            )
            
            // Rate limit errors - Smart retry based on error types
            is AppError.RateLimitError.TooManyRequests -> IntelligentError(
                userMessage = "Too many requests. Please wait a moment and try again.",
                shouldRetry = true,
                errorCategory = "RATE_LIMITED",
                debugContext = "Rate limiting, retry with exponential backoff appropriate"
            )
            is AppError.RateLimitError.QuotaExceeded -> IntelligentError(
                userMessage = "Service quota exceeded. Please try again later.",
                shouldRetry = true,
                errorCategory = "QUOTA_EXCEEDED",
                debugContext = "Quota issue, retry after delay appropriate"
            )
            is AppError.DatabaseError.OperationFailed -> IntelligentError(
                userMessage = "Database error. Please try again in a moment.",
                shouldRetry = true,
                errorCategory = "DATABASE_ERROR",
                debugContext = "Database issue, temporary problem, retry appropriate"
            )
            
            // Unknown errors - Log extensively, don't retry
            is AppError.UnknownError -> IntelligentError(
                userMessage = "An unexpected error occurred. Please try again.",
                shouldRetry = false,
                errorCategory = "UNKNOWN_ERROR",
                debugContext = "Unexpected error, needs investigation, manual retry only"
            )
            
            // Fallback for any other AppError types
            else -> IntelligentError(
                userMessage = "Failed to create group. Please try again.",
                shouldRetry = false,
                errorCategory = "UNHANDLED_ERROR",
                debugContext = "Unhandled AppError type: ${appError::class.simpleName}"
            )
        }
    }
    
    /**
     * Map raw exceptions to intelligent errors when AppError mapping isn't available
     */
    private fun mapExceptionToIntelligentError(exception: Throwable): IntelligentError {
        return when (exception) {
            is FirebaseAuthException -> IntelligentError(
                userMessage = "Authentication error. Please sign in and try again.",
                shouldRetry = false,
                errorCategory = "FIREBASE_AUTH_ERROR",
                debugContext = "Firebase Auth: ${exception.errorCode} - ${exception.message}"
            )
            is FirebaseFirestoreException -> when (exception.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> IntelligentError(
                    userMessage = "You don't have permission to create groups.",
                    shouldRetry = false,
                    errorCategory = "FIRESTORE_PERMISSION_DENIED",
                    debugContext = "Firestore permission denied: ${exception.message}"
                )
                FirebaseFirestoreException.Code.UNAVAILABLE -> IntelligentError(
                    userMessage = "Service temporarily unavailable. Please try again.",
                    shouldRetry = true,
                    errorCategory = "FIRESTORE_UNAVAILABLE",
                    debugContext = "Firestore unavailable: ${exception.message}"
                )
                else -> IntelligentError(
                    userMessage = "Database error. Please try again.",
                    shouldRetry = true,
                    errorCategory = "FIRESTORE_ERROR",
                    debugContext = "Firestore error ${exception.code}: ${exception.message}"
                )
            }
            is UnknownHostException -> IntelligentError(
                userMessage = "Please check your internet connection.",
                shouldRetry = true,
                errorCategory = "NO_INTERNET_CONNECTION",
                debugContext = "Network connectivity issue: ${exception.message}"
            )
            is SocketTimeoutException -> IntelligentError(
                userMessage = "Connection timed out. Please try again.",
                shouldRetry = true,
                errorCategory = "CONNECTION_TIMEOUT",
                debugContext = "Socket timeout: ${exception.message}"
            )
            else -> IntelligentError(
                userMessage = "An unexpected error occurred. Please try again.",
                shouldRetry = false,
                errorCategory = "UNEXPECTED_EXCEPTION",
                debugContext = "Unexpected exception: ${exception::class.simpleName} - ${exception.message}"
            )
        }
    }
    
    /**
     * Comprehensive error logging that preserves operational intelligence
     * This addresses your concern about debugging and production troubleshooting
     */
    private fun logOperationError(workerName: String, originalError: Any, intelligentError: IntelligentError) {
        // In production, this would go to your logging service (Firebase Crashlytics, etc.)
        println("=== [$workerName] INTELLIGENT ERROR ANALYSIS ===")
        println("Error Category: ${intelligentError.errorCategory}")
        println("User Message: ${intelligentError.userMessage}")
        println("Should Retry: ${intelligentError.shouldRetry}")
        println("Debug Context: ${intelligentError.debugContext}")
        println("Original Error Type: ${originalError::class.simpleName}")
        println("Original Error Message: ${originalError}")
        println("Run Attempt: $runAttemptCount")
        
        // Include stack trace for debugging
        if (originalError is Throwable) {
            println("Stack Trace:")
            println(originalError.stackTraceToString())
        }
        
        println("=== END ERROR ANALYSIS ===")
    }
}