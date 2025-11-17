package data

import android.content.Context
import android.util.Log
import com.mision.biihlive.data.aws.S3ClientProvider
import domain.models.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class S3PhotoService(private val context: Context) {

    companion object {
        private const val TAG = "S3PhotoService"
        private const val BATCH_SIZE = 10
    }

    private var allPhotos: MutableList<PhotoItem> = mutableListOf()
    private var nextContinuationToken: String? = null
    private var isInitialLoadDone = false

    suspend fun loadInitialPhotos(): List<PhotoItem> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Loading initial photos batch from S3 gallery")

        try {
            // Cargar fotos desde S3 usando el método que ya tenemos
            val result = S3ClientProvider.listAllGalleryImages(
                limit = 30, // Cargar más para tener buffer
                continuationToken = null
            )

            result.fold(
                onSuccess = { galleryResult ->
                    Log.d(TAG, "Successfully loaded ${galleryResult.images.size} images from S3")

                    // Convertir GalleryImage a PhotoItem
                    val photoItems = galleryResult.images.map { galleryImage ->
                        PhotoItem(
                            photoUrl = galleryImage.fullUrl, // Usar URL full para mejor calidad
                            id = galleryImage.imageId,
                            s3Key = galleryImage.key ?: galleryImage.fullUrl.substringAfter(".net/"),
                            lastModified = galleryImage.uploadedAt,
                            description = "Gallery photo",
                            userId = galleryImage.userId ?: "unknown",
                            userName = "User ${(galleryImage.userId ?: "unknown").take(8)}",
                            likes = (50..500).random(),
                            comments = (5..50).random(),
                            shares = (1..20).random()
                        )
                    }

                    allPhotos.clear()
                    allPhotos.addAll(photoItems)
                    nextContinuationToken = galleryResult.nextContinuationToken
                    isInitialLoadDone = true

                    // Retornar solo los primeros BATCH_SIZE
                    return@fold photoItems.take(BATCH_SIZE)
                },
                onFailure = { exception ->
                    Log.e(TAG, "Error loading photos from S3", exception)
                    return@fold emptyList()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading initial photos", e)
            return@withContext emptyList()
        }
    }

    suspend fun loadNextPhotos(): List<PhotoItem> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Loading next photos batch")

        try {
            // Si ya tenemos fotos en buffer, retornarlas
            val currentSize = allPhotos.size
            val nextBatchStartIndex = if (isInitialLoadDone) {
                // Calcular cuántas fotos ya hemos mostrado
                val photosShown = (currentSize / BATCH_SIZE) * BATCH_SIZE
                if (photosShown < currentSize) {
                    // Aún hay fotos en buffer
                    val nextBatch = allPhotos.drop(photosShown).take(BATCH_SIZE)
                    Log.d(TAG, "Returning ${nextBatch.size} photos from buffer")
                    return@withContext nextBatch
                }
                currentSize
            } else {
                0
            }

            // Si no hay más fotos en buffer y hay un token de continuación, cargar más de S3
            if (nextContinuationToken != null) {
                val result = S3ClientProvider.listAllGalleryImages(
                    limit = 30,
                    continuationToken = nextContinuationToken
                )

                result.fold(
                    onSuccess = { galleryResult ->
                        Log.d(TAG, "Loaded ${galleryResult.images.size} more images from S3")

                        val newPhotoItems = galleryResult.images.map { galleryImage ->
                            PhotoItem(
                                photoUrl = galleryImage.fullUrl,
                                id = galleryImage.imageId,
                                s3Key = galleryImage.key ?: galleryImage.fullUrl.substringAfter(".net/"),
                                lastModified = galleryImage.uploadedAt,
                                description = "Gallery photo",
                                userId = galleryImage.userId ?: "unknown",
                                userName = "User ${(galleryImage.userId ?: "unknown").take(8)}",
                                likes = (50..500).random(),
                                comments = (5..50).random(),
                                shares = (1..20).random()
                            )
                        }

                        allPhotos.addAll(newPhotoItems)
                        nextContinuationToken = galleryResult.nextContinuationToken

                        // Retornar los primeros BATCH_SIZE de las nuevas fotos
                        return@fold newPhotoItems.take(BATCH_SIZE)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error loading more photos from S3", exception)
                        return@fold emptyList()
                    }
                )
            } else {
                Log.d(TAG, "No more photos to load from S3")
                // Si no hay más fotos en S3, reiniciar desde el principio (scroll infinito circular)
                if (allPhotos.isNotEmpty()) {
                    Log.d(TAG, "Restarting from beginning for infinite scroll")
                    return@withContext allPhotos.take(BATCH_SIZE)
                }
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading next photos", e)
            return@withContext emptyList()
        }
    }

    fun hasMorePhotos(): Boolean {
        // Siempre hay más fotos para scroll infinito
        return allPhotos.isNotEmpty() || nextContinuationToken != null
    }

    suspend fun shufflePhotos(): List<PhotoItem> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Shuffling all photos")

        if (allPhotos.isEmpty()) {
            // Si no hay fotos, intentar cargar primero
            loadInitialPhotos()
        }

        if (allPhotos.isNotEmpty()) {
            allPhotos.shuffle()
            return@withContext allPhotos.take(BATCH_SIZE)
        }

        return@withContext emptyList()
    }
}