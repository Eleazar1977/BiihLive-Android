package com.mision.biihlive.domain.chat.usecase

import com.mision.biihlive.domain.chat.model.ChatPreview
import com.mision.biihlive.domain.chat.repository.IChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Caso de uso para obtener la lista de chats del usuario
 */
class GetChatsUseCase(
    private val chatRepository: IChatRepository
) {
    suspend operator fun invoke(userId: String): Flow<Result<List<ChatPreview>>> = flow {
        emit(Result.success(emptyList())) // Estado inicial
        
        chatRepository.getChats(userId)
            .onSuccess { chats ->
                // Ordenar chats: primero los fijados, luego por última actividad
                val sortedChats = chats.sortedWith(
                    compareByDescending<ChatPreview> { it.isPinned }
                        .thenByDescending { it.lastMessageTime ?: 0 }
                )
                emit(Result.success(sortedChats))
            }
            .onFailure { error ->
                emit(Result.failure(error))
            }
    }
    
    suspend fun filterByType(userId: String, userType: com.mision.biihlive.domain.chat.model.UserType): Result<List<ChatPreview>> {
        return chatRepository.getChatsByType(userType)
    }
}