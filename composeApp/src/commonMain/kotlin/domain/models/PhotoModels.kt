package domain.models

data class PhotoItem(
    val photoUrl: String,
    val id: String,
    val s3Key: String = "",
    val lastModified: Long = System.currentTimeMillis(),
    val description: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileUrl: String = "",
    val likes: Int = 0,
    val comments: Int = 0,
    val shares: Int = 0
)

data class PhotoUiState(
    val isLoading: Boolean = false,
    val photos: List<PhotoItem> = emptyList(),
    val error: String? = null,
    val hasMorePhotos: Boolean = true,
    val currentPhotoIndex: Int = 0
)