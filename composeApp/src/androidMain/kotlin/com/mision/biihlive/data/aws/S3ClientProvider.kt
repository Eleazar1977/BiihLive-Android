package com.mision.biihlive.data.aws

import android.content.Context
import android.util.Log
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.ListObjectsV2Request
import com.amazonaws.ClientConfiguration
import com.amazonaws.retry.PredefinedRetryPolicies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Proveedor del cliente S3 para Biihlive
 * Gestiona la conexión con AWS S3 para upload de imágenes
 */
object S3ClientProvider {
    private const val TAG = "S3ClientProvider"

    // Identity Pool ID para credenciales temporales de S3 (solo para acceso a S3, no autenticación de usuario)
    // La autenticación de usuario se hace con Firebase Auth
    // Región: eu-west-3
    // Creado: 2025-09-24
    private const val COGNITO_POOL_ID = "eu-west-3:93df5af8-4cf5-4520-b868-cb586153655f"

    // Configuración del bucket de Biihlive
    const val BUCKET_NAME = "biihlivemedia"
    const val BASE_S3_URL = "https://biihlivemedia.s3.eu-west-3.amazonaws.com"
    const val CLOUDFRONT_URL = "https://d183hg75gdabnr.cloudfront.net"

    @Volatile
    private var instance: AmazonS3Client? = null

    /**
     * Estructura de carpetas en S3:
     * userprofile/{userId}/full_{timestamp}.png - Imagen completa perfil (1024x1024)
     * userprofile/{userId}/thumbnail_{timestamp}.png - Thumbnail perfil (150x150)
     * gallery/{userId}/full_{imageId}.png - Imagen completa galería (1920x1920)
     * gallery/{userId}/thumbnail_{imageId}.png - Thumbnail galería (300x300)
     * gallery/{userId}/metadata_{imageId}.json - Metadata de la imagen (opcional)
     */
    object Paths {
        object UserProfile {
            fun getFullPath(userId: String, timestamp: Long) = "userprofile/$userId/full_$timestamp.png"
            fun getThumbnailPath(userId: String, timestamp: Long) = "userprofile/$userId/thumbnail_$timestamp.png"

            // Para obtener la imagen más reciente (sin timestamp específico)
            fun getFullPath(userId: String) = "userprofile/$userId/full_*.png"
            fun getThumbnailPath(userId: String) = "userprofile/$userId/thumbnail_*.png"
        }

        object Gallery {
            fun getFullPath(userId: String, imageId: String) = "gallery/$userId/full_$imageId.png"
            fun getThumbnailPath(userId: String, imageId: String) = "gallery/$userId/thumbnail_$imageId.png"
            fun getMetadataPath(userId: String, imageId: String) = "gallery/$userId/metadata_$imageId.json"
        }
    }

    /**
     * Inicializa el cliente S3 con credenciales temporales de AWS
     * (Cognito Identity Pool solo para S3, no para autenticación de usuario)
     * Debe llamarse una vez al inicio de la app
     */
    fun initialize(context: Context) {
        if (instance == null) {
            synchronized(this) {
                if (instance == null) {
                    try {
                        val credentials = CognitoCachingCredentialsProvider(
                            context.applicationContext,
                            COGNITO_POOL_ID,
                            Regions.EU_WEST_3
                        )

                        // Configuración con timeouts más largos y retry policy
                        val clientConfig = ClientConfiguration().apply {
                            connectionTimeout = 30000 // 30 segundos
                            socketTimeout = 30000 // 30 segundos
                            maxErrorRetry = 3 // Reintentar hasta 3 veces
                            retryPolicy = PredefinedRetryPolicies.DEFAULT // Policy con backoff exponencial
                        }

                        instance = AmazonS3Client(credentials, clientConfig).apply {
                            setRegion(Region.getRegion(Regions.EU_WEST_3))
                        }

                        Log.d(TAG, "Cliente S3 inicializado correctamente para Biihlive")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al inicializar el cliente S3", e)
                        throw e
                    }
                }
            }
        }
    }

    /**
     * Obtiene la instancia del cliente S3
     * @throws IllegalStateException si no se ha inicializado
     */
    fun getInstance(): AmazonS3Client {
        return instance ?: throw IllegalStateException(
            "S3Client no inicializado. Asegúrate de llamar a initialize() primero."
        )
    }

