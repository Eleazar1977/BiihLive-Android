package com.mision.biihlive.data.chat.dto

import kotlinx.serialization.Serializable

/**
 * DTOs para comunicación con API Gateway / DynamoDB
 */

@Serializable
data class ChatDto(
    val chatId: String,
    val participants: List<String>,
    val lastMessage: MessageDto? = null,
    val unreadCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val isGroup: Boolean = false,
    val groupName: String? = null,
    val groupImage: String? = null,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val chatType: String = "DIRECT"
)

@Serializable
data class MessageDto(
    val messageId: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val type: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val replyTo: String? = null,
    val isEdited: Boolean = false,
    val editedAt: Long? = null
)

@Serializable
data class ChatUserDto(
    val userId: String,
    val nickname: String,
    val imageUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null,
    val userType: String = "REGULAR"
)

@Serializable
data class ChatPreviewDto(
    val chatId: String,
    val otherUser: ChatUserDto,
    val lastMessage: MessageDto? = null,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false
)

@Serializable
data class CreateChatRequest(
    val userId: String,
    val otherUserId: String,
    val isGroup: Boolean = false,
    val groupName: String? = null
)

@Serializable
data class SendMessageRequest(
    val chatId: String,
    val senderId: String,
    val content: String,
    val type: String = "TEXT",
    val mediaUrl: String? = null
)

@Serializable
data class ChatListResponse(
    val chats: List<ChatPreviewDto>,
    val totalUnread: Int
)

@Serializable
data class MessagesResponse(
    val messages: List<MessageDto>,
    val hasMore: Boolean,
    val nextToken: String? = null
)