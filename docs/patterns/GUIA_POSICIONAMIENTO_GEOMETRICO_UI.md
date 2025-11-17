# Guía de Posicionamiento Geométrico de Elementos UI

## 📐 Fundamentos Matemáticos

### Sistema de Coordenadas en Jetpack Compose

```
         0° (12 en punto)
              |
              |
90° -------- Center -------- 270°
   (3)        |           (9)
              |
           180° (6)
```

**Importante:**
- Los ángulos se miden desde las 12 en punto (0°) en sentido horario
- X positivo = derecha, Y positivo = abajo
- `offset(x, y)` mueve elementos relativamente

### Conversión Ángulo de Reloj a Grados

| Posición | Ángulo |
|----------|--------|
| 12 en punto | 0° |
| 1 en punto | 30° |
| 2 en punto | 60° |
| 3 en punto | 90° |
| 4 en punto | 120° |
| 5 en punto | 150° |
| 6 en punto | 180° |
| 7 en punto | 210° |
| 8 en punto | 240° |
| 9 en punto | 270° |
| 10 en punto | 300° |
| 11 en punto | 330° |

---

## 🎯 Fórmulas de Posicionamiento Circular

### 1. Elemento FUERA del círculo (tocando tangencialmente)

**Objetivo:** Badge tocando el borde exterior del círculo

```kotlin
// Parámetros base
val avatarRadius = 45.dp // Radio del círculo base
val borderWidth = 3.dp   // Ancho del borde decorativo
val badgeRadius = 9.dp   // Radio del badge (tamaño / 2)

// Distancia desde el centro
val distanceFromCenter = avatarRadius + borderWidth + badgeRadius

// Ángulo deseado (ejemplo: 4 en punto = 120°)
val angle = 120.0
val angleRad = Math.toRadians(angle)

// Coordenadas finales
val offsetX = (distanceFromCenter.value * kotlin.math.sin(angleRad)).dp
val offsetY = -(distanceFromCenter.value * kotlin.math.cos(angleRad)).dp
```

**Caso de uso:** Badges de nivel, notificaciones externas

---

### 2. Elemento DENTRO del círculo (parte inferior tocando borde)

**Objetivo:** Badge dentro del círculo con su borde inferior en la tangente

```kotlin
// Parámetros base
val avatarRadius = 45.dp
val borderWidth = 3.dp
val imageRadius = avatarRadius - borderWidth // Radio real de la imagen
val badgeHeight = 12.dp // Altura total del badge

// Distancia desde el centro (mitad del badge antes del borde)
val distanceFromCenter = imageRadius - (badgeHeight / 2)

// Para ajustar posición (ejemplo: 10% más abajo)
val adjustedDistance = distanceFromCenter * 1.1f

// Ángulo (ejemplo: 6 en punto = 180°)
val angle = 180.0
val angleRad = Math.toRadians(angle)

// Coordenadas finales
val offsetX = (adjustedDistance.value * kotlin.math.sin(angleRad)).dp
val offsetY = -(adjustedDistance.value * kotlin.math.cos(angleRad)).dp
```

**Caso de uso:** Badges internos, overlays, watermarks

---

### 3. Elemento en esquina (TopStart, TopEnd, etc.)

**Objetivo:** Badge en esquina con offset personalizado

```kotlin
// Para TopStart (-45°)
Box(
    modifier = Modifier
        .offset(
            x = offsetX.dp,  // Negativo = más afuera, Positivo = más adentro
            y = offsetY.dp   // Negativo = más arriba, Positivo = más abajo
        )
        .align(Alignment.TopStart)
)

// Cálculo de offset para tangencia perfecta
// Cuando el avatar es más grande, necesita menos offset hacia adentro
val offsetForWinner = -2.dp  // Avatar 90dp
val offsetForNormal = -4.dp  // Avatar 70dp
```

**Fórmula para calcular offset de tangencia:**
```kotlin
val badgeRadius = badgeSize / 2
val avatarTotalRadius = (avatarSize + borderWidth * 2) / 2
val offset = -(avatarTotalRadius - badgeRadius)
// Negativo porque TopStart está en la esquina exterior
```

**Caso de uso:** Badges de posición, íconos de estado

---

## 🔧 Directivas de Implementación

### Paso 1: Identificar Elementos y Objetivos

```markdown
**Checklist antes de comenzar:**
- [ ] ¿Cuál es el tamaño del elemento base (avatar/círculo)?
- [ ] ¿Tiene borde decorativo? ¿Cuánto mide?
- [ ] ¿El elemento va dentro o fuera del círculo?
- [ ] ¿En qué posición de reloj debe ir? (1-12)
- [ ] ¿El tamaño varía según condiciones (isWinner, etc.)?
```

