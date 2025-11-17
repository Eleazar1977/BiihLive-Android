package com.mision.biihlive.presentation.social.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mision.biihlive.data.repository.FirestoreRepository
import com.mision.biihlive.domain.users.model.UserPreview
import com.mision.biihlive.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

data class FollowersFollowingUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentTab: Int = 0, // 0 = Seguidores, 1 = Siguiendo, 2 = Grupos, 3 = Ranking
    val followers: List<UserPreview> = emptyList(),
    val following: List<UserPreview> = emptyList(),
    val filteredFollowers: List<UserPreview> = emptyList(),
    val filteredFollowing: List<UserPreview> = emptyList(),
    val searchQuery: String = "",
    val hasMoreFollowers: Boolean = true,
    val hasMoreFollowing: Boolean = true,
    val nextTokenFollowers: String? = null,
    val nextTokenFollowing: String? = null,
    val followingUsers: Set<String> = emptySet(), // IDs de usuarios que el usuario actual sigue
    val loadingFollow: Set<String> = emptySet() // IDs de usuarios con follow en proceso
)

class FollowersFollowingViewModel(
    private val userId: String, // ID del usuario cuyos seguidores/seguidos estamos viendo
    private val initialTab: Int = 0, // 0 = Seguidores, 1 = Siguiendo, 2 = Grupos, 3 = Ranking
    private val firestoreRepository: FirestoreRepository,
    private val sessionManager: SessionManager,
    private val context: android.content.Context
) : ViewModel() {

    companion object {
        private const val TAG = "FollowersFollowingVM"
        private const val PAGE_SIZE = 20
    }

    private val _uiState = MutableStateFlow(FollowersFollowingUiState(currentTab = initialTab))
    val uiState: StateFlow<FollowersFollowingUiState> = _uiState.asStateFlow()

    private val currentUserId = sessionManager.getUserId(context) ?: ""

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Cargar datos según el tab inicial
                if (initialTab == 0) {
                    loadFollowers(isInitial = true)
                } else {
                    loadFollowing(isInitial = true)
                }

                // Cargar los estados de seguimiento del usuario actual
                loadCurrentUserFollowingStates()

            } catch (e: Exception) {
                Log.e(TAG, "Error cargando datos iniciales", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar los datos"
                )
            }
        }
    }

    fun switchTab(tabIndex: Int) {
        if (_uiState.value.currentTab == tabIndex) return

        _uiState.value = _uiState.value.copy(currentTab = tabIndex)

        // Cargar datos del tab si no se han cargado
        viewModelScope.launch {
            if (tabIndex == 0 && _uiState.value.followers.isEmpty()) {
                loadFollowers(isInitial = true)
            } else if (tabIndex == 1 && _uiState.value.following.isEmpty()) {
                loadFollowing(isInitial = true)
            }
        }
    }

    fun searchUsers(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)

        if (_uiState.value.currentTab == 0) {
            // Filtrar seguidores
            val filtered = if (query.isEmpty()) {
                _uiState.value.followers
            } else {
                _uiState.value.followers.filter { user ->
                    user.nickname.contains(query, ignoreCase = true)
                }
            }
            _uiState.value = _uiState.value.copy(filteredFollowers = filtered)
        } else {
            // Filtrar siguiendo
            val filtered = if (query.isEmpty()) {
                _uiState.value.following
            } else {
                _uiState.value.following.filter { user ->
                    user.nickname.contains(query, ignoreCase = true)
                }
            }
            _uiState.value = _uiState.value.copy(filteredFollowing = filtered)
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore) return

        viewModelScope.launch {
            if (_uiState.value.currentTab == 0) {
                loadMoreFollowers()
            } else {
                loadMoreFollowing()
            }
        }
    }

    private suspend fun loadFollowers(isInitial: Boolean = false) {
        try {
            _uiState.value = _uiState.value.copy(
                isLoading = isInitial,
                isLoadingMore = !isInitial
            )

            // Llamar a Firestore para obtener seguidores
            val result = firestoreRepository.getFollowersWithDetails(
                userId = userId,
                limit = PAGE_SIZE
            )

            result.fold(
                onSuccess = { (users, nextToken) ->
                    _uiState.value = _uiState.value.copy(
                        followers = if (isInitial) users else _uiState.value.followers + users,
                        filteredFollowers = if (isInitial) users else _uiState.value.filteredFollowers + users,
                        hasMoreFollowers = nextToken != null,
                        nextTokenFollowers = nextToken,
                        isLoading = false,
                        isLoadingMore = false,
                        error = null
                    )
                },
                onFailure = { e ->
                    Log.e(TAG, "Error cargando seguidores", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = "Error al cargar seguidores"
                    )
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error cargando seguidores", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoadingMore = false,
                error = "Error al cargar seguidores"
            )
        }
    }

    private suspend fun loadFollowing(isInitial: Boolean = false) {
        try {
            _uiState.value = _uiState.value.copy(
                isLoading = isInitial,
                isLoadingMore = !isInitial
            )

            // Llamar a Firestore para obtener usuarios seguidos
            val result = firestoreRepository.getFollowingWithDetails(
                userId = userId,
                limit = PAGE_SIZE
            )

            result.fold(
                onSuccess = { (users, nextToken) ->
                    _uiState.value = _uiState.value.copy(
                        following = if (isInitial) users else _uiState.value.following + users,
                        filteredFollowing = if (isInitial) users else _uiState.value.filteredFollowing + users,
                        hasMoreFollowing = nextToken != null,
                        nextTokenFollowing = nextToken,
                        isLoading = false,
                        isLoadingMore = false,
                        error = null
                    )
                },
                onFailure = { e ->
                    Log.e(TAG, "Error cargando siguiendo", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = "Error al cargar usuarios seguidos"
                    )
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error cargando siguiendo", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoadingMore = false,
                error = "Error al cargar usuarios seguidos"
            )
        }
    }

    private suspend fun loadMoreFollowers() {
        if (!_uiState.value.hasMoreFollowers || _uiState.value.nextTokenFollowers == null) return
        loadFollowers(isInitial = false)
    }

    private suspend fun loadMoreFollowing() {
        if (!_uiState.value.hasMoreFollowing || _uiState.value.nextTokenFollowing == null) return
        loadFollowing(isInitial = false)
    }

    private suspend fun loadCurrentUserFollowingStates() {
        try {
            // Cargar los IDs de usuarios que el usuario actual sigue
            val result = firestoreRepository.getFollowingIds(currentUserId)
            result.fold(
                onSuccess = { followingIds ->
                    _uiState.value = _uiState.value.copy(
                        followingUsers = followingIds
                    )
                },
                onFailure = { e ->
                    Log.e(TAG, "Error cargando estados de seguimiento", e)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando estados de seguimiento", e)
        }
    }

    fun toggleFollow(targetUserId: String) {
        viewModelScope.launch {
            try {
                val isFollowing = _uiState.value.followingUsers.contains(targetUserId)

                // Actualización optimista
                _uiState.value = _uiState.value.copy(
                    followingUsers = if (isFollowing) {
                        _uiState.value.followingUsers - targetUserId
                    } else {
                        _uiState.value.followingUsers + targetUserId
                    },
                    loadingFollow = _uiState.value.loadingFollow + targetUserId
                )

                // Ejecutar operación real
                val result = if (isFollowing) {
                    firestoreRepository.unfollowUser(currentUserId, targetUserId)
                } else {
                    firestoreRepository.followUser(currentUserId, targetUserId)
                }

                result.fold(
                    onSuccess = {
                        Log.d(TAG, if (isFollowing) "Dejó de seguir a $targetUserId" else "Siguiendo a $targetUserId")

                        // Si estamos en la lista de "Siguiendo" y dejamos de seguir,
                        // debemos quitar al usuario de la lista
                        if (isFollowing && _uiState.value.currentTab == 1) {
                            _uiState.value = _uiState.value.copy(
                                following = _uiState.value.following.filter { it.userId != targetUserId },
                                filteredFollowing = _uiState.value.filteredFollowing.filter { it.userId != targetUserId }
                            )
                        }
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Error en toggle follow", e)
                        // Revertir cambio optimista
                        _uiState.value = _uiState.value.copy(
                            followingUsers = if (isFollowing) {
                                _uiState.value.followingUsers + targetUserId
                            } else {
                                _uiState.value.followingUsers - targetUserId
                            }
                        )
                    }
                )

                _uiState.value = _uiState.value.copy(
                    loadingFollow = _uiState.value.loadingFollow - targetUserId
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error en toggle follow", e)
                _uiState.value = _uiState.value.copy(
                    loadingFollow = _uiState.value.loadingFollow - targetUserId
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(searchQuery = "")

            if (_uiState.value.currentTab == 0) {
                loadFollowers(isInitial = true)
            } else {
                loadFollowing(isInitial = true)
            }

            loadCurrentUserFollowingStates()
        }
    }
}