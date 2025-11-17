package com.mision.biihlive.core.managers

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.mision.biihlive.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestor centralizado de identidad del usuario usando Firebase Auth.
 * ÚNICA fuente de verdad para el userId en toda la aplicación.
 *
 * Este manager garantiza que siempre se use el Firebase UID como ID único.
 *
 * @property context Contexto de la aplicación para persistencia
 */
class UserIdManager private constructor(
    private val context: Context
) {
    private val TAG = "UserIdManager"
    private var cachedUserId: String? = null
    private var cachedUserDetails: UserDetails? = null
    private val firebaseAuth = FirebaseAuth.getInstance()

    data class UserDetails(
        val userId: String,
        val email: String?,
        val nickname: String?,
        val profilePicture: String?
    )

    /**
     * Obtiene el ID único del usuario actual.
     * SIEMPRE retorna el Firebase UID.
     *
     * @return ID único del usuario (Firebase UID)
     * @throws UserNotAuthenticatedException si no hay usuario autenticado
     */
    suspend fun getCurrentUserId(): String = withContext(Dispatchers.IO) {
        // 1. PRIORIDAD: Firebase Auth (fuente de verdad)
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null && !currentUser.isAnonymous) {
            val firebaseUserId = currentUser.uid
            Log.d(TAG, "✅ Usuario autenticado en Firebase Auth: $firebaseUserId")

            // Actualizar cache y sesión con Firebase UID
            cachedUserId = firebaseUserId
            SessionManager.saveUserId(context, firebaseUserId)

            return@withContext firebaseUserId
        }

        // 2. Cache en memoria (solo si coincide con Firebase)
        cachedUserId?.let { cached ->
            Log.w(TAG, "⚠️ Cache encontrado pero no hay Firebase Auth válido: $cached")
            // Limpiar cache inválido
            cachedUserId = null
        }

        // 3. Sesión persistente (legacy, pero verificar con Firebase)
        val sessionUserId = SessionManager.getUserId(context)
        if (!sessionUserId.isNullOrBlank()) {
            Log.w(TAG, "⚠️ Sesión persistente encontrada pero no hay Firebase Auth válido: $sessionUserId")
            Log.w(TAG, "⚠️ Esto indica sesión de migración AWS. Requiere re-autenticación Firebase.")
        }

        // 4. No hay usuario autenticado EN FIREBASE
        Log.e(TAG, "❌ No hay usuario autenticado en Firebase Auth")
        Log.e(TAG, "💡 Sugerencia: El usuario debe hacer login nuevamente")
        throw UserNotAuthenticatedException("Usuario no autenticado en Firebase Auth - requiere login")
    }

    /**
     * Obtiene los detalles completos del usuario actual.
     *
     * @return Detalles del usuario o null si no está autenticado
     */
    suspend fun getUserDetails(): UserDetails = withContext(Dispatchers.IO) {
        // Cache en memoria
        cachedUserDetails?.let {
            Log.d(TAG, "Retornando user details desde cache: ${it.email}")
            return@withContext it
        }

        try {
            // Obtener desde Firebase Auth
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                val userDetails = UserDetails(
                    userId = currentUser.uid,
                    email = currentUser.email,
                    nickname = currentUser.displayName,
                    profilePicture = currentUser.photoUrl?.toString()
                )

                Log.d(TAG, "User details obtenidos desde Firebase:")
                Log.d(TAG, "  - UserId: ${userDetails.userId}")
                Log.d(TAG, "  - Email: ${userDetails.email}")
                Log.d(TAG, "  - Nickname: ${userDetails.nickname}")
                Log.d(TAG, "  - ProfilePicture: ${userDetails.profilePicture}")

                cachedUserDetails = userDetails
                return@withContext userDetails
            } else {
                throw UserNotAuthenticatedException("Usuario no autenticado en Firebase")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo detalles del usuario", e)
            throw UserNotAuthenticatedException("Error obteniendo detalles: ${e.message}")
        }
    }

    /**
     * Actualiza el cache después de un login exitoso.
     */
    fun updateUserIdAfterLogin() {
        Log.d(TAG, "Limpiando cache después de login")
        cachedUserId = null
        cachedUserDetails = null
    }

    /**
     * Limpia el cache (usado en logout).
     */
    fun clearCache() {
        Log.d(TAG, "Limpiando cache de UserIdManager")
        cachedUserId = null
        cachedUserDetails = null
    }

    companion object {
        @Volatile
        private var INSTANCE: UserIdManager? = null

        fun getInstance(context: Context): UserIdManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserIdManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

/**
 * Excepción lanzada cuando no hay usuario autenticado.
 */
class UserNotAuthenticatedException(message: String) : Exception(message)