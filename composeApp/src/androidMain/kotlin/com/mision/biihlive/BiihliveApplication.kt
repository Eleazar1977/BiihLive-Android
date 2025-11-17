package com.mision.biihlive

import android.app.Application
import android.util.Log
import com.mision.biihlive.config.FirebaseConfig
import com.mision.biihlive.data.aws.S3ClientProvider
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import com.mision.biihlive.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BiihliveApplication : Application() {

    override fun onCreate() {
        Log.e("BIIHLIVE_APP", "🚀🚀🚀 === INICIANDO APLICACIÓN BIIHLIVE === 🚀🚀🚀")
        super.onCreate()

        try {
            Log.e("BIIHLIVE_APP", "🔥 Inicializando Firebase...")
            FirebaseConfig.initialize(this)
            Log.e("BIIHLIVE_APP", "✅ Firebase inicializado exitosamente")
        } catch (e: Exception) {
            Log.e("BIIHLIVE_APP", "❌ Error inicializando Firebase: ${e.message}", e)
            e.printStackTrace()
        }

        // Inicializar S3ClientProvider para upload de imágenes
        try {
            Log.e("BIIHLIVE_APP", "☁️ Inicializando S3ClientProvider...")
            CoroutineScope(Dispatchers.IO).launch {
                S3ClientProvider.initialize(this@BiihliveApplication)
                Log.e("BIIHLIVE_APP", "✅ S3ClientProvider inicializado exitosamente")
            }
        } catch (e: Exception) {
            Log.e("BIIHLIVE_APP", "❌ Error inicializando S3ClientProvider: ${e.message}", e)
            e.printStackTrace()
        }

        // Configurar Coil globalmente para optimizar manejo de imágenes
        try {
            Log.e("BIIHLIVE_APP", "📸 Configurando Coil ImageLoader...")
            configureCoil()
            Log.e("BIIHLIVE_APP", "✅ Coil configurado exitosamente")
        } catch (e: Exception) {
            Log.e("BIIHLIVE_APP", "❌ Error configurando Coil: ${e.message}", e)
        }

        Log.e("BIIHLIVE_APP", "🏁🏁🏁 === APLICACIÓN BIIHLIVE INICIADA === 🏁🏁🏁")
    }

    private fun configureCoil() {
        val imageLoader = ImageLoader.Builder(this)
            // Configurar cliente HTTP con timeouts apropiados
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    // No añadir caché HTTP para evitar problemas con CloudFront
                    .cache(null)
                    .build()
            }
            // Configurar caché de memoria - AUMENTADO para mejor rendimiento en listas
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 25% de memoria disponible (más razonable)
                    .strongReferencesEnabled(true) // Mantener referencias fuertes
                    .build()
            }
            // Configurar caché de disco - AUMENTADO para almacenar más imágenes
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(200 * 1024 * 1024) // 200 MB para mejor cache de thumbnails
                    .build()
            }
            // Configurar políticas de caché por defecto
            .respectCacheHeaders(true) // Respetar headers de CloudFront
            .crossfade(200) // Animación más rápida (200ms)
            .placeholder(R.drawable.ic_default_avatar) // Placeholder mientras carga
            .error(R.drawable.ic_default_avatar) // Imagen de error
            .fallback(R.drawable.ic_default_avatar) // Fallback si URL es null
            .build()

        // Establecer como el ImageLoader por defecto de Coil
        Coil.setImageLoader(imageLoader)

        Log.d("BIIHLIVE_APP", "Coil configurado con caché reducido y sin caché HTTP")
    }
}