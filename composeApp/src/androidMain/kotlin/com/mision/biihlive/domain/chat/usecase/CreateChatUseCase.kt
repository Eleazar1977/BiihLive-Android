package com.mision.biihlive.domain.chat.usecase

import com.mision.biihlive.domain.chat.model.Chat
import com.mision.biihlive.domain.chat.repository.IChatRepository

/**
 * Caso de uso para crear o abrir un chat existente
 */
class CreateChatUseCase(
    private val chatRepository: IChatRepository
) {
    /**
     * Crea un nuevo chat o devuelve el existente si ya hay uno entre estos usuarios
     */
    suspend operator fun invoke(
        currentUserId: String,
        otherUserId: String
    ): Result<Chat> {
        // Generar el chatId consistente con el backend (ordenando los IDs alfabéticamente)
        val expectedChatId = listOf(currentUserId, otherUserId).sorted().joinToString("_")

        // Primero buscar si ya existe un chat entre estos usuarios
        return chatRepository.getChats(currentUserId)
            .fold(
                onSuccess = { chats ->
                    // Buscar un chat existente con el chatId esperado
                    val existingChat = chats.find { chat ->
                        // Comparar directamente con el chatId generado
                        chat.chatId == expectedChatId
                    }

                    if (existingChat != null) {
                        // Si existe, devolver el chat existente
                        chatRepository.getChatById(existingChat.chatId)
                    } else {
                        // Si no existe, crear uno nuevo
                        chatRepository.createChat(
                            participantIds = listOf(currentUserId, otherUserId),
                            isGroup = false
                        )
                    }
                },
                onFailure = { error ->
                    // Si falla obtener los chats, intentar crear uno nuevo
                    chatRepository.createChat(
                        participantIds = listOf(currentUserId, otherUserId),
                        isGroup = false
                    )
                }
            )
    }
}