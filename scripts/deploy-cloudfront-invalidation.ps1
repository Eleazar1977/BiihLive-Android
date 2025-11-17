# Script para desplegar el sistema de invalidacion de CloudFront para Biihlive
# Este script crea la infraestructura necesaria en AWS
# Fecha: 2025-09-30

[CmdletBinding()]
param()

Write-Host "[INICIANDO] Deploy del sistema de invalidacion de CloudFront" -ForegroundColor Cyan
Write-Host ""

# Configuracion
$REGION = "eu-west-3"
$DISTRIBUTION_ID = "E1U2UV08CTO53W"
$FUNCTION_NAME = "BiihliveCloudFrontInvalidation"
$API_NAME = "BiihliveCloudFrontInvalidation"
$ROLE_NAME = "CloudFrontInvalidationLambdaRole"
$POLICY_NAME = "CloudFrontInvalidationPolicy"

# Verificar AWS CLI
Write-Host "[1/8] Verificando AWS CLI..." -ForegroundColor Yellow
try {
    $awsVersion = aws --version 2>&1 | Out-String
    Write-Host "  [OK] AWS CLI instalado: $awsVersion" -ForegroundColor Green
} catch {
    Write-Host "  [ERROR] AWS CLI no encontrado. Por favor instalalo primero." -ForegroundColor Red
    exit 1
}

# Obtener Account ID
Write-Host "[2/8] Obteniendo Account ID..." -ForegroundColor Yellow
$ACCOUNT_ID = aws sts get-caller-identity --query Account --output text --region $REGION
Write-Host "  [OK] Account ID: $ACCOUNT_ID" -ForegroundColor Green

# Crear rol IAM
Write-Host "[3/8] Creando rol IAM..." -ForegroundColor Yellow
$trustPolicy = @'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
'@

$trustPolicy | Out-File -FilePath trust-policy.json -Encoding UTF8

$roleExists = aws iam get-role --role-name $ROLE_NAME --region $REGION 2>&1 | Out-String
if ($LASTEXITCODE -eq 0) {
    Write-Host "  [INFO] Rol ya existe, saltando creacion" -ForegroundColor Yellow
} else {
    aws iam create-role `
        --role-name $ROLE_NAME `
        --assume-role-policy-document file://trust-policy.json `
        --region $REGION | Out-Null

    aws iam attach-role-policy `
        --role-name $ROLE_NAME `
        --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole | Out-Null

    Write-Host "  [OK] Rol creado" -ForegroundColor Green
}

# Crear politica personalizada
Write-Host "[4/8] Creando politica de CloudFront..." -ForegroundColor Yellow
$cfPolicy = @'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "cloudfront:CreateInvalidation",
        "cloudfront:GetInvalidation",
        "cloudfront:ListInvalidations"
      ],
      "Resource": "*"
    }
  ]
}
'@

$cfPolicy | Out-File -FilePath cloudfront-policy.json -Encoding UTF8

$policyArn = "arn:aws:iam::${ACCOUNT_ID}:policy/${POLICY_NAME}"
$policyExists = aws iam get-policy --policy-arn $policyArn --region $REGION 2>&1 | Out-String
if ($LASTEXITCODE -eq 0) {
    Write-Host "  [INFO] Politica ya existe, saltando creacion" -ForegroundColor Yellow
} else {
    aws iam create-policy `
        --policy-name $POLICY_NAME `
        --policy-document file://cloudfront-policy.json | Out-Null

    Write-Host "  [OK] Politica creada" -ForegroundColor Green
}

aws iam attach-role-policy `
    --role-name $ROLE_NAME `
    --policy-arn $policyArn 2>&1 | Out-Null

# Crear codigo Lambda
Write-Host "[5/8] Preparando codigo Lambda..." -ForegroundColor Yellow
$lambdaCode = @'
import json
import boto3
import uuid
from datetime import datetime

