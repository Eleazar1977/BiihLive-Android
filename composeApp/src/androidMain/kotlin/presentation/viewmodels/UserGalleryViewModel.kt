package presentation.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.UserGalleryService
import domain.models.PhotoUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserGalleryViewModel(
    private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "UserGalleryViewModel"
        private const val PRELOAD_THRESHOLD = 7 // Trigger preload when 7 items before end
    }

    private val userGalleryService = UserGalleryService(context)

    private val _uiState = MutableStateFlow(PhotoUiState())
    val uiState: StateFlow<PhotoUiState> = _uiState.asStateFlow()

    private var isLoadingMore = false
    private var currentUserId: String = ""

    init {
        Log.d(TAG, "UserGalleryViewModel inicializado")
    }

    fun loadUserGallery(userId: String) {
        if (userId.isEmpty()) {
            Log.w(TAG, "Cannot load gallery for empty userId")
            return
        }

        // Si es un usuario diferente, resetear estado
        if (currentUserId != userId) {
            currentUserId = userId
            _uiState.value = PhotoUiState()
        }

        if (_uiState.value.photos.isEmpty()) {
            loadInitialPhotos()
        }
    }

    private fun loadInitialPhotos() {
        if (currentUserId.isEmpty()) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                Log.d(TAG, "Cargando fotos iniciales para usuario: $currentUserId")

                val photos = userGalleryService.loadInitialPhotos(currentUserId)

                if (photos.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        photos = photos,
                        isLoading = false,
                        hasMorePhotos = userGalleryService.hasMorePhotos()
                    )
                    Log.d(TAG, "✅ ${photos.size} fotos cargadas para usuario $currentUserId")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No se encontraron fotos para este usuario"
                    )
                    Log.w(TAG, "No se encontraron fotos para usuario $currentUserId")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error cargando fotos iniciales para usuario $currentUserId", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar las fotos: ${e.message}"
                )
            }
        }
    }

    fun updateCurrentPhotoIndex(index: Int) {
        val currentState = _uiState.value

        // Actualizar índice actual
        _uiState.value = currentState.copy(currentPhotoIndex = index)

        // Trigger preload when approaching end - MISMO PATRÓN QUE PHOTOFEED OPTIMIZADO
        if (index >= currentState.photos.size - PRELOAD_THRESHOLD &&
            currentState.hasMorePhotos &&
            !isLoadingMore) {

            Log.d(TAG, "🚀 PRELOAD TRIGGER: index=$index, total=${currentState.photos.size}, threshold=$PRELOAD_THRESHOLD")
            loadMorePhotos()
        }
    }

    private fun loadMorePhotos() {
        if (isLoadingMore || !_uiState.value.hasMorePhotos || currentUserId.isEmpty()) {
            return
        }

        isLoadingMore = true

        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Cargando más fotos para usuario $currentUserId...")

                val newPhotos = userGalleryService.loadNextPhotos()

                if (newPhotos.isNotEmpty()) {
                    val currentPhotos = _uiState.value.photos
                    val updatedPhotos = currentPhotos + newPhotos

                    _uiState.value = _uiState.value.copy(
                        photos = updatedPhotos,
                        hasMorePhotos = userGalleryService.hasMorePhotos()
                    )

                    Log.d(TAG, "✅ ${newPhotos.size} fotos más cargadas. Total: ${updatedPhotos.size}")
                } else {
                    Log.d(TAG, "No hay más fotos para cargar")
                    _uiState.value = _uiState.value.copy(hasMorePhotos = false)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error cargando más fotos para usuario $currentUserId", e)
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun refreshPhotos() {
        if (currentUserId.isEmpty()) return

        Log.d(TAG, "🔄 Refrescando fotos para usuario $currentUserId")

        viewModelScope.launch {
            try {
                _uiState.value = PhotoUiState(isLoading = true)

                // Recargar desde el inicio
                val photos = userGalleryService.loadInitialPhotos(currentUserId)

                if (photos.isNotEmpty()) {
                    _uiState.value = PhotoUiState(
                        photos = photos,
                        hasMorePhotos = userGalleryService.hasMorePhotos()
                    )
                    Log.d(TAG, "✅ Fotos refrescadas: ${photos.size} fotos para usuario $currentUserId")
                } else {
                    _uiState.value = PhotoUiState(
                        error = "No se encontraron fotos para este usuario"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error refrescando fotos para usuario $currentUserId", e)
                _uiState.value = PhotoUiState(
                    error = "Error al recargar las fotos: ${e.message}"
                )
            }
        }
    }

    fun shufflePhotos() {
        if (currentUserId.isEmpty()) return

        viewModelScope.launch {
            try {
                Log.d(TAG, "🔀 Mezclando fotos para usuario $currentUserId")

                val shuffledPhotos = userGalleryService.shufflePhotos()

                if (shuffledPhotos.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        photos = shuffledPhotos,
                        currentPhotoIndex = 0 // Reset al inicio después del shuffle
                    )
                    Log.d(TAG, "✅ Fotos mezcladas: ${shuffledPhotos.size} fotos")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error mezclando fotos para usuario $currentUserId", e)
            }
        }
    }

    fun getCurrentUserId(): String = currentUserId

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "UserGalleryViewModel cleared")
    }
}