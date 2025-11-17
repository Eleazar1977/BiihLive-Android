# Configuración de Subida de Fotos de Perfil a S3 - COMPLETADA

## Resumen de Configuración

Se ha configurado exitosamente el sistema de subida de fotos de perfil a S3 para la aplicación Biihlive.

## Componentes Configurados

### 1. Cognito Identity Pool
- **Identity Pool ID**: `eu-west-3:bce99bf2-9c89-4cd5-a674-b68da1b75a34`
- **Nombre**: BiihliveDirectAccess
- **Región**: eu-west-3
- **Vinculado con User Pool**: eu-west-3_1QeyxVcF9

### 2. Roles IAM
- **Rol Autenticado**: `Cognito_BiihliveDirectAccess_AuthRole`
  - Política `S3ProfileUploadAccess` configurada con permisos para:
    - Subir imágenes a `s3://biihlivebucket/userprofile/{cognitoSub}/*`
    - Leer imágenes del mismo path
    - Eliminar imágenes del mismo path

### 3. Archivos Actualizados

#### amplifyconfiguration.json
- Agregada configuración `CognitoIdentity` con el Identity Pool ID
- Agregada configuración `storage` para el bucket S3

#### ProfileImageRepository.kt
- Actualizado `COGNITO_POOL_ID` con el ID correcto
- Eliminada verificación de pool vacío (ya no necesaria)
- Agregada función `invalidateCloudFrontCache()` (usando timestamps por ahora)

## Estructura de S3

```
s3://biihlivebucket/
└── userprofile/
    └── {cognitoSub}/
        ├── full.jpg       (1024x1024)
        └── thumbnail.jpg  (150x150)
```

## URLs de CloudFront

Las imágenes estarán disponibles en:
- **Thumbnail**: `https://d183hg75gdabnr.cloudfront.net/userprofile/{cognitoSub}/thumbnail.jpg`
- **Full**: `https://d183hg75gdabnr.cloudfront.net/userprofile/{cognitoSub}/full.jpg`

## Scripts Auxiliares Creados

1. **setup-identity-pool.ps1** - Crea un nuevo Identity Pool desde cero
2. **check-identity-pool.ps1** - Verifica la configuración actual
3. **invalidate-cloudfront-after-upload.ps1** - Invalida caché de CloudFront
4. **update-s3-policy.json** - Política IAM para acceso a S3

## Próximos Pasos para Probar

### 1. Compilar y ejecutar la aplicación
```bash
cd AndroidStudioProjects/Biihlive
./gradlew clean
./gradlew assembleDebug
```

### 2. Probar la funcionalidad
1. Abrir la app en un dispositivo/emulador
2. Ir al perfil del usuario
3. Tocar el avatar para abrir el diálogo fullscreen
4. Tocar el ícono de cámara
5. Seleccionar una imagen
6. Confirmar la subida en el preview circular
7. Verificar que la imagen se sube a S3 y se muestra correctamente

### 3. Verificar en AWS
```bash
# Ver si la imagen se subió a S3
aws s3 ls s3://biihlivebucket/userprofile/{cognitoSub}/ --region eu-west-3

# Ver el estado en DynamoDB
aws dynamodb get-item \
  --table-name BIILIVEDB-USERS \
  --key '{"PK":{"S":"cognitoSub"},"SK":{"S":"PROFILE"}}' \
  --region eu-west-3 \
  --query 'Item.hasProfilePhoto.BOOL'
```

## Notas Importantes

1. **Caché de CloudFront**: Actualmente se usa timestamp en las URLs para bypass de caché. Para producción, considerar:
   - Configurar TTL más corto en CloudFront
   - Implementar invalidación real con AWS SDK
   - Usar Lambda trigger en S3 para invalidación automática

2. **Seguridad**: Los usuarios solo pueden subir/modificar imágenes en su propio path (`userprofile/{cognitoSub}/*`)

3. **Procesamiento de Imágenes**:
   - Las imágenes se procesan localmente antes de subir
   - Se generan 2 tamaños: thumbnail (150x150) y full (1024x1024)
   - Se convierten a JPG con compresión optimizada

4. **Manejo de Errores**:
   - Si falla la subida, se muestra mensaje amigable al usuario
   - Los logs detallados ayudan en debugging
   - El campo hasProfilePhoto se actualiza solo tras subida exitosa

## Solución de Problemas

### Si la subida falla con "Token is null"
- Verificar que el usuario esté autenticado correctamente
- Revisar que el Identity Pool ID sea correcto en ProfileImageRepository.kt
- Verificar que el rol IAM tenga los permisos correctos

### Si las imágenes no se muestran después de subir
- Verificar el valor de hasProfilePhoto en DynamoDB
- Limpiar caché de Coil en la app
- Verificar que CloudFront tenga acceso al bucket S3

### Si aparece error de permisos
```bash
# Verificar política del rol
aws iam get-role-policy \
  --role-name Cognito_BiihliveDirectAccess_AuthRole \
  --policy-name S3ProfileUploadAccess \
  --region eu-west-3
```

## Contacto

Para cualquier problema o duda, revisar los logs en Android Studio con el filtro "ProfileImageRepository".

---

**Fecha de configuración**: 2025-09-24
**Configurado por**: Claude Assistant
**Estado**: ✅ LISTO PARA PRUEBAS