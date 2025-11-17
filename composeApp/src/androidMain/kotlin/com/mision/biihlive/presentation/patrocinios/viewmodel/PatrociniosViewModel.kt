package com.mision.biihlive.presentation.patrocinios.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mision.biihlive.data.repository.FirestoreRepository
import com.mision.biihlive.domain.users.model.UserPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PatrociniosUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentTab: Int = 0, // 0 = Patrocinios (me patrocinan), 1 = Patrocinando (yo patrocino)
    val patrocinios: List<UserPreview> = emptyList(),    // Usuarios que YO patrocino (tab "Patrocinando")
    val patrocinadores: List<UserPreview> = emptyList(), // Usuarios que ME patrocinan (tab "Patrocinios")
    val searchQuery: String = ""
)

class PatrociniosViewModel(
    private val userId: String,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    companion object {
        private const val TAG = "PatrociniosVM"
    }

    private val _uiState = MutableStateFlow(PatrociniosUiState())
    val uiState: StateFlow<PatrociniosUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                Log.d(TAG, "💰 [PATROCINIO_DEBUG] Cargando datos para userId: $userId")

                // Cargar datos reales de patrocinios
                loadPatrocinios()    // Usuarios que YO patrocino (tab "Patrocinando")
                loadPatrocinadores() // Usuarios que ME patrocinan (tab "Patrocinios")

            } catch (e: Exception) {
                Log.e(TAG, "Error cargando datos iniciales", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar los datos"
                )
            }
        }
    }

    private suspend fun loadPatrocinios() {
        try {
            Log.d(TAG, "💰 [PATROCINIO_DEBUG] Obteniendo usuarios que patrocino para userId: $userId")

            // Usar función específica para patrocinios (usuarios que YO patrocino)
            val result = firestoreRepository.getPatrociniosWithDetails(userId)

            result.fold(
                onSuccess = { patrocinios ->
                    Log.d(TAG, "💰 [PATROCINIO_DEBUG] ✅ Patrocinios obtenidos: ${patrocinios.size}")

                    _uiState.value = _uiState.value.copy(
                        patrocinios = patrocinios,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { e ->
                    Log.e(TAG, "Error cargando patrocinios", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error al cargar patrocinios"
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando patrocinios", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Error al cargar patrocinios"
            )
        }
    }

    private suspend fun loadPatrocinadores() {
        try {
            Log.d(TAG, "💰 [PATROCINIO_DEBUG] Obteniendo usuarios que me patrocinan para userId: $userId")

            // Usar función específica para patrocinadores (usuarios que ME patrocinan)
            val result = firestoreRepository.getPatrocinadoresWithDetails(userId)

            result.fold(
                onSuccess = { patrocinadores ->
                    Log.d(TAG, "💰 [PATROCINIO_DEBUG] ✅ Patrocinadores obtenidos: ${patrocinadores.size}")

                    _uiState.value = _uiState.value.copy(
                        patrocinadores = patrocinadores,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { e ->
                    Log.e(TAG, "Error cargando patrocinadores", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error al cargar patrocinadores"
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando patrocinadores", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Error al cargar patrocinadores"
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
            loadPatrocinios()    // Usuarios que YO patrocino
            loadPatrocinadores() // Usuarios que ME patrocinan
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * FUNCIÓN DE TESTING - Crear datos de prueba para patrocinios
     * Crear algunas relaciones de patrocinio y recargar datos
     */
    fun createTestData() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🧪 [PATROCINIO_TEST] Creando datos de prueba...")

                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // IDs de usuarios existentes conocidos del screenshot
                val joseAngel = "1UM5l7iQ9DeLmzDgW7Xtb98gHOi1" // Jose Angel
                val usuario2 = "O8HjD4kGwuNS76ldNKZYDW5KcZ82"  // Usuario 2
                val usuario3 = "SBz2ZvZU9iWauY7czWKM8SZ0YvJ2"  // Usuario 3

                // Test 1: Jose Angel patrocina a Usuario2
                val resultado1 = firestoreRepository.patrocinarUsuario(joseAngel, usuario2)

                // Test 2: Usuario3 patrocina a Jose Angel
                val resultado2 = firestoreRepository.patrocinarUsuario(usuario3, joseAngel)

                val exitos = mutableListOf<String>()
                val errores = mutableListOf<String>()

                resultado1.fold(
                    onSuccess = {
                        exitos.add("✅ Jose Angel → Usuario2")
                        Log.d(TAG, "🧪 [PATROCINIO_TEST] ✅ Patrocinio 1 creado")
                    },
                    onFailure = {
                        errores.add("❌ Jose Angel → Usuario2: ${it.message}")
                        Log.e(TAG, "🧪 [PATROCINIO_TEST] ❌ Error patrocinio 1", it)
                    }
                )

                resultado2.fold(
                    onSuccess = {
                        exitos.add("✅ Usuario3 → Jose Angel")
                        Log.d(TAG, "🧪 [PATROCINIO_TEST] ✅ Patrocinio 2 creado")
                    },
                    onFailure = {
                        errores.add("❌ Usuario3 → Jose Angel: ${it.message}")
                        Log.e(TAG, "🧪 [PATROCINIO_TEST] ❌ Error patrocinio 2", it)
                    }
                )

                if (errores.isEmpty()) {
                    Log.d(TAG, "🧪 [PATROCINIO_TEST] ✅ Todos los datos creados exitosamente")

                    // Recargar datos para ver los nuevos cambios
                    loadPatrocinios()    // Usuarios que YO patrocino
                    loadPatrocinadores() // Usuarios que ME patrocinan
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Errores: ${errores.joinToString(", ")}"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "🧪 [PATROCINIO_TEST] ❌ Error inesperado", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error inesperado: ${e.message}"
                )
            }
        }
    }

}