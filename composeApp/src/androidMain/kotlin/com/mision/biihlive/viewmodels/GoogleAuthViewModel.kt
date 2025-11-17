package com.mision.biihlive.viewmodels

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.mision.biihlive.R

class GoogleAuthViewModel : ViewModel() {

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _googleAccount = MutableLiveData<GoogleSignInAccount?>()
    val googleAccount: LiveData<GoogleSignInAccount?> = _googleAccount

    private val _googleToken = MutableLiveData<String?>()
    val googleToken: LiveData<String?> = _googleToken

    private lateinit var googleSignInClient: GoogleSignInClient

    init {
        _authState.value = AuthState.SignedOut
    }

    fun initGoogleSignIn(context: Context) {
        try {
            // Configuración básica - solo solicitar información de perfil y email (SIN idToken)
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestProfile()  // Solicita nombre, foto, etc.
                .requestEmail()    // Solicita dirección de email
                .build()

            googleSignInClient = GoogleSignIn.getClient(context, gso)
            Log.d("GoogleAuthDiag", "GoogleSignInClient inicializado con configuración básica")

            checkGoogleSignInStatus(context)
        } catch (e: Exception) {
            Log.e("GoogleAuthDiag", "Error al inicializar GoogleSignInClient", e)
            _authState.value = AuthState.Error("Error al inicializar: ${e.message}")
        }
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun handleSignInResult(result: ActivityResult) {
        Log.d("GoogleAuthDiag", "=== PROCESANDO RESULTADO DE GOOGLE SIGN IN ===")
        Log.d("GoogleAuthDiag", "Result Code: ${result.resultCode}")

        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            Log.d("GoogleAuthDiag", "Task obtenido del intent")

            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("GoogleAuthDiag", "✅ INICIO DE SESIÓN EXITOSO")
                Log.d("GoogleAuthDiag", "Email: ${account?.email}")
                Log.d("GoogleAuthDiag", "Nombre: ${account?.displayName}")
                Log.d("GoogleAuthDiag", "ID: ${account?.id}")

                _googleAccount.value = account
                _authState.value = AuthState.SignedIn

                // Obtener el token si está disponible
                account?.idToken?.let { token ->
                    _googleToken.value = token
                    Log.d("GoogleAuthDiag", "Token ID obtenido")
                } ?: run {
                    Log.w("GoogleAuthDiag", "⚠️ Token ID no disponible (puede ser normal según la configuración)")
                }

            } catch (e: ApiException) {
                Log.e("GoogleAuthDiag", "❌ ERROR EN GOOGLE SIGN IN")
                Log.e("GoogleAuthDiag", "Status Code: ${e.statusCode}")
                Log.e("GoogleAuthDiag", "Status Message: ${getStatusCodeString(e.statusCode)}")
                Log.e("GoogleAuthDiag", "Exception: ${e.message}")

                val errorMessage = when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Inicio de sesión cancelado por el usuario"
                    GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Error en el inicio de sesión"
                    GoogleSignInStatusCodes.NETWORK_ERROR -> "Error de red"
                    GoogleSignInStatusCodes.INVALID_ACCOUNT -> "Cuenta inválida"
                    else -> "Error desconocido: ${e.statusCode}"
                }

                _authState.value = AuthState.Error(errorMessage)
            }

        } catch (e: Exception) {
            Log.e("GoogleAuthDiag", "❌ EXCEPCIÓN GENERAL AL PROCESAR RESULTADO")
            Log.e("GoogleAuthDiag", "Exception: ${e.message}", e)
            _authState.value = AuthState.Error("Error general: ${e.message}")
        }

        Log.d("GoogleAuthDiag", "=== FIN PROCESAMIENTO RESULTADO ===")
    }

    private fun checkGoogleSignInStatus(context: Context) {
        try {
            Log.d("GoogleAuthDiag", "Verificando si hay sesión activa de Google")
            val account = GoogleSignIn.getLastSignedInAccount(context)

            if (account != null) {
                Log.d("GoogleAuthDiag", "Cuenta de Google encontrada: ${account.email}")
                _googleAccount.value = account
                _authState.value = AuthState.SignedIn
            } else {
                Log.d("GoogleAuthDiag", "No hay cuenta de Google guardada")
                _authState.value = AuthState.SignedOut
            }
        } catch (e: Exception) {
            Log.e("GoogleAuthDiag", "Error al verificar estado de inicio de sesión", e)
            _authState.value = AuthState.SignedOut
        }
    }

    fun signOut(context: Context) {
        Log.d("GoogleAuthDiag", "Cerrando sesión")

        googleSignInClient.signOut().addOnCompleteListener {
            Log.d("GoogleAuthDiag", "Sesión cerrada con éxito")
            _authState.value = AuthState.SignedOut
            _googleAccount.value = null
            _googleToken.value = null
        }.addOnFailureListener { e ->
            Log.e("GoogleAuthDiag", "Error al cerrar sesión", e)
            _authState.value = AuthState.Error("Error al cerrar sesión: ${e.message}")
        }
    }
    
    /**
     * Limpia completamente la sesión y revoca el acceso
     * Útil cuando vas a cambiar a un sistema de autenticación diferente
     */
    fun forceSignOutAndRevoke(context: Context) {
        Log.d("GoogleAuthDiag", "Forzando cierre de sesión y revocando acceso")
        
        googleSignInClient.signOut().addOnCompleteListener { signOutTask ->
            if (signOutTask.isSuccessful) {
                Log.d("GoogleAuthDiag", "Sign out exitoso, revocando acceso...")
                
                googleSignInClient.revokeAccess().addOnCompleteListener { revokeTask ->
                    if (revokeTask.isSuccessful) {
                        Log.d("GoogleAuthDiag", "✅ Acceso revocado completamente")
                    } else {
                        Log.w("GoogleAuthDiag", "Advertencia: No se pudo revocar el acceso")
                    }
                    
                    _authState.value = AuthState.SignedOut
                    _googleAccount.value = null
                    _googleToken.value = null
                }
            } else {
                Log.e("GoogleAuthDiag", "Error al cerrar sesión")
                _authState.value = AuthState.Error("Error al cerrar sesión")
            }
        }
    }

    fun setGoogleAccount(account: GoogleSignInAccount) {
        _googleAccount.value = account
        _authState.value = AuthState.SignedIn
        _googleToken.value = account.idToken
        Log.d("GoogleAuthDiag", "Cuenta de Google establecida manualmente: ${account.email}")
    }

    private fun getStatusCodeString(statusCode: Int): String {
        return when (statusCode) {
            CommonStatusCodes.SUCCESS -> "SUCCESS"
            CommonStatusCodes.SIGN_IN_REQUIRED -> "SIGN_IN_REQUIRED"
            CommonStatusCodes.INVALID_ACCOUNT -> "INVALID_ACCOUNT"
            CommonStatusCodes.RESOLUTION_REQUIRED -> "RESOLUTION_REQUIRED"
            CommonStatusCodes.NETWORK_ERROR -> "NETWORK_ERROR"
            CommonStatusCodes.INTERNAL_ERROR -> "INTERNAL_ERROR"
            CommonStatusCodes.SERVICE_DISABLED -> "SERVICE_DISABLED"
            CommonStatusCodes.DEVELOPER_ERROR -> "DEVELOPER_ERROR"
            CommonStatusCodes.ERROR -> "ERROR"
            GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "SIGN_IN_CANCELLED"
            GoogleSignInStatusCodes.SIGN_IN_FAILED -> "SIGN_IN_FAILED"
            else -> "UNKNOWN_$statusCode"
        }
    }
}