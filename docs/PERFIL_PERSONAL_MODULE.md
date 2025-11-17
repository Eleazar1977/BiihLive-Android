# 📱 Módulo Perfil Personal - Biihlive

## 📅 Estado: 2025-10-03

### 🎯 Descripción General
El módulo de Perfil Personal es el sistema completo de gestión de perfiles de usuario, incluyendo visualización, edición, galería de fotos, y estadísticas. Soporta tanto el perfil propio (logueado) como perfiles consultados de otros usuarios.

## 🏗️ Arquitectura

### 📁 Estructura de Archivos
```
composeApp/src/androidMain/kotlin/com/mision/biihlive/
├── presentation/perfil/
│   ├── PerfilUsuarioScreen.kt           # UI principal del perfil
│   ├── PerfilUsuarioLogueadoViewModel.kt # Lógica perfil logueado
│   ├── PerfilUsuarioConsultadoViewModel.kt # Lógica perfil consultado
│   ├── PerfilUiState.kt                 # Estado UI compartido
│   └── components/
│       ├── ImagePreviewDialog.kt        # Preview básico (deprecated)
│       └── ModernImagePreviewDialog.kt  # Preview moderno actual
├── data/
│   ├── aws/
│   │   └── S3ClientProvider.kt          # Cliente S3 robusto
│   └── repository/
│       └── ProfileImageRepository.kt    # Repositorio de imágenes
├── utils/
│   └── ImageProcessor.kt                # Procesamiento de imágenes
└── domain/perfil/
    └── model/
        └── PerfilUsuario.kt              # Modelo de datos
```

## 🎨 UI/UX Especificaciones

### 📱 Pantalla Principal de Perfil

#### Layout Structure
```
┌─────────────────────────────┐
│      TopAppBar              │
├─────────────────────────────┤
│ ┌─────┐  ┌────────────────┐ │
│ │Avatar│  │ Nombre         │ │
│ │     │  │ @username      │ │
│ │     │  │ Puntos: 1,234  │ │
│ └─────┘  └────────────────┘ │
├─────────────────────────────┤
│   Seguidores | Siguiendo    │
│      123    |     456       │
├─────────────────────────────┤
│   [Tab Fotos] [Tab Videos]  │
├─────────────────────────────┤
│  ┌────┐ ┌────┐ ┌────┐      │
│  │Foto│ │Foto│ │Foto│      │
│  └────┘ └────┘ └────┘      │
│  ┌────┐ ┌────┐ ┌────┐      │
│  │Foto│ │Foto│ │Foto│      │
│  └────┘ └────┘ └────┘      │
└─────────────────────────────┘
       [FAB +]
```

#### Especificaciones de Diseño
- **Avatar**: 100.dp circular con borde 2.dp
- **Espaciados**:
  - Padding horizontal: 16.dp
  - Entre secciones: 2.dp
  - Entre avatar e info: 8.dp
- **Galería Grid**:
  - 3 columnas fijas
  - Aspect ratio 1:1
  - Espaciado: 1.dp
  - Bordes redondeados: 8.dp

### 🖼️ ModernImagePreviewDialog

#### Características
- **Fondo**: Negro completo (sin transparencias)
- **Imagen**: Aspect ratio 3:4 con bordes 24.dp
- **Botones**:
  - Altura: 48.dp
  - Bordes redondeados: 24.dp
  - Texto: 14.sp Medium
  - Iconos: 18.dp
- **Animaciones**:
  - Scale animation entrada/salida
  - Rotación icono durante upload
  - Gradientes en botones
- **Padding inferior**: 80.dp (evita overlap con nav bar)

### 📸 FullScreenGalleryDialog

#### Características
- **Navegación**: HorizontalPager con swipe
- **Indicadores**: Puntos de página actual
- **Gestos**: Tap para cerrar, swipe horizontal
- **Animaciones**: Fade in/out suave

## 🔧 Funcionalidades Técnicas

### 📤 Upload de Imágenes

#### Procesamiento (ImageProcessor.kt)
```kotlin
// Galería
Full: 1920x1920 PNG
Thumbnail: 300x300 PNG

// Perfil
Full: 1024x1024 PNG
Thumbnail: 150x150 PNG
```

#### Estructura S3
```
biihlivemedia/
├── userprofile/{userId}/
│   ├── full_{timestamp}.png
│   └── thumbnail_{timestamp}.png
└── gallery/{userId}/
    ├── full_{timestamp}_{uuid}.png
    ├── thumbnail_{timestamp}_{uuid}.png
    └── metadata_{timestamp}_{uuid}.json
```

