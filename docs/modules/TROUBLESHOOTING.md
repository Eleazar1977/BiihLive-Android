# 🔧 Troubleshooting

## Problemas Comunes y Soluciones

### 🔴 Identity Pool Error
```
Error: IdentityPool 'eu-west-3:xxx' not found
```
**Solución:**
```bash
# Verificar Identity Pool
aws cognito-identity describe-identity-pool \
  --identity-pool-id eu-west-3:bce99bf2-9c89-4cd5-a674-b68da1b75a34 \
  --region eu-west-3

# Si no existe, crear con script
powershell .\scripts\setup-identity-pool.ps1
```

### 🔴 AppSync Field Undefined
```
Field 'campoNuevo' in type 'Query' is undefined
```
**Solución:**
1. Actualizar schema GraphQL
2. Agregar resolver si es necesario
3. Ver: [`AWS_BACKEND.md`](AWS_BACKEND.md#actualizar-schema)

### 🔴 CloudFront Cache Issues
```
Imagen de perfil no se actualiza
```
**Solución:**
```bash
# Invalidar caché
aws cloudfront create-invalidation \
  --distribution-id EXXXXXXXXX \
  --paths "/profile-photos/*"

# O usar timestamps en URLs
?t=1234567890
```

### 🔴 DynamoDB Throttling
```
ProvisionedThroughputExceededException
```
**Solución:**
- Cambiar a On-Demand billing
- O aumentar RCU/WCU
- Implementar exponential backoff

### 🔴 Kotlin Type Mismatch
```
Argument type mismatch: actual type is 'Application', but 'Application' was expected
```
**Solución:**
```kotlin
// Mover dependencia de commonMain a androidMain
// en build.gradle.kts
androidMain.dependencies {
    implementation(libs.androidx.lifecycle.viewmodel)
}
```

### 🔴 S3 Access Denied
```
Access Denied uploading to S3
```
**Solución:**
1. Verificar Cognito Identity Pool
2. Verificar IAM roles
3. Verificar bucket policy:
```json
{
    "Version": "2012-10-17",
    "Statement": [{
        "Sid": "AllowCognitoUsers",
        "Effect": "Allow",
        "Principal": {
            "Federated": "cognito-identity.amazonaws.com"
        },
        "Action": ["s3:GetObject", "s3:PutObject"],
        "Resource": "arn:aws:s3:::biihlivemedia/*"
    }]
}
```

### 🔴 GraphQL Null Values
```
JSONException: Value null at field cannot be converted
```
**Solución:**
```kotlin
// Usar optXXX en lugar de getXXX
val campo = jsonObject.optString("campo", "default")
val numero = jsonObject.optInt("numero", 0)
val booleano = jsonObject.optBoolean("activo", false)
```

### 🔴 Compose Preview Not Working
```
Preview not showing
```
**Solución:**
```kotlin
@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ComponentPreview() {
    BiihliveTheme {
        // Component with mock data
    }
}
```

### 🔴 Build Errors

#### Clean Build
```bash
./gradlew clean
./gradlew assembleDebug
```

#### Invalid Cache
```bash
# Windows
rmdir /s /q .gradle
rmdir /s /q build
rmdir /s /q composeApp\build

# Rebuild
./gradlew assembleDebug
```

### 🔴 AWS CLI Issues

#### Configure Profile
```bash
aws configure --profile biihlive
AWS Access Key ID: xxx
AWS Secret Access Key: xxx
Default region: eu-west-3
```

#### Use Profile
```bash
aws s3 ls --profile biihlive
export AWS_PROFILE=biihlive
```

## Debug Commands

### Ver Logs Android
```bash
# All logs
adb logcat

# Filtered
adb logcat | grep -E "BIIHLIVE|AppSync|Cognito"

# Clear and start fresh
adb logcat -c && adb logcat | grep BIIHLIVE
```

### Test API Calls
```bash
# GraphQL query
curl -X POST https://xxx.appsync-api.eu-west-3.amazonaws.com/graphql \
  -H "x-api-key: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"query":"query { listPerfilUsuarios { items { userId nickname } } }"}'
```

### Inspect DynamoDB
```bash
# Get item
aws dynamodb get-item \
  --table-name BIILIVEDB-USERS \
  --key '{"userId":{"S":"test-user"}}' \
  --region eu-west-3

# Query
aws dynamodb query \
  --table-name BIIHLIVE-SOCIAL-V2 \
  --key-condition-expression "PK = :pk" \
  --expression-attribute-values '{":pk":{"S":"user123"}}' \
  --region eu-west-3
```

## Performance Issues

### App Lenta
1. Habilitar R8/ProGuard
2. Reducir tamaño imágenes
3. Lazy loading en listas
4. Verificar memory leaks

### Imágenes No Cargan
1. Verificar CloudFront status
2. Verificar S3 permisos
3. Limpiar caché Coil:
```kotlin
imageLoader.memoryCache?.clear()
imageLoader.diskCache?.clear()
```

## Contacto Soporte
- AWS Support: Console → Support Center
- Android Issues: GitHub Issues del proyecto
- Logs importantes: Siempre incluir stacktrace completo