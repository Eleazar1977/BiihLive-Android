package com.mision.biihlive.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mision.biihlive.data.repository.PasswordRecoveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estados de la UI para recuperación de contraseña
 */
data class PasswordRecoveryUiState(
    val step: RecoveryStep = RecoveryStep.SEND_CODE,
    val isLoading: Boolean = false,
    val isVerifying: Boolean = false,
    val isChangingPassword: Boolean = false,
    val userEmail: String = "",
    val enteredCode: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val message: String? = null,
    val error: String? = null,
    val canResend: Boolean = true,
    val resendCooldown: Int = 0,
    val isRecoveryComplete: Boolean = false,
    val passwordErrors: List<String> = emptyList()
)

/**
 * Pasos del proceso de recuperación de contraseña
 */
enum class RecoveryStep {
    SEND_CODE,          // Pantalla inicial para ingresar email y enviar código
    ENTER_CODE,         // Pantalla para ingresar código de recuperación
    NUEVA_PASSWORD,     // Pantalla para ingresar nueva contraseña
    COMPLETED           // Recuperación completada
}

/**
 * ViewModel para manejo de recuperación de contraseña con códigos de 6 dígitos
 * Basado en EmailVerificationViewModel pero adaptado para password recovery
 */
class PasswordRecoveryViewModel(
    private val repository: PasswordRecoveryRepository = PasswordRecoveryRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "PasswordRecoveryVM"
        private const val RESEND_COOLDOWN_SECONDS = 60
    }

    private val _uiState = MutableStateFlow(PasswordRecoveryUiState())
    val uiState: StateFlow<PasswordRecoveryUiState> = _uiState.asStateFlow()

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
     * Enviar código de recuperación con validación de email
     */
    fun sendRecoveryCode() {
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
                // ✅ NAVEGACIÓN OPTIMISTA: Validación rápida inicial (3 segundos)
                Log.d(TAG, "Validando email: $currentEmail")
                kotlinx.coroutines.delay(3000) // Simular validación inicial

                // Navegar optimistamente a la pantalla de código
                Log.d(TAG, "Navegando a pantalla de código mientras se envía email...")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        step = RecoveryStep.ENTER_CODE,
                        message = "Enviando código de recuperación a tu email...",
                        canResend = false,
                        resendCooldown = RESEND_COOLDOWN_SECONDS
                    )
                }
                startResendCooldown()

                // ✅ ENVÍO EN BACKGROUND: Enviar email de forma asíncrona
                Log.d(TAG, "Enviando código de recuperación en background...")
                repository.sendRecoveryCode(currentEmail)
                    .onSuccess { message ->
                        Log.d(TAG, "✅ Código de recuperación enviado exitosamente")
                        _uiState.update {
                            it.copy(
                                message = "Código de recuperación enviado correctamente. Revisa tu email."
                            )
                        }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "❌ Error enviando código de recuperación", error)
                        _uiState.update {
                            it.copy(
                                error = "${error.message}. Usa el botón reenviar.",
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
                verifyRecoveryCode(newCode)
            }
        }
    }

    /**
     * Verificar código de recuperación de 6 dígitos
     */
    private fun verifyRecoveryCode(code: String) {
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
                repository.verifyRecoveryCode(_uiState.value.userEmail, code)
                    .onSuccess { message ->
                        Log.d(TAG, "Código de recuperación verificado exitosamente")
                        _uiState.update {
                            it.copy(
                                isVerifying = false,
                                step = RecoveryStep.NUEVA_PASSWORD,
                                message = "Código válido. Ahora ingresa tu nueva contraseña"
                            )
                        }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Error verificando código de recuperación", error)
                        _uiState.update {
                            it.copy(
                                isVerifying = false,
                                error = error.message ?: "Código incorrecto",
                                enteredCode = "" // Limpiar código incorrecto
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción verificando código de recuperación", e)
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
     * Actualizar nueva contraseña
     */
    fun updateNewPassword(newPassword: String) {
        _uiState.update {
            it.copy(
                newPassword = newPassword,
                passwordErrors = validatePassword(newPassword),
                error = null
            )
        }
    }

    /**
     * Actualizar confirmación de contraseña
     */
    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.update {
            it.copy(
                confirmPassword = confirmPassword,
                error = null
            )
        }
    }

    /**
     * Validar contraseña y retornar lista de errores
     */
    private fun validatePassword(password: String): List<String> {
        val errors = mutableListOf<String>()

        if (password.length < 6) {
            errors.add("Mínimo 6 caracteres")
        }
        if (!password.any { it.isLetter() }) {
            errors.add("Debe contener al menos una letra")
        }
        if (!password.any { it.isDigit() }) {
            errors.add("Debe contener al menos un número")
        }

        return errors
    }

    /**
     * Cambiar contraseña con validaciones
     */
    fun changePassword() {
        val currentState = _uiState.value

        // Validar contraseñas
        if (!repository.doPasswordsMatch(currentState.newPassword, currentState.confirmPassword)) {
            _uiState.update {
                it.copy(error = "Las contraseñas no coinciden")
            }
            return
        }

        if (!repository.isValidPassword(currentState.newPassword)) {
            _uiState.update {
                it.copy(error = "La contraseña debe tener al menos 6 caracteres")
            }
            return
        }

        if (currentState.passwordErrors.isNotEmpty()) {
            _uiState.update {
                it.copy(error = "Corrige los errores en la contraseña")
            }
            return
        }

        if (currentState.isChangingPassword) return

        _uiState.update {
            it.copy(
                isChangingPassword = true,
                error = null,
                message = null
            )
        }

        viewModelScope.launch {
            try {
                repository.resetPasswordWithCode(
                    email = currentState.userEmail,
                    code = currentState.enteredCode,
                    newPassword = currentState.newPassword
                )
                    .onSuccess { message ->
                        Log.d(TAG, "Contraseña cambiada exitosamente")
                        _uiState.update {
                            it.copy(
                                isChangingPassword = false,
                                step = RecoveryStep.COMPLETED,
                                isRecoveryComplete = true,
                                message = message
                            )
                        }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Error cambiando contraseña", error)
                        _uiState.update {
                            it.copy(
                                isChangingPassword = false,
                                error = error.message ?: "Error cambiando contraseña"
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción cambiando contraseña", e)
                _uiState.update {
                    it.copy(
                        isChangingPassword = false,
                        error = "Error inesperado: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Reenviar código de recuperación
     */
    fun resendRecoveryCode() {
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
                repository.resendRecoveryCode(_uiState.value.userEmail)
                    .onSuccess { message ->
                        Log.d(TAG, "Código de recuperación reenviado exitosamente")
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
                        Log.e(TAG, "Error reenviando código de recuperación", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Error reenviando código"
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción reenviando código de recuperación", e)
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
            RecoveryStep.ENTER_CODE -> {
                _uiState.update {
                    it.copy(
                        step = RecoveryStep.SEND_CODE,
                        enteredCode = "",
                        error = null,
                        message = null
                    )
                }
            }
            RecoveryStep.NUEVA_PASSWORD -> {
                _uiState.update {
                    it.copy(
                        step = RecoveryStep.ENTER_CODE,
                        newPassword = "",
                        confirmPassword = "",
                        passwordErrors = emptyList(),
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
            PasswordRecoveryUiState()
        }
    }

    /**
     * Verificar si el botón de cambiar contraseña debe estar habilitado
     */
    fun isChangePasswordEnabled(): Boolean {
        val state = _uiState.value
        return state.newPassword.isNotBlank() &&
               state.confirmPassword.isNotBlank() &&
               state.passwordErrors.isEmpty() &&
               repository.doPasswordsMatch(state.newPassword, state.confirmPassword) &&
               !state.isChangingPassword
    }
}