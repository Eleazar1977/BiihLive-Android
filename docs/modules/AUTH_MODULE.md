# 🔐 Módulo de Autenticación

## Descripción
Sistema de autenticación con Firebase Auth y Google OAuth.

## Archivos Principales
```
viewmodels/
└── FirebaseAuthViewModel.kt     # Auth con Firebase (único)

core/managers/
└── UserIdManager.kt             # Gestión centralizada de identidad

utils/
└── SessionManager.kt            # Persistencia de sesión local
```

## Configuración Firebase
```yaml
Project ID: biihlive-aa5c3
Database: basebiihlive (no default)
Auth Providers: Email/Password, Google
Region: Multi-región
```

## Flujos Implementados

### 1. Registro con Email + Verificación de 6 Dígitos ✅
```kotlin
// Flujo completo implementado (13 Nov 2025)
SignUpScreen -> FirebaseAuthViewModel.signUp(email, password)
    -> Firebase Auth.createUser()
    -> Cloud Function.sendEmailVerificationCode()
    -> AuthState.SignUpRequiresConfirmation
    -> Auto-navegación a EmailVerificationScreen
    -> Usuario ingresa 6 dígitos
    -> Cloud Function.verifyEmailCode()
    -> Firebase Auth.updateUser(emailVerified=true)
    -> UserIdManager.updateCache()
```

### 2. Login
```kotlin
// Email/Password
signIn(email, password) -> Firebase Auth -> SessionManager.saveUserId()

// Google OAuth
googleSignIn() -> Firebase Auth -> UserIdManager.updateCache()
```

### 3. Recuperación de Contraseña
```kotlin
forgotPassword(email) -> confirmForgotPassword(code, newPassword)
```

## Tokens y Sesión
- **AccessToken**: Para API calls (1 hora)
- **RefreshToken**: Para renovar sesión (30 días)
- **IdToken**: Info del usuario
- Guardados en: SharedPreferences (Android)

## Manejo de Errores
| Error | Mensaje Usuario | Acción |
|-------|----------------|---------|
| UserNotFoundException | "Usuario no encontrado" | Mostrar registro |
| NotAuthorizedException | "Credenciales incorrectas" | Retry |
| UserNotConfirmedException | "Confirma tu email" | Ir a confirmación |
| NetworkError | "Sin conexión" | Retry automático |

## Estados del ViewModel

### AuthState (Sealed Class) ✅
```kotlin
// Estados específicos implementados en FirebaseAuthViewModel
sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class SignUpRequiresConfirmation(val email: String) : AuthState()  // ← CLAVE para navegación
    data class Error(val message: String) : AuthState()
}
```

### UiState (Data Class)
```kotlin
data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null,
    val user: FirebaseUser? = null
)
```

### Navigation Fix (13 Nov 2025) ✅
```kotlin
// PROBLEMA RESUELTO: SignUpScreen no navegaba automáticamente
// CAUSA: Escuchaba isAuthenticated en lugar de AuthState.SignUpRequiresConfirmation

// SignUpScreen.kt - Solución implementada:
val authState by authViewModel.authState.collectAsState()
LaunchedEffect(authState) {
    when (authState) {
        is AuthState.SignUpRequiresConfirmation -> {
            onNavigateToConfirmation(email)  // ✅ Navega automáticamente
        }
        else -> { }
    }
}
```

## Integración con Google
1. **Web Client ID**: `1234567890-abc.apps.googleusercontent.com`
2. **SHA-1**: Configurado en Firebase Console
3. **Scopes**: email, profile, openid

## Testing
```bash
# Test login
aws cognito-idp initiate-auth \
  --client-id 2vquhtd73jg37t1sf8uov9b7j2 \
  --auth-flow USER_PASSWORD_AUTH \
  --auth-parameters USERNAME=test@email.com,PASSWORD=Test123!
```

## Estado Actual - 13 Noviembre 2025

### ✅ COMPLETADO:
- **Registro con verificación por email**: ✅ Funcionando con códigos de 6 dígitos
- **Firebase Auth integración**: ✅ Completamente operativa
- **Cloud Functions**: ✅ Deployadas y funcionando (v2 + Node.js 20)
- **AWS SES**: ✅ Envío de emails desde noreply@biihlive.com
- **Navegación automática**: ✅ **REPARADA** - SignUpScreen → EmailVerificationScreen
- **Estados de autenticación**: ✅ AuthState con navegación específica
- **EmailVerificationScreen**: ✅ UI completa con 6 campos de entrada

### 🔧 BUGS RESUELTOS:
- **SignUpScreen navegación**: ✅ Corregida escucha de AuthState.SignUpRequiresConfirmation
- **Firebase Functions DB**: ✅ Conectadas a "basebiihlive" en lugar de default
- **Email delivery**: ✅ Códigos llegan correctamente a emails reales

### 📱 FLUJO DE USUARIO OPERATIVO:
```
SignUp → Firebase Auth → Cloud Functions → AWS SES → EmailVerificationScreen → Verificación → Home
```

### 🧪 READY FOR TESTING:
- ✅ Compilación exitosa
- ✅ APK instalado en dispositivo
- ✅ Sistema end-to-end funcional

## Próximas Mejoras
- [ ] Biometría (huella/face)
- [ ] Login con Apple
- [ ] MFA (Multi-Factor Auth)
- [ ] Session persistence mejorada
- [ ] Rate limiting en verificación de códigos
- [ ] Analytics de conversión de registro