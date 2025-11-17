package com.mision.biihlive.presentation.ranking.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mision.biihlive.data.repository.FirestoreRepository
import com.mision.biihlive.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel para el sistema de ranking
 * Migrado de AppSyncRepository a FirestoreRepository
 */
class RankingViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val sessionManager: SessionManager,
    private val context: Context,
    private val targetUserId: String? = null,
    private val initialTab: Int? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(RankingUiState())
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "RankingViewModel"

        /**
         * Mapea las preferencias de ranking a índices de tabs
         * @param preference La preferencia del usuario: "local", "provincial", "nacional", "mundial", "grupo"
         * @return El índice del tab correspondiente (0-4), por defecto 0 (Local)
         */
        fun mapPreferenceToTabIndex(preference: String?): Int {
            return when (preference?.lowercase()) {
                "local" -> 0
                "provincial" -> 1
                "nacional" -> 2
                "mundial" -> 3
                "grupo" -> 4
                else -> 0  // Por defecto Local
            }
        }
    }

    init {
        // Configurar tab inicial y cargar datos correspondientes
        val tabToLoad = initialTab ?: 0  // Por defecto Local (0) si no se especifica
        _uiState.update { it.copy(currentTab = tabToLoad) }

        // Cargar datos según el tab inicial
        when (tabToLoad) {
            0 -> loadLocalRanking()
            1 -> loadProvincialRanking()
            2 -> loadNacionalRanking()
            3 -> loadMundialRanking()
            4 -> loadGrupoRanking()
            else -> loadLocalRanking()  // Fallback a Local
        }
    }

    /**
     * Cambiar a un tab específico
     */
    fun switchTab(tabIndex: Int) {
        _uiState.update { it.copy(currentTab = tabIndex) }

        // Cargar datos según el tab seleccionado
        when (tabIndex) {
            0 -> loadLocalRanking()
            1 -> loadProvincialRanking()
            2 -> loadNacionalRanking()
            3 -> loadMundialRanking()
            4 -> loadGrupoRanking()
        }
    }

    /**
     * Limpiar mensaje de error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Cargar ranking local
     */
    private fun loadLocalRanking() {
        if (_uiState.value.loadingTabs.contains(0)) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(loadingTabs = it.loadingTabs + 0)
            }

            try {
                Log.d(TAG, "🏠 [RANKING_VM] Cargando ranking local...")

                // Usar targetUserId si está disponible, sino el usuario actual (lógica del APK)
                val userId = targetUserId ?: sessionManager.getUserId(context)
                if (userId == null) {
                    Log.e(TAG, "Usuario no disponible para ranking local")
                    _uiState.update {
                        it.copy(
                            error = "Usuario no disponible",
                            loadingTabs = it.loadingTabs - 0
                        )
                    }
                    return@launch
                }

                Log.d(TAG, "🏠 [RANKING_VM] Usando ubicación del usuario: $userId (target: $targetUserId)")
                val result = firestoreRepository.getRankingLocal(userId, limit = 50)

                result.fold(
                    onSuccess = { ranking ->
                        Log.d(TAG, "Ranking local cargado exitosamente: ${ranking.size} usuarios")

                        // Convertir de FirestoreRepository.RankingUser a presentation.RankingUser
                        val rankingUsers = ranking.map { firestoreUser ->
                            RankingUser(
                                userId = firestoreUser.userId,
                                nickname = firestoreUser.nickname,
                                fullName = firestoreUser.fullName,
                                totalScore = firestoreUser.totalScore,
                                ubicacion = firestoreUser.ubicacion,
                                ciudad = firestoreUser.ciudad,
                                provincia = firestoreUser.provincia,
                                pais = firestoreUser.pais,
                                nivel = firestoreUser.nivel,
                                isVerified = firestoreUser.isVerified,
                                profileImageUrl = firestoreUser.profileImageUrl,
                                thumbnailImageUrl = firestoreUser.thumbnailImageUrl,
                                countryCode = firestoreUser.countryCode,
                                subdivisionCode = firestoreUser.subdivisionCode,
                                postalCode = firestoreUser.postalCode
                            )
                        }

                        _uiState.update {
                            it.copy(
                                localRanking = rankingUsers,
                                loadingTabs = it.loadingTabs - 0
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error cargando ranking local", error)
                        _uiState.update {
                            it.copy(
                                error = "Error cargando ranking local: ${error.message}",
                                loadingTabs = it.loadingTabs - 0
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception cargando ranking local", e)
                _uiState.update {
                    it.copy(
                        error = "Error inesperado: ${e.message}",
                        loadingTabs = it.loadingTabs - 0
                    )
                }
            }
        }
    }

    /**
     * Cargar ranking provincial
     */
    private fun loadProvincialRanking() {
        if (_uiState.value.loadingTabs.contains(1)) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(loadingTabs = it.loadingTabs + 1)
            }

            try {
                Log.d(TAG, "🏛️ [RANKING_VM] Cargando ranking provincial...")

                // Usar targetUserId si está disponible, sino el usuario actual (lógica del APK)
                val userId = targetUserId ?: sessionManager.getUserId(context)
                if (userId == null) {
                    Log.e(TAG, "Usuario no disponible para ranking provincial")
                    _uiState.update {
                        it.copy(
                            error = "Usuario no disponible",
                            loadingTabs = it.loadingTabs - 1
                        )
                    }
                    return@launch
                }

                Log.d(TAG, "🏛️ [RANKING_VM] Usando ubicación del usuario: $userId (target: $targetUserId)")
                val result = firestoreRepository.getRankingProvincial(userId, limit = 50)

                result.fold(
                    onSuccess = { ranking ->
                        val rankingUsers = ranking.map { firestoreUser ->
                            RankingUser(
                                userId = firestoreUser.userId,
                                nickname = firestoreUser.nickname,
                                fullName = firestoreUser.fullName,
                                totalScore = firestoreUser.totalScore,
                                ubicacion = firestoreUser.ubicacion,
                                ciudad = firestoreUser.ciudad,
                                provincia = firestoreUser.provincia,
                                pais = firestoreUser.pais,
                                nivel = firestoreUser.nivel,
                                isVerified = firestoreUser.isVerified,
                                profileImageUrl = firestoreUser.profileImageUrl,
                                thumbnailImageUrl = firestoreUser.thumbnailImageUrl,
                                countryCode = firestoreUser.countryCode,
                                subdivisionCode = firestoreUser.subdivisionCode,
                                postalCode = firestoreUser.postalCode
                            )
                        }

                        _uiState.update {
                            it.copy(
                                provincialRanking = rankingUsers,
                                loadingTabs = it.loadingTabs - 1
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error cargando ranking provincial", error)
                        _uiState.update {
                            it.copy(
                                error = "Error cargando ranking provincial: ${error.message}",
                                loadingTabs = it.loadingTabs - 1
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Error inesperado: ${e.message}",
                        loadingTabs = it.loadingTabs - 1
                    )
                }
            }
        }
    }

    /**
     * Cargar ranking nacional
     */
    private fun loadNacionalRanking() {
        if (_uiState.value.loadingTabs.contains(2)) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(loadingTabs = it.loadingTabs + 2)
            }

            try {
                Log.d(TAG, "🇪🇸 [RANKING_VM] Cargando ranking nacional...")

                // Usar targetUserId si está disponible, sino el usuario actual (lógica del APK)
                val userId = targetUserId ?: sessionManager.getUserId(context)
                if (userId == null) {
                    Log.e(TAG, "Usuario no disponible para ranking nacional")
                    _uiState.update {
                        it.copy(
                            error = "Usuario no disponible",
                            loadingTabs = it.loadingTabs - 2
                        )
                    }
                    return@launch
                }

                Log.d(TAG, "🇪🇸 [RANKING_VM] Usando ubicación del usuario: $userId (target: $targetUserId)")
                val result = firestoreRepository.getRankingNacional(userId, limit = 50)

                result.fold(
                    onSuccess = { ranking ->
                        val rankingUsers = ranking.map { firestoreUser ->
                            RankingUser(
                                userId = firestoreUser.userId,
                                nickname = firestoreUser.nickname,
                                fullName = firestoreUser.fullName,
                                totalScore = firestoreUser.totalScore,
                                ubicacion = firestoreUser.ubicacion,
                                ciudad = firestoreUser.ciudad,
                                provincia = firestoreUser.provincia,
                                pais = firestoreUser.pais,
                                nivel = firestoreUser.nivel,
                                isVerified = firestoreUser.isVerified,
                                profileImageUrl = firestoreUser.profileImageUrl,
                                thumbnailImageUrl = firestoreUser.thumbnailImageUrl,
                                countryCode = firestoreUser.countryCode,
                                subdivisionCode = firestoreUser.subdivisionCode,
                                postalCode = firestoreUser.postalCode
                            )
                        }

                        _uiState.update {
                            it.copy(
                                nacionalRanking = rankingUsers,
                                loadingTabs = it.loadingTabs - 2
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error cargando ranking nacional", error)
                        _uiState.update {
                            it.copy(
                                error = "Error cargando ranking nacional: ${error.message}",
                                loadingTabs = it.loadingTabs - 2
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Error inesperado: ${e.message}",
                        loadingTabs = it.loadingTabs - 2
                    )
                }
            }
        }
    }

    /**
     * Cargar ranking mundial
     */
    private fun loadMundialRanking() {
        if (_uiState.value.loadingTabs.contains(3)) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(loadingTabs = it.loadingTabs + 3)
            }

            try {
                Log.d(TAG, "🌍 [RANKING_VM] Ranking mundial (sin filtro), targetUserId: $targetUserId")
                val result = firestoreRepository.getRankingMundial(limit = 50)

                result.fold(
                    onSuccess = { ranking ->
                        val rankingUsers = ranking.map { firestoreUser ->
                            RankingUser(
                                userId = firestoreUser.userId,
                                nickname = firestoreUser.nickname,
                                fullName = firestoreUser.fullName,
                                totalScore = firestoreUser.totalScore,
                                ubicacion = firestoreUser.ubicacion,
                                ciudad = firestoreUser.ciudad,
                                provincia = firestoreUser.provincia,
                                pais = firestoreUser.pais,
                                nivel = firestoreUser.nivel,
                                isVerified = firestoreUser.isVerified,
                                profileImageUrl = firestoreUser.profileImageUrl,
                                thumbnailImageUrl = firestoreUser.thumbnailImageUrl,
                                countryCode = firestoreUser.countryCode,
                                subdivisionCode = firestoreUser.subdivisionCode,
                                postalCode = firestoreUser.postalCode
                            )
                        }

                        _uiState.update {
                            it.copy(
                                mundialRanking = rankingUsers,
                                loadingTabs = it.loadingTabs - 3
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error cargando ranking mundial", error)
                        _uiState.update {
                            it.copy(
                                error = "Error cargando ranking mundial: ${error.message}",
                                loadingTabs = it.loadingTabs - 3
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Error inesperado: ${e.message}",
                        loadingTabs = it.loadingTabs - 3
                    )
                }
            }
        }
    }

    /**
     * Cargar ranking de grupos (no implementado aún)
     */
    private fun loadGrupoRanking() {
        // TODO: Implementar cuando se defina la estructura de grupos
        Log.d(TAG, "Ranking de grupos no implementado aún")
    }
}