package com.mision.biihlive.presentation.suscripciones.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mision.biihlive.data.repository.FirestoreRepository
import com.mision.biihlive.domain.users.model.UserPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SuscripcionesUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentTab: Int = 0, // 0 = Suscripciones, 1 = Suscriptores
    val suscripciones: List<UserPreview> = emptyList(),
    val suscriptores: List<UserPreview> = emptyList(),
    val searchQuery: String = ""
)

class SuscripcionesViewModel(
    private val userId: String,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SuscripcionesVM"
    }

    private val _uiState = MutableStateFlow(SuscripcionesUiState())
    val uiState: StateFlow<SuscripcionesUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                Log.d(TAG, "💳 [SUSCR_DEBUG] Cargando datos para userId: $userId")

                // Cargar datos reales de suscripciones
                loadSuscripciones()
                loadSuscriptores()

            } catch (e: Exception) {
                Log.e(TAG, "Error cargando datos iniciales", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar los datos"
                )
            }
        }
    }

    private suspend fun loadSuscripciones() {
        try {
            Log.d(TAG, "💳 [SUSCR_DEBUG] Obteniendo suscripciones para userId: $userId")

            // Usar función específica para suscripciones
            val result = firestoreRepository.getSuscripcionesWithDetails(userId)

            result.fold(
                onSuccess = { suscripciones ->
                    Log.d(TAG, "💳 [SUSCR_DEBUG] ✅ Suscripciones obtenidas: ${suscripciones.size}")

                    _uiState.value = _uiState.value.copy(
                        suscripciones = suscripciones,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { e ->
                    Log.e(TAG, "Error cargando suscripciones", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error al cargar suscripciones"
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando suscripciones", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Error al cargar suscripciones"
            )
        }
    }

    private suspend fun loadSuscriptores() {
        try {
            Log.d(TAG, "💳 [SUSCR_DEBUG] Obteniendo suscriptores para userId: $userId")

            // Usar función específica para suscriptores
            val result = firestoreRepository.getSuscriptoresWithDetails(userId)

            result.fold(
                onSuccess = { suscriptores ->
                    Log.d(TAG, "💳 [SUSCR_DEBUG] ✅ Suscriptores obtenidos: ${suscriptores.size}")

                    _uiState.value = _uiState.value.copy(
                        suscriptores = suscriptores,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { e ->
                    Log.e(TAG, "Error cargando suscriptores", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error al cargar suscriptores"
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando suscriptores", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Error al cargar suscriptores"
            )
        }
    }

    fun switchTab(tabIndex: Int) {
        if (_uiState.value.currentTab == tabIndex) return
        _uiState.value = _uiState.value.copy(currentTab = tabIndex)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(searchQuery = "", isLoading = true)
            loadSuscripciones()
            loadSuscriptores()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * FUNCIÓN DE TESTING - Crear datos de prueba para suscripciones
     * Crear algunas suscripciones de prueba y recargar datos
     */
    fun createTestData() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🧪 [SUSCR_TEST] Creando datos de prueba...")

                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // IDs de usuarios existentes conocidos del screenshot
                val joseAngel = "1UM5l7iQ9DeLmzDgW7Xtb98gHOi1" // Jose Angel
                val usuario2 = "O8HjD4kGwuNS76ldNKZYDW5KcZ82"  // Usuario 2
                val usuario3 = "SBz2ZvZU9iWauY7czWKM8SZ0YvJ2"  // Usuario 3

                // Test 1: Jose Angel se suscribe a Usuario2
                val resultado1 = firestoreRepository.suscribirUsuario(joseAngel, usuario2)

                // Test 2: Usuario3 se suscribe a Jose Angel
                val resultado2 = firestoreRepository.suscribirUsuario(usuario3, joseAngel)

                val exitos = mutableListOf<String>()
                val errores = mutableListOf<String>()

                resultado1.fold(
                    onSuccess = {
                        exitos.add("✅ Jose Angel → Usuario2")
                        Log.d(TAG, "🧪 [SUSCR_TEST] ✅ Suscripción 1 creada")
                    },
                    onFailure = {
                        errores.add("❌ Jose Angel → Usuario2: ${it.message}")
                        Log.e(TAG, "🧪 [SUSCR_TEST] ❌ Error suscripción 1", it)
                    }
                )

                resultado2.fold(
                    onSuccess = {
                        exitos.add("✅ Usuario3 → Jose Angel")
                        Log.d(TAG, "🧪 [SUSCR_TEST] ✅ Suscripción 2 creada")
                    },
                    onFailure = {
                        errores.add("❌ Usuario3 → Jose Angel: ${it.message}")
                        Log.e(TAG, "🧪 [SUSCR_TEST] ❌ Error suscripción 2", it)
                    }
                )

                if (errores.isEmpty()) {
                    Log.d(TAG, "🧪 [SUSCR_TEST] ✅ Todos los datos creados exitosamente")

                    // Recargar datos para ver los nuevos cambios
                    loadSuscripciones()
                    loadSuscriptores()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Errores: ${errores.joinToString(", ")}"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "🧪 [SUSCR_TEST] ❌ Error inesperado", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error inesperado: ${e.message}"
                )
            }
        }
    }

}