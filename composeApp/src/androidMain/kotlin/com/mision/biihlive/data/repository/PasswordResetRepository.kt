package com.mision.biihlive.data.repository

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * Repository para recuperación de contraseña con correo corporativo
 * Usa Firebase Cloud Functions con extensión de email (noreply@biihlive.com)
 */
class PasswordResetRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {
    companion object {
        private const val TAG = "PasswordResetRepo"
        private const val FUNCTION_SEND_RESET_EMAIL = "sendPasswordResetEmail"
    }

    /**
     * Enviar email de recuperación de contraseña usando correo corporativo
     */
    suspend fun sendPasswordResetEmail(email: String): Result<String> {
        return try {
            if (email.isBlank()) {
                return Result.failure(Exception("El email es requerido"))
            }

            if (!isValidEmail(email)) {
                return Result.failure(Exception("El formato del email no es válido"))
            }

            val data = hashMapOf(
                "email" to email.trim().lowercase(),
                "language" to "es", // Español por defecto
                "template" to "password_reset" // Template específico para password reset
            )

            Log.d(TAG, "Enviando email de recuperación a: $email")

            val result = functions.getHttpsCallable(FUNCTION_SEND_RESET_EMAIL)
                .call(data)
                .await()

            val responseData = result.data as? Map<String, Any>
            val message = responseData?.get("message") as? String
                ?: "Email de recuperación enviado correctamente"

            Log.d(TAG, "✅ Email de recuperación enviado exitosamente")
            Result.success(message)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enviando email de recuperación", e)
            Result.failure(
                Exception("Error enviando email de recuperación: ${e.message}")
            )
        }
    }

    /**
     * Validar formato de email básico
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Verificar si el email está registrado en el sistema
     * Esta función puede ser llamada antes del envío para mejor UX
     */
    suspend fun checkEmailExists(email: String): Result<Boolean> {
        return try {
            val data = hashMapOf("email" to email.trim().lowercase())

            val result = functions.getHttpsCallable("checkUserEmailExists")
                .call(data)
                .await()

            val responseData = result.data as? Map<String, Any>
            val exists = responseData?.get("exists") as? Boolean ?: false

            Log.d(TAG, "Email $email existe: $exists")
            Result.success(exists)

        } catch (e: Exception) {
            Log.w(TAG, "No se pudo verificar email, continuando con envío", e)
            // En caso de error, asumir que existe para no bloquear el flujo
            Result.success(true)
        }
    }
}