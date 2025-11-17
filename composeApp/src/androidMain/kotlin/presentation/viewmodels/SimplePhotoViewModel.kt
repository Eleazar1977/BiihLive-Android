package presentation.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.S3PhotoService
import domain.models.PhotoItem
import domain.models.PhotoUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SimplePhotoViewModel(private val context: Context) : ViewModel() {
    
    companion object {
        private const val TAG = "SimplePhotoViewModel"
    }
    
    private val photoService = S3PhotoService(context)
    
    private val _uiState = MutableStateFlow(PhotoUiState(isLoading = true))
    val uiState: StateFlow<PhotoUiState> = _uiState.asStateFlow()
    
    private var isLoadingMore = false
    
    init {
        Log.d(TAG, "SimplePhotoViewModel initialized with batch loading")
        loadInitialPhotos()
    }
    
    private fun loadInitialPhotos() {
        Log.d(TAG, "Loading initial photos batch...")
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val initialBatch = photoService.loadInitialPhotos()
                Log.d(TAG, "Loaded initial batch: ${initialBatch.size} photos")
                
                if (initialBatch.isNotEmpty()) {
                    _uiState.value = PhotoUiState(
                        isLoading = false,
                        photos = initialBatch,
                        hasMorePhotos = photoService.hasMorePhotos(),
                        currentPhotoIndex = 0
                    )
                } else {
                    _uiState.value = PhotoUiState(
                        isLoading = false,
                        error = "No photos found",
                        photos = emptyList()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading initial photos", e)
                _uiState.value = PhotoUiState(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred",
                    photos = emptyList()
                )
            }
        }
    }
    
    fun loadMorePhotos() {
        if (isLoadingMore || !_uiState.value.hasMorePhotos) {
            Log.d(TAG, "Skip loading more: isLoadingMore=$isLoadingMore, hasMorePhotos=${_uiState.value.hasMorePhotos}")
            return
        }
        
        Log.d(TAG, "Loading more photos...")
        isLoadingMore = true
        
        viewModelScope.launch {
            try {
                val nextBatch = photoService.loadNextPhotos()
                Log.d(TAG, "Loaded next batch: ${nextBatch.size} photos")
                
                if (nextBatch.isNotEmpty()) {
                    val currentState = _uiState.value
                    val allPhotos = currentState.photos + nextBatch
                    
                    _uiState.value = currentState.copy(
                        photos = allPhotos,
                        hasMorePhotos = photoService.hasMorePhotos()
                    )
                    Log.d(TAG, "Total photos now: ${allPhotos.size}, has more: ${photoService.hasMorePhotos()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading more photos", e)
            } finally {
                isLoadingMore = false
            }
        }
    }
    
    fun refreshPhotos() {
        Log.d(TAG, "Refreshing photos...")
        isLoadingMore = false
        loadInitialPhotos()
    }
    
    fun shufflePhotos() {
        Log.d(TAG, "Shuffling photos...")
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val shuffledBatch = photoService.shufflePhotos()
                Log.d(TAG, "Shuffled photos: ${shuffledBatch.size} in new batch")
                
                _uiState.value = PhotoUiState(
                    isLoading = false,
                    photos = shuffledBatch,
                    hasMorePhotos = photoService.hasMorePhotos(),
                    currentPhotoIndex = 0
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error shuffling photos", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun updateCurrentPhotoIndex(index: Int) {
        val currentState = _uiState.value
        if (index >= 0 && index < currentState.photos.size) {
            _uiState.value = currentState.copy(currentPhotoIndex = index)
            
            // Load more photos when approaching the end (3 photos before the end to be safe)
            if (index >= currentState.photos.size - 3 && currentState.hasMorePhotos && !isLoadingMore) {
                Log.d(TAG, "Near end of photos (index $index/${currentState.photos.size}), loading more...")
                loadMorePhotos()
            }
        }
    }
}