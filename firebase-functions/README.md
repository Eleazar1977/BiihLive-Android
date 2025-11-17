# Firebase Functions - Password Recovery System

## 🚀 **Sistema de Recuperación de Contraseña para Biihlive**

Este documento explica cómo configurar y desplegar las Firebase Cloud Functions para el sistema de recuperación de contraseña que reutiliza la arquitectura de códigos de verificación existente.

## 📋 **Funciones Incluidas**

### **1. sendPasswordRecoveryCode**
- **Propósito**: Envía código de 6 dígitos para recuperación de contraseña
- **Email**: Utiliza `noreply@biihlive.com` (branding corporativo)
- **Validación**: Verifica que el usuario exista en Firebase Auth
- **Seguridad**: Tokens con expiración de 10 minutos

### **2. verifyPasswordRecoveryCode**
- **Propósito**: Verifica código de 6 dígitos ingresado por el usuario
- **Límites**: Máximo 5 intentos por código
- **Validación**: Código numérico de 6 dígitos + verificación de expiración
- **Seguridad**: Control de intentos fallidos

### **3. resetPasswordWithCode**
- **Propósito**: Cambia contraseña después de verificar código válido
- **Validación**: Contraseña mínimo 6 caracteres
- **Seguridad**: Revoca todas las sesiones activas del usuario
- **Token**: Marca el código como usado tras cambio exitoso

### **4. resendPasswordRecoveryCode**
- **Propósito**: Reenvía nuevo código si el anterior expiró
- **Límites**: Un reenvío por minuto máximo
- **Funcionalidad**: Genera nuevo código con nueva expiración

### **5. cleanupExpiredTokens** (Automática)
- **Propósito**: Limpia tokens expirados cada hora
- **Programación**: Ejecuta automáticamente vía Cloud Scheduler
- **Mantenimiento**: Mantiene la base de datos limpia

## 🔧 **Configuración Requerida**

### **1. Variables de Entorno**
```bash
# Configurar en Firebase Functions
firebase functions:config:set email.user="noreply@biihlive.com"
firebase functions:config:set email.password="tu_app_password_aqui"
```

### **2. Estructura Firestore**
Las funciones crean automáticamente la colección:

```javascript
passwordRecoveryTokens/{userId}/
  email: string              // Email del usuario
  code: string              // Código de 6 dígitos
  createdAt: timestamp      // Momento de creación
  expiresAt: timestamp      // Expiración (10 min)
  attempts: number          // Intentos fallidos (max 5)
  isUsed: boolean           // Si ya fue utilizado
  usedAt: timestamp?        // Momento de uso (opcional)
```

### **3. Configuración Email SMTP**
- **Proveedor**: Gmail (configurable)
- **Email**: `noreply@biihlive.com`
- **Autenticación**: App Password (no password regular)
- **Templates**: HTML corporativo con branding Biihlive

## 🚀 **Instalación y Deployment**

### **Paso 1: Configurar Firebase CLI**
```bash
# Instalar Firebase CLI (si no está instalado)
npm install -g firebase-tools

# Hacer login
firebase login

# Configurar proyecto
firebase use biihlive-aa5c3
```

### **Paso 2: Instalar Dependencias**
```bash
# Navegar a carpeta functions
cd firebase-functions

# Instalar dependencias
npm install
```

### **Paso 3: Configurar Variables de Entorno**
```bash
# Configurar credenciales de email
firebase functions:config:set email.user="noreply@biihlive.com"
firebase functions:config:set email.password="tu_gmail_app_password"

# Verificar configuración
firebase functions:config:get
```

### **Paso 4: Testing Local (Opcional)**
```bash
# Iniciar emuladores
firebase emulators:start --only functions

# Testing con curl
curl -X POST http://localhost:5001/biihlive-aa5c3/us-central1/sendPasswordRecoveryCode \
  -H "Content-Type: application/json" \
  -d '{"data": {"email": "test@example.com"}}'
```

