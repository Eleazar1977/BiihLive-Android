package domain.models

data class VideoItem(
    val videoUrl: String,
    val id: String,
    val s3Key: String = "",
    val lastModified: Long = 0L,
    val description: String = ""
)

data class VideoUiState(
    val isLoading: Boolean = false,
    val videos: List<VideoItem> = emptyList(),
    val currentVideoIndex: Int = 0,
    val error: String? = null,
    val hasMoreVideos: Boolean = true
)