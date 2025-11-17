package com.mision.biihlive.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.mision.biihlive.utils.ImageProcessor
import com.mision.biihlive.data.aws.S3ClientProvider
import com.mision.biihlive.config.AWSConfig
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Repository para manejar la subida de imágenes de perfil a AWS S3.
 * Utiliza S3ClientProvider para subir imágenes directamente a S3.
 */
class ProfileImageRepository(
    private val context: Context
) {
    companion object {
        private const val TAG = "ProfileImageRepository"
    }

    private val imageProcessor = ImageProcessor(context)

    data class ProfileImageData(
        val fullImageUrl: String = "",
        val thumbnailUrl: String = ""
    )

    /**
     * Sube una imagen de perfil a S3.
     *
     * @param uri URI de la imagen seleccionada
     * @param userId ID del usuario
     * @return Flow con el resultado de la operación
     */
    suspend fun uploadProfileImage(
        uri: Uri,
        userId: String
    ): Flow<Result<ProfileImageData>> = flow {
        try {
            Log.d(TAG, "[INICIANDO] Procesamiento de imagen para usuario: $userId")

            // Verificar que S3ClientProvider esté inicializado
            if (!S3ClientProvider.isInitialized()) {
                Log.d(TAG, "[INFO] Inicializando S3ClientProvider...")
                S3ClientProvider.initialize(context)
            }

            // Procesar la imagen
            Log.d(TAG, "[PROCESANDO] Procesando imagen...")
            val processResult = imageProcessor.processImage(uri)
            if (processResult.isFailure) {
                Log.e(TAG, "[ERROR] Fallo al procesar imagen: ${processResult.exceptionOrNull()?.message}")
                emit(Result.failure(processResult.exceptionOrNull()!!))
                return@flow
            }

            val processedImages = processResult.getOrThrow()
            Log.d(TAG, "[OK] Imagen procesada. Full: ${processedImages.fullImageBytes.size} bytes, Thumbnail: ${processedImages.thumbnailBytes.size} bytes")

            // Subir imágenes a S3 usando S3ClientProvider
            Log.d(TAG, "[SUBIENDO] Subiendo imágenes a S3...")
            val uploadResult = S3ClientProvider.uploadProfileImages(
                userId = userId,
                fullImageData = processedImages.fullImageBytes,
                thumbnailData = processedImages.thumbnailBytes
            )

            if (uploadResult.isFailure) {
                Log.e(TAG, "[ERROR] Fallo al subir imágenes a S3: ${uploadResult.exceptionOrNull()?.message}")
                emit(Result.failure(uploadResult.exceptionOrNull()!!))
                return@flow
            }

            val (fullUrl, thumbnailUrl) = uploadResult.getOrThrow()
            Log.d(TAG, "[OK] Imágenes subidas exitosamente.")
            Log.d(TAG, "  - Full URL: $fullUrl")
            Log.d(TAG, "  - Thumbnail URL: $thumbnailUrl")
            Log.d(TAG, "  - User ID: $userId")


            // Invalidar caché local de Coil
            invalidateLocalCache(userId)

            // Añadir timestamp a las URLs para evitar caché
            val timestamp = System.currentTimeMillis()
            val fullUrlWithTimestamp = "$fullUrl?v=$timestamp"
            val thumbnailUrlWithTimestamp = "$thumbnailUrl?v=$timestamp"

            Log.d(TAG, "[URLS] Generando URLs con timestamp para bypass de caché:")
            Log.d(TAG, "  - Full con timestamp: $fullUrlWithTimestamp")
            Log.d(TAG, "  - Thumbnail con timestamp: $thumbnailUrlWithTimestamp")

            val profileImageData = ProfileImageData(
                fullImageUrl = fullUrlWithTimestamp,
                thumbnailUrl = thumbnailUrlWithTimestamp
            )

            Log.d(TAG, "[COMPLETADO] ✅ Proceso completado exitosamente")
            Log.d(TAG, "  - Total tiempo: ${System.currentTimeMillis() - timestamp}ms")
            emit(Result.success(profileImageData))

        } catch (e: Exception) {
            Log.e(TAG, "[ERROR] Error general en uploadProfileImage: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Invalida el caché local de Coil para las imágenes del usuario
     */
    private suspend fun invalidateLocalCache(userId: String) = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "[CACHE LOCAL] Iniciando limpieza de caché local Coil...")
            Log.d(TAG, "  - User ID: $userId")

            val imageLoader = ImageLoader.Builder(context)
                .respectCacheHeaders(false)
                .build()

            // URLs que necesitamos limpiar del caché
            val thumbnailUrl = "${S3ClientProvider.CLOUDFRONT_URL}/userprofile/$userId/thumbnail.png"
            val fullUrl = "${S3ClientProvider.CLOUDFRONT_URL}/userprofile/$userId/full.png"

            Log.d(TAG, "[CACHE LOCAL] URLs a limpiar:")
            Log.d(TAG, "  - Thumbnail: $thumbnailUrl")
            Log.d(TAG, "  - Full: $fullUrl")

            // Limpiar TODO el caché como hace proyectobase (más efectivo)
            val diskCacheCleared = imageLoader.diskCache?.clear()
            val memoryCacheCleared = imageLoader.memoryCache?.clear()

            Log.d(TAG, "[CACHE LOCAL] Limpieza completada:")
            Log.d(TAG, "  - Disk cache limpiado: ${diskCacheCleared != null}")
            Log.d(TAG, "  - Memory cache limpiado: ${memoryCacheCleared != null}")

            // No es necesario duplicarlo aquí

            Log.d(TAG, "[CACHE LOCAL] ✅ Caché local limpiado correctamente")

        } catch (e: Exception) {
            Log.e(TAG, "[CACHE LOCAL] ⚠️ Error invalidando caché: ${e.message}")
            Log.e(TAG, "  - Stack: ", e)
            // No es crítico, continuar
        }
    }
}