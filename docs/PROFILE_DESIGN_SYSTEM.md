# Sistema de Diseño - Perfil Biihlive

## Filosofía de Diseño
**Estilo:** Moderno, profesional, minimalista
**Inspiración:** Instagram Stories, BeReal, TikTok Profile
**Año:** 2024/2025 trends

## 📐 GRID SYSTEM & LAYOUT

### Base Grid: 8dp
Todos los espaciados son múltiplos de 8:
- Micro: 4dp
- Small: 8dp
- Medium: 16dp
- Large: 24dp
- XLarge: 32dp

### Layout Principal
```
[Padding 16dp]
┌─────────────────────────────────────┐
│  ┌──────┐   ┌─────────────────────┐ │
│  │Avatar│   │ Nombre              │ │
│  │112dp │   │ Badge Nivel         │ │
│  └──────┘   └─────────────────────┘ │
│   Puntos                            │
│                                     │
│  [Descripción]                      │
│  [Ubicación]                        │
│  [Stats: Seguidores | Siguiendo]   │
│  [Acciones: Editar | Suscripciones] │
└─────────────────────────────────────┘
```

## 🎯 JERARQUÍAS Y TAMAÑOS

### Avatar Section
- **Tamaño avatar:** 112dp (25% más grande que versión anterior)
- **Progress circular:**
  - Stroke width: 8dp (prominente pero elegante)
  - Color activo: #FF7300 (Naranja Light)
  - Color fondo: outline con 30% opacity
- **Texto puntos:** 12sp (labelSmall)

### Información Principal
- **Nickname:**
  - Tamaño: 24sp (headlineSmall)
  - Peso: Bold
  - Color: onSurface

- **Badge Nivel:**
  - Container: Naranja #FF7300
  - Texto: White
  - Padding: 12dp horizontal, 4dp vertical
  - BorderRadius: CircleShape

### Estadísticas
- **Números:**
  - Tamaño: 16sp (bodyLarge)
  - Peso: Bold
  - Color: onSurface

- **Labels:**
  - Tamaño: 12sp (bodySmall)
  - Color: primary (clickeable) o onSurfaceVariant

## 🎨 SISTEMA DE COLORES

### Colores Brand
```kotlin
val BiihliveOrangeLight = Color(0xFFFF7300)  // Principal
val BiihliveCeleste = Color(0xFF1DC3FF)      // Secundario
val BiihliveGreen = Color(0xFF60BF19)        // Estado/Éxito
```

### Grises - Tema Claro
```kotlin
val TextImportant = Color(0xFF2C2C2C)        // onSurface
val TextSecondary = Color(0xFF757575)        // onSurfaceVariant
val ProgressBg = Color(0xFFE0E0E0, alpha=0.3f) // outline 30%
val Divider = Color(0xFFE0E0E0)              // outline
```

### Grises - Tema Oscuro
```kotlin
val TextImportantDark = Color(0xFFE8E8E8)    // onSurface
val TextSecondaryDark = Color(0xFFB0B0B0)    // onSurfaceVariant
val ProgressBgDark = Color(0xFF404040, alpha=0.3f) // outline 30%
val DividerDark = Color(0xFF404040)          // outline
```

## 🗂️ SISTEMA DE TABS GLOBAL

### Colores de Tabs - Tema Claro
**IMPORTANTE:** Aplicar en TODAS las pantallas de la aplicación

