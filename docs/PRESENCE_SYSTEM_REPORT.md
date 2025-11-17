# Sistema de Presencia en Tiempo Real - Informe Técnico

**Fecha**: 2025-09-27
**Estado**: Implementación parcial con limitaciones de escalabilidad
**Prioridad**: Alta para implementación definitiva

## 📊 Estado Actual del Sistema

### Implementación Actual (Workaround Temporal)

El sistema de presencia actualmente funciona mediante un "hack" que usa la mutation `updateTotalScore` con valores especiales:

- **totalScore = -2**: Usuario ONLINE
- **totalScore = -1**: Usuario OFFLINE
- **totalScore >= 0**: Valores normales de puntos (ignorados por el sistema de presencia)

### Arquitectura Actual

```
┌─────────────────────────────────────────────┐
│             Cliente (App Android)            │
├─────────────────────────────────────────────┤
│  PresenceManager                             │
│  ├── goOnline(userId)                        │
│  ├── goOffline(userId)                       │
│  └── subscribeToUserPresence(userId)         │
└──────────────┬──────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────┐
│            AWS AppSync (GraphQL)             │
├─────────────────────────────────────────────┤
│  Mutation: updateTotalScore                  │
│  Subscription: onProfileUpdate(userId: ID!)  │
└──────────────┬──────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────┐
│              DynamoDB Tables                 │
├─────────────────────────────────────────────┤
│  BIILIVEDB-USERS                            │
│  └── totalScore: -2 (online) / -1 (offline) │
└─────────────────────────────────────────────┘
```

## ⚠️ Limitaciones Críticas

### 1. Subscription Requiere userId Específico

**Problema**: La subscription `onProfileUpdate` en AppSync requiere un `userId` obligatorio como argumento.

```graphql
# ❌ NO FUNCIONA - No se puede escuchar a todos
subscription {
    onProfileUpdate {
        userId
        totalScore
    }
}

# ✅ FUNCIONA - Pero solo para UN usuario
subscription($userId: ID!) {
    onProfileUpdate(userId: $userId) {
        userId
        totalScore
    }
}
```

**Implicaciones**:
- No existe broadcast global de cambios
- Se requiere una subscription WebSocket por cada usuario monitoreado
- Límite práctico de ~100-200 usuarios simultáneos por cliente

### 2. Escalabilidad

| Usuarios Seguidos | Subscriptions Requeridas | Viabilidad | Problema |
|-------------------|-------------------------|------------|----------|
| 1-10 | 1-10 | ✅ Excelente | Ninguno |
| 10-50 | 10-50 | ✅ Bueno | Mínimo overhead |
| 50-200 | 50-200 | ⚠️ Límite | Alto consumo de recursos |
| 200-1000 | 200-1000 | ❌ No viable | Sobrecarga de WebSockets |
| 1000+ | 1000+ | ❌ Imposible | Límites de AWS superados |

### 3. Costos de AWS

Cada subscription activa consume:
- **Conexión WebSocket**: $0.25 por millón de minutos de conexión
- **Mensajes**: $1.00 por millón de mensajes
- **Data Transfer**: Costo adicional por GB transferido

**Ejemplo de costo mensual** (1000 usuarios activos):
- Si cada usuario sigue a 100 personas = 100,000 subscriptions
- Costo estimado: ~$500-1000/mes solo en presencia

### 4. Complejidad de Gestión

```kotlin
// Código actual - No escalable
fun subscribeToMultipleUsers(userIds: List<String>) {
    userIds.forEach { userId ->
        subscribeToUserPresence(userId)  // Crear subscription individual
    }
}
```

**Problemas**:
- Gestión manual de ciclo de vida de subscriptions
- Memory leaks si no se limpian correctamente
- Reconexión compleja tras pérdida de conectividad

## 🎯 Soluciones Propuestas

### Solución 1: Subscription Global con Lambda (RECOMENDADA)

**Arquitectura Propuesta**:

