package com.dawitf.akahidegn.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.dawitf.akahidegn.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    
    @Query("SELECT * FROM chat_messages WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getMessagesByGroupPaged(groupId: String): PagingSource<Int, ChatMessageEntity>
    
    @Query("SELECT * FROM chat_messages WHERE groupId = :groupId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(groupId: String, limit: Int = 50): Flow<List<ChatMessageEntity>>
    
    @Query("SELECT * FROM chat_messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): ChatMessageEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)
    
    @Update
    suspend fun updateMessage(message: ChatMessageEntity)
    
    @Delete
    suspend fun deleteMessage(message: ChatMessageEntity)
    
    @Query("DELETE FROM chat_messages WHERE groupId = :groupId")
    suspend fun deleteMessagesByGroup(groupId: String)
    
    @Query("DELETE FROM chat_messages WHERE timestamp < :timestamp")
    suspend fun deleteOldMessages(timestamp: Long)
}
