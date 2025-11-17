# 📝 EDITAR PERFIL MODULE

## 🎯 **DESCRIPCIÓN**
Módulo completo para la edición de perfiles de usuario con todos los campos del modelo CSV. Permite actualizar información personal, configuración de privacidad, preferencias y localización.

## 📂 **ESTRUCTURA DE ARCHIVOS**
```
presentation/perfil/
├── EditarPerfilScreen.kt          # Pantalla principal de edición
└── PerfilPersonalLogueadoScreen.kt # Navegación desde botón "Editar Perfil"

navigation/
├── Screen.kt                       # Ruta EditarPerfil agregada
└── AppNavigation.kt               # Composable de navegación
```

## 🎨 **COMPONENTES PRINCIPALES**

### **EditarPerfilScreen**
- **Pantalla principal** con LazyColumn scrollable
- **TopBar** con botón guardar y navegación back
- **Secciones organizadas** por funcionalidad

### **AvatarSection**
- **Avatar circular** de 120dp con borde corporativo
- **Badge de editar** con ícono de cámara
- **Image picker** integrado para cambio de foto
- **Texto instructivo** "Tocar para cambiar foto"

### **CamposTextoSection**
- **Nombre completo** (OutlinedTextField)
- **Nickname** (OutlinedTextField)
- **Descripción** (OutlinedTextField multilínea, 3-5 líneas)

### **SwitchesSection**
- **Mostrar estado** online/offline (Switch con BiihliveGreen)
- **Compartir geolocalización** con permisos móvil
- **Textos explicativos** para cada opción

### **PreferenciasSection**
- **Preferencia ranking**: Local/Nacional/Mundial (ExposedDropdownMenuBox)
- **Tipo cuenta**: Personal/Empresa (ExposedDropdownMenuBox)

### **LocalizacionSection**
- **País**: Solo España hardcodeado
- **Provincia**: 17 provincias españolas
- **Ciudad**: Por provincia (Madrid → Madrid, Getafe, Móstoles...)
- **Desplegables en cascada** con reset automático

## 📊 **MODELO DE DATOS**

### **Campos Editables (del CSV)**
```kotlin
// Campos de texto
fullName: String           // Nombre completo
nickname: String          // Nickname único
description: String       // Descripción personal

// Switches de privacidad
mostrarEstado: Boolean     // Mostrar estado online/offline
compartirGeolocalizacion: Boolean // Usar ubicación para rankings

// Desplegables de preferencias
rankingPreference: String  // "local", "nacional", "mundial"
tipoCuenta: String        // "persona", "empresa"

// Localización (hardcodeada España)
pais: String              // "España"
provincia: String         // Provincia seleccionada
ciudad: String           // Ciudad por provincia
```

## 🎨 **SISTEMA DE DISEÑO**

### **Colores Aplicados**
- **BiihliveBlue** (#1DC3FF): Bordes focused, labels, dropdowns
- **BiihliveOrange** (#FF7300): Badge de cámara, botón guardar
- **BiihliveGreen** (#60BF19): Switches activos
- **Material Theme**: Colores adaptativos para backgrounds

### **Espaciado y Tipografía**
- **Padding horizontal**: 16dp consistente
- **Vertical spacing**: 16dp entre secciones, 12dp entre campos
- **Corner radius**: 8dp para campos y botones
- **Typography**: Material Design 3 con FontWeight apropiado

## 🧭 **NAVEGACIÓN**

### **Rutas Definidas**
```kotlin
// Screen.kt
object EditarPerfil : Screen("editar_perfil")

// AppNavigation.kt
composable(Screen.EditarPerfil.route) {
    EditarPerfilScreen(navController = navController)
}
```

### **Flujo de Navegación**
```
PerfilPersonalLogueadoScreen → Botón "Editar Perfil"
→ EditarPerfilScreen → TopBar "Guardar" → Volver
```

## 📱 **DATOS HARDCODEADOS**

### **Perfil Demo (Eleazar)**
```kotlin
PerfilUsuario(
    userId = "d159109e-1001-70e2-7415-37944d99d7d3",
    nickname = "Eleazar",
    fullName = "",
    description = "No te alegres de mi suerte...",
    ubicacion = Ubicacion(
        ciudad = "Madrid",
        provincia = "Madrid",
        pais = "España"
    )
)
```

### **Localización España**
```kotlin
val provinciasEspana = listOf(
    "Madrid", "Barcelona", "Valencia", "Sevilla", "Murcia",
    "Vizcaya", "Alicante", "Cádiz", "A Coruña", "Asturias"...
)

val ciudadesPorProvincia = mapOf(
    "Madrid" to listOf("Madrid", "Getafe", "Móstoles"...),
    "Barcelona" to listOf("Barcelona", "Hospitalet de Llobregat"...)
)
```

## 🔄 **ESTADO DE DESARROLLO**

### **✅ IMPLEMENTADO**
- [x] Pantalla completa con todos los campos del CSV
- [x] Avatar con sistema de cambio de imagen
- [x] Campos de texto con validación visual
- [x] Switches de privacidad funcionales
- [x] Desplegables en cascada para localización
- [x] Navegación conectada desde perfil personal
- [x] Sistema de colores corporativo aplicado
- [x] Responsive design para diferentes pantallas

### **🚧 PENDIENTE (Futuro)**
- [ ] Integración con backend para guardar cambios
- [ ] Validaciones de campos (nickname único, etc.)
- [ ] Upload real de avatar a S3
- [ ] Permisos de geolocalización del dispositivo
- [ ] Integración con Amazon Location Service
- [ ] Estados de carga y error
- [ ] Confirmación de cambios guardados

## 🔧 **INTEGRACIÓN FUTURA**

### **Backend Integration**
```kotlin
// ViewModel futuro
class EditarPerfilViewModel {
    fun guardarCambios(perfil: PerfilUsuario) {
        // AppSync mutation updatePerfilUsuario
        // Validar campos únicos
        // Actualizar DynamoDB
        // Refresh cache de perfil
    }
}
```

### **Validaciones Necesarias**
- **Nickname único** en BIILIVEDB-USERS
- **Email válido** si se agrega campo
- **Límites de caracteres** en descripción
- **Formatos válidos** para campos de texto

## 📋 **TESTING**

### **Casos de Prueba**
1. **Navegación**: Botón "Editar Perfil" → Pantalla → Guardar → Volver
2. **Cambio avatar**: Tocar avatar → Gallery picker → Preview
3. **Campos texto**: Editar y validar límites
4. **Switches**: Toggle estados y verificar colores
5. **Localización**: Cascada país → provincia → ciudad
6. **Responsive**: Diferentes tamaños de pantalla

### **Estados Edge**
- **Campos vacíos**: Comportamiento correcto
- **Textos largos**: Overflow y truncado
- **Sin permisos**: Gallery picker manejo de errores
- **Rotación pantalla**: Conservar estado

---

**Última actualización**: 2025-10-16
**Estado**: ✅ Completamente implementado e integrado
**Commit**: `16f77b6` - feat: Implementar pantalla de editar perfil
**Próximo**: Integración con backend y validaciones