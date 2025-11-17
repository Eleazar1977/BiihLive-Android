# 🎨 SISTEMA DE DISEÑO BIIHLIVE

## 📐 FILOSOFÍA DE DISEÑO

### Principios Fundamentales
- **Limpio**: Espacios amplios, jerarquía clara, sin ruido visual
- **Moderno**: Material Design 3, animaciones suaves, gestos naturales
- **Estilizado**: Coherencia visual absoluta, atención al detalle
- **Multimedia-first**: Optimizado para contenido de video/foto

## 🎨 PALETA DE COLORES

### Colores Principales
```kotlin
// Primary - Celeste
val BiihliveBlue = Color(0xFF1DC3FF)        // Enlaces, elementos secundarios
val BiihliveBlueLight = Color(0xFF7DD3FC)   // Variante clara

// Secondary - Verde
val BiihliveGreen = Color(0xFF60BF19)       // Online, éxito, confirmaciones
val BiihliveGreenLight = Color(0xFFA8D982)  // Variante clara

// Tertiary - Naranja (ACCIÓN PRINCIPAL)
val BiihliveOrange = Color(0xFFDC5A01)      // Versión oscura
val BiihliveOrangeLight = Color(0xFFFF7300) // CTAs, botones principales
```

### Uso de Colores

| Color | Uso Principal | Ejemplo |
|-------|--------------|---------|
| **Naranja Light** | Botones de acción principal | Seguir, Publicar, Confirmar |
| **Celeste** | Enlaces, verificado, secundario | @menciones, badges |
| **Verde** | Estados online, éxito | Indicador online, checks |
| **Grises** | Navegación, texto secundario | Iconos navbar (Gray500/Gray600), descripciones |
| **Negro** | Fondos multimedia | Videos, fotos, live streaming |

## 📏 SISTEMA DE ESPACIADO

### Grid Base: 4dp
```kotlin
object Spacing {
    val xs = 4.dp   // Muy pequeño
    val sm = 8.dp   // Pequeño
    val md = 12.dp  // Medio
    val lg = 16.dp  // Grande (estándar)
    val xl = 24.dp  // Extra grande
    val xxl = 32.dp // Doble extra
}
```

### Aplicación
- **Padding pantallas**: 16dp horizontal
- **Entre elementos**: 8dp (compacto), 12dp (normal), 16dp (amplio)
- **Entre secciones**: 24dp
- **Cards**: 16dp padding interno

## 🔤 TIPOGRAFÍA

### Escala Tipográfica
```kotlin
// Familia: BeVietnamPro (6 pesos)
object Typography {
    val DisplayLarge = 32.sp   // Títulos principales
    val HeadlineLarge = 24.sp  // Encabezados de pantalla
    val HeadlineMedium = 20.sp // Secciones
    val TitleLarge = 18.sp     // Títulos de cards
    val TitleMedium = 16.sp    // Subtítulos
    val BodyLarge = 14.sp      // Texto principal
    val BodyMedium = 13.sp     // Texto secundario
    val LabelLarge = 12.sp     // Botones, etiquetas
    val LabelSmall = 11.sp     // Caption, metadata
}
```

## 🔘 COMPONENTES

### Botones

#### Botón Principal (CTA)
```kotlin
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = BiihliveOrangeLight
        ),
        modifier = modifier
            .height(48.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
```

#### Botón Pequeño (Listas)
```kotlin
@Composable
fun SmallButton(
    text: String,
    onClick: () -> Unit,
    isFollowing: Boolean = false
) {
    if (isFollowing) {
        OutlinedButton(
            onClick = onClick,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = BiihliveOrangeLight
            ),
            border = BorderStroke(1.dp, BiihliveOrangeLight),
            modifier = Modifier
                .height(28.dp)
                .width(90.dp)
        ) {
            Text(text, fontSize = 12.sp)
        }
    } else {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = BiihliveOrangeLight
            ),
            modifier = Modifier
                .height(28.dp)
                .width(90.dp)
        ) {
            Text(text, fontSize = 12.sp)
        }
    }
}
```

### Dimensiones Estándar

