# 🚨 PATRÓN CRÍTICO: Scope en Composables

## ❌ ERROR COMÚN: "Unresolved reference 'uiState'"

### **¿Por qué ocurre?**
Las funciones `@Composable` separadas NO tienen acceso automático a las variables del scope padre.

## ✅ REGLA DE ORO
> **SIEMPRE pasa los datos necesarios como parámetros explícitos a las funciones Composable separadas**

## Ejemplos

### ❌ INCORRECTO
```kotlin
@Composable
fun ParentScreen(viewModel: MyViewModel) {
    val uiState = viewModel.uiState.collectAsState()

    // Lista de items
    LazyColumn {
        items(users) { user ->
            UserItem(user)  // ❌ NO pasa los datos necesarios
        }
    }
}

@Composable
private fun UserItem(user: User) {
    // ❌ ERROR: uiState no existe en este scope
    val imageUrl = uiState.userProfileImages[user.userId]
    val isFollowing = uiState.followingUsers.contains(user.userId)
}
```

### ✅ CORRECTO
```kotlin
@Composable
fun ParentScreen(viewModel: MyViewModel) {
    val uiState = viewModel.uiState.collectAsState()

    LazyColumn {
        items(users) { user ->
            UserItem(
                user = user,
                imageUrl = uiState.userProfileImages[user.userId],  // ✅ Pasamos el dato
                isFollowing = uiState.followingUsers.contains(user.userId)  // ✅ Pasamos el dato
            )
        }
    }
}

@Composable
private fun UserItem(
    user: User,
    imageUrl: String? = null,  // ✅ Recibimos como parámetro
    isFollowing: Boolean = false  // ✅ Recibimos como parámetro
) {
    // Usamos los parámetros, no intentamos acceder a uiState
    AsyncImage(model = imageUrl)
    Button(text = if (isFollowing) "Siguiendo" else "Seguir")
}
```

## 📋 CHECKLIST antes de extraer un Composable

1. [ ] **Identifica TODOS los datos que necesita el componente**
2. [ ] **Agrega parámetros para cada dato necesario**
3. [ ] **En la llamada, pasa los datos desde el scope padre**
4. [ ] **NO intentes acceder a variables del scope padre directamente**

## Ventajas de este patrón

✅ **Testeable**: Puedes probar componentes aislados
✅ **Reusable**: El componente no depende de un estado específico
✅ **Claro**: Los parámetros documentan qué datos necesita
✅ **Sin errores**: Evitas "Unresolved reference"

## Casos comunes en Biihlive

### UserItem / UserRow
```kotlin
// Siempre necesita:
- user: UserPreview
- imageUrl: String? (desde uiState.userProfileImages)
- isFollowing: Boolean (desde uiState.followingUsers)
- onClick: () -> Unit
- onFollowClick: () -> Unit
```

### PerfilInfo / ProfileContent
```kotlin
// Siempre necesita:
- perfil: PerfilUsuario
- profileImageUrl: String?
- profileThumbnailUrl: String?
- siguienteNivel: Int
- progreso: Double
```

### FullScreenImageDialog
```kotlin
// Siempre necesita:
- isVisible: Boolean
- perfil: PerfilUsuario
- profileImageUrl: String? (NO uiState.profileImageUrl)
- onDismiss: () -> Unit
```

## 🔴 RECORDATORIO FINAL

**ANTES de crear cualquier función @Composable separada:**
1. Lista TODOS los datos que usa del estado
2. Conviértelos en parámetros
3. Pásalos desde el componente padre

**NUNCA** intentes acceder a `uiState`, `viewModel` o cualquier variable del scope padre desde una función Composable separada.

---
*Última actualización: 2025-01-07*
*Errores evitados: 100% cuando se sigue este patrón*