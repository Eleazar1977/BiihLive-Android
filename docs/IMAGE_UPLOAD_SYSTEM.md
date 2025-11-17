# SISTEMA DE SUBIDA DE IMÁGENES DE PERFIL - DOCUMENTACIÓN COMPLETA

## 📋 Resumen Ejecutivo

Sistema completo de upload de fotos de perfil a AWS S3 con CloudFront CDN, invalidación de caché automática, y bypass temporal para mostrar imágenes actualizadas inmediatamente.

## 🏗️ Arquitectura

### Componentes Principales

1. **AWS S3**: Almacenamiento de imágenes
   - Bucket: `biihlivemedia`
   - Path: `/userprofile/{userId}/full.png` y `/userprofile/{userId}/thumbnail.png`

2. **AWS CloudFront**: CDN para distribución de imágenes
   - Distribution ID: `E1HZ8WQ7IXAQXD`
   - URL: `https://d183hg75gdabnr.cloudfront.net`

3. **AWS Lambda + API Gateway**: Invalidación de caché
   - API Gateway ID: `ig0ikgy5df`
   - Endpoint: `https://ig0ikgy5df.execute-api.eu-west-3.amazonaws.com/prod/invalidate`

4. **Coil**: Librería de carga de imágenes en Android

## ⚠️ PROBLEMAS ENCONTRADOS Y SOLUCIONES

### Problema 1: Imagen vieja al volver al perfil
**Síntoma**: Después de subir nueva foto, al salir y volver al perfil se muestra la imagen antigua.

**Causa**:
- El bypass de caché (`shouldBypassImageCache`) se perdía cuando el ViewModel se destruía
- El timestamp del último upload no se persistía entre sesiones

**Solución Implementada**:
```kotlin
// Persistir timestamp en SharedPreferences
private val sharedPrefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
private var lastUploadTimestamp: Long = sharedPrefs.getLong("last_upload_timestamp", 0L)

// Bypass activo por 5 minutos (tiempo de propagación de CloudFront)
private const val CACHE_BYPASS_DURATION_MS = 5 * 60 * 1000L // 5 minutos
```

### Problema 2: Parpadeo del avatar durante upload
**Síntoma**: El avatar parpadea 2-3 veces durante el proceso de upload.

**Causa**:
1. Primera actualización: Se limpia el caché de Coil
2. Segunda actualización: Se actualiza el UI state con `uploadSuccess = true`
3. Tercera actualización: Se recarga el perfil con `cargarPerfil()`

**Solución Parcial**:
- Reducido a 1-2 parpadeos manteniendo la imagen en memoria durante la actualización
- **TODO**: Implementar transición suave con placeholder temporal

### Problema 3: CloudFront tarda en propagar invalidación
**Síntoma**: Incluso con invalidación, la imagen tarda en actualizarse.

**Solución**:
- Bypass de S3 directo temporal (3-5 segundos) durante invalidación
- URLs con timestamp `?v=timestamp` para evitar caché del navegador
- Bypass extendido a 5 minutos para cubrir tiempo de propagación

## 🔄 Flujo Completo del Sistema

### 1. Usuario selecciona imagen
```kotlin
imagePickerLauncher.launch(
    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
)
```

### 2. Procesamiento de imagen
```kotlin
// ImageProcessor.kt
- Redimensiona a 1024x1024 (full) y 150x150 (thumbnail)
- Comprime como PNG (100% calidad)
- Maneja rotación EXIF
```

### 3. Upload a S3
```kotlin
// ProfileImageRepository.kt
S3ClientProvider.uploadProfileImages(
    userId = cognitoSub,
    fullImageData = processedImages.fullImageBytes,
    thumbnailData = processedImages.thumbnailBytes
)
```

### 4. Invalidación de CloudFront
```kotlin
// CloudFrontInvalidator.kt
CloudFrontInvalidator.invalidateUserProfileImages(userId)
// Activa bypass S3 directo por 3-5 segundos
CloudFrontUtils.setForceS3Direct(true)
```

### 5. Actualización del UI State
```kotlin
// PerfilUsuarioLogueadoViewModel.kt
// Persiste timestamp
lastUploadTimestamp = System.currentTimeMillis()
sharedPrefs.edit().putLong("last_upload_timestamp", lastUploadTimestamp).apply()

// Activa bypass en UI state
_uiState.update {
    it.copy(shouldBypassImageCache = true)
}
```

### 6. URLs con bypass de caché
```kotlin
// CloudFrontUtils.kt
fun getProfilePhotoUrl(userId: String, bypassCache: Boolean): String {
    var url = "$baseUrl/userprofile/$userId/$size.png"
    if (bypassCache) {
        url += "?v=${System.currentTimeMillis()}"
    }
    return url
}
```

