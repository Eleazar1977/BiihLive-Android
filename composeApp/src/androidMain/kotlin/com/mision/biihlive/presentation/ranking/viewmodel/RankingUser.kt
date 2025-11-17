package com.mision.biihlive.presentation.ranking.viewmodel

/**
 * Modelo de usuario para el ranking (presentation layer)
 * Separado del modelo de datos de FirestoreRepository para mantener separación de capas
 */
data class RankingUser(
    val userId: String,
    val nickname: String,
    val fullName: String,
    val totalScore: Int,
    val ubicacion: String,
    val ciudad: String,
    val provincia: String,
    val pais: String,
    val nivel: Int,
    val isVerified: Boolean,
    val profileImageUrl: String,
    val thumbnailImageUrl: String,
    val countryCode: String,
    val subdivisionCode: String,
    val postalCode: String
)