def lambda_handler(event, context):
    try:
        # Obtener userId del evento
        if 'body' in event and event['body']:
            body = json.loads(event['body'])
            user_id = body.get('userId')
        else:
            user_id = event.get('userId')

        if not user_id:
            return {
                'statusCode': 400,
                'headers': {
                    'Content-Type': 'application/json',
                    'Access-Control-Allow-Origin': '*',
                    'Access-Control-Allow-Headers': 'Content-Type',
                    'Access-Control-Allow-Methods': 'POST, GET, OPTIONS'
                },
                'body': json.dumps({
                    'error': 'userId is required'
                })
            }

        cloudfront = boto3.client('cloudfront')
        distribution_id = 'E1U2UV08CTO53W'
        invalidation_paths = [f'/userprofile/{user_id}/*']

        response = cloudfront.create_invalidation(
            DistributionId=distribution_id,
            InvalidationBatch={
                'Paths': {
                    'Quantity': len(invalidation_paths),
                    'Items': invalidation_paths
                },
                'CallerReference': f'biihlive-{user_id}-{int(datetime.now().timestamp())}'
            }
        )

        invalidation_id = response['Invalidation']['Id']

        return {
            'statusCode': 200,
            'headers': {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*',
                'Access-Control-Allow-Headers': 'Content-Type',
                'Access-Control-Allow-Methods': 'POST, GET, OPTIONS'
            },
            'body': json.dumps({
                'message': 'Invalidation created successfully',
                'invalidationId': invalidation_id,
                'userId': user_id,
                'paths': invalidation_paths,
                'distributionId': distribution_id,
                'timestamp': datetime.now().isoformat()
            })
        }

    except Exception as e:
        print(f"Error: {str(e)}")
        return {
            'statusCode': 500,
            'headers': {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*',
                'Access-Control-Allow-Headers': 'Content-Type',
                'Access-Control-Allow-Methods': 'POST, GET, OPTIONS'
            },
            'body': json.dumps({
                'error': 'Internal server error',
                'details': str(e)
            })
        }
'@

$lambdaCode | Out-File -FilePath lambda_function.py -Encoding UTF8

# Crear ZIP
if (Test-Path lambda-deployment.zip) {
    Remove-Item lambda-deployment.zip -Force
}

Add-Type -Assembly "System.IO.Compression.FileSystem"
$compression = [System.IO.Compression.CompressionLevel]::Optimal
$includeBaseDirectory = $false
$zipPath = Join-Path $PWD "lambda-deployment.zip"
$tempDir = New-Item -ItemType Directory -Force -Path ".\temp_lambda"
Copy-Item lambda_function.py $tempDir
[System.IO.Compression.ZipFile]::CreateFromDirectory($tempDir, $zipPath, $compression, $includeBaseDirectory)
Remove-Item -Recurse -Force $tempDir

Write-Host "  [OK] Codigo Lambda preparado" -ForegroundColor Green

# Esperar un momento para que el rol este listo
Start-Sleep -Seconds 5

# Crear o actualizar funcion Lambda
Write-Host "[6/8] Desplegando funcion Lambda..." -ForegroundColor Yellow
$functionExists = aws lambda get-function --function-name $FUNCTION_NAME --region $REGION 2>&1 | Out-String
if ($LASTEXITCODE -eq 0) {
    Write-Host "  [INFO] Funcion existe, actualizando codigo..." -ForegroundColor Yellow
    aws lambda update-function-code `
        --function-name $FUNCTION_NAME `
        --zip-file fileb://lambda-deployment.zip `
        --region $REGION | Out-Null
    Write-Host "  [OK] Funcion actualizada" -ForegroundColor Green
} else {
    aws lambda create-function `
        --function-name $FUNCTION_NAME `
        --runtime python3.9 `
        --role "arn:aws:iam::${ACCOUNT_ID}:role/${ROLE_NAME}" `
        --handler lambda_function.lambda_handler `
        --zip-file fileb://lambda-deployment.zip `
        --description "Invalidate CloudFront cache for Biihlive user profiles" `
        --timeout 30 `
        --region $REGION | Out-Null
    Write-Host "  [OK] Funcion Lambda creada" -ForegroundColor Green
}

# Crear API Gateway
Write-Host "[7/8] Configurando API Gateway..." -ForegroundColor Yellow

