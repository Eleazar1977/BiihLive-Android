package com.mision.biihlive.utils

/**
 * Obtiene el versionCode actual de la aplicación
 */
expect fun getCurrentVersionCode(): Int

/**
 * Abre Google Play Store en la página de la aplicación especificada
 */
expect fun openPlayStore(packageName: String)