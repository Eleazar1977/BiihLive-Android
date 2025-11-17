package com.mision.biihlive.domain.perfil.usecase

import com.mision.biihlive.domain.perfil.model.PerfilUsuario
import com.mision.biihlive.domain.perfil.repository.PerfilRepository

class ActualizarPerfilUseCase(
    private val perfilRepository: PerfilRepository
) {
    suspend fun actualizarPerfil(perfil: PerfilUsuario): Result<PerfilUsuario> {
        return perfilRepository.actualizarPerfil(perfil)
    }
    
    suspend fun actualizarDescripcion(userId: String, descripcion: String): Result<Unit> {
        return perfilRepository.actualizarDescripcion(userId, descripcion)
    }
    
    suspend fun actualizarNickname(userId: String, nickname: String): Result<Unit> {
        return perfilRepository.actualizarNickname(userId, nickname)
    }
    
    suspend fun actualizarFotoPerfil(userId: String, photoUrl: String): Result<Unit> {
        return perfilRepository.actualizarFotoPerfil(userId, photoUrl)
    }
}