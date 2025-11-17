package com.mision.biihlive.domain.perfil.repository

import com.mision.biihlive.domain.perfil.model.PerfilUsuario
import kotlinx.coroutines.flow.Flow

interface PerfilRepository {
    suspend fun obtenerPerfil(userId: String): Result<PerfilUsuario>
    suspend fun actualizarPerfil(perfil: PerfilUsuario): Result<PerfilUsuario>
    suspend fun actualizarDescripcion(userId: String, descripcion: String): Result<Unit>
    suspend fun actualizarNickname(userId: String, nickname: String): Result<Unit>
    suspend fun actualizarFotoPerfil(userId: String, photoUrl: String): Result<Unit>
    suspend fun incrementarSeguidores(userId: String): Result<Unit>
    suspend fun decrementarSeguidores(userId: String): Result<Unit>
    suspend fun obtenerRanking(userId: String): Result<Map<String, Int>>
    fun observarPerfil(userId: String): Flow<PerfilUsuario?>
}