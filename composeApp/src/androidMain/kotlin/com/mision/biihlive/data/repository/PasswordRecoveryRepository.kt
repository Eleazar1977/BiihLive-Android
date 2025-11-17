package com.mision.biihlive.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * Repository para recuperación de contraseña con códigos de 6 dígitos
 * Extiende EmailVerificationRepository para reutilizar funcionalidad existente
 */
class PasswordRecoveryRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "PasswordRecoveryRepo"
        // ✅ Nombres actualizados para coincidir con Cloud Functions desplegadas
        private const val FUNCTION_SEND_RECOVERY_CODE = "sendPasswordResetCode"
        private const val FUNCTION_VERIFY_RECOVERY_CODE = "verifyPasswordResetCode"
        private const val FUNCTION_RESET_PASSWORD = "resetPasswordWithCode"
        // Reenvío reutiliza la misma función de envío
        private const val FUNCTION_RESEND_RECOVERY_CODE = "sendPasswordResetCode"
    }

    /**
     * Enviar código de recuperación de contraseña al email especificado
     * Utiliza Firebase Cloud Functions para envío de códigos de 6 dígitos
     */
    suspend fun sendRecoveryCode(email: String): Result<String> {
        return try {
            if (email.isBlank()) {
                return Result.failure(Exception("Debes ingresar un email"))
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                return Result.failure(Exception("Ingresa un email válido"))
            }

            val emailTrimmed = email.trim().lowercase()
            Log.d(TAG, "🔄 Enviando código de recuperación a: $emailTrimmed")

            val data = hashMapOf("email" to emailTrimmed)
            Log.d(TAG, "📤 Datos enviados a Firebase Function: $data")

            val result = functions.getHttpsCallable(FUNCTION_SEND_RECOVERY_CODE)
                .call(data)
                .await()

            val responseData = result.data as? Map<String, Any>
            val message = responseData?.get("message") as? String ?: "Código enviado"

            Log.d(TAG, "✅ Código de recuperación enviado exitosamente")
            Result.success(message)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enviando código de recuperación", e)

            val errorMessage = when {
                e.message?.contains("not-found") == true -> "No existe una cuenta con este email"
                e.message?.contains("invalid-argument") == true -> "Email es requerido"
                e.message?.contains("too-many-requests") == true -> "Demasiados intentos. Espera unos minutos"
                else -> "Error enviando código: ${e.message}"
            }

            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Verificar código de recuperación de 6 dígitos
     */
    suspend fun verifyRecoveryCode(email: String, code: String): Result<String> {
        return try {
            if (code.length != 6 || !code.all { it.isDigit() }) {
                return Result.failure(Exception("El código debe ser de 6 dígitos numéricos"))
            }

            val data = hashMapOf(
                "email" to email.trim().lowercase(),
                "code" to code
            )

            Log.d(TAG, "Verificando código de recuperación para: $email")

            val result = functions.getHttpsCallable(FUNCTION_VERIFY_RECOVERY_CODE)
                .call(data)
                .await()

            val responseData = result.data as? Map<String, Any>
            val message = responseData?.get("message") as? String ?: "Código válido"

            Log.d(TAG, "Código de recuperación verificado exitosamente")
            Result.success(message)

        } catch (e: Exception) {
            Log.e(TAG, "Error verificando código de recuperación", e)

            val errorMessage = when {
                e.message?.contains("deadline-exceeded") == true -> "El código ha expirado"
                e.message?.contains("resource-exhausted") == true -> "Demasiados intentos fallidos"
                e.message?.contains("invalid-argument") == true -> {
                    when {
                        e.message?.contains("Código incorrecto") == true -> e.message!!
                        else -> "Código inválido"
                    }
                }
                e.message?.contains("not-found") == true -> "Código de recuperación no encontrado o expirado"
                else -> "Error verificando código: ${e.message}"
            }

            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Cambiar contraseña con código verificado
     */
    suspend fun resetPasswordWithCode(
        email: String,
        code: String,
        newPassword: String
    ): Result<String> {
        return try {
            if (newPassword.length < 6) {
                return Result.failure(Exception("La contraseña debe tener al menos 6 caracteres"))
            }

            val data = hashMapOf(
                "email" to email.trim().lowercase(),
                "code" to code,
                "newPassword" to newPassword
            )

            Log.d(TAG, "Cambiando contraseña para: $email")

            val result = functions.getHttpsCallable(FUNCTION_RESET_PASSWORD)
                .call(data)
                .await()

            val responseData = result.data as? Map<String, Any>
            val message = responseData?.get("message") as? String ?: "Contraseña cambiada exitosamente"

            Log.d(TAG, "Contraseña cambiada exitosamente")
            Result.success(message)

        } catch (e: Exception) {
            Log.e(TAG, "Error cambiando contraseña", e)

            val errorMessage = when {
                e.message?.contains("invalid-argument") == true -> {
                    when {
                        e.message?.contains("Código inválido") == true -> "Código inválido o expirado"
                        e.message?.contains("weak-password") == true -> "La contraseña es muy débil"
                        else -> "Datos inválidos"
                    }
                }
                e.message?.contains("not-found") == true -> "Código expirado. Solicita uno nuevo"
                else -> "Error cambiando contraseña: ${e.message}"
            }

            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Reenviar código de recuperación
     */
    suspend fun resendRecoveryCode(email: String): Result<String> {
        return try {
            val data = hashMapOf(
                "email" to email.trim().lowercase()
            )

            Log.d(TAG, "Reenviando código de recuperación a: $email")

            val result = functions.getHttpsCallable(FUNCTION_RESEND_RECOVERY_CODE)
                .call(data)
                .await()

            val responseData = result.data as? Map<String, Any>
            val message = responseData?.get("message") as? String ?: "Código reenviado"

            Log.d(TAG, "Código de recuperación reenviado exitosamente")
            Result.success(message)

        } catch (e: Exception) {
            Log.e(TAG, "Error reenviando código de recuperación", e)

            val errorMessage = when {
                e.message?.contains("resource-exhausted") == true ->
                    "Debes esperar antes de solicitar un nuevo código"
                e.message?.contains("too-many-requests") == true ->
                    "Demasiados intentos. Espera unos minutos"
                else -> "Error reenviando código: ${e.message}"
            }

            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Validar formato de email
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() &&
               android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    /**
     * Validar formato de contraseña
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    /**
     * Verificar si las contraseñas coinciden
     */
    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword && password.isNotBlank()
    }
}