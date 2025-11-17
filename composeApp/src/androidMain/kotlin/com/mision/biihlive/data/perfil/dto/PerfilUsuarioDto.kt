package com.mision.biihlive.data.perfil.dto

import kotlinx.serialization.Serializable

@Serializable
data class PerfilUsuarioDto(
    val PK: String,
    val SK: String,
    val userId: String,
    val nickname: String,
    val fullName: String,
    val description: String,
    val totalScore: Int,
    val tipo: String,
    val ubicacion: UbicacionDto,
    val rankingPreference: String,
    val createdAt: Long,
    val photoUrl: String? = null,
    val email: String? = null
)

@Serializable
data class UbicacionDto(
    val ciudad: String,
    val provincia: String,
    val pais: String
)