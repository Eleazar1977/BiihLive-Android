package com.mision.biihlive.domain.chat.model

/**
 * Modelo de dominio para un chat/conversación
 */
data class Chat(
    val id: String,
    val participants: List<ChatUser>,
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val isGroup: Boolean = false,
    val groupName: String? = null,
    val groupImage: String? = null,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val chatType: ChatType = ChatType.DIRECT
)

enum class ChatType {
    DIRECT,      // Chat 1 a 1
    GROUP,       // Grupo normal
    BROADCAST,   // Lista de difusión
    CHANNEL      // Canal (solo admins pueden escribir)
}

/**
 * Información resumida del chat para mostrar en listas
 */
data class ChatPreview(
    val chatId: String,
    val displayName: String,
    val displayImage: String?,
    val lastMessageText: String?,
    val lastMessageTime: Long?,
    val unreadCount: Int,
    val isPinned: Boolean,
    val isMuted: Boolean,
    val userType: UserType,
    val isOnline: Boolean = false,
    val allowsStatusVisible: Boolean = false // Campo mostrarEstado del usuario
)