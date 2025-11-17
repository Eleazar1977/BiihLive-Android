# Script de Configuración S3 Gallery System
# BIIHLIVE - Sistema de Galería Multimedia

Write-Host "[INFO] Iniciando configuración del bucket S3 para galería..." -ForegroundColor Green

$BUCKET = "biihlivemedia"
$REGION = "eu-west-3"

# 1. Crear estructura de carpetas
Write-Host "[INFO] Creando estructura de carpetas..." -ForegroundColor Yellow

# Crear archivos placeholder para establecer la estructura
$folders = @(
    "gallery/",
    "gallery/temp/",
    "gallery/users/"
)

foreach ($folder in $folders) {
    Write-Host "  Creando: $folder"
    echo "" | aws s3 cp - "s3://$BUCKET/$folder.placeholder" --region $REGION 2>$null
}

# 2. Configurar CORS para permitir uploads desde la app
Write-Host "[INFO] Configurando CORS..." -ForegroundColor Yellow

$corsConfig = @'
{
    "CORSRules": [
        {
            "AllowedHeaders": ["*"],
            "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
            "AllowedOrigins": ["*"],
            "ExposeHeaders": ["ETag", "x-amz-server-side-encryption", "x-amz-request-id"],
            "MaxAgeSeconds": 3000
        }
    ]
}
'@

$corsConfig | Out-File -FilePath "cors-config.json" -Encoding UTF8
aws s3api put-bucket-cors --bucket $BUCKET --cors-configuration file://cors-config.json --region $REGION
Remove-Item "cors-config.json"

# 3. Configurar política del bucket
Write-Host "[INFO] Configurando política del bucket..." -ForegroundColor Yellow

$bucketPolicy = @'
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "PublicReadGetObject",
            "Effect": "Allow",
            "Principal": "*",
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::biihlivemedia/gallery/*",
            "Condition": {
                "StringLike": {
                    "s3:x-amz-server-side-encryption": "AES256"
                }
            }
        },
        {
            "Sid": "AllowCognitoUsersUpload",
            "Effect": "Allow",
            "Principal": {
                "Federated": "cognito-identity.amazonaws.com"
            },
            "Action": [
                "s3:PutObject",
                "s3:PutObjectAcl",
                "s3:GetObject",
                "s3:DeleteObject"
            ],
            "Resource": "arn:aws:s3:::biihlivemedia/gallery/users/*",
            "Condition": {
                "StringEquals": {
                    "cognito-identity.amazonaws.com:aud": "eu-west-3:bce99bf2-9c89-4cd5-a674-b68da1b75a34"
                }
            }
        }
    ]
}
'@

$bucketPolicy | Out-File -FilePath "bucket-policy.json" -Encoding UTF8
aws s3api put-bucket-policy --bucket $BUCKET --policy file://bucket-policy.json --region $REGION
Remove-Item "bucket-policy.json"

# 4. Configurar ciclo de vida para archivos temporales
Write-Host "[INFO] Configurando lifecycle para limpiar archivos temporales..." -ForegroundColor Yellow

$lifecycleConfig = @'
{
    "Rules": [
        {
            "Id": "DeleteTempFiles",
            "Status": "Enabled",
            "Prefix": "gallery/temp/",
            "Expiration": {
                "Days": 1
            }
        },
        {
            "Id": "MoveToGlacierOldPhotos",
            "Status": "Enabled",
            "Prefix": "gallery/users/",
            "Transitions": [
                {
                    "Days": 90,
                    "StorageClass": "INTELLIGENT_TIERING"
                },
                {
                    "Days": 365,
                    "StorageClass": "GLACIER"
                }
            ]
        }
    ]
}
'@

$lifecycleConfig | Out-File -FilePath "lifecycle-config.json" -Encoding UTF8
aws s3api put-bucket-lifecycle-configuration --bucket $BUCKET --lifecycle-configuration file://lifecycle-config.json --region $REGION
Remove-Item "lifecycle-config.json"

# 5. Habilitar versionado
Write-Host "[INFO] Habilitando versionado..." -ForegroundColor Yellow
aws s3api put-bucket-versioning --bucket $BUCKET --versioning-configuration Status=Enabled --region $REGION

# 6. Configurar encriptación por defecto
Write-Host "[INFO] Configurando encriptación por defecto..." -ForegroundColor Yellow

$encryptionConfig = @'
{
    "Rules": [
        {
            "ApplyServerSideEncryptionByDefault": {
                "SSEAlgorithm": "AES256"
            },
            "BucketKeyEnabled": true
        }
    ]
}
'@

$encryptionConfig | Out-File -FilePath "encryption-config.json" -Encoding UTF8
aws s3api put-bucket-encryption --bucket $BUCKET --server-side-encryption-configuration file://encryption-config.json --region $REGION
Remove-Item "encryption-config.json"

# 7. Verificar configuración
Write-Host "`n[INFO] Verificando configuración..." -ForegroundColor Green
Write-Host "  Bucket: $BUCKET"
Write-Host "  Region: $REGION"

aws s3api get-bucket-cors --bucket $BUCKET --region $REGION --output table
aws s3api get-bucket-versioning --bucket $BUCKET --region $REGION --output table
aws s3api get-bucket-encryption --bucket $BUCKET --region $REGION --output table

Write-Host "`n[OK] Configuración S3 completada exitosamente!" -ForegroundColor Green
Write-Host "[INFO] URLs base:" -ForegroundColor Cyan
Write-Host "  S3: https://$BUCKET.s3.$REGION.amazonaws.com/gallery/"
Write-Host "  CloudFront: https://d3[xxx].cloudfront.net/gallery/"

Write-Host "`n[NEXT] Ejecuta setup-cloudfront-gallery.ps1 para configurar CDN" -ForegroundColor Yellow