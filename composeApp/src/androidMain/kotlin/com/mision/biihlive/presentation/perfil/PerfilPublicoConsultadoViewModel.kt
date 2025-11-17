package com.mision.biihlive.presentation.perfil

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mision.biihlive.domain.perfil.model.PerfilUsuario
import com.mision.biihlive.domain.perfil.usecase.ObtenerPerfilUseCase
import com.mision.biihlive.domain.users.model.UserPreview
import com.mision.biihlive.data.repository.FirestoreRepository
import com.mision.biihlive.utils.Calcular
import com.mision.biihlive.utils.LevelCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mision.biihlive.utils.SessionManager
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.CachePolicy

/**
 * ViewModel para perfiles de otros usuarios (no el usuario logueado)
 * Usado cuando se visita el perfil de otro usuario desde chat o búsqueda
 */
class PerfilPublicoConsultadoViewModel(
    application: Application,
    private val obtenerPerfilUseCase: ObtenerPerfilUseCase,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PerfilUiState())
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    // Estado de seguimiento del usuario actual
    private var currentUserId: String? = null

    private val calcular = Calcular()
    private val context = application.applicationContext
    private val imageLoader = ImageLoader.Builder(context)
        .crossfade(false)
        .build()

    companion object {
        private const val TAG = "PerfilPublicoConsultadoViewModel"
    }

    /**
     * Carga perfil usando Firestore (nueva implementación)
     * @param userId ID del usuario a consultar
     */
    private suspend fun cargarPerfilConFirestore(userId: String): Result<PerfilUsuario> {
        return try {
            Log.d(TAG, "🔄 Cargando perfil con Firestore para userId: $userId")
            val result = firestoreRepository.getPerfilUsuario(userId)

            result.fold(
                onSuccess = { perfil ->
                    if (perfil != null) {
                        Result.success(perfil)
                    } else {
                        Result.failure(Exception("Perfil no encontrado en Firestore"))
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en Firestore para userId: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * LEGACY: Carga perfil usando API Gateway (implementación actual)
     * @param userId ID del usuario a consultar (Cognito Sub)
     */
    private suspend fun cargarPerfilConApiGateway(userId: String): Result<PerfilUsuario> {
        return try {
            Log.d(TAG, "🔄 LEGACY: Cargando perfil con API Gateway para userId: $userId")
            obtenerPerfilUseCase(userId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en API Gateway para userId: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Carga el perfil de un usuario específico por su ID
     * @param userId ID del usuario a consultar (Cognito Sub)
     */
    fun cargarPerfilDeUsuario(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                Log.d(TAG, "🚀 Cargando perfil del usuario con Firestore: $userId")

                // Usar Firestore como backend principal
                var result = cargarPerfilConFirestore(userId)

                if (result.isFailure) {
                    Log.w(TAG, "⚠️ Firestore falló, usando fallback a API Gateway")
                    result = cargarPerfilConApiGateway(userId)
                } else {
                    Log.d(TAG, "✅ Firestore exitoso para userId: $userId")
                }

                result.fold(
                    onSuccess = { perfil ->
                        Log.d(TAG, "✅ Perfil cargado exitosamente:")
                        Log.d(TAG, "  - UserId: ${perfil.userId}")
                        Log.d(TAG, "  - Nickname: ${perfil.nickname}")
                        Log.d(TAG, "  - TotalScore: ${perfil.totalScore}")
                        Log.d(TAG, "  - Nivel: ${perfil.nivel}")
                        Log.d(TAG, "  - PhotoUrl: ${perfil.photoUrl}")
                        Log.d(TAG, "  - 🔍 VERIFICACIÓN: isVerified = ${perfil.isVerified} (${if (perfil.isVerified == true) "✅ VERIFICADO" else "❌ NO VERIFICADO"})")

                        val siguienteNivel = LevelCalculator.getThresholdForNextLevel(perfil.totalScore)
                        val progreso = LevelCalculator.calculateProgressToNextLevel(perfil.totalScore)

                        // Cargar estado de seguimiento
                        cargarEstadoSeguimiento(perfil.userId)

                        // Cargar estado de suscripción
                        cargarEstadoSuscripcion(perfil.userId)

                        // Cargar estado de patrocinio
                        cargarEstadoPatrocinio(perfil.userId)

                        // Cargar patrocinador actual (si está siendo patrocinado)
                        cargarPatrocinadorActual(perfil.userId)

                        // Obtener URLs de imágenes dinámicamente desde S3 y contadores de userStats
                        viewModelScope.launch(Dispatchers.IO) {
                            val profileImages = try {
                                com.mision.biihlive.data.aws.S3ClientProvider.getMostRecentProfileImage(perfil.userId)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error obteniendo imágenes de S3: ${e.message}")
                                null
                            }

                            // Obtener contadores actualizados de userStats
                            val statsResult = try {
                                firestoreRepository.getUserStats(perfil.userId)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error obteniendo userStats: ${e.message}")
                                Result.failure(e)
                            }

                            val (followersCount, followingCount) = if (statsResult.isSuccess) {
                                statsResult.getOrNull() ?: Pair(perfil.seguidores, perfil.siguiendo)
                            } else {
                                // Fallback a contadores legacy del perfil
                                Log.w(TAG, "📊 [STATS_DEBUG] Usando contadores legacy del perfil público")
                                Pair(perfil.seguidores, perfil.siguiendo)
                            }

                            Log.d(TAG, "📊 [STATS_DEBUG] Contadores finales para perfil público: $followersCount seguidores, $followingCount siguiendo")

                            // Obtener posición en ranking según preferencia del usuario consultado
                            val rankingResult = try {
                                firestoreRepository.getUserRankingPosition(perfil.userId)
                            } catch (e: Exception) {
                                Log.e(TAG, "🏆 [RANKING_POS] Error obteniendo posición de ranking: ${e.message}")
                                Result.failure(e)
                            }

                            val (rankingPosition, rankingScope) = if (rankingResult.isSuccess) {
                                rankingResult.getOrNull() ?: Pair("N/A", "N/A")
                            } else {
                                Log.w(TAG, "🏆 [RANKING_POS] Usando valores por defecto para ranking público")
                                Pair("N/A", "N/A")
                            }

                            Log.d(TAG, "🏆 [RANKING_POS] Posición final para perfil público: $rankingPosition en $rankingScope")

                            // Crear perfil actualizado con contadores de userStats
                            val perfilConStats = perfil.copy(
                                seguidores = followersCount,
                                siguiendo = followingCount
                            )

                            withContext(Dispatchers.Main) {
                                _uiState.update {
                                    it.copy(
                                        perfil = perfilConStats,
                                        isLoading = false,
                                        error = null,
                                        siguienteNivel = siguienteNivel,
                                        progreso = progreso,
                                        profileImageUrl = profileImages?.first,
                                        profileThumbnailUrl = profileImages?.second,
                                        rankingPosition = rankingPosition,
                                        rankingScope = rankingScope,
                                        isLoadingRanking = false
                                    )
                                }

                                // Precargar imagen del perfil consultado si hay URLs
                                if (profileImages != null) {
                                    preloadProfileImageWithUrls(perfilConStats, profileImages.first, profileImages.second)
                                }

                                // Cargar preview de seguidores para mostrar avatares superpuestos
                                loadPreviewFollowers(perfilConStats.userId)
                            }
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error cargando perfil del usuario: $userId", exception)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "No se pudo cargar el perfil del usuario"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception cargando perfil del usuario", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al cargar el perfil: ${e.message}"
                    )
                }
            }
        }
    }

    fun limpiarError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Carga las imágenes de la galería del usuario consultado
     */
    fun loadGalleryImages(loadMore: Boolean = false) {
        val currentState = _uiState.value
        val perfil = currentState.perfil ?: return

        // Si ya está cargando, no hacer nada
        if (currentState.isLoadingGallery) return

        // Si no hay más imágenes, no hacer nada
        if (loadMore && !currentState.hasMoreGalleryImages) return

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isLoadingGallery = true) }
            }

            try {
                val token = if (loadMore) currentState.galleryNextToken else null
                val resultOrFailure = com.mision.biihlive.data.aws.S3ClientProvider.listUserGalleryImages(
                    userId = perfil.userId,
                    continuationToken = token,
                    limit = 15
                )

                resultOrFailure.fold(
                    onSuccess = { result ->
                        withContext(Dispatchers.Main) {
                            val newImages = if (loadMore) {
                                currentState.galleryImages + result.images
                            } else {
                                result.images
                            }

                            _uiState.update {
                                it.copy(
                                    galleryImages = newImages,
                                    isLoadingGallery = false,
                                    galleryNextToken = result.nextContinuationToken,
                                    hasMoreGalleryImages = result.nextContinuationToken != null
                                )
                            }

                            Log.d(TAG, "Galería cargada: ${result.images.size} imágenes, hasMore: ${result.nextContinuationToken != null}")
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error cargando galería", exception)
                        withContext(Dispatchers.Main) {
                            _uiState.update { it.copy(isLoadingGallery = false) }
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando galería", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isLoadingGallery = false) }
                }
            }
        }
    }



    private fun preloadProfileImageWithUrls(perfil: PerfilUsuario, fullUrl: String, thumbnailUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Precargando imágenes con URLs dinámicas:")
                Log.d(TAG, "  - Full: $fullUrl")
                Log.d(TAG, "  - Thumbnail: $thumbnailUrl")

                // Precargar thumbnail (más importante, se muestra primero)
                val thumbnailRequest = ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .memoryCacheKey("profile_${perfil.userId}")
                    .diskCacheKey("profile_${perfil.userId}")
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build()

                imageLoader.enqueue(thumbnailRequest)
                Log.d(TAG, "Precargando thumbnail del perfil consultado: $thumbnailUrl")

                // Precargar imagen completa (para cuando se abra en fullscreen)
                val fullRequest = ImageRequest.Builder(context)
                    .data(fullUrl)
                    .memoryCacheKey("profile_full_${perfil.userId}")
                    .diskCacheKey("profile_full_${perfil.userId}")
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build()

                imageLoader.enqueue(fullRequest)
                Log.d(TAG, "Precargando imagen completa del perfil consultado: $fullUrl")
            } catch (e: Exception) {
                Log.e(TAG, "Error precargando imagen del perfil consultado", e)
            }
        }
    }

    /**
     * Carga el estado de seguimiento del usuario consultado
     */
    private fun cargarEstadoSeguimiento(targetUserId: String) {
        viewModelScope.launch {
            currentUserId = SessionManager.getUserId(context)
            currentUserId?.let { currentId ->
                try {
                    val result = firestoreRepository.isFollowing(currentId, targetUserId)
                    result.fold(
                        onSuccess = { isFollowing ->
                            _uiState.update { it.copy(isFollowing = isFollowing) }
                            Log.d(TAG, "Estado de seguimiento cargado: isFollowing=$isFollowing")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Error cargando estado de seguimiento", error)
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Exception cargando estado de seguimiento", e)
                }
            }
        }
    }

    /**
     * Sigue o deja de seguir al usuario
     */
    fun toggleFollow() {
        viewModelScope.launch {
            val perfil = _uiState.value.perfil ?: return@launch
            val currentId = currentUserId ?: SessionManager.getUserId(context) ?: return@launch
            val isFollowing = _uiState.value.isFollowing

            // Actualización optimista inmediata
            _uiState.update { state ->
                state.copy(
                    isFollowing = !isFollowing,
                    isLoadingFollow = true
                )
            }

            // Ejecutar la operación real
            val result = if (isFollowing) {
                firestoreRepository.unfollowUser(currentId, perfil.userId)
            } else {
                firestoreRepository.followUser(currentId, perfil.userId)
            }

            result.fold(
                onSuccess = {
                    Log.d(TAG, "${if (!isFollowing) "Siguiendo" else "Dejó de seguir"} a ${perfil.nickname}")
                    _uiState.update { it.copy(isLoadingFollow = false) }
                },
                onFailure = { error ->
                    Log.e(TAG, "Error en follow/unfollow, revirtiendo", error)
                    // Revertir el cambio si falló
                    _uiState.update { state ->
                        state.copy(
                            isFollowing = isFollowing, // Volver al estado original
                            isLoadingFollow = false,
                            error = "Error al ${if (isFollowing) "dejar de seguir" else "seguir"} al usuario"
                        )
                    }
                }
            )
        }
    }

    /**
     * Carga una preview de los primeros seguidores del usuario consultado
     * Para mostrar avatares superpuestos como Instagram
     */
    fun loadPreviewFollowers(userId: String, limit: Int = 4) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPreviewFollowers = true) }

            try {
                val result = firestoreRepository.getFollowersWithDetails(
                    userId = userId,
                    limit = limit
                )

                result.fold(
                    onSuccess = { (users, _) ->
                        // Cargar URLs dinámicas de S3 para cada usuario
                        viewModelScope.launch(Dispatchers.IO) {
                            val usersWithImages = users.map { user ->
                                try {
                                    val profileImages = com.mision.biihlive.data.aws.S3ClientProvider.getMostRecentProfileImage(user.userId)
                                    if (profileImages != null) {
                                        user.copy(photoUrl = profileImages.second) // thumbnail URL
                                    } else {
                                        user
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error obteniendo imagen S3 para ${user.nickname}: ${e.message}")
                                    user
                                }
                            }

                            withContext(Dispatchers.Main) {
                                _uiState.update {
                                    it.copy(
                                        previewFollowers = usersWithImages,
                                        isLoadingPreviewFollowers = false
                                    )
                                }
                                Log.d(TAG, "📊 Preview de seguidores cargado con URLs S3: ${usersWithImages.size} usuarios")
                                usersWithImages.forEachIndexed { index, user ->
                                    Log.d(TAG, "👤 [$index] Usuario: ${user.nickname} | PhotoUrl: '${user.photoUrl}' | UserId: ${user.userId}")
                                }
                            }
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error cargando preview de seguidores", error)
                        _uiState.update {
                            it.copy(
                                previewFollowers = emptyList(),
                                isLoadingPreviewFollowers = false
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception cargando preview de seguidores", e)
                _uiState.update {
                    it.copy(
                        previewFollowers = emptyList(),
                        isLoadingPreviewFollowers = false
                    )
                }
            }
        }
    }

    /**
     * Cargar estado de suscripción del usuario actual respecto al perfil consultado
     */
    private fun cargarEstadoSuscripcion(targetUserId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingSuscripcion = true) }

                val currentUserId = SessionManager.getUserId(getApplication())
                if (currentUserId != null && currentUserId != targetUserId) {
                    val result = firestoreRepository.estaSuscrito(currentUserId, targetUserId)
                    result.fold(
                        onSuccess = { isSuscrito ->
                            _uiState.update {
                                it.copy(
                                    isSuscrito = isSuscrito,
                                    isLoadingSuscripcion = false
                                )
                            }
                            Log.d(TAG, "💳 [SUSCR_UI] Estado suscripción cargado: $isSuscrito")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Error verificando suscripción", error)
                            _uiState.update {
                                it.copy(
                                    isSuscrito = false,
                                    isLoadingSuscripcion = false
                                )
                            }
                        }
                    )
                } else {
                    // Si es el mismo usuario o no hay usuario autenticado
                    _uiState.update {
                        it.copy(
                            isSuscrito = false,
                            isLoadingSuscripcion = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception verificando estado de suscripción", e)
                _uiState.update {
                    it.copy(
                        isSuscrito = false,
                        isLoadingSuscripcion = false
                    )
                }
            }
        }
    }

    /**
     * Cargar estado de patrocinio del usuario actual respecto al perfil consultado
     */
    private fun cargarEstadoPatrocinio(targetUserId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingPatrocinio = true) }

                val currentUserId = SessionManager.getUserId(getApplication())
                if (currentUserId != null && currentUserId != targetUserId) {
                    val result = firestoreRepository.estaPatrocinando(currentUserId, targetUserId)
                    result.fold(
                        onSuccess = { estaPatrocinando ->
                            _uiState.update {
                                it.copy(
                                    estaPatrocinando = estaPatrocinando,
                                    isLoadingPatrocinio = false
                                )
                            }
                            Log.d(TAG, "💰 [PATR_UI] Estado patrocinio cargado: $estaPatrocinando")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Error verificando patrocinio", error)
                            _uiState.update {
                                it.copy(
                                    estaPatrocinando = false,
                                    isLoadingPatrocinio = false
                                )
                            }
                        }
                    )
                } else {
                    // Si es el mismo usuario o no hay usuario autenticado
                    _uiState.update {
                        it.copy(
                            estaPatrocinando = false,
                            isLoadingPatrocinio = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception verificando estado de patrocinio", e)
                _uiState.update {
                    it.copy(
                        estaPatrocinando = false,
                        isLoadingPatrocinio = false
                    )
                }
            }
        }
    }

    /**
     * Cargar patrocinador actual (si el usuario está siendo patrocinado)
     */
    private fun cargarPatrocinadorActual(targetUserId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingPatrocinador = true) }

                val result = firestoreRepository.getPatrocinadorActual(targetUserId)
                result.fold(
                    onSuccess = { patrocinador ->
                        _uiState.update {
                            it.copy(
                                patrocinadorActual = patrocinador,
                                tienePatrocinador = patrocinador != null,
                                isLoadingPatrocinador = false
                            )
                        }
                        if (patrocinador != null) {
                            Log.d(TAG, "💰 [PATR_UI] Patrocinador cargado: ${patrocinador.nickname}")
                        } else {
                            Log.d(TAG, "💰 [PATR_UI] No tiene patrocinador activo")
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error cargando patrocinador actual", error)
                        _uiState.update {
                            it.copy(
                                patrocinadorActual = null,
                                tienePatrocinador = false,
                                isLoadingPatrocinador = false
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception cargando patrocinador actual", e)
                _uiState.update {
                    it.copy(
                        patrocinadorActual = null,
                        tienePatrocinador = false,
                        isLoadingPatrocinador = false
                    )
                }
            }
        }
    }

}