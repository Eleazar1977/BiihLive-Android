package com.mision.biihlive.domain.chat.model

/**
 * Modelo de dominio para un mensaje en el chat
 */
data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val type: MessageType,
    val timestamp: Long,
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    val status: MessageStatus = MessageStatus.SENT,  // Estado del mensaje
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val replyTo: String? = null,
    val replyToMessageId: String? = null,  // ID del mensaje al que responde
    val isEdited: Boolean = false,
    val editedAt: Long? = null
)

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    LOCATION,
    STICKER,
    GIF,
    SYSTEM // Para mensajes del sistema como "Usuario se unió al chat"
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}