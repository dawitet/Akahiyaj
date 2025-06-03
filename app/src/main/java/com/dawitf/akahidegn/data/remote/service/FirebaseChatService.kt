package com.dawitf.akahidegn.data.remote.service

import com.dawitf.akahidegn.ChatMessage
import com.dawitf.akahidegn.core.result.Result
import kotlinx.coroutines.flow.Flow

interface FirebaseChatService {
    
    fun getMessages(groupId: String): Flow<Result<List<ChatMessage>>>
    
    suspend fun sendMessage(groupId: String, message: ChatMessage): Result<ChatMessage>
    
    suspend fun getMessageById(messageId: String): Result<ChatMessage>
    
    suspend fun deleteMessage(groupId: String, messageId: String): Result<Unit>
}
