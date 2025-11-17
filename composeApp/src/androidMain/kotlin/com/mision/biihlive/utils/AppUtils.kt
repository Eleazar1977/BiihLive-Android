package com.mision.biihlive.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

// Variable global para el contexto de la aplicación
private var appContext: Context? = null

/**
 * Inicializa el contexto de la aplicación
 */
fun initializeAppContext(context: Context) {
    appContext = context.applicationContext
}

/**
 * Abre Google Play Store en la página de la aplicación especificada
 */
actual fun openPlayStore(packageName: String) {
    val context = appContext ?: return
    
    try {
        // Intentar abrir con la app de Google Play Store
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        // Si falla, abrir en navegador web
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Si todo falla, no hacer nada
        }
    }
}

/**
 * Obtiene el versionCode actual de la aplicación
 */
actual fun getCurrentVersionCode(): Int {
    val context = appContext ?: return 1
    
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
    } catch (e: Exception) {
        1 // Valor por defecto
    }
}