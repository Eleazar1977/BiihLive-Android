package com.mision.biihlive.data.chat.mapper

import com.mision.biihlive.data.chat.dto.*
import com.mision.biihlive.domain.chat.model.*

/**
 * Mapper para convertir entre DTOs y modelos de dominio
 */
object ChatMapper {
    
    fun ChatDto.toDomain(participants: List<ChatUser>): Chat {
        return Chat(
            id = chatId,
            participants = participants,
            lastMessage = lastMessage?.toDomain(),
            unreadCount = unreadCount,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isGroup = isGroup,
            groupName = groupName,
            groupImage = groupImage,
            isPinned = isPinned,
            isMuted = isMuted,
            isArchived = isArchived,
            chatType = ChatType.valueOf(chatType)
        )
    }
    
    fun MessageDto.toDomain(): Message {
        return Message(
            id = messageId,
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            content = content,
            type = MessageType.valueOf(type),
            timestamp = timestamp,
            isRead = isRead,
            isDelivered = isDelivered,
            mediaUrl = mediaUrl,
            thumbnailUrl = thumbnailUrl,
            replyTo = replyTo,
            isEdited = isEdited,
            editedAt = editedAt
        )
    }
    
    fun ChatUserDto.toDomain(): ChatUser {
        return ChatUser(
            userId = userId,
            nickname = nickname,
            imageUrl = imageUrl,
            lastSeen = lastSeen,
            userType = UserType.valueOf(userType)
        )
    }
    
    fun ChatPreviewDto.toDomain(): ChatPreview {
        return ChatPreview(
            chatId = chatId,
            displayName = otherUser.nickname,
            displayImage = otherUser.imageUrl,
            lastMessageText = lastMessage?.content,
            lastMessageTime = lastMessage?.timestamp,
            unreadCount = unreadCount,
            isPinned = isPinned,
            isMuted = isMuted,
            userType = UserType.valueOf(otherUser.userType)
        )
    }
    
    // Conversiones inversas (Domain to DTO)
    
    fun Chat.toDto(): ChatDto {
        return ChatDto(
            chatId = id,
            participants = participants.map { it.userId },
            lastMessage = lastMessage?.toDto(),
            unreadCount = unreadCount,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isGroup = isGroup,
            groupName = groupName,
            groupImage = groupImage,
            isPinned = isPinned,
            isMuted = isMuted,
            isArchived = isArchived,
            chatType = chatType.name
        )
    }
    
    fun Message.toDto(): MessageDto {
        return MessageDto(
            messageId = id,
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            content = content,
            type = type.name,
            timestamp = timestamp,
            isRead = isRead,
            isDelivered = isDelivered,
            mediaUrl = mediaUrl,
            thumbnailUrl = thumbnailUrl,
            replyTo = replyTo,
            isEdited = isEdited,
            editedAt = editedAt
        )
    }
    
    fun ChatUser.toDto(): ChatUserDto {
        return ChatUserDto(
            userId = userId,
            nickname = nickname,
            imageUrl = imageUrl,
            lastSeen = lastSeen,
            userType = userType.name
        )
    }
}