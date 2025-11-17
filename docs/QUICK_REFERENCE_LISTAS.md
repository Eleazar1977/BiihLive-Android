# 🚀 Quick Reference - User Lists Design Rules
## Biihlive - Guía Rápida de Implementación

---

## 📏 Dimensiones (Copiar y Pegar)

```kotlin
// Avatar y borde
val AVATAR_SIZE = 53.dp
val AVATAR_BORDER = 2.dp
val AVATAR_IMAGE_SIZE = 112 // px en cache

// Indicadores
val ONLINE_BADGE = 11.dp
val VERIFIED_BADGE = 18.dp

// Espaciados
val ITEM_PADDING_H = 16.dp
val ITEM_PADDING_V = 9.dp
val AVATAR_SPACING = 11.dp
val NAME_BADGE_SPACING = 4.dp
val TEXT_SPACING = 2.dp

// Divisores
val DIVIDER_START = 80.dp
val DIVIDER_ALPHA = 0.8f
```

---

## 📝 Tipografía (SIEMPRE usar del tema)

```kotlin
// ✅ Nombre de usuario
style = MaterialTheme.typography.titleSmall

// ✅ Descripción
style = MaterialTheme.typography.bodyMedium.copy(
    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
)

// ❌ NUNCA usar
fontSize = 16.sp  // ← NO!
fontWeight = FontWeight.Bold  // ← NO!
```

---

## 🎨 Colores Corporativos

```kotlin
val BiihliveOrange = Color(0xFFDC5A01)
val BiihliveBlue = Color(0xFF1DC3FF)
val BiihliveGreen = Color(0xFF60BF19)
```

---

## 💻 Template Básico

```kotlin
@Composable
fun MyUserList(users: List<UserPreview>, onUserClick: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(top = 2.dp, bottom = 8.dp)
    ) {
        items(items = users, key = { it.userId }) { user ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUserClick(user.userId) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar (53dp)
                DynamicBorderedAvatar(user)
                
                Spacer(modifier = Modifier.width(11.dp))
                
                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.nickname,
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (user.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            VerifiedBadge()
                        }
                    }
                    if (user.description != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = user.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // Trailing content (botones, menú, etc.)
                TrailingContent()
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(start = 80.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
```

---

## ✅ Checklist Rápido

**Antes de Commit:**
- [ ] Avatar: 53.dp ✓
- [ ] Padding vertical: 9.dp ✓
- [ ] Padding horizontal: 16.dp ✓
- [ ] Spacing avatar-texto: 11.dp ✓
- [ ] Tipografía: MaterialTheme ✓
- [ ] Divisor start: 80.dp ✓
- [ ] Cache de imágenes: 112×112px ✓
- [ ] Key en LazyColumn: userId ✓

---

## 🎯 Fórmula de Altura

```
Item Height = 9dp (top) + 53dp (avatar) + 9dp (bottom) ≈ 71dp
Con texto adicional: ~72-80dp
```

---

## 🔧 Troubleshooting Común

| Problema | Solución |
|----------|----------|
| Divisor mal alineado | `padding(start = 80.dp)` |
| Texto cortado | `maxLines = 1, overflow = Ellipsis` |
| Lentitud en scroll | Verificar cache de imágenes (112px) |
| Fuente incorrecta | Usar `MaterialTheme.typography` |
| Avatar muy grande | Debe ser 53.dp |

---

## 📞 Ayuda

Ver documento completo: `docs/REGLAS_DISEÑO_LISTAS.md`

Equipo UX/UI - Biihlive © 2025
