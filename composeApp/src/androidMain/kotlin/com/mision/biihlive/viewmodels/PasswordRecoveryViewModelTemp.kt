package com.mision.biihlive.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mision.biihlive.data.repository.PasswordRecoveryRepositoryTemp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estados de la UI para recuperación temporal (solo envío de email)
 */
data class PasswordRecoveryTempUiState(
    val step: RecoveryTempStep = RecoveryTempStep.SEND_EMAIL,
    val isLoading: Boolean = false,
    val userEmail: String = "",
    val message: String? = null,
    val error: String? = null,
    val isRecoveryComplete: Boolean = false
)

/**
 * Pasos simplificados del proceso temporal
 */
enum class RecoveryTempStep {
    SEND_EMAIL,     // Pantalla para ingresar email
    COMPLETED       // Email enviado exitosamente
}

/**
 * ViewModel temporal para recuperación de contraseña
 * Solo maneja envío de email usando Firebase Auth nativo
 */
class PasswordRecoveryViewModelTemp(
    private val repository: PasswordRecoveryRepositoryTemp = PasswordRecoveryRepositoryTemp()
) : ViewModel() {

    companion object {
        private const val TAG = "PasswordRecoveryTempVM"
    }

    private val _uiState = MutableStateFlow(PasswordRecoveryTempUiState())
    val uiState: StateFlow<PasswordRecoveryTempUiState> = _uiState.asStateFlow()

    /**
     * Actualizar email ingresado por el usuario
     */
    fun updateEmail(newEmail: String) {
        _uiState.update {
            it.copy(
                userEmail = newEmail.trim(),
                error = null
            )
        }
    }

    /**
     * Enviar email de recuperación
     */
    fun sendRecoveryEmail() {
        val currentEmail = _uiState.value.userEmail.trim()

        if (!repository.isValidEmail(currentEmail)) {
            _uiState.update {
                it.copy(error = "Ingresa un email válido")
            }
            return
        }

        if (_uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                message = null
            )
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Enviando email de recuperación a: $currentEmail")

                repository.sendRecoveryEmail(currentEmail)
                    .onSuccess { message ->
                        Log.d(TAG, "✅ Email enviado exitosamente")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                step = RecoveryTempStep.COMPLETED,
                                isRecoveryComplete = true,
                                message = message
                            )
                        }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "❌ Error enviando email", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Error enviando email"
                            )
                        }
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Excepción enviando email", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error inesperado: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Limpiar errores
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Limpiar mensajes
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    /**
     * Reiniciar proceso
     */
    fun resetProcess() {
        _uiState.update {
            PasswordRecoveryTempUiState()
        }
    }
}