package com.mision.biihlive.data.chat.datasource

import com.mision.biihlive.data.chat.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * DataSource para comunicación con API Gateway
 * DEPRECATED: Sistema de chat será reimplementado con AppSync GraphQL
 * Esta clase será eliminada en futuras versiones
 * Mantener temporalmente hasta implementar nuevo sistema de chat
 */
@Deprecated(
    message = "Sistema de chat será reimplementado con AppSync GraphQL",
    level = DeprecationLevel.WARNING
)
class ChatRemoteDataSource {

    companion object {
        // TODO: Eliminar cuando se migre a AppSync GraphQL
        // TEMPORAL: Usando REST API hasta completar migración
        private const val BASE_URL = "https://u5izbom7kc.execute-api.eu-west-3.amazonaws.com/prod"
    }
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }
    
    // Chats
    suspend fun getChats(userId: String): Result<ChatListResponse> {
        return try {
            val response = client.get("$BASE_URL/chats") {
                parameter("userId", userId)
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<ChatListResponse>())
            } else if (response.status == HttpStatusCode.NotFound) {
                // Temporal: Si el endpoint no existe, retornar lista vacía
                println("Endpoint /chats no encontrado (404), retornando lista vacía temporalmente")
                Result.success(ChatListResponse(chats = emptyList(), totalUnread = 0))
            } else {
                Result.failure(Exception("Error al obtener chats: ${response.status}"))
            }
        } catch (e: Exception) {
            // Si es un 404, retornar lista vacía temporalmente
            if (e.message?.contains("404") == true) {
                println("Endpoint /chats no implementado aún, retornando lista vacía")
                Result.success(ChatListResponse(chats = emptyList(), totalUnread = 0))
            } else {
                println("Error al obtener chats: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    suspend fun getChatById(chatId: String): Result<ChatDto> {
        return try {
            val response = client.get("$BASE_URL/chats/$chatId") {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            }
            
            if (response.status.isSuccess()) {
                Result.success(response.body<ChatDto>())
            } else {
                Result.failure(Exception("Error al obtener chat: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createChat(request: CreateChatRequest): Result<ChatDto> {
        return try {
            val response = client.post("$BASE_URL/chats") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            if (response.status.isSuccess()) {
                Result.success(response.body<ChatDto>())
            } else {
                Result.failure(Exception("Error al crear chat: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteChat(chatId: String): Result<Unit> {
        return try {
            val response = client.delete("$BASE_URL/chats/$chatId")
            
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al eliminar chat: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Mensajes
    suspend fun getMessages(chatId: String, limit: Int = 20, beforeTimestamp: Long? = null): Result<MessagesResponse> {
        return try {
            val response = client.get("$BASE_URL/chats/$chatId/messages") {
                parameter("limit", limit)
                beforeTimestamp?.let { parameter("beforeTimestamp", it) }
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<MessagesResponse>())
            } else {
                Result.failure(Exception("Error al obtener mensajes: ${response.status}"))
            }
        } catch (e: Exception) {
            println("Error al obtener mensajes: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun sendMessage(request: SendMessageRequest): Result<MessageDto> {
        return try {
            val response = client.post("$BASE_URL/chats/${request.chatId}/messages") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            if (response.status.isSuccess()) {
                Result.success(response.body<MessageDto>())
            } else {
                Result.failure(Exception("Error al enviar mensaje: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun markAsRead(chatId: String, messageIds: List<String>): Result<Unit> {
        return try {
            val response = client.put("$BASE_URL/chats/$chatId/read") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("messageIds" to messageIds))
            }
            
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al marcar como leído: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun setTypingStatus(chatId: String, userId: String, isTyping: Boolean): Result<Unit> {
        return try {
            val response = client.post("$BASE_URL/chats/$chatId/typing") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "userId" to userId,
                    "isTyping" to isTyping
                ))
            }
            
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al actualizar estado de escritura: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}