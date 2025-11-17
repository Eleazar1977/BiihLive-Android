# ☁️ AWS Backend

## Arquitectura General
```
Mobile App ←→ CloudFront ←→ AppSync (GraphQL)
                ↓              ↓
               S3         DynamoDB
                          ↓
                      Lambda (Triggers)
```

## Servicios AWS

### 🔷 AppSync (GraphQL API)
```yaml
API ID: chxkj33wdrc3vfk6vhlcbn52w4
Endpoint: https://jn4i4tufjbef5lbtdtu4jmmzoq.appsync-api.eu-west-3.amazonaws.com/graphql
Region: eu-west-3
Auth: API_KEY + Cognito User Pools
```

### 🔷 DynamoDB Tables

#### BIILIVEDB-USERS
```yaml
PK: userId
Attributes:
  - nickname, nombreCompleto
  - seguidores, siguiendo, puntos, nivel
  - ciudad, pais, descripcion
  - isOnline, lastSeen
```

#### BIIHLIVE-SOCIAL-V2
```yaml
PK: followerId
SK: FOLLOWING#{followedId}
GSI1PK: followedId
GSI1SK: FOLLOWER#{followerId}
```

#### BIILIVEDB-CHATS
```yaml
PK: conversationId
SK: timestamp
Attributes:
  - senderId, receiverId
  - message, readStatus
```

### 🔷 S3 Buckets

#### biihlivemedia
```
Structure:
├── profile-photos/
│   └── {userId}/
│       └── photo_{timestamp}.jpg
├── fotos/
│   └── {userId}/
│       ├── full_{timestamp}.jpg
│       └── thumbnail_{timestamp}.jpg
└── videos/
    └── {userId}/
        └── video_{timestamp}.mp4
```

### 🔷 CloudFront CDN
```yaml
Distribution: d3example.cloudfront.net
Origins:
  - S3: biihlivemedia
Behaviors:
  - /profile-photos/* → Cache 24h
  - /fotos/* → Cache Forever (inmutable)
  - /videos/* → No cache
```

### 🔷 Cognito
```yaml
User Pool: eu-west-3_1QeyxVcF9
Client ID: 2vquhtd73jg37t1sf8uov9b7j2
Identity Pool: eu-west-3:bce99bf2-9c89-4cd5-a674-b68da1b75a34
Auth Flows:
  - USER_PASSWORD_AUTH
  - ALLOW_REFRESH_TOKEN_AUTH
```

### 🔷 Lambda Functions

#### UpdateUserCounters
- Trigger: DynamoDB Streams (BIIHLIVE-SOCIAL-V2)
- Acción: Actualiza contadores seguidores/siguiendo

#### ProcessProfilePhoto
- Trigger: S3 PUT (profile-photos/*)
- Acción: Resize, optimize, generate thumbnails

#### CalculateRankings
- Trigger: CloudWatch Events (daily)
- Acción: Calcula rankings por ciudad/país

## Resolvers AppSync

### Tipos de Resolvers
1. **Direct DynamoDB** - Operaciones simples
2. **Pipeline** - Operaciones múltiples tablas
3. **Lambda** - Lógica compleja

### Ejemplo VTL Resolver
```vtl
# Request
{
    "version": "2017-02-28",
    "operation": "GetItem",
    "key": {
        "userId": $util.dynamodb.toDynamoDBJson($ctx.args.userId)
    }
}

# Response
$util.toJson($ctx.result)
```

## Scripts de Gestión

### Actualizar Schema
```bash
# Convertir a base64
powershell "[Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes((Get-Content 'schema.graphql' -Raw)))" > schema.b64

# Actualizar
aws appsync start-schema-creation \
  --api-id chxkj33wdrc3vfk6vhlcbn52w4 \
  --definition "$(cat schema.b64)" \
  --region eu-west-3
```

### Invalidar CloudFront
```bash
aws cloudfront create-invalidation \
  --distribution-id E1DISTRIBUTION \
  --paths "/profile-photos/*"
```

### Consultar DynamoDB
```bash
aws dynamodb get-item \
  --table-name BIILIVEDB-USERS \
  --key '{"userId":{"S":"user123"}}' \
  --region eu-west-3
```

## Monitoreo

### CloudWatch Dashboards
- API Gateway requests/errors
- DynamoDB throttles
- Lambda invocations/errors
- S3 GET/PUT requests

### Alarmas Configuradas
- High API latency (>1s)
- DynamoDB throttling
- Lambda errors >1%
- S3 4xx/5xx errors

## Costos Estimados
- DynamoDB: ~$25/mes (on-demand)
- S3: ~$10/mes (100GB storage)
- CloudFront: ~$15/mes (1TB transfer)
- Lambda: ~$5/mes (1M invocations)
- AppSync: ~$20/mes (1M requests)