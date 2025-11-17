# Solución para Conflictos de Google Auth: "There is already a user signed in"

## Problema Identificado

El error **"There is already a user signed in"** ocurre cuando hay conflictos entre diferentes sistemas de autenticación de Google en la misma aplicación:

1. **GoogleSignInClient** (método clásico) - usado en `GoogleAuthViewModel`
2. **CredentialManager** (método nativo moderno) - usado en `NativeGoogleAuthViewModel`  
3. **Amplify Auth con Google** - usado en `SigningInScreen`

### Causa Raíz

- Cuando `GoogleAuthViewModel.initGoogleSignIn()` se ejecuta, crea una instancia de `GoogleSignInClient`
- Esta instancia mantiene información de sesiones activas
- Al intentar usar `CredentialManager`, detecta la sesión existente y bloquea el flujo

## Solución Implementada

### 1. Limpieza Automática de Sesiones

Se agregó en `NativeGoogleAuthViewModel` la función `clearGoogleSignInSessions()`:

```kotlin
private suspend fun clearGoogleSignInSessions(context: Context) {
    try {
        // Configuración básica para GoogleSignIn
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestProfile()
            .requestEmail()
            .build()
        
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        
        // Verificar si hay una cuenta activa
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null) {
            // Cerrar sesión de GoogleSignInClient
            googleSignInClient.signOut().await()
            // También limpiar el token
            googleSignInClient.revokeAccess().await()
        }
        
    } catch (e: Exception) {
        // No bloquear el flujo principal por errores de limpieza
    }
}
```

### 2. Flujo Modificado de Autenticación

En `signInWithGoogle()` se agregó la limpieza como primer paso:

```kotlin
fun signInWithGoogle(context: Context) {
    viewModelScope.launch {
        try {
            // PASO 1: Limpiar sesiones previas de GoogleSignInClient
            clearGoogleSignInSessions(context)
            
            // PASO 2-4: Continuar con CredentialManager
            // ...resto del flujo
        } catch (e: Exception) {
            // Manejo de errores
        }
    }
}
```

### 3. Verificación Preventiva

Se agregó `checkForGoogleSignInConflicts()` para detectar problemas antes de que ocurran:

```kotlin
fun checkForGoogleSignInConflicts(context: Context) {
    try {
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null) {
            Log.w("NativeGoogleAuth", "⚠️ ADVERTENCIA: Hay una sesión activa de GoogleSignInClient")
            Log.w("NativeGoogleAuth", "Cuenta: ${lastAccount.email}")
        }
    } catch (e: Exception) {
        Log.e("NativeGoogleAuth", "Error al verificar conflictos: ${e.message}")
    }
}
```

### 4. Mejoras en GoogleAuthViewModel

Se agregó `forceSignOutAndRevoke()` para limpiezas más agresivas:

```kotlin
fun forceSignOutAndRevoke(context: Context) {
    googleSignInClient.signOut().addOnCompleteListener { signOutTask ->
        if (signOutTask.isSuccessful) {
            googleSignInClient.revokeAccess().addOnCompleteListener { revokeTask ->
                // Limpieza completa
                _authState.value = AuthState.SignedOut
                _googleAccount.value = null
                _googleToken.value = null
            }
        }
    }
}
```

## Flujo de Aplicación Actualizado

### Arquitectura Actual

```
SignInScreen (Cognito + Google button) 
    ↓ (Google button pressed)
SigningInScreen (Amplify Auth)
    ↓ (if native approach needed)
NativeSignInScreen (CredentialManager)
```

### Cómo Funciona la Solución

1. **Inicio**: `NativeSignInScreen` se inicializa
2. **Verificación**: `checkForGoogleSignInConflicts()` reporta conflictos potenciales
3. **Usuario hace click**: Se ejecuta `signInWithGoogle()`
4. **Limpieza**: `clearGoogleSignInSessions()` elimina sesiones previas
5. **Autenticación**: CredentialManager procede sin conflictos

## Mejores Prácticas para Evitar Conflictos Futuros

### 1. Principio de Separación

