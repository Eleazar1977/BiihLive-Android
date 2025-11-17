package data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.mision.biihlive.data.repository.FirestoreRepository
import domain.models.AuthorInfo
import domain.models.Comment
import domain.models.Post
import domain.models.PostsUiState
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository para manejar posts con funcionalidades sociales
 * Usa la nueva colección 'posts' en Firestore basebiihlive
 */
class SocialPostsRepository {

    companion object {
        private const val TAG = "SocialPostsRepository"
        private const val POSTS_COLLECTION = "posts"
        private const val USERS_COLLECTION = "users"
        private const val DATABASE_NAME = "basebiihlive"
        private const val CLOUDFRONT_URL = "https://d183hg75gdabnr.cloudfront.net"
        private const val DEFAULT_TIMESTAMP = "1759240530172"
    }

    // Usar base de datos específica 'basebiihlive'
    private val firestore: FirebaseFirestore by lazy {
        try {
            Firebase.firestore(DATABASE_NAME)
        } catch (e: Exception) {
            // Fallback a base default si no está disponible la específica
            Log.w(TAG, "No se pudo conectar a base $DATABASE_NAME, usando default", e)
            FirebaseFirestore.getInstance()
        }
    }

    private val auth = FirebaseAuth.getInstance()
    private val firestoreRepository = FirestoreRepository()

    /**
     * Obtener feed de posts con paginación
     */
    suspend fun getFeedPosts(
        limit: Int = 20,
        lastDocument: DocumentSnapshot? = null
    ): Result<Pair<List<Post>, DocumentSnapshot?>> {
        return try {
            Log.d(TAG, "🔄 Cargando feed de posts (limit: $limit)")

            // TEMPORAL: Consulta simplificada sin índice compuesto mientras se crea el índice
            val query = firestore.collection(POSTS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit((limit * 2).toLong()) // Aumentamos limite para compensar filtrado

            val snapshot = if (lastDocument != null) {
                query.startAfter(lastDocument).get().await()
            } else {
                query.get().await()
            }

            if (snapshot.isEmpty) {
                Log.d(TAG, "📭 No se encontraron posts")
                return Result.success(Pair(emptyList(), null))
            }

            Log.d(TAG, "📄 Obtenidos ${snapshot.documents.size} posts de Firestore")

            // Convertir documentos a Post
            val allPosts = snapshot.documents.mapNotNull { doc ->
                Post.fromFirestore(doc, getCurrentUserId())
            }

            // TEMPORAL: Filtrar en código mientras se crea el índice compuesto
            val posts = allPosts
                .filter { it.status == "active" && it.isPublic }
                .take(limit)

            Log.d(TAG, "📄 Posts filtrados: ${posts.size} de ${allPosts.size} totales")

            if (posts.isEmpty()) {
                Log.d(TAG, "📭 No se encontraron posts después del filtrado")
                return Result.success(Pair(emptyList(), null))
            }

            // Enriquecer con información de autores
            val enrichedPosts = enrichPostsWithAuthorInfo(posts)

            // Enriquecer con estados de likes del usuario actual
            val postsWithLikes = enrichPostsWithLikeStatus(enrichedPosts)

            val lastDoc = snapshot.documents.lastOrNull()

            Log.d(TAG, "✅ Feed procesado: ${postsWithLikes.size} posts con información completa")

            Result.success(Pair(postsWithLikes, lastDoc))

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo feed de posts", e)
            Result.failure(e)
        }
    }

    /**
     * Dar like a un post
     */
    suspend fun likePost(postId: String): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuario no autenticado"))

        return try {
            Log.d(TAG, "👍 Dando like al post $postId")

            firestore.runTransaction { transaction ->
                val postRef = firestore.collection(POSTS_COLLECTION).document(postId)
                val likeRef = postRef.collection("likes").document(userId)

                // Verificar si ya existe el like
                val existingLike = transaction.get(likeRef)
                if (!existingLike.exists()) {
                    // Agregar like
                    transaction.set(likeRef, mapOf(
                        "userId" to userId,
                        "createdAt" to FieldValue.serverTimestamp()
                    ))

                    // Incrementar contador
                    transaction.update(postRef, "likesCount", FieldValue.increment(1))

                    // TODO: Crear actividad para notificación
                    Log.d(TAG, "✅ Like agregado al post $postId")
                } else {
                    Log.d(TAG, "⚠️ El usuario ya dio like al post $postId")
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error dando like al post $postId", e)
            Result.failure(e)
        }
    }

    /**
     * Quitar like de un post
     */
    suspend fun unlikePost(postId: String): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuario no autenticado"))

        return try {
            Log.d(TAG, "👎 Quitando like del post $postId")

            firestore.runTransaction { transaction ->
                val postRef = firestore.collection(POSTS_COLLECTION).document(postId)
                val likeRef = postRef.collection("likes").document(userId)

                // Verificar si existe el like
                val existingLike = transaction.get(likeRef)
                if (existingLike.exists()) {
                    // Quitar like
                    transaction.delete(likeRef)

                    // Decrementar contador
                    transaction.update(postRef, "likesCount", FieldValue.increment(-1))

                    Log.d(TAG, "✅ Like removido del post $postId")
                } else {
                    Log.d(TAG, "⚠️ El usuario no había dado like al post $postId")
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error quitando like del post $postId", e)
            Result.failure(e)
        }
    }

    /**
     * Verificar si el usuario actual ha dado like a un post
     */
    suspend fun isPostLiked(postId: String): Result<Boolean> {
        val userId = getCurrentUserId() ?: return Result.success(false)

        return try {
            val likeDoc = firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .collection("likes")
                .document(userId)
                .get()
                .await()

            Result.success(likeDoc.exists())
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error verificando like del post $postId", e)
            Result.failure(e)
        }
    }

    /**
     * Agregar comentario a un post
     */
    suspend fun addComment(postId: String, content: String): Result<String> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuario no autenticado"))

        return try {
            Log.d(TAG, "💬 Agregando comentario al post $postId")

            val commentId = UUID.randomUUID().toString()

            firestore.runTransaction { transaction ->
                val postRef = firestore.collection(POSTS_COLLECTION).document(postId)
                val commentRef = postRef.collection("comments").document(commentId)

                // Agregar comentario
                val commentData = mapOf(
                    "postId" to postId,
                    "userId" to userId,
                    "content" to content,
                    "likesCount" to 0,
                    "repliesCount" to 0,
                    "replyToCommentId" to null,
                    "isEdited" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                transaction.set(commentRef, commentData)

                // Incrementar contador de comentarios
                transaction.update(postRef, "commentsCount", FieldValue.increment(1))

                Log.d(TAG, "✅ Comentario agregado al post $postId")
            }.await()

            Result.success(commentId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error agregando comentario al post $postId", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener comentarios de un post
     */
    suspend fun getComments(
        postId: String,
        limit: Int = 50,
        lastDocument: DocumentSnapshot? = null
    ): Result<Pair<List<Comment>, DocumentSnapshot?>> {
        return try {
            Log.d(TAG, "💬 Obteniendo comentarios del post $postId")

            val query = firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            val snapshot = if (lastDocument != null) {
                query.startAfter(lastDocument).get().await()
            } else {
                query.get().await()
            }

            val comments = snapshot.documents.mapNotNull { doc ->
                Comment.fromFirestore(doc)
            }

            // Enriquecer con información de autores
            val enrichedComments = enrichCommentsWithAuthorInfo(comments)

            val lastDoc = snapshot.documents.lastOrNull()

            Log.d(TAG, "✅ Obtenidos ${enrichedComments.size} comentarios del post $postId")

            Result.success(Pair(enrichedComments, lastDoc))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo comentarios del post $postId", e)
            Result.failure(e)
        }
    }

    // ----- FUNCIONES AUXILIARES -----

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Enriquecer posts con información de autores
     */
    private suspend fun enrichPostsWithAuthorInfo(posts: List<Post>): List<Post> {
        return try {
            val userIds = posts.map { it.userId }.distinct()
            val userInfoMap = mutableMapOf<String, AuthorInfo>()

            // Obtener información de usuarios en paralelo
            userIds.forEach { userId ->
                try {
                    val userResult = firestoreRepository.getPerfilUsuario(userId)
                    if (userResult.isSuccess) {
                        val profile = userResult.getOrNull()
                        if (profile != null) {
                            userInfoMap[userId] = AuthorInfo.fromUserProfile(
                                userId = userId,
                                nickname = profile.nickname,
                                profileImageUrl = generateThumbnailUrl(userId)
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error obteniendo información de usuario $userId", e)
                }
            }

            // Aplicar información de autores a posts
            posts.map { post ->
                post.copy(authorInfo = userInfoMap[post.userId])
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error enriqueciendo posts con información de autores", e)
            posts
        }
    }

    /**
     * Enriquecer posts con estado de likes del usuario actual
     */
    private suspend fun enrichPostsWithLikeStatus(posts: List<Post>): List<Post> {
        val userId = getCurrentUserId() ?: return posts

        return try {
            posts.map { post ->
                val isLikedResult = isPostLiked(post.postId)
                val isLiked = isLikedResult.getOrElse { false }
                post.copy(isLiked = isLiked)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enriqueciendo posts con estado de likes", e)
            posts
        }
    }

    /**
     * Enriquecer comentarios con información de autores
     */
    private suspend fun enrichCommentsWithAuthorInfo(comments: List<Comment>): List<Comment> {
        return try {
            val userIds = comments.map { it.userId }.distinct()
            val userInfoMap = mutableMapOf<String, AuthorInfo>()

            userIds.forEach { userId ->
                try {
                    val userResult = firestoreRepository.getPerfilUsuario(userId)
                    if (userResult.isSuccess) {
                        val profile = userResult.getOrNull()
                        if (profile != null) {
                            userInfoMap[userId] = AuthorInfo.fromUserProfile(
                                userId = userId,
                                nickname = profile.nickname,
                                profileImageUrl = generateThumbnailUrl(userId)
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error obteniendo información de usuario $userId para comentario", e)
                }
            }

            comments.map { comment ->
                comment.copy(authorInfo = userInfoMap[comment.userId])
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error enriqueciendo comentarios con información de autores", e)
            comments
        }
    }

    /**
     * Generar URL de thumbnail siguiendo el patrón del proyecto
     */
    private fun generateThumbnailUrl(userId: String): String {
        return "$CLOUDFRONT_URL/userprofile/$userId/thumbnail_$DEFAULT_TIMESTAMP.png"
    }
}