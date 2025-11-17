# CloudFront Cache Configuration para Biihlive

## Información de CloudFront

- **Distribution ID**: E1HZ8WQ7IXAQXD
- **Domain**: https://d183hg75gdabnr.cloudfront.net
- **S3 Bucket**: biihlivemedia
- **Región**: eu-west-3

## Scripts Disponibles

### 1. `get-cloudfront-info.ps1`
Obtiene la información de tu distribución CloudFront.
```powershell
.\get-cloudfront-info.ps1
```

### 2. `set-s3-cache-headers.ps1`
Configura los headers Cache-Control en los objetos S3.
- **Thumbnails**: 24 horas de cache
- **Full images**: 1 hora de cache
```powershell
.\set-s3-cache-headers.ps1
```

### 3. `invalidate-cloudfront-cache.ps1`
Invalida el cache de CloudFront cuando necesitas actualización inmediata.

**Invalidar un usuario específico:**
```powershell
.\invalidate-cloudfront-cache.ps1 -UserId "91b950fe-a0a1-7089-29fc-bd301495950b"
```

**Invalidar todos los perfiles:**
```powershell
.\invalidate-cloudfront-cache.ps1 -All
```

### 4. `configure-cloudfront-cache.ps1`
Configura los TTL en CloudFront (usar con precaución).
```powershell
.\configure-cloudfront-cache.ps1
```

## Proceso Recomendado

### Para Configuración Inicial:

1. **Configurar headers en S3:**
   ```powershell
   .\set-s3-cache-headers.ps1
   ```

2. **Invalidar cache existente:**
   ```powershell
   .\invalidate-cloudfront-cache.ps1 -All
   ```

### Cuando un Usuario Actualiza su Foto:

1. **Subir nueva foto a S3** (desde la app)

2. **Invalidar cache del usuario específico:**
   ```powershell
   .\invalidate-cloudfront-cache.ps1 -UserId "user-id-aqui"
   ```

## Configuración en la App

En `CloudFrontUtils.kt`:

```kotlin
// CloudFront está configurado con TTL corto
private const val USE_CLOUDFRONT = true  // true = CloudFront, false = S3 directo
```

### URLs Generadas:
- **CloudFront**: `https://d183hg75gdabnr.cloudfront.net/userprofile/{userId}/{size}.jpg`
- **S3 Directo**: `https://biihlivemedia.s3.eu-west-3.amazonaws.com/userprofile/{userId}/{size}.jpg`

## Estrategia de Cache

### TTL Configurados:
| Tipo | Cache Duration | Razón |
|------|---------------|--------|
| Thumbnail | 24 horas | Cambia poco, puede cachear más |
| Full | 1 hora | Puede cambiar más frecuentemente |

### Bypass de Cache:
La app añade `?v=timestamp` a las URLs para forzar actualización cuando sea necesario.

## Costos de Invalidación

- **Gratis**: Primeras 1,000 invalidaciones/mes
- **Después**: $0.005 USD por path invalidado

## Troubleshooting

### Si las imágenes no se actualizan:

1. **Verificar que CloudFront esté activo:**
   ```powershell
   .\get-cloudfront-info.ps1
   ```

2. **Forzar invalidación:**
   ```powershell
   .\invalidate-cloudfront-cache.ps1 -UserId "user-id"
   ```

3. **Si persiste, cambiar temporalmente a S3 directo:**
   En `CloudFrontUtils.kt`:
   ```kotlin
   private const val USE_CLOUDFRONT = false  // Usar S3 directo
   ```

## Notas Importantes

1. Los cambios en CloudFront pueden tardar 5-15 minutos en propagarse globalmente
2. Las invalidaciones son inmediatas pero tienen un costo después de 1,000/mes
3. El timestamp en URLs (`?v=xxx`) ayuda a bypass de cache del navegador
4. CloudFront es más rápido que S3 directo cuando el cache funciona correctamente

## Comandos AWS CLI Útiles

**Ver estado de distribución:**
```bash
aws cloudfront get-distribution --id E1HZ8WQ7IXAQXD --region eu-west-3
```

**Crear invalidación manual:**
```bash
aws cloudfront create-invalidation --distribution-id E1HZ8WQ7IXAQXD --paths "/userprofile/*"
```

**Ver objetos en S3:**
```bash
aws s3 ls s3://biihlivemedia/userprofile/ --recursive
```