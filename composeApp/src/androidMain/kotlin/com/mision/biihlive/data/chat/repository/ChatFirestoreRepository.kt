package com.mision.biihlive.data.chat.repository

import android.content.Context
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.mision.biihlive.config.FirebaseConfig
import com.mision.biihlive.core.managers.UserIdManager
import com.mision.biihlive.data.repository.ProfileImageRepository
import com.mision.biihlive.domain.chat.model.*
import com.mision.biihlive.domain.chat.repository.IChatRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

/**
 * Implementación Firebase del repositorio de chat
 * Reemplaza la implementación AWS deprecated
 */
class ChatFirestoreRepository(
    private val context: Context,
    private val profileImageRepository: ProfileImageRepository = ProfileImageRepository(context)
) : IChatRepository {

    companion object {
        private const val TAG = "ChatFirestoreRepository"
        private const val CHATS_COLLECTION = "chats"
        private const val MESSAGES_COLLECTION = "messages"
        private const val USERS_COLLECTION = "users"
        private const val USER_STATS_COLLECTION = "userStats"
        private const val PRESENCE_COLLECTION = "presence"
        private const val CHAT_SETTINGS_COLLECTION = "chatSettings"

        // Constantes para URLs de imágenes
        private const val CLOUDFRONT_URL = "https://d183hg75gdabnr.cloudfront.net"
        private const val DEFAULT_TIMESTAMP = "1759240530172"
    }

    private val firestore = FirebaseConfig.getFirestore()

    private suspend fun getCurrentUserId(): String {
        return UserIdManager.getInstance(context).getCurrentUserId()
    }

    /**
     * Fuerza una actualización de mensajes desde el servidor para resolver problemas de cache
     */
    private suspend fun forceRefreshMessagesFromServer(chatId: String) {
        try {
            // Realizar una consulta forzada al servidor para "calentar" el cache con datos actualizados
            val messagesFromServer = firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1) // Solo necesitamos un mensaje para forzar la actualización
                .get(Source.SERVER) // Forzar consulta al servidor
                .await()

            Log.d(TAG, "🔄 Actualización forzada completada. Documentos obtenidos: ${messagesFromServer.size()}")
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo forzar actualización desde servidor: ${e.message}")
        }
    }

    /**
     * Función de debugging para verificar datos reales en el servidor
     */
    suspend fun debugVerifyServerData(chatId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // MOSTRAR EXACTAMENTE QUE BASE DE DATOS ESTÁ SIENDO CONSULTADA
            val dbInfo = firestore.app.options.databaseUrl ?: "unknown"
            val projectId = firestore.app.options.projectId ?: "unknown"

            Log.e(TAG, "🚨 [VERIFICACION CRITICA] CONSULTANDO BASE DE DATOS:")
            Log.e(TAG, "🚨 Project ID: $projectId")
            Log.e(TAG, "🚨 Database URL: $dbInfo")
            Log.e(TAG, "🚨 Firestore instance: ${firestore.javaClass.simpleName}")
            Log.e(TAG, "🚨 Chat ID consultado: $chatId")
            Log.e(TAG, "🚨 Path completo: $CHATS_COLLECTION/$chatId/$MESSAGES_COLLECTION")

            Log.d(TAG, "🔍 [DEBUG] Verificando datos reales en servidor para chat: $chatId")
            Log.d(TAG, "🔍 [DEBUG] Base de datos: basebiihlive")
            Log.d(TAG, "🔍 [DEBUG] Colección: $CHATS_COLLECTION")

            // Forzar consulta directa al servidor
            val serverSnapshot = firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_COLLECTION)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get(Source.SERVER)
                .await()

            val debugInfo = buildString {
                appendLine("=== DATOS REALES EN SERVIDOR ===")
                appendLine("ChatId: $chatId")
                appendLine("Total documentos en servidor: ${serverSnapshot.size()}")
                appendLine()

                if (serverSnapshot.isEmpty) {
                    appendLine("❌ NO HAY MENSAJES EN EL SERVIDOR")
                } else {
                    serverSnapshot.documents.forEachIndexed { index, doc ->
                        appendLine("📄 Documento [$index]: ${doc.id}")
                        appendLine("   - text: '${doc.getString("text") ?: "null"}'")
                        appendLine("   - senderId: ${doc.getString("senderId") ?: "null"}")
                        appendLine("   - isDeleted: ${doc.getBoolean("isDeleted") ?: false}")
                        appendLine("   - timestamp: ${doc.getTimestamp("timestamp")}")
                        appendLine("   - exists: ${doc.exists()}")
                        appendLine()
                    }
                }
            }

            Log.d(TAG, debugInfo)
            Result.success(debugInfo)
        } catch (e: Exception) {
            val error = "Error verificando datos del servidor: ${e.message}"
            Log.e(TAG, error)
            Result.failure(Exception(error))
        }
    }

    /**
     * Limpia cache local y fuerza datos desde servidor
     */
    suspend fun clearCacheAndForceServerData(chatId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🧹 Limpiando cache y forzando datos del servidor para chat: $chatId")

            // Intentar limpiar cache (Firebase no permite limpieza selectiva, pero podemos intentar)
            try {
                firestore.clearPersistence().await()
                Log.d(TAG, "🧹 Cache de persistencia limpiado")
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo limpiar persistencia (app probablemente en uso): ${e.message}")
            }

            // Forzar nueva consulta desde servidor
            forceRefreshMessagesFromServer(chatId)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando cache: ${e.message}")
            Result.failure(e)
        }
    }

    // =============== OPERACIONES DE CHAT ===============

    override suspend fun getChats(userId: String): Result<List<ChatPreview>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Obteniendo chats para usuario: $userId")

            // Consulta simplificada para evitar problemas de índices
            val snapshot = firestore.collection(CHATS_COLLECTION)
                .whereArrayContains("participants", userId)
                .get()
                .await()

            val chatPreviews = snapshot.documents
                .filter { doc ->
                    // Filtrar manualmente por isActive
                    val isActive = doc.getBoolean("isActive") ?: true
                    isActive
                }
                .mapNotNull { doc ->
                try {
                    val chatData = doc.data ?: return@mapNotNull null
                    val participants = chatData["participants"] as? List<String> ?: emptyList()
                    val lastMessage = chatData["lastMessage"] as? Map<String, Any>

                    // Obtener el otro usuario (para chats 1-a-1)
                    val otherUserId = participants.firstOrNull { it != userId }
                    val displayName = if (chatData["type"] == "group") {
                        chatData["name"] as? String ?: "Grupo"
                    } else {
                        // Obtener nickname del otro usuario desde Firestore
                        getUserDisplayName(otherUserId ?: "")
                    }

                    val displayImage = if (chatData["type"] == "group") {
                        chatData["avatar"] as? String
                    } else {
                        // Generar URL de imagen desde S3
                        generateUserImageUrl(otherUserId ?: "")
                    }

                    val participantData = chatData["participantData"] as? Map<String, Any> ?: emptyMap()
                    val userParticipantData = participantData[userId] as? Map<String, Any> ?: emptyMap()

                    // Obtener estado en línea y configuración de visibilidad para el otro usuario
                    val (isOnline, allowsStatusVisible) = if (otherUserId != null && otherUserId.isNotEmpty()) {
                        getUserOnlineStatus(otherUserId)
                    } else {
                        Pair(false, false)
                    }

                    ChatPreview(
                        chatId = doc.id,
                        displayName = displayName,
                        displayImage = displayImage,
                        lastMessageText = lastMessage?.get("text") as? String,
                        lastMessageTime = (lastMessage?.get("timestamp") as? Timestamp)?.toDate()?.time,
                        unreadCount = calculateUnreadCount(doc.id, userId),
                        isPinned = userParticipantData["pinned"] as? Boolean ?: false,
                        isMuted = userParticipantData["muted"] as? Boolean ?: false,
                        userType = UserType.REGULAR, // TODO: Determinar desde datos del usuario
                        isOnline = isOnline,
                        allowsStatusVisible = allowsStatusVisible
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error procesando chat ${doc.id}: ${e.message}")
                    null
                }
            }
            .sortedByDescending { chatPreview ->
                // Ordenar manualmente por updatedAt (timestamp del último mensaje)
                chatPreview.lastMessageTime ?: 0L
            }

            Log.d(TAG, "✅ Chats obtenidos: ${chatPreviews.size}")
            Result.success(chatPreviews)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo chats: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getChatById(chatId: String): Result<Chat> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Obteniendo chat: $chatId")

            val snapshot = firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .get()
                .await()

            if (!snapshot.exists()) {
                return@withContext Result.failure(Exception("Chat no encontrado"))
            }

            val chatData = snapshot.data!!
            val participants = chatData["participants"] as? List<String> ?: emptyList()
            val lastMessage = chatData["lastMessage"] as? Map<String, Any>

            // Convertir participantes a ChatUser
            val chatUsers = participants.mapNotNull { userId ->
                try {
                    ChatUser(
                        userId = userId,
                        nickname = getUserDisplayName(userId),
                        imageUrl = generateUserImageUrl(userId),
                        lastSeen = System.currentTimeMillis(),
                        userType = UserType.REGULAR
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error obteniendo datos de usuario $userId: ${e.message}")
                    null
                }
            }

            val chat = Chat(
                id = chatId,
                participants = chatUsers,
                lastMessage = lastMessage?.let { convertToMessage(it, chatId) },
                unreadCount = calculateUnreadCount(chatId, getCurrentUserId()),
                createdAt = (chatData["createdAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                updatedAt = (chatData["updatedAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                isGroup = chatData["type"] == "group",
                groupName = chatData["name"] as? String,
                groupImage = chatData["avatar"] as? String,
                isPinned = false, // TODO: Obtener de participantData
                isMuted = false, // TODO: Obtener de participantData
                isArchived = false, // TODO: Obtener de participantData
                chatType = if (chatData["type"] == "group") ChatType.GROUP else ChatType.DIRECT
            )

            Log.d(TAG, "✅ Chat obtenido: ${chat.id}")
            Result.success(chat)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo chat $chatId: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun createChat(participantIds: List<String>, isGroup: Boolean): Result<Chat> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId()
            val allParticipants = (participantIds + currentUserId).distinct()

            Log.d(TAG, "🔄 Creando chat con participantes: $allParticipants, isGroup: $isGroup")

            // Para chats 1-a-1, verificar si ya existe
            if (!isGroup && allParticipants.size == 2) {
                val existingChatId = generateDirectChatId(allParticipants[0], allParticipants[1])
                val existingChat = firestore.collection(CHATS_COLLECTION)
                    .document(existingChatId)
                    .get()
                    .await()

                if (existingChat.exists()) {
                    Log.d(TAG, "✅ Chat 1-a-1 ya existe: $existingChatId")
                    return@withContext getChatById(existingChatId)
                }
            }

            val chatId = if (isGroup) {
                "group_${UUID.randomUUID()}_${System.currentTimeMillis()}"
            } else {
                generateDirectChatId(allParticipants[0], allParticipants[1])
            }

            val timestamp = Timestamp.now()

            // Crear participantData
            val participantData = allParticipants.associateWith { userId ->
                mapOf(
                    "role" to if (userId == currentUserId && isGroup) "admin" else "member",
                    "joinedAt" to timestamp,
                    "lastReadMessageId" to "",
                    "unreadCount" to 0,
                    "notifications" to true,
                    "archived" to false,
                    "pinned" to false,
                    "muted" to false
                )
            }

            val chatData = mapOf(
                "type" to if (isGroup) "group" else "direct",
                "name" to if (isGroup) "Nuevo Grupo" else "",
                "description" to "",
                "avatar" to "",
                "participants" to allParticipants,
                "participantData" to participantData,
                "lastMessage" to null,
                "createdAt" to timestamp,
                "updatedAt" to timestamp,
                "isActive" to true
            )

            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .set(chatData)
                .await()

            // Actualizar userStats para todos los participantes
            updateUserChatStats(allParticipants, 1)

            Log.d(TAG, "✅ Chat creado: $chatId")
            getChatById(chatId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando chat: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun deleteChat(chatId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .update("isActive", false)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun archiveChat(chatId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId()
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .update("participantData.${currentUserId}.archived", true)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pinChat(chatId: String, isPinned: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId()
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .update("participantData.${currentUserId}.pinned", isPinned)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun muteChat(chatId: String, isMuted: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId()
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .update("participantData.${currentUserId}.muted", isMuted)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =============== OPERACIONES DE MENSAJES ===============

    override suspend fun getMessages(chatId: String, limit: Int, beforeTimestamp: Long?): Result<List<Message>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Obteniendo mensajes del chat: $chatId")

            // Consulta simplificada para evitar problemas de índices
            var query = firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            if (beforeTimestamp != null) {
                query = query.startAfter(Timestamp(java.util.Date(beforeTimestamp)))
            }

            val snapshot = query.get().await()

            val messages = snapshot.documents
                .filter { doc ->
                    // Filtrar manualmente mensajes no eliminados
                    val isDeleted = doc.getBoolean("isDeleted") ?: false
                    !isDeleted
                }
                .mapNotNull { doc ->
                    convertFirestoreToMessage(doc)
                }.reversed() // Invertir para orden cronológico

            Log.d(TAG, "✅ Mensajes obtenidos: ${messages.size}")
            Result.success(messages)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo mensajes: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(
        chatId: String,
        content: String,
        type: MessageType,
        mediaUrl: String?
    ): Result<Message> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId()
            val messageId = UUID.randomUUID().toString()
            val timestamp = Timestamp.now()

            Log.d(TAG, "📤 Enviando mensaje en chat: $chatId")

            val messageData = mapOf(
                "chatId" to chatId,
                "senderId" to currentUserId,
                "text" to content,
                "type" to type.name.lowercase(),
                "mediaUrl" to (mediaUrl ?: ""),
                "timestamp" to timestamp,
                "status" to mapOf(
                    "sent" to timestamp,
                    "delivered" to null,
                    "read" to emptyMap<String, Any>()
                ),
                "isDeleted" to false,
                "deletedFor" to emptyList<String>(),
                "reactions" to emptyMap<String, List<String>>(),
                "replyTo" to "",
                "editedAt" to null
            )

            // Batch write para atomicidad
            val batch = firestore.batch()

            // Crear mensaje
            val messageRef = firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_COLLECTION)
                .document(messageId)
            batch.set(messageRef, messageData)

            // Actualizar último mensaje del chat
            val chatRef = firestore.collection(CHATS_COLLECTION).document(chatId)
            val lastMessage = mapOf(
                "id" to messageId,
                "text" to if (type == MessageType.TEXT) content else getMediaPreview(type),
                "senderId" to currentUserId,
                "timestamp" to timestamp,
                "type" to type.name.lowercase()
            )

            // Obtener participantes del chat para actualizar contadores de no leídos
            val chatDoc = chatRef.get().await()
            val participants = chatDoc.get("participants") as? List<String> ?: emptyList()

            // Usar set con merge para manejar chats nuevos y existentes
            batch.set(chatRef, mapOf(
                "lastMessage" to lastMessage,
                "updatedAt" to timestamp
            ), SetOptions.merge())

            // Incrementar unreadCount para todos los participantes excepto el emisor
            participants.forEach { participantId ->
                if (participantId != currentUserId) {
                    batch.update(chatRef, "participantData.${participantId}.unreadCount", FieldValue.increment(1))
                }
            }

            batch.commit().await()

            // Crear objeto Message para retornar
            val message = Message(
                id = messageId,
                chatId = chatId,
                senderId = currentUserId,
                senderName = getUserDisplayName(currentUserId),
                content = content,
                type = type,
                timestamp = timestamp.toDate().time,
                isRead = false,
                isDelivered = false,
                status = MessageStatus.SENT,
                mediaUrl = mediaUrl,
                thumbnailUrl = null,
                replyTo = null,
                replyToMessageId = null,
                isEdited = false,
                editedAt = null
            )

            Log.d(TAG, "✅ Mensaje enviado: $messageId")
            Result.success(message)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enviando mensaje: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId()

            // Obtener el mensaje para saber a qué chat pertenece
            val allChats = firestore.collection(CHATS_COLLECTION)
                .get(Source.SERVER) // Forzar consulta al servidor para evitar cache
                .await()
            var messageRef: DocumentReference? = null
            var chatId = ""

            for (chatDoc in allChats.documents) {
                val messageQuery = chatDoc.reference.collection(MESSAGES_COLLECTION)
                    .document(messageId)
                    .get(Source.SERVER) // Forzar consulta al servidor
                    .await()

                if (messageQuery.exists()) {
                    messageRef = messageQuery.reference
                    chatId = chatDoc.id
                    break
                }
            }

            if (messageRef == null) {
                return@withContext Result.failure(Exception("Mensaje no encontrado"))
            }

            // Marcar mensaje como eliminado (soft delete)
            messageRef.update(
                mapOf(
                    "isDeleted" to true,
                    "deletedBy" to currentUserId,
                    "deletedAt" to Timestamp.now()
                )
            ).await()

            Log.d(TAG, "✅ Mensaje eliminado: $messageId")

            // Forzar actualización desde el servidor para asegurar consistencia
            try {
                Log.d(TAG, "🔄 Forzando actualización desde servidor para chat: $chatId")
                forceRefreshMessagesFromServer(chatId)
            } catch (e: Exception) {
                Log.w(TAG, "Warning: No se pudo forzar actualización: ${e.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando mensaje: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun editMessage(messageId: String, newContent: String): Result<Message> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId()

            // Obtener el mensaje para saber a qué chat pertenece
            val allChats = firestore.collection(CHATS_COLLECTION).get().await()
            var messageRef: DocumentReference? = null
            var chatId = ""
            var messageData: DocumentSnapshot? = null

            for (chatDoc in allChats.documents) {
                val messageQuery = chatDoc.reference.collection(MESSAGES_COLLECTION)
                    .document(messageId)
                    .get()
                    .await()

                if (messageQuery.exists()) {
                    messageRef = messageQuery.reference
                    chatId = chatDoc.id
                    messageData = messageQuery
                    break
                }
            }

            if (messageRef == null || messageData == null) {
                return@withContext Result.failure(Exception("Mensaje no encontrado"))
            }

            // Verificar que el usuario es el dueño del mensaje
            val senderId = messageData.getString("senderId")
            if (senderId != currentUserId) {
                return@withContext Result.failure(Exception("No tienes permisos para editar este mensaje"))
            }

            val editedAt = Timestamp.now()

            // Actualizar mensaje
            messageRef.update(
                mapOf(
                    "text" to newContent,
                    "editedAt" to editedAt,
                    "isEdited" to true
                )
            ).await()

            // Crear objeto Message actualizado
            val updatedMessage = Message(
                id = messageId,
                chatId = chatId,
                senderId = currentUserId,
                senderName = getUserDisplayName(currentUserId),
                content = newContent,
                type = MessageType.valueOf((messageData.getString("type") ?: "text").uppercase()),
                timestamp = (messageData.getTimestamp("timestamp"))?.toDate()?.time ?: System.currentTimeMillis(),
                isRead = false, // TODO: Calcular desde status.read
                isDelivered = false, // TODO: Calcular desde status.delivered
                status = MessageStatus.SENT,
                mediaUrl = messageData.getString("mediaUrl"),
                thumbnailUrl = null,
                replyTo = messageData.getString("replyTo"),
                replyToMessageId = null,
                isEdited = true,
                editedAt = editedAt.toDate().time
            )

            Log.d(TAG, "✅ Mensaje editado: $messageId")
            Result.success(updatedMessage)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error editando mensaje: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(chatId: String, messageIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId()
            val timestamp = Timestamp.now()

            val batch = firestore.batch()

            messageIds.forEach { messageId ->
                val messageRef = firestore.collection(CHATS_COLLECTION)
                    .document(chatId)
                    .collection(MESSAGES_COLLECTION)
                    .document(messageId)

                batch.update(messageRef, "status.read.$currentUserId", timestamp)
            }

            // Actualizar último mensaje leído y resetear contador de no leídos
            val chatRef = firestore.collection(CHATS_COLLECTION).document(chatId)
            batch.update(chatRef, "participantData.${currentUserId}.lastReadMessageId", messageIds.last())
            batch.update(chatRef, "participantData.${currentUserId}.unreadCount", 0)

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =============== OBSERVADORES EN TIEMPO REAL ===============

    override fun observeChat(chatId: String): Flow<Chat> = callbackFlow<Chat> {
        val listener = firestore.collection(CHATS_COLLECTION)
            .document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observando chat: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot?.exists() == true) {
                    try {
                        val chatData = snapshot.data!!
                        val participants = chatData["participants"] as? List<String> ?: emptyList()
                        val lastMessage = chatData["lastMessage"] as? Map<String, Any>

                        // Convertir participantes a ChatUser usando suspendCancellableCoroutine
                        kotlinx.coroutines.runBlocking {
                            val chatUsers = participants.mapNotNull { userId ->
                                try {
                                    ChatUser(
                                        userId = userId,
                                        nickname = getUserDisplayName(userId),
                                        imageUrl = generateUserImageUrl(userId),
                                        lastSeen = System.currentTimeMillis(),
                                        userType = UserType.REGULAR
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error obteniendo datos de usuario $userId: ${e.message}")
                                    null
                                }
                            }

                            val chat = Chat(
                                id = chatId,
                                participants = chatUsers,
                                lastMessage = lastMessage?.let { convertToMessage(it, chatId) },
                                unreadCount = kotlinx.coroutines.runBlocking { calculateUnreadCount(chatId, getCurrentUserId()) },
                                createdAt = (chatData["createdAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                                updatedAt = (chatData["updatedAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                                isGroup = chatData["type"] == "group",
                                groupName = chatData["name"] as? String,
                                groupImage = chatData["avatar"] as? String,
                                isPinned = false, // TODO: Obtener de participantData
                                isMuted = false, // TODO: Obtener de participantData
                                isArchived = false, // TODO: Obtener de participantData
                                chatType = if (chatData["type"] == "group") ChatType.GROUP else ChatType.DIRECT
                            )

                            trySend(chat)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error procesando snapshot de chat: ${e.message}")
                        close(e)
                    }
                }
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    override fun observeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        Log.d(TAG, "🔄 Iniciando observación de mensajes para chat: $chatId")
        Log.d(TAG, "🔄 Usando base de datos: basebiihlive")
        Log.d(TAG, "🔄 MODO DEBUG: Forzando datos desde servidor")

        // Primero hacer una consulta inicial desde el servidor para asegurar datos reales
        launch {
            try {
                debugVerifyServerData(chatId)
            } catch (e: Exception) {
                Log.e(TAG, "Error en verificación inicial: ${e.message}")
            }
        }

        val listener = firestore.collection(CHATS_COLLECTION)
            .document(chatId)
            .collection(MESSAGES_COLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error observando mensajes: ${error.message}")
                    Log.e(TAG, "❌ ChatId: $chatId, Collection: $CHATS_COLLECTION")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val isFromCache = snapshot.metadata.isFromCache

                    Log.d(TAG, "📱 Snapshot recibido para chat: $chatId")
                    Log.d(TAG, "📱 Total documentos en snapshot: ${snapshot.documents.size}")
                    Log.d(TAG, "📱 Metadatos - FromCache: $isFromCache")

                    // SI LOS DATOS VIENEN DE CACHE Y HAY DOCUMENTOS, RECHAZARLOS
                    if (isFromCache && snapshot.documents.isNotEmpty()) {
                        Log.w(TAG, "⚠️ DATOS DE CACHE DETECTADOS - Ignorando ${snapshot.documents.size} documentos")
                        // NO enviar datos de cache, esperar datos del servidor
                        return@addSnapshotListener
                    }

                    // Detailed logging of each document
                    snapshot.documents.forEachIndexed { index, doc ->
                        val isDeleted = doc.getBoolean("isDeleted") ?: false
                        val senderId = doc.getString("senderId") ?: "unknown"
                        val text = doc.getString("text") ?: "no-text"
                        val timestamp = doc.getTimestamp("timestamp")

                        Log.d(TAG, "📄 Documento [$index]: ${doc.id}")
                        Log.d(TAG, "   - text: '$text'")
                        Log.d(TAG, "   - senderId: $senderId")
                        Log.d(TAG, "   - isDeleted: $isDeleted")
                        Log.d(TAG, "   - timestamp: $timestamp")
                        Log.d(TAG, "   - exists: ${doc.exists()}")
                    }

                    val messages = snapshot.documents
                        .filter { doc ->
                            // Filtrar manualmente mensajes no eliminados
                            val isDeleted = doc.getBoolean("isDeleted") ?: false
                            if (isDeleted) {
                                Log.d(TAG, "🗑️ Filtrando mensaje eliminado: ${doc.id}")
                            }
                            !isDeleted
                        }
                        .mapNotNull { doc ->
                            try {
                                val message = convertFirestoreToMessage(doc)
                                if (message != null) {
                                    Log.d(TAG, "✅ Mensaje convertido: ${message.id} - '${message.content}'")
                                    message
                                } else {
                                    Log.w(TAG, "⚠️ Mensaje null después de conversión: ${doc.id}")
                                    null
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error convirtiendo mensaje ${doc.id}: ${e.message}")
                                null
                            }
                        }

                    Log.d(TAG, "📱 Mensajes válidos finales: ${messages.size} (de ${snapshot.documents.size} documentos)")
                    Log.d(TAG, "📱 Fuente de datos: ${if (isFromCache) "CACHE" else "SERVIDOR"}")
                    trySend(messages)
                }
            }

        awaitClose {
            Log.d(TAG, "🔴 Deteniendo observación de mensajes para chat: $chatId")
            listener.remove()
        }
    }.flowOn(Dispatchers.IO)

    override fun observeTypingStatus(chatId: String): Flow<Map<String, Boolean>> {
        TODO("Implementar observador de estado escribiendo")
    }

    override fun observeUnreadCount(userId: String): Flow<Int> = callbackFlow {
        Log.d(TAG, "🔔 Iniciando observador de mensajes no leídos para usuario: $userId")

        val listener = firestore.collection(CHATS_COLLECTION)
            .whereArrayContains("participants", userId)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observando chats para contar no leídos: ${error.message}")
                    trySend(0)
                    return@addSnapshotListener
                }

                var totalUnreadCount = 0

                snapshot?.documents?.forEach { doc ->
                    try {
                        val chatId = doc.id
                        val participantData = doc.get("participantData") as? Map<String, Any>
                        val userParticipantData = participantData?.get(userId) as? Map<String, Any>

                        // Verificar si el chat está silenciado
                        val isMuted = userParticipantData?.get("muted") as? Boolean ?: false

                        if (!isMuted) {
                            val lastReadMessageId = userParticipantData?.get("lastReadMessageId") as? String ?: ""

                            if (lastReadMessageId.isEmpty()) {
                                // Si no hay último mensaje leído, contar todos los mensajes del chat que no sean del usuario
                                val lastMessage = doc.get("lastMessage") as? Map<String, Any>
                                val lastMessageSenderId = lastMessage?.get("senderId") as? String

                                if (lastMessageSenderId != null && lastMessageSenderId != userId) {
                                    totalUnreadCount += 1
                                }
                            } else {
                                // TODO: Implementar conteo más preciso basado en lastReadMessageId
                                // Por ahora, usamos una aproximación simple
                                val lastMessage = doc.get("lastMessage") as? Map<String, Any>
                                val lastMessageSenderId = lastMessage?.get("senderId") as? String
                                val lastMessageId = lastMessage?.get("id") as? String

                                if (lastMessageSenderId != null &&
                                    lastMessageSenderId != userId &&
                                    lastMessageId != null &&
                                    lastMessageId != lastReadMessageId) {
                                    totalUnreadCount += 1
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error calculando no leídos para chat ${doc.id}: ${e.message}")
                    }
                }

                Log.d(TAG, "🔔 Total mensajes no leídos: $totalUnreadCount")
                trySend(totalUnreadCount)
            }

        awaitClose {
            Log.d(TAG, "🔔 Cerrando observador de mensajes no leídos")
            listener.remove()
        }
    }.flowOn(Dispatchers.IO)

    // =============== BÚSQUEDA Y FILTROS ===============

    override suspend fun searchChats(query: String): Result<List<ChatPreview>> {
        TODO("Implementar búsqueda de chats")
    }

    override suspend fun searchMessages(chatId: String, query: String): Result<List<Message>> {
        TODO("Implementar búsqueda de mensajes")
    }

    override suspend fun getChatsByType(userType: UserType): Result<List<ChatPreview>> {
        TODO("Implementar filtro por tipo de usuario")
    }

    // =============== OPERACIONES DE GRUPO ===============

    override suspend fun addParticipant(chatId: String, userId: String): Result<Unit> {
        TODO("Implementar agregar participante")
    }

    override suspend fun removeParticipant(chatId: String, userId: String): Result<Unit> {
        TODO("Implementar remover participante")
    }

    override suspend fun updateGroupInfo(chatId: String, name: String?, imageUrl: String?): Result<Unit> {
        TODO("Implementar actualización de info de grupo")
    }

    // =============== TYPING INDICATORS ===============

    override suspend fun setTypingStatus(chatId: String, isTyping: Boolean): Result<Unit> {
        TODO("Implementar indicador de escribiendo")
    }

    // =============== MEDIA ===============

    override suspend fun uploadMedia(filePath: String, mediaType: MessageType): Result<String> {
        TODO("Implementar subida de archivos usando S3ClientProvider")
    }

    // =============== FUNCIONES AUXILIARES ===============

    private fun generateDirectChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "chat_${userId1}_${userId2}"
        } else {
            "chat_${userId2}_${userId1}"
        }
    }

    private suspend fun getUserDisplayName(userId: String): String {
        return try {
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            userDoc.getString("nickname") ?: userDoc.getString("nombre") ?: "Usuario"
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo nombre de usuario $userId: ${e.message}")
            "Usuario"
        }
    }

    private fun generateUserImageUrl(userId: String): String? {
        return try {
            generateThumbnailUrl(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error generando URL de imagen para $userId: ${e.message}")
            null
        }
    }

    fun generateThumbnailUrl(userId: String): String {
        return "$CLOUDFRONT_URL/userprofile/$userId/thumbnail_$DEFAULT_TIMESTAMP.png"
    }

    /**
     * Observa en tiempo real el estado de presencia de un usuario
     * Emite cuando el estado online/offline cambia
     */
    fun observeUserPresence(userId: String): Flow<Pair<Boolean, Boolean>> = callbackFlow {
        Log.d(TAG, "🔴 Iniciando observación de presencia en tiempo real para: $userId")

        // Listener para cambios en la colección presence
        val presenceListener = firestore.collection(PRESENCE_COLLECTION)
            .document(userId)
            .addSnapshotListener { presenceSnapshot, presenceError ->
                if (presenceError != null) {
                    Log.e(TAG, "❌ Error observando presencia de $userId: ${presenceError.message}")
                    return@addSnapshotListener
                }

                // Listener para cambios en la configuración del usuario
                firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .addSnapshotListener { userSnapshot, userError ->
                        if (userError != null) {
                            Log.e(TAG, "❌ Error observando usuario $userId: ${userError.message}")
                            return@addSnapshotListener
                        }

                        try {
                            // Calcular estado de presencia
                            val isOnline = if (presenceSnapshot?.exists() == true) {
                                val lastSeen = presenceSnapshot.getTimestamp("lastSeen")?.toDate()?.time ?: 0
                                val status = presenceSnapshot.getString("status") ?: "offline"
                                val currentTime = System.currentTimeMillis()

                                // Fix: Manejar timestamps en el futuro (posibles problemas de sincronización)
                                val timeDifference = currentTime - lastSeen
                                val isRecentlyActive = if (lastSeen > currentTime) {
                                    // Si lastSeen está en el futuro, considerar como actividad reciente
                                    Log.w(TAG, "⚠️ Timestamp en el futuro detectado en observeUserOnlineStatus para $userId: lastSeen=$lastSeen > currentTime=$currentTime")
                                    true
                                } else {
                                    // Lógica normal: actividad dentro de los últimos 5 minutos
                                    timeDifference < 300_000 // 5 minutos
                                }

                                status == "online" && isRecentlyActive
                            } else {
                                false
                            }

                            // Obtener configuración de visibilidad
                            val allowsStatusVisible = userSnapshot?.getBoolean("mostrarEstado") ?: false

                            Log.d(TAG, "🟢 Estado de presencia actualizado para $userId: online=$isOnline, visible=$allowsStatusVisible")

                            // Emitir nuevo estado
                            trySend(Pair(isOnline, allowsStatusVisible))

                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error procesando estado de presencia para $userId: ${e.message}")
                        }
                    }
            }

        awaitClose {
            Log.d(TAG, "🔴 Cerrando observación de presencia para: $userId")
            presenceListener.remove()
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun calculateUnreadCount(chatId: String, userId: String): Int {
        return try {
            // Leer directamente el contador de mensajes no leídos desde participantData
            val chatDoc = firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .get()
                .await()

            Log.d(TAG, "🔍 DEBUG: Calculando unreadCount para userId=$userId, chatId=$chatId")
            Log.d(TAG, "🔍 DEBUG: Chat existe: ${chatDoc.exists()}")

            if (!chatDoc.exists()) {
                Log.w(TAG, "⚠️ Chat $chatId no existe")
                return 0
            }

            val participantData = chatDoc.get("participantData") as? Map<String, Any>
            Log.d(TAG, "🔍 DEBUG: participantData existe: ${participantData != null}")
            Log.d(TAG, "🔍 DEBUG: participantData keys: ${participantData?.keys}")

            val userParticipantData = participantData?.get(userId) as? Map<String, Any>
            Log.d(TAG, "🔍 DEBUG: userParticipantData para $userId existe: ${userParticipantData != null}")
            Log.d(TAG, "🔍 DEBUG: userParticipantData keys: ${userParticipantData?.keys}")

            val unreadCount = userParticipantData?.get("unreadCount") as? Number ?: 0
            Log.d(TAG, "📊 RESULTADO: Mensajes no leídos para $userId en chat $chatId: ${unreadCount.toInt()}")

            unreadCount.toInt()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo mensajes no leídos para $userId en chat $chatId: ${e.message}")
            0
        }
    }

    /**
     * Obtiene el estado de presencia y configuración de visibilidad de un usuario
     */
    suspend fun getUserOnlineStatus(userId: String): Pair<Boolean, Boolean> {
        return try {
            Log.d(TAG, "🟢 Obteniendo estado en línea para usuario: $userId")

            // Consultar datos del usuario desde la colección users
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (!userDoc.exists()) {
                Log.w(TAG, "⚠️ Usuario no encontrado en colección users: $userId")
                return Pair(false, false)
            }

            // Obtener el campo mostrarEstado
            val allowsStatusVisible = userDoc.getBoolean("mostrarEstado") ?: false
            Log.d(TAG, "👁️ Campo mostrarEstado para $userId: $allowsStatusVisible")

            // Verificar presencia del usuario (consultar colección presence)
            val presenceDoc = firestore.collection(PRESENCE_COLLECTION)
                .document(userId)
                .get()
                .await()

            val isOnline = if (presenceDoc.exists()) {
                val lastSeen = presenceDoc.getTimestamp("lastSeen")?.toDate()?.time ?: 0
                val status = presenceDoc.getString("status") ?: "offline"
                val currentTime = System.currentTimeMillis()

                // Considerar usuario en línea si:
                // 1. El estado es "online" Y
                // 2. La última actividad fue hace menos de 5 minutos (300,000 ms)

                // Fix: Manejar timestamps en el futuro (posibles problemas de sincronización)
                val timeDifference = currentTime - lastSeen
                val isRecentlyActive = if (lastSeen > currentTime) {
                    // Si lastSeen está en el futuro, considerar como actividad reciente
                    Log.w(TAG, "⚠️ Timestamp en el futuro detectado para $userId: lastSeen=$lastSeen > currentTime=$currentTime")
                    true
                } else {
                    // Lógica normal: actividad dentro de los últimos 5 minutos
                    timeDifference < 300_000 // 5 minutos
                }

                val isCurrentlyOnline = status == "online" && isRecentlyActive

                Log.d(TAG, "🟢 Estado de presencia para $userId: status=$status, lastSeen=$lastSeen, currentTime=$currentTime, timeDiff=${timeDifference}ms, isOnline=$isCurrentlyOnline")
                isCurrentlyOnline
            } else {
                Log.d(TAG, "🔴 No se encontró documento de presencia para $userId - considerado offline")
                false
            }

            Log.d(TAG, "📊 Estado final para $userId: isOnline=$isOnline, allowsStatusVisible=$allowsStatusVisible")
            Pair(isOnline, allowsStatusVisible)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo estado en línea para $userId: ${e.message}")
            Pair(false, false) // Default: offline y no mostrar estado
        }
    }

    /**
     * Actualiza el estado de presencia del usuario actual
     */
    suspend fun updateUserPresence(isOnline: Boolean = true): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentUserId = getCurrentUserId()
            Log.d(TAG, "🟢 Actualizando presencia para usuario: $currentUserId, online: $isOnline")

            val presenceData = mapOf(
                "userId" to currentUserId,
                "status" to if (isOnline) "online" else "offline",
                "lastSeen" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )

            firestore.collection(PRESENCE_COLLECTION)
                .document(currentUserId)
                .set(presenceData, SetOptions.merge())
                .await()

            Log.d(TAG, "✅ Presencia actualizada para $currentUserId: ${if (isOnline) "online" else "offline"}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando presencia: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun updateUserChatStats(userIds: List<String>, increment: Int) {
        try {
            val batch = firestore.batch()

            userIds.forEach { userId ->
                val statsRef = firestore.collection(USER_STATS_COLLECTION).document(userId)
                batch.update(statsRef,
                    "totalChats", FieldValue.increment(increment.toLong()),
                    "lastChatActivity", Timestamp.now()
                )
            }

            batch.commit().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando estadísticas de chat: ${e.message}")
        }
    }

    private fun convertFirestoreToMessage(doc: DocumentSnapshot): Message? {
        return try {
            val data = doc.data ?: return null

            Message(
                id = doc.id,
                chatId = data["chatId"] as? String ?: "",
                senderId = data["senderId"] as? String ?: "",
                senderName = "", // Se obtiene después
                content = data["text"] as? String ?: "",
                type = MessageType.valueOf((data["type"] as? String ?: "text").uppercase()),
                timestamp = (data["timestamp"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                isRead = false, // TODO: Calcular desde status.read
                isDelivered = false, // TODO: Calcular desde status.delivered
                status = MessageStatus.SENT, // TODO: Convertir desde status
                mediaUrl = data["mediaUrl"] as? String,
                thumbnailUrl = null,
                replyTo = data["replyTo"] as? String,
                replyToMessageId = null,
                isEdited = data["editedAt"] != null,
                editedAt = (data["editedAt"] as? Timestamp)?.toDate()?.time
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error convirtiendo mensaje de Firestore: ${e.message}")
            null
        }
    }

    private fun convertToMessage(messageData: Map<String, Any>, chatId: String): Message? {
        return try {
            Message(
                id = messageData["id"] as? String ?: "",
                chatId = chatId,
                senderId = messageData["senderId"] as? String ?: "",
                senderName = "", // Se obtiene después
                content = messageData["text"] as? String ?: "",
                type = MessageType.valueOf((messageData["type"] as? String ?: "text").uppercase()),
                timestamp = (messageData["timestamp"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                isRead = false,
                isDelivered = false,
                status = MessageStatus.SENT,
                mediaUrl = null,
                thumbnailUrl = null,
                replyTo = null,
                replyToMessageId = null,
                isEdited = false,
                editedAt = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error convirtiendo último mensaje: ${e.message}")
            null
        }
    }

    private fun getMediaPreview(type: MessageType): String {
        return when (type) {
            MessageType.IMAGE -> "📷 Imagen"
            MessageType.VIDEO -> "🎥 Video"
            MessageType.AUDIO -> "🎵 Audio"
            MessageType.FILE -> "📎 Archivo"
            MessageType.LOCATION -> "📍 Ubicación"
            MessageType.STICKER -> "😀 Sticker"
            MessageType.GIF -> "🎬 GIF"
            else -> "Mensaje"
        }
    }
}