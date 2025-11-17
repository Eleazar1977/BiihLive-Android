package com.mision.biihlive.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.mision.biihlive.config.FirebaseConfig
import com.mision.biihlive.core.managers.UserIdManager
import com.mision.biihlive.data.repository.EmailVerificationRepository
import com.mision.biihlive.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseAuthViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val userIdManager = UserIdManager.getInstance(context)
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseConfig.getFirestore()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // Propiedades derivadas del authState para compatibilidad con las pantallas
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        checkCurrentAuthStatus()

        // Sincronizar propiedades derivadas con authState
        viewModelScope.launch {
            authState.collect { state ->
                _isLoading.value = state is AuthState.Loading
                _error.value = if (state is AuthState.Error) state.message else null
            }
        }
    }

    /**
     * Verifica si hay una sesión activa de Firebase
     */
    private fun checkCurrentAuthStatus() {
        viewModelScope.launch {
            try {
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null && !currentUser.isAnonymous) {
                    Log.d("FirebaseAuth", "Sesión existente detectada: ${currentUser.uid}")

                    // Guardar información de usuario
                    saveUserSession(currentUser)

                    // TODO: Conectar sistema de presencia cuando esté disponible

                    _isAuthenticated.value = true
                    _authState.value = AuthState.SignedIn
                } else {
                    Log.d("FirebaseAuth", "No hay sesión activa")
                    _isAuthenticated.value = false
                    _authState.value = AuthState.SignedOut
                }
            } catch (e: Exception) {
                Log.e("FirebaseAuth", "Error verificando sesión", e)
                _authState.value = AuthState.Error("Error verificando sesión: ${e.message}")
            }
        }
    }

    /**
     * Registro con email y contraseña
     * Ahora usa códigos de verificación en lugar de enlaces
     */
    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("El email y la contraseña son requeridos")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                    val user = result.user

                    if (user != null) {
                        Log.d("FirebaseAuth", "Usuario creado exitosamente: ${user.uid}")

                        // Enviar automáticamente el código de verificación
                        try {
                            val repository = EmailVerificationRepository()
                            val result = repository.sendVerificationCode(email, user.uid)

                            if (result.isSuccess) {
                                Log.d("FirebaseAuth", "Código de verificación enviado automáticamente")
                                _authState.value = AuthState.SignUpRequiresConfirmation(email)
                            } else {
                                Log.e("FirebaseAuth", "Error enviando código: ${result.exceptionOrNull()?.message}")
                                _authState.value = AuthState.Error("Error enviando código de verificación: ${result.exceptionOrNull()?.message}")
                            }
                        } catch (e: Exception) {
                            Log.e("FirebaseAuth", "Error enviando código automáticamente", e)
                            _authState.value = AuthState.Error("Error enviando código: ${e.message}")
                        }
                    } else {
                        _authState.value = AuthState.Error("Error al crear usuario")
                    }
                }
            } catch (e: Exception) {
                Log.e("FirebaseAuth", "Error en registro", e)
                _authState.value = AuthState.Error("Error en el registro: ${e.message}")
            }
        }
    }

    /**
     * Inicio de sesión con email y contraseña
     */
    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("El email y la contraseña son requeridos")
            return
        }

        _authState.value = AuthState.Loading
        Log.d("FirebaseAuth", "Iniciando sesión con email: $email")

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                    val user = result.user

                    if (user != null) {
                        Log.d("FirebaseAuth", "Inicio de sesión exitoso: ${user.uid}")

                        // Verificar que el email esté verificado
                        user.reload().await() // Refrescar datos del usuario

                        if (user.isEmailVerified) {
                            Log.d("FirebaseAuth", "Email verificado - permitiendo acceso")

                            // Crear perfil si no existe (para usuarios que se registraron con verificación)
                            createUserProfile(user, email)

                            // Guardar sesión
                            saveUserSession(user)

                            _isAuthenticated.value = true
                            _authState.value = AuthState.SignedIn
                        } else {
                            Log.w("FirebaseAuth", "Email NO verificado - requiere confirmación")
                            _authState.value = AuthState.SignUpRequiresConfirmation(email)
                        }
                    } else {
                        _authState.value = AuthState.Error("Error en el inicio de sesión")
                    }
                }
            } catch (e: Exception) {
                Log.e("FirebaseAuth", "Error en inicio de sesión", e)
                _authState.value = AuthState.Error("Error en el inicio de sesión: ${e.message}")
            }
        }
    }

    /**
     * Inicio de sesión con Google (usando token ID)
     */
    fun signInWithGoogle(idToken: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    val result = firebaseAuth.signInWithCredential(credential).await()
                    val user = result.user

                    if (user != null) {
                        Log.d("FirebaseAuth", "Google Sign-In exitoso: ${user.uid}")

                        // Crear o actualizar perfil
                        user.email?.let { createUserProfile(user, it) }

                        // Guardar sesión
                        saveUserSession(user)

                        // TODO: Conectar presencia cuando esté disponible

                        _isAuthenticated.value = true
                        _authState.value = AuthState.SignedIn
                    } else {
                        _authState.value = AuthState.Error("Error en Google Sign-In")
                    }
                }
            } catch (e: Exception) {
                Log.e("FirebaseAuth", "Error en Google Sign-In", e)
                _authState.value = AuthState.Error("Error en Google Sign-In: ${e.message}")
            }
        }
    }

    /**
     * Cerrar sesión
     */
    fun signOut() {
        Log.d("FirebaseAuth", "=== INICIANDO LOGOUT ===")

        // TODO: Desconectar sistema de presencia cuando esté disponible

        // Limpiar sesiones locales
        SessionManager.clearSession(context)
        userIdManager.clearCache()

        // Actualizar estados inmediatamente
        _isAuthenticated.value = false
        _authState.value = AuthState.SignedOut

        // Cerrar sesión en Firebase
        try {
            firebaseAuth.signOut()
            Log.d("FirebaseAuth", "Firebase sesión cerrada exitosamente")
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Error cerrando sesión de Firebase", e)
        }

        Log.d("FirebaseAuth", "=== LOGOUT COMPLETADO ===")
    }

    /**
     * Recuperar contraseña usando Firebase Auth nativo con dominio corporativo configurado
     */
    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("El email es requerido")
            return
        }

        _authState.value = AuthState.Loading
        _isLoading.value = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Usar Firebase Auth nativo para password reset
                    // Firebase está configurado para usar dominio corporativo en la consola
                    firebaseAuth.sendPasswordResetEmail(email).await()

                    Log.d("FirebaseAuth", "✅ Email de recuperación enviado vía Firebase Auth")
                    _authState.value = AuthState.PasswordResetSent(
                        "Se ha enviado un enlace de recuperación a tu email desde noreply@biihlive.com"
                    )
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("FirebaseAuth", "❌ Error enviando email de recuperación", e)
                _authState.value = AuthState.Error("Error al enviar email de recuperación: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    /**
     * Reenviar email de verificación
     */
    fun resendEmailVerification(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("El email es requerido")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Buscar usuario por email y reenviar verificación
                    val currentUser = firebaseAuth.currentUser
                    if (currentUser != null && currentUser.email == email) {
                        // Usuario actual - reenviar directo
                        currentUser.sendEmailVerification().await()
                        Log.d("FirebaseAuth", "Email de verificación reenviado a usuario actual: $email")
                    } else {
                        // Usuario no actual - intentar login temporal para reenviar
                        Log.w("FirebaseAuth", "Reenvío de verificación solicitado para email diferente al usuario actual")
                        _authState.value = AuthState.Error("Por favor, intenta registrarte nuevamente para recibir un nuevo email de verificación")
                        return@withContext
                    }

                    _authState.value = AuthState.SignUpRequiresConfirmation(email)
                    Log.d("FirebaseAuth", "Email de verificación reenviado exitosamente a: $email")
                }
            } catch (e: Exception) {
                Log.e("FirebaseAuth", "Error reenviando email de verificación", e)
                _authState.value = AuthState.Error("Error al reenviar email: ${e.message}")
            }
        }
    }

    /**
     * Verificar código de email de 6 dígitos
     * Como Firebase no soporta códigos personalizados, este método verifica
     * si el usuario ya confirmó su email haciendo clic en el enlace enviado
     */
    fun verifyEmailCode(email: String, code: String) {
        if (email.isBlank() || code.isBlank()) {
            _authState.value = AuthState.Error("El email y el código son requeridos")
            return
        }

        if (code.length != 6 || !code.all { it.isDigit() }) {
            _authState.value = AuthState.Error("El código debe tener 6 dígitos")
            return
        }

        _authState.value = AuthState.Loading
        Log.d("FirebaseAuth", "Verificando código para email: $email")

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val currentUser = firebaseAuth.currentUser

                    if (currentUser != null && currentUser.email == email) {
                        // Recargar información del usuario para obtener estado más reciente
                        currentUser.reload().await()

                        if (currentUser.isEmailVerified) {
                            Log.d("FirebaseAuth", "Email verificado exitosamente - creando perfil")

                            // Crear perfil de usuario
                            createUserProfile(currentUser, email)

                            // Guardar sesión
                            saveUserSession(currentUser)

                            _isAuthenticated.value = true
                            _authState.value = AuthState.SignedIn
                        } else {
                            Log.w("FirebaseAuth", "Email aún no verificado")
                            _authState.value = AuthState.Error("Email aún no verificado. Por favor, haz clic en el enlace enviado a tu correo e intenta nuevamente.")
                        }
                    } else {
                        Log.e("FirebaseAuth", "No hay usuario actual o el email no coincide")
                        _authState.value = AuthState.Error("Error de verificación. Por favor, intenta registrarte nuevamente.")
                    }
                }
            } catch (e: Exception) {
                Log.e("FirebaseAuth", "Error verificando código", e)
                _authState.value = AuthState.Error("Error en la verificación: ${e.message}")
            }
        }
    }

    /**
     * Verificar automáticamente si el email ya fue verificado
     * Útil para detectar cuando el usuario hizo clic en el enlace de verificación
     */
    fun checkEmailVerificationStatus(email: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val currentUser = firebaseAuth.currentUser

                    if (currentUser != null && currentUser.email == email) {
                        // Recargar información del usuario
                        currentUser.reload().await()

                        if (currentUser.isEmailVerified) {
                            Log.d("FirebaseAuth", "Email verificado automáticamente detectado")

                            // Crear perfil de usuario
                            createUserProfile(currentUser, email)

                            // Guardar sesión
                            saveUserSession(currentUser)

                            _isAuthenticated.value = true
                            _authState.value = AuthState.SignedIn
                        } else {
                            Log.d("FirebaseAuth", "Email aún no verificado en check automático")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FirebaseAuth", "Error en verificación automática", e)
                // No cambiar el estado de error para verificación automática
            }
        }
    }

    /**
     * Guardar información de sesión del usuario
     */
    private suspend fun saveUserSession(user: FirebaseUser) {
        try {
            val userId = user.uid
            val email = user.email ?: ""
            val displayName = user.displayName ?: ""

            // Guardar en SessionManager
            SessionManager.saveUserId(context, userId)
            SessionManager.saveUserEmail(context, email)
            SessionManager.saveUserName(context, displayName)

            // Actualizar UserIdManager
            userIdManager.updateUserIdAfterLogin()

            Log.d("FirebaseAuth", "Sesión guardada - UserId: $userId, Email: $email")
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Error guardando sesión", e)
        }
    }

    /**
     * Crear perfil de usuario en Firestore
     */
    private suspend fun createUserProfile(user: FirebaseUser, email: String) {
        try {
            val currentTime = System.currentTimeMillis()
            val nickname = user.displayName?.takeIf { it.isNotBlank() }
                ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }

            val userProfile = hashMapOf(
                // Identificación básica
                "userId" to user.uid,
                "email" to email,
                "displayName" to (user.displayName ?: ""),
                "fullName" to (user.displayName ?: nickname),
                "nickname" to nickname,
                "photoUrl" to (user.photoUrl?.toString() ?: ""),

                // Estado y configuración
                "description" to "¡Bienvenido/a a BiihLive!",
                "isVerified" to false,
                "mostrarEstado" to true,
                "donacion" to false,
                "tipo" to "PERSONAL",
                "rankingPreference" to "local",
                "totalScore" to 0,

                // Contadores sociales
                "seguidores" to 0,
                "siguiendo" to 0,
                "suscripciones" to 0,
                "suscriptores" to 0,

                // Timestamps
                "createdAt" to currentTime,
                "lastLoginAt" to currentTime,
                "lastUpdated" to currentTime,
                "isOnline" to true,

                // Configuración de suscripciones
                "subscriptionConfig" to hashMapOf(
                    "price" to "9.99",
                    "duration" to "1 mes",
                    "isEnabled" to false,
                    "currency" to "€",
                    "description" to "¡Únete a mi mundo en BiihLive! Suscríbete y no te pierdas ninguna de mis transmisiones en vivo.",
                    "options" to listOf(
                        hashMapOf(
                            "id" to "${nickname.lowercase()}-subscription-001",
                            "displayName" to "Plan Mensual",
                            "price" to "9.99",
                            "duration" to "1 mes",
                            "durationInDays" to 30,
                            "isActive" to true
                        )
                    )
                ),

                // Configuración de patrocinios
                "patrocinioConfig" to hashMapOf(
                    "price" to "19.99",
                    "duration" to "1 mes",
                    "isEnabled" to false,
                    "currency" to "€",
                    "description" to "¡Patrocina mi contenido en BiihLive! Ayúdame a seguir creando y forma parte de mi comunidad exclusiva.",
                    "options" to listOf(
                        hashMapOf(
                            "id" to "${nickname.lowercase()}-patrocinio-001",
                            "displayName" to "Plan Patrocinio Mensual",
                            "price" to "19.99",
                            "duration" to "1 mes",
                            "durationInDays" to 30,
                            "isActive" to true
                        )
                    )
                ),

                // Ubicación (valores por defecto)
                "ubicacion" to hashMapOf(
                    "ciudad" to "",
                    "provincia" to "",
                    "pais" to "España",
                    "countryCode" to "ES",
                    "formattedAddress" to "",
                    "lat" to 0.0,
                    "lng" to 0.0,
                    "placeId" to "",
                    "privacyLevel" to "city"
                )
            )

            firestore.collection("users")
                .document(user.uid)
                .set(userProfile)
                .await()

            // También crear documento en userStats para contadores
            createUserStats(user.uid)

            Log.d("FirebaseAuth", "Perfil completo de usuario creado en Firestore: ${user.uid}")
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Error creando perfil en Firestore", e)
        }
    }

    /**
     * Crear documento userStats para contadores escalables
     */
    private suspend fun createUserStats(userId: String) {
        try {
            val userStats = hashMapOf(
                "followersCount" to 0,
                "followingCount" to 0,
                "totalChats" to 0,
                "unreadChats" to 0,
                "createdAt" to System.currentTimeMillis(),
                "lastChatActivity" to System.currentTimeMillis()
            )

            firestore.collection("userStats")
                .document(userId)
                .set(userStats)
                .await()

            Log.d("FirebaseAuth", "UserStats creado para usuario: $userId")
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Error creando userStats", e)
        }
    }

    // === MÉTODOS DE COMPATIBILIDAD ===

    /**
     * Inicio de sesión con email y contraseña (alias para compatibilidad)
     */
    fun signInWithEmailAndPassword(email: String, password: String) {
        signIn(email, password)
    }

    /**
     * Registro con email y contraseña (alias para compatibilidad)
     */
    fun signUpWithEmailAndPassword(email: String, password: String) {
        signUp(email, password)
    }

    /**
     * Inicio de sesión con Google (usando Activity callback)
     * Esta función debe llamarse desde la UI que tiene acceso al Activity
     */
    fun signInWithGoogle() {
        _authState.value = AuthState.Error("Para usar Google Sign-In, use el método signInWithGoogleFromActivity desde la Activity.")
    }

    /**
     * Inicio de sesión con Google usando One Tap UI
     * Debe llamarse desde la Activity para poder mostrar la UI
     */
    fun signInWithGoogleFromActivity(
        activity: androidx.activity.ComponentActivity,
        onLaunchSignIn: (androidx.activity.result.IntentSenderRequest) -> Unit
    ) {
        if (_authState.value is AuthState.Loading) return

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val googleAuth = com.mision.biihlive.utils.GoogleOneTapAuth(activity)

                // Diagnóstico de configuración
                googleAuth.diagnoseConfiguration()

                googleAuth.beginSignIn(
                    onLaunchIntent = { intentSenderRequest ->
                        Log.d("FirebaseAuth", "Lanzando Google One Tap UI...")
                        onLaunchSignIn(intentSenderRequest)
                    },
                    onNoAccounts = {
                        Log.w("FirebaseAuth", "No hay cuentas Google disponibles")
                        _authState.value = AuthState.Error("No hay cuentas Google disponibles en este dispositivo")
                    }
                )
            } catch (e: Exception) {
                Log.e("FirebaseAuth", "Error iniciando Google Sign-In", e)
                _authState.value = AuthState.Error("Error al iniciar Google Sign-In: ${e.message}")
            }
        }
    }

    /**
     * Manejar el resultado del Google Sign-In
     */
    fun handleGoogleSignInResult(
        activity: androidx.activity.ComponentActivity,
        data: android.content.Intent?,
        resultCode: Int
    ) {
        viewModelScope.launch {
            try {
                val googleAuth = com.mision.biihlive.utils.GoogleOneTapAuth(activity)
                val accountInfo = googleAuth.handleSignInResult(data, resultCode)

                if (accountInfo != null && accountInfo.idToken != null) {
                    Log.d("FirebaseAuth", "Google Account recibido: ${accountInfo.email}")

                    // Usar el token ID para autenticar en Firebase
                    signInWithGoogle(accountInfo.idToken)
                } else {
                    Log.w("FirebaseAuth", "No se pudo obtener token ID de Google")
                    _authState.value = AuthState.Error("Error al procesar la cuenta de Google")
                }
            } catch (e: Exception) {
                Log.e("FirebaseAuth", "Error procesando resultado de Google Sign-In", e)
                _authState.value = AuthState.Error("Error procesando Google Sign-In: ${e.message}")
            }
        }
    }

    /**
     * Enviar email de recuperación de contraseña (alias para compatibilidad)
     */
    fun sendPasswordResetEmail(email: String) {
        resetPassword(email)
    }

    /**
     * Refrescar estado de autenticación
     */
    fun refreshAuthState() {
        checkCurrentAuthStatus()
    }

    /**
     * Resetear estado de autenticación a inicial
     */
    fun resetAuthState() {
        _authState.value = AuthState.Initial
        _error.value = null
    }

    /**
     * Completar el proceso de autenticación después de verificación de email
     * Llamado desde EmailVerificationScreen cuando se completa la verificación
     */
    fun completeEmailVerification() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val currentUser = firebaseAuth.currentUser

                    if (currentUser != null) {
                        Log.d("FirebaseAuth", "Completando verificación de email para: ${currentUser.uid}")

                        // Recargar usuario para obtener estado actual
                        currentUser.reload().await()

                        // Crear perfil de usuario
                        createUserProfile(currentUser, currentUser.email ?: "")

                        // Guardar sesión
                        saveUserSession(currentUser)

                        // TODO: Conectar sistema de presencia cuando esté disponible

                        _isAuthenticated.value = true
                        _authState.value = AuthState.SignedIn

                        Log.d("FirebaseAuth", "Verificación de email completada exitosamente")
                    } else {
                        _authState.value = AuthState.Error("No hay usuario autenticado")
                    }
                }
            } catch (e: Exception) {
                Log.e("FirebaseAuth", "Error completando verificación de email", e)
                _authState.value = AuthState.Error("Error completando verificación: ${e.message}")
            }
        }
    }

    /**
     * Completar Google Sign-In con información de GoogleAccountSelector
     */
    fun completeGoogleSignIn(
        email: String,
        displayName: String,
        photoUrl: String?,
        idToken: String // ← ID token para Firebase Auth
    ) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    Log.d("FirebaseAuth", "Completando Google Sign-In para: $email")

                    // Crear credencial de Google para Firebase Auth
                    val credential = GoogleAuthProvider.getCredential(idToken, null)

                    // Autenticar con Firebase usando el ID token de Google
                    val authResult = firebaseAuth.signInWithCredential(credential).await()
                    val firebaseUser = authResult.user

                    if (firebaseUser != null) {
                        Log.d("FirebaseAuth", "✅ Autenticado en Firebase Auth - UID: ${firebaseUser.uid}")

                        // Verificar si el usuario ya tiene perfil en Firestore
                        val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()

                        if (!userDoc.exists()) {
                            // Usuario nuevo - crear perfil en Firestore con Firebase UID
                            Log.d("FirebaseAuth", "Usuario nuevo, creando perfil con Firebase UID...")
                            createFirebaseUserProfile(firebaseUser.uid, email, displayName, photoUrl)
                        } else {
                            Log.d("FirebaseAuth", "Usuario existente, perfil ya creado")
                        }

                        _isAuthenticated.value = true
                        _authState.value = AuthState.SignedIn
                        Log.d("FirebaseAuth", "Google Sign-In completado exitosamente")
                    } else {
                        throw Exception("Error: Usuario Firebase nulo después de autenticación")
                    }
                }
            } catch (e: Exception) {
                Log.e("FirebaseAuth", "Error completando Google Sign-In", e)
                _authState.value = AuthState.Error("Error en Google Sign-In: ${e.message}")
            }
        }
    }

    /**
     * Crear perfil de usuario en Firestore usando Firebase UID correcto
     */
    private suspend fun createFirebaseUserProfile(
        firebaseUID: String, // ← Firebase Auth UID real
        email: String,
        displayName: String,
        photoUrl: String?
    ) {
        try {
            val currentTime = System.currentTimeMillis()
            val nickname = displayName.takeIf { it.isNotBlank() }
                ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }

            val userProfile = hashMapOf(
                // Identificación básica - USAR FIREBASE UID
                "userId" to firebaseUID, // ← Firebase Auth UID real
                "email" to email,
                "displayName" to displayName,
                "fullName" to displayName,
                "nickname" to nickname,
                "photoUrl" to (photoUrl ?: ""),

                // Estado y configuración
                "description" to "¡Bienvenido/a a BiihLive!",
                "isVerified" to false,
                "mostrarEstado" to true,
                "donacion" to false,
                "tipo" to "PERSONAL",
                "rankingPreference" to "local",
                "totalScore" to 0,

                // Contadores
                "seguidores" to 0,
                "siguiendo" to 0,
                "publicaciones" to 0,
                "puntuacion" to 0,

                // Fechas
                "createdAt" to currentTime,
                "updatedAt" to currentTime,

                // Configuraciones de suscripción y patrocinio
                "subscriptionConfig" to hashMapOf(
                    "price" to "9.99",
                    "duration" to "1 mes",
                    "isEnabled" to false,
                    "currency" to "€",
                    "description" to "¡Únete a mi mundo en Biihlive! Suscríbete y no te pierdas ninguna de mis transmisiones en vivo."
                ),
                "patrocinioConfig" to hashMapOf(
                    "price" to "4.99",
                    "duration" to "1 mes",
                    "isEnabled" to false,
                    "currency" to "€",
                    "description" to "¡Patrocina mi contenido en Biihlive! Tu apoyo me ayuda a crear mejor contenido para la comunidad."
                ),

                // Ubicación opcional
                "ubicacion" to hashMapOf(
                    "pais" to "",
                    "provincia" to "",
                    "ciudad" to ""
                )
            )

            // Crear documento en Firestore usando Firebase UID como ID del documento
            firestore.collection("users").document(firebaseUID).set(userProfile).await()
            Log.d("FirebaseAuth", "✅ Perfil creado en Firestore con Firebase UID: $firebaseUID")

            // Crear userStats con contadores iniciales
            val userStats = hashMapOf(
                "followersCount" to 0,
                "followingCount" to 0,
                "suscripcionesCount" to 0,
                "suscriptoresCount" to 0,
                "createdAt" to currentTime
            )

            firestore.collection("userStats").document(firebaseUID).set(userStats).await()
            Log.d("FirebaseAuth", "✅ UserStats creado para Firebase UID: $firebaseUID")

        } catch (e: Exception) {
            Log.e("FirebaseAuth", "❌ Error creando perfil de usuario", e)
            throw e
        }
    }

    /**
     * Crear perfil de usuario con información de Google (FUNCIÓN LEGACY - DEPRECADA)
     */
    private suspend fun createGoogleUserProfile(
        email: String,
        displayName: String,
        photoUrl: String?
    ) {
        try {
            // Usar email como userId temporal hasta que tengamos un UID real
            val tempUserId = email.replace("@", "_").replace(".", "_")

            val currentTime = System.currentTimeMillis()
            val nickname = displayName.takeIf { it.isNotBlank() }
                ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }

            val userProfile = hashMapOf(
                // Identificación básica
                "userId" to tempUserId,
                "email" to email,
                "displayName" to displayName,
                "fullName" to displayName,
                "nickname" to nickname,
                "photoUrl" to (photoUrl ?: ""),

                // Estado y configuración
                "description" to "¡Bienvenido/a a BiihLive!",
                "isVerified" to false,
                "mostrarEstado" to true,
                "donacion" to false,
                "tipo" to "PERSONAL",
                "rankingPreference" to "local",
                "totalScore" to 0,

                // Contadores sociales
                "seguidores" to 0,
                "siguiendo" to 0,
                "suscripciones" to 0,
                "suscriptores" to 0,

                // Timestamps
                "createdAt" to currentTime,
                "lastLoginAt" to currentTime,
                "lastUpdated" to currentTime,
                "isOnline" to true,

                // Configuración de suscripciones
                "subscriptionConfig" to hashMapOf(
                    "price" to "9.99",
                    "duration" to "1 mes",
                    "isEnabled" to false,
                    "currency" to "€",
                    "description" to "¡Únete a mi mundo en BiihLive! Suscríbete y no te pierdas ninguna de mis transmisiones en vivo."
                ),

                // Configuración de patrocinios
                "patrocinioConfig" to hashMapOf(
                    "price" to "19.99",
                    "duration" to "1 mes",
                    "isEnabled" to false,
                    "currency" to "€",
                    "description" to "¡Patrocina mi contenido en BiihLive! Ayúdame a seguir creando y forma parte de mi comunidad exclusiva."
                ),

                // Ubicación (valores por defecto)
                "ubicacion" to hashMapOf(
                    "ciudad" to "",
                    "provincia" to "",
                    "pais" to "España",
                    "countryCode" to "ES",
                    "formattedAddress" to "",
                    "lat" to 0.0,
                    "lng" to 0.0,
                    "placeId" to "",
                    "privacyLevel" to "city"
                )
            )

            // Guardar en Firestore
            firestore.collection("users")
                .document(tempUserId)
                .set(userProfile)
                .await()

            // También crear documento en userStats
            createUserStats(tempUserId)

            // Guardar sesión local
            SessionManager.saveUserId(context, tempUserId)
            SessionManager.saveUserEmail(context, email)
            SessionManager.saveUserName(context, displayName)
            userIdManager.updateUserIdAfterLogin()

            Log.d("FirebaseAuth", "Perfil de Google creado exitosamente: $tempUserId")
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Error creando perfil de Google", e)
            throw e
        }
    }

    /**
     * Eliminar cuenta del usuario actual
     */
    fun deleteAccount(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            onError("No hay usuario autenticado")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Eliminar usuario de Firebase Auth
                    currentUser.delete().await()
                    Log.d("FirebaseAuth", "Cuenta eliminada exitosamente")

                    // Limpiar sesiones locales
                    SessionManager.clearSession(context)
                    userIdManager.clearCache()

                    // Actualizar estados
                    _isAuthenticated.value = false
                    _authState.value = AuthState.SignedOut
                }

                // Llamar callback de éxito en el hilo principal
                onSuccess()
                Log.d("FirebaseAuth", "Proceso de eliminación de cuenta completado")

            } catch (e: Exception) {
                Log.e("FirebaseAuth", "Error eliminando cuenta", e)
                _authState.value = AuthState.Error("Error al eliminar cuenta: ${e.message}")
                onError("Error al eliminar cuenta: ${e.message}")
            }
        }
    }
}