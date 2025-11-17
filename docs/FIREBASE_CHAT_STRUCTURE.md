# 💬 ESTRUCTURA DE CHAT FIREBASE - BIIHLIVE

## 🎯 DISEÑO BASADO EN RECOMENDACIONES GOOGLE

> Basado en consulta con Gemini - Especialista en Google Technologies
> Estructura optimizada para escalabilidad y tiempo real

## 📊 ESTRUCTURA DE COLECCIONES FIRESTORE

### Base: `basebiihlive` (Nuestra base existente)

```javascript
// COLECCIÓN: /chats/{chatId}
{
  id: "chat_user1_user2", // Para 1-a-1: ordenado alfabéticamente
  type: "direct" | "group",
  name: "Chat Name", // Solo para grupos
  description: "Description", // Solo para grupos
  avatar: "https://cloudfront.../", // Solo para grupos
  participants: ["userId1", "userId2", "userId3"],
  participantData: {
    "userId1": {
      role: "admin" | "member",
      joinedAt: Timestamp,
      lastReadMessageId: "msg_456",
      notifications: true,
      archived: false,
      pinned: false,
      muted: false
    }
  },
  lastMessage: {
    id: "msg_789",
    text: "Last message preview",
    senderId: "userId2",
    timestamp: Timestamp,
    type: "text" | "image" | "video" | "audio"
  },
  createdAt: Timestamp,
  updatedAt: Timestamp,
  isActive: true
}

// SUBCOLECCIÓN: /chats/{chatId}/messages/{messageId}
{
  id: "msg_123",
  chatId: "chat_user1_user2",
  senderId: "userId1",
  text: "¡Hola! ¿Cómo estás?",
  type: "text" | "image" | "video" | "audio" | "file",
  mediaUrl: "https://biihlivemedia.s3.../", // S3 URLs (integración existente)
  mediaMetadata: {
    fileName: "image.jpg",
    fileSize: 1024000,
    duration: 30, // Para audio/video
    thumbnailUrl: "https://cloudfront.../" // Thumbnail de S3
  },
  replyTo: "msg_456", // Para respuestas
  timestamp: Timestamp,
  editedAt: Timestamp, // Si fue editado
  status: {
    sent: Timestamp,
    delivered: Timestamp,
    read: {
      "userId2": Timestamp,
      "userId3": Timestamp
    }
  },
  reactions: {
    "👍": ["userId2", "userId3"],
    "❤️": ["userId1"]
  },
  isDeleted: false,
  deletedFor: ["userId1"] // Borrado solo para usuarios específicos
}

// COLECCIÓN: /userStats/{userId} (EXTENDER EXISTENTE)
{
  // Campos existentes
  followersCount: number,
  followingCount: number,
  suscripcionesCount: number,
  suscriptoresCount: number,

  // NUEVOS campos para chat
  totalChats: number,
  unreadChats: number,
  totalMessages: number,
  lastChatActivity: Timestamp
}

// COLECCIÓN: /users/{userId}/presence (NUEVA)
{
  userId: "user_123",
  isOnline: true,
  lastSeen: Timestamp,
  isTyping: {
    "chat_123": Timestamp // Expira después de 3 segundos
  },
  deviceInfo: {
    platform: "android",
    version: "1.0.0"
  }
}

// COLECCIÓN: /users/{userId}/chatSettings (NUEVA)
{
  notifications: {
    enabled: true,
    mutedChats: ["chat_123", "chat_456"],
    soundEnabled: true,
    vibrationEnabled: true
  },
  privacy: {
    lastSeen: "everyone" | "contacts" | "nobody",
    profilePhoto: "everyone" | "contacts" | "nobody",
    readReceipts: true
  },
  autoDownload: {
    images: true,
    videos: false,
    audio: true,
    documents: false
  }
}
```

## 🔗 INTEGRACIÓN CON ARQUITECTURA EXISTENTE

### Reutilización de Componentes Biihlive

```kotlin
// Integrar con UserIdManager existente
val currentUserId = UserIdManager.getCurrentUserId()

// Integrar con S3ClientProvider para media
val mediaUrl = S3ClientProvider.uploadChatMedia(file, chatId, messageId)

// Integrar con FirestoreRepository existente
class ChatFirestoreRepository(
    private val firestore: FirebaseFirestore = Firebase.firestore(database = "basebiihlive"),
    private val s3Client: S3ClientProvider,
    private val userIdManager: UserIdManager
)
```

### Nomenclatura de Chat IDs

```kotlin
// Para chats 1-a-1: IDs ordenados alfabéticamente
fun generateChatId(userId1: String, userId2: String): String {
    return if (userId1 < userId2) {
        "chat_${userId1}_${userId2}"
    } else {
        "chat_${userId2}_${userId1}"
    }
}

// Para grupos: UUID + timestamp
fun generateGroupChatId(): String {
    return "group_${UUID.randomUUID()}_${System.currentTimeMillis()}"
}
```

