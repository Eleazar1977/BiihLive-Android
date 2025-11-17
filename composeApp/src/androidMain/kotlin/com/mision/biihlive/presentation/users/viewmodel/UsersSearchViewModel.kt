package com.mision.biihlive.presentation.users.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mision.biihlive.data.repository.FirestoreRepository
import com.mision.biihlive.domain.users.model.UserPreview
import com.mision.biihlive.core.managers.UserIdManager
import com.mision.biihlive.utils.SessionManager
import com.mision.biihlive.data.chat.repository.ChatFirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil.Coil
import coil.request.ImageRequest
import coil.request.CachePolicy

data class UsersSearchUiState(
    val isLoading: Boolean = false,
    val users: List<UserPreview> = emptyList(),
    val filteredUsers: List<UserPreview> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null,
    val creatingChatForUser: String? = null, // ID del usuario para el que se está creando chat
    val showOnlineOnly: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreData: Boolean = true,
    val followingUsers: Set<String> = emptySet(), // IDs de usuarios seguidos
    val loadingFollow: Set<String> = emptySet(), // IDs en proceso de follow/unfollow
    val lastDocumentId: String? = null // Para paginación
)

/**
 * ViewModel para la búsqueda de usuarios
 */
class UsersSearchViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
    private val chatRepository: ChatFirestoreRepository = ChatFirestoreRepository(context)
    private val userIdManager: UserIdManager = UserIdManager.getInstance(context)

    private val _uiState = MutableStateFlow(UsersSearchUiState())
    val uiState: StateFlow<UsersSearchUiState> = _uiState.asStateFlow()

    companion object {
        private const val CLOUDFRONT_URL = "https://d183hg75gdabnr.cloudfront.net"
        // Timestamp conocido que funciona para la mayoría de usuarios
        private const val DEFAULT_TIMESTAMP = "1759240530172"
    }

    init {
        android.util.Log.d("UsersSearchViewModel", "🚀 [INIT_DEBUG] Iniciando UsersSearchViewModel")
        loadUsers()
    }

    /**
     * Genera la URL del thumbnail para un usuario
     * Usa un timestamp conocido que funciona para la mayoría
     */
    private fun generateThumbnailUrl(userId: String): String {
        return "$CLOUDFRONT_URL/userprofile/$userId/thumbnail_$DEFAULT_TIMESTAMP.png"
    }
    
    private fun loadUsers() {
        viewModelScope.launch {
            android.util.Log.d("UsersSearchViewModel", "🔄 [LOAD_DEBUG] Iniciando loadUsers()")
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Obtener el userId actual para cargar estados de seguimiento
            val currentUserId = SessionManager.getUserId(context)
            android.util.Log.d("UsersSearchViewModel", "🔄 [LOAD_DEBUG] Usuario actual: $currentUserId")

            // Cargar usuarios desde Firestore y estados de seguimiento en paralelo (20 por página)
            android.util.Log.d("UsersSearchViewModel", "🔄 [LOAD_DEBUG] Cargando usuarios desde Firestore...")
            val usersResult = firestoreRepository.listUsuarios(limit = 20)

            android.util.Log.d("UsersSearchViewModel", "🔄 [LOAD_DEBUG] Cargando estados de seguimiento...")
            val followingResult = if (currentUserId != null) {
                firestoreRepository.getFollowingIds(currentUserId)
            } else {
                android.util.Log.w("UsersSearchViewModel", "🔄 [LOAD_DEBUG] ⚠️ Usuario no logueado, no se cargarán estados de seguimiento")
                Result.success(emptySet())
            }

            usersResult.fold(
                onSuccess = { users ->
                    android.util.Log.d("UsersSearchViewModel", "🔄 [LOAD_DEBUG] ✅ Usuarios obtenidos: ${users.size}")

                    // Obtener datos de presencia para cada usuario en paralelo
                    android.util.Log.d("UsersSearchViewModel", "🔄 [PRESENCE_DEBUG] Obteniendo estados de presencia...")
                    val usersWithPresence = coroutineScope {
                        users.map { user ->
                            async {
                                try {
                                    val (isOnline, allowsStatusVisible) = chatRepository.getUserOnlineStatus(user.userId)
                                    android.util.Log.d("UsersSearchViewModel", "🔄 [PRESENCE_DEBUG] Usuario ${user.nickname}: online=$isOnline, allowsVisible=$allowsStatusVisible")

                                    user.copy(
                                        photoUrl = generateThumbnailUrl(user.userId),
                                        isOnline = isOnline,
                                        mostrarEstado = allowsStatusVisible
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("UsersSearchViewModel", "🔄 [PRESENCE_DEBUG] Error obteniendo presencia para ${user.nickname}: ${e.message}")
                                    user.copy(
                                        photoUrl = generateThumbnailUrl(user.userId),
                                        isOnline = false,
                                        mostrarEstado = false
                                    )
                                }
                            }
                        }.awaitAll()
                    }

                    android.util.Log.d("UsersSearchViewModel", "🔄 [PRESENCE_DEBUG] ✅ Estados de presencia obtenidos para ${usersWithPresence.size} usuarios")

                    // Obtener estados de seguimiento
                    followingResult.fold(
                        onSuccess = { followingIds ->
                            android.util.Log.d("UsersSearchViewModel", "🔄 [LOAD_DEBUG] ✅ Estados de seguimiento obtenidos exitosamente: ${followingIds.size} usuarios seguidos")
                            android.util.Log.d("UsersSearchViewModel", "🔄 [LOAD_DEBUG] ✅ IDs seguidos: $followingIds")

                            val filteredList = filterUsers(usersWithPresence, _uiState.value.searchQuery, _uiState.value.showOnlineOnly)

                            // Actualizar UI con AMBOS datos al mismo tiempo
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    users = usersWithPresence,
                                    filteredUsers = filteredList,
                                    followingUsers = followingIds,
                                    error = null,
                                    lastDocumentId = usersWithPresence.lastOrNull()?.userId,
                                    hasMoreData = usersWithPresence.size >= 20  // Hay más datos si obtuvimos el máximo
                                )
                            }

                            android.util.Log.d("UsersSearchViewModel", "🔄 [LOAD_DEBUG] ✅ UI actualizada con ${usersWithPresence.size} usuarios y ${followingIds.size} seguidos")
                        },
                        onFailure = { error ->
                            android.util.Log.e("UsersSearchViewModel", "🔄 [LOAD_DEBUG] ❌ Error obteniendo estados de seguimiento", error)

                            // Actualizar UI solo con usuarios (sin estados de seguimiento)
                            val filteredList = filterUsers(usersWithPresence, _uiState.value.searchQuery, _uiState.value.showOnlineOnly)
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    users = usersWithPresence,
                                    filteredUsers = filteredList,
                                    followingUsers = emptySet(),
                                    error = null,
                                    lastDocumentId = usersWithPresence.lastOrNull()?.userId,
                                    hasMoreData = usersWithPresence.size >= 20
                                )
                            }
                        }
                    )

                    // TODO: Implementar actualización de presencia en tiempo real cuando esté disponible
                    android.util.Log.d("UsersSearchViewModel", "🔄 [LOAD_DEBUG] ✅ Proceso de carga completado exitosamente")
                },
                onFailure = { error ->
                    android.util.Log.e("UsersSearchViewModel", "🔄 [LOAD_DEBUG] ❌ Error cargando usuarios", error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }
    
    fun searchUsers(query: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val filteredList = filterUsers(currentState.users, query, currentState.showOnlineOnly)

            _uiState.update { state ->
                state.copy(
                    searchQuery = query,
                    filteredUsers = filteredList
                )
            }

            // Si la búsqueda no está vacía, obtener más usuarios para buscar
            if (query.isNotBlank()) {
                // Buscar en un conjunto más amplio de usuarios (primera página más amplia)
                firestoreRepository.listUsuarios(limit = 100)  // Sin searchTerm para obtener más usuarios
                    .onSuccess { todosLosUsuarios ->
                        // Aplicar URLs dinámicas y filtrar localmente
                        val usersWithUrls = todosLosUsuarios.map { usuario ->
                            usuario.copy(photoUrl = generateThumbnailUrl(usuario.userId))
                        }

                        // Filtrar localmente con búsqueda avanzada
                        val searchResults = filterUsers(usersWithUrls, query, _uiState.value.showOnlineOnly)

                        _uiState.update { state ->
                            state.copy(
                                filteredUsers = searchResults,
                                hasMoreData = false  // Desactivar paginación durante búsqueda
                            )
                        }

                        android.util.Log.d("UsersSearchViewModel", "Búsqueda avanzada completada: ${searchResults.size} resultados de ${usersWithUrls.size} usuarios")
                    }
                    .onFailure { error ->
                        android.util.Log.e("UsersSearchViewModel", "Error en búsqueda avanzada", error)
                    }
            } else {
                // Si no hay búsqueda, restaurar paginación
                _uiState.update { state ->
                    state.copy(hasMoreData = state.users.size >= 20)
                }
            }
        }
    }
    
    fun toggleOnlineOnly() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val newShowOnlineOnly = !currentState.showOnlineOnly
            val filteredList = filterUsers(currentState.users, currentState.searchQuery, newShowOnlineOnly)

            _uiState.update { state ->
                state.copy(
                    showOnlineOnly = newShowOnlineOnly,
                    filteredUsers = filteredList
                )
            }
        }
    }
    
    /**
     * Crear chat y navegar - Patrón correcto basado en proyecto base
     * 1. Busca chat existente
     * 2. Si no existe, lo crea
     * 3. Ejecuta callback con chatId válido para navegación
     */
    fun createChatAndNavigate(otherUserId: String, displayName: String, onChatCreated: (String, String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(creatingChatForUser = otherUserId, error = null) }

            try {
                val currentUserId = userIdManager.getCurrentUserId()
                android.util.Log.d("UsersSearchViewModel", "Creando chat entre $currentUserId y $otherUserId")

                // Generar ID determinístico para chat directo
                val chatId = generateDirectChatId(currentUserId, otherUserId)
                android.util.Log.d("UsersSearchViewModel", "ID de chat generado: $chatId")

                // Verificar si ya existe el chat
                val existingChatResult = chatRepository.getChatById(chatId)

                val finalChatId = if (existingChatResult.isSuccess) {
                    val existingChat = existingChatResult.getOrNull()
                    if (existingChat != null) {
                        android.util.Log.d("UsersSearchViewModel", "Chat existente encontrado: ${existingChat.id}")
                        existingChat.id
                    } else {
                        // No existe, crear nuevo chat
                        android.util.Log.d("UsersSearchViewModel", "Creando nuevo chat")
                        val createResult = chatRepository.createChat(listOf(currentUserId, otherUserId), false)
                        if (createResult.isSuccess) {
                            createResult.getOrNull()!!.id
                        } else {
                            throw Exception("Error creando chat: ${createResult.exceptionOrNull()?.message}")
                        }
                    }
                } else {
                    // Error o no existe, crear nuevo chat
                    android.util.Log.d("UsersSearchViewModel", "Chat no existe, creando nuevo")
                    val createResult = chatRepository.createChat(listOf(currentUserId, otherUserId), false)
                    if (createResult.isSuccess) {
                        createResult.getOrNull()!!.id
                    } else {
                        throw Exception("Error creando chat: ${createResult.exceptionOrNull()?.message}")
                    }
                }

                _uiState.update { it.copy(creatingChatForUser = null) }

                // Ejecutar callback con chatId válido y displayName
                onChatCreated(finalChatId, displayName)
                android.util.Log.d("UsersSearchViewModel", "Chat listo para navegación: $finalChatId")

            } catch (e: Exception) {
                android.util.Log.e("UsersSearchViewModel", "Error en createChatAndNavigate", e)
                _uiState.update {
                    it.copy(
                        creatingChatForUser = null,
                        error = "Error creando conversación: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Cargar más usuarios (paginación)
     */
    fun loadMoreUsers() {
        // No cargar más si ya estamos cargando o no hay más datos o hay búsqueda activa
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMoreData || _uiState.value.searchQuery.isNotBlank()) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val currentState = _uiState.value
            val currentUserId = SessionManager.getUserId(context)

            // Cargar más usuarios usando el último documento
            val moreUsersResult = firestoreRepository.listUsuarios(
                limit = 20,
                lastDocument = currentState.lastDocumentId
            )

            moreUsersResult.fold(
                onSuccess = { newUsers ->
                    val newUsersWithUrls = newUsers.map { user ->
                        user.copy(photoUrl = generateThumbnailUrl(user.userId))
                    }

                    // Combinar usuarios existentes con nuevos
                    val allUsers = currentState.users + newUsersWithUrls
                    val filteredList = filterUsers(allUsers, currentState.searchQuery, currentState.showOnlineOnly)

                    _uiState.update { state ->
                        state.copy(
                            isLoadingMore = false,
                            users = allUsers,
                            filteredUsers = filteredList,
                            lastDocumentId = newUsersWithUrls.lastOrNull()?.userId,
                            hasMoreData = newUsersWithUrls.size >= 20  // Hay más datos si obtuvimos el máximo
                        )
                    }

                    android.util.Log.d("UsersSearchViewModel", "Paginación exitosa: +${newUsersWithUrls.size} usuarios, total: ${allUsers.size}")
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            error = "Error cargando más usuarios: ${error.message}"
                        )
                    }
                    android.util.Log.e("UsersSearchViewModel", "Error en paginación", error)
                }
            )
        }
    }

    /**
     * Refrescar la lista de usuarios (público para UI)
     */
    fun refreshUsers() {
        loadUsers()
    }

    private suspend fun filterUsers(
        users: List<UserPreview>,
        query: String,
        onlineOnly: Boolean
    ): List<UserPreview> {
        var filtered = users

        // Filtrar por estado online si es necesario (usando campo isOnline del usuario)
        if (onlineOnly) {
            filtered = filtered.filter { it.isOnline && it.mostrarEstado }
        }

        // Filtrar por búsqueda (nickname, fullName, description)
        if (query.isNotBlank()) {
            filtered = filtered.filter { user ->
                user.nickname.contains(query, ignoreCase = true) ||
                user.fullName.contains(query, ignoreCase = true) ||
                user.description?.contains(query, ignoreCase = true) == true
            }
        }

        // Excluir el usuario actual de la lista usando UserIdManager
        val currentUserId = try {
            userIdManager.getCurrentUserId()
        } catch (e: Exception) {
            android.util.Log.e("UsersSearchViewModel", "Error obteniendo userId para filtrar", e)
            // Si falla, usar SessionManager como fallback
            SessionManager.getUserId(context) ?: "default_user"
        }

        filtered = filtered.filter { it.userId != currentUserId }

        // Ordenar: verificados primero, luego por popularidad (totalScore), luego online
        return filtered.sortedWith(
            compareByDescending<UserPreview> { it.isVerified }
                .thenByDescending { it.totalScore }
                .thenByDescending { it.isOnline && it.mostrarEstado }
        )
    }

    /**
     * Sigue o deja de seguir a un usuario
     */
    fun toggleFollow(userId: String) {
        viewModelScope.launch {
            val currentUserId = SessionManager.getUserId(context)
            android.util.Log.d("UsersSearchViewModel", "👤 [FOLLOW_DEBUG] toggleFollow llamado: currentUserId=$currentUserId, targetUserId=$userId")

            if (currentUserId == null) {
                android.util.Log.e("UsersSearchViewModel", "👤 [FOLLOW_DEBUG] ❌ Usuario no logueado, no se puede seguir")
                return@launch
            }

            val isFollowing = _uiState.value.followingUsers.contains(userId)
            android.util.Log.d("UsersSearchViewModel", "👤 [FOLLOW_DEBUG] Estado actual: isFollowing=$isFollowing")

            // Actualización optimista INMEDIATA - evita el delay
            _uiState.update { state ->
                val newFollowingUsers = if (isFollowing) {
                    state.followingUsers - userId
                } else {
                    state.followingUsers + userId
                }
                android.util.Log.d("UsersSearchViewModel", "👤 [FOLLOW_DEBUG] Actualización optimista: ${state.followingUsers.size} -> ${newFollowingUsers.size}")

                state.copy(
                    followingUsers = newFollowingUsers,
                    loadingFollow = state.loadingFollow + userId
                )
            }

            // Ejecutar la operación real
            android.util.Log.d("UsersSearchViewModel", "👤 [FOLLOW_DEBUG] Ejecutando operación: ${if (isFollowing) "unfollow" else "follow"}")
            val result = if (isFollowing) {
                firestoreRepository.unfollowUser(currentUserId, userId)
            } else {
                firestoreRepository.followUser(currentUserId, userId)
            }

            result.fold(
                onSuccess = {
                    android.util.Log.d("UsersSearchViewModel", "👤 [FOLLOW_DEBUG] ✅ Operación exitosa: ${if (isFollowing) "unfollow" else "follow"}")
                    // Quitar de loading
                    _uiState.update { state ->
                        state.copy(loadingFollow = state.loadingFollow - userId)
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("UsersSearchViewModel", "👤 [FOLLOW_DEBUG] ❌ Error en follow/unfollow, revirtiendo", error)
                    // Revertir el cambio si falló
                    _uiState.update { state ->
                        state.copy(
                            followingUsers = if (isFollowing) {
                                state.followingUsers + userId // Volver a agregar si estaba siguiendo
                            } else {
                                state.followingUsers - userId // Quitar si no estaba siguiendo
                            },
                            loadingFollow = state.loadingFollow - userId,
                            error = "Error al ${if (isFollowing) "dejar de seguir" else "seguir"} al usuario"
                        )
                    }
                }
            )
        }
    }

    /**
     * Refresca solo los estados de seguimiento (para casos especiales)
     */
    fun refreshFollowingStates() {
        viewModelScope.launch {
            val currentUserId = SessionManager.getUserId(context) ?: return@launch

            firestoreRepository.getFollowingIds(currentUserId).fold(
                onSuccess = { followingIds ->
                    _uiState.update {
                        it.copy(followingUsers = followingIds)
                    }
                    android.util.Log.d("UsersSearchViewModel", "✅ Estados de seguimiento actualizados: ${followingIds.size} usuarios")
                },
                onFailure = { error ->
                    android.util.Log.e("UsersSearchViewModel", "Error actualizando estados de seguimiento", error)
                }
            )
        }
    }

    /**
     * Genera un ID determinístico para chat directo entre el usuario actual y otro usuario
     * Formato: chat_userId1_userId2 (ordenado alfabéticamente)
     */
    fun generateDirectChatId(otherUserId: String): String {
        return try {
            // Usar SessionManager como fallback síncrono para obtener el userId
            val currentUserId = SessionManager.getUserId(context) ?: throw Exception("No user ID found")
            if (currentUserId < otherUserId) {
                "chat_${currentUserId}_${otherUserId}"
            } else {
                "chat_${otherUserId}_${currentUserId}"
            }
        } catch (e: Exception) {
            android.util.Log.e("UsersSearchViewModel", "Error generando chatId", e)
            "chat_temp_${System.currentTimeMillis()}"
        }
    }

    /**
     * Genera un ID determinístico para chat directo entre dos usuarios específicos
     * Formato: chat_userId1_userId2 (ordenado alfabéticamente)
     */
    private fun generateDirectChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "chat_${userId1}_${userId2}"
        } else {
            "chat_${userId2}_${userId1}"
        }
    }

}