    /**
     * Sube una imagen a S3
     * @param s3Key La clave/path en S3 (ej: "userprofile/userId/full.png")
     * @param imageData Los bytes de la imagen
     * @param contentType El tipo MIME (ej: "image/png")
     */
    suspend fun uploadImage(
        s3Key: String,
        imageData: ByteArray,
        contentType: String = "image/png"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando subida de imagen a S3: $s3Key")

            val metadata = ObjectMetadata().apply {
                this.contentType = contentType
                contentLength = imageData.size.toLong()
                // Headers HTTP para control de caché
                cacheControl = "max-age=0, no-cache, no-store, must-revalidate"
                // Establecer tiempo de expiración inmediato
                httpExpiresDate = java.util.Date(System.currentTimeMillis())
            }

            val inputStream = ByteArrayInputStream(imageData)

            val putObjectRequest = PutObjectRequest(
                BUCKET_NAME,
                s3Key,
                inputStream,
                metadata
            )

            getInstance().putObject(putObjectRequest)

            val imageUrl = "$CLOUDFRONT_URL/$s3Key"
            Log.d(TAG, "Imagen subida exitosamente. URL: $imageUrl")

            Result.success(imageUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error al subir imagen a S3", e)
            Result.failure(e)
        }
    }

    /**
     * Sube la foto de perfil de un usuario (full y thumbnail)
     * @param userId ID del usuario (Firebase UID)
     * @param fullImageData Imagen completa (1024x1024)
     * @param thumbnailData Thumbnail (150x150)
     */
    suspend fun uploadProfileImages(
        userId: String,
        fullImageData: ByteArray,
        thumbnailData: ByteArray
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Subiendo imágenes de perfil para usuario: $userId")

            // Generar timestamp para nombres únicos
            val timestamp = System.currentTimeMillis()

            // Subir imagen completa con timestamp
            val fullPath = Paths.UserProfile.getFullPath(userId, timestamp)
            val fullResult = uploadImage(fullPath, fullImageData)

            if (fullResult.isFailure) {
                return@withContext Result.failure(
                    fullResult.exceptionOrNull() ?: Exception("Error subiendo imagen completa")
                )
            }

            // Subir thumbnail con el mismo timestamp
            val thumbnailPath = Paths.UserProfile.getThumbnailPath(userId, timestamp)
            val thumbnailResult = uploadImage(thumbnailPath, thumbnailData)

            if (thumbnailResult.isFailure) {
                return@withContext Result.failure(
                    thumbnailResult.exceptionOrNull() ?: Exception("Error subiendo thumbnail")
                )
            }

            val fullUrl = fullResult.getOrNull()!!
            val thumbnailUrl = thumbnailResult.getOrNull()!!

            Log.d(TAG, "Ambas imágenes subidas exitosamente")
            Result.success(Pair(fullUrl, thumbnailUrl))
        } catch (e: Exception) {
            Log.e(TAG, "Error al subir imágenes de perfil", e)
            Result.failure(e)
        }
    }

    /**
     * Verifica si el cliente está inicializado
     */
    fun isInitialized(): Boolean = instance != null

    /**
     * Obtiene la imagen de perfil más reciente de un usuario
     * @return Par de URLs (full, thumbnail) o null si no hay imágenes
     */
    suspend fun getMostRecentProfileImage(userId: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Buscando imagen más reciente para usuario: $userId")

            // Listar objetos en el prefijo del usuario con configuración específica
            val listRequest = ListObjectsV2Request()
                .withBucketName(BUCKET_NAME)
                .withPrefix("userprofile/$userId/")
                .withMaxKeys(10) // Limitar a 10 objetos máximo para evitar timeouts

            val result = try {
                getInstance().listObjectsV2(listRequest)
            } catch (e: com.amazonaws.AmazonClientException) {
                Log.w(TAG, "Timeout o error de conexión al listar imágenes, usando URLs por defecto", e)
                // Devolver URLs por defecto basadas en el patrón esperado
                // Esto evita que falle toda la carga del perfil por un timeout
                return@withContext null
            }

            val objects = result.objectSummaries

            Log.d(TAG, "Objetos encontrados en S3 para $userId: ${objects.size}")
            objects.forEach { obj ->
                Log.d(TAG, "  - ${obj.key} (tamaño: ${obj.size})")
            }

            if (objects.isEmpty()) {
                Log.d(TAG, "No hay imágenes de perfil para usuario $userId")
                return@withContext null
            }

            // Separar full y thumbnails
            val fullImages = objects.filter { it.key.contains("/full_") }
            val thumbnailImages = objects.filter { it.key.contains("/thumbnail_") }

            Log.d(TAG, "Imágenes full encontradas: ${fullImages.size}")
            Log.d(TAG, "Imágenes thumbnail encontradas: ${thumbnailImages.size}")

            // Obtener la más reciente de cada tipo (por nombre, que incluye timestamp)
            val mostRecentFull = fullImages.maxByOrNull { it.key }
            val mostRecentThumbnail = thumbnailImages.maxByOrNull { it.key }

            if (mostRecentFull != null && mostRecentThumbnail != null) {
                val fullUrl = "$CLOUDFRONT_URL/${mostRecentFull.key}"
                val thumbnailUrl = "$CLOUDFRONT_URL/${mostRecentThumbnail.key}"
                Log.d(TAG, "Imagen más reciente encontrada:")
                Log.d(TAG, "  - Full: ${mostRecentFull.key}")
                Log.d(TAG, "  - Thumbnail: ${mostRecentThumbnail.key}")
                Log.d(TAG, "URLs generadas:")
                Log.d(TAG, "  - Full URL: $fullUrl")
                Log.d(TAG, "  - Thumbnail URL: $thumbnailUrl")
                Pair(fullUrl, thumbnailUrl)
            } else {
                Log.d(TAG, "No se encontraron pares completos de imágenes")
                Log.d(TAG, "  - Full images: $fullImages")
                Log.d(TAG, "  - Thumbnail images: $thumbnailImages")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo imagen más reciente", e)
            Log.e(TAG, "Stack trace:", e)
            null
        }
    }

    /**
     * Sube una imagen de galería con UUID único (full y thumbnail)
     * NO guarda en base de datos, solo sube al bucket
     * @param userId ID del usuario (Firebase UID)
     * @param fullImageData Imagen completa procesada (1920x1920)
     * @param thumbnailData Thumbnail procesado (300x300)
     * @param metadata Metadata opcional para la imagen (caption, tags, etc)
     * @return Result con Pair(imageId, CloudFront URLs)
     */
    suspend fun uploadGalleryImages(
        userId: String,
        fullImageData: ByteArray,
        thumbnailData: ByteArray,
        metadata: Map<String, Any>? = null
    ): Result<Triple<String, String, String>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando subida de imagen de galería para usuario: $userId")

            // Generar ID único con timestamp para orden cronológico
            val timestamp = System.currentTimeMillis()
            val uuid = java.util.UUID.randomUUID().toString().take(8) // Solo primeros 8 caracteres del UUID
            val imageId = "${timestamp}_${uuid}"
            Log.d(TAG, "ID generado para imagen: $imageId (timestamp: $timestamp)")

            // Subir imagen completa
            val fullPath = Paths.Gallery.getFullPath(userId, imageId)
            val fullResult = uploadImage(fullPath, fullImageData)

            if (fullResult.isFailure) {
                return@withContext Result.failure(
                    fullResult.exceptionOrNull() ?: Exception("Error subiendo imagen completa de galería")
                )
            }

            // Subir thumbnail
            val thumbnailPath = Paths.Gallery.getThumbnailPath(userId, imageId)
            val thumbnailResult = uploadImage(thumbnailPath, thumbnailData)

            if (thumbnailResult.isFailure) {
                return@withContext Result.failure(
                    thumbnailResult.exceptionOrNull() ?: Exception("Error subiendo thumbnail de galería")
                )
            }

            // Metadata es opcional - si se proporciona se sube con la misma estructura simplificada
            if (metadata != null) {
                val metadataPath = Paths.Gallery.getMetadataPath(userId, imageId)
                val metadataWithTimestamp = metadata.toMutableMap().apply {
                    put("uploadedAt", System.currentTimeMillis())
                    put("imageId", imageId)
                    put("userId", userId)
                }

                val metadataJson = org.json.JSONObject(metadataWithTimestamp).toString()
                val metadataBytes = metadataJson.toByteArray(Charsets.UTF_8)

                uploadImage(metadataPath, metadataBytes, "application/json")
                Log.d(TAG, "Metadata subido: $metadataPath")
            }

            val fullUrl = fullResult.getOrNull()!!
            val thumbnailUrl = thumbnailResult.getOrNull()!!

            Log.d(TAG, "✅ Imagen de galería subida exitosamente:")
            Log.d(TAG, "  - ImageID: $imageId")
            Log.d(TAG, "  - Full URL: $fullUrl")
            Log.d(TAG, "  - Thumbnail URL: $thumbnailUrl")

            // Retorna imageId y las URLs
            Result.success(Triple(imageId, fullUrl, thumbnailUrl))
        } catch (e: Exception) {
            Log.e(TAG, "Error al subir imagen de galería", e)
            Result.failure(e)
        }
    }

    /**
     * Lista las imágenes de galería de un usuario con paginación
     * @param userId ID del usuario
     * @param limit Número máximo de imágenes a retornar
     * @param continuationToken Token para paginación (null para primera página)
     * @return Lista de imágenes con sus URLs y token de continuación
     */
    suspend fun listUserGalleryImages(
        userId: String,
        limit: Int = 15,
        continuationToken: String? = null
    ): Result<GalleryListResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Listando imágenes de galería para usuario: $userId, limit: $limit")

            val listRequest = ListObjectsV2Request()
                .withBucketName(BUCKET_NAME)
                .withPrefix("gallery/$userId/")
                .withMaxKeys(limit * 3) // Multiplicar por 3 porque cada imagen tiene full, thumbnail y metadata

            if (continuationToken != null) {
                listRequest.withContinuationToken(continuationToken)
            }

            val result = try {
                getInstance().listObjectsV2(listRequest)
            } catch (e: com.amazonaws.AmazonClientException) {
                Log.w(TAG, "Timeout o error de conexión al listar galería, devolviendo lista vacía", e)
                return@withContext Result.success(
                    GalleryListResult(
                        images = emptyList(),
                        nextContinuationToken = null
                    )
                )
            }
            val objects = result.objectSummaries

            Log.d(TAG, "Objetos encontrados: ${objects.size}")

            // Agrupar archivos por imageId y guardar fecha de modificación
            val imageGroups = mutableMapOf<String, MutableList<String>>()
            val imageDates = mutableMapOf<String, Long>()

            objects.forEach { obj ->
                val key = obj.key
                // Extraer imageId del path: gallery/userId/tipo_imageId.ext
                val fileName = key.substringAfterLast("/")

                when {
                    fileName.startsWith("thumbnail_") -> {
                        val imageId = fileName.removePrefix("thumbnail_").removeSuffix(".png")
                        imageGroups.getOrPut(imageId) { mutableListOf() }.add("thumbnail:$key")
                        // Guardar fecha de modificación del objeto
                        imageDates[imageId] = obj.lastModified?.time ?: 0L
                    }
                    fileName.startsWith("full_") -> {
                        val imageId = fileName.removePrefix("full_").removeSuffix(".png")
                        imageGroups.getOrPut(imageId) { mutableListOf() }.add("full:$key")
                        // Guardar fecha si no existe o es más reciente
                        val existingDate = imageDates[imageId] ?: 0L
                        val currentDate = obj.lastModified?.time ?: 0L
                        if (currentDate > existingDate) {
                            imageDates[imageId] = currentDate
                        }
                    }
                    fileName.startsWith("metadata_") -> {
                        val imageId = fileName.removePrefix("metadata_").removeSuffix(".json")
                        imageGroups.getOrPut(imageId) { mutableListOf() }.add("metadata:$key")
                        // No actualizar fecha para metadata
                    }
                }
            }

            // Convertir a lista de GalleryImage
            val galleryImages = imageGroups.mapNotNull { (imageId, files) ->
                val thumbnailKey = files.find { it.startsWith("thumbnail:") }?.removePrefix("thumbnail:")
                val fullKey = files.find { it.startsWith("full:") }?.removePrefix("full:")

                if (thumbnailKey != null && fullKey != null) {
                    // Usar la fecha real de S3 para el ordenamiento
                    val uploadedAt = imageDates[imageId] ?: 0L

                    Log.d(TAG, "Imagen ID: $imageId, Fecha: $uploadedAt (${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(uploadedAt))})")

                    GalleryImage(
                        imageId = imageId,
                        thumbnailUrl = "$CLOUDFRONT_URL/$thumbnailKey",
                        fullUrl = "$CLOUDFRONT_URL/$fullKey",
                        uploadedAt = uploadedAt,
                        userId = userId,
                        key = fullKey
                    )
                } else {
                    null
                }
            }.sortedByDescending {
                // Ordenar por fecha real de S3
                it.uploadedAt
            } // Más recientes primero basado en fecha de modificación en S3
            .take(limit)

            Log.d(TAG, "Imágenes de galería procesadas: ${galleryImages.size}")
            galleryImages.forEachIndexed { index, image ->
                Log.d(TAG, "  [$index] ID: ${image.imageId}, Fecha: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(image.uploadedAt))}")
            }

            Result.success(
                GalleryListResult(
                    images = galleryImages,
                    nextContinuationToken = if (result.isTruncated) result.nextContinuationToken else null
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error listando imágenes de galería", e)
            Result.failure(e)
        }
    }

    /**
     * Lista todas las imágenes de galería de todos los usuarios
     */
    suspend fun listAllGalleryImages(
        limit: Int = 30,
        continuationToken: String? = null
    ): Result<GalleryListResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Listando todas las imágenes de galería, limit: $limit")

            val listRequest = ListObjectsV2Request()
                .withBucketName(BUCKET_NAME)
                .withPrefix("gallery/") // Todas las imágenes en gallery/
                .withMaxKeys(limit * 3) // Multiplicar por 3 porque cada imagen tiene full, thumbnail y metadata

            if (continuationToken != null) {
                listRequest.withContinuationToken(continuationToken)
            }

            val result = try {
                getInstance().listObjectsV2(listRequest)
            } catch (e: com.amazonaws.AmazonClientException) {
                Log.w(TAG, "Timeout o error de conexión al listar galería global, devolviendo lista vacía", e)
                return@withContext Result.success(
                    GalleryListResult(
                        images = emptyList(),
                        nextContinuationToken = null
                    )
                )
            }

            Log.d(TAG, "Respuesta S3: ${result.objectSummaries.size} objetos encontrados")

            // Agrupar archivos por imageId y usuario
            val imageGroups = mutableMapOf<String, MutableList<String>>()
            val imageDates = mutableMapOf<String, Long>()
            val imageUsers = mutableMapOf<String, String>()

            result.objectSummaries.forEach { obj ->
                val key = obj.key

                // Extraer userId e imageId del path: gallery/userId/tipo_imageId.ext
                val pathParts = key.split("/")
                if (pathParts.size >= 3) {
                    val userId = pathParts[1]
                    val fileName = pathParts.last()

                    when {
                        fileName.startsWith("thumbnail_") -> {
                            val imageId = fileName.removePrefix("thumbnail_").removeSuffix(".png")
                            val fullImageId = "$userId/$imageId"
                            imageGroups.getOrPut(fullImageId) { mutableListOf() }.add("thumbnail:$key")
                            imageUsers[fullImageId] = userId
                            imageDates[fullImageId] = obj.lastModified?.time ?: 0L
                        }
                        fileName.startsWith("full_") -> {
                            val imageId = fileName.removePrefix("full_").removeSuffix(".png")
                            val fullImageId = "$userId/$imageId"
                            imageGroups.getOrPut(fullImageId) { mutableListOf() }.add("full:$key")
                            imageUsers[fullImageId] = userId
                            val existingDate = imageDates[fullImageId] ?: 0L
                            val currentDate = obj.lastModified?.time ?: 0L
                            if (currentDate > existingDate) {
                                imageDates[fullImageId] = currentDate
                            }
                        }
                    }
                }
            }

            // Convertir a lista de GalleryImage
            val galleryImages = imageGroups.mapNotNull { (fullImageId, files) ->
                val thumbnailKey = files.find { it.startsWith("thumbnail:") }?.removePrefix("thumbnail:")
                val fullKey = files.find { it.startsWith("full:") }?.removePrefix("full:")
                val userId = imageUsers[fullImageId]
                val imageId = fullImageId.substringAfter("/")

                if (thumbnailKey != null && fullKey != null && userId != null) {
                    val uploadedAt = imageDates[fullImageId] ?: 0L

                    GalleryImage(
                        imageId = imageId,
                        userId = userId,
                        thumbnailUrl = "$CLOUDFRONT_URL/$thumbnailKey",
                        fullUrl = "$CLOUDFRONT_URL/$fullKey",
                        uploadedAt = uploadedAt,
                        key = fullKey
                    )
                } else {
                    null
                }
            }.sortedByDescending {
                // Ordenar por fecha más reciente primero (feed cronológico)
                it.uploadedAt
            }.take(limit)

            Log.d(TAG, "Imágenes de galería global procesadas: ${galleryImages.size}")

            Result.success(
                GalleryListResult(
                    images = galleryImages,
                    nextContinuationToken = if (result.isTruncated) result.nextContinuationToken else null
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error listando todas las imágenes de galería", e)
            Result.failure(e)
        }
    }

    /**
     * Limpia la instancia del cliente (útil para logout)
     */
    fun clear() {
        instance = null
        Log.d(TAG, "Cliente S3 limpiado")
    }

    /**
     * Clase para representar una imagen de galería
     */
    data class GalleryImage(
        val imageId: String,
        val thumbnailUrl: String,
        val fullUrl: String,
        val uploadedAt: Long = 0L,
        val userId: String? = null,
        val key: String? = null
    )

    /**
     * Resultado de listar imágenes de galería con paginación
     */
    data class GalleryListResult(
        val images: List<GalleryImage>,
        val nextContinuationToken: String? = null
    )
}