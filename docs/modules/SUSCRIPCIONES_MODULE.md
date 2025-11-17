# 💳 SUSCRIPCIONES MODULE

## 🎯 **DESCRIPCIÓN**
Módulo completo para gestión de suscripciones con dos tabs (Suscripciones/Suscriptores). Maneja usuarios suscritos y suscriptores con fechas de unión, expiración y estados de suscripción.

## 📂 **ESTRUCTURA DE ARCHIVOS**
```
domain/suscripciones/model/
└── Suscripcion.kt                  # Modelos de datos

presentation/suscripciones/screens/
└── SuscripcionesScreen.kt          # Pantalla principal con tabs

presentation/perfil/
└── PerfilPersonalLogueadoScreen.kt # Navegación desde botón "Suscripciones"

navigation/
├── Screen.kt                       # Ruta Suscripciones agregada
└── AppNavigation.kt               # Composable de navegación
```

## 🎨 **COMPONENTES PRINCIPALES**

### **SuscripcionesScreen**
- **Pantalla principal** con sistema de tabs
- **TopBar** con título "Suscripciones" y navegación back
- **BottomBar** con BiihliveNavigationBar integrada
- **Estados de carga** y listas vacías

### **Tab System**
- **Tab 1**: "Suscripciones" (usuarios a los que me suscribí)
- **Tab 2**: "Suscriptores" (usuarios suscritos a mí)
- **Indicador naranja** corporativo (BiihliveOrangeLight)
- **Colores dinámicos** según tab seleccionado

### **SuscripcionItem**
- **Avatar circular** (56dp) con borde dinámico
- **Indicador online/offline** con PresenceManager
- **Nickname** con badge de verificado opcional
- **"Unido el: [fecha]"** formateada (2025-01-07)
- **"Expira: [fecha]"** con color de alerta si expira pronto
- **Flecha navegación** a perfil del usuario

## 📊 **MODELO DE DATOS**

### **Suscripcion (Core Model)**
```kotlin
data class Suscripcion(
    val suscripcionId: String,     // ID único de la suscripción
    val userId: String,            // Usuario suscrito
    val nickname: String,          // Nombre de usuario
    val imageUrl: String? = null,  // URL del avatar
    val isVerified: Boolean = false, // Badge de verificado
    val fechaUnion: Long,          // Timestamp de unión
    val fechaExpiracion: Long,     // Timestamp de expiración
    val tipo: String = "premium",  // Tipo de suscripción
    val estado: String = "activa"  // activa, expirada, cancelada
)
```

### **SuscripcionPreview (UI Optimized)**
```kotlin
data class SuscripcionPreview(
    val suscripcionId: String,
    val userId: String,
    val nickname: String,
    val imageUrl: String? = null,
    val isVerified: Boolean = false,
    val fechaUnionFormateada: String,     // "2025-01-07"
    val fechaExpiracionFormateada: String, // "2025-02-07"
    val diasRestantes: Int,               // Días hasta expiración
    val estaExpirada: Boolean = false     // Estado calculado
)
```

## 🎨 **SISTEMA DE DISEÑO**

