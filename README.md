# 📱 Biihlive - Kotlin Multiplatform Project

> **⚠️ IMPORTANTE**: Este es un proyecto **Kotlin Multiplatform (KMP)** con Jetpack Compose Multiplatform, NO un proyecto Android tradicional.

## 🎯 Descripción

Biihlive es una aplicación social desarrollada con Kotlin Multiplatform targeting Android e iOS, que utiliza AWS como backend (Cognito, AppSync GraphQL, DynamoDB, S3, CloudFront).

## 🏗️ Arquitectura del Proyecto (KMP)

### Estructura Principal
- **`/composeApp`** - Aplicación principal compartida con Compose Multiplatform
  - `commonMain` - Código común para todas las plataformas
  - `androidMain` - Código específico de Android
  - `iosMain` - Código específico de iOS

- **`/iosApp`** - Punto de entrada para iOS (SwiftUI wrapper)
- **`/shared`** - Código compartido entre plataformas
- **`/aws-config`** - Scripts y configuración AWS
- **`/docs`** - Documentación completa del proyecto

## ✅ Funcionalidades Implementadas

### **Core Features**
- 🔐 **Autenticación**: Cognito + Google Sign-In nativo
- 👥 **Perfiles completos**: Con fotos, galería personal vía S3/CloudFront
- 🤝 **Sistema social**: Follow/unfollow con actualización optimista
- 🔍 **Búsqueda de usuarios**: Con filtros y estados de seguimiento
- ✅ **Sistema de verificación**: Badges azules para usuarios verificados
- 🔄 **Presencia en tiempo real**: Estados online/offline
- 📱 **UI/UX**: Material Design 3, temas claro/oscuro adaptativos

### **Características Técnicas**
- 🏗️ **AppSync GraphQL**: 90% migrado, queries/mutations robustas
- 📊 **Estados de UI**: StateFlow unidireccional, manejo de errores
- 🖼️ **Sistema de imágenes**: Upload, compresión, URLs dinámicas
- 🎨 **Sistema de diseño**: Colores consistentes, componentes reutilizables

## 🚀 Comandos de Desarrollo

```bash
# Build Android
./gradlew assembleDebug

# Instalar en dispositivo Android
./gradlew installDebug

# Clean build
./gradlew clean
```

## 📖 Documentación

- **[CLAUDE.md](CLAUDE.md)** - Documentación técnica completa
- **[CLAUDE_INSTRUCTIONS.md](CLAUDE_INSTRUCTIONS.md)** - Instrucciones para Claude Code
- **[docs/](docs/)** - Documentación adicional y backups

## 🔧 Configuración AWS

El proyecto requiere configuración AWS (ya incluida en `aws-config/`):
- Cognito User Pool + Identity Pool
- AppSync GraphQL API
- DynamoDB para datos
- S3 + CloudFront para imágenes

## 📱 Plataformas Soportadas

- ✅ **Android** (Principal - completamente implementado)
- 🚧 **iOS** (Estructura preparada, pendiente implementación específica)

## 🔄 Estado del Proyecto (Octubre 2025)

### **✅ Estable y Funcional**
- Sistema de perfiles con fotos y galería
- Autenticación y gestión de sesiones
- Sistema social (follow/unfollow, búsqueda usuarios)
- Sistema de presencia online/offline
- UI/UX con Material Design 3

### **🚧 En Desarrollo**
- **Sistema de Chat**: Marcado como deprecated, reimplementación pendiente con AppSync GraphQL
- Optimizaciones de rendimiento
- Notificaciones push

### **📈 Roadmap**
1. Reimplementar chat con AppSync GraphQL
2. Sistema de videos y contenido multimedia
3. Gamificación (puntos, rankings)
4. Completar implementación iOS

---

**Nota**: Para desarrollo activo, revisar **[CLAUDE.md](CLAUDE.md)** que contiene el estado actual detallado del proyecto con arquitectura técnica y flujos implementados.