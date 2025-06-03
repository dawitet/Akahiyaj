package com.dawitf.akahidegn.domain.repository

import androidx.paging.PagingData
import com.dawitf.akahidegn.ChatMessage
import com.dawitf.akahidegn.core.result.Result
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    
    fun getMessagesPaged(groupId: String): Flow<PagingData<ChatMessage>>
    
    fun getRecentMessages(groupId: String, limit: Int = 50): Flow<Result<List<ChatMessage>>>
    
    suspend fun sendMessage(groupId: String, message: ChatMessage): Result<ChatMessage>
    
    suspend fun getMessageById(messageId: String): Result<ChatMessage>
    
    suspend fun deleteMessage(messageId: String): Result<Unit>
    
    suspend fun syncMessages(groupId: String): Result<Unit>
    
    suspend fun clearChatHistory(groupId: String): Result<Unit>
}
