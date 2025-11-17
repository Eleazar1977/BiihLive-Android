package com.mision.biihlive.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * Repository para verificación de email con códigos de 6 dígitos
 * Integra con Firebase Cloud Functions para envío y verificación
 */
class EmailVerificationRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "EmailVerificationRepo"
        private const val FUNCTION_SEND_CODE = "sendEmailVerificationCode"
        private const val FUNCTION_VERIFY_CODE = "verifyEmailCode"
        private const val FUNCTION_RESEND_CODE = "resendEmailVerificationCode"
    }

    /**
     * Enviar código de verificación al email del usuario
     */
    suspend fun sendVerificationCode(email: String, userId: String): Result<String> {
        return try {
            Log.d(TAG, "📤 Enviando código de verificación a: '$email' con userId: '$userId'")
            Log.d(TAG, "🔍 DEBUG - email.isEmpty(): ${email.isEmpty()}, userId.isEmpty(): ${userId.isEmpty()}")

            if (email.isEmpty() || userId.isEmpty()) {
                Log.e(TAG, "❌ ERROR: Email o userId están vacíos - email: '$email', userId: '$userId'")
                return Result.failure(Exception("Email y userId no pueden estar vacíos"))
            }

            val data = hashMapOf(
                "email" to email,
                "userId" to userId
            )

            Log.d(TAG, "🔄 Llamando a Firebase Function con datos: $data")
            Log.d(TAG, "🔄 DEBUG - data['email']: '${data["email"]}', data['userId']: '${data["userId"]}'")

            val result = functions.getHttpsCallable(FUNCTION_SEND_CODE)
                .call(data)
                .await()

            val responseData = result.data as? Map<String, Any>
            val message = responseData?.get("message") as? String ?: "Código enviado"

            Log.d(TAG, "Código enviado exitosamente")
            Result.success(message)

        } catch (e: Exception) {
            Log.e(TAG, "Error enviando código de verificación", e)
            Result.failure(e)
        }
    }

    /**
     * Verificar código de 6 dígitos
     */
    suspend fun verifyCode(code: String): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("No hay usuario autenticado"))
            }

            if (code.length != 6 || !code.all { it.isDigit() }) {
                return Result.failure(Exception("El código debe ser de 6 dígitos numéricos"))
            }

            val data = hashMapOf(
                "userId" to currentUser.uid,
                "code" to code
            )

            Log.d(TAG, "Verificando código para usuario: ${currentUser.uid}")

            val result = functions.getHttpsCallable(FUNCTION_VERIFY_CODE)
                .call(data)
                .await()

            val responseData = result.data as? Map<String, Any>
            val message = responseData?.get("message") as? String ?: "Verificación exitosa"

            Log.d(TAG, "Código verificado exitosamente")
            Result.success(message)

        } catch (e: Exception) {
            Log.e(TAG, "Error verificando código", e)

            // Extraer mensaje de error específico de Cloud Functions
            val errorMessage = when {
                e.message?.contains("deadline-exceeded") == true -> "El código ha expirado"
                e.message?.contains("resource-exhausted") == true -> "Demasiados intentos fallidos"
                e.message?.contains("invalid-argument") == true -> {
                    when {
                        e.message?.contains("Código incorrecto") == true -> e.message!!
                        else -> "Código inválido"
                    }
                }
                e.message?.contains("already-exists") == true -> "Este email ya fue verificado"
                e.message?.contains("not-found") == true -> "Código de verificación no encontrado"
                else -> "Error en la verificación: ${e.message}"
            }

            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Reenviar código de verificación
     */
    suspend fun resendVerificationCode(email: String, userId: String): Result<String> {
        return try {
            val data = hashMapOf(
                "email" to email,
                "userId" to userId
            )

            Log.d(TAG, "Reenviando código de verificación a: $email")

            val result = functions.getHttpsCallable(FUNCTION_RESEND_CODE)
                .call(data)
                .await()

            val responseData = result.data as? Map<String, Any>
            val message = responseData?.get("message") as? String ?: "Código reenviado"

            Log.d(TAG, "Código reenviado exitosamente")
            Result.success(message)

        } catch (e: Exception) {
            Log.e(TAG, "Error reenviando código", e)

            val errorMessage = when {
                e.message?.contains("resource-exhausted") == true ->
                    "Debes esperar antes de solicitar un nuevo código"
                else -> "Error reenviando código: ${e.message}"
            }

            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Obtener información del usuario actual
     */
    fun getCurrentUserInfo(): Pair<String?, String?> {
        val currentUser = auth.currentUser
        return Pair(currentUser?.uid, currentUser?.email)
    }

    /**
     * Verificar si el email actual ya está verificado
     */
    suspend fun isEmailAlreadyVerified(): Boolean {
        return try {
            auth.currentUser?.reload()?.await()
            auth.currentUser?.isEmailVerified == true
        } catch (e: Exception) {
            Log.w(TAG, "Error verificando estado del email", e)
            false
        }
    }
}