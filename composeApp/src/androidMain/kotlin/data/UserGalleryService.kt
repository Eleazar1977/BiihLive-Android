package data

import android.content.Context
import android.util.Log
import com.mision.biihlive.data.aws.S3ClientProvider
import domain.models.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserGalleryService(private val context: Context) {

    companion object {
        private const val TAG = "UserGalleryService"
        private const val BATCH_SIZE = 15 // Mismo tamaño que PhotoFeed
    }

    private var allPhotos: MutableList<PhotoItem> = mutableListOf()
    private var bufferPhotos: MutableList<PhotoItem> = mutableListOf()
    private var nextContinuationToken: String? = null
    private var isInitialLoadDone = false
    private var isPreloadingBuffer = false
    private var targetUserId: String = ""

    suspend fun loadInitialPhotos(userId: String): List<PhotoItem> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Loading initial photos for user: $userId")

        // Reiniciar estado si cambiamos de usuario
        if (targetUserId != userId) {
            targetUserId = userId
            allPhotos.clear()
            bufferPhotos.clear()
            nextContinuationToken = null
            isInitialLoadDone = false
            isPreloadingBuffer = false
        }

        try {
            // Cargar fotos del usuario específico usando S3ClientProvider
            val result = S3ClientProvider.listUserGalleryImages(
                userId = userId,
                limit = 60, // Buffer agresivo: 4x el batch size para eliminar delays
                continuationToken = null
            )

            result.fold(
                onSuccess = { galleryResult ->
                    Log.d(TAG, "Successfully loaded ${galleryResult.images.size} images for user $userId")

                    // Convertir GalleryImage a PhotoItem
                    val photoItems = galleryResult.images.map { galleryImage ->
                        PhotoItem(
                            photoUrl = galleryImage.fullUrl,
                            id = galleryImage.imageId,
                            s3Key = galleryImage.key ?: galleryImage.fullUrl.substringAfter(".net/"),
                            lastModified = galleryImage.uploadedAt,
                            description = "Gallery photo",
                            userId = userId,
                            userName = "User ${userId.take(8)}",
                            likes = (50..500).random(),
                            comments = (5..50).random(),
                            shares = (1..20).random()
                        )
                    }

                    allPhotos.clear()
                    allPhotos.addAll(photoItems)
                    nextContinuationToken = galleryResult.nextContinuationToken
                    isInitialLoadDone = true

                    // Llenar buffer secundario inmediatamente para eliminar delays futuros
                    fillSecondaryBuffer()

                    // Retornar solo los primeros BATCH_SIZE
                    return@fold photoItems.take(BATCH_SIZE)
                },
                onFailure = { exception ->
                    Log.e(TAG, "Error loading photos for user $userId", exception)
                    return@fold emptyList()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading initial photos for user $userId", e)
            return@withContext emptyList()
        }
    }

    suspend fun loadNextPhotos(): List<PhotoItem> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Loading next photos batch for user $targetUserId")

        if (targetUserId.isEmpty()) {
            Log.w(TAG, "No target user set, cannot load photos")
            return@withContext emptyList()
        }

        try {
            // DOBLE BUFFER: Priorizar buffer secundario si está disponible
            if (bufferPhotos.isNotEmpty()) {
                val nextBatch = bufferPhotos.take(BATCH_SIZE)
                Log.d(TAG, "⚡ FAST: Returning ${nextBatch.size} photos from SECONDARY BUFFER")

                // Mover buffer secundario al principal
                allPhotos.addAll(bufferPhotos)
                bufferPhotos.clear()

                // Rellenar buffer secundario inmediatamente para el próximo batch
                fillSecondaryBuffer()

                return@withContext nextBatch
            }

            // Si ya tenemos fotos en buffer principal, retornarlas
            val currentSize = allPhotos.size
            val nextBatchStartIndex = if (isInitialLoadDone) {
                val photosShown = (currentSize / BATCH_SIZE) * BATCH_SIZE
                if (photosShown < currentSize) {
                    val nextBatch = allPhotos.drop(photosShown).take(BATCH_SIZE)
                    Log.d(TAG, "Returning ${nextBatch.size} photos from primary buffer")

                    if (bufferPhotos.isEmpty()) {
                        fillSecondaryBuffer()
                    }

                    return@withContext nextBatch
                }
                currentSize
            } else {
                0
            }

            // Si no hay más fotos en buffer y hay un token de continuación, cargar más de S3
            if (nextContinuationToken != null) {
                val result = S3ClientProvider.listUserGalleryImages(
                    userId = targetUserId,
                    limit = 45, // Buffer intermedio agresivo
                    continuationToken = nextContinuationToken
                )

                result.fold(
                    onSuccess = { galleryResult ->
                        Log.d(TAG, "Loaded ${galleryResult.images.size} more images for user $targetUserId")

                        val newPhotoItems = galleryResult.images.map { galleryImage ->
                            PhotoItem(
                                photoUrl = galleryImage.fullUrl,
                                id = galleryImage.imageId,
                                s3Key = galleryImage.key ?: galleryImage.fullUrl.substringAfter(".net/"),
                                lastModified = galleryImage.uploadedAt,
                                description = "Gallery photo",
                                userId = targetUserId,
                                userName = "User ${targetUserId.take(8)}",
                                likes = (50..500).random(),
                                comments = (5..50).random(),
                                shares = (1..20).random()
                            )
                        }

                        allPhotos.addAll(newPhotoItems)
                        nextContinuationToken = galleryResult.nextContinuationToken

                        // Rellenar buffer secundario inmediatamente con datos restantes
                        if (newPhotoItems.size > BATCH_SIZE) {
                            bufferPhotos.clear()
                            bufferPhotos.addAll(newPhotoItems.drop(BATCH_SIZE))
                            Log.d(TAG, "✅ Filled secondary buffer with ${bufferPhotos.size} photos")
                        } else {
                            fillSecondaryBuffer()
                        }

                        // Retornar los primeros BATCH_SIZE de las nuevas fotos
                        return@fold newPhotoItems.take(BATCH_SIZE)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error loading more photos for user $targetUserId", exception)
                        return@fold emptyList()
                    }
                )
            } else {
                Log.d(TAG, "No more photos to load for user $targetUserId")
                // Si no hay más fotos en S3, reiniciar desde el principio (scroll infinito circular)
                if (allPhotos.isNotEmpty()) {
                    Log.d(TAG, "Restarting from beginning for infinite scroll")
                    return@withContext allPhotos.take(BATCH_SIZE)
                }
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading next photos for user $targetUserId", e)
            return@withContext emptyList()
        }
    }

    fun hasMorePhotos(): Boolean {
        return allPhotos.isNotEmpty() || bufferPhotos.isNotEmpty() || nextContinuationToken != null
    }

    suspend fun shufflePhotos(): List<PhotoItem> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Shuffling all photos for user $targetUserId")

        if (allPhotos.isEmpty() && targetUserId.isNotEmpty()) {
            loadInitialPhotos(targetUserId)
        }

        if (allPhotos.isNotEmpty()) {
            allPhotos.shuffle()
            return@withContext allPhotos.take(BATCH_SIZE)
        }

        return@withContext emptyList()
    }

    /**
     * Llena el buffer secundario en background para eliminar delays
     * Sistema de doble buffer inspirado en ProyectoBase
     */
    private suspend fun fillSecondaryBuffer() {
        if (isPreloadingBuffer || nextContinuationToken == null || targetUserId.isEmpty()) {
            return
        }

        Log.d(TAG, "🔄 Filling secondary buffer proactively for user $targetUserId...")
        isPreloadingBuffer = true

        try {
            val result = S3ClientProvider.listUserGalleryImages(
                userId = targetUserId,
                limit = 30, // Buffer secundario mediano
                continuationToken = nextContinuationToken
            )

            result.fold(
                onSuccess = { galleryResult ->
                    val newPhotoItems = galleryResult.images.map { galleryImage ->
                        PhotoItem(
                            photoUrl = galleryImage.fullUrl,
                            id = galleryImage.imageId,
                            s3Key = galleryImage.key ?: galleryImage.fullUrl.substringAfter(".net/"),
                            lastModified = galleryImage.uploadedAt,
                            description = "Gallery photo",
                            userId = targetUserId,
                            userName = "User ${targetUserId.take(8)}",
                            likes = (50..500).random(),
                            comments = (5..50).random(),
                            shares = (1..20).random()
                        )
                    }

                    bufferPhotos.clear()
                    bufferPhotos.addAll(newPhotoItems)
                    nextContinuationToken = galleryResult.nextContinuationToken

                    Log.d(TAG, "✅ Secondary buffer filled with ${newPhotoItems.size} photos for user $targetUserId")
                },
                onFailure = { exception ->
                    Log.w(TAG, "Warning: Failed to fill secondary buffer for user $targetUserId", exception)
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Warning: Error filling secondary buffer for user $targetUserId", e)
        } finally {
            isPreloadingBuffer = false
        }
    }
}