**❌ No mezclar sistemas:**
```kotlin
// NO hagas esto en la misma pantalla/flujo
GoogleSignIn.getClient(context, gso) // Sistema clásico  
CredentialManager.create(context)    // Sistema moderno
```

**✅ Usa un sistema por flujo:**
```kotlin
// Elige uno por pantalla/funcionalidad
class ModernAuthViewModel {
    private val credentialManager = CredentialManager.create(context)
    // Solo CredentialManager aquí
}
```

### 2. Limpieza Proactiva

**✅ Siempre limpia antes de cambiar sistemas:**
```kotlin
// Al migrar de GoogleSignInClient a CredentialManager
fun migrateToModernAuth(context: Context) {
    // 1. Limpiar sistema anterior
    clearGoogleSignInSessions(context)
    
    // 2. Usar nuevo sistema
    useCredentialManager(context)
}
```

### 3. Logs para Debugging

**✅ Logs descriptivos:**
```kotlin
Log.d("Auth", "🔄 Migrando de GoogleSignInClient a CredentialManager")
Log.w("Auth", "⚠️ Sesión detectada: ${account.email}")
Log.d("Auth", "✅ Limpieza exitosa, procediendo con CredentialManager")
```

### 4. Manejo de Errores

**✅ Errores no críticos en limpieza:**
```kotlin
try {
    googleSignInClient.signOut().await()
} catch (e: Exception) {
    // Log pero no bloquear el flujo principal
    Log.w("Auth", "Advertencia en limpieza: ${e.message}")
}
```

## Archivos Modificados

1. **`C:\Users\asus\AndroidStudioProjects\Biihlive\Biihlive\composeApp\src\androidMain\kotlin\com\mision\biihlive\viewmodels\NativeGoogleAuthViewModel.kt`**
   - ✅ Agregado: `clearGoogleSignInSessions()`
   - ✅ Agregado: `checkForGoogleSignInConflicts()`
   - ✅ Modificado: `signInWithGoogle()` con limpieza automática

2. **`C:\Users\asus\AndroidStudioProjects\Biihlive\Biihlive\composeApp\src\androidMain\kotlin\com\mision\biihlive\screens\NativeSignInScreen.kt`**
   - ✅ Agregado: Verificación de conflictos en `LaunchedEffect`

3. **`C:\Users\asus\AndroidStudioProjects\Biihlive\Biihlive\composeApp\src\androidMain\kotlin\com\mision\biihlive\viewmodels\GoogleAuthViewModel.kt`**
   - ✅ Agregado: `forceSignOutAndRevoke()` para limpieza completa

## Testing

### Para Probar la Solución

1. **Simular el Conflicto:**
   ```kotlin
   // Usar primero GoogleSignInClient
   val googleAuthViewModel = GoogleAuthViewModel()
   googleAuthViewModel.initGoogleSignIn(context)
   
   // Luego intentar CredentialManager
   val nativeAuthViewModel = NativeGoogleAuthViewModel()
   nativeAuthViewModel.signInWithGoogle(context) // Debería funcionar ahora
   ```

2. **Verificar Logs:**
   ```
   D/NativeGoogleAuth: ⚠️ ADVERTENCIA: Hay una sesión activa de GoogleSignInClient
   D/NativeGoogleAuth: Cuenta: usuario@gmail.com
   D/NativeGoogleAuth: Limpiando sesiones previas de GoogleSignInClient...
   D/NativeGoogleAuth: ✅ Sesiones de GoogleSignInClient limpiadas exitosamente
   D/NativeGoogleAuth: Mostrando selector de cuentas nativo...
   ```

### Indicadores de Éxito

- ✅ No más errores "There is already a user signed in"
- ✅ CredentialManager muestra selector nativo correctamente
- ✅ Logs indican limpieza exitosa
- ✅ Flujo de autenticación completo funciona

## Resumen

La solución implementada:

1. **Detecta** conflictos entre sistemas de autenticación
2. **Limpia** automáticamente sesiones previas de GoogleSignInClient
3. **Procede** con CredentialManager sin interferencias
4. **Mantiene** la compatibilidad con el código existente
5. **Proporciona** logs detallados para debugging

Esto resuelve el problema de manera robusta y transparente para el usuario final.