```
┌─────────────────────────────────────────────┐
│             Cliente (App Android)            │
├─────────────────────────────────────────────┤
│  Una sola subscription: onPresenceUpdates    │
└──────────────┬──────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────┐
│            AWS AppSync (GraphQL)             │
├─────────────────────────────────────────────┤
│  NEW: subscription onPresenceUpdates {       │
│    userId, status, timestamp                 │
│  }                                           │
└──────────────┬──────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────┐
│            AWS Lambda Function               │
├─────────────────────────────────────────────┤
│  presenceBroadcastHandler()                  │
│  - Recibe cambio de estado                   │
│  - Identifica usuarios interesados           │
│  - Envía notificación a subscriptores        │
└──────────────┬──────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────┐
│              DynamoDB Tables                 │
├─────────────────────────────────────────────┤
│  NEW: PRESENCE-STATUS                        │
│  - PK: userId                                │
│  - status: online/offline                    │
│  - lastSeen: timestamp                       │
│  - connections: [deviceIds]                  │
└─────────────────────────────────────────────┘
```

**Ventajas**:
- ✅ Una sola subscription por cliente
- ✅ Escalable a millones de usuarios
- ✅ Filtrado inteligente en servidor
- ✅ Menor costo de AWS

**Implementación en GraphQL Schema**:

```graphql
type Subscription {
    # Nueva subscription global (sin argumentos requeridos)
    onPresenceUpdates: PresenceUpdate
        @aws_subscribe(mutations: ["updatePresence"])
}

type PresenceUpdate {
    userId: ID!
    status: PresenceStatus!
    timestamp: AWSTimestamp!
    deviceCount: Int
}

enum PresenceStatus {
    ONLINE
    OFFLINE
    AWAY
    BUSY
}

type Mutation {
    # Nueva mutation dedicada para presencia
    updatePresence(
        userId: ID!
        status: PresenceStatus!
        deviceId: String
    ): PresenceUpdate
}
```

### Solución 2: Sistema Híbrido (Temporal)

Mientras se implementa la solución definitiva:

```kotlin
class HybridPresenceManager {
    // Subscription directa para usuarios críticos (máx 50)
    private val criticalUsers = mutableSetOf<String>()

    // Polling para el resto
    private val pollingUsers = mutableSetOf<String>()

    fun manageUserPresence(userId: String, isFollowing: Boolean) {
        when {
            isFollowing && criticalUsers.size < 50 -> {
                subscribeDirectly(userId)
                criticalUsers.add(userId)
            }
            else -> {
                addToPolling(userId)
                pollingUsers.add(userId)
            }
        }
    }

    // Polling cada 30 segundos para usuarios no críticos
    private fun startPolling() {
        timer.schedule(30_000) {
            batchCheckPresence(pollingUsers)
        }
    }
}
```

### Solución 3: DynamoDB Streams + EventBridge

**Arquitectura basada en eventos**:

1. DynamoDB Stream captura cambios en tabla PRESENCE-STATUS
2. Lambda procesa el stream
3. EventBridge distribuye eventos
4. AppSync Subscriptions reciben eventos filtrados

```yaml
# serverless.yml ejemplo
functions:
  presenceStreamHandler:
    handler: src/handlers/presenceStream.handler
    events:
      - stream:
          type: dynamodb
          arn: !GetAtt PresenceTable.StreamArn
          filterPatterns:
            - eventName: [INSERT, MODIFY]
    environment:
      EVENTBRIDGE_BUS: !Ref PresenceEventBus
```

## 📋 Plan de Implementación Recomendado

### Fase 1: Preparación (1 semana)
- [ ] Diseñar nueva tabla DynamoDB PRESENCE-STATUS
- [ ] Crear esquema GraphQL actualizado
- [ ] Documentar flujos de datos

### Fase 2: Backend (2 semanas)
- [ ] Implementar Lambda presenceBroadcastHandler
- [ ] Crear nueva mutation updatePresence
- [ ] Configurar subscription onPresenceUpdates
- [ ] Implementar filtrado inteligente basado en relaciones sociales

### Fase 3: Migración Cliente (1 semana)
- [ ] Actualizar PresenceManager para usar nueva API
- [ ] Implementar fallback al sistema actual
- [ ] Testing con grupos pequeños

### Fase 4: Rollout (1 semana)
- [ ] Deploy gradual por porcentaje de usuarios
- [ ] Monitoreo de métricas y costos
- [ ] Ajuste de parámetros de performance

## 💰 Análisis de Costos

### Sistema Actual (No escalable)
```
1000 usuarios × 100 seguidos = 100,000 subscriptions
Costo mensual: ~$500-1000
```

### Sistema Propuesto (Escalable)
```
1000 usuarios × 1 subscription = 1,000 subscriptions
+ Lambda invocations + DynamoDB
Costo mensual: ~$50-100
```

