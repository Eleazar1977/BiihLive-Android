package com.mision.biihlive.presentation.suscripcion.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mision.biihlive.data.repository.FirestoreRepository
import com.mision.biihlive.domain.perfil.model.PerfilUsuario
import com.mision.biihlive.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SuscripcionUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val perfil: PerfilUsuario? = null,
    val estaSuscrito: Boolean = false,
    val isProcessingSuscripcion: Boolean = false
)

class SuscripcionViewModel(
    private val targetUserId: String,
    private val firestoreRepository: FirestoreRepository,
    private val sessionManager: SessionManager,
    private val context: android.content.Context
) : ViewModel() {

    companion object {
        private const val TAG = "SuscripcionVM"
    }

    private val _uiState = MutableStateFlow(SuscripcionUiState())
    val uiState: StateFlow<SuscripcionUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                Log.d(TAG, "💳 [SUSCR_UI] Cargando datos para usuario: $targetUserId")

                // Cargar perfil del usuario
                val perfilResult = firestoreRepository.getPerfilUsuario(targetUserId)
                perfilResult.fold(
                    onSuccess = { perfil ->
                        if (perfil != null) {
                            _uiState.value = _uiState.value.copy(perfil = perfil)
                            Log.d(TAG, "💳 [SUSCR_UI] Perfil cargado: ${perfil.nickname}")
                        }
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Error cargando perfil", e)
                        _uiState.value = _uiState.value.copy(error = "Error al cargar el perfil")
                    }
                )

                // Verificar estado de suscripción
                checkSuscripcionStatus()

            } catch (e: Exception) {
                Log.e(TAG, "Error en loadInitialData", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar los datos"
                )
            }
        }
    }

    private suspend fun checkSuscripcionStatus() {
        try {
            val currentUserId = sessionManager.getUserId(context)
            if (currentUserId != null) {
                val result = firestoreRepository.estaSuscrito(currentUserId, targetUserId)
                result.fold(
                    onSuccess = { estaSuscrito ->
                        _uiState.value = _uiState.value.copy(
                            estaSuscrito = estaSuscrito,
                            isLoading = false
                        )
                        Log.d(TAG, "💳 [SUSCR_UI] Estado de suscripción: $estaSuscrito")
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Error verificando suscripción", e)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Error al verificar suscripción"
                        )
                    }
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Usuario no autenticado"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en checkSuscripcionStatus", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Error al verificar suscripción"
            )
        }
    }

    fun toggleSuscripcion() {
        val currentUserId = sessionManager.getUserId(context)
        if (currentUserId == null) {
            _uiState.value = _uiState.value.copy(error = "Usuario no autenticado")
            return
        }

        if (currentUserId == targetUserId) {
            _uiState.value = _uiState.value.copy(error = "No puedes suscribirte a ti mismo")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isProcessingSuscripcion = true, error = null)

                if (_uiState.value.estaSuscrito) {
                    // Cancelar suscripción
                    Log.d(TAG, "💳 [SUSCR_UI] Cancelando suscripción a $targetUserId")
                    val result = firestoreRepository.cancelarSuscripcion(currentUserId, targetUserId)
                    result.fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(
                                estaSuscrito = false,
                                isProcessingSuscripcion = false
                            )
                            Log.d(TAG, "💳 [SUSCR_UI] ✅ Suscripción cancelada")
                        },
                        onFailure = { e ->
                            Log.e(TAG, "Error cancelando suscripción", e)
                            _uiState.value = _uiState.value.copy(
                                isProcessingSuscripcion = false,
                                error = "Error al cancelar suscripción"
                            )
                        }
                    )
                } else {
                    // Suscribirse
                    Log.d(TAG, "💳 [SUSCR_UI] Suscribiéndose a $targetUserId")
                    val result = firestoreRepository.suscribirUsuario(currentUserId, targetUserId)
                    result.fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(
                                estaSuscrito = true,
                                isProcessingSuscripcion = false
                            )
                            Log.d(TAG, "💳 [SUSCR_UI] ✅ Suscripción exitosa")
                        },
                        onFailure = { e ->
                            Log.e(TAG, "Error suscribiéndose", e)
                            _uiState.value = _uiState.value.copy(
                                isProcessingSuscripcion = false,
                                error = "Error al suscribirse"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en toggleSuscripcion", e)
                _uiState.value = _uiState.value.copy(
                    isProcessingSuscripcion = false,
                    error = "Error al procesar la suscripción"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}