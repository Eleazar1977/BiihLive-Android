# 📊 ESTADO BACKEND - BIIHLIVE
*Última actualización: 04/09/2025 01:35*

## 🎯 RESUMEN
**Stack Backend**: AWS (Cognito, Aurora PostgreSQL, DynamoDB, Lambda, S3)
**Región Principal**: eu-west-3 (París)
**Estado**: Autenticación y base de datos parcialmente configuradas

---

## ✅ SERVICIOS AWS CONFIGURADOS

### 1. AWS COGNITO (Autenticación)
**Estado**: ✅ OPERATIVO

#### User Pool Principal
- **Pool Name**: `biihlive-app-users`
- **Pool ID**: `eu-west-3_0ztFzMyy5`
- **Domain**: `biihlive-auth-dev`
- **Configuración**:
  - Autenticación: Email + Password
  - MFA: Opcional
  - Región: eu-west-3

#### User Pool Desarrollo
- **Pool Name**: `modulos3d254ba1`
- **Pool ID**: `eu-west-3_1QeyxVcF9`
- **Estado**: Activo (testing)

---

### 2. AURORA POSTGRESQL SERVERLESS V2
**Estado**: ✅ OPERATIVO CON TABLAS BÁSICAS

#### Cluster Principal
- **Identifier**: `biihlive-db-cluster`
- **Endpoint**: `biihlive-db-cluster.cluster-c3m0acc8255d.eu-west-3.rds.amazonaws.com`
- **Puerto**: 5432
- **Database**: `biihlivedb`
- **Usuario**: `postgres`
- **Password**: `BiihliveDB2024!`

#### Configuración
- **Engine**: Aurora PostgreSQL 15.4
- **Capacidad**: 0.5-1.0 ACUs (Serverless v2)
- **VPC**: vpc-0f60f79de5d090bca (default)
- **Security Group**: sg-0a20d335019e51988
- **Subnet**: subnet-00ba9d45a3e530fd3
- **Encriptación**: KMS habilitada
- **Data API**: ✅ Habilitada
- **Backup**: 7 días retención

#### Tablas Creadas
✅ **users**
- user_id (VARCHAR 255, PK)
- email (VARCHAR 255, UNIQUE NOT NULL)
- username (VARCHAR 100, UNIQUE NOT NULL)
- created_at (TIMESTAMP DEFAULT CURRENT_TIMESTAMP)

✅ **user_settings**
- user_id (VARCHAR 255, PK, FK → users)
- language (VARCHAR 10, DEFAULT 'es')
- timezone (VARCHAR 50, DEFAULT 'UTC')
- currency (VARCHAR 3, DEFAULT 'EUR')

✅ **payment_methods**
- payment_method_id (VARCHAR 255, PK)
- user_id (VARCHAR 255, FK → users)
- stripe_payment_method_id (VARCHAR 255, UNIQUE)
- type (VARCHAR 50)
- last_four (VARCHAR 4)
- brand (VARCHAR 50)

✅ **transactions**
- transaction_id (VARCHAR 255, PK)
- from_user_id (VARCHAR 255, FK → users)
- to_user_id (VARCHAR 255, FK → users)
- type (VARCHAR 50)
- amount (DECIMAL 15,2)
- currency (VARCHAR 3, DEFAULT 'EUR')
- stripe_payment_intent_id (VARCHAR 255, UNIQUE)
- status (VARCHAR 50, DEFAULT 'pending')
- created_at (TIMESTAMP)

✅ **Usuario Sistema**
- user_id: 'system'
- email: 'system@biihlive.com'
- username: 'system'

---

### 3. AWS SECRETS MANAGER
**Estado**: ✅ CONFIGURADO

#### Secret de Base de Datos
- **Name**: `biihlive/db/credentials`
- **ARN**: `arn:aws:secretsmanager:eu-west-3:559050234725:secret:biihlive/db/credentials-tzIE32`
- **Contenido**: {"username":"postgres","password":"BiihliveDB2024!"}

