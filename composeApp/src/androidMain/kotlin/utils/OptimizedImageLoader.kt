@file:OptIn(coil.annotation.ExperimentalCoilApi::class)

package utils

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Configuración optimizada de ImageLoader para BiihLive
 *
 * Configurado específicamente para:
 * - Feed de fotos con scroll rápido
 * - Imágenes S3 + CloudFront CDN
 * - Mejor performance que configuración automática
 *
 * Optimizaciones implementadas:
 * - Cache de memoria: 25% RAM disponible
 * - Cache de disco: 2% almacenamiento disponible
 * - Timeouts agresivos para conexión
 * - Crossfade optimizado para UX fluida
 */
object OptimizedImageLoader {

    /**
     * Crear ImageLoader optimizado para BiihLive
     */
    fun create(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    // 25% de memoria RAM disponible para cache de imágenes
                    .maxSizePercent(0.25)
                    // Cache hasta 50 bitmaps grandes en memoria
                    .maxSizeBytes(50 * 1024 * 1024) // 50MB máximo
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("biih_image_cache"))
                    // 2% del almacenamiento disponible para cache de disco
                    .maxSizePercent(0.02)
                    // Máximo 200MB en disco
                    .maxSizeBytes(200 * 1024 * 1024) // 200MB máximo
                    .build()
            }
            .okHttpClient {
                // Cliente HTTP optimizado para S3/CloudFront
                OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)     // Conexión rápida (era 30s)
                    .readTimeout(10, TimeUnit.SECONDS)       // Lectura optimizada (era 30s)
                    .writeTimeout(10, TimeUnit.SECONDS)      // Escritura optimizada
                    .retryOnConnectionFailure(true)          // Retry automático
                    .build()
            }
            // Políticas de cache agresivas
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            // Crossfade suave pero rápido
            .crossfade(300) // 300ms para UX fluida
            // Respectar cabeceras HTTP de cache
            .respectCacheHeaders(false) // Ignorar headers para mejor cache
            .build()
    }

    /**
     * ImageLoader específico para thumbnails pequeños
     * Optimizado para listas con muchos elementos
     */
    fun createForThumbnails(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    // Cache más agresivo para thumbnails
                    .maxSizePercent(0.15) // 15% memoria para thumbnails
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("biih_thumbnails_cache"))
                    .maxSizePercent(0.01) // 1% disco para thumbnails
                    .build()
            }
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(3, TimeUnit.SECONDS)     // Aún más rápido para thumbnails
                    .readTimeout(5, TimeUnit.SECONDS)
                    .writeTimeout(5, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build()
            }
            .crossfade(200) // Crossfade más rápido para thumbnails
            .respectCacheHeaders(false)
            .build()
    }

    /**
     * Limpiar caches manualmente si es necesario
     */
    fun clearCache(context: Context) {
        val imageLoader = create(context)
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }

    /**
     * Configuraciones específicas para diferentes tamaños
     */
    object ImageSizes {
        // Para feed de fotos fullscreen
        const val FULL_IMAGE_WIDTH = 1920
        const val FULL_IMAGE_HEIGHT = 1920

        // Para thumbnails en listas
        const val THUMBNAIL_WIDTH = 150
        const val THUMBNAIL_HEIGHT = 150

        // Para avatares de perfil
        const val AVATAR_WIDTH = 100
        const val AVATAR_HEIGHT = 100
    }
}