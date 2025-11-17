# Scripts de Gestión de Imágenes S3 - Biihlive

## 🎯 Resumen Rápido

Después de subir una foto desde la app, si la imagen no se actualiza:

```powershell
# Paso 1: Verificar que la imagen se subió a S3
.\check-s3-upload.ps1 -UserId "91b950fe-a0a1-7089-29fc-bd301495950b"

# Paso 2: Invalidar el caché de CloudFront
.\invalidate-user-cloudfront.ps1 -UserId "91b950fe-a0a1-7089-29fc-bd301495950b"

# Paso 3: En la app, hacer pull-to-refresh en el perfil
```

## 📝 Scripts Disponibles

### 1. `check-s3-upload.ps1`
Verifica si las imágenes se subieron correctamente a S3.

**Uso:**
```powershell
.\check-s3-upload.ps1 -UserId "91b950fe-a0a1-7089-29fc-bd301495950b"
```

**¿Qué hace?**
- Verifica si existen `thumbnail.jpg` y `full.jpg` en S3
- Muestra el tamaño y fecha de modificación
- Verifica el valor de `hasProfilePhoto` en DynamoDB
- Proporciona las URLs de CloudFront y S3

### 2. `invalidate-user-cloudfront.ps1`
Invalida el caché de CloudFront para forzar la actualización de imágenes.

**Uso:**
```powershell
.\invalidate-user-cloudfront.ps1 -UserId "91b950fe-a0a1-7089-29fc-bd301495950b"
```

**¿Qué hace?**
- Crea una invalidación en CloudFront para las rutas del usuario
- La invalidación toma 1-2 minutos en propagarse
- Después de ejecutarlo, hacer pull-to-refresh en la app

### 3. `fix-profile-photo-flag.ps1`
Actualiza el campo `hasProfilePhoto` en DynamoDB.

**Uso:**
```powershell
# Para marcar que el usuario tiene foto
.\fix-profile-photo-flag.ps1 -UserId "91b950fe-a0a1-7089-29fc-bd301495950b" -HasPhoto $true

# Para marcar que el usuario NO tiene foto
.\fix-profile-photo-flag.ps1 -UserId "91b950fe-a0a1-7089-29fc-bd301495950b" -HasPhoto $false
```

**¿Cuándo usarlo?**
- Si la foto existe en S3 pero `hasProfilePhoto` es false
- Si se eliminó la foto pero `hasProfilePhoto` sigue siendo true

## 🔧 Solución de Problemas

### Problema: "La imagen se subió pero no se ve en la app"

1. **Verificar que la imagen está en S3:**
   ```powershell
   .\check-s3-upload.ps1 -UserId "tu-user-id"
   ```

2. **Si la imagen está en S3 pero no se ve:**
   ```powershell
   # Invalidar caché de CloudFront
   .\invalidate-user-cloudfront.ps1 -UserId "tu-user-id"
   ```

3. **Esperar 1-2 minutos y hacer pull-to-refresh en el perfil**

4. **Si aún no se ve, verificar hasProfilePhoto:**
   ```powershell
   .\fix-profile-photo-flag.ps1 -UserId "tu-user-id" -HasPhoto $true
   ```

### Problema: "Error de permisos al ejecutar scripts"

Ejecutar PowerShell como administrador o:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Problema: "AWS CLI no configurado"

Configurar AWS CLI:
```powershell
aws configure
# Ingresar:
# - Access Key ID
# - Secret Access Key
# - Default region: eu-west-3
# - Default output: json
```

## 📍 Información Técnica

### Estructura en S3
```
s3://biihlivemedia/
└── userprofile/
    └── {userId}/
        ├── thumbnail.jpg (150x150)
        └── full.jpg (1024x1024)
```

### URLs de CloudFront
- Base: `https://d183hg75gdabnr.cloudfront.net`
- Thumbnail: `/userprofile/{userId}/thumbnail.jpg`
- Full: `/userprofile/{userId}/full.jpg`

### DynamoDB
- Tabla: `BIILIVEDB-USERS`
- PK: `{userId}`
- SK: `PROFILE`
- Campo clave: `hasProfilePhoto` (Boolean)

## 🚀 Flujo Completo de Actualización de Imagen

1. **App sube imagen a S3** → `ProfileImageRepository.uploadProfileImage()`
2. **Se actualizan los archivos en S3** → `thumbnail.jpg` y `full.jpg`
3. **Se actualiza DynamoDB** → `hasProfilePhoto = true`
4. **Se invalida caché local (Coil)** → Automático en la app
5. **Se invalida CloudFront** → Manual con script o automático (pendiente)
6. **Usuario hace pull-to-refresh** → Ve la nueva imagen

## ⚠️ Notas Importantes

- La invalidación de CloudFront puede tardar 1-2 minutos
- El caché del navegador/app también puede interferir
- Las URLs con timestamp (`?v=timestamp`) ayudan a bypass de caché
- CloudFront tiene un TTL configurado que puede demorar actualizaciones

---

**Última actualización:** 2025-09-24
**Configurado por:** Claude Assistant