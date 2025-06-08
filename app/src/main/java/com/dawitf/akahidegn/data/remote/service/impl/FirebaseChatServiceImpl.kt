package com.dawitf.akahidegn.data.remote.service.impl

import com.dawitf.akahidegn.ChatMessage
import com.dawitf.akahidegn.core.error.AppError
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.core.retry.RetryMechanism
import com.dawitf.akahidegn.data.remote.service.FirebaseChatService
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseChatServiceImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val retryMechanism: RetryMechanism
) : FirebaseChatService {
    
    override fun getMessages(groupId: String): Flow<Result<List<ChatMessage>>> = callbackFlow {
        val messagesRef = database.reference.child("messages").child(groupId)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val messages = snapshot.children.mapNotNull { child ->
                        child.getValue(ChatMessage::class.java)?.copy(messageId = child.key ?: "")
                    }.sortedByDescending { it.timestamp }
                    
                    trySend(Result.Success(messages))
                } catch (e: Exception) {
                    trySend(Result.Error(AppError.NetworkError.DataParsingError(e.message ?: "Failed to parse messages")))
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                trySend(Result.Error(AppError.NetworkError.FirebaseError(error.message)))
            }
        }
        
        messagesRef.addValueEventListener(listener)
        awaitClose { messagesRef.removeEventListener(listener) }
    }
    
    override suspend fun sendMessage(groupId: String, message: ChatMessage): Result<ChatMessage> {
        return try {
            retryMechanism.withRetry {
                val messagesRef = database.reference.child("messages").child(groupId)
                val messageRef = if (message.messageId?.isNotEmpty() == true) {
                    messagesRef.child(message.messageId!!)
                } else {
                    messagesRef.push()
                }
                
                val messageWithId = message.copy(
                    messageId = messageRef.key ?: "",
                    timestamp = System.currentTimeMillis() // Use current time instead of ServerValue.TIMESTAMP cast
                )
                
                messageRef.setValue(messageWithId).await()
                Result.Success(messageWithId)
            }
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.FirebaseError(e.message ?: "Failed to send message"))
        }
    }
    
    override suspend fun getMessageById(messageId: String): Result<ChatMessage> {
        return try {
            retryMechanism.withRetry {
                // This would require knowing the groupId, or having a different data structure
                // For now, return not found error
                Result.Error(AppError.ValidationError.NotFound("Message retrieval by ID not implemented"))
            }
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.FirebaseError(e.message ?: "Failed to get message"))
        }
    }
    
    override suspend fun deleteMessage(groupId: String, messageId: String): Result<Unit> {
        return try {
            retryMechanism.withRetry {
                database.reference
                    .child("messages")
                    .child(groupId)
                    .child(messageId)
                    .removeValue()
                    .await()
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError.FirebaseError(e.message ?: "Failed to delete message"))
        }
    }
}
