# 📱 MÓDULO DE PERFIL - DOCUMENTACIÓN COMPLETA

## 📅 Última actualización: 2025-10-02

## 🏗️ ARQUITECTURA DEL MÓDULO

### Componentes Principales

```
presentation/perfil/
├── PerfilUsuarioScreen.kt              # UI del perfil logueado
├── PerfilConsultadoScreen.kt           # UI del perfil consultado
├── PerfilUsuarioLogueadoViewModel.kt   # ViewModel perfil propio
├── PerfilUsuarioConsultadoViewModel.kt # ViewModel perfil otros
└── PerfilUiState.kt                    # Estados de UI

data/
├── aws/S3ClientProvider.kt             # Cliente S3 para imágenes
└── repository/
    └── FirestoreRepository.kt          # Implementación Firestore

domain/perfil/
├── model/PerfilUsuario.kt              # Modelo de datos
├── repository/PerfilRepository.kt      # Interfaz repositorio
└── usecase/                             # Casos de uso
```

## 🔄 FLUJO DE DATOS ACTUAL

### 1. PERFIL LOGUEADO (Funciona ✅)

```kotlin
// PerfilUsuarioLogueadoViewModel.kt
cargarPerfil() {
    // 1. Obtener datos del perfil desde Firestore (base: basebiihlive)
    val perfil = appSyncRepository.getMyProfile()

    // 2. Consultar S3 para obtener URLs dinámicas de imágenes
    val profileImages = S3ClientProvider.getMostRecentProfileImage(perfil.userId)
    // Retorna: Pair(fullUrl, thumbnailUrl) con timestamps reales

    // 3. Actualizar UI State
    _uiState.update {
        it.copy(
            perfil = perfil,
            profileImageUrl = profileImages?.first,    // URL full con timestamp real
            profileThumbnailUrl = profileImages?.second // URL thumbnail con timestamp real
        )
    }
}

// PerfilUsuarioScreen.kt
PerfilInfo(
    profileImageUrl = uiState.profileImageUrl,      // Usa URL dinámica
    profileThumbnailUrl = uiState.profileThumbnailUrl // Usa URL dinámica
)

AsyncImage(
    model = imageUrl // URL de CloudFront con timestamp real:
                    // https://d183hg75gdabnr.cloudfront.net/userprofile/{userId}/thumbnail_{timestamp}.png
)
```

### 2. PERFIL CONSULTADO (Funciona ✅)

```kotlin
// PerfilUsuarioConsultadoViewModel.kt
cargarPerfilDeUsuario(userId: String) {
    // 1. Obtener datos del perfil desde Firestore (base: basebiihlive)
    val perfil = obtenerPerfilUseCase(userId)

    // 2. Consultar S3 para obtener URLs dinámicas de imágenes
    val profileImages = S3ClientProvider.getMostRecentProfileImage(perfil.userId)

    // 3. Actualizar UI State con URLs dinámicas
    _uiState.update {
        it.copy(
            perfil = perfil,
            profileImageUrl = profileImages?.first,
            profileThumbnailUrl = profileImages?.second
        )
    }
}

// PerfilConsultadoScreen.kt
PerfilConsultadoInfo(
    profileImageUrl = uiState.profileImageUrl,      // Usa URL dinámica
    profileThumbnailUrl = uiState.profileThumbnailUrl // Usa URL dinámica
)

AsyncImage(
    model = imageUrl // URL de CloudFront con timestamp real
)
```

## 🔑 SISTEMA DE IMÁGENES

### Estructura en S3
```
s3://biihlivemedia/
└── userprofile/
    └── {cognitoSub}/
        ├── full_{timestamp}.png       # 1024x1024
        └── thumbnail_{timestamp}.png   # 150x150
```

### Ejemplo Real
```
userprofile/91b950fe-a0a1-7089-29fc-bd301495950b/
├── full_1759240530172.png       (349.0 KB)
└── thumbnail_1759240530172.png  (28.3 KB)
```

### Método Clave: getMostRecentProfileImage()