| Componente | Altura | Ancho | Notas |
|------------|--------|-------|-------|
| **Botón principal** | 48dp | fillMaxWidth | Pantallas de auth, CTAs |
| **Botón normal** | 40dp | wrap/min 100dp | Diálogos, forms |
| **Botón pequeño** | 28dp | 90dp fijo | Listas (seguir) |
| **TextField** | 56dp | fillMaxWidth | Inputs estándar |
| **TopBar** | 56dp | fillMaxWidth | Navegación superior |
| **BottomBar** | 64dp | fillMaxWidth | Navegación inferior |
| **Avatar lista** | 56dp | 56dp | Circular |
| **Avatar perfil** | 120dp | 120dp | Página de perfil |

## 🎯 ESTADOS VISUALES

### Estados de Interacción
```kotlin
// Opacidades
object StateOpacity {
    const val Disabled = 0.38f
    const val Pressed = 0.12f
    const val Hover = 0.08f
    const val Focus = 0.12f
}

// Ejemplo de uso
modifier = Modifier
    .alpha(if (enabled) 1f else StateOpacity.Disabled)
    .clickable(enabled = enabled) { onClick() }
```

### Indicadores de Carga
```kotlin
@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = BiihliveOrangeLight,
            strokeWidth = 2.dp,
            modifier = Modifier.size(40.dp)
        )
    }
}
```

### Estados Vacíos
```kotlin
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        description?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
```

## 🎬 CONTENIDO MULTIMEDIA

### Reglas Estrictas
1. **Fondo negro SIEMPRE** para videos/fotos/live
2. **Controles blancos** con transparencia
3. **Sin distracciones** durante reproducción
4. **Overlays** con negro 50% opacity

```kotlin
@Composable
fun MediaContainer(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        content()

        // Overlay para controles
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )
    }
}
```

## 📱 NAVEGACIÓN DEL SISTEMA

### Configuración de Barras del Sistema
```kotlin
// En App.kt
val systemUiController = rememberSystemUiController()
val useDarkIcons = !darkTheme

SideEffect {
    systemUiController.setStatusBarColor(
        color = Color.Transparent,
        darkIcons = useDarkIcons
    )
    systemUiController.setNavigationBarColor(
        color = Color.Transparent,
        darkIcons = useDarkIcons
    )
}
```

