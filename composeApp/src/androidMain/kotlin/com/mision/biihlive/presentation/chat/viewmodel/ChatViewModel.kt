package com.mision.biihlive.presentation.chat.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mision.biihlive.core.managers.UserIdManager
import com.mision.biihlive.data.chat.repository.ChatFirestoreRepository
import com.mision.biihlive.domain.chat.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

/**
 * ViewModel para una conversación individual de chat
 */
class ChatViewModel(
    private val chatId: String,
    private val context: Context,
    private val chatRepository: ChatFirestoreRepository = ChatFirestoreRepository(context)
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _chat = MutableStateFlow<Chat?>(null)
    val chat = _chat.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText = _messageText.asStateFlow()

    private val _typingUsers = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val typingUsers = _typingUsers.asStateFlow()

    // Computed properties
    val canSendMessage = messageText.map { it.trim().isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    // Combinar mensajes reales con optimistas para la UI
    val allMessages = combine(_messages, _uiState) { realMessages, uiState ->
        (realMessages + uiState.optimisticMessages).sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val otherParticipants = _chat.map { chat ->
        try {
            val currentUserId = UserIdManager.getInstance(context).getCurrentUserId()
            chat?.participants?.filter { it.userId != currentUserId } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val chatTitle = _chat.map { chat ->
        try {
            when {
                chat == null -> "Chat"
                chat.isGroup -> chat.groupName ?: "Grupo"
                else -> {
                    val currentUserId = UserIdManager.getInstance(context).getCurrentUserId()
                    val otherUser = chat.participants.firstOrNull { it.userId != currentUserId }
                    otherUser?.nickname ?: "Usuario"
                }
            }
        } catch (e: Exception) {
            "Chat"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "Chat")

    init {
        Log.d(TAG, "🚀 Inicializando ChatViewModel para chat: $chatId")
        loadChat()
        loadMessages()
        observeMessagesRealTime()
    }

    private fun loadChat() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Cargando información del chat: $chatId")

                chatRepository.getChatById(chatId).fold(
                    onSuccess = { chatData ->
                        Log.d(TAG, "✅ Chat cargado: ${chatData.id}")
                        _chat.value = chatData
                        _uiState.update { it.copy(chatError = null) }

                        // Observar estado de presencia del otro usuario en tiempo real
                        observeOtherUserPresence(chatData)
                    },
                    onFailure = { error ->
                        Log.d(TAG, "ℹ️ Chat no existe aún (se creará al enviar primer mensaje): ${error.message}")
                        // No es un error - el chat se creará automáticamente al enviar el primer mensaje
                        _chat.value = null
                        _uiState.update { it.copy(chatError = null) }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción cargando chat: ${e.message}")
                _uiState.update {
                    it.copy(chatError = e.message ?: "Error desconocido")
                }
            }
        }
    }

    private fun observeOtherUserPresence(chatData: Chat) {
        viewModelScope.launch {
            try {
                val currentUserId = UserIdManager.getInstance(context).getCurrentUserId()

                // Encontrar el otro usuario (no el actual)
                val otherUser = chatData.participants.find { it.userId != currentUserId }

                if (otherUser != null) {
                    Log.d(TAG, "🟢 Iniciando observación de presencia en tiempo real para: ${otherUser.userId}")

                    // Actualizar userId inmediatamente
                    _uiState.update {
                        it.copy(otherUserId = otherUser.userId)
                    }

                    // Observar cambios de presencia en tiempo real
                    chatRepository.observeUserPresence(otherUser.userId)
                        .catch { error ->
                            Log.e(TAG, "❌ Error observando presencia de ${otherUser.userId}: ${error.message}")
                        }
                        .collect { (isOnline, showStatus) ->
                            Log.d(TAG, "🔄 Presencia actualizada en tiempo real: online=$isOnline, showStatus=$showStatus")

                            _uiState.update {
                                it.copy(
                                    otherUserIsOnline = isOnline,
                                    showOnlineStatus = showStatus
                                )
                            }
                        }
                } else {
                    Log.d(TAG, "⚠️ No se encontró otro usuario en el chat")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error iniciando observación de presencia: ${e.message}")
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingMessages = true) }

                Log.d(TAG, "🔄 Cargando mensajes del chat: $chatId")

                chatRepository.getMessages(chatId).fold(
                    onSuccess = { messageList ->
                        Log.d(TAG, "✅ Mensajes cargados: ${messageList.size}")
                        _messages.value = messageList
                        _uiState.update {
                            it.copy(
                                isLoadingMessages = false,
                                messagesError = null
                            )
                        }

                        // Marcar mensajes como leídos
                        markMessagesAsRead()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error cargando mensajes: ${error.message}")
                        _uiState.update {
                            it.copy(
                                isLoadingMessages = false,
                                messagesError = error.message ?: "Error cargando mensajes"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción cargando mensajes: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoadingMessages = false,
                        messagesError = e.message ?: "Error desconocido"
                    )
                }
            }
        }
    }

    private fun observeMessagesRealTime() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "👁️ Iniciando observación en tiempo real para chat: $chatId")

                chatRepository.observeMessages(chatId).collect { messageList ->
                    Log.d(TAG, "🔄 Mensajes actualizados en tiempo real: ${messageList.size}")
                    _messages.value = messageList

                    // Auto-marcar como leído cuando llegan nuevos mensajes
                    markMessagesAsRead()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en observación tiempo real: ${e.message}")
            }
        }
    }

    fun sendMessage() {
        val text = _messageText.value.trim()
        if (text.isEmpty()) {
            Log.w(TAG, "⚠️ Intento de enviar mensaje vacío")
            return
        }

        viewModelScope.launch {
            try {
                val currentUserId = UserIdManager.getInstance(context).getCurrentUserId()
                val localId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()

                // 1. ENVÍO OPTIMISTA - Crear mensaje inmediatamente
                val optimisticMessage = Message(
                    id = "", // Temporal, se actualizará con el ID real
                    chatId = chatId,
                    senderId = currentUserId,
                    senderName = "Tú", // Placeholder
                    content = text,
                    type = MessageType.TEXT,
                    timestamp = timestamp,
                    isRead = false,
                    isDelivered = false,
                    status = MessageStatus.SENDING, // Estado inicial optimista
                    mediaUrl = null,
                    thumbnailUrl = null,
                    replyTo = null,
                    replyToMessageId = null,
                    isEdited = false,
                    editedAt = null
                )

                // 2. MOSTRAR INMEDIATAMENTE en UI
                _uiState.update { currentState ->
                    currentState.copy(
                        optimisticMessages = currentState.optimisticMessages + optimisticMessage,
                        sendError = null
                    )
                }

                // 3. LIMPIAR TEXTO INMEDIATAMENTE
                _messageText.value = ""

                Log.d(TAG, "🚀 Mensaje optimista agregado, enviando en background...")

                // 4. ENVIAR EN BACKGROUND
                chatRepository.sendMessage(
                    chatId = chatId,
                    content = text,
                    type = MessageType.TEXT
                ).fold(
                    onSuccess = { sentMessage ->
                        Log.d(TAG, "✅ Mensaje enviado exitosamente: ${sentMessage.id}")

                        // 5. REMOVER mensaje optimista y dejar que el listener real-time maneje el resto
                        _uiState.update { currentState ->
                            currentState.copy(
                                optimisticMessages = currentState.optimisticMessages.filter {
                                    it != optimisticMessage
                                }
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error enviando mensaje: ${error.message}")

                        // 6. MARCAR mensaje optimista como FALLIDO
                        _uiState.update { currentState ->
                            val updatedOptimistic = currentState.optimisticMessages.map { msg ->
                                if (msg == optimisticMessage) {
                                    msg.copy(status = MessageStatus.FAILED)
                                } else {
                                    msg
                                }
                            }
                            currentState.copy(
                                optimisticMessages = updatedOptimistic,
                                sendError = "Error enviando mensaje: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción enviando mensaje: ${e.message}")
                _uiState.update {
                    it.copy(sendError = "Error: ${e.message}")
                }
            }
        }
    }

    fun updateMessageText(text: String) {
        _messageText.value = text

        // TODO: Implementar indicador de "escribiendo"
        // setTypingStatus(text.isNotEmpty())
    }

    fun loadMoreMessages() {
        val oldestMessage = _messages.value.firstOrNull()
        if (oldestMessage == null || _uiState.value.isLoadingMoreMessages) {
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingMoreMessages = true) }

                Log.d(TAG, "📜 Cargando más mensajes anteriores a: ${oldestMessage.timestamp}")

                chatRepository.getMessages(
                    chatId = chatId,
                    limit = 20,
                    beforeTimestamp = oldestMessage.timestamp
                ).fold(
                    onSuccess = { olderMessages ->
                        if (olderMessages.isNotEmpty()) {
                            Log.d(TAG, "✅ Mensajes anteriores cargados: ${olderMessages.size}")
                            _messages.update { currentMessages ->
                                olderMessages + currentMessages
                            }
                        } else {
                            Log.d(TAG, "📄 No hay más mensajes anteriores")
                            _uiState.update { it.copy(hasMoreMessages = false) }
                        }

                        _uiState.update { it.copy(isLoadingMoreMessages = false) }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error cargando mensajes anteriores: ${error.message}")
                        _uiState.update {
                            it.copy(
                                isLoadingMoreMessages = false,
                                messagesError = error.message ?: "Error cargando mensajes"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción cargando mensajes anteriores: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoadingMoreMessages = false,
                        messagesError = e.message ?: "Error desconocido"
                    )
                }
            }
        }
    }

    private fun markMessagesAsRead() {
        viewModelScope.launch {
            try {
                val currentUserId = UserIdManager.getInstance(context).getCurrentUserId()
                val unreadMessages = _messages.value.filter { message ->
                    !message.isRead && message.senderId != currentUserId
                }

                if (unreadMessages.isEmpty()) return@launch

                val messageIds = unreadMessages.map { it.id }
                Log.d(TAG, "👁️ Marcando ${messageIds.size} mensajes como leídos")

                chatRepository.markAsRead(chatId, messageIds).fold(
                    onSuccess = {
                        Log.d(TAG, "✅ Mensajes marcados como leídos")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error marcando como leídos: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción marcando como leídos: ${e.message}")
            }
        }
    }

    fun refreshMessages() {
        Log.d(TAG, "🔄 Refrescando mensajes")
        loadMessages()
    }

    fun refreshChat() {
        Log.d(TAG, "🔄 Refrescando chat")
        loadChat()
        loadMessages()
    }

    fun sendImage(imageUri: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSendingMessage = true) }

                Log.d(TAG, "📷 Enviando imagen: $imageUri")

                // TODO: Implementar subida de imagen usando S3ClientProvider
                chatRepository.uploadMedia(imageUri, MessageType.IMAGE).fold(
                    onSuccess = { mediaUrl ->
                        // Enviar mensaje con URL de la imagen
                        chatRepository.sendMessage(
                            chatId = chatId,
                            content = "",
                            type = MessageType.IMAGE,
                            mediaUrl = mediaUrl
                        ).fold(
                            onSuccess = { message ->
                                Log.d(TAG, "✅ Imagen enviada: ${message.id}")
                                _uiState.update {
                                    it.copy(
                                        isSendingMessage = false,
                                        sendError = null
                                    )
                                }
                            },
                            onFailure = { error ->
                                Log.e(TAG, "❌ Error enviando imagen: ${error.message}")
                                _uiState.update {
                                    it.copy(
                                        isSendingMessage = false,
                                        sendError = "Error enviando imagen"
                                    )
                                }
                            }
                        )
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error subiendo imagen: ${error.message}")
                        _uiState.update {
                            it.copy(
                                isSendingMessage = false,
                                sendError = "Error subiendo imagen"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción enviando imagen: ${e.message}")
                _uiState.update {
                    it.copy(
                        isSendingMessage = false,
                        sendError = e.message ?: "Error desconocido"
                    )
                }
            }
        }
    }

    fun replyToMessage(originalMessage: Message, replyText: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSendingMessage = true) }

                Log.d(TAG, "↩️ Respondiendo a mensaje: ${originalMessage.id}")

                chatRepository.sendMessage(
                    chatId = chatId,
                    content = replyText,
                    type = MessageType.TEXT
                    // TODO: Agregar parámetro replyToMessageId
                ).fold(
                    onSuccess = { message ->
                        Log.d(TAG, "✅ Respuesta enviada: ${message.id}")
                        _uiState.update {
                            it.copy(
                                isSendingMessage = false,
                                sendError = null,
                                replyingTo = null
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error enviando respuesta: ${error.message}")
                        _uiState.update {
                            it.copy(
                                isSendingMessage = false,
                                sendError = error.message ?: "Error enviando respuesta"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción enviando respuesta: ${e.message}")
                _uiState.update {
                    it.copy(
                        isSendingMessage = false,
                        sendError = e.message ?: "Error desconocido"
                    )
                }
            }
        }
    }

    fun setReplyingTo(message: Message?) {
        _uiState.update { it.copy(replyingTo = message) }
    }

    fun clearErrors() {
        _uiState.update {
            it.copy(
                chatError = null,
                messagesError = null,
                sendError = null
            )
        }
    }

    fun isOwnMessage(message: Message): Boolean {
        return try {
            val currentUserId = runBlocking { UserIdManager.getInstance(context).getCurrentUserId() }
            message.senderId == currentUserId
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extrae los IDs de participantes del chatId
     * Formato esperado: chat_userId1_userId2
     */
    private fun extractParticipantsFromChatId(chatId: String): List<String> {
        return try {
            if (chatId.startsWith("chat_")) {
                val parts = chatId.removePrefix("chat_").split("_")
                if (parts.size >= 2) {
                    listOf(parts[0], parts[1])
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extrayendo participantes de chatId: ${e.message}")
            emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🔄 ChatViewModel destruido para chat: $chatId")
    }
}

/**
 * Estado de UI para el chat individual
 */
data class ChatUiState(
    val isLoadingMessages: Boolean = false,
    val isLoadingMoreMessages: Boolean = false,
    val isSendingMessage: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val chatError: String? = null,
    val messagesError: String? = null,
    val sendError: String? = null,
    val replyingTo: Message? = null,
    val isTyping: Boolean = false,
    val otherUserIsOnline: Boolean = false,
    val showOnlineStatus: Boolean = false,
    val otherUserId: String? = null,
    val optimisticMessages: List<Message> = emptyList()
)