## 📱 ADAPTACIÓN A MODELOS EXISTENTES

### Actualizar Modelos de Dominio (Compatibilidad)

```kotlin
// Mantener interfaces existentes, actualizar implementación
data class Chat(
    val id: String,
    val participants: List<ChatUser>, // Usar ChatUser existente
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val isGroup: Boolean = false,
    val groupName: String? = null,
    val groupImage: String? = null,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val chatType: ChatType = ChatType.DIRECT,

    // NUEVOS campos Firebase
    val participantData: Map<String, ParticipantData> = emptyMap(),
    val isActive: Boolean = true
)

// Extender Message existente
data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val type: MessageType,
    val timestamp: Long,
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    val status: MessageStatus = MessageStatus.SENT,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val replyTo: String? = null,
    val replyToMessageId: String? = null,
    val isEdited: Boolean = false,
    val editedAt: Long? = null,

    // NUEVOS campos Firebase
    val reactions: Map<String, List<String>> = emptyMap(),
    val deletedFor: List<String> = emptyList(),
    val mediaMetadata: MediaMetadata? = null
)
```

## 🔒 REGLAS DE SEGURIDAD FIRESTORE

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/basebiihlive/documents {

    // Chats - solo participantes pueden acceder
    match /chats/{chatId} {
      allow read, write: if request.auth != null
        && request.auth.uid in resource.data.participants;

      // Mensajes del chat
      match /messages/{messageId} {
        allow read: if request.auth != null
          && request.auth.uid in get(/databases/basebiihlive/documents/chats/$(chatId)).data.participants;
        allow create: if request.auth != null
          && request.auth.uid in get(/databases/basebiihlive/documents/chats/$(chatId)).data.participants
          && request.auth.uid == request.resource.data.senderId;
        allow update: if request.auth != null
          && request.auth.uid == resource.data.senderId;
      }
    }

    // Presencia - solo el usuario puede escribir la suya
    match /users/{userId}/presence {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }

    // Configuración de chat - solo el usuario
    match /users/{userId}/chatSettings {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }

    // UserStats - extender reglas existentes
    match /userStats/{userId} {
      allow read: if request.auth != null;
      allow update: if request.auth != null
        && (request.auth.uid == userId ||
            // Permitir updates de contadores de chat
            request.resource.data.diff(resource.data).affectedKeys()
              .hasOnly(['totalChats', 'unreadChats', 'totalMessages', 'lastChatActivity']));
    }
  }
}
```

## 📈 ÍNDICES COMPUESTOS REQUERIDOS

```javascript
// Para Firebase Console - Crear estos índices:

// 1. Chats por participante y actividad
Collection: chats
Fields: participants (Array) + updatedAt (Descending)

// 2. Mensajes por chat y timestamp
Collection: chats/{chatId}/messages
Fields: chatId + timestamp (Descending)

// 3. Mensajes no eliminados
Collection: chats/{chatId}/messages
Fields: isDeleted + timestamp (Descending)

// 4. Mensajes por remitente
Collection: chats/{chatId}/messages
Fields: senderId + timestamp (Descending)

// 5. Presencia por usuario
Collection: users/{userId}/presence
Fields: isOnline + lastSeen (Descending)
```

## 🚀 COMANDOS DE MIGRACIÓN

### Crear Índices Automáticamente
```bash
# Conectar a Firebase
firebase login
firebase use biihlive-aa5c3

# Los índices se crearán automáticamente cuando se ejecuten las queries
# O crear manualmente desde Firebase Console
```

## 🔧 PRÓXIMOS PASOS DE IMPLEMENTACIÓN

1. **✅ Actualizar ChatFirestoreRepository** - Reemplazar ChatRemoteDataSource
2. **✅ Integrar con S3ClientProvider** - Para multimedia
3. **✅ Actualizar ViewModels** - Para usar nuevo repository
4. **✅ Crear UI Screens** - ChatScreen y MessageListScreen
5. **✅ Implementar navegación** - Desde perfiles a chat
6. **✅ Testing completo** - Envío, recepción, multimedia

## 📝 COMPATIBILIDAD CON ESTRUCTURA ACTUAL

- ✅ **Mantiene IChatRepository**: Misma interfaz, nueva implementación
- ✅ **Reutiliza modelos**: Extiende sin romper compatibilidad
- ✅ **Integra S3**: Para multimedia como resto del proyecto
- ✅ **Usa UserIdManager**: Para obtener usuario actual
- ✅ **Firestore "basebiihlive"**: Misma base que resto del proyecto
- ✅ **Escalable**: Preparado para millones de usuarios según Google

---

**Estructura optimizada por Gemini (Google Technologies) para máximo rendimiento y escalabilidad**