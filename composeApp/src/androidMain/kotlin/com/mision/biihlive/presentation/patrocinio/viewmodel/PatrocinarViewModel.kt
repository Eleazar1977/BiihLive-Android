package com.mision.biihlive.presentation.patrocinio.viewmodel

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

data class PatrocinioUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val perfil: PerfilUsuario? = null,
    val estaPatrocinando: Boolean = false,
    val isProcessingPatrocinio: Boolean = false,
    val valorPatrocinio: String = "EUR 70/mes", // Valor por defecto
    val patrocinioInfo: Map<String, Any>? = null
)

class PatrocinarViewModel(
    private val targetUserId: String,
    private val firestoreRepository: FirestoreRepository,
    private val sessionManager: SessionManager,
    private val context: android.content.Context
) : ViewModel() {

    companion object {
        private const val TAG = "PatrocinarVM"
    }

    private val _uiState = MutableStateFlow(PatrocinioUiState())
    val uiState: StateFlow<PatrocinioUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                Log.d(TAG, "💰 [PATR_UI] Cargando datos para usuario: $targetUserId")

                // Cargar perfil del usuario
                val perfilResult = firestoreRepository.getPerfilUsuario(targetUserId)
                perfilResult.fold(
                    onSuccess = { perfil ->
                        if (perfil != null) {
                            _uiState.value = _uiState.value.copy(perfil = perfil)
                            Log.d(TAG, "💰 [PATR_UI] Perfil cargado: ${perfil.nickname}")
                        }
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Error cargando perfil", e)
                        _uiState.value = _uiState.value.copy(error = "Error al cargar el perfil")
                    }
                )

                // Verificar estado de patrocinio
                checkPatrocinioStatus()

            } catch (e: Exception) {
                Log.e(TAG, "Error en loadInitialData", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar los datos"
                )
            }
        }
    }

    private suspend fun checkPatrocinioStatus() {
        try {
            val currentUserId = sessionManager.getUserId(context)
            if (currentUserId != null) {
                // Verificar si está patrocinando
                val patrocinandoResult = firestoreRepository.estaPatrocinando(currentUserId, targetUserId)
                patrocinandoResult.fold(
                    onSuccess = { estaPatrocinando ->
                        _uiState.value = _uiState.value.copy(
                            estaPatrocinando = estaPatrocinando,
                            isLoading = false
                        )
                        Log.d(TAG, "💰 [PATR_UI] Estado de patrocinio: $estaPatrocinando")

                        // Si está patrocinando, obtener la información del patrocinio
                        if (estaPatrocinando) {
                            loadPatrocinioInfo(currentUserId)
                        }
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Error verificando patrocinio", e)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Error al verificar patrocinio"
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
            Log.e(TAG, "Error en checkPatrocinioStatus", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Error al verificar patrocinio"
            )
        }
    }

    private suspend fun loadPatrocinioInfo(currentUserId: String) {
        try {
            val patrocinioInfoResult = firestoreRepository.getPatrocinioInfo(currentUserId, targetUserId)
            patrocinioInfoResult.fold(
                onSuccess = { patrocinioInfo ->
                    if (patrocinioInfo != null) {
                        // Actualizar valor del patrocinio desde la base de datos
                        val valorFromDB = patrocinioInfo["valorPatrocinio"] as? String ?: "EUR 70/mes"
                        _uiState.value = _uiState.value.copy(
                            patrocinioInfo = patrocinioInfo,
                            valorPatrocinio = valorFromDB
                        )
                        Log.d(TAG, "💰 [PATR_UI] Info del patrocinio cargada: $valorFromDB")
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "Error cargando info del patrocinio", e)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error en loadPatrocinioInfo", e)
        }
    }

    fun updateValorPatrocinio(newValue: String) {
        _uiState.value = _uiState.value.copy(valorPatrocinio = newValue)
    }

    fun togglePatrocinio() {
        val currentUserId = sessionManager.getUserId(context)
        if (currentUserId == null) {
            _uiState.value = _uiState.value.copy(error = "Usuario no autenticado")
            return
        }

        if (currentUserId == targetUserId) {
            _uiState.value = _uiState.value.copy(error = "No puedes patrocinarte a ti mismo")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isProcessingPatrocinio = true, error = null)

                if (_uiState.value.estaPatrocinando) {
                    // Cancelar patrocinio
                    Log.d(TAG, "💰 [PATR_UI] Cancelando patrocinio a $targetUserId")
                    val result = firestoreRepository.cancelarPatrocinio(currentUserId, targetUserId)
                    result.fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(
                                estaPatrocinando = false,
                                isProcessingPatrocinio = false,
                                patrocinioInfo = null
                            )
                            Log.d(TAG, "💰 [PATR_UI] ✅ Patrocinio cancelado")
                        },
                        onFailure = { e ->
                            Log.e(TAG, "Error cancelando patrocinio", e)
                            _uiState.value = _uiState.value.copy(
                                isProcessingPatrocinio = false,
                                error = "Error al cancelar patrocinio"
                            )
                        }
                    )
                } else {
                    // Patrocinar
                    Log.d(TAG, "💰 [PATR_UI] Patrocinando a $targetUserId con valor: ${_uiState.value.valorPatrocinio}")
                    val result = firestoreRepository.patrocinarUsuario(
                        currentUserId,
                        targetUserId,
                        _uiState.value.valorPatrocinio
                    )
                    result.fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(
                                estaPatrocinando = true,
                                isProcessingPatrocinio = false
                            )
                            Log.d(TAG, "💰 [PATR_UI] ✅ Patrocinio exitoso")
                            // Cargar la información del nuevo patrocinio
                            loadPatrocinioInfo(currentUserId)
                        },
                        onFailure = { e ->
                            Log.e(TAG, "Error patrocinando", e)
                            _uiState.value = _uiState.value.copy(
                                isProcessingPatrocinio = false,
                                error = "Error al patrocinar"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en togglePatrocinio", e)
                _uiState.value = _uiState.value.copy(
                    isProcessingPatrocinio = false,
                    error = "Error al procesar el patrocinio"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}