```kotlin
// S3ClientProvider.kt
suspend fun getMostRecentProfileImage(userId: String): Pair<String, String>? {
    // 1. Lista archivos en S3
    val listRequest = ListObjectsV2Request()
        .withBucketName("biihlivemedia")
        .withPrefix("userprofile/$userId/")

    val result = s3Client.listObjectsV2(listRequest)

    // 2. Encuentra más recientes
    val fullImages = objects.filter { it.key.contains("/full_") }
    val thumbnailImages = objects.filter { it.key.contains("/thumbnail_") }

    val mostRecentFull = fullImages.maxByOrNull { it.key }
    val mostRecentThumbnail = thumbnailImages.maxByOrNull { it.key }

    // 3. Genera URLs de CloudFront
    return Pair(
        "https://d183hg75gdabnr.cloudfront.net/${mostRecentFull.key}",
        "https://d183hg75gdabnr.cloudfront.net/${mostRecentThumbnail.key}"
    )
}
```

## ⚠️ PROBLEMAS CONOCIDOS

### 1. CloudFrontUtils con Timestamp Hardcodeado (Deprecado)
```kotlin
// CloudFrontUtils.kt - DEPRECADO ⚠️
val timestamp = "1759240530172" // HARDCODEADO
val size = if (useThumbnail) "thumbnail_$timestamp" else "full_$timestamp"
// Ya no se usa en los perfiles, mantener solo por compatibilidad temporal
```

### 3. Política del Bucket S3
```json
// NECESARIO para que CloudFront funcione
{
    "Statement": [{
        "Sid": "PublicReadUserProfile",
        "Effect": "Allow",
        "Principal": "*",
        "Action": "s3:GetObject",
        "Resource": "arn:aws:s3:::biihlivemedia/userprofile/*"
    }]
}
```

## ✅ SOLUCIONES IMPLEMENTADAS

### 1. Consulta Dinámica de S3
- `getMostRecentProfileImage()` lista archivos reales
- Obtiene timestamps correctos
- Genera URLs válidas de CloudFront

### 2. Estado de UI Ampliado
```kotlin
// PerfilUiState.kt
data class PerfilUiState(
    val perfil: PerfilUsuario? = null,
    val profileImageUrl: String? = null,      // URL full dinámica
    val profileThumbnailUrl: String? = null,  // URL thumbnail dinámica
    // ... otros campos
)
```

### 3. Política del Bucket Actualizada
- Permite acceso público a `/userprofile/*`
- CloudFront puede leer las imágenes
- Sin "Access Denied"

## ✅ TAREAS COMPLETADAS

- [x] ~~Actualizar `PerfilUsuarioConsultadoViewModel` para usar `getMostRecentProfileImage()`~~ ✅ COMPLETADO
- [x] ~~Eliminar `CloudFrontUtils` con timestamp hardcodeado~~ ✅ ELIMINADO
- [x] ~~Migrar a sistema inmutable (URLs con timestamp en el nombre)~~ ✅ YA IMPLEMENTADO

## 🔍 DEBUGGING

### Verificar Imágenes en S3
```bash
aws s3 ls s3://biihlivemedia/userprofile/{userId}/ --region eu-west-3
```

### Verificar Política del Bucket
```bash
aws s3api get-bucket-policy --bucket biihlivemedia --region eu-west-3
```

### Logs Importantes
```kotlin
// S3ClientProvider
"Objetos encontrados en S3 para $userId: ${objects.size}"
"URLs generadas:"
"  - Full URL: $fullUrl"
"  - Thumbnail URL: $thumbnailUrl"
```

## 📝 NOTAS IMPORTANTES

1. **SIEMPRE** consultar S3 para obtener URLs dinámicas
2. **NUNCA** hardcodear timestamps en URLs
3. **VERIFICAR** permisos del bucket para CloudFront
4. **DOCUMENTAR** cambios en políticas de AWS
5. **USAR** `getMostRecentProfileImage()` para ambos perfiles (logueado y consultado)

---

*Última revisión: 2025-10-02*
*Estado: Sistema completo funcionando - Código limpio sin dependencias deprecadas*