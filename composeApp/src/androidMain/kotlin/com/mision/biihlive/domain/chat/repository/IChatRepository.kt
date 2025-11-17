package com.mision.biihlive.domain.chat.repository

import com.mision.biihlive.domain.chat.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Interface del repositorio de chat siguiendo Clean Architecture
 */
interface IChatRepository {
    
    // Operaciones de Chat
    suspend fun getChats(userId: String): Result<List<ChatPreview>>
    suspend fun getChatById(chatId: String): Result<Chat>
    suspend fun createChat(participantIds: List<String>, isGroup: Boolean = false): Result<Chat>
    suspend fun deleteChat(chatId: String): Result<Unit>
    suspend fun archiveChat(chatId: String): Result<Unit>
    suspend fun pinChat(chatId: String, isPinned: Boolean): Result<Unit>
    suspend fun muteChat(chatId: String, isMuted: Boolean): Result<Unit>
    
    // Operaciones de Mensajes
    suspend fun getMessages(chatId: String, limit: Int = 20, beforeTimestamp: Long? = null): Result<List<Message>>
    suspend fun sendMessage(chatId: String, content: String, type: MessageType = MessageType.TEXT, mediaUrl: String? = null): Result<Message>
    suspend fun deleteMessage(messageId: String): Result<Unit>
    suspend fun editMessage(messageId: String, newContent: String): Result<Message>
    suspend fun markAsRead(chatId: String, messageIds: List<String>): Result<Unit>
    
    // Operaciones en tiempo real
    fun observeChat(chatId: String): Flow<Chat>
    fun observeMessages(chatId: String): Flow<List<Message>>
    fun observeTypingStatus(chatId: String): Flow<Map<String, Boolean>>
    fun observeUnreadCount(userId: String): Flow<Int>
    
    // Búsqueda y filtros
    suspend fun searchChats(query: String): Result<List<ChatPreview>>
    suspend fun searchMessages(chatId: String, query: String): Result<List<Message>>
    suspend fun getChatsByType(userType: UserType): Result<List<ChatPreview>>
    
    // Operaciones de grupo
    suspend fun addParticipant(chatId: String, userId: String): Result<Unit>
    suspend fun removeParticipant(chatId: String, userId: String): Result<Unit>
    suspend fun updateGroupInfo(chatId: String, name: String?, imageUrl: String?): Result<Unit>
    
    // Typing indicators
    suspend fun setTypingStatus(chatId: String, isTyping: Boolean): Result<Unit>
    
    // Media
    suspend fun uploadMedia(filePath: String, mediaType: MessageType): Result<String>
}