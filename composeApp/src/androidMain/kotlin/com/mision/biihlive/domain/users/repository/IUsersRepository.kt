package com.mision.biihlive.domain.users.repository

import com.mision.biihlive.domain.users.model.User
import com.mision.biihlive.domain.users.model.UserPreview
import kotlinx.coroutines.flow.Flow

/**
 * Interface del repositorio de usuarios
 */
interface IUsersRepository {
    
    /**
     * Obtener todos los usuarios disponibles para chat
     */
    suspend fun getAllUsers(): Result<List<UserPreview>>
    
    /**
     * Buscar usuarios por nombre o nickname
     */
    suspend fun searchUsers(query: String): Result<List<UserPreview>>
    
    /**
     * Obtener detalles completos de un usuario
     */
    suspend fun getUserById(userId: String): Result<User>
    
    /**
     * Obtener usuarios online
     */
    suspend fun getOnlineUsers(): Result<List<UserPreview>>
    
    /**
     * Observar cambios en la lista de usuarios
     */
    fun observeUsers(): Flow<List<UserPreview>>
}