### Paso 2: Elegir Fórmula Apropiada

| Caso | Fórmula a usar |
|------|---------------|
| Badge fuera tocando | `distanceFromCenter = avatarRadius + borderWidth + badgeRadius` |
| Badge dentro en borde | `distanceFromCenter = (avatarRadius - borderWidth) - (badgeHeight / 2)` |
| Badge en esquina | `offset negativo para tangencia` |
| Ajuste fino | Multiplicar `distanceFromCenter` por factor (0.9 - 1.2) |

### Paso 3: Implementar con Código

```kotlin
// Template genérico
@Composable
fun ElementWithBadge(
    size: Dp,
    borderWidth: Dp = 3.dp,
    badgePosition: Int, // 1-12 posición de reloj
    badgeInside: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(contentAlignment = Alignment.Center) {
        // Elemento base
        Circle(size = size, border = borderWidth)
        
        // Badge posicionado
        val radius = size / 2
        val imageRadius = radius - borderWidth
        val badgeSize = 18.dp
        val badgeRadius = badgeSize / 2
        val badgeHeight = 14.dp // Si es rectangular
        
        val distanceFromCenter = if (badgeInside) {
            imageRadius - (badgeHeight / 2)
        } else {
            radius + borderWidth + badgeRadius
        }
        
        val angle = (badgePosition * 30).toDouble() // Convertir posición reloj a grados
        val angleRad = Math.toRadians(angle)
        
        val offsetX = (distanceFromCenter.value * kotlin.math.sin(angleRad)).dp
        val offsetY = -(distanceFromCenter.value * kotlin.math.cos(angleRad)).dp
        
        Box(
            modifier = Modifier
                .offset(x = offsetX, y = offsetY)
                .align(Alignment.Center)
        ) {
            Badge()
        }
    }
}
```

### Paso 4: Ajustes Finos

```kotlin
// Si el elemento se ve muy pegado
distanceFromCenter * 1.05f // 5% más lejos

// Si el elemento se ve muy separado
distanceFromCenter * 0.95f // 5% más cerca

// Para elementos rectangulares, considerar orientación
if (angle in 45.0..135.0 || angle in 225.0..315.0) {
    // Zona horizontal: ajustar por ancho
    adjustedDistance += badgeWidth / 4
}
```

---

## 📊 Casos de Uso Reales del Proyecto

### Caso 1: Badges de Posición en Podio

**Requisito:** Badge pequeño tocando esquina superior izquierda, considerando avatares de diferente tamaño

```kotlin
// Avatar normal: 70dp, Avatar ganador: 90dp
// Badge: 18dp de diámetro
// Borde: 3dp

// Solución implementada:
Box(
    modifier = Modifier
        .offset(
            x = if (isWinner) -2.dp else -4.dp,
            y = if (isWinner) -2.dp else -4.dp
        )
        .align(Alignment.TopStart)
) {
    Badge(size = 18.dp)
}

// Explicación:
// Avatar 90dp necesita -2dp (menor offset porque es más grande)
// Avatar 70dp necesita -4dp (mayor offset para compensar tamaño menor)
```

### Caso 2: Badge de Nivel a las 6 en Punto

**Requisito:** Badge dentro del avatar, parte inferior tocando borde inferior de imagen

```kotlin
val avatarRadius = if (isWinner) 45.dp else 35.dp
val borderWidth = 3.dp
val imageRadius = avatarRadius - borderWidth
val badgeHeight = 12.dp
val distanceFromCenter = (imageRadius - (badgeHeight / 2)) * 1.1f // 10% ajuste

val angle = 180.0 // 6 en punto
val angleRad = Math.toRadians(angle)
val offsetX = (distanceFromCenter.value * kotlin.math.sin(angleRad)).dp // = 0
val offsetY = -(distanceFromCenter.value * kotlin.math.cos(angleRad)).dp

Box(
    modifier = Modifier
        .offset(x = offsetX, y = offsetY)
        .align(Alignment.Center)
) {
    LevelBadge()
}
```

### Caso 3: Badge en Posición 4 en Punto (Fuera)

**Requisito:** Badge de nivel fuera del avatar, en posición 4 en punto

