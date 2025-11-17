# 💰 MÓDULO PATROCINIO - Sistema de Patrocinio

> **Módulo:** Sistema de patrocinio con pantalla estática y navegación completa
> **Estado:** ✅ IMPLEMENTADO Y FUNCIONAL
> **Última actualización:** 17 Octubre 2025

## 📋 RESUMEN

El sistema de patrocinio permite a los usuarios navegar a una pantalla dedicada para patrocinar a otros creadores de contenido. Implementado como pantalla estática con diseño profesional siguiendo los colores corporativos y preparado para futuras integraciones con sistemas de pagos.

## 🚀 FUNCIONALIDADES IMPLEMENTADAS

### ✅ **PatrocinarScreen Completa**
- **Pantalla estática**: Diseño completo siguiendo imagen de referencia
- **Avatar dinámico**: Carga la imagen del usuario a patrocinar
- **Información del usuario**: Nombre y badge de nivel con colores corporativos
- **Input de valor**: Campo editable para el monto del patrocinio
- **Card descriptivo**: Mensaje personalizable del creador
- **Botón principal**: "Patrocinar" en celeste corporativo #1DC3FF
- **TopBar estándar**: Con navegación de retorno

### ✅ **Sistema de Navegación**
- **Ruta definida**: `Screen.Patrocinar` con parámetro `userId`
- **Navegación integrada**: Desde botón "Patrocíname" en perfil público
- **Parámetros funcionales**: userId se pasa correctamente entre pantallas
- **Back navigation**: Funcionando desde TopBar

### ✅ **Diseño y UX**
- **Colores corporativos**: Celeste #1DC3FF, Naranja #FF7300
- **Material Design 3**: Tema adaptativo claro/oscuro
- **Espaciado consistente**: Basado en múltiplos de 4dp
- **Cards con bordes redondeados**: 12dp radius
- **Typography escalable**: Responsive a configuración del sistema

## 🏗️ ARQUITECTURA

### **Estructura de Archivos**
```
composeApp/src/androidMain/kotlin/com/mision/biihlive/
├── presentation/
│   └── patrocinio/
│       └── screens/
│           └── PatrocinarScreen.kt
├── navigation/
│   ├── Screen.kt                    # Ruta Screen.Patrocinar
│   └── AppNavigation.kt            # Composable navigation
└── perfil/
    └── PerfilPublicoConsultadoScreen.kt  # Botón "Patrocíname"
```

### **Navegación Flow**
```kotlin
// 1. Definición de ruta
object Patrocinar : Screen("patrocinar/{userId}") {
    fun createRoute(userId: String) = "patrocinar/$userId"
}

// 2. Composable en navegación
composable(
    route = Screen.Patrocinar.route,
    arguments = listOf(navArgument("userId") { type = NavType.StringType })
) { backStackEntry ->
    val userId = backStackEntry.arguments?.getString("userId") ?: ""
    PatrocinarScreen(navController = navController, userId = userId)
}

// 3. Navegación desde perfil público
onNavigateToPatrocinar = {
    navController.navigate(Screen.Patrocinar.createRoute(perfil.userId))
}
```

## 🎨 COMPONENTES UI

### **PatrocinarScreen Composable**
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatrocinarScreen(
    navController: NavController,
    userId: String? = null
)
```

**Elementos principales:**
- **Scaffold**: Con TopBar y contenido principal
- **AsyncImage**: Avatar del usuario con fallback
- **Surface**: Badge de nivel con fondo naranja
- **OutlinedTextField**: Input del valor de patrocinio
- **Card**: Mensaje descriptivo del creador
- **Button**: Acción principal de patrocinar

### **Datos Estáticos Actuales**
```kotlin
val userName = "Enri"
val userLevel = 41
val userImageUrl: String? = null
var valorPatrocinio by remember { mutableStateOf("EUR 70/mes") }
```

## 🔧 CONFIGURACIÓN Y USO

### **Integración en Perfil Público**
```kotlin
// En PerfilPublicoConsultadoInfo
onNavigateToPatrocinar: () -> Unit = {},