### Bottom Navigation
- **Iconos no seleccionados**: Gray500 (#64748B)
- **Iconos seleccionados**: Gray600 (#475569)
- **Live button**: SIEMPRE naranja (BiihliveOrangeLight)
- **Altura**: 64dp
- **Tamaño iconos**: 26dp

## ✨ ANIMACIONES

### Duraciones Estándar
```kotlin
object AnimationDuration {
    const val Fast = 150      // Micro interacciones
    const val Normal = 300    // Transiciones normales
    const val Slow = 500      // Énfasis
    const val VerySlow = 800  // Pantallas completas
}
```

### Transiciones Comunes
```kotlin
// Fade In/Out
animateFloatAsState(
    targetValue = if (visible) 1f else 0f,
    animationSpec = tween(AnimationDuration.Normal)
)

// Scale
animateFloatAsState(
    targetValue = if (selected) 1.1f else 1f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
)
```

## 📊 LISTAS Y CARDS

### Lista de Usuarios
```kotlin
@Composable
fun UserListItem(
    user: User,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar (56dp)
        AsyncImage(
            model = user.avatar,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = user.status,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Acción
        SmallButton(
            text = if (user.isFollowing) "Siguiendo" else "Seguir",
            onClick = { /* */ },
            isFollowing = user.isFollowing
        )
    }
}
```

## ♿ ACCESIBILIDAD

### Tamaños Mínimos
- **Área táctil mínima**: 48dp x 48dp
- **Texto mínimo**: 12sp
- **Iconos mínimos**: 24dp

### Contraste
- **Texto sobre fondo**: Ratio mínimo 4.5:1
- **Texto grande**: Ratio mínimo 3:1
- **Elementos interactivos**: Claramente distinguibles

### Soporte de Temas
```kotlin
// SIEMPRE probar en ambos temas
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ComponentPreview() {
    BiihliveTheme {
        // Tu componente
    }
}
```

## 🚀 CHECKLIST DE IMPLEMENTACIÓN

Antes de crear CUALQUIER pantalla:

- [ ] Usar colores del tema (NUNCA hardcodear)
- [ ] Aplicar espaciado estándar (múltiplos de 4dp)
- [ ] Botones con tamaños consistentes
- [ ] Probar en tema claro Y oscuro
- [ ] Verificar accesibilidad (tamaños mínimos)
- [ ] Agregar estados (loading, empty, error)
- [ ] Implementar animaciones suaves
- [ ] Respetar jerarquía tipográfica
- [ ] Mantener coherencia con pantallas existentes

## 📝 EJEMPLOS DE CÓDIGO

### Pantalla Completa Estándar
```kotlin
@Composable
fun StandardScreen(
    title: String,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            StandardTopBar(
                title = title,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Contenido
        }
    }
}
```

### Diálogo Estándar

#### **Especificaciones Estrictas**
| Elemento | Valor | Justificación |
|----------|-------|--------------|
| **Ancho min/max** | 280dp / 320dp | Compacto para móviles |
| **Corner radius** | 16dp | Más sutil que Material 3 default |
| **Padding contenido** | 24dp | Estándar Material 3 |
| **Título size** | 18sp | Compacto pero legible |
| **Texto size** | 14sp | Estándar para body |
| **Espacio título-texto** | 16dp | Compacto |
| **Espacio texto-botones** | 24dp | Separación clara |
| **Altura botones** | 36dp | Touch target mínimo |
| **Elevación** | 3dp | Sutil pero presente |

```kotlin
@Composable
fun StandardDialog(
    title: String,
    message: String,
    confirmText: String = "Aceptar",
    dismissText: String = "Cancelar",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    showDialog: Boolean = true,
    isDangerous: Boolean = false // Para acciones destructivas
) {
    if (!showDialog) return

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .widthIn(min = 280.dp, max = 320.dp) // CRÍTICO: Ancho controlado
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp), // Menos redondeado
        title = {
            Text(
                text = title,
                fontSize = 18.sp, // Más compacto
                fontWeight = FontWeight.SemiBold,
                lineHeight = 22.sp
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDangerous)
                        MaterialTheme.colorScheme.error
                    else
                        BiihliveOrangeLight
                )
            ) {
                Text(
                    text = confirmText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText, fontSize = 14.sp)
            }
        },
        tonalElevation = 3.dp
    )
}
```

#### **Tipos de Diálogos**

1. **Confirmación (2 botones)**
   - Para acciones reversibles
   - Botón confirm: Naranja
   - Botón dismiss: Gris

2. **Destructivo (2 botones)**
   - Para acciones irreversibles
   - Botón confirm: Rojo
   - Botón dismiss: Gris
   - isDangerous = true

3. **Información (1 botón)**
   - Solo informar al usuario
   - Un botón "OK" naranja

4. **Loading (sin botones)**
   - CircularProgressIndicator 32dp
   - Mensaje debajo
   - No dismissible

#### **Uso Incorrecto ❌**
```kotlin
// MAL - Demasiado grande
AlertDialog(
    modifier = Modifier.fillMaxWidth(), // NO!
    title = { Text("Título", fontSize = 24.sp) }, // Muy grande
    ...
)
```

#### **Uso Correcto ✅**
```kotlin
// BIEN - Usar StandardDialog
StandardDialog(
    title = "Confirmar acción",
    message = "¿Estás seguro?",
    onConfirm = { /* */ },
    onDismiss = { /* */ }
)
```

## 🎯 RESUMEN EJECUTIVO

### Lo Más Importante
1. **Naranja para CTAs** - TODOS los botones principales
2. **Respeta el tema** - Claro/oscuro siempre funcionando
3. **Fondo negro multimedia** - Videos, fotos, live
4. **Coherencia absoluta** - Mismos tamaños, espacios, colores
5. **Mobile-first** - Optimizado para touch, gestos naturales

### Prohibido
- Hardcodear colores
- Cambiar dimensiones sin razón
- Ignorar el tema del sistema
- Crear estilos nuevos innecesarios
- Olvidar estados (loading, error, empty)

---

*Este documento es la verdad absoluta del diseño. Consúltalo SIEMPRE antes de implementar cualquier pantalla.*