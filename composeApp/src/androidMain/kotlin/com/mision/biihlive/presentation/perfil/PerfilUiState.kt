package com.mision.biihlive.presentation.perfil

import com.mision.biihlive.domain.perfil.model.PerfilUsuario
import com.mision.biihlive.domain.users.model.UserPreview

data class PerfilUiState(
    val perfil: PerfilUsuario? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,  // Nuevo campo para actualización en segundo plano
    val error: String? = null,
    val isUpdating: Boolean = false,
    val updateSuccess: Boolean = false,

    // Estados de subida de imagen
    val isUploadingImage: Boolean = false,
    val uploadProgress: Float = 0f,
    val uploadSuccess: Boolean = false,

    // Datos calculados
    val siguienteNivel: Int = 0,
    val progreso: Double = 0.0,

    // Control de caché de imágenes
    val shouldBypassImageCache: Boolean = false,  // Activo por 30 segundos después de upload

    // URLs de imágenes de perfil desde S3
    val profileImageUrl: String? = null,
    val profileThumbnailUrl: String? = null,

    // Galería de fotos
    val galleryImages: List<com.mision.biihlive.data.aws.S3ClientProvider.GalleryImage> = emptyList(),
    val isLoadingGallery: Boolean = false,
    val galleryNextToken: String? = null,
    val hasMoreGalleryImages: Boolean = false,

    // Estado de seguimiento (para perfil público consultado)
    val isFollowing: Boolean = false,
    val isLoadingFollow: Boolean = false,

    // Estado de suscripción (para perfil público consultado)
    val isSuscrito: Boolean = false,
    val isLoadingSuscripcion: Boolean = false,

    // Estado de patrocinio (para perfil público consultado)
    val estaPatrocinando: Boolean = false,
    val isLoadingPatrocinio: Boolean = false,

    // Patrocinador actual (si el usuario está siendo patrocinado)
    val patrocinadorActual: PerfilUsuario? = null,
    val isLoadingPatrocinador: Boolean = false,
    val tienePatrocinador: Boolean = false,

    // Preview de seguidores (para perfil público consultado)
    val previewFollowers: List<UserPreview> = emptyList(),
    val isLoadingPreviewFollowers: Boolean = false,

    // Posición en ranking según preferencia del usuario
    val rankingPosition: String = "N/A",  // Ej: "3º"
    val rankingScope: String = "N/A",     // Ej: "Madrid", "España", "Mundial"
    val isLoadingRanking: Boolean = false
) {
    val isContentVisible: Boolean get() = !isLoading && perfil != null
    val hasError: Boolean get() = error != null
}