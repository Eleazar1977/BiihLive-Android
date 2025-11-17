package com.mision.biihlive.domain.suscripciones.model

data class Suscripcion(
    val suscripcionId: String,
    val userId: String, // Usuario suscrito
    val nickname: String,
    val imageUrl: String? = null,
    val isVerified: Boolean = false,
    val fechaUnion: Long, // Timestamp de cuando se unió
    val fechaExpiracion: Long, // Timestamp de cuando expira
    val tipo: String = "premium", // Tipo de suscripción
    val estado: String = "activa" // Estado: activa, expirada, cancelada
)

// Para uso en UI - datos preprocesados
data class SuscripcionPreview(
    val suscripcionId: String,
    val userId: String,
    val nickname: String,
    val imageUrl: String? = null,
    val isVerified: Boolean = false,
    val fechaUnionFormateada: String, // "2025-01-07"
    val fechaExpiracionFormateada: String, // "2025-02-07"
    val diasRestantes: Int, // Días hasta expiración
    val estaExpirada: Boolean = false
)