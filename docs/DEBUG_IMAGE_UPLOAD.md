# DEBUG: SISTEMA DE SUBIDA DE IMÁGENES

## Logs Agregados para Identificar el Problema

### 1. ProfileImageRepository.kt
- ✅ URLs completas de S3/CloudFront después de subir
- ✅ Estado de invalidación de CloudFront
- ✅ Detalles de limpieza de caché local
- ✅ Timestamps para verificar timing

### 2. CloudFrontInvalidator.kt
- ✅ API Gateway ID y endpoint completo
- ✅ Request/Response HTTP detallados
- ✅ Estado de invalidación (Success/Failed)
- ✅ Activación de bypass S3 directo

### 3. CloudFrontUtils.kt
- ✅ URL base usada (CloudFront vs S3 directo)
- ✅ Estado de forceS3Direct
- ✅ URLs finales generadas con/sin timestamp
- ✅ Modo de bypass activo/inactivo

### 4. PerfilUsuarioLogueadoViewModel.kt
- ✅ Timestamp de upload registrado
- ✅ Estado de hasProfilePhoto del perfil
- ✅ URLs de precarga con bypass automático
- ✅ Limpieza de caché con resultados

## Flujo de Debug Paso a Paso

### 1. SUBIR IMAGEN
Busca en Logcat (filtro: TAG:"ProfileImageRepository"):
```
[OK] Imágenes subidas exitosamente.
  - Full URL: https://d183hg75gdabnr.cloudfront.net/userprofile/{userId}/full.png
  - User ID (cognitoSub): {userId}
```

### 2. INVALIDACIÓN CLOUDFRONT
Busca en Logcat (filtro: TAG:"CloudFrontInvalidator"):
```
[INVALIDATION START] ========================================
  User ID: {userId}
  API Gateway ID: ig0ikgy5df
  Distribution ID: E1HZ8WQ7IXAQXD

[HTTP REQUEST] ========================================
  - URL: https://ig0ikgy5df.execute-api.eu-west-3.amazonaws.com/prod/invalidate
  - Payload: {"userId":"..."}

[HTTP RESPONSE] ========================================
  - Status Code: 200
  - Response Body: {"invalidationId":"..."}

[SUCCESS] ✅ Invalidación de CloudFront completada
  - Activando bypass S3 por 3 segundos
```

### 3. GENERACIÓN DE URLs
Busca en Logcat (filtro: TAG:"CloudFrontUtils"):
```
[URL GENERATED] ========================================
  - User ID: {userId}
  - Base URL: https://d183hg75gdabnr.cloudfront.net (o S3 si bypass activo)
  - Force S3: true/false
  - Bypass Cache: true/false
  - Final URL: https://...?v=timestamp
```

### 4. RECARGA DE PERFIL
Busca en Logcat (filtro: TAG:"PerfilUsuarioLogueadoViewModel"):
```
[RELOAD PROFILE] ========================================
  - Forzando recarga del perfil después de upload

[CARGAR PERFIL] ========================================
🔔 PERFIL ACTUALIZADO (TIEMPO REAL)
  - hasProfilePhoto: true/false

[PRELOAD IMAGE] ========================================
  - Force bypass cache: true (por 30 segundos después de upload)
  - Thumbnail URL: https://...?v=timestamp
```

## Posibles Problemas y Soluciones

### PROBLEMA 1: La invalidación falla
**Síntoma en logs:**
```
[LAMBDA FAILED] ❌ Fallo la invalidación
  - Status Code: 403/500
```
**Solución:**
- Verificar que Lambda tiene permisos CloudFront
- Verificar Distribution ID correcto

### PROBLEMA 2: URLs siguen apuntando a CloudFront con caché
**Síntoma en logs:**
```
[URL GENERATED]
  - Force S3: false
  - Bypass Cache: false
```
**Solución:**
- El bypass S3 debería activarse automáticamente
- Verificar que CloudFrontInvalidator está llamando setForceS3Direct(true)

### PROBLEMA 3: hasProfilePhoto no se actualiza
**Síntoma en logs:**
```
🔔 PERFIL ACTUALIZADO
  - hasProfilePhoto: false (debería ser true)
```
**Solución:**
- Verificar que AppSync está devolviendo el campo actualizado
- El campo ya NO se actualiza desde la app (se eliminó esa lógica)

### PROBLEMA 4: Caché de Coil no se limpia
**Síntoma en logs:**
```
[CACHE CLEAR]
  - Memory cache 'profile_{userId}': NOT FOUND
```
**Solución:**
- Es normal si no había imagen previa en caché
- El importante es que se limpie el disco: "Disk cache: CLEARED ALL"

## Comando para Filtrar Todos los Logs Relevantes

En Android Studio Logcat:
```
tag:ProfileImageRepository | tag:CloudFrontInvalidator | tag:CloudFrontUtils | tag:PerfilUsuarioLogueadoViewModel
```

## Flujo Esperado Correcto

1. **Upload exitoso** → URLs de S3/CloudFront generadas
2. **Invalidación CloudFront** → Status 200, invalidationId recibido
3. **Bypass S3 activo** → Por 3-5 segundos mientras se propaga
4. **URLs con timestamp** → Para evitar caché del navegador/app
5. **Perfil recargado** → hasProfilePhoto actualizado
6. **Imagen visible** → Nueva imagen mostrada inmediatamente

## Si Todo Falla - Solución Manual

1. Limpiar caché de la app (Android Settings → Apps → Biihlive → Clear Cache)
2. Esperar 2-3 minutos para que CloudFront propague la invalidación
3. Forzar cierre de la app y volver a abrir

## Verificación en AWS

### Verificar invalidación:
```bash
aws cloudfront list-invalidations --distribution-id E1HZ8WQ7IXAQXD --query 'InvalidationList.Items[0]'
```

### Verificar imagen en S3:
```bash
aws s3 ls s3://biihlivemedia/userprofile/{userId}/
```

### Ver logs de Lambda:
```bash
aws logs tail /aws/lambda/BiihliveCloudFrontInvalidation --follow --region eu-west-3
```

---

**Con estos logs detallados, deberías poder identificar exactamente dónde está fallando el flujo.**