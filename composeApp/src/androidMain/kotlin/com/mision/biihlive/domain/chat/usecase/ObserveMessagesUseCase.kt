package com.mision.biihlive.domain.chat.usecase

import com.mision.biihlive.domain.chat.model.Message
import com.mision.biihlive.domain.chat.repository.IChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Caso de uso para observar mensajes en tiempo real
 */
class ObserveMessagesUseCase(
    private val chatRepository: IChatRepository
) {
    operator fun invoke(chatId: String): Flow<List<Message>> {
        return chatRepository.observeMessages(chatId)
            .map { messages ->
                // Ordenar mensajes por timestamp
                messages.sortedBy { it.timestamp }
            }
    }
    
    fun observeUnreadCount(userId: String): Flow<Int> {
        return chatRepository.observeUnreadCount(userId)
    }
    
    fun observeTypingStatus(chatId: String): Flow<Map<String, Boolean>> {
        return chatRepository.observeTypingStatus(chatId)
    }
}