---

### 4. AWS LAMBDA
**Estado**: ⚠️ PARCIALMENTE CONFIGURADO

#### Functions Creadas
- **biihlive-setup-db**
  - Runtime: Python 3.11
  - Handler: index.lambda_handler
  - VPC: Configurada
  - Layer: AWSSDKPandas-Python311
  - Estado: Creada pero sin librería psycopg2
  - IAM Role: biihlive-lambda-vpc-role

---

## ⏳ SERVICIOS PENDIENTES

### DynamoDB (No iniciado)
- [ ] biihlive-posts
- [ ] biihlive-likes
- [ ] biihlive-comments
- [ ] biihlive-follows
- [ ] biihlive-stories
- [ ] biihlive-messages
- [ ] biihlive-conversations
- [ ] biihlive-notifications
- [ ] biihlive-feeds
- [ ] biihlive-user-stats

### S3 Buckets (No iniciado)
- [ ] biihlive-media-uploads
- [ ] biihlive-profile-pictures
- [ ] biihlive-static-assets

### CloudFront (No iniciado)
- [ ] Distribución para media
- [ ] Distribución para assets

### API Gateway (No iniciado)
- [ ] REST API principal
- [ ] WebSocket API para chat

### Lambda Functions Pendientes
- [ ] Cognito PostConfirmation
- [ ] Image processor
- [ ] Video transcoder trigger
- [ ] Feed generator
- [ ] Notification dispatcher

### MediaConvert (No iniciado)
- [ ] Job templates para video

### Amazon IVS (No iniciado)
- [ ] Canales para streaming

### Stripe Integration (No iniciado)
- [ ] Webhooks
- [ ] Payment processing

---

## 🔑 INFORMACIÓN DE ACCESO

### Account ID
```
559050234725
```

### ARNs Importantes
```
# Aurora Cluster
arn:aws:rds:eu-west-3:559050234725:cluster:biihlive-db-cluster

# Secret Manager
arn:aws:secretsmanager:eu-west-3:559050234725:secret:biihlive/db/credentials-tzIE32

# Cognito User Pool
arn:aws:cognito-idp:eu-west-3:559050234725:userpool/eu-west-3_0ztFzMyy5

# Lambda Function
arn:aws:lambda:eu-west-3:559050234725:function:biihlive-setup-db

# IAM Role
arn:aws:iam::559050234725:role/biihlive-lambda-vpc-role
```

---

## 📝 NOTAS IMPORTANTES

### Limitaciones Actuales
1. **Aurora Serverless v2**: No soporta Query Editor directo
2. **Data API**: Habilitada y funcionando con AWS CLI
3. **VPC**: Aurora está en VPC privada (no accesible públicamente)
4. **Lambda**: Necesita layer personalizada para psycopg2

### Decisiones de Arquitectura
1. **Aurora para datos transaccionales**: Usuarios, pagos, configuración
2. **DynamoDB para interacciones sociales**: Posts, likes, mensajes
3. **Cognito para autenticación**: User pools configurados
4. **Data API para acceso**: Evita problemas de VPC

### Credenciales Temporales
- Las credenciales están en Secrets Manager
- TODO: Rotar passwords antes de producción
- TODO: Configurar IAM roles para acceso

---

## 🚀 PRÓXIMOS PASOS CRÍTICOS

1. **Completar esquema Aurora** (9 tablas restantes)
2. **Crear tablas DynamoDB** (10 tablas)
3. **Configurar S3 buckets** para media
4. **Crear API Gateway** con Cognito authorizer
5. **Lambda PostConfirmation** para sync Cognito→Aurora

---

## 💰 COSTOS ACTUALES ESTIMADOS

- **Aurora Serverless v2**: ~$50/mes (0.5 ACU mínimo)
- **Cognito**: Gratis (primeros 50K usuarios)
- **Secrets Manager**: $0.40/mes por secreto
- **Lambda**: Gratis (dentro del free tier)
- **Total actual**: ~$51/mes