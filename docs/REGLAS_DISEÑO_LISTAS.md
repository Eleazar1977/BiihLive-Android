# 📐 Reglas de Diseño - Listas de Usuarios
## Biihlive Design System - User Lists Specifications

**Versión:** 1.0  
**Última actualización:** Octubre 2025  
**Proyecto:** Biihlive KMP (Kotlin Multiplatform)  
**Autor:** UX/UI Design Team

---

## 📋 Tabla de Contenidos

1. [Introducción](#introducción)
2. [Principios de Diseño](#principios-de-diseño)
3. [Especificaciones Técnicas](#especificaciones-técnicas)
4. [Componentes Base](#componentes-base)
5. [Tipografía del Sistema](#tipografía-del-sistema)
6. [Espaciados y Proporciones](#espaciados-y-proporciones)
7. [Estados de Interacción](#estados-de-interacción)
8. [Implementación en Código](#implementación-en-código)
9. [Casos de Uso](#casos-de-uso)
10. [Checklist de Implementación](#checklist-de-implementación)

---

## 🎯 Introducción

Este documento define las **reglas de diseño** para todas las listas de usuarios en la aplicación Biihlive. El objetivo es mantener **consistencia visual**, optimizar el **rendimiento** y garantizar una **experiencia de usuario fluida** en todas las pantallas que muestren listas de usuarios.

### Alcance del Documento

Este estándar aplica a:
- ✅ Lista de búsqueda de usuarios (`UsersSearchScreen`)
- ✅ Lista de seguidores (`FollowersFollowingScreen` - Tab Seguidores)
- ✅ Lista de siguiendo (`FollowersFollowingScreen` - Tab Siguiendo)
- ✅ Lista de participantes en eventos
- ✅ Lista de usuarios en chats grupales
- ✅ Cualquier componente que muestre usuarios en formato lista

---

## 🎨 Principios de Diseño

### 1. **Compacto pero Legible**
- Maximizar contenido visible sin sacrificar legibilidad
- Espaciados suficientes para touch targets (mínimo 48dp)
- Densidad visual optimizada para scrolling rápido

### 2. **Jerarquía Visual Clara**
- Avatares como ancla visual principal
- Nombre de usuario destacado sobre descripción
- Indicadores de estado discretos pero visibles

### 3. **Consistencia del Design System**
- Uso exclusivo de `MaterialTheme.typography` (Material Design 3)
- Colores semánticos del tema (`onSurface`, `onSurfaceVariant`, etc.)
- Espaciados basados en sistema de 8dp grid

### 4. **Performance-First**
- Tamaños de imagen optimizados (thumbnail 112×112px)
- Caching agresivo con Coil
- Lazy loading para listas largas

---

## 📏 Especificaciones Técnicas

### Dimensiones Base (Sistema 8dp)

```kotlin
// 🎯 VALORES ESTÁNDAR - NO MODIFICAR SIN APROBACIÓN
object UserListDimensions {
    // Avatares
    val AVATAR_SIZE = 53.dp              // Tamaño base del avatar
    val AVATAR_BORDER = 2.dp             // Grosor del borde dinámico
    val AVATAR_THUMBNAIL_SIZE = 112      // Tamaño de imagen en cache (px)
    
    // Indicadores
    val ONLINE_INDICATOR_SIZE = 11.dp    // Badge de estado online
    val VERIFIED_BADGE_SIZE = 18.dp      // Icono de verificación
    
    // Espaciados horizontales
    val ITEM_PADDING_HORIZONTAL = 16.dp  // Padding lateral del item
    val AVATAR_CONTENT_SPACING = 11.dp   // Espacio avatar-contenido
    val NAME_BADGE_SPACING = 4.dp        // Espacio nombre-verificado
    
    // Espaciados verticales
    val ITEM_PADDING_VERTICAL = 9.dp     // Padding superior/inferior del item
    val TEXT_SPACING = 2.dp              // Espacio nombre-descripción
    
    // Divisores
    val DIVIDER_START_PADDING = 80.dp    // Alineación del divisor con contenido
    val DIVIDER_ALPHA = 0.8f             // Opacidad del divisor
    
    // Touch targets
    val MIN_TOUCH_TARGET = 48.dp         // Mínimo para accesibilidad
    val ACTION_BUTTON_SIZE = 40.dp       // Botones de acción (ej: menú)
}
```

### Altura Total del Item

```
┌─────────────────────────────────────────────────────┐
│  ↕ 9dp padding top                                  │
├─────────────────────────────────────────────────────┤
│  ↔ 16dp │ ⚫ 53dp │ ↔ 11dp │ Content │ ↔ 16dp       │
│         │ Avatar │        │  Area   │               │
├─────────────────────────────────────────────────────┤
│  ↕ 9dp padding bottom                               │
└─────────────────────────────────────────────────────┘

Altura total: 9dp + 53dp + 9dp = 71dp (aprox. 72dp con contenido)
```

---

## 🧩 Componentes Base

### 1. Avatar con Borde Dinámico

```kotlin
@Composable
fun DynamicBorderedAvatar(
    imageUrl: String?,
    nickname: String,
    isOnline: Boolean = false,
    showOnlineIndicator: Boolean = true,
    size: Dp = 53.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Obtener color dominante del avatar
        val dominantColor by rememberDominantColor(
            imageUrl = imageUrl,
            fallbackColor = Color.Gray.copy(alpha = 0.3f)
        )

        // Container con borde dinámico
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    color = dominantColor,
                    shape = CircleShape
                )
                .padding(2.dp) // Borde
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(200)
                    .size(112, 112) // Thumbnail optimizado
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .memoryCacheKey("thumb_$nickname")
                    .build(),
                contentDescription = "Avatar de $nickname",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_default_avatar),
                error = painterResource(R.drawable.ic_default_avatar),
                fallback = painterResource(R.drawable.ic_default_avatar)
            )
        }

        // Indicador de online
        if (isOnline && showOnlineIndicator) {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .background(
                        color = Color(0xFF60BF19), // BiihliveGreen
                        shape = CircleShape
                    )
                    .align(Alignment.BottomEnd)
            )
        }
    }
}
```

### 2. Información de Usuario

```kotlin
@Composable
fun UserInformation(
    nickname: String,
    description: String?,
    isVerified: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Nombre + Badge de verificación
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = nickname,
                style = MaterialTheme.typography.titleSmall, // 14sp Medium
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (isVerified) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verificado",
                    tint = Color(0xFF1DC3FF), // BiihliveBlue
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Descripción (opcional)
        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
```

### 3. Item Completo de Lista

```kotlin
@Composable
fun StandardUserListItem(
    user: UserPreview,
    onClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        DynamicBorderedAvatar(
            imageUrl = user.imageUrl,
            nickname = user.nickname,
            isOnline = user.isOnline,
            showOnlineIndicator = user.mostrarEstado
        )

        Spacer(modifier = Modifier.width(11.dp))

        // Información
        UserInformation(
            nickname = user.nickname,
            description = user.description,
            isVerified = user.isVerified,
            modifier = Modifier.weight(1f)
        )

        // Contenido adicional (botones, menú, etc.)
        trailingContent?.invoke()
    }
}
```

---

## 📝 Tipografía del Sistema

### ⚠️ REGLA CRÍTICA: Uso de MaterialTheme.typography

**SIEMPRE usar `MaterialTheme.typography`** - NUNCA valores manuales como `fontSize = 16.sp`

```kotlin
// ❌ INCORRECTO - NO USAR
Text(
    text = user.nickname,
    fontSize = 16.sp,
    fontWeight = FontWeight.SemiBold
)

// ✅ CORRECTO - USAR SIEMPRE
Text(
    text = user.nickname,
    style = MaterialTheme.typography.titleSmall // 14sp Medium del tema
)
```

### Estilos Aprobados para Listas

| Elemento | Estilo del Tema | Resultado Visual |
|----------|-----------------|------------------|
| **Nombre de usuario** | `MaterialTheme.typography.titleSmall` | 14sp, FontWeight.Medium |
| **Descripción** | `MaterialTheme.typography.bodyMedium` | 14sp, FontWeight.Normal |
| **Contadores** | `MaterialTheme.typography.labelSmall` | 11sp, FontWeight.Medium |
| **Timestamps** | `MaterialTheme.typography.labelMedium` | 12sp, FontWeight.Medium |

### Colores Semánticos

```kotlin
// ✅ Textos principales
color = MaterialTheme.colorScheme.onSurface

// ✅ Textos secundarios (descripciones)
color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)

// ✅ Textos deshabilitados
color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

// ✅ Textos de error
color = MaterialTheme.colorScheme.error
```

---

## 📐 Espaciados y Proporciones

### Sistema de 8dp Grid

Todos los espaciados deben ser múltiplos de **4dp** (sub-unidad) u **8dp** (unidad base):

```kotlin
// ✅ Espaciados válidos
2.dp   // Sub-sub-unidad (casos excepcionales)
4.dp   // Sub-unidad
8.dp   // Unidad base
11.dp  // Ajuste específico (53dp/~5 ≈ 11dp proporcional)
12.dp  // 1.5 unidades
16.dp  // 2 unidades
24.dp  // 3 unidades
32.dp  // 4 unidades

// ❌ Espaciados NO válidos
7.dp   // No es múltiplo de 4
13.dp  // No sigue el sistema
15.dp  // No alineado con grid
```

### Tabla de Referencia Rápida

| Uso | Valor | Justificación |
|-----|-------|---------------|
| Avatar | 53dp | Compacto, visible, proporcionado |
| Padding item vertical | 9dp | Balance densidad/touch target |
| Padding item horizontal | 16dp | Alineación con márgenes globales |
| Spacing avatar-texto | 11dp | Proporción visual con avatar 53dp |
| Badge verificado | 18dp | Visible sin dominar el nombre |
| Indicador online | 11dp | Proporcional al avatar (~20%) |
| Divisor start | 80dp | 16 + 53 + 11 = alineado con texto |

---

## 🎭 Estados de Interacción

### Estados Visuales

```kotlin
sealed class UserItemState {
    object Default : UserItemState()
    object Pressed : UserItemState()
    object Selected : UserItemState()
    object Loading : UserItemState()
    object Error : UserItemState()
}
```

### Feedback Táctil

```kotlin
// ✅ Item clickeable con ripple
Row(
    modifier = Modifier
        .fillMaxWidth()
        .clickable(
            indication = rememberRipple(bounded = true),
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
        .padding(horizontal = 16.dp, vertical = 9.dp)
) { /* ... */ }

// ✅ Estado de loading
if (isLoading) {
    CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        strokeWidth = 2.dp,
        color = MaterialTheme.colorScheme.primary
    )
}
```

### Animaciones

```kotlin
// ✅ Transición suave de opacidad
AnimatedVisibility(
    visible = showItem,
    enter = fadeIn(animationSpec = tween(200)),
    exit = fadeOut(animationSpec = tween(150))
) {
    StandardUserListItem(user = user, onClick = { })
}
```

---

## 💻 Implementación en Código

### Template Completo

```kotlin
@Composable
fun UserListScreen(
    users: List<UserPreview>,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(top = 2.dp, bottom = 8.dp)
    ) {
        items(
            items = users,
            key = { it.userId }
        ) { user ->
            StandardUserListItem(
                user = user,
                onClick = { onUserClick(user.userId) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(start = 80.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
```

### Optimización de Performance

```kotlin
// ✅ Key estable para LazyColumn
items(
    items = users,
    key = { it.userId } // ← IMPORTANTE: key única por item
) { user ->
    // ...
}

// ✅ Cache de imágenes optimizado
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(user.imageUrl)
        .size(112, 112) // ← Thumbnail, no full-res
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .memoryCacheKey("thumb_${user.userId}") // ← Key único
        .build(),
    // ...
)
```

---

## 📱 Casos de Uso

### 1. Lista de Búsqueda (UsersSearchScreen)

**Características:**
- Barra de búsqueda superior
- Botón "Seguir/Siguiendo" a la derecha
- Sin menú de opciones

```kotlin
StandardUserListItem(
    user = user,
    onClick = { navController.navigate("profile/${user.userId}") },
    trailingContent = {
        FollowButton(
            isFollowing = user.isFollowing,
            isLoading = isLoadingFollow,
            onClick = { viewModel.toggleFollow(user.userId) }
        )
    }
)
```

### 2. Lista de Seguidores/Siguiendo

**Características:**
- Tabs superiores (Seguidores / Siguiendo)
- Menú de tres puntos en "Siguiendo"
- Sin botones de acción en "Seguidores"

```kotlin
StandardUserListItem(
    user = user,
    onClick = { navController.navigate("profile/${user.userId}") },
    trailingContent = if (currentTab == Tab.Following) {
        {
            ThreeDotsMenu(
                onUnfollow = { viewModel.showUnfollowDialog(user) },
                onMessage = { navController.navigate("chat/${user.userId}") }
            )
        }
    } else null
)
```

### 3. Lista de Participantes en Evento

**Características:**
- Indicador de asistencia confirmada
- Badge de organizador
- Sin acciones directas

```kotlin
StandardUserListItem(
    user = user,
    onClick = { navController.navigate("profile/${user.userId}") },
    trailingContent = {
        Row(spacing = 4.dp) {
            if (user.isOrganizer) {
                OrganizerBadge()
            }
            if (user.hasConfirmed) {
                ConfirmationIcon()
            }
        }
    }
)
```

---

## ✅ Checklist de Implementación

### Pre-Implementation

- [ ] Revisar este documento completo
- [ ] Verificar que el componente es una lista de usuarios
- [ ] Identificar características específicas del caso de uso
- [ ] Planificar el `trailingContent` necesario

### Durante Implementación

#### Estructura Base
- [ ] Usar `LazyColumn` con `key = { user.userId }`
- [ ] Padding vertical del item: **9.dp**
- [ ] Padding horizontal del item: **16.dp**
- [ ] Avatar de **53.dp** con borde dinámico de **2.dp**
- [ ] Indicador online de **11.dp** (si aplica)
- [ ] Spacing avatar-contenido: **11.dp**

#### Tipografía
- [ ] Nombre: `MaterialTheme.typography.titleSmall`
- [ ] Descripción: `MaterialTheme.typography.bodyMedium`
- [ ] Colores: `onSurface` y `onSurfaceVariant`
- [ ] NO usar `fontSize` ni `fontWeight` manuales

#### Badges y Estados
- [ ] Badge verificado: **18.dp**, color `#1DC3FF`
- [ ] Spacing nombre-badge: **4.dp**
- [ ] Indicador online: **11.dp**, color `#60BF19`

#### Divisores
- [ ] Padding start: **80.dp**
- [ ] Color: `surfaceVariant` con alpha **0.8f**
- [ ] Grosor: **1.dp** (default de `HorizontalDivider`)

#### Performance
- [ ] Imágenes cacheadas con `diskCachePolicy.ENABLED`
- [ ] Thumbnail de **112×112px**, no full-res
- [ ] Memory cache key único: `"thumb_${userId}"`
- [ ] CrossFade de **200ms** para transiciones suaves

### Post-Implementation

- [ ] Compilar y verificar visualmente
- [ ] Test en diferentes tamaños de pantalla
- [ ] Verificar scroll fluido (60fps)
- [ ] Comprobar alineación de divisores
- [ ] Test con TalkBack (accesibilidad)
- [ ] Validar consistencia con otras listas
- [ ] Code review con equipo UX/UI

---

## 🔄 Control de Versiones

| Versión | Fecha | Cambios | Autor |
|---------|-------|---------|-------|
| 1.0 | Oct 2025 | Documento inicial - Especificaciones base | UX/UI Team |
| | | Avatar: 53dp, Padding: 9dp, Tipografía: Material Theme | |

---

## 📞 Contacto y Soporte

Para dudas, propuestas de cambio o reporte de inconsistencias:

- **Equipo UX/UI**: Revisar y aprobar cambios a este documento
- **Equipo Android**: Implementación y code reviews
- **Slack Channel**: `#design-system-biihlive`
- **Documento vivo**: Este archivo debe actualizarse con cada iteración

---

## 📚 Referencias

- [Material Design 3 - Lists](https://m3.material.io/components/lists/overview)
- [Jetpack Compose - LazyColumn](https://developer.android.com/jetpack/compose/lists)
- [Coil - Image Loading](https://coil-kt.github.io/coil/compose/)
- [Accessibility - Touch Targets](https://m3.material.io/foundations/accessible-design/overview)

---

**⚠️ IMPORTANTE:** Este documento es la **única fuente de verdad** para listas de usuarios en Biihlive. Cualquier desviación debe ser discutida y aprobada por el equipo de diseño antes de implementarse.

---

**Última revisión:** Octubre 2025  
**Próxima revisión:** Enero 2026 (o al añadir nuevos casos de uso)
