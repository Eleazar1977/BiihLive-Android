package com.mision.biihlive.presentation.perfil

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mision.biihlive.core.managers.UserIdManager
import com.mision.biihlive.core.managers.UserNotAuthenticatedException
import com.mision.biihlive.data.repository.FirestoreRepository
import com.mision.biihlive.domain.perfil.model.PerfilUsuario
import com.mision.biihlive.domain.perfil.usecase.ActualizarPerfilUseCase
import com.mision.biihlive.domain.perfil.usecase.ObtenerPerfilUseCase
import com.mision.biihlive.utils.Calcular
import com.mision.biihlive.utils.SessionManager
import com.mision.biihlive.utils.LevelCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.mision.biihlive.data.repository.ProfileImageRepository

class PerfilPersonalLogueadoViewModel(
    application: Application,
    private val obtenerPerfilUseCase: ObtenerPerfilUseCase,
    private val actualizarPerfilUseCase: ActualizarPerfilUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PerfilUiState())
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    private val context = application.applicationContext
    private val calcular = Calcular()
    private val userIdManager = UserIdManager.getInstance(context)
    private val firestoreRepository = FirestoreRepository()
    private val profileImageRepository = ProfileImageRepository(context)
    private var profileSubscriptionJob: Job? = null
    private val imageLoader = ImageLoader.Builder(context)
        .crossfade(false)
        .build()

    // Persistir timestamp del último upload para mantener bypass activo
    private val sharedPrefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
    private var lastUploadTimestamp: Long = sharedPrefs.getLong("last_upload_timestamp", 0L)

    companion object {
        private const val TAG = "PerfilPersonalLogueadoViewModel"
        // CloudFront puede tardar hasta 5 minutos en propagar la invalidación
        private const val CACHE_BYPASS_DURATION_MS = 5 * 60 * 1000L // 5 minutos
    }
    
    init {
        cargarPerfil()
    }
    
    fun cargarPerfil(targetUserId: String? = null) {
        Log.d(TAG, "[CARGAR PERFIL] ========================================")
        Log.d(TAG, "  - Target User ID: ${targetUserId ?: "current user"}")
        Log.d(TAG, "  - Timestamp: ${System.currentTimeMillis()}")

        // Cancelar subscription anterior si existe
        profileSubscriptionJob?.cancel()

        profileSubscriptionJob = viewModelScope.launch {
            // Solo mostrar loading si no hay datos previos
            if (_uiState.value.perfil == null) {
                Log.d(TAG, "  - Estado: Cargando (sin datos previos)")
                _uiState.update { it.copy(isLoading = true, error = null) }
            } else {
                // Si hay datos previos, mostrarlos mientras se actualiza en segundo plano
                Log.d(TAG, "  - Estado: Refrescando (con datos previos)")
                _uiState.update { it.copy(isRefreshing = true, error = null) }
            }

            try {
                // Obtener el userId actual usando UserIdManager como ÚNICA fuente de verdad
                val userId = targetUserId ?: try {
                    userIdManager.getCurrentUserId()
                } catch (e: UserNotAuthenticatedException) {
                    Log.e(TAG, "Usuario no autenticado", e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = "No hay sesión activa. Por favor, inicia sesión."
                        )
                    }
                    return@launch
                }

                Log.d(TAG, "########################################")
                Log.d(TAG, "### INICIANDO SUBSCRIPTION DE PERFIL ###")
                Log.d(TAG, "########################################")
                Log.d(TAG, "targetUserId recibido: $targetUserId")
                Log.d(TAG, "userId obtenido de UserIdManager: $userId")

                // Obtener perfil desde Firestore
                Log.d(TAG, ">>> OBTENIENDO PERFIL con userId: $userId")

                try {
                    val result = firestoreRepository.getMyProfile(userId)
                    val perfil = result.getOrNull()

                    if (perfil != null) {
                        Log.d(TAG, "========================================")
                        Log.d(TAG, "🔔 PERFIL OBTENIDO DE FIRESTORE")
                        Log.d(TAG, "========================================")
                        Log.d(TAG, "  - Timestamp: ${System.currentTimeMillis()}")
                        Log.d(TAG, "  - UserId: ${perfil.userId}")
                        Log.d(TAG, "  - Nickname: ${perfil.nickname}")
                        Log.d(TAG, "  - TotalScore: ${perfil.totalScore}")
                        Log.d(TAG, "  - Description: ${perfil.description}")
                        Log.d(TAG, "  - Seguidores: ${perfil.seguidores}")
                        Log.d(TAG, "  - Siguiendo: ${perfil.siguiendo}")
                        Log.d(TAG, "  - Ciudad: ${perfil.ubicacion.ciudad}")
                        Log.d(TAG, "  - Provincia: ${perfil.ubicacion.provincia}")
                        Log.d(TAG, "  - País: ${perfil.ubicacion.pais}")
                        Log.d(TAG, "========================================")

                        val siguienteNivel = LevelCalculator.getThresholdForNextLevel(perfil.totalScore)
                        val progreso = LevelCalculator.calculateProgressToNextLevel(perfil.totalScore)

                        // Verificar si debemos mantener el bypass de caché activo
                        val timeSinceUpload = System.currentTimeMillis() - lastUploadTimestamp
                        val shouldBypassCache = timeSinceUpload < CACHE_BYPASS_DURATION_MS

                        if (shouldBypassCache) {
                            Log.d(TAG, "[CACHE BYPASS] Activo - Han pasado ${timeSinceUpload / 1000} segundos desde el último upload")
                        }

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
                                Log.w(TAG, "📊 [STATS_DEBUG] Usando contadores legacy del perfil")
                                Pair(perfil.seguidores, perfil.siguiendo)
                            }

                            Log.d(TAG, "📊 [STATS_DEBUG] Contadores finales para perfil: $followersCount seguidores, $followingCount siguiendo")

                            // Obtener posición en ranking según preferencia del usuario
                            val rankingResult = try {
                                firestoreRepository.getUserRankingPosition(perfil.userId)
                            } catch (e: Exception) {
                                Log.e(TAG, "🏆 [RANKING_POS] Error obteniendo posición de ranking: ${e.message}")
                                Result.failure(e)
                            }

                            val (rankingPosition, rankingScope) = if (rankingResult.isSuccess) {
                                rankingResult.getOrNull() ?: Pair("N/A", "N/A")
                            } else {
                                Log.w(TAG, "🏆 [RANKING_POS] Usando valores por defecto para ranking")
                                Pair("N/A", "N/A")
                            }

                            Log.d(TAG, "🏆 [RANKING_POS] Posición final para perfil: $rankingPosition en $rankingScope")

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
                                        isRefreshing = false,
                                        error = null,
                                        siguienteNivel = siguienteNivel,
                                        progreso = progreso,
                                        shouldBypassImageCache = shouldBypassCache,
                                        profileImageUrl = profileImages?.first,
                                        profileThumbnailUrl = profileImages?.second,
                                        rankingPosition = rankingPosition,
                                        rankingScope = rankingScope,
                                        isLoadingRanking = false
                                    )
                                }

                                // Precargar imagen del perfil en el caché si hay URLs
                                if (profileImages != null) {
                                    preloadProfileImageWithUrls(perfilConStats, profileImages.first, profileImages.second)
                                }
                            }
                        }

                        // Los datos sociales ya vienen incluidos en el perfil
                        // No necesitamos cargarlos por separado
                    } else {
                        Log.e(TAG, "Perfil es null en la respuesta")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = "No se encontró el perfil del usuario"
                            )
                        }
                        Log.e(TAG, "Perfil es null en la respuesta de Firestore")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = "No se encontró el perfil del usuario en Firestore"
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error obteniendo perfil de Firestore", e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = "Error al cargar el perfil desde Firestore: ${e.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception cargando perfil", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = "Error al cargar el perfil: ${e.message}"
                    )
                }
            }
        }
    }
    
    
    private fun crearPerfilBasico(userId: String) {
        // Obtener detalles del usuario si están disponibles
        viewModelScope.launch {
            try {
                val userDetails = userIdManager.getUserDetails()

                val perfilBasico = PerfilUsuario(
                    userId = userId,
                    nickname = userDetails.nickname ?: userDetails.email?.substringBefore("@") ?: "Usuario",
                    fullName = userDetails.nickname ?: "",
                    description = "",
                    totalScore = 0,
                    tipo = "persona",
                    ubicacion = com.mision.biihlive.domain.perfil.model.Ubicacion(
                        ciudad = "",
                        provincia = "",
                        pais = "",
                        countryCode = "",
                        formattedAddress = "",
                        lat = null,
                        lng = null,
                        placeId = "",
                        privacyLevel = "city"
                    ),
                    rankingPreference = "local",
                    createdAt = System.currentTimeMillis(),
                    photoUrl = userDetails.profilePicture,
                    email = userDetails.email,
                    nivel = 1
                )

                _uiState.update {
                    it.copy(
                        perfil = perfilBasico,
                        isLoading = false,
                        isRefreshing = false,
                        siguienteNivel = 200,
                        progreso = 0.0
                    )
                }

                // Intentar guardar el perfil básico en la base de datos
                actualizarPerfilUseCase.actualizarPerfil(perfilBasico)
            } catch (e: Exception) {
                Log.e(TAG, "Error creando perfil básico", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = "Error al crear perfil básico"
                    )
                }
            }
        }
    }
    
    fun actualizarNickname(nickname: String) {
        val perfil = _uiState.value.perfil ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, updateSuccess = false) }

            val result = firestoreRepository.updateProfile(
                userId = perfil.userId,
                nickname = nickname
            )
            
            result.fold(
                onSuccess = { perfilActualizado ->
                    _uiState.update {
                        it.copy(
                            perfil = perfilActualizado,
                            isUpdating = false,
                            updateSuccess = true
                        )
                    }
                    
                    // Actualizar SessionManager
                    SessionManager.updateUserName(context, nickname)
                },
                onFailure = { exception ->
                    Log.e(TAG, "Error actualizando nickname", exception)
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Error al actualizar el nombre"
                        )
                    }
                }
            )
        }
    }
    
    fun actualizarDescripcion(descripcion: String) {
        val perfil = _uiState.value.perfil ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, updateSuccess = false) }

            val result = firestoreRepository.updateProfile(
                userId = perfil.userId,
                description = descripcion
            )
            
            result.fold(
                onSuccess = { perfilActualizado ->
                    _uiState.update {
                        it.copy(
                            perfil = perfilActualizado,
                            isUpdating = false,
                            updateSuccess = true
                        )
                    }
                },
                onFailure = { exception ->
                    Log.e(TAG, "Error actualizando descripción", exception)
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Error al actualizar la descripción"
                        )
                    }
                }
            )
        }
    }
    
    fun actualizarFotoPerfil(photoUrl: String) {
        val perfil = _uiState.value.perfil ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, updateSuccess = false) }

            val result = firestoreRepository.updateProfile(
                userId = perfil.userId,
                photoUrl = photoUrl
            )

            result.fold(
                onSuccess = { perfilActualizado ->
                    _uiState.update {
                        it.copy(
                            perfil = perfilActualizado,
                            isUpdating = false,
                            updateSuccess = true
                        )
                    }
                },
                onFailure = { exception ->
                    Log.e(TAG, "Error actualizando foto", exception)
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Error al actualizar la foto"
                        )
                    }
                }
            )
        }
    }

    fun actualizarRankingPreference(rankingPreference: String) {
        val perfil = _uiState.value.perfil ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, updateSuccess = false) }

            val result = firestoreRepository.updateProfile(
                userId = perfil.userId,
                rankingPreference = rankingPreference
            )

            result.fold(
                onSuccess = { perfilActualizado ->
                    _uiState.update {
                        it.copy(
                            perfil = perfilActualizado,
                            isUpdating = false,
                            updateSuccess = true
                        )
                    }
                },
                onFailure = { exception ->
                    Log.e(TAG, "Error actualizando ranking preference", exception)
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Error al actualizar la preferencia de ranking"
                        )
                    }
                }
            )
        }
    }

    fun actualizarTipoCuenta(tipo: String) {
        val perfil = _uiState.value.perfil ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, updateSuccess = false) }

            val result = firestoreRepository.updateProfile(
                userId = perfil.userId,
                tipo = tipo
            )

            result.fold(
                onSuccess = { perfilActualizado ->
                    _uiState.update {
                        it.copy(
                            perfil = perfilActualizado,
                            isUpdating = false,
                            updateSuccess = true
                        )
                    }
                },
                onFailure = { exception ->
                    Log.e(TAG, "Error actualizando tipo de cuenta", exception)
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Error al actualizar el tipo de cuenta"
                        )
                    }
                }
            )
        }
    }

    fun actualizarUbicacion(pais: String, provincia: String, ciudad: String) {
        val perfil = _uiState.value.perfil ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, updateSuccess = false) }

            val result = firestoreRepository.updateProfile(
                userId = perfil.userId,
                pais = pais,
                provincia = provincia,
                ciudad = ciudad
            )

            result.fold(
                onSuccess = { perfilActualizado ->
                    _uiState.update {
                        it.copy(
                            perfil = perfilActualizado,
                            isUpdating = false,
                            updateSuccess = true
                        )
                    }
                },
                onFailure = { exception ->
                    Log.e(TAG, "Error actualizando ubicación", exception)
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Error al actualizar la ubicación"
                        )
                    }
                }
            )
        }
    }

    fun actualizarMostrarEstado(mostrarEstado: Boolean) {
        val perfil = _uiState.value.perfil ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, updateSuccess = false) }

            val result = firestoreRepository.updateProfile(
                userId = perfil.userId,
                mostrarEstado = mostrarEstado
            )

            result.fold(
                onSuccess = { perfilActualizado ->
                    _uiState.update {
                        it.copy(
                            perfil = perfilActualizado,
                            isUpdating = false,
                            updateSuccess = true
                        )
                    }
                },
                onFailure = { exception ->
                    Log.e(TAG, "Error actualizando mostrar estado", exception)
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Error al actualizar la privacidad del estado"
                        )
                    }
                }
            )
        }
    }

    fun limpiarError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun limpiarUpdateSuccess() {
        _uiState.update { it.copy(updateSuccess = false) }
    }

    // Función para forzar actualización del perfil y limpiar caché de imágenes
    fun forceRefreshProfile() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Forzando actualización de perfil y limpiando caché de imágenes...")

                // Limpiar caché de Coil
                clearImageCache()

                // Forzar recarga del perfil
                _uiState.update { it.copy(isRefreshing = true) }

                // Cancelar subscription actual y recargar
                profileSubscriptionJob?.cancel()
                cargarPerfil()

                Log.d(TAG, "Actualización forzada completada")
            } catch (e: Exception) {
                Log.e(TAG, "Error al forzar actualización", e)
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = "Error al actualizar el perfil"
                    )
                }
            }
        }
    }

    // Función para limpiar el caché de imágenes de Coil
    private suspend fun clearImageCache() {
        withContext(Dispatchers.IO) {
            try {
                val userId = userIdManager.getCurrentUserId()
                Log.d(TAG, "[CLEAR CACHE] ========================================")
                Log.d(TAG, "  - User ID: $userId")

                // Obtener el ImageLoader de Coil
                val imageLoader = ImageLoader.Builder(context).build()

                // Limpiar solo las imágenes del perfil actual
                val key1 = MemoryCache.Key("profile_$userId")
                val key2 = MemoryCache.Key("profile_full_$userId")
                val removed1 = imageLoader.memoryCache?.remove(key1)
                val removed2 = imageLoader.memoryCache?.remove(key2)
                Log.d(TAG, "  - Memory 'profile_$userId': ${if (removed1 != null) "CLEARED" else "NOT FOUND"}")
                Log.d(TAG, "  - Memory 'profile_full_$userId': ${if (removed2 != null) "CLEARED" else "NOT FOUND"}")

                // Para el disco, limpiamos todo
                imageLoader.diskCache?.clear()
                Log.d(TAG, "  - Disk cache: CLEARED ALL")


            } catch (e: Exception) {
                Log.e(TAG, "Error limpiando caché de imágenes", e)
            }
        }
    }

    private fun preloadProfileImageWithUrls(perfil: PerfilUsuario, fullUrl: String, thumbnailUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "[PRELOAD IMAGE WITH URLS] ========================================")
                Log.d(TAG, "  - User ID: ${perfil.userId}")
                Log.d(TAG, "  - Thumbnail URL: $thumbnailUrl")
                Log.d(TAG, "  - Full URL: $fullUrl")

                // Precargar thumbnail (más importante, se muestra primero)
                val thumbnailRequest = ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .memoryCacheKey("profile_${perfil.userId}")
                    .diskCacheKey("profile_${perfil.userId}")
                    .listener(
                        onSuccess = { _, _ ->
                            Log.d(TAG, "[PRELOAD] ✅ Thumbnail cargado exitosamente")
                        },
                        onError = { _, result ->
                            Log.e(TAG, "[PRELOAD] ❌ Error cargando thumbnail: ${result.throwable.message}")
                        }
                    )
                    .build()
                imageLoader.enqueue(thumbnailRequest)

                // Precargar imagen completa
                val fullRequest = ImageRequest.Builder(context)
                    .data(fullUrl)
                    .memoryCacheKey("profile_full_${perfil.userId}")
                    .diskCacheKey("profile_full_${perfil.userId}")
                    .listener(
                        onSuccess = { _, _ ->
                            Log.d(TAG, "[PRELOAD] ✅ Imagen completa cargada exitosamente")
                        }
                    )
                    .build()
                imageLoader.enqueue(fullRequest)

                Log.d(TAG, "========================================")
            } catch (e: Exception) {
                Log.e(TAG, "[PRELOAD] Error precargando imágenes con URLs", e)
            }
        }
    }


    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun uploadProfileImage(uri: android.net.Uri) {
        Log.d(TAG, "[UPLOAD IMAGE] =======================================")
        Log.d(TAG, "  - URI: $uri")
        Log.d(TAG, "  - Timestamp: ${System.currentTimeMillis()}")

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(
                    isUploadingImage = true,
                    uploadSuccess = false,
                    error = null
                ) }

                val userId = try {
                    userIdManager.getCurrentUserId()
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isUploadingImage = false,
                            error = "Usuario no autenticado"
                        )
                    }
                    return@launch
                }

                Log.d(TAG, "  - User ID obtenido: $userId")
                Log.d(TAG, "  - Iniciando upload a S3...")

                // Crear repositorio de imagen
                val profileImageRepository = com.mision.biihlive.data.repository.ProfileImageRepository(
                    context = context
                )

                // Actualizar progreso mientras sube
                // Procesando imagen...

                // Subir imagen
                profileImageRepository.uploadProfileImage(uri, userId)
                    .collect { result ->
                        result.fold(
                            onSuccess = { imageData ->
                                Log.d(TAG, "[UPLOAD SUCCESS] =======================================")
                                Log.d(TAG, "  - Full URL: ${imageData.fullImageUrl}")
                                Log.d(TAG, "  - Thumbnail URL: ${imageData.thumbnailUrl}")

                                // SOLUCIÓN CORRECTA: Usar el imageLoader del ViewModel y limpiar caché específico
                                Log.d(TAG, "[CACHE CLEAR] Limpiando caché de Coil...")
                                val memKey1 = MemoryCache.Key("profile_$userId")
                                val memKey2 = MemoryCache.Key("profile_full_$userId")

                                val removed1 = imageLoader.memoryCache?.remove(memKey1)
                                val removed2 = imageLoader.memoryCache?.remove(memKey2)
                                Log.d(TAG, "  - Memory cache 'profile_$userId': ${if (removed1 != null) "REMOVED" else "NOT FOUND"}")
                                Log.d(TAG, "  - Memory cache 'profile_full_$userId': ${if (removed2 != null) "REMOVED" else "NOT FOUND"}")

                                // La limpieza de disco es asíncrona, pero la de memoria es más crítica para la UI inmediata
                                viewModelScope.launch(Dispatchers.IO) {
                                    Log.d(TAG, "[DISK CACHE] Limpiando caché de disco...")
                                    imageLoader.diskCache?.remove("profile_$userId")
                                    imageLoader.diskCache?.remove("profile_full_$userId")
                                    Log.d(TAG, "  - Disk cache limpiado para user: $userId")
                                }

                                Log.d(TAG, "[CACHE CLEAR] ✅ Caché de Coil invalidado")

                                // Marcar y persistir timestamp de upload para bypass de caché
                                lastUploadTimestamp = System.currentTimeMillis()
                                sharedPrefs.edit().putLong("last_upload_timestamp", lastUploadTimestamp).apply()
                                Log.d(TAG, "[TIMESTAMP] Upload timestamp registrado y persistido: $lastUploadTimestamp")

                                _uiState.update { currentState ->
                                    currentState.copy(
                                        isUploadingImage = false,
                                        uploadSuccess = true,
                                        shouldBypassImageCache = true  // Activar bypass para URLs con timestamp
                                    )
                                }

                                // Recargar perfil inmediatamente para obtener la URL actualizada
                                Log.d(TAG, "[RELOAD PROFILE] ========================================")
                                Log.d(TAG, "  - Forzando recarga del perfil después de upload")
                                Log.d(TAG, "  - Timestamp: ${System.currentTimeMillis()}")
                                cargarPerfil()

                                // Limpiar el estado de éxito después de 2 segundos
                                kotlinx.coroutines.delay(2000)
                                _uiState.update { it.copy(uploadSuccess = false) }

                                // Mantener bypass de caché activo por 5 minutos
                                // No desactivamos el bypass aquí, se maneja por timestamp
                                Log.d(TAG, "[BYPASS CACHE] Bypass de caché activo por ${CACHE_BYPASS_DURATION_MS / 1000} segundos")
                            },
                            onFailure = { exception ->
                                Log.e(TAG, "[UPLOAD FAILED] ========================================")
                                Log.e(TAG, "  - Error: ${exception.message}")
                                Log.e(TAG, "  - Type: ${exception.javaClass.simpleName}")
                                Log.e(TAG, "  - Stack: ", exception)
                                _uiState.update {
                                    it.copy(
                                        isUploadingImage = false,
                                        error = "Error al subir imagen: ${exception.message}"
                                    )
                                }
                            }
                        )
                    }
            } catch (e: Exception) {
                Log.e(TAG, "[UPLOAD EXCEPTION] ========================================")
                Log.e(TAG, "  - Error: ${e.message}")
                Log.e(TAG, "  - Type: ${e.javaClass.simpleName}")
                Log.e(TAG, "  - Stack: ", e)
                _uiState.update {
                    it.copy(
                        isUploadingImage = false,
                        error = "Error inesperado: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Sube una imagen a la galería del usuario (NO al perfil)
     * Solo sube al bucket S3, no guarda en base de datos
     */
    fun uploadGalleryImage(uri: android.net.Uri) {
        Log.d(TAG, "[UPLOAD GALLERY] =======================================")
        Log.d(TAG, "  - URI: $uri")
        Log.d(TAG, "  - Timestamp: ${System.currentTimeMillis()}")

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(
                    isUploadingImage = true,
                    uploadSuccess = false,
                    error = null
                ) }

                val userId = try {
                    userIdManager.getCurrentUserId()
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isUploadingImage = false,
                            error = "Usuario no autenticado"
                        )
                    }
                    return@launch
                }

                Log.d(TAG, "  - User ID obtenido: $userId")
                Log.d(TAG, "  - Procesando imagen para galería...")

                // Procesar imagen con los tamaños de galería
                val imageProcessor = com.mision.biihlive.utils.ImageProcessor(context)
                val processResult = imageProcessor.processImageForGallery(uri)

                if (processResult.isFailure) {
                    Log.e(TAG, "[GALLERY ERROR] Error procesando imagen", processResult.exceptionOrNull())
                    _uiState.update {
                        it.copy(
                            isUploadingImage = false,
                            error = "Error procesando la imagen"
                        )
                    }
                    return@launch
                }

                val processedImages = processResult.getOrThrow()
                Log.d(TAG, "  - Imagen procesada exitosamente")
                Log.d(TAG, "  - Full size: ${processedImages.fullImageBytes.size} bytes")
                Log.d(TAG, "  - Thumbnail size: ${processedImages.thumbnailBytes.size} bytes")

                // Verificar S3ClientProvider está inicializado
                if (!com.mision.biihlive.data.aws.S3ClientProvider.isInitialized()) {
                    Log.d(TAG, "  - Inicializando S3ClientProvider...")
                    com.mision.biihlive.data.aws.S3ClientProvider.initialize(context)
                }

                // Crear metadata para la imagen
                val metadata = mapOf(
                    "uploadedBy" to userId,
                    "deviceType" to "android",
                    "appVersion" to "1.0.0"
                )

                // Subir a S3 con estructura de galería
                Log.d(TAG, "  - Subiendo a S3 en /gallery/$userId/...")
                val uploadResult = com.mision.biihlive.data.aws.S3ClientProvider.uploadGalleryImages(
                    userId = userId,
                    fullImageData = processedImages.fullImageBytes,
                    thumbnailData = processedImages.thumbnailBytes,
                    metadata = metadata
                )

                uploadResult.fold(
                    onSuccess = { (imageId, fullUrl, thumbnailUrl) ->
                        Log.d(TAG, "[GALLERY SUCCESS] =======================================")
                        Log.d(TAG, "  - Image ID: $imageId")
                        Log.d(TAG, "  - Full URL: $fullUrl")
                        Log.d(TAG, "  - Thumbnail URL: $thumbnailUrl")
                        Log.d(TAG, "  ✅ Imagen subida a galería exitosamente")
                        Log.d(TAG, "=========================================================")

                        _uiState.update {
                            it.copy(
                                isUploadingImage = false,
                                uploadSuccess = true,
                                error = null
                            )
                        }

                        // Recargar las imágenes de la galería para mostrar la nueva imagen
                        Log.d(TAG, "[GALLERY REFRESH] Recargando galería después de upload exitoso...")
                        // Delay para dar tiempo de propagación en S3
                        kotlinx.coroutines.delay(1500) // 1.5 segundos para asegurar propagación
                        loadGalleryImages(loadMore = false)

                        // TODO: En el futuro, guardar en base de datos
                        // Por ahora solo subimos al bucket
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "[GALLERY ERROR] Error subiendo a S3", exception)
                        _uiState.update {
                            it.copy(
                                isUploadingImage = false,
                                error = "Error subiendo la imagen: ${exception.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "[GALLERY ERROR] Error general", e)
                _uiState.update {
                    it.copy(
                        isUploadingImage = false,
                        error = "Error inesperado: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Carga las imágenes de galería del usuario actual
     * @param loadMore Si es true, carga la siguiente página usando el token de continuación
     */
    fun loadGalleryImages(loadMore: Boolean = false) {
        val userId = uiState.value.perfil?.userId ?: return

        Log.d(TAG, "[GALLERY LOAD] =======================================")
        Log.d(TAG, "  - User ID: $userId")
        Log.d(TAG, "  - Load more: $loadMore")

        // Si ya estamos cargando, no hacer nada
        if (_uiState.value.isLoadingGallery) {
            Log.d(TAG, "  - Ya está cargando galería, ignorando")
            return
        }

        // Si es loadMore pero no hay más imágenes, no hacer nada
        if (loadMore && !_uiState.value.hasMoreGalleryImages) {
            Log.d(TAG, "  - No hay más imágenes para cargar")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingGallery = true) }

                val continuationToken = if (loadMore) _uiState.value.galleryNextToken else null

                // Llamar a S3ClientProvider para obtener las imágenes
                com.mision.biihlive.data.aws.S3ClientProvider.listUserGalleryImages(
                    userId = userId,
                    limit = 15,
                    continuationToken = continuationToken
                ).fold(
                    onSuccess = { result ->
                        Log.d(TAG, "[GALLERY SUCCESS] =====================================")
                        Log.d(TAG, "  - Imágenes cargadas: ${result.images.size}")
                        Log.d(TAG, "  - Tiene más: ${result.nextContinuationToken != null}")

                        _uiState.update { currentState ->
                            currentState.copy(
                                galleryImages = if (loadMore) {
                                    currentState.galleryImages + result.images
                                } else {
                                    result.images
                                },
                                galleryNextToken = result.nextContinuationToken,
                                hasMoreGalleryImages = result.nextContinuationToken != null,
                                isLoadingGallery = false
                            )
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "[GALLERY ERROR] Error cargando galería", exception)
                        _uiState.update {
                            it.copy(
                                isLoadingGallery = false,
                                error = "Error cargando galería: ${exception.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "[GALLERY EXCEPTION] Error inesperado", e)
                _uiState.update {
                    it.copy(
                        isLoadingGallery = false,
                        error = "Error inesperado: ${e.message}"
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Cancelar la subscription cuando el ViewModel se destruye
        profileSubscriptionJob?.cancel()
        Log.d(TAG, "ViewModel destruido, subscription cancelada")
    }

    /*
    private fun cargarDatosSociales(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSocial = true) }

            try {
                // Obtener contadores actualizados de userStats
                val statsResult = try {
                    firestoreRepository.getUserStats(userId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error obteniendo userStats en cargarDatosSociales: ${e.message}")
                    Result.failure(e)
                }

                val (followersCount, followingCount) = if (statsResult.isSuccess) {
                    statsResult.getOrNull() ?: run {
                        // Fallback a contadores del perfil actual
                        val currentPerfil = _uiState.value.perfil
                        Pair(currentPerfil?.seguidores ?: 0, currentPerfil?.siguiendo ?: 0)
                    }
                } else {
                    // Fallback a contadores del perfil actual
                    val currentPerfil = _uiState.value.perfil
                    Log.w(TAG, "📊 [STATS_DEBUG] Usando contadores legacy en cargarDatosSociales")
                    Pair(currentPerfil?.seguidores ?: 0, currentPerfil?.siguiendo ?: 0)
                }

                Log.d(TAG, "📊 [STATS_DEBUG] Contadores sociales actualizados - Seguidores: $followersCount, Seguidos: $followingCount")

                _uiState.update {
                    it.copy(
                        followersCount = followersCount,
                        followingCount = followingCount,
                        isLoadingSocial = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando datos sociales", e)
                _uiState.update { it.copy(isLoadingSocial = false) }
            }
        }
    }
    */
}