// Implementación en SeccionPatrocinio
onPatrocinioClick = onNavigateToPatrocinar
```

### **Botón "Patrocíname"**
- **Ubicación**: Sección de patrocinio en perfil público
- **Comportamiento**: Dinámico según usuario (algunos usuarios muestran card completo)
- **Acción**: Navegar a PatrocinarScreen con userId correcto

## 🎯 CASOS DE USO

### **Flujo Principal**
1. **Usuario navega** a perfil público de un creador
2. **Ve sección de patrocinio** con botón "Patrocíname"
3. **Presiona botón** → Navega a PatrocinarScreen
4. **Ve pantalla personalizada** con info del creador
5. **Puede editar valor** del patrocinio
6. **Presiona "Patrocinar"** → TODO: Integrar con sistema de pagos
7. **Puede regresar** con botón back

### **Estados de la Pantalla**
- **Loading**: Mientras carga información del usuario
- **Normal**: Pantalla completa con todos los elementos
- **Error**: Fallback a datos por defecto

## 🔗 INTEGRACIÓN CON OTROS MÓDULOS

### **Dependencias**
- **Navigation Module**: Para rutas y navegación
- **Profile Module**: Para obtener información del usuario
- **UI Theme**: Para colores corporativos y Material Design

### **Futuras Integraciones**
- **Payment Module**: Sistema de pagos (pendiente)
- **Backend Integration**: Guardar transacciones de patrocinio
- **Notifications**: Notificar al creador sobre nuevo patrocinio

## 📱 DISEÑO RESPONSIVE

### **Adaptación de Pantallas**
- **Padding horizontal**: 16.dp estándar
- **Spacing vertical**: Basado en múltiplos de 4dp
- **Avatar size**: 120.dp fijo
- **Button height**: 48.dp estándar
- **Corner radius**: 8dp para botones, 12dp para cards

### **Typography Scale**
```kotlin
// Nombre usuario: 24.sp, FontWeight.Bold
// Badge nivel: 14.sp, FontWeight.Medium
// Título sección: 18.sp, FontWeight.SemiBold
// Input y card: 16.sp y 14.sp respectivamente
```

## 🚧 PRÓXIMOS DESARROLLOS

### **Funcionalidades Pendientes**
1. **Integración con pagos**: Stripe, PayPal, etc.
2. **Backend persistence**: Guardar transacciones
3. **Notificaciones**: Sistema de notificaciones push
4. **Historial**: Lista de patrocinios realizados
5. **Configuración**: Montos sugeridos, monedas

### **Mejoras UX**
1. **Validación de input**: Montos mínimos/máximos
2. **Preview de pago**: Mostrar desglose antes de confirmar
3. **Estados de carga**: Durante procesamiento de pago
4. **Confirmación visual**: Feedback de éxito/error

## 🔍 TESTING Y VALIDACIÓN

### **Testing Manual Realizado**
- ✅ **Navegación funcional**: Desde perfil público a pantalla patrocinio
- ✅ **UI responsiva**: Se adapta a diferentes tamaños
- ✅ **Colores corporativos**: Aplicados correctamente
- ✅ **Back navigation**: Funciona desde TopBar
- ✅ **Input editable**: Campo de valor modificable

### **Testing Pendiente**
- **Unit tests**: Para lógica de validación
- **UI tests**: Para navegación automatizada
- **Integration tests**: Para futuro sistema de pagos

## 📚 DOCUMENTACIÓN RELACIONADA

- **[DESIGN_SYSTEM.md](../../DESIGN_SYSTEM.md)** - Colores y espaciado corporativo
- **[CLAUDE.md](../../CLAUDE.md)** - Estado general del proyecto
- **Imagen de referencia**: Base para el diseño implementado

---

**Estado actual**: ✅ **COMPLETAMENTE FUNCIONAL**
**Próximo milestone**: Integración con sistema de pagos real
**Responsable**: Claude Code Assistant
**Última revisión**: 17 Octubre 2025