# Buscar si ya existe
$existingApi = aws apigateway get-rest-apis --region $REGION --query "items[?name=='$API_NAME'].id" --output text
if ($existingApi) {
    $API_ID = $existingApi
    Write-Host "  [INFO] API ya existe con ID: $API_ID" -ForegroundColor Yellow
} else {
    $API_ID = aws apigateway create-rest-api `
        --name $API_NAME `
        --description "API for CloudFront cache invalidation" `
        --region $REGION `
        --query 'id' `
        --output text
    Write-Host "  [OK] API creada con ID: $API_ID" -ForegroundColor Green
}

# Obtener resource root
$ROOT_RESOURCE_ID = aws apigateway get-resources `
    --rest-api-id $API_ID `
    --region $REGION `
    --query 'items[0].id' `
    --output text

# Buscar o crear recurso /invalidate
$existingResource = aws apigateway get-resources `
    --rest-api-id $API_ID `
    --region $REGION `
    --query "items[?pathPart=='invalidate'].id" `
    --output text

if ($existingResource) {
    $RESOURCE_ID = $existingResource
    Write-Host "  [INFO] Recurso /invalidate ya existe" -ForegroundColor Yellow
} else {
    $RESOURCE_ID = aws apigateway create-resource `
        --rest-api-id $API_ID `
        --parent-id $ROOT_RESOURCE_ID `
        --path-part "invalidate" `
        --region $REGION `
        --query 'id' `
        --output text
    Write-Host "  [OK] Recurso /invalidate creado" -ForegroundColor Green
}

# Crear metodos
aws apigateway put-method `
    --rest-api-id $API_ID `
    --resource-id $RESOURCE_ID `
    --http-method POST `
    --authorization-type NONE `
    --region $REGION 2>&1 | Out-Null

aws apigateway put-method `
    --rest-api-id $API_ID `
    --resource-id $RESOURCE_ID `
    --http-method OPTIONS `
    --authorization-type NONE `
    --region $REGION 2>&1 | Out-Null

# Configurar integraciones
$LAMBDA_ARN = "arn:aws:lambda:${REGION}:${ACCOUNT_ID}:function:${FUNCTION_NAME}"

aws apigateway put-integration `
    --rest-api-id $API_ID `
    --resource-id $RESOURCE_ID `
    --http-method POST `
    --type AWS_PROXY `
    --integration-http-method POST `
    --uri "arn:aws:apigateway:${REGION}:lambda:path/2015-03-31/functions/${LAMBDA_ARN}/invocations" `
    --region $REGION 2>&1 | Out-Null

# CORS para OPTIONS
$requestTemplates = '{"application/json": "{\"statusCode\": 200}"}'
aws apigateway put-integration `
    --rest-api-id $API_ID `
    --resource-id $RESOURCE_ID `
    --http-method OPTIONS `
    --type MOCK `
    --request-templates $requestTemplates `
    --region $REGION 2>&1 | Out-Null

aws apigateway put-method-response `
    --rest-api-id $API_ID `
    --resource-id $RESOURCE_ID `
    --http-method OPTIONS `
    --status-code 200 `
    --response-parameters "method.response.header.Access-Control-Allow-Headers=false,method.response.header.Access-Control-Allow-Methods=false,method.response.header.Access-Control-Allow-Origin=false" `
    --region $REGION 2>&1 | Out-Null

$responseParams = '{"method.response.header.Access-Control-Allow-Headers":"''Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token''","method.response.header.Access-Control-Allow-Methods":"''GET,POST,OPTIONS''","method.response.header.Access-Control-Allow-Origin":"''*''"}'
aws apigateway put-integration-response `
    --rest-api-id $API_ID `
    --resource-id $RESOURCE_ID `
    --http-method OPTIONS `
    --status-code 200 `
    --response-parameters $responseParams `
    --region $REGION 2>&1 | Out-Null

# Permisos Lambda
aws lambda add-permission `
    --function-name $FUNCTION_NAME `
    --statement-id "apigateway-invoke-$(Get-Date -Format 'yyyyMMddHHmmss')" `
    --action lambda:InvokeFunction `
    --principal apigateway.amazonaws.com `
    --source-arn "arn:aws:execute-api:${REGION}:${ACCOUNT_ID}:${API_ID}/*/POST/invalidate" `
    --region $REGION 2>&1 | Out-Null

Write-Host "  [OK] API Gateway configurado" -ForegroundColor Green

# Deploy
Write-Host "[8/8] Desplegando API..." -ForegroundColor Yellow
aws apigateway create-deployment `
    --rest-api-id $API_ID `
    --stage-name prod `
    --stage-description "Production stage" `
    --description "Production deployment" `
    --region $REGION | Out-Null

Write-Host "  [OK] API desplegada" -ForegroundColor Green

# Limpiar archivos temporales
Remove-Item -Force trust-policy.json -ErrorAction SilentlyContinue
Remove-Item -Force cloudfront-policy.json -ErrorAction SilentlyContinue
Remove-Item -Force lambda_function.py -ErrorAction SilentlyContinue
Remove-Item -Force lambda-deployment.zip -ErrorAction SilentlyContinue

# Resultado final
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "[COMPLETADO] Infraestructura desplegada exitosamente" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "API Gateway ID: $API_ID" -ForegroundColor Yellow
Write-Host "Endpoint: https://${API_ID}.execute-api.${REGION}.amazonaws.com/prod/invalidate" -ForegroundColor Yellow
Write-Host ""
Write-Host "[IMPORTANTE] Actualiza el archivo AWSConfig.kt con:" -ForegroundColor Cyan
Write-Host "  const val API_GATEWAY_ID = `"$API_ID`"" -ForegroundColor White
Write-Host ""
Write-Host "Para probar la invalidacion:" -ForegroundColor Cyan
Write-Host "  curl -X POST https://${API_ID}.execute-api.${REGION}.amazonaws.com/prod/invalidate -H `"Content-Type: application/json`" -d '{`"userId`": `"test-user`"}'" -ForegroundColor White
Write-Host ""