### 🔄 Sistema de Actualización

#### Auto-refresh de Galería
```kotlin
// Después de upload exitoso
delay(1500) // Espera propagación S3
loadGalleryImages(loadMore = false)
```

#### Pull-to-Refresh
- Implementado con `PullToRefreshBox` de Material3
- Recarga perfil y estadísticas desde AppSync

### ⚡ Optimizaciones S3

#### Configuración Robusta
```kotlin
ClientConfiguration {
    connectionTimeout = 30000     // 30 segundos
    socketTimeout = 30000        // 30 segundos
    maxErrorRetry = 3           // 3 reintentos
    retryPolicy = DEFAULT       // Backoff exponencial
}
```

#### Manejo de Errores
- Timeouts no crashean la app
- Fallback a valores por defecto
- Logs detallados para debugging
- Límite de objetos en listado (10 max)

## 📊 Estado del Perfil (PerfilUiState)

```kotlin
data class PerfilUiState(
    // Datos básicos
    val perfil: PerfilUsuario? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,

    // Upload
    val isUploadingImage: Boolean = false,
    val uploadProgress: Float = 0f,
    val uploadSuccess: Boolean = false,

    // URLs de perfil
    val profileImageUrl: String? = null,
    val profileThumbnailUrl: String? = null,

    // Galería
    val galleryImages: List<GalleryImage> = emptyList(),
    val isLoadingGallery: Boolean = false,
    val galleryNextToken: String? = null,
    val hasMoreGalleryImages: Boolean = false
)
```

## 🐛 Problemas Conocidos y Soluciones

### ✅ Resueltos
1. **Galería no se refrescaba después de upload**
   - Solución: `loadGalleryImages()` después de upload con delay 1.5s

2. **Imágenes no ordenadas cronológicamente**
   - Solución: IDs con formato `{timestamp}_{uuid}` y ordenamiento por fecha S3

3. **Timeouts de S3 crasheaban la app**
   - Solución: Configuración robusta con retry y manejo de errores

4. **Botón crecía durante upload**
   - Solución: Texto fijo "Subiendo" sin puntos suspensivos

5. **Overlap con navigation bar**
   - Solución: Padding inferior 80.dp en diálogo

## 📋 Roadmap Futuro

### Próximas Características
- [ ] Base de datos DynamoDB para galería
- [ ] Sistema de likes y comentarios en fotos
- [ ] Eliminar fotos de galería
- [ ] Editar caption de fotos
- [ ] Compartir fotos a otras redes
- [ ] Filtros y efectos en fotos
- [ ] Stories temporales (24h)

### Optimizaciones Pendientes
- [ ] Caché local con Room/SQLite
- [ ] Precarga de thumbnails
- [ ] Compresión WebP
- [ ] Upload en background
- [ ] Resumable uploads

## 🔧 Comandos de Desarrollo

```bash
# Compilar módulo
./gradlew :composeApp:assembleDebug

# Logs de perfil
adb logcat | grep "Perfil\|GALLERY\|S3Client"

# Limpiar caché de imágenes
adb shell pm clear com.mision.biihlive

# Test de upload
adb shell am start -n com.mision.biihlive/.MainActivity \
  -e test_upload true
```

## 📝 Notas de Implementación

### Consideraciones Importantes
1. **Siempre usar PNG** para evitar problemas de caché
2. **Delay mínimo 1.5s** después de upload para propagación S3
3. **No anidar scrollables** (usar Column/Row en vez de LazyGrid dentro de LazyColumn)
4. **Timeouts largos** para conexiones lentas (30s mínimo)
5. **IDs únicos** con timestamp para orden cronológico

### Patrones de Código
```kotlin
// Upload con progreso
viewModelScope.launch {
    _uiState.update { it.copy(isUploadingImage = true) }

    try {
        val result = s3Client.upload(...)
        delay(1500) // Propagación
        loadGalleryImages()
    } catch (e: Exception) {
        // Manejo de error
    } finally {
        _uiState.update { it.copy(isUploadingImage = false) }
    }
}
```

## 📚 Referencias
- [Material Design 3 Guidelines](https://m3.material.io)
- [AWS S3 Android SDK](https://docs.aws.amazon.com/aws-mobile/latest/developerguide/s3.html)
- [Jetpack Compose Best Practices](https://developer.android.com/jetpack/compose/performance)

---

**Última actualización**: 2025-10-03
**Versión**: 1.0.0
**Estado**: ✅ Estable y Funcional