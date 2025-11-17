package com.mision.biihlive.presentation.chat.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mision.biihlive.core.managers.UserIdManager
import com.mision.biihlive.data.chat.repository.ChatFirestoreRepository
import com.mision.biihlive.domain.chat.model.ChatPreview
import com.mision.biihlive.domain.chat.model.UserType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel para la lista de chats/conversaciones
 */
class MessagesListViewModel(
    private val context: Context,
    private val chatRepository: ChatFirestoreRepository = ChatFirestoreRepository(context)
) : ViewModel() {

    companion object {
        private const val TAG = "MessagesListViewModel"
    }

    private val _uiState = MutableStateFlow(MessagesListUiState())
    val uiState = _uiState.asStateFlow()

    private val _chats = MutableStateFlow<List<ChatPreview>>(emptyList())
    val chats = _chats.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ChatFilter.ALL)
    val selectedFilter = _selectedFilter.asStateFlow()

    // Filtrar chats basado en query de búsqueda y filtro seleccionado
    val filteredChats = combine(
        _chats,
        _searchQuery,
        _selectedFilter
    ) { chats, query, filter ->
        var filtered = chats

        // Aplicar filtro por tipo
        filtered = when (filter) {
            ChatFilter.ALL -> filtered
            ChatFilter.UNREAD -> filtered.filter { it.unreadCount > 0 }
            ChatFilter.PINNED -> filtered.filter { it.isPinned }
            ChatFilter.ARCHIVED -> filtered.filter { false } // TODO: Implementar archived
            ChatFilter.MUTED -> filtered.filter { it.isMuted }
        }

        // Aplicar filtro de búsqueda
        if (query.isNotBlank()) {
            filtered = filtered.filter { chat ->
                chat.displayName.contains(query, ignoreCase = true) ||
                chat.lastMessageText?.contains(query, ignoreCase = true) == true
            }
        }

        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadChats()
    }

    fun loadChats() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val currentUserId = try {
                    UserIdManager.getInstance(context).getCurrentUserId()
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Usuario no autenticado: ${e.message}"
                        )
                    }
                    return@launch
                }

                Log.d(TAG, "🔄 Cargando chats para usuario: $currentUserId")

                chatRepository.getChats(currentUserId).fold(
                    onSuccess = { chatList ->
                        Log.d(TAG, "✅ Chats cargados: ${chatList.size}")
                        _chats.value = chatList
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                totalUnreadCount = chatList.sumOf { chat -> chat.unreadCount }
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error cargando chats: ${error.message}")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Error desconocido"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción cargando chats: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error desconocido"
                    )
                }
            }
        }
    }

    fun refreshChats() {
        Log.d(TAG, "🔄 Refrescando lista de chats")
        loadChats()
    }

    fun searchChats(query: String) {
        Log.d(TAG, "🔍 Buscando chats: '$query'")
        _searchQuery.value = query
    }

    fun setFilter(filter: ChatFilter) {
        Log.d(TAG, "🔽 Aplicando filtro: $filter")
        _selectedFilter.value = filter
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun createNewChat(participantIds: List<String>, isGroup: Boolean = false) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isCreatingChat = true) }

                Log.d(TAG, "🔄 Creando nuevo chat con: $participantIds")

                chatRepository.createChat(participantIds, isGroup).fold(
                    onSuccess = { chat ->
                        Log.d(TAG, "✅ Chat creado: ${chat.id}")
                        _uiState.update { it.copy(isCreatingChat = false) }
                        // Recargar lista para mostrar el nuevo chat
                        loadChats()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error creando chat: ${error.message}")
                        _uiState.update {
                            it.copy(
                                isCreatingChat = false,
                                error = error.message ?: "Error creando chat"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción creando chat: ${e.message}")
                _uiState.update {
                    it.copy(
                        isCreatingChat = false,
                        error = e.message ?: "Error desconocido"
                    )
                }
            }
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ Eliminando chat: $chatId")

                chatRepository.deleteChat(chatId).fold(
                    onSuccess = {
                        Log.d(TAG, "✅ Chat eliminado: $chatId")
                        // Remover de la lista local
                        _chats.update { currentChats ->
                            currentChats.filter { it.chatId != chatId }
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error eliminando chat: ${error.message}")
                        _uiState.update {
                            it.copy(error = error.message ?: "Error eliminando chat")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción eliminando chat: ${e.message}")
                _uiState.update {
                    it.copy(error = e.message ?: "Error desconocido")
                }
            }
        }
    }

    fun pinChat(chatId: String, isPinned: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📌 ${if (isPinned) "Fijando" else "Desfijando"} chat: $chatId")

                chatRepository.pinChat(chatId, isPinned).fold(
                    onSuccess = {
                        // Actualizar en la lista local
                        _chats.update { currentChats ->
                            currentChats.map { chat ->
                                if (chat.chatId == chatId) {
                                    chat.copy(isPinned = isPinned)
                                } else {
                                    chat
                                }
                            }
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error fijando chat: ${error.message}")
                        _uiState.update {
                            it.copy(error = error.message ?: "Error fijando chat")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción fijando chat: ${e.message}")
            }
        }
    }

    fun muteChat(chatId: String, isMuted: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔇 ${if (isMuted) "Silenciando" else "Desilenciando"} chat: $chatId")

                chatRepository.muteChat(chatId, isMuted).fold(
                    onSuccess = {
                        // Actualizar en la lista local
                        _chats.update { currentChats ->
                            currentChats.map { chat ->
                                if (chat.chatId == chatId) {
                                    chat.copy(isMuted = isMuted)
                                } else {
                                    chat
                                }
                            }
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error silenciando chat: ${error.message}")
                        _uiState.update {
                            it.copy(error = error.message ?: "Error silenciando chat")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción silenciando chat: ${e.message}")
            }
        }
    }

    fun archiveChat(chatId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📦 Archivando chat: $chatId")

                chatRepository.archiveChat(chatId).fold(
                    onSuccess = {
                        // Remover de la lista principal (los archivados se ven en filtro)
                        _chats.update { currentChats ->
                            currentChats.filter { it.chatId != chatId }
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error archivando chat: ${error.message}")
                        _uiState.update {
                            it.copy(error = error.message ?: "Error archivando chat")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción archivando chat: ${e.message}")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun markChatAsRead(chatId: String) {
        // Actualizar conteo local inmediatamente
        _chats.update { currentChats ->
            currentChats.map { chat ->
                if (chat.chatId == chatId) {
                    chat.copy(unreadCount = 0)
                } else {
                    chat
                }
            }
        }

        // Actualizar conteo total
        _uiState.update { currentState ->
            currentState.copy(
                totalUnreadCount = _chats.value.sumOf { it.unreadCount }
            )
        }
    }
}

/**
 * Estado de UI para la lista de mensajes
 */
data class MessagesListUiState(
    val isLoading: Boolean = false,
    val isCreatingChat: Boolean = false,
    val error: String? = null,
    val totalUnreadCount: Int = 0,
    val isRefreshing: Boolean = false
)

/**
 * Filtros disponibles para la lista de chats
 */
enum class ChatFilter {
    ALL,        // Todos los chats
    UNREAD,     // Solo con mensajes no leídos
    PINNED,     // Solo chats fijados
    ARCHIVED,   // Solo chats archivados
    MUTED       // Solo chats silenciados
}