```kotlin
val avatarRadius = if (isWinner) 45.dp else 35.dp
val borderWidth = 3.dp
val badgeHalfHeight = 7.dp
val distanceFromCenter = avatarRadius + borderWidth + badgeHalfHeight

val angle = 120.0 // 4 en punto
val angleRad = Math.toRadians(angle)
val offsetX = (distanceFromCenter.value * kotlin.math.sin(angleRad)).dp
val offsetY = -(distanceFromCenter.value * kotlin.math.cos(angleRad)).dp

Box(
    modifier = Modifier
        .offset(x = offsetX, y = offsetY)
        .align(Alignment.Center)
) {
    LevelBadge()
}
```

---

## 🎨 Tabla de Referencias Rápidas

### Offsets Comunes para Esquinas

| Elemento | Avatar 70dp | Avatar 90dp | Lógica |
|----------|-------------|-------------|---------|
| Badge 18dp TopStart (tangente) | -4.dp, -4.dp | -2.dp, -2.dp | Menor offset para avatar grande |
| Badge 18dp TopEnd (tangente) | offset(4.dp, -4.dp) | offset(2.dp, -2.dp) | X positivo para derecha |
| Badge 24dp TopStart | -2.dp, -2.dp | 0.dp, 0.dp | Badge más grande = menos offset |

### Ángulos Frecuentes

| Posición Descripción | Ángulo | sin(θ) | cos(θ) | offsetX | offsetY |
|---------------------|--------|--------|--------|---------|---------|
| 12 (arriba) | 0° | 0 | 1 | 0 | -distance |
| 3 (derecha) | 90° | 1 | 0 | +distance | 0 |
| 4 (diagonal DR) | 120° | 0.866 | -0.5 | +0.866×d | +0.5×d |
| 6 (abajo) | 180° | 0 | -1 | 0 | +distance |
| 9 (izquierda) | 270° | -1 | 0 | -distance | 0 |

---

## ⚙️ Debugging y Troubleshooting

### Problema: El elemento no aparece donde esperaba

**Checklist de depuración:**

1. **Verificar sistema de coordenadas**
   ```kotlin
   // ¿Estás usando el signo correcto en offsetY?
   val offsetY = -(distanceFromCenter * kotlin.math.cos(angleRad)).dp
   // Nota el signo negativo para invertir Y
   ```

2. **Verificar alignment del Box padre**
   ```kotlin
   // El badge debe estar en Box con alignment Center
   Box(contentAlignment = Alignment.Center) { ... }
   ```

3. **Verificar unidades**
   ```kotlin
   // Todas las distancias deben ser Dp, no Float
   val distance = 45.dp // ✅ Correcto
   val distance = 45f   // ❌ Incorrecto
   ```

4. **Logs de depuración**
   ```kotlin
   Log.d("BadgePosition", """
       avatarRadius: $avatarRadius
       distanceFromCenter: $distanceFromCenter
       angle: $angle
       offsetX: $offsetX
       offsetY: $offsetY
   """.trimIndent())
   ```

### Problema: El elemento está muy cerca o muy lejos

**Soluciones:**

```kotlin
// Demasiado cerca → Aumentar distancia
val adjustedDistance = distanceFromCenter * 1.1f // +10%

// Demasiado lejos → Reducir distancia
val adjustedDistance = distanceFromCenter * 0.9f // -10%

// Ajuste fino en incrementos de 1dp
val offsetX = calculatedOffsetX + 1.dp // Mover 1dp a la derecha
val offsetY = calculatedOffsetY - 2.dp // Mover 2dp hacia arriba
```

### Problema: Diferentes tamaños se ven inconsistentes

**Solución: Función de escala proporcional**

```kotlin
fun calculateProportionalOffset(
    baseSize: Dp,
    currentSize: Dp,
    baseOffset: Dp
): Dp {
    val scaleFactor = currentSize.value / baseSize.value
    return baseOffset * scaleFactor
}

// Uso:
val offset = calculateProportionalOffset(
    baseSize = 70.dp,      // Avatar de referencia
    currentSize = 90.dp,   // Avatar actual
    baseOffset = -4.dp     // Offset del avatar de referencia
)
// Resultado: -5.14dp (proporcional)
```

---

## 📝 Plantilla de Implementación

