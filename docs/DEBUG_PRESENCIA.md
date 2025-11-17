# Debug Sistema de Presencia - Instrucciones

## 1. Ejecutar la app y filtrar logs

Abre una terminal y ejecuta:
```bash
adb logcat | grep -E "PRESENCIA|DEBUG_SIGNIN|UsersSearchScreen"
```

## 2. Proceso de prueba

### Dispositivo 1 (Hugo):
1. Abre la app
2. Inicia sesión con hugo@mail.com
3. **VERIFICAR EN LOGS:**
   - Debe aparecer: `[PRESENCIA] INICIANDO goOnline para usuario: [ID_HUGO]`
   - Debe aparecer: `[PRESENCIA] Usuario [ID_HUGO] marcado como ONLINE`

### Dispositivo 2 (Eleazar):
1. Abre la app
2. Inicia sesión con eleazar@mail.com
3. **VERIFICAR EN LOGS:**
   - Debe aparecer: `[PRESENCIA] INICIANDO goOnline para usuario: [ID_ELEAZAR]`
   - Debe aparecer: `[PRESENCIA] Usuario [ID_ELEAZAR] marcado como ONLINE`

### En ambos dispositivos:
4. Navega a la lista de usuarios
5. **VERIFICAR EN LOGS:**
   - `[UI] Renderizando indicador para Hugo: isOnline=true/false`
   - `[PRESENCIA] isUserOnline([ID]) -> resultado`

## 3. Qué buscar en los logs

### ✅ ÉXITO si ves:
```
[PRESENCIA] INICIANDO goOnline para usuario: e18900ce-10c1-700f-50ec-8ed9b3a84b4b
[PRESENCIA] Usuario e18900ce-10c1-700f-50ec-8ed9b3a84b4b marcado como ONLINE
[PRESENCIA] Subscription conectada
[PRESENCIA] Actualización de presencia: userId=XXX, status=online
```

### ❌ FALLO si ves:
```
[PRESENCIA] Error en subscription
[PRESENCIA] Error al notificar cambio de presencia
[PRESENCIA] isUserOnline(XXX) -> result=false (cuando debería ser true)
```

## 4. Posibles problemas

### A. PresenceManager no se inicializa
- **Síntoma**: No aparecen logs de `[PRESENCIA] INICIANDO goOnline`
- **Solución**: Verificar que el login llama a `presenceManager.goOnline()`

### B. Subscriptions no funcionan
- **Síntoma**: No aparecen logs de `handlePresenceUpdate`
- **Causa**: Las subscriptions de AppSync no están configuradas
- **Solución**: Verificar configuración de AppSync

### C. Mutation updateUserPresence no existe
- **Síntoma**: Error al notificar cambio de presencia
- **Causa**: La mutation no está en el schema de GraphQL
- **Solución**: Actualizar schema de AppSync

### D. UI no actualiza
- **Síntoma**: `isUserOnline` siempre devuelve false
- **Causa**: PresenceManager no tiene usuarios en su lista
- **Solución**: Verificar que goOnline se ejecutó correctamente

## 5. Comandos útiles

```bash
# Ver todos los logs de presencia
adb logcat | grep PRESENCIA

# Limpiar logs anteriores
adb logcat -c

# Ver solo errores
adb logcat *:E | grep PRESENCIA
```

## 6. Información actual del sistema

- **PresenceManager**: Sistema local en memoria (no usa DB)
- **Conexión**: Se establece al hacer login
- **Desconexión**: Se ejecuta al hacer logout
- **UI**: Consulta PresenceManager.isUserOnline() para mostrar indicador

## NOTA IMPORTANTE

El sistema actual NO usa subscriptions reales de AppSync porque:
1. La mutation `updateUserPresence` no existe en el schema
2. La subscription `onUserPresence` no está configurada

Por ahora el sistema es LOCAL - cada dispositivo solo ve su propio estado.
Para que funcione entre dispositivos, necesitamos configurar AppSync.