### **Paso 5: Deploy a Producción**
```bash
# Deploy todas las funciones
firebase deploy --only functions

# O deploy función específica
firebase deploy --only functions:sendPasswordRecoveryCode
```

### **Paso 6: Verificar Deployment**
```bash
# Ver logs
firebase functions:log

# Ver funciones desplegadas
firebase functions:list
```

## 🔒 **Configuración Gmail App Password**

### **Cómo obtener Gmail App Password:**

1. **Ir a tu cuenta Google**: https://myaccount.google.com/
2. **Seguridad** → **Verificación en 2 pasos** (debe estar activada)
3. **Contraseñas de aplicaciones** → **Seleccionar app** → **Correo**
4. **Generar contraseña** → Copiar la contraseña de 16 caracteres
5. **Usar esta contraseña** en la configuración de Firebase Functions

### **Configurar en Firebase:**
```bash
firebase functions:config:set email.password="abcd efgh ijkl mnop"
```

## 📱 **Integración con App Android**

### **Funciones llamadas desde PasswordRecoveryRepository.kt:**

```kotlin
// 1. Enviar código
functions.getHttpsCallable("sendPasswordRecoveryCode")

// 2. Verificar código
functions.getHttpsCallable("verifyPasswordRecoveryCode")

// 3. Cambiar contraseña
functions.getHttpsCallable("resetPasswordWithCode")

// 4. Reenviar código
functions.getHttpsCallable("resendPasswordRecoveryCode")
```

## 🛡️ **Seguridad Implementada**

### **Validaciones:**
- ✅ Usuario debe existir en Firebase Auth
- ✅ Códigos expiran en 10 minutos
- ✅ Máximo 5 intentos por código
- ✅ Un reenvío por minuto
- ✅ Códigos de un solo uso
- ✅ Revocación de sesiones al cambiar password

### **Rate Limiting:**
- ✅ Control de intentos fallidos
- ✅ Límite de tiempo entre reenvíos
- ✅ Limpieza automática de tokens expirados

### **Branding:**
- ✅ Emails desde `noreply@biihlive.com`
- ✅ Templates HTML corporativos
- ✅ Sujetos personalizados para Biihlive

## 📊 **Monitoreo y Logs**

### **Ver logs en tiempo real:**
```bash
# Logs de todas las funciones
firebase functions:log

# Logs específicos
firebase functions:log --only sendPasswordRecoveryCode
```

### **Métricas importantes a monitorear:**
- Emails enviados exitosamente
- Códigos verificados correctamente
- Intentos fallidos por código
- Passwords cambiados exitosamente
- Tokens expirados limpiados

## 🔄 **Flujo Completo de Recuperación**

```
1. Usuario ingresa email → sendPasswordRecoveryCode
   ↓
2. Email enviado con código → Usuario recibe código
   ↓
3. Usuario ingresa código → verifyPasswordRecoveryCode
   ↓
4. Código válido → Usuario ingresa nueva password
   ↓
5. Cambio de password → resetPasswordWithCode
   ↓
6. Password cambiada → Usuario puede hacer login
```

## 🚨 **Troubleshooting**

### **Errores Comunes:**

#### **Error: "Gmail authentication failed"**
- Verificar App Password de Gmail
- Confirmar que 2FA está activado en Gmail
- Reconfigurar variables de entorno

#### **Error: "Function not found"**
- Verificar deployment: `firebase functions:list`
- Re-deploy: `firebase deploy --only functions`

#### **Error: "User not found"**
- Usuario debe existir en Firebase Auth
- Verificar email exacto (case sensitive)

#### **Error: "Code expired"**
- Códigos expiran en 10 minutos
- Usar resend para generar nuevo código

## ✅ **Testing del Sistema**

### **Test completo:**
1. **Enviar código**: Verificar email recibido
2. **Verificar código**: Comprobar validación
3. **Cambiar password**: Confirmar cambio exitoso
4. **Login**: Verificar que nueva password funciona

¡El sistema está listo para uso en producción! 🎉