### 7. Carga de imagen con Coil deshabilitando caché
```kotlin
// PerfilUsuarioScreen.kt
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(imageUrl)
        .diskCachePolicy(
            if (shouldBypassImageCache) CachePolicy.DISABLED
            else CachePolicy.ENABLED
        )
        .memoryCachePolicy(
            if (shouldBypassImageCache) CachePolicy.DISABLED
            else CachePolicy.ENABLED
        )
```

## 📁 Archivos Clave del Sistema

### Kotlin/Android
- `ProfileImageRepository.kt` - Upload a S3 y coordinación
- `ImageProcessor.kt` - Procesamiento y compresión de imágenes
- `S3ClientProvider.kt` - Cliente S3 para upload
- `CloudFrontInvalidator.kt` - Invalidación de caché CDN
- `CloudFrontUtils.kt` - Generación de URLs con bypass
- `PerfilUsuarioLogueadoViewModel.kt` - Lógica de negocio y persistencia
- `PerfilUsuarioScreen.kt` - UI con bypass de caché
- `AWSConfig.kt` - Configuración centralizada de AWS

### AWS/Backend
- `lambda_function.py` - Lambda para invalidación CloudFront
- `deploy-cloudfront-invalidation.ps1` - Script de deployment

## 🔧 Configuración AWS Requerida

### Identity Pool (Cognito)
```
Region: eu-west-3
Pool ID: eu-west-3:93df5af8-4cf5-4520-b868-cb586153655f
```

### IAM Policy para S3
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:GetObject"
            ],
            "Resource": "arn:aws:s3:::biihlivemedia/userprofile/*"
        }
    ]
}
```

### Lambda Function Policy
```json
{
    "Effect": "Allow",
    "Action": [
        "cloudfront:CreateInvalidation"
    ],
    "Resource": "arn:aws:cloudfront::*:distribution/E1HZ8WQ7IXAQXD"
}
```

## 🐛 Debugging

### Logs importantes a revisar
```bash
# Android Studio Logcat - Filtros útiles
tag:ProfileImageRepository | tag:CloudFrontInvalidator | tag:CloudFrontUtils | tag:PerfilUsuarioLogueadoViewModel
```

### Puntos de verificación
1. **Upload exitoso**: Buscar `[UPLOAD SUCCESS]` en logs
2. **Invalidación CloudFront**: Buscar `[SUCCESS] ✅ Invalidación de CloudFront completada`
3. **Bypass activo**: Buscar `[CACHE BYPASS] Activo`
4. **URLs generadas**: Buscar `[URL GENERATED]`

### Comandos AWS CLI útiles
```bash
# Verificar invalidación
aws cloudfront list-invalidations \
  --distribution-id E1HZ8WQ7IXAQXD \
  --region eu-west-3

# Ver imagen en S3
aws s3 ls s3://biihlivemedia/userprofile/{userId}/ \
  --region eu-west-3

# Ver logs de Lambda
aws logs tail /aws/lambda/BiihliveCloudFrontInvalidation \
  --follow \
  --region eu-west-3
```

## ⏰ Tiempos del Sistema

| Operación | Tiempo |
|-----------|---------|
| Procesamiento de imagen | 1-2 segundos |
| Upload a S3 | 2-5 segundos |
| Invalidación CloudFront | 200-500ms (trigger) |
| Propagación CloudFront | 2-5 minutos |
| Bypass de caché activo | 5 minutos |
| Limpieza caché Coil | Instantáneo |

## 🚀 Mejoras Futuras Recomendadas

1. **Eliminar parpadeo del avatar**
   - Implementar placeholder temporal durante upload
   - Usar transición suave entre imagen vieja y nueva
   - Mantener imagen en memoria durante actualización

2. **Optimizar tiempos de propagación**
   - Considerar usar S3 directo por defecto para perfiles propios
   - Implementar pre-warming de CloudFront
   - Usar versioning de archivos en S3

3. **Mejorar UX durante upload**
   - Mostrar progreso real del upload (no solo spinner)
   - Permitir cancelar upload en progreso
   - Preview de la imagen antes de confirmar

4. **Sistema de respaldo**
   - Guardar última imagen válida localmente
   - Implementar retry automático en caso de fallo
   - Validación de imagen antes de reemplazar la anterior

## 📝 Notas Importantes

- **NUNCA** confiar solo en CloudFront para imágenes recién subidas
- **SIEMPRE** usar bypass de caché por al menos 5 minutos después del upload
- **PERSISTIR** el timestamp del último upload para mantener bypass entre sesiones
- Las imágenes son PNG para evitar problemas de compresión con JPG
- El sistema funciona pero tiene margen de mejora en UX (parpadeo)

## 🔗 Referencias

- [AWS CloudFront Invalidation](https://docs.aws.amazon.com/cloudfront/latest/APIReference/API_CreateInvalidation.html)
- [Coil Image Loading](https://coil-kt.github.io/coil/)
- [AWS S3 Android SDK](https://aws-amplify.github.io/docs/android/storage)

---

**Última actualización**: 2025-09-30
**Versión**: 1.0
**Estado**: Funcional con issues menores (parpadeo)