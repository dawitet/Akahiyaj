package com.dawitf.akahidegn.data.mapper

import com.dawitf.akahidegn.ChatMessage
import com.dawitf.akahidegn.data.local.entity.ChatMessageEntity

fun ChatMessageEntity.toDomainModel(): ChatMessage {
    return ChatMessage(
        messageId = id,
        senderId = senderId,
        senderName = senderName,
        text = message,
        timestamp = timestamp
    )
}

fun ChatMessage.toEntity(groupId: String): ChatMessageEntity {
    return ChatMessageEntity(
        id = this.messageId ?: "",
        groupId = groupId,
        senderId = this.senderId ?: "",
        senderName = this.senderName ?: "",
        message = this.text ?: "",
        timestamp = this.timestamp ?: 0L,
        isLocal = false
    )
}

// Extension for mapping list of entities to domain models
fun List<ChatMessageEntity>.toDomainModels(): List<ChatMessage> = map { it.toDomainModel() }

fun List<ChatMessage>.toEntities(groupId: String): List<ChatMessageEntity> {
    return map { it.toEntity(groupId) }
}
