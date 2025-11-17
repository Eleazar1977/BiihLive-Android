# DOCUMENTACIÓN MÓDULO PERFIL - BIIHLIVE

## ESTRUCTURA ACTUAL

### Pantallas Principales
- `PerfilUsuarioScreen.kt` - Perfil del usuario logueado
- `PerfilConsultadoScreen.kt` - Perfil de otros usuarios

### ViewModels
- `PerfilUsuarioLogueadoViewModel.kt` - Lógica del perfil propio
- `PerfilUsuarioConsultadoViewModel.kt` - Lógica de perfiles consultados

### Componentes Clave
- `CircularProgressBar.kt` - Barra de progreso circular para puntos
- Avatar circular con Shape: CircleShape
- Sistema de seguidores/siguiendo

### Interconexiones Directas

#### 1. Navegación
- **Desde:** HomeScreen, búsqueda de usuarios, chat
- **Hacia:** FollowersFollowingScreen, configuración, edición de perfil
- **Archivo:** `AppNavigation.kt`
- **Route:** `Screen.PerfilUsuario`, `Screen.PerfilConsultado`

#### 2. Repositorios
- `AppSyncRepository.kt` - Conexión con AWS AppSync
- `ProfileImageRepository.kt` - Gestión de imágenes S3
- **Queries GraphQL:** getPerfilUsuario, updatePerfilUsuario

#### 3. Estados y Modelos
```kotlin
data class PerfilUiState(
    val perfil: PerfilUsuario? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFollowing: Boolean = false
)
```

#### 4. Dependencias AWS
- Cognito para autenticación
- S3 para imágenes
- AppSync para datos

## CAMBIOS ESTRUCTURALES PENDIENTES

### 26/09/2025 - Sesión actual

#### Requerimiento 1: Barra de Progreso
- **Estado:** ✅ Completado
- **Cambio:** Color naranja light (#FF7300), grosor 100% mayor (de 6dp a 12dp)
- **Archivo:** `PerfilUsuarioScreen.kt` línea 337-338
- **Impacto:** Visual únicamente
- **Detalles:**
  - strokeWidth: 12dp (antes 6dp)
  - progressColor: BiihliveOrangeLight

#### Requerimiento 2: Reorganización de Layout
- **Estado:** ✅ Completado (actualizado 26/09)
- **Cambios realizados:**
  1. Avatar aumentado a 112dp (25% más grande)
  2. Avatar movido arriba a la izquierda
  3. Columna derecha reorganizada: Nickname (+ubicación) → Badge → Descripción
  4. Badge de nivel en naranja light
  5. Puntos sin texto "puntos" (solo números)
  6. Distribución vertical con `SpaceBetween` para ocupar todo el alto del avatar
  7. Ubicación agregada a la derecha del nickname
- **Estructura nueva:**
  ```
  Row {
    Column {                    | Column (height: 112dp exactos) {
      Avatar círculo (112dp)    |   Nickname + 📍Ciudad (arriba)
      Puntos XX/XXX (debajo)   |   Badge Nivel (centro)
    }                          |   Descripción (abajo)
  }
  ```
- **Nota importante:** La columna derecha tiene altura de 112dp (igual al círculo), los puntos NO cuentan en la altura total

#### Requerimiento 3: Sistema de Colores Grises
- **Estado:** ✅ Completado
- **Cambios aplicados:**
  1. Fondo del progress circular: `MaterialTheme.colorScheme.outline`
  2. Nickname: `MaterialTheme.colorScheme.onSurface`
  3. Números de seguidores: `MaterialTheme.colorScheme.onSurface`
- **Diseño validado:** Con agente diseñador (contraste AAA/AA accesibilidad)
- **Adaptación automática:** Tema claro/oscuro

#### Requerimiento 4: Sistema de Ranking
- **Estado:** ✅ Implementado (hardcoded)
- **Cambios realizados:**
  1. Reemplazado "Puntos" por sistema de ranking
  2. Muestra posición: 1º, 2º, 3º, etc.
  3. Ámbito de ranking: Madrid (hardcoded por ahora)
- **Lógica futura:**
  - Ranking calculado por ubicación seleccionada (Madrid, Mundial, etc.)
  - Posición basada en puntaje total vs. otros usuarios del ámbito
- **UI actual:**
  - Seguidores | Siguiendo | 3º
  - --------- | --------- | Madrid

#### Arquitectura Modular Propuesta
- Separar lógica de UI en componentes reutilizables
- Crear sub-módulos para:
  - ProfileHeader
  - ProfileStats
  - ProfileContent
  - ProfileActions

## COLORES DEL SISTEMA

### Colores principales
- Naranja Light: #FF7300 (acciones principales)
- Celeste: #1DC3FF (secundario)
- Verde: #60BF19 (online/éxito)

### Paleta de grises (Material Design 3)
- **onSurface**: Textos importantes (nicknames, números de seguidores)
  - Claro: #2C2C2C (contraste 15.3:1)
  - Oscuro: #E8E8E8 (contraste 13.8:1)
- **outline**: Fondo del progress circular
  - Claro: #E0E0E0
  - Oscuro: #404040
- **onSurfaceVariant**: Textos secundarios (descripciones, labels)
  - Claro: #757575 (contraste 4.6:1)
  - Oscuro: #B0B0B0 (contraste 4.8:1)

## NOTAS TÉCNICAS
- El proyecto usa Kotlin Multiplatform (KMP)
- Clean Architecture + MVVM
- Jetpack Compose para UI
- La barra de progreso inicia desde las 6 horas (270°)