package com.mision.biihlive.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Repository temporal para recuperación de contraseña usando Firebase Auth nativo
 * Esta versión funciona SIN necesidad de Cloud Functions
 * Será reemplazada por la versión completa cuando se desplieguen las Functions
 */
class PasswordRecoveryRepositoryTemp(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "PasswordRecoveryTemp"
    }

    /**
     * Enviar email de recuperación usando Firebase Auth nativo
     */
    suspend fun sendRecoveryEmail(email: String): Result<String> {
        return try {
            if (email.isBlank()) {
                return Result.failure(Exception("Debes ingresar un email"))
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                return Result.failure(Exception("Ingresa un email válido"))
            }

            val emailTrimmed = email.trim().lowercase()
            Log.d(TAG, "🔄 Enviando reset email nativo a: $emailTrimmed")

            // Usar Firebase Auth nativo para reset password
            auth.sendPasswordResetEmail(emailTrimmed).await()

            Log.d(TAG, "✅ Email de reset enviado exitosamente")
            Result.success("Email de recuperación enviado. Revisa tu bandeja de entrada y spam.")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enviando email de recuperación", e)

            val errorMessage = when {
                e.message?.contains("user-not-found") == true ||
                e.message?.contains("USER_NOT_FOUND") == true ->
                    "No existe una cuenta con este email"
                e.message?.contains("too-many-requests") == true ||
                e.message?.contains("TOO_MANY_ATTEMPTS_TRY_LATER") == true ->
                    "Demasiados intentos. Espera unos minutos"
                e.message?.contains("INVALID_EMAIL") == true ->
                    "Formato de email inválido"
                else -> "Error enviando email: ${e.message}"
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
}