```kotlin
/**
 * Template para posicionar badge en elemento circular
 * 
 * @param elementSize Tamaño del elemento base (avatar, círculo)
 * @param borderWidth Ancho del borde decorativo
 * @param badgeSize Tamaño del badge a posicionar
 * @param clockPosition Posición en formato reloj (1-12)
 * @param inside true si el badge va dentro del círculo
 * @param adjustment Factor de ajuste (0.8 - 1.2)
 */
@Composable
fun PositionedBadge(
    elementSize: Dp,
    borderWidth: Dp = 3.dp,
    badgeSize: Dp = 18.dp,
    clockPosition: Int, // 1-12
    inside: Boolean = false,
    adjustment: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val elementRadius = elementSize / 2
    val imageRadius = elementRadius - borderWidth
    val badgeRadius = badgeSize / 2
    
    val distanceFromCenter = if (inside) {
        (imageRadius - badgeRadius) * adjustment
    } else {
        (elementRadius + borderWidth + badgeRadius) * adjustment
    }
    
    val angleDegrees = (clockPosition * 30).toDouble()
    val angleRad = Math.toRadians(angleDegrees)
    
    val offsetX = (distanceFromCenter.value * kotlin.math.sin(angleRad)).dp
    val offsetY = -(distanceFromCenter.value * kotlin.math.cos(angleRad)).dp
    
    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(badgeSize)
    ) {
        content()
    }
}

// Uso:
PositionedBadge(
    elementSize = 90.dp,
    clockPosition = 6,  // 6 en punto
    inside = true,
    adjustment = 1.1f   // 10% más abajo
) {
    Text("42", style = MaterialTheme.typography.labelSmall)
}
```

---

## 🚀 Optimizaciones y Best Practices

### 1. Cachear cálculos costosos

```kotlin
// ❌ Malo: Recalcular en cada recomposición
@Composable
fun BadgePosition() {
    val angle = Math.toRadians(120.0)
    val offsetX = (45 * kotlin.math.sin(angle)).dp
    // ...
}

// ✅ Bueno: Calcular una vez
@Composable
fun BadgePosition() {
    val offsetX = remember { (45 * kotlin.math.sin(Math.toRadians(120.0))).dp }
    // ...
}
```

### 2. Extraer constantes

```kotlin
// Constantes de diseño
object BadgeConstants {
    val SMALL_BADGE_SIZE = 18.dp
    val MEDIUM_BADGE_SIZE = 24.dp
    val DEFAULT_BORDER_WIDTH = 3.dp
    
    object ClockPositions {
        const val TOP = 0.0
        const val ONE = 30.0
        const val TWO = 60.0
        const val THREE = 90.0
        const val FOUR = 120.0
        const val FIVE = 150.0
        const val SIX = 180.0
        // ...
    }
}
```

### 3. Función helper para ángulos

```kotlin
fun Int.toClockAngle(): Double = this * 30.0

// Uso:
val angle = 4.toClockAngle() // 120.0
```

### 4. Extension para cálculos geométricos

```kotlin
data class CircularPosition(
    val radius: Dp,
    val angleDegrees: Double
) {
    fun toOffset(): DpOffset {
        val angleRad = Math.toRadians(angleDegrees)
        return DpOffset(
            x = (radius.value * kotlin.math.sin(angleRad)).dp,
            y = -(radius.value * kotlin.math.cos(angleRad)).dp
        )
    }
}

// Uso limpio:
val position = CircularPosition(radius = 50.dp, angleDegrees = 120.0)
val offset = position.toOffset()
```

---

## 📚 Referencias Adicionales

### Fórmulas Trigonométricas

- **Sin θ**: Componente horizontal (X)
- **Cos θ**: Componente vertical (Y, invertido)
- **Tan θ**: Raramente usado en UI, útil para pendientes

### Conversión de Unidades

```kotlin
// Dp a Px (para cálculos internos si necesario)
val px = with(LocalDensity.current) { 48.dp.toPx() }

// Px a Dp (de vuelta a UI)
val dp = with(LocalDensity.current) { 96f.toDp() }
```

### Compose Modifiers Relacionados

- `graphicsLayer {}`: Para transformaciones sin afectar layout
- `offset {}`: Para posicionamiento relativo
- `align {}`: Para alineación dentro de Box
- `padding {}`: Para espaciado interno

---

## 🎯 Checklist Final de Implementación

Antes de dar por terminado el posicionamiento:

- [ ] ¿Se ve bien en avatar pequeño (70dp)?
- [ ] ¿Se ve bien en avatar grande (90dp)?
- [ ] ¿Los badges no se solapan entre sí?
- [ ] ¿El posicionamiento es consistente con otros elementos?
- [ ] ¿Se respetan los límites del círculo (dentro/fuera)?
- [ ] ¿El código es legible y está comentado?
- [ ] ¿Los valores mágicos están documentados?
- [ ] ¿Se probó en diferentes tamaños de pantalla?

---

**Creado:** 2025-10-28  
**Proyecto:** Biihlive  
**Componente de referencia:** `RankingScreen.kt` - PodiumUser  
**Versión:** 1.0

---

*Este documento es una guía viva. Actualizar cuando se descubran nuevas técnicas o patrones.*