**Ahorro estimado: 90% en costos de infraestructura**

## 🔧 Configuración AWS Requerida

### 1. IAM Roles
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "dynamodb:PutItem",
                "dynamodb:GetItem",
                "dynamodb:Query",
                "dynamodb:StreamRead"
            ],
            "Resource": "arn:aws:dynamodb:*:*:table/PRESENCE-STATUS*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "appsync:GraphQL"
            ],
            "Resource": "*"
        }
    ]
}
```

### 2. DynamoDB Table Design
```yaml
PRESENCE-STATUS:
  PK: userId (String)
  SK: "STATUS" (String)
  Attributes:
    - status: String (online/offline/away)
    - lastSeen: Number (timestamp)
    - deviceCount: Number
    - connections: List<String> (deviceIds)
  GSI1:
    - GSI1PK: status (String)
    - GSI1SK: lastSeen (Number)
  TTL:
    - ttl: lastSeen + 300 (5 minutos)
```

### 3. Lambda Function (Python)
```python
import boto3
import json
from datetime import datetime

dynamodb = boto3.resource('dynamodb')
appsync = boto3.client('appsync')

def handler(event, context):
    """
    Procesa cambios de presencia y notifica a subscriptores
    """
    for record in event['Records']:
        if record['eventName'] in ['INSERT', 'MODIFY']:
            user_id = record['dynamodb']['Keys']['PK']['S']
            new_status = record['dynamodb']['NewImage']['status']['S']

            # Obtener lista de interesados (followers + following)
            interested_users = get_interested_users(user_id)

            # Enviar actualización via AppSync
            broadcast_presence_update(user_id, new_status, interested_users)

    return {'statusCode': 200}

def get_interested_users(user_id):
    """
    Obtiene usuarios que deben recibir esta actualización
    Basado en relaciones de follow/following
    """
    # Query BIIHLIVE-SOCIAL-V2 table
    # Return list of userIds
    pass

def broadcast_presence_update(user_id, status, recipients):
    """
    Envía actualización a través de AppSync subscriptions
    """
    mutation = """
        mutation PublishPresence($input: PresenceInput!) {
            publishPresenceUpdate(input: $input) {
                userId
                status
                timestamp
            }
        }
    """

    for recipient in recipients:
        appsync.graphql(
            query=mutation,
            variables={
                'input': {
                    'userId': user_id,
                    'status': status,
                    'recipientId': recipient,
                    'timestamp': datetime.now().isoformat()
                }
            }
        )
```

## 🎯 Métricas de Éxito

| Métrica | Actual | Objetivo | Medición |
|---------|--------|----------|----------|
| Latencia de actualización | 2-5 segundos | <1 segundo | CloudWatch |
| Subscriptions por usuario | 50-200 | 1 | AppSync Metrics |
| Costo mensual | $500-1000 | <$100 | AWS Cost Explorer |
| Usuarios concurrentes soportados | ~100 | 10,000+ | Load Testing |
| Tasa de error | 5% | <0.1% | CloudWatch Alarms |

## 🔍 Investigación Adicional Requerida

1. **AppSync Custom Resolvers**
   - Pipeline resolvers para lógica compleja
   - Direct Lambda resolvers
   - Batch resolvers para optimización

2. **WebSocket API Gateway**
   - Como alternativa a AppSync Subscriptions
   - Mayor control pero más complejidad

3. **AWS IoT Core**
   - Para presencia en tiempo real masiva
   - Pub/Sub con topics dinámicos

4. **ElastiCache/Redis**
   - Cache de estados de presencia
   - Reducir lecturas a DynamoDB

## 📝 Notas Finales

El sistema actual es funcional pero **NO ES PRODUCCIÓN-READY** para una aplicación con miles de usuarios. La implementación de una subscription global con Lambda es **CRÍTICA** para el éxito a largo plazo de la aplicación.

**Recomendación**: Priorizar la implementación de la Solución 1 (Lambda + Subscription Global) en el próximo sprint de desarrollo.

---

*Documento preparado para revisión e implementación futura del sistema de presencia escalable.*

*Para consultas técnicas o aclaraciones, revisar la implementación actual en:*
- `/presentation/presence/PresenceManager.kt`
- `/data/repository/AppSyncRepository.kt`