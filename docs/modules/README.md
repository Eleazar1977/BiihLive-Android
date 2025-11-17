# 📚 Documentación Modular Biihlive

## 🎯 Propósito
Esta carpeta contiene documentación detallada de cada módulo del proyecto Biihlive, organizada para facilitar el acceso a información específica sin sobrecargar el contexto principal.

## 📖 Cómo Usar Esta Documentación

### Para Contexto General
Lee [`CLAUDE.md`](../../CLAUDE.md) en la raíz del proyecto - contiene información esencial en ~120 líneas.

### Para Trabajo Específico
Carga solo el módulo relevante:
- Trabajando en autenticación → [`AUTH_MODULE.md`](AUTH_MODULE.md)
- Trabajando en perfiles → [`PROFILE_MODULE.md`](PROFILE_MODULE.md)
- Problemas técnicos → [`TROUBLESHOOTING.md`](TROUBLESHOOTING.md)

## 📦 Módulos Disponibles

### Core Features
- [`AUTH_MODULE.md`](AUTH_MODULE.md) - Sistema de autenticación con Cognito
- [`PROFILE_MODULE.md`](PROFILE_MODULE.md) - Perfiles de usuario y fotos S3
- [`SOCIAL_MODULE.md`](SOCIAL_MODULE.md) - Follow/unfollow y relaciones
- [`CHAT_MODULE.md`](CHAT_MODULE.md) - Sistema de mensajería

### En Desarrollo
- [`MEDIA_MODULE.md`](MEDIA_MODULE.md) - Videos, fotos y live streaming
- [`POINTS_MODULE.md`](POINTS_MODULE.md) - Gamificación y rankings

### Infraestructura
- [`AWS_BACKEND.md`](AWS_BACKEND.md) - Servicios AWS y configuración
- [`TROUBLESHOOTING.md`](TROUBLESHOOTING.md) - Solución de problemas comunes

## 🔄 Mantenimiento

### Al Agregar Features
1. Actualizar el módulo correspondiente
2. Si es feature nueva, crear nuevo archivo MODULE_NAME.md
3. Actualizar tabla en [`CLAUDE.md`](../../CLAUDE.md)
4. Mantener cada archivo bajo 300 líneas

### Al Resolver Problemas
1. Documentar en [`TROUBLESHOOTING.md`](TROUBLESHOOTING.md)
2. Incluir: Error → Causa → Solución

## 📊 Estado de Módulos

| Módulo | Líneas | Última Actualización | Completitud |
|--------|--------|---------------------|-------------|
| AUTH | ~90 | 2025-01-30 | ✅ 100% |
| PROFILE | ~100 | 2025-01-30 | ✅ 100% |
| SOCIAL | ~120 | 2025-01-30 | ✅ 100% |
| CHAT | ~150 | 2025-01-30 | ⚠️ 70% |
| MEDIA | ~250 | 2025-01-30 | 🚧 40% |
| POINTS | ~200 | 2025-01-30 | 🚧 30% |
| AWS_BACKEND | ~180 | 2025-01-30 | ✅ 95% |
| TROUBLESHOOTING | ~200 | 2025-01-30 | 📝 Continuo |

## 💡 Tips para Claude/AI

### Carga Eficiente
```
# Solo contexto esencial
Lee CLAUDE.md

# Para feature específica
Lee CLAUDE.md y docs/modules/SOCIAL_MODULE.md

# Para debugging
Lee docs/modules/TROUBLESHOOTING.md y docs/modules/AWS_BACKEND.md
```

### Evitar Sobrecarga
- NO cargar todos los módulos a la vez
- NO incluir módulos irrelevantes para la tarea
- SI la tarea toca múltiples módulos, cargar incrementalmente

## 🚀 Quick Links

### Documentación Principal
- [`../../CLAUDE.md`](../../CLAUDE.md) - Core documentation
- [`../../CLAUDE_INSTRUCTIONS.md`](../../CLAUDE_INSTRUCTIONS.md) - Development rules
- [`../../DESIGN_SYSTEM.md`](../../DESIGN_SYSTEM.md) - UI/UX guidelines

### Scripts y Herramientas
- `../../scripts/` - AWS setup scripts
- `../../docs/estado_front.md` - Frontend status
- `../../docs/estado_back.md` - Backend status

---
*Esta estructura modular permite mantener contexto manejable y acceso rápido a información específica.*