#### Tab Seleccionado
- **Texto:** BiihliveOrangeLight (#FF7300)
- **Indicador (línea inferior):** BiihliveOrangeLight (#FF7300)
- **Grosor indicador:** 2-3dp
- **Peso texto:** SemiBold

#### Tab No Seleccionado
- **Texto:** BiihliveCeleste (#1DC3FF)
- **Indicador:** Sin indicador o transparente
- **Peso texto:** Regular

#### Implementación Compose
```kotlin
TabRow(
    selectedTabIndex = selectedTab,
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = BiihliveCeleste,  // Color para tabs no seleccionados
    indicator = { tabPositions ->
        TabRowDefaults.Indicator(
            color = BiihliveOrangeLight,  // Indicador naranja
            height = 2.dp
        )
    }
) {
    Tab(
        selected = isSelected,
        onClick = { /* ... */ },
        text = {
            Text(
                text = "Tab Name",
                color = if (isSelected) BiihliveOrangeLight else BiihliveCeleste,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Regular
            )
        }
    )
}
```

### Colores de Tabs - Tema Oscuro
- **Tab seleccionado:** Mantener BiihliveOrangeLight (#FF7300)
- **Tab no seleccionado:** BiihliveCeleste con 80% opacity
- **Indicador:** BiihliveOrangeLight (#FF7300)

### Casos de Uso
- ✅ Aplicar en: Perfil, Chat, Multimedia, Configuración
- ✅ Todos los TabRow de la aplicación
- ✅ Bottom Navigation tabs (mismo esquema de colores)

## 📏 ESPACIADOS ESPECÍFICOS

### Entre Elementos
- Avatar ↔ Info: 16dp
- Secciones verticales: 16dp
- Items en listas: 8dp
- Padding contenedor: 16dp

### Componentes
```kotlin
// Avatar con progress
Box(modifier = Modifier.size(112.dp))

// Spacing vertical entre secciones
Column(
    verticalArrangement = Arrangement.spacedBy(16.dp)
)

// Spacing horizontal
Row(
    horizontalArrangement = Arrangement.spacedBy(16.dp)
)
```

## 🔤 TIPOGRAFÍA

### Escala Tipográfica
- **Headline:** 24sp - Nickname principal
- **Title:** 18sp - Títulos de sección
- **Body Large:** 16sp - Números importantes
- **Body Medium:** 14sp - Texto general
- **Label:** 12sp - Etiquetas y captions
- **Small:** 10sp - Badges y microtexto

### Pesos
- **Bold:** Nickname, números
- **SemiBold:** Botones, CTAs
- **Regular:** Descripciones, texto general

## 🎯 RATIOS Y PROPORCIONES

### Relación Avatar/Pantalla
- Pantalla móvil estándar: 360dp ancho
- Avatar: 112dp = 31% del ancho
- Proporción áurea aplicada

### Distribución Visual
```
Avatar (31%) | Espacio (4%) | Info (65%)
```

## ✨ ELEMENTOS DE MODERNIDAD

### Tendencias 2024/2025
1. **Progress Rings:** Populares en stories/reels
2. **Badges flotantes:** Nivel como sticker
3. **Grises suaves:** Menos contraste, más elegante
4. **Espacios amplios:** Diseño respirable
5. **Microinteracciones:** Tap para fullscreen

### Animaciones Sugeridas
- Progress bar: Animación suave al cargar
- Avatar: Scale 0.95 al presionar
- Badge: Bounce sutil al aparecer

## 📱 RESPONSIVE CONSIDERATIONS

### Breakpoints
- Small: 360dp (típico)
- Medium: 412dp (modernos)
- Large: 600dp+ (tablets)

### Adaptaciones
- Avatar escala proporcionalmente
- Texto nunca menor a 12sp
- Mantener grid de 8dp

## ✅ CHECKLIST DE IMPLEMENTACIÓN

- [x] Avatar 112dp con progress circular
- [x] Stroke width 8dp
- [x] Gris claro con 30% opacity
- [x] Layout reorganizado: Nickname → Badge → Descripción
- [x] Ubicación a la derecha del nickname
- [x] Badge en naranja light
- [x] Altura columna derecha = círculo (112dp)
- [x] Sistema de ranking en lugar de puntos
- [x] Ámbito ranking en celeste (BiihliveBlue)
- [x] Grid system 8dp
- [x] Colores Material Design 3

## 🔄 PRÓXIMAS MEJORAS

1. **Animaciones:**
   - Transición suave al cambiar progreso
   - Efecto ripple en elementos clickeables

2. **Accesibilidad:**
   - ContentDescriptions completas
   - Focus order correcto
   - Tamaños mínimos de tap (48dp)

3. **Performance:**
   - Lazy loading de imágenes
   - Cache strategy optimizada

---
*Última actualización: 26/09/2025 - Sesión 5*
*Versión: 1.1*
*Cambio: Agregado sistema de colores global para tabs*