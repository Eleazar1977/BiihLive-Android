# 🚀 INICIO RÁPIDO - Upload de Galería

## ⚡ En 3 pasos

### 1️⃣ Instalar dependencias
```bash
pip install boto3 pillow
```

### 2️⃣ Configurar AWS
```bash
aws configure
# Ingresa tu AWS Access Key ID
# Ingresa tu AWS Secret Access Key
# Región: eu-west-3
```

### 3️⃣ Subir imágenes

#### Opción A: Usar el script de ejemplo
```bash
# 1. Edita ejemplo_upload.py y cambia:
#    - USER_ID con tu Firebase UID
#    - IMAGES_FOLDER con la ruta a tus imágenes

# 2. Ejecuta:
python ejemplo_upload.py
```

#### Opción B: Usar el script principal directamente
```bash
python upload_gallery_images.py \
  --user-id TU_USER_ID \
  --folder ./mis_imagenes \
  --uuid-format timestamp
```

## 📋 Formatos de UUID

### Timestamp (Recomendado - Código actual Android)
```bash
--uuid-format timestamp
```
Genera: `1759464354684_a1b2c3d4`

### UUID Completo (Compatible con versiones antiguas)
```bash
--uuid-format full
```
Genera: `0672ada6-f2eb-442d-abce-bdfcdac56eef`

## 🧪 Probar con imágenes de test

```bash
# 1. Crear carpeta de test
mkdir test_images

# 2. Copiar algunas imágenes ahí
copy foto1.jpg test_images/
copy foto2.png test_images/

# 3. Ejecutar test
python test_upload.py
```

## 📁 Estructura resultante en S3

Después del upload, tus archivos estarán en:
```
s3://biihlivemedia/gallery/{userId}/
├── full_{imageId}.jpg
├── thumbnail_{imageId}.jpg
└── metadata_{imageId}.json
```

Accesibles vía CloudFront:
```
https://d183hg75gdabnr.cloudfront.net/gallery/{userId}/full_{imageId}.jpg
https://d183hg75gdabnr.cloudfront.net/gallery/{userId}/thumbnail_{imageId}.jpg
```

## 📊 Ejemplo completo real

```bash
# Usuario de ejemplo del metadata que proporcionaste
python upload_gallery_images.py \
  --user-id 91b950fe-a0a1-7089-29fc-bd301495950b \
  --folder C:/Users/asus/Pictures/prueba \
  --uuid-format full

# Output esperado:
# 🖼️  Procesando: foto1.jpg
#   🆔 Image ID: 0672ada6-f2eb-442d-abce-bdfcdac56eef
#   📤 Subiendo: gallery/91b950fe-a0a1-7089-29fc-bd301495950b/full_0672ada6...
#   ✅ Completado!
```

## 🔍 Verificar uploads

### Ver archivos en S3
```bash
aws s3 ls s3://biihlivemedia/gallery/TU_USER_ID/
```

### Descargar metadata
```bash
aws s3 cp s3://biihlivemedia/gallery/TU_USER_ID/metadata_IMAGEN_ID.json ./
cat metadata_IMAGEN_ID.json
```

## ❓ Troubleshooting rápido

### "No credentials found"
```bash
aws configure
# Ingresa tus credenciales AWS
```

### "Access Denied"
Tu usuario AWS necesita permisos en el bucket `biihlivemedia`

### "No module named 'boto3'"
```bash
pip install boto3 pillow
```

### "Image file not found"
Verifica la ruta de tus imágenes:
```bash
# Windows
dir C:\ruta\a\imagenes

# Usa rutas absolutas o relativas correctas
```

## 📚 Más información

- Ver detalles completos: `GALLERY_UPLOAD_SCRIPT_README.md`
- Código Android original: `composeApp/src/.../S3ClientProvider.kt`

## 🎯 Casos de uso comunes

### Subir fotos de vacaciones
```bash
python upload_gallery_images.py \
  --user-id d1JYlixIvrPKqCmm29GYuZUygD92 \
  --folder "C:/Users/asus/Pictures/Vacaciones 2024" \
  --uuid-format timestamp
```

### Migrar galería existente
```bash
python upload_gallery_images.py \
  --user-id USUARIO_ANTIGUO \
  --folder ./galeria_antigua \
  --uuid-format full
```

### Upload con metadata personalizado
```bash
# Crear metadata.json con tags
echo '{"tags": ["familia", "2024"], "location": "Buenos Aires"}' > metadata.json

python upload_gallery_images.py \
  --user-id TU_USER_ID \
  --images foto1.jpg foto2.jpg \
  --metadata-file metadata.json
```

---

✅ **¡Todo listo!** Ahora puedes subir imágenes a la galería de usuarios en S3 siguiendo la misma lógica que el código Android.
