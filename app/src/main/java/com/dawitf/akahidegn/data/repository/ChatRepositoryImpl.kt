package com.dawitf.akahidegn.data.repository

import androidx.paging.*
import com.dawitf.akahidegn.ChatMessage
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.data.local.dao.ChatMessageDao
import com.dawitf.akahidegn.data.mapper.toDomainModel
import com.dawitf.akahidegn.data.mapper.toEntity
import com.dawitf.akahidegn.data.mapper.toDomainModels
import com.dawitf.akahidegn.data.remote.service.FirebaseChatService
import com.dawitf.akahidegn.domain.repository.ChatRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val localDataSource: ChatMessageDao,
    private val remoteDataSource: FirebaseChatService
) : ChatRepository {

    override fun getMessagesPaged(groupId: String): Flow<PagingData<ChatMessage>> {
        return Pager(
            config = PagingConfig(
                pageSize = 50,
                enablePlaceholders = false,
                prefetchDistance = 10
            ),
            pagingSourceFactory = { localDataSource.getMessagesByGroupPaged(groupId) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }
    }

    override fun getRecentMessages(groupId: String, limit: Int): Flow<Result<List<ChatMessage>>> {
        return combine(
            localDataSource.getRecentMessages(groupId, limit),
            remoteDataSource.getMessages(groupId)
        ) { localMessages, remoteResult ->
            when {
                remoteResult.isSuccess -> {
                    val remoteMessages = remoteResult.getOrNull() ?: emptyList()
                    // Cache remote messages locally
                    try {
                        localDataSource.insertMessages(remoteMessages.map { it.toEntity(groupId) })
                    } catch (e: Exception) {
                        // Log error but don't fail
                    }
                    Result.success(remoteMessages.take(limit))
                }
                localMessages.isNotEmpty() -> {
                    // Return cached data if remote fails
                    Result.success(localMessages.toDomainModels())
                }
                else -> {
                    remoteResult
                }
            }
        }
    }

    override suspend fun sendMessage(groupId: String, message: ChatMessage): Result<ChatMessage> {
        // First save locally for immediate UI update
        val localMessage = message.copy(messageId = "temp_${System.currentTimeMillis()}")
        try {
            localDataSource.insertMessage(localMessage.toEntity(groupId))
        } catch (e: Exception) {
            // Continue even if local save fails
        }

        // Then send to remote
        val result = remoteDataSource.sendMessage(groupId, message)
        if (result.isSuccess) {
            val sentMessage = result.getOrNull()
            if (sentMessage != null) {
                // Update local with the real message
                localDataSource.insertMessage(sentMessage.toEntity(groupId))
            }
        }

        return result
    }

    override suspend fun getMessageById(messageId: String): Result<ChatMessage> {
        // Try local first
        val localMessage = localDataSource.getMessageById(messageId)
        if (localMessage != null) {
            return Result.success(localMessage.toDomainModel())
        }

        // Fall back to remote
        return remoteDataSource.getMessageById(messageId)
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        // For now, we need groupId to delete from remote
        // This is a limitation of the current Firebase structure
        return try {
            localDataSource.getMessageById(messageId)?.let { messageEntity ->
                val result = remoteDataSource.deleteMessage(messageEntity.groupId, messageId)
                if (result.isSuccess) {
                    localDataSource.deleteMessage(messageEntity)
                }
                result
            } ?: Result.failure(AppError.ValidationError.NotFound("Message not found"))
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError.OperationFailed(e.message ?: "Delete failed"))
        }
    }

    override suspend fun syncMessages(groupId: String): Result<Unit> {
        return try {
            // Clean up old messages (older than 30 days)
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            localDataSource.deleteOldMessages(thirtyDaysAgo)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError.OperationFailed(e.message ?: "Sync failed"))
        }
    }

    override suspend fun clearChatHistory(groupId: String): Result<Unit> {
        return try {
            localDataSource.deleteMessagesByGroup(groupId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError.OperationFailed(e.message ?: "Clear history failed"))
        }
    }
}