### **Colores Aplicados**
- **BiihliveBlue** (#1DC3FF): Tabs no seleccionados, badges verificado
- **BiihliveOrangeLight** (#FF7300): Tab activo, indicador de tab
- **BiihliveGreen** (#60BF19): Indicador de usuario online
- **MaterialTheme.colorScheme.error**: Fechas próximas a expirar (≤7 días)

### **Layout y Espaciado**
- **Avatar**: 56dp con borde 2dp y padding interno
- **Spacing horizontal**: 16dp consistente
- **Spacing vertical**: 12dp entre items
- **Divider**: Desde 88dp (alineado con texto)
- **Corner radius**: CircleShape para avatares

### **Tipografía**
- **Nickname**: 16sp, FontWeight.SemiBold
- **Fechas**: 13sp, color variant para legibilidad
- **Tab labels**: 12sp con FontWeight dinámico

## 🧭 **NAVEGACIÓN**

### **Rutas Definidas**
```kotlin
// Screen.kt
object Suscripciones : Screen("suscripciones")

// AppNavigation.kt
composable(Screen.Suscripciones.route) {
    SuscripcionesScreen(navController = navController)
}
```

### **Flujo de Navegación**
```
PerfilPersonalLogueadoScreen → Botón "Suscripciones"
→ SuscripcionesScreen → Click en item
→ PerfilConsultado del usuario
```

## 📱 **DATOS MOCK (DEMO)**

### **Suscripciones Demo**
```kotlin
val suscripcionesMock = listOf(
    SuscripcionPreview(
        suscripcionId = "1",
        userId = "user1",
        nickname = "Eleazar",
        fechaUnionFormateada = "2025-01-07",
        fechaExpiracionFormateada = "2025-02-07",
        diasRestantes = 23,
        isVerified = false
    ),
    // Dani, Jose Angel (verificado), Moises...
)
```

### **Suscriptores Demo**
```kotlin
val suscriptoresMock = listOf(
    SuscripcionPreview(
        userId = "subs1",
        nickname = "Maria González",
        isVerified = true,
        diasRestantes = 21
    ),
    // Carlos Ruiz...
)
```

## 🔄 **ESTADOS DE UI**

### **Estados Implementados**
- **Loading**: CircularProgressIndicator centrado
- **Empty State**: Ícono + mensaje según tab
  - "No tienes suscripciones aún"
  - "No tienes suscriptores aún"
- **Error State**: Preparado para futuras integraciones
- **Content State**: Lista con items funcionales

### **Interacciones**
- **Tab switching**: Cambio inmediato de lista
- **Item click**: Navegación a perfil del usuario
- **Pull to refresh**: Estructura preparada
- **Infinite scroll**: Expandible para paginación

## 🎯 **INTEGRACIÓN FUTURA**

### **Base de Datos BIILIVEDB-SUBSCRIPTIONS**
```kotlin
// Estructura esperada
{
    "suscripcionId": "uuid",
    "userId": "d159109e-1001-70e2-7415-37944d99d7d3",
    "suscriptorId": "otro-user-id",
    "fechaInicio": 1704628800000,    // Timestamp
    "fechaFin": 1707307200000,       // Timestamp
    "tipo": "premium",               // premium, basic, etc.
    "estado": "activa",              // activa, pausada, expirada
    "renovacionAutomatica": true,
    "metodoPago": "stripe_card_xxx"
}
```

### **GraphQL Queries Necesarias**
```graphql
# Obtener mis suscripciones
query GetMisSuscripciones($userId: ID!) {
    listSuscripcionesByUser(userId: $userId) {
        suscripcionId
        userId
        fechaInicio
        fechaFin
        estado
        # Join con usuario para nickname, avatar
    }
}

# Obtener mis suscriptores
query GetMisSuscriptores($suscriptorId: ID!) {
    listSuscriptoresByUser(suscriptorId: $suscriptorId) {
        # Similar structure
    }
}
```

### **ViewModel Futuro**
```kotlin
class SuscripcionesViewModel {
    fun loadSuscripciones() {
        // GraphQL query a BIILIVEDB-SUBSCRIPTIONS
        // Procesar fechas y calcular días restantes
        // Combinar con datos de usuarios
    }

    fun loadSuscriptores() {
        // Query inversa para mis suscriptores
    }
}
```

## 📋 **CARACTERÍSTICAS DESTACADAS**

### **Consistencia con Otras Listas**
- **Mismo patrón** que FollowersFollowingScreen
- **Avatar + borde dinámico** consistente
- **Badge de verificado** igual que otras listas
- **Indicador online** integrado con PresenceManager
- **Navigation pattern** idéntico

### **Diseño Inspirado en la Imagen**
- **Layout exacto** según imagen proporcionada
- **Fechas formateadas** en español
- **Colores de estado** para expiraciones próximas
- **Tipografía y espaciado** coherente

## 🔧 **ESTADOS DE DESARROLLO**

### **✅ IMPLEMENTADO**
- [x] Pantalla completa con dos tabs funcionales
- [x] Items de lista con diseño según imagen
- [x] Modelo de datos completo y optimizado
- [x] Navegación conectada desde perfil personal
- [x] Estados vacíos y de carga
- [x] Integración con PresenceManager
- [x] Sistema de colores corporativo
- [x] Datos mock realistas para demo

### **🚧 PENDIENTE (Futuro)**
- [ ] Integración con BIILIVEDB-SUBSCRIPTIONS
- [ ] Queries GraphQL para backend
- [ ] Sistema de pagos y renovaciones
- [ ] Notificaciones de expiración
- [ ] Filtros por tipo de suscripción
- [ ] Gestión de suscripciones (pausar, cancelar)
- [ ] Métricas y analytics

## 📋 **TESTING**

### **Casos de Prueba**
1. **Navegación**: Botón "Suscripciones" → Pantalla → Back
2. **Tabs**: Switch entre "Suscripciones" y "Suscriptores"
3. **Items**: Click → Navegación a perfil del usuario
4. **Estados online**: Verificar indicador verde
5. **Badges verificado**: Mostrar/ocultar según usuario
6. **Fechas**: Formato correcto y colores de alerta

### **Edge Cases**
- **Listas vacías**: Mensajes apropiados
- **Usuarios sin avatar**: Placeholder correcto
- **Fechas expiradas**: Color de error
- **Nombres largos**: Truncado con ellipsis

---

**Última actualización**: 2025-10-16
**Estado**: ✅ Completamente implementado con datos mock
**Commit**: `8f2bb78` - feat: Implementar pantalla de suscripciones
**Próximo**: Integración con backend GraphQL y BIILIVEDB-SUBSCRIPTIONS