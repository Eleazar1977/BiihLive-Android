package domain.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Modelo principal para posts con funcionalidades sociales
 * Reemplaza el PhotoItem anterior con estructura escalable
 */
data class Post(
    val postId: String,
    val userId: String,
    val type: String = "photo", // "photo", "video", etc.
    val mediaUrl: String,
    val thumbnailUrl: String? = null, // Para videos
    val description: String = "",

    // Contadores sociales
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val viewsCount: Int = 0,

    // Estados del usuario actual
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,

    // Metadata
    val hashtags: List<String> = emptyList(),
    val location: PostLocation? = null,
    val isPublic: Boolean = true,
    val status: String = "active", // "active", "deleted", "reported"

    // Timestamps
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,

    // Información del autor (desnormalizada para performance)
    val authorInfo: AuthorInfo? = null,
    
    // ===== NUEVOS CAMPOS PARA OPTIMIZACIÓN (Backwards Compatible) =====
    
    /**
     * Score aleatorio para sampling eficiente en feed
     * Valor entre 0.0 y 1.0 para queries optimizadas
     * Se genera al crear el post o se agrega con Cloud Function
     */
    val randomScore: Double? = null,
    
    /**
     * Metadata de imagen para optimización de carga
     * Incluye: blurhash, dimensiones, colores, formatos
     */
    val imageMetadata: ImageMetadata? = null
) {
    companion object {
        fun fromFirestore(document: DocumentSnapshot, currentUserId: String? = null): Post? {
            return try {
                val data = document.data ?: return null

                Post(
                    postId = document.id,
                    userId = data["userId"] as? String ?: return null,
                    type = data["type"] as? String ?: "photo",
                    mediaUrl = data["mediaUrl"] as? String ?: return null,
                    thumbnailUrl = data["thumbnailUrl"] as? String,
                    description = data["description"] as? String ?: "",

                    // Contadores
                    likesCount = (data["likesCount"] as? Long)?.toInt() ?: 0,
                    commentsCount = (data["commentsCount"] as? Long)?.toInt() ?: 0,
                    sharesCount = (data["sharesCount"] as? Long)?.toInt() ?: 0,
                    viewsCount = (data["viewsCount"] as? Long)?.toInt() ?: 0,

                    // Estados (se llenarán después con consultas separadas)
                    isLiked = false, // Se actualiza después
                    isBookmarked = false,

                    // Metadata
                    hashtags = (data["hashtags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    location = (data["location"] as? Map<String, Any>)?.let { locationMap ->
                        PostLocation.fromMap(locationMap)
                    },
                    isPublic = data["isPublic"] as? Boolean ?: true,
                    status = data["status"] as? String ?: "active",

                    // Timestamps
                    createdAt = data["createdAt"] as? Timestamp,
                    updatedAt = data["updatedAt"] as? Timestamp,

                    // Información del autor (se llena después)
                    authorInfo = null, // Se actualiza después
                    
                    // ===== NUEVOS CAMPOS OPCIONALES =====
                    randomScore = data["randomScore"] as? Double,
                    imageMetadata = (data["imageMetadata"] as? Map<String, Any>)?.let { 
                        ImageMetadata.fromMap(it) 
                    }
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Metadata de imagen para optimización de carga progresiva
 * 
 * Incluye información crítica para:
 * - Placeholders con BlurHash (carga instantánea)
 * - Color dominante como fallback
 * - Dimensiones y aspect ratio para layout
 * - Formatos disponibles para content negotiation
 * - Estado de procesamiento de variantes
 */
data class ImageMetadata(
    val imageId: String,
    val cdnBaseUrl: String = "https://d183hg75gdabnr.cloudfront.net",
    val originalPath: String, // "originals/posts/{userId}/{imageId}.jpg"
    
    // CRÍTICO para placeholders instantáneos
    val blurhash: String? = null, // ~30 bytes, genera blur placeholder
    val dominantColor: String? = null, // "#4A6FA5" - color de fondo
    
    // Dimensiones originales
    val width: Int? = null,
    val height: Int? = null,
    val aspectRatio: Float? = null, // width / height
    
    // Formatos disponibles para content negotiation
    val formats: List<String> = listOf("avif", "webp", "jpeg"),
    
    // Estado de procesamiento de variantes
    val processingStatus: String = "pending" // "pending", "processing", "complete"
) {
    companion object {
        /**
         * Parsear ImageMetadata desde Map de Firestore
         */
        fun fromMap(map: Map<String, Any>): ImageMetadata {
            return ImageMetadata(
                imageId = map["imageId"] as? String ?: "",
                cdnBaseUrl = map["cdnBaseUrl"] as? String ?: "https://d183hg75gdabnr.cloudfront.net",
                originalPath = map["originalPath"] as? String ?: "",
                blurhash = map["blurhash"] as? String,
                dominantColor = map["dominantColor"] as? String,
                width = (map["width"] as? Long)?.toInt(),
                height = (map["height"] as? Long)?.toInt(),
                aspectRatio = (map["aspectRatio"] as? Double)?.toFloat(),
                formats = (map["formats"] as? List<*>)?.filterIsInstance<String>() 
                    ?: listOf("avif", "webp", "jpeg"),
                processingStatus = map["processingStatus"] as? String ?: "complete"
            )
        }
        
        /**
         * Convertir a Map para guardar en Firestore
         */
        fun ImageMetadata.toMap(): Map<String, Any?> {
            return mapOf(
                "imageId" to imageId,
                "cdnBaseUrl" to cdnBaseUrl,
                "originalPath" to originalPath,
                "blurhash" to blurhash,
                "dominantColor" to dominantColor,
                "width" to width,
                "height" to height,
                "aspectRatio" to aspectRatio,
                "formats" to formats,
                "processingStatus" to processingStatus
            )
        }
    }
}

/**
 * Información del autor del post (desnormalizada)
 */
data class AuthorInfo(
    val userId: String,
    val nickname: String,
    val profileImageUrl: String? = null,
    val isVerified: Boolean = false,
    val isFollowed: Boolean = false // Estado del usuario actual
) {
    companion object {
        fun fromUserProfile(userId: String, nickname: String, profileImageUrl: String? = null): AuthorInfo {
            return AuthorInfo(
                userId = userId,
                nickname = nickname,
                profileImageUrl = profileImageUrl ?: generateDefaultProfileUrl(userId),
                isVerified = false,
                isFollowed = false
            )
        }

        private fun generateDefaultProfileUrl(userId: String): String {
            return "https://d183hg75gdabnr.cloudfront.net/userprofile/$userId/thumbnail_1728914400.png"
        }
    }
}

/**
 * Ubicación del post
 */
data class PostLocation(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val city: String? = null,
    val country: String? = null,
    val address: String? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any>): PostLocation {
            return PostLocation(
                latitude = map["latitude"] as? Double,
                longitude = map["longitude"] as? Double,
                city = map["city"] as? String,
                country = map["country"] as? String,
                address = map["address"] as? String
            )
        }
    }
}

/**
 * Comentario de un post
 */
data class Comment(
    val commentId: String,
    val postId: String,
    val userId: String,
    val content: String,
    val likesCount: Int = 0,
    val repliesCount: Int = 0,
    val replyToCommentId: String? = null, // Para replies anidados
    val isEdited: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,

    // Información del autor
    val authorInfo: AuthorInfo? = null,

    // Estado del usuario actual
    val isLiked: Boolean = false
) {
    companion object {
        fun fromFirestore(document: DocumentSnapshot): Comment? {
            return try {
                val data = document.data ?: return null

                Comment(
                    commentId = document.id,
                    postId = data["postId"] as? String ?: return null,
                    userId = data["userId"] as? String ?: return null,
                    content = data["content"] as? String ?: return null,
                    likesCount = (data["likesCount"] as? Long)?.toInt() ?: 0,
                    repliesCount = (data["repliesCount"] as? Long)?.toInt() ?: 0,
                    replyToCommentId = data["replyToCommentId"] as? String,
                    isEdited = data["isEdited"] as? Boolean ?: false,
                    createdAt = data["createdAt"] as? Timestamp,
                    updatedAt = data["updatedAt"] as? Timestamp,
                    authorInfo = null, // Se llena después
                    isLiked = false // Se llena después
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Estados de UI para posts
 */
data class PostsUiState(
    val isLoading: Boolean = false,
    val posts: List<Post> = emptyList(),
    val error: String? = null,
    val hasMorePosts: Boolean = true,
    val currentPostIndex: Int = 0
)

/**
 * Estados de UI para comentarios
 */
data class CommentsUiState(
    val isLoading: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val error: String? = null,
    val hasMoreComments: Boolean = true
)