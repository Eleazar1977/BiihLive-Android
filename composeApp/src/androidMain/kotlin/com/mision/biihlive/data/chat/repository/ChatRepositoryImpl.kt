package com.mision.biihlive.data.chat.repository

import android.content.Context
import com.mision.biihlive.data.chat.datasource.ChatRemoteDataSource
import com.mision.biihlive.data.chat.dto.*
import com.mision.biihlive.data.chat.mapper.ChatMapper.toDomain
import com.mision.biihlive.data.chat.mapper.ChatMapper.toDto
import com.mision.biihlive.domain.chat.model.*
import com.mision.biihlive.domain.chat.repository.IChatRepository
import com.mision.biihlive.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Implementación del repositorio de chat
 * DEPRECATED: Sistema de chat será reimplementado con AppSync GraphQL
 * - Reemplazar ChatRemoteDataSource con AppSyncRepository
 * - Crear mutations y queries GraphQL para chat
 * - Mantener la misma interfaz IChatRepository
 */
@Deprecated(
    message = "Sistema de chat será reimplementado con AppSync GraphQL",
    level = DeprecationLevel.WARNING
)
class ChatRepositoryImpl(
    private val context: Context,
    // TODO: Reemplazar con AppSyncRepository cuando se complete migración
    private val remoteDataSource: ChatRemoteDataSource = ChatRemoteDataSource()
) : IChatRepository {
    
    // Cache en memoria para optimización
    private val messagesCache = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    private val chatsCache = MutableStateFlow<List<ChatPreview>>(emptyList())
    private val typingStatusCache = MutableStateFlow<Map<String, Map<String, Boolean>>>(emptyMap())
    
    override suspend fun getChats(userId: String): Result<List<ChatPreview>> {
        return remoteDataSource.getChats(userId).map { response ->
            val previews = response.chats.map { it.toDomain() }
            chatsCache.value = previews
            previews
        }
    }
    
    override suspend fun getChatById(chatId: String): Result<Chat> {
        return remoteDataSource.getChatById(chatId).map { dto ->
            // Los participantes vienen del backend con los datos reales
            val participants = dto.participants.map { userId ->
                ChatUser(
                    userId = userId,
                    nickname = userId, // Se actualizará desde el backend
                    imageUrl = null,
                    lastSeen = System.currentTimeMillis()
                )
            }
            dto.toDomain(participants)
        }
    }
    
    override suspend fun createChat(participantIds: List<String>, isGroup: Boolean): Result<Chat> {
        val currentUserId = getCurrentUserId()
        val request = CreateChatRequest(
            userId = currentUserId,
            otherUserId = participantIds.first(), // Para chat 1 a 1
            isGroup = isGroup
        )

        return remoteDataSource.createChat(request).map { dto ->
            // Los participantes vienen del backend con los datos reales
            val participants = participantIds.map { userId ->
                ChatUser(
                    userId = userId,
                    nickname = userId, // Se actualizará desde el backend
                    imageUrl = null,
                    lastSeen = System.currentTimeMillis()
                )
            }
            dto.toDomain(participants)
        }
    }
    
    override suspend fun deleteChat(chatId: String): Result<Unit> {
        return remoteDataSource.deleteChat(chatId)
    }
    
    override suspend fun archiveChat(chatId: String): Result<Unit> {
        // Implementar con API
        return Result.success(Unit)
    }
    
    override suspend fun pinChat(chatId: String, isPinned: Boolean): Result<Unit> {
        // Implementar con API
        return Result.success(Unit)
    }
    
    override suspend fun muteChat(chatId: String, isMuted: Boolean): Result<Unit> {
        // Implementar con API
        return Result.success(Unit)
    }
    
    override suspend fun getMessages(chatId: String, limit: Int, beforeTimestamp: Long?): Result<List<Message>> {
        return remoteDataSource.getMessages(chatId, limit, beforeTimestamp).map { response ->
            val messages = response.messages.map { it.toDomain() }
            
            // Actualizar cache
            val currentCache = messagesCache.value.toMutableMap()
            currentCache[chatId] = messages
            messagesCache.value = currentCache
            
            messages
        }
    }
    
    override suspend fun sendMessage(
        chatId: String,
        content: String,
        type: MessageType,
        mediaUrl: String?
    ): Result<Message> {
        val currentUserId = getCurrentUserId()
        val currentUserName = getCurrentUserName()
        
        val request = SendMessageRequest(
            chatId = chatId,
            senderId = currentUserId,
            content = content,
            type = type.name,
            mediaUrl = mediaUrl
        )
        
        return remoteDataSource.sendMessage(request).map { dto ->
            val message = dto.toDomain()
            
            // Actualizar cache local inmediatamente
            val currentCache = messagesCache.value.toMutableMap()
            val chatMessages = currentCache[chatId]?.toMutableList() ?: mutableListOf()
            chatMessages.add(message)
            currentCache[chatId] = chatMessages
            messagesCache.value = currentCache
            
            message
        }
    }
    
    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        // Implementar con API
        return Result.success(Unit)
    }
    
    override suspend fun editMessage(messageId: String, newContent: String): Result<Message> {
        // Implementar con API
        return Result.failure(NotImplementedError("Edición de mensajes no implementada"))
    }
    
    override suspend fun markAsRead(chatId: String, messageIds: List<String>): Result<Unit> {
        return remoteDataSource.markAsRead(chatId, messageIds)
    }
    
    override fun observeChat(chatId: String): Flow<Chat> {
        // Por ahora usamos polling cada 5 segundos
        return flow {
            while (true) {
                getChatById(chatId).onSuccess { chat ->
                    emit(chat)
                }
                delay(5000) // Polling cada 5 segundos
            }
        }
    }
    
    override fun observeMessages(chatId: String): Flow<List<Message>> {
        // Observar el cache y actualizar con polling más frecuente
        return flow {
            // Emitir mensajes del cache inmediatamente si existen
            messagesCache.value[chatId]?.let { cachedMessages ->
                if (cachedMessages.isNotEmpty()) {
                    emit(cachedMessages)
                }
            }

            // Polling más frecuente para mensajes en tiempo real
            while (true) {
                getMessages(chatId).onSuccess { messages ->
                    // Solo emitir si hay cambios
                    val currentCache = messagesCache.value[chatId] ?: emptyList()
                    if (messages != currentCache) {
                        emit(messages)
                    }
                }
                delay(1500) // Polling cada 1.5 segundos para mejor respuesta
            }
        }
    }
    
    override fun observeTypingStatus(chatId: String): Flow<Map<String, Boolean>> {
        return typingStatusCache.map { cache ->
            cache[chatId] ?: emptyMap()
        }
    }
    
    override fun observeUnreadCount(userId: String): Flow<Int> {
        return chatsCache.map { chats ->
            chats.sumOf { it.unreadCount }
        }
    }
    
    override suspend fun searchChats(query: String): Result<List<ChatPreview>> {
        // Buscar en cache local por ahora
        val filtered = chatsCache.value.filter { chat ->
            chat.displayName.contains(query, ignoreCase = true) ||
            chat.lastMessageText?.contains(query, ignoreCase = true) == true
        }
        return Result.success(filtered)
    }
    
    override suspend fun searchMessages(chatId: String, query: String): Result<List<Message>> {
        val messages = messagesCache.value[chatId] ?: emptyList()
        val filtered = messages.filter { message ->
            message.content.contains(query, ignoreCase = true)
        }
        return Result.success(filtered)
    }
    
    override suspend fun getChatsByType(userType: UserType): Result<List<ChatPreview>> {
        val filtered = chatsCache.value.filter { chat ->
            chat.userType == userType
        }
        return Result.success(filtered)
    }
    
    override suspend fun addParticipant(chatId: String, userId: String): Result<Unit> {
        // Implementar con API
        return Result.success(Unit)
    }
    
    override suspend fun removeParticipant(chatId: String, userId: String): Result<Unit> {
        // Implementar con API
        return Result.success(Unit)
    }
    
    override suspend fun updateGroupInfo(chatId: String, name: String?, imageUrl: String?): Result<Unit> {
        // Implementar con API
        return Result.success(Unit)
    }
    
    override suspend fun setTypingStatus(chatId: String, isTyping: Boolean): Result<Unit> {
        val currentUserId = getCurrentUserId()
        return remoteDataSource.setTypingStatus(chatId, currentUserId, isTyping).also {
            // Actualizar cache local
            val currentCache = typingStatusCache.value.toMutableMap()
            val chatTyping = currentCache[chatId]?.toMutableMap() ?: mutableMapOf()
            chatTyping[currentUserId] = isTyping
            currentCache[chatId] = chatTyping
            typingStatusCache.value = currentCache
        }
    }
    
    override suspend fun uploadMedia(filePath: String, mediaType: MessageType): Result<String> {
        // TODO: Implementar subida a S3
        return Result.failure(NotImplementedError("Subida de archivos no implementada"))
    }
    
    // Helpers del SessionManager - obtener userId real
    private fun getCurrentUserId(): String {
        // Primero intentar obtener del SessionManager
        val storedId = SessionManager.getUserId(context)
        if (storedId != null && storedId != "default_user" && !storedId.contains("@")) {
            return storedId
        }

        // Si no hay userId válido guardado, debería obtenerse de Cognito
        // pero como esto no es suspend, retornamos default
        // El ViewModel debería asegurarse de guardar el userId correcto
        return storedId ?: "default_user"
    }

    private fun getCurrentUserName(): String {
        return SessionManager.getGoogleName(context)
            ?: SessionManager.getUserEmail(context)?.substringBefore("@")
            ?: "Usuario"
    }
}