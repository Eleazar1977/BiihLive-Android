package com.mision.biihlive.domain.perfil.usecase

import com.mision.biihlive.domain.perfil.model.PerfilUsuario
import com.mision.biihlive.domain.perfil.repository.PerfilRepository

class ObtenerPerfilUseCase(
    private val perfilRepository: PerfilRepository
) {
    suspend operator fun invoke(userId: String): Result<PerfilUsuario> {
        return perfilRepository.obtenerPerfil(userId)
    }
}