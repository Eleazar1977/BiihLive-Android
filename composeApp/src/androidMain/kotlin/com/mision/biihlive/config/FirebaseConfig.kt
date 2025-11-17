package com.mision.biihlive.config

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * ⚡ CONFIGURACIÓN CENTRALIZADA DE FIREBASE ⚡
 *
 * 🎯 USO: En lugar de usar FirebaseFirestore.getInstance() hardcodeado,
 *         SIEMPRE usar: FirebaseConfig.getFirestore()
 *
 * ✅ BENEFICIOS:
 *   - Una sola configuración para TODA la app
 *   - Automáticamente apunta a base "basebiihlive"
 *   - Sin hardcode en 80 lugares diferentes
 *   - Configuración consistente
 *
 * ❌ NO USAR: FirebaseFirestore.getInstance() (apunta a base default)
 * ✅ USAR:    FirebaseConfig.getFirestore() (apunta a basebiihlive)
 */
object FirebaseConfig {
    private const val TAG = "FIREBASE_CONFIG"
    private var isInitialized = false

    fun initialize(applicationContext: Context) {
        try {
            Log.d(TAG, "🔥🔥🔥 === INICIANDO CONFIGURACIÓN DE FIREBASE === 🔥🔥🔥")

            // Verificar contexto
            Log.d(TAG, "1. Contexto de aplicación: ${applicationContext.javaClass.simpleName}")

            if (isInitialized) {
                Log.d(TAG, "2. ✅ Firebase ya está inicializado, saltando...")
                return
            }

            // Firebase se inicializa automáticamente con google-services.json
            // Solo verificamos que esté disponible
            val firebaseApp = try {
                FirebaseApp.getInstance()
            } catch (e: IllegalStateException) {
                Log.d(TAG, "3. Inicializando Firebase por primera vez...")
                FirebaseApp.initializeApp(applicationContext)
            }

            if (firebaseApp != null) {
                Log.d(TAG, "4. ✅ Firebase App inicializado: ${firebaseApp.name}")

                // Configurar Firestore
                configureFirestore()

                // Verificar Auth
                verifyAuth()

                isInitialized = true
                Log.i(TAG, "5. ✅ FIREBASE INICIALIZADO COMPLETAMENTE")
            } else {
                throw Exception("No se pudo inicializar Firebase")
            }

        } catch (error: Exception) {
            Log.e(TAG, "❌ ERROR AL INICIALIZAR FIREBASE")
            Log.e(TAG, "Error message: ${error.message}")
            Log.e(TAG, "Error class: ${error.javaClass.simpleName}")
            Log.e(TAG, "Stack trace: ${error.stackTraceToString()}")
            throw error
        }
    }

    private lateinit var _firestoreInstance: FirebaseFirestore

    /**
     * Devuelve la instancia de Firestore configurada para "basebiihlive"
     * USAR ESTA FUNCIÓN EN LUGAR DE FirebaseFirestore.getInstance()
     */
    fun getFirestore(): FirebaseFirestore {
        if (::_firestoreInstance.isInitialized) {
            return _firestoreInstance
        }

        // Si no está inicializada, configurarla
        _firestoreInstance = Firebase.firestore(database = "basebiihlive")
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false) // DESHABILITAR CACHE PARA DEBUGGING
            .build()
        _firestoreInstance.firestoreSettings = settings

        Log.e(TAG, "🚨🚨🚨 FIRESTORE CONFIGURADO:")
        Log.e(TAG, "🚨 Database: basebiihlive")
        Log.e(TAG, "🚨 Cache deshabilitado para debugging")
        Log.e(TAG, "🚨 Instancia: ${_firestoreInstance.javaClass.simpleName}")

        return _firestoreInstance
    }

    private fun configureFirestore() {
        try {
            Log.d(TAG, "Configurando Firestore para base: basebiihlive")

            _firestoreInstance = Firebase.firestore(database = "basebiihlive")

            // Configuraciones opcionales de Firestore
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false) // CACHE DESHABILITADO PARA DEBUGGING
                .build()

            _firestoreInstance.firestoreSettings = settings

            Log.d(TAG, "✅ Firestore configurado exitosamente para base: basebiihlive")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error configurando Firestore: ${e.message}")
        }
    }

    private fun verifyAuth() {
        try {
            Log.d(TAG, "Verificando Firebase Auth...")

            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser

            if (currentUser != null) {
                Log.d(TAG, "✅ Usuario autenticado encontrado: ${currentUser.uid}")
            } else {
                Log.d(TAG, "ℹ️ No hay usuario autenticado (esto es normal)")
            }

        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error verificando Auth: ${e.message}")
        }
    }

    fun isFirebaseInitialized(): Boolean = isInitialized
}