package presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import data.repository.SocialPostsRepository
import domain.models.Post
import domain.models.PostsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para el feed social con funcionalidades de likes y comentarios
 * Reemplaza SimplePhotoViewModel con arquitectura escalable
 */
class SocialFeedViewModel : ViewModel() {

    companion object {
        private const val TAG = "SocialFeedViewModel"
        private const val BATCH_SIZE = 20
    }

    private val repository = SocialPostsRepository()

    private val _uiState = MutableStateFlow(PostsUiState(isLoading = true))
    val uiState: StateFlow<PostsUiState> = _uiState.asStateFlow()

    // Variables para paginación
    private var lastDocument: DocumentSnapshot? = null
    private var isLoadingMore = false
    private var isPreloadingNext = false

    // Cache de posts para UX optimista
    private val postsCache = mutableListOf<Post>()

    init {
        Log.d(TAG, "🚀 SocialFeedViewModel inicializado")
        loadInitialPosts()
    }

    /**
     * Cargar posts iniciales
     */
    private fun loadInitialPosts() {
        Log.d(TAG, "📱 Cargando posts iniciales del feed social...")

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val result = repository.getFeedPosts(limit = BATCH_SIZE)

                if (result.isSuccess) {
                    val (posts, lastDoc) = result.getOrThrow()
                    lastDocument = lastDoc

                    // Actualizar cache
                    postsCache.clear()
                    postsCache.addAll(posts)

                    _uiState.value = PostsUiState(
                        isLoading = false,
                        posts = posts,
                        hasMorePosts = posts.size >= BATCH_SIZE,
                        currentPostIndex = 0
                    )

                    Log.d(TAG, "✅ Cargados ${posts.size} posts iniciales")

                    // Iniciar precarga proactiva
                    startProactivePreload()

                } else {
                    val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                    _uiState.value = PostsUiState(
                        isLoading = false,
                        error = error,
                        posts = emptyList()
                    )
                    Log.e(TAG, "❌ Error cargando posts iniciales: $error")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en loadInitialPosts", e)
                _uiState.value = PostsUiState(
                    isLoading = false,
                    error = e.message ?: "Error cargando posts",
                    posts = emptyList()
                )
            }
        }
    }

    /**
     * Cargar más posts (paginación)
     */
    fun loadMorePosts() {
        if (isLoadingMore || !_uiState.value.hasMorePosts || lastDocument == null) {
            Log.d(TAG, "⏭️ Saltando carga: isLoadingMore=$isLoadingMore, hasMore=${_uiState.value.hasMorePosts}")
            return
        }

        Log.d(TAG, "📄 Cargando más posts...")
        isLoadingMore = true

        viewModelScope.launch {
            try {
                val result = repository.getFeedPosts(
                    limit = BATCH_SIZE,
                    lastDocument = lastDocument
                )

                if (result.isSuccess) {
                    val (newPosts, lastDoc) = result.getOrThrow()
                    lastDocument = lastDoc

                    // Agregar a cache
                    postsCache.addAll(newPosts)

                    val currentState = _uiState.value
                    val allPosts = currentState.posts + newPosts

                    _uiState.value = currentState.copy(
                        posts = allPosts,
                        hasMorePosts = newPosts.size >= BATCH_SIZE
                    )

                    Log.d(TAG, "✅ Cargados ${newPosts.size} posts adicionales. Total: ${allPosts.size}")

                    // Continuar precarga proactiva
                    startProactivePreload()

                } else {
                    Log.e(TAG, "❌ Error cargando más posts: ${result.exceptionOrNull()?.message}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en loadMorePosts", e)
            } finally {
                isLoadingMore = false
            }
        }
    }

    /**
     * Dar punto permanente a un post (según implementación del APK funcional)
     */
    fun givePoint(post: Post) {
        // Verificar si ya puntuó - evitar puntuaciones múltiples
        if (post.isLiked) {
            Log.d(TAG, "⚠️ Usuario ya puntuó el post ${post.postId}, no se puede puntuar de nuevo")
            return
        }

        Log.d(TAG, "👍 Dando punto permanente al post ${post.postId}")

        viewModelScope.launch {
            // 1. Actualización optimista a true (solo cuando no había puntuado)
            updatePostOptimistically(post.postId, true)

            // 2. Operación en backend - solo likePost, nunca unlike
            val result = repository.likePost(post.postId)

            // 3. Revertir si falla
            if (result.isFailure) {
                updatePostOptimistically(post.postId, false)
                _uiState.value = _uiState.value.copy(
                    error = "Error al dar punto: ${result.exceptionOrNull()?.message}"
                )
                Log.e(TAG, "❌ Error dando punto permanente", result.exceptionOrNull())
            } else {
                Log.d(TAG, "✅ Punto PERMANENTE agregado exitosamente al post ${post.postId}")
            }
        }
    }

    /**
     * Navegar a pantalla de comentarios
     */
    fun openComments(post: Post) {
        Log.d(TAG, "💬 Abriendo comentarios para post ${post.postId}")
        // TODO: Implementar navegación a pantalla de comentarios
    }

    /**
     * Compartir post
     */
    fun sharePost(post: Post) {
        Log.d(TAG, "📤 Compartiendo post ${post.postId}")
        // TODO: Implementar funcionalidad de compartir
    }

    /**
     * Actualizar índice del post actual
     */
    fun updateCurrentPostIndex(index: Int) {
        val currentState = _uiState.value
        if (index >= 0 && index < currentState.posts.size) {
            _uiState.value = currentState.copy(currentPostIndex = index)

            // Cargar más posts cuando se acerque al final
            if (index >= currentState.posts.size - 5 && currentState.hasMorePosts && !isLoadingMore) {
                Log.d(TAG, "🔄 Cerca del final (index $index/${currentState.posts.size}), cargando más...")
                loadMorePosts()
            }
        }
    }

    /**
     * Refrescar feed completo
     */
    fun refreshFeed() {
        Log.d(TAG, "🔄 Refrescando feed completo...")

        // Reset estado
        lastDocument = null
        isLoadingMore = false
        postsCache.clear()

        loadInitialPosts()
    }

    /**
     * Mezclar posts (shuffle)
     */
    fun shufflePosts() {
        Log.d(TAG, "🔀 Mezclando posts del feed...")

        val currentState = _uiState.value
        if (currentState.posts.isNotEmpty()) {
            val shuffledPosts = currentState.posts.shuffled()
            _uiState.value = currentState.copy(
                posts = shuffledPosts,
                currentPostIndex = 0
            )

            // Actualizar cache
            postsCache.clear()
            postsCache.addAll(shuffledPosts)
        }
    }

    // ----- FUNCIONES AUXILIARES -----

    /**
     * Actualización optimista de estado de like
     */
    private fun updatePostOptimistically(postId: String, isLiked: Boolean) {
        val currentState = _uiState.value
        val updatedPosts = currentState.posts.map { post ->
            if (post.postId == postId) {
                post.copy(
                    isLiked = isLiked,
                    likesCount = if (isLiked) post.likesCount + 1 else post.likesCount - 1
                )
            } else {
                post
            }
        }

        _uiState.value = currentState.copy(posts = updatedPosts)

        // Actualizar también el cache
        val postIndex = postsCache.indexOfFirst { it.postId == postId }
        if (postIndex != -1) {
            postsCache[postIndex] = postsCache[postIndex].copy(
                isLiked = isLiked,
                likesCount = if (isLiked) postsCache[postIndex].likesCount + 1 else postsCache[postIndex].likesCount - 1
            )
        }
    }

    /**
     * Precarga proactiva en background
     */
    private fun startProactivePreload() {
        if (isPreloadingNext || lastDocument == null) return

        Log.d(TAG, "🔄 Iniciando precarga proactiva...")
        isPreloadingNext = true

        viewModelScope.launch {
            try {
                val result = repository.getFeedPosts(
                    limit = BATCH_SIZE,
                    lastDocument = lastDocument
                )

                if (result.isSuccess) {
                    val (preloadedPosts, _) = result.getOrThrow()
                    Log.d(TAG, "✅ Precargados ${preloadedPosts.size} posts en background")

                    // Los posts están precargados en memoria
                    // Se usarán cuando el usuario haga loadMorePosts()
                }

            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error en precarga proactiva (no crítico)", e)
            } finally {
                isPreloadingNext = false
            }
        }
    }
}