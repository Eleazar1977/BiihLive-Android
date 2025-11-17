package com.mision.biihlive.domain.chat.usecase

import com.mision.biihlive.domain.chat.model.Message
import com.mision.biihlive.domain.chat.model.MessageType
import com.mision.biihlive.domain.chat.repository.IChatRepository

/**
 * Caso de uso para enviar mensajes
 */
class SendMessageUseCase(
    private val chatRepository: IChatRepository
) {
    suspend operator fun invoke(
        chatId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        mediaUrl: String? = null
    ): Result<Message> {
        // Validaciones
        if (content.isBlank() && mediaUrl == null) {
            return Result.failure(IllegalArgumentException("El mensaje no puede estar vacío"))
        }
        
        if (content.length > 5000) {
            return Result.failure(IllegalArgumentException("El mensaje es demasiado largo"))
        }
        
        // Enviar mensaje
        return chatRepository.sendMessage(chatId, content, type, mediaUrl)
    }
    
    suspend fun sendMediaMessage(
        chatId: String,
        filePath: String,
        mediaType: MessageType,
        caption: String = ""
    ): Result<Message> {
        // Primero subir el archivo
        return chatRepository.uploadMedia(filePath, mediaType)
            .fold(
                onSuccess = { mediaUrl ->
                    // Luego enviar el mensaje con la URL del media
                    invoke(chatId, caption, mediaType, mediaUrl)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
    }
}