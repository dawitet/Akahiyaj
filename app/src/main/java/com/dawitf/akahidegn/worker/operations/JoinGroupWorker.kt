package com.dawitf.akahidegn.worker.operations

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.domain.repository.GroupRepository
import com.dawitf.akahidegn.core.optimistic.OptimisticOperationsManager
import com.dawitf.akahidegn.core.event.UiEventManager
import com.dawitf.akahidegn.domain.model.MemberInfo
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@HiltWorker
class JoinGroupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val groupRepository: GroupRepository,
    private val optimisticOperationsManager: OptimisticOperationsManager,
    private val uiEventManager: UiEventManager
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_GROUP_ID = "group_id"
        const val KEY_USER_ID = "user_id"
        const val KEY_OPERATION_ID = "operation_id"
        const val KEY_USER_NAME = "user_name"
    }

    override suspend fun doWork(): Result {
        val operationId = inputData.getString(KEY_OPERATION_ID) ?: return Result.failure()
        
        return withContext(Dispatchers.IO) {
	        try {
		        val groupId = inputData.getString(KEY_GROUP_ID) ?: return@withContext Result.failure()
		        val userId = inputData.getString(KEY_USER_ID) ?: return@withContext Result.failure()
                val userName = inputData.getString(KEY_USER_NAME) ?: "Joined User" // Get the user's name
                val userInfo = MemberInfo(name = userName) // Create the MemberInfo object

		        // Perform the actual repository operation
                val result = groupRepository.joinGroupOptimistic(groupId, userId, userInfo)

		        when (result) {
			        is com.dawitf.akahidegn.core.result.Result.Success -> {
				        optimisticOperationsManager.markOperationSuccess(operationId)
				        uiEventManager.showSuccess("Successfully joined group!")
				        Result.success()
			        }

			        is com.dawitf.akahidegn.core.result.Result.Error -> {
				        val intelligentError = mapToIntelligentError(AppError.UnknownError(result.error))
				        optimisticOperationsManager.markOperationFailed(operationId)
				        uiEventManager.showError(intelligentError.userMessage)
				        logOperationError("JoinGroupWorker", result.error, intelligentError)

				        if (intelligentError.shouldRetry) {
					        Result.retry()
				        } else {
					        Result.failure()
				        }
			        }

			        is com.dawitf.akahidegn.core.result.Result.Loading -> {
				        Result.retry()
			        }
		        }
	        } catch (e: Exception) {
		        val intelligentError = mapExceptionToIntelligentError(e)
		        optimisticOperationsManager.markOperationFailed(operationId)
		        uiEventManager.showError(intelligentError.userMessage)
		        logOperationError("JoinGroupWorker", e, intelligentError)
		        Result.failure()
	        } as Result
        }
    }
    
    /**
     * Intelligent error handling - Same pattern as CreateGroupWorker
     */
    private data class IntelligentError(
        val userMessage: String,
        val shouldRetry: Boolean,
        val errorCategory: String,
        val debugContext: String
    )
    
    private fun mapToIntelligentError(appError: AppError): IntelligentError {
        return when (appError) {
            is AppError.NetworkError.ConnectionTimeout -> IntelligentError(
                userMessage = "Connection timed out. Please check your internet and try again.",
                shouldRetry = true,
                errorCategory = "NETWORK_TIMEOUT",
                debugContext = "Join operation failed due to connection timeout"
            )
            is AppError.NetworkError.NoInternet -> IntelligentError(
                userMessage = "Please check your internet connection and try again.",
                shouldRetry = true,
                errorCategory = "NO_INTERNET",
                debugContext = "Join operation requires internet connectivity"
            )
            is AppError.AuthenticationError.NotAuthenticated -> IntelligentError(
                userMessage = "Please sign in to join groups.",
                shouldRetry = false,
                errorCategory = "AUTH_REQUIRED",
                debugContext = "User must authenticate to join groups"
            )
            is AppError.AuthenticationError.InvalidCredentials -> IntelligentError(
                userMessage = "You don't have permission to join this group.",
                shouldRetry = false,
                errorCategory = "PERMISSION_DENIED",
                debugContext = "User lacks permission for this specific group"
            )
            is AppError.ValidationError.NotFound -> IntelligentError(
                userMessage = "This group no longer exists.",
                shouldRetry = false,
                errorCategory = "GROUP_NOT_FOUND",
                debugContext = "Group has been deleted or doesn't exist"
            )
            is AppError.ValidationError.OperationNotAllowed -> IntelligentError(
                userMessage = "Cannot join this group right now.",
                shouldRetry = false,
                errorCategory = "GROUP_FULL",
                debugContext = "Group has reached maximum capacity or join not allowed"
            )
            is AppError.ValidationError.InvalidInput -> IntelligentError(
                userMessage = "You are already a member of this group.",
                shouldRetry = false,
                errorCategory = "ALREADY_JOINED",
                debugContext = "User already belongs to this group"
            )
            is AppError.RateLimitError.TooManyRequests -> IntelligentError(
                userMessage = "Too many requests. Please wait a moment and try again.",
                shouldRetry = true,
                errorCategory = "RATE_LIMITED",
                debugContext = "Rate limiting active, retry with backoff"
            )
            else -> IntelligentError(
                userMessage = "Failed to join group. Please try again.",
                shouldRetry = false,
                errorCategory = "UNHANDLED_ERROR",
                debugContext = "Unhandled AppError type: ${appError::class.simpleName}"
            )
        }
    }
    
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
                    userMessage = "You don't have permission to join this group.",
                    shouldRetry = false,
                    errorCategory = "FIRESTORE_PERMISSION_DENIED",
                    debugContext = "Firestore permission denied: ${exception.message}"
                )
                FirebaseFirestoreException.Code.NOT_FOUND -> IntelligentError(
                    userMessage = "This group no longer exists.",
                    shouldRetry = false,
                    errorCategory = "GROUP_NOT_FOUND",
                    debugContext = "Firestore document not found: ${exception.message}"
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
    
    private fun logOperationError(workerName: String, originalError: Any, intelligentError: IntelligentError) {
        println("=== [$workerName] INTELLIGENT ERROR ANALYSIS ===")
        println("Error Category: ${intelligentError.errorCategory}")
        println("User Message: ${intelligentError.userMessage}")
        println("Should Retry: ${intelligentError.shouldRetry}")
        println("Debug Context: ${intelligentError.debugContext}")
        println("Original Error Type: ${originalError::class.simpleName}")
        println("Original Error Message: ${originalError}")
        println("Run Attempt: $runAttemptCount")
        
        if (originalError is Throwable) {
            println("Stack Trace:")
            println(originalError.stackTraceToString())
        }
        
        println("=== END ERROR ANALYSIS ===")
    }
}
