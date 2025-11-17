# 👤 Módulo de Perfiles

## Descripción
Sistema de perfiles con foto S3/CloudFront, progreso circular y estadísticas.

## Componentes Principales

### ViewModels
- `PerfilUsuarioLogueadoViewModel.kt` - Perfil propio
- `PerfilUsuarioConsultadoViewModel.kt` - Perfil de otros

### Screens
- `PerfilUsuarioScreen.kt` - UI del perfil
- `PerfilConsultadoScreen.kt` - Perfil de terceros

## Sistema de Fotos de Perfil

### Flujo de Upload
```kotlin
1. Seleccionar foto → Comprimir (1024x1024, 80% quality)
2. Upload S3: profile-photos/{userId}/photo_{timestamp}.jpg
3. NO guardar URL en DB (se consulta dinámicamente)
4. Invalidar CloudFront caché
```

### URLs Dinámicas
```kotlin
// Construir URL en runtime
val profilePhotoUrl = "https://d3example.cloudfront.net/profile-photos/$userId/"
// S3 ListObjects → Última foto por timestamp
```

## Estructura del Perfil
```kotlin
data class PerfilUsuario(
    // Identificación
    val userId: String,
    val nickname: String,
    val nombreCompleto: String?,

    // Stats
    val seguidores: Int,
    val siguiendo: Int,
    val puntos: Int,
    val nivel: Int,

    // Info
    val descripcion: String?,
    val ciudad: String?,
    val pais: String?,

    // Estado
    val isOnline: Boolean = false,
    val lastSeen: Long? = null
)
```

## UI Components

### Barra de Progreso Circular
```kotlin
CircularProgressBar(
    progress = (puntos % 1000) / 1000f,
    size = 180.dp,
    strokeWidth = 8.dp,
    color = BiihliveOrangeLight
)
```

### Layout del Perfil
```
[Avatar 112dp + Progress] | [Nickname + 📍Ciudad]
[    Puntos/Nivel      ]  | [Badge Nivel        ]
                          | [Descripción        ]

[Seguidores | Siguiendo | Ranking]
```

## Queries GraphQL

### getPerfilUsuario
```graphql
query GetPerfilUsuario($userId: ID!) {
    getPerfilUsuario(userId: $userId) {
        userId
        nickname
        nombreCompleto
        descripcion
        ciudad
        pais
        seguidores
        siguiendo
        puntos
        nivel
        isOnline
        lastSeen
    }
}
```

## Sistema de Ranking
- Basado en puntos totales
- Scope: Ciudad → País → Global
- Update: Cada 24h (Lambda scheduled)
- Display: "3º (Madrid)" con color celeste

## Caché y Performance
- Imágenes: Coil con memoria + disco
- Datos: StateFlow con 5min TTL
- Precarga: Al navegar desde lista
- Key: `profile_${userId}`

## Estados del ViewModel
```kotlin
sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profile: PerfilUsuario) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}
```