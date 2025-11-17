package com.mision.biihlive.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object SessionManager {
    private const val PREF_NAME = "BiihliveSession"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_COGNITO_SUB = "cognito_sub" // NUEVA - Para guardar Cognito Sub específicamente
    private const val KEY_GOOGLE_EMAIL = "google_email"
    private const val KEY_GOOGLE_NAME = "google_name"
    private const val KEY_GOOGLE_PHOTO_URL = "google_photo_url"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    fun saveGoogleSession(context: Context, email: String, name: String? = null, photoUrl: String? = null, userId: String? = null) {
        Log.d("SessionManager", "Guardando sesión Google para: $email, userId: $userId")

        getPrefs(context).edit().apply {
            putString(KEY_GOOGLE_EMAIL, email)
            // Si se proporciona userId, usarlo, sino no generar uno artificial
            userId?.let {
                putString(KEY_USER_ID, it)
                // También guardar como Cognito Sub si es un ID válido
                if (!it.contains("@") && it != "default_user") {
                    putString(KEY_COGNITO_SUB, it)
                }
            }
            name?.let { putString(KEY_GOOGLE_NAME, it) }
            photoUrl?.let { putString(KEY_GOOGLE_PHOTO_URL, it) }
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    private fun generateUserIdFromEmail(email: String): String {
        // Generar un UUID basado en el email para consistencia
        return java.util.UUID.nameUUIDFromBytes(email.toByteArray()).toString()
    }
    
    fun getGoogleEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_GOOGLE_EMAIL, null)
    }
    
    fun getGoogleName(context: Context): String? {
        return getPrefs(context).getString(KEY_GOOGLE_NAME, null)
    }
    
    fun getGooglePhotoUrl(context: Context): String? {
        return getPrefs(context).getString(KEY_GOOGLE_PHOTO_URL, null)
    }
    
    data class GoogleUserInfo(
        val email: String,
        val name: String?,
        val photoUrl: String?
    )
    
    fun getGoogleUserInfo(context: Context): GoogleUserInfo? {
        val email = getGoogleEmail(context) ?: return null
        return GoogleUserInfo(
            email = email,
            name = getGoogleName(context),
            photoUrl = getGooglePhotoUrl(context)
        )
    }
    
    fun isLoggedIn(context: Context): Boolean {
        val isLoggedIn = getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
        val email = getGoogleEmail(context)
        Log.d("SessionManager", "Verificando sesión - LoggedIn: $isLoggedIn, Email: $email")
        return isLoggedIn && !email.isNullOrEmpty()
    }
    
    fun clearSession(context: Context) {
        Log.d("SessionManager", "Limpiando sesión")
        getPrefs(context).edit().clear().apply()
    }
    
    fun updateUserName(context: Context, name: String) {
        getPrefs(context).edit().apply {
            putString(KEY_GOOGLE_NAME, name)
            apply()
        }
    }
    
    // Métodos para Cognito Sub (NUEVOS)
    fun saveCognitoSub(context: Context, cognitoSub: String) {
        Log.d("SessionManager", "Guardando Cognito Sub: $cognitoSub")
        val editor = getPrefs(context).edit()
        editor.putString(KEY_COGNITO_SUB, cognitoSub)
        editor.putString(KEY_USER_ID, cognitoSub) // También actualizar el userId
        editor.apply()
    }

    fun getCognitoSub(context: Context): String? {
        return getPrefs(context).getString(KEY_COGNITO_SUB, null)
    }

    // Métodos actualizados para compatibilidad
    fun getUserId(context: Context): String? {
        // Primero intentar obtener el Cognito Sub
        val cognitoSub = getCognitoSub(context)
        if (cognitoSub != null) {
            Log.d("SessionManager", "Retornando Cognito Sub como userId: $cognitoSub")
            return cognitoSub
        }

        // Luego buscar el userId guardado
        val storedId = getPrefs(context).getString(KEY_USER_ID, null)

        // NUNCA retornar email o "default_user"
        if (storedId != null && !storedId.contains("@") && storedId != "default_user") {
            Log.d("SessionManager", "Retornando userId guardado: $storedId")
            return storedId
        }

        Log.w("SessionManager", "No se encontró userId válido")
        return null // Mejor null que "default_user"
    }

    fun saveUserId(context: Context, userId: String) {
        Log.d("SessionManager", "Guardando userId: $userId")
        // Si el userId no es un email, también guardarlo como Cognito Sub
        if (!userId.contains("@") && userId != "default_user") {
            saveCognitoSub(context, userId)
        } else {
            // Solo guardar como userId regular
            getPrefs(context).edit().apply {
                putString(KEY_USER_ID, userId)
                apply()
            }
        }
    }
    
    fun getUserEmail(context: Context): String? {
        return getGoogleEmail(context)
    }

    fun saveUserEmail(context: Context, email: String) {
        getPrefs(context).edit().apply {
            putString(KEY_GOOGLE_EMAIL, email)
            apply()
        }
    }

    fun saveUserName(context: Context, name: String) {
        getPrefs(context).edit().apply {
            putString(KEY_GOOGLE_NAME, name)
            apply()
        }
    }

    fun getDisplayName(context: Context): String? {
        return getGoogleName(context)
    }
    
    fun saveUserInfo(
        context: Context,
        userId: String,
        displayName: String,
        email: String,
        googleIdToken: String?,
        googleName: String?,
        googlePhotoUrl: String?,
        googleEmail: String
    ) {
        saveGoogleSession(
            context = context,
            email = googleEmail,
            name = googleName ?: displayName,
            photoUrl = googlePhotoUrl,
            userId = userId
        )
    }
}