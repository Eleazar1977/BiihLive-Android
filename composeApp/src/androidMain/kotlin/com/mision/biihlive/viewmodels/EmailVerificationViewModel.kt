package com.mision.biihlive.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mision.biihlive.data.repository.EmailVerificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estados de la UI para verificación de email
 */
data class EmailVerificationUiState(
    val step: VerificationStep = VerificationStep.SEND_CODE,
    val isLoading: Boolean = false,
    val isVerifying: Boolean = false,
    val enteredCode: String = "",
    val userEmail: String? = null,
    val message: String? = null,
    val error: String? = null,
    val canResend: Boolean = true,
    val resendCooldown: Int = 0,
    val isVerificationComplete: Boolean = false
)

/**
 * Pasos del proceso de verificación
 */
enum class VerificationStep {
    SEND_CODE,      // Pantalla inicial para enviar código
    ENTER_CODE,     // Pantalla para ingresar código
    COMPLETED       // Verificación completada
}

/**
 * ViewModel para manejo de verificación de email con códigos de 6 dígitos
 */
class EmailVerificationViewModel(
    private val userEmail: String,
    private val userId: String,
    private val repository: EmailVerificationRepository = EmailVerificationRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "EmailVerificationVM"
        private const val RESEND_COOLDOWN_SECONDS = 60
    }

    private val _uiState = MutableStateFlow(EmailVerificationUiState())
    val uiState: StateFlow<EmailVerificationUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
        checkIfAlreadyVerified()
    }

    /**
     * Cargar información del usuario actual
     */
    private fun loadUserInfo() {
        val (userId, email) = repository.getCurrentUserInfo()
        _uiState.update {
            it.copy(userEmail = email)
        }
        Log.d(TAG, "Usuario cargado: $email")
    }

    /**
     * Verificar si el email ya está verificado
     */
    private fun checkIfAlreadyVerified() {
        viewModelScope.launch {
            if (repository.isEmailAlreadyVerified()) {
                Log.d(TAG, "Email ya verificado, completando proceso")
                _uiState.update {
                    it.copy(
                        step = VerificationStep.COMPLETED,
                        isVerificationComplete = true,
                        message = "Tu email ya está verificado"
                    )
                }
            }
        }
    }

    /**
     * Enviar código de verificación con navegación optimista
     */
    fun sendVerificationCode() {
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
                // ✅ NAVEGACIÓN OPTIMISTA: Validación rápida inicial (3 segundos)
                Log.d(TAG, "Validando datos del usuario...")
                kotlinx.coroutines.delay(3000) // Simular validación inicial

                // Navegar optimistamente a la pantalla de código
                Log.d(TAG, "Navegando a pantalla de código mientras se envía email...")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        step = VerificationStep.ENTER_CODE,
                        message = "Enviando código a tu email...",
                        canResend = false,
                        resendCooldown = RESEND_COOLDOWN_SECONDS
                    )
                }
                startResendCooldown()

                // ✅ ENVÍO EN BACKGROUND: Enviar email de forma asíncrona
                Log.d(TAG, "Enviando email en background...")
                repository.sendVerificationCode(userEmail, userId)
                    .onSuccess { message ->
                        Log.d(TAG, "✅ Código enviado exitosamente en background")
                        _uiState.update {
                            it.copy(
                                message = "Código enviado correctamente. Revisa tu email."
                            )
                        }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "❌ Error enviando código en background", error)
                        _uiState.update {
                            it.copy(
                                error = "Error enviando email: ${error.message}. Usa el botón reenviar.",
                                message = null
                            )
                        }
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Excepción en navegación optimista", e)
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
     * Actualizar código ingresado por el usuario
     */
    fun updateCode(newCode: String) {
        if (newCode.length <= 6 && newCode.all { it.isDigit() }) {
            _uiState.update { it.copy(enteredCode = newCode, error = null) }

            // Auto-verificar cuando se completa el código
            if (newCode.length == 6) {
                verifyCode(newCode)
            }
        }
    }

    /**
     * Verificar código de 6 dígitos
     */
    private fun verifyCode(code: String) {
        if (_uiState.value.isVerifying) return

        _uiState.update {
            it.copy(
                isVerifying = true,
                error = null,
                message = null
            )
        }

        viewModelScope.launch {
            try {
                repository.verifyCode(code)
                    .onSuccess { message ->
                        Log.d(TAG, "Código verificado exitosamente")
                        _uiState.update {
                            it.copy(
                                isVerifying = false,
                                step = VerificationStep.COMPLETED,
                                isVerificationComplete = true,
                                message = message
                            )
                        }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Error verificando código", error)
                        _uiState.update {
                            it.copy(
                                isVerifying = false,
                                error = error.message ?: "Código incorrecto",
                                enteredCode = "" // Limpiar código incorrecto
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción verificando código", e)
                _uiState.update {
                    it.copy(
                        isVerifying = false,
                        error = "Error inesperado: ${e.message}",
                        enteredCode = ""
                    )
                }
            }
        }
    }

    /**
     * Reenviar código de verificación
     */
    fun resendCode() {
        if (_uiState.value.isLoading || !_uiState.value.canResend) return

        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                message = null,
                enteredCode = ""
            )
        }

        viewModelScope.launch {
            try {
                repository.resendVerificationCode(userEmail, userId)
                    .onSuccess { message ->
                        Log.d(TAG, "Código reenviado exitosamente")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                message = message,
                                canResend = false,
                                resendCooldown = RESEND_COOLDOWN_SECONDS
                            )
                        }
                        startResendCooldown()
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Error reenviando código", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Error reenviando código"
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción reenviando código", e)
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
     * Iniciar countdown para habilitar reenvío
     */
    private fun startResendCooldown() {
        viewModelScope.launch {
            var countdown = RESEND_COOLDOWN_SECONDS
            while (countdown > 0) {
                _uiState.update { it.copy(resendCooldown = countdown) }
                kotlinx.coroutines.delay(1000)
                countdown--
            }
            _uiState.update {
                it.copy(
                    canResend = true,
                    resendCooldown = 0
                )
            }
        }
    }

    /**
     * Volver al paso anterior
     */
    fun goBack() {
        when (_uiState.value.step) {
            VerificationStep.ENTER_CODE -> {
                _uiState.update {
                    it.copy(
                        step = VerificationStep.SEND_CODE,
                        enteredCode = "",
                        error = null,
                        message = null
                    )
                }
            }
            else -> {
                // No hacer nada o manejar navegación global
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
     * Reiniciar proceso (para casos de error grave)
     */
    fun resetProcess() {
        _uiState.update {
            EmailVerificationUiState(
                userEmail = it.userEmail
            )
        }
        checkIfAlreadyVerified()
    }
}