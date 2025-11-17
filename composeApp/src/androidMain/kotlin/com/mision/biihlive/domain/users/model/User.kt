package com.mision.biihlive.domain.users.model

/**
 * Modelo de dominio para un usuario del sistema
 */
data class User(
    val userId: String,
    val nickname: String,
    val email: String? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val points: Int = 0,
    val level: Int = 1,
    val location: String? = null,
    val mostrarEstado: Boolean = true, // Preferencia del usuario para mostrar su estado
    val lastSeen: Long? = null,
    val isVerified: Boolean = false,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val userType: String = "REGULAR" // REGULAR, SUBSCRIBER, PREMIUM, etc.
)

/**
 * Modelo simplificado para mostrar en listas - debe coincidir con VTL resolver response
 */
data class UserPreview(
    val userId: String,
    val nickname: String,
    val fullName: String,
    val description: String,
    val totalScore: Int,
    val nivel: Int,
    val tipo: String, // persona, empresa, etc
    val rankingPreference: String,
    val createdAt: Long,
    val photoUrl: String? = null,
    val email: String? = null,
    val isVerified: Boolean = false,

    // Campos sociales y de estado
    val seguidores: Int = 0,
    val siguiendo: Int = 0,
    val isOnline: Boolean = false,
    val mostrarEstado: Boolean = true,

    // Campos de ubicación (estructura simplificada para Firestore)
    val ciudad: String = "",
    val provincia: String = "",
    val pais: String = "",
    val countryCode: String = "",
    val formattedAddress: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val placeId: String = "",
    val privacyLevel: String = "city"
) {
    // Campos de retrocompatibilidad temporal - DEPRECATED
    @Deprecated("Use photoUrl instead", ReplaceWith("photoUrl"))
    val imageUrl: String? get() = photoUrl

    @Deprecated("Use tipo instead", ReplaceWith("tipo"))
    val userType: String get() = tipo

    companion object {
        // Factory method para compatibilidad con interfaz anterior
        @Deprecated("Use new constructor with all fields", ReplaceWith("UserPreview(userId, nickname, fullName, description, totalScore, nivel, tipo, rankingPreference, createdAt, photoUrl, null, isVerified)"))
        fun createLegacy(
            userId: String,
            nickname: String,
            imageUrl: String? = null,
            description: String? = null,
            isVerified: Boolean = false,
            userType: String = "REGULAR"
        ): UserPreview = UserPreview(
            userId = userId,
            nickname = nickname,
            fullName = nickname, // Usar nickname como fullName por defecto
            description = description ?: "",
            totalScore = 0,
            nivel = 1,
            tipo = userType.lowercase(),
            rankingPreference = "local",
            createdAt = System.currentTimeMillis(),
            photoUrl = imageUrl,
            isVerified = isVerified
        )
    }
}