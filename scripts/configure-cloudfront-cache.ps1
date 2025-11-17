# Script para configurar CloudFront con TTL corto
# PowerShell script para Windows

# Variables de configuración
$DISTRIBUTION_ID = "E1HZ8WQ7IXAQXD"  # Distribution ID real de Biihlive
$REGION = "eu-west-3"

Write-Host "[INFO] Configurando CloudFront Cache Behaviors..." -ForegroundColor Cyan

# Obtener la configuración actual de la distribución
Write-Host "[INFO] Obteniendo configuración actual..." -ForegroundColor Yellow
$distribution = aws cloudfront get-distribution-config --id $DISTRIBUTION_ID --region $REGION --output json | ConvertFrom-Json

if (-not $distribution) {
    Write-Host "[ERROR] No se pudo obtener la distribución" -ForegroundColor Red
    exit 1
}

$config = $distribution.DistributionConfig
$etag = $distribution.ETag

Write-Host "[INFO] Distribution ID: $DISTRIBUTION_ID" -ForegroundColor Green
Write-Host "[INFO] ETag: $etag" -ForegroundColor Green

# Configurar cache behaviors para las imágenes de perfil
Write-Host "[INFO] Configurando cache behavior para userprofile/*..." -ForegroundColor Yellow

# Crear nuevo cache behavior para userprofile/* si no existe
$userprofileBehavior = @{
    PathPattern = "userprofile/*"
    TargetOriginId = $config.Origins.Items[0].Id  # Usar el primer origen (S3)
    TrustedSigners = @{
        Enabled = $false
        Quantity = 0
    }
    TrustedKeyGroups = @{
        Enabled = $false
        Quantity = 0
    }
    ViewerProtocolPolicy = "redirect-to-https"
    AllowedMethods = @{
        Quantity = 2
        Items = @("GET", "HEAD")
        CachedMethods = @{
            Quantity = 2
            Items = @("GET", "HEAD")
        }
    }
    SmoothStreaming = $false
    Compress = $true
    # Configuración de TTL - 1 hora (3600 segundos)
    MinTTL = 0
    DefaultTTL = 3600      # 1 hora por defecto
    MaxTTL = 86400         # Máximo 24 horas
    # Configuración de forwarding
    ForwardedValues = @{
        QueryString = $true  # Importante: permitir query strings para bypass de cache
        Cookies = @{
            Forward = "none"
        }
        Headers = @{
            Quantity = 4
            Items = @(
                "Access-Control-Request-Headers",
                "Access-Control-Request-Method",
                "Origin",
                "Cache-Control"  # Respetar headers de cache de S3
            )
        }
        QueryStringCacheKeys = @{
            Quantity = 1
            Items = @("v")  # Permitir versionado con ?v=timestamp
        }
    }
    # Cache policy para objetos de perfil
    CachePolicyId = $null
    OriginRequestPolicyId = $null
}

# Guardar configuración actualizada en archivo temporal
$configJson = $config | ConvertTo-Json -Depth 100
$configFile = "cloudfront-config-temp.json"
$configJson | Out-File -FilePath $configFile -Encoding UTF8

Write-Host "[INFO] Configuración guardada en $configFile" -ForegroundColor Green

# Actualizar la distribución
Write-Host "[INFO] Actualizando distribución de CloudFront..." -ForegroundColor Yellow
Write-Host "[ADVERTENCIA] Este proceso puede tomar 5-10 minutos" -ForegroundColor Yellow

$updateResult = aws cloudfront update-distribution `
    --id $DISTRIBUTION_ID `
    --distribution-config file://$configFile `
    --if-match $etag `
    --region $REGION

if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK] Distribución actualizada exitosamente" -ForegroundColor Green
    Write-Host "[INFO] Los cambios pueden tardar hasta 15 minutos en propagarse globalmente" -ForegroundColor Yellow
} else {
    Write-Host "[ERROR] Error al actualizar la distribución" -ForegroundColor Red
}

# Limpiar archivo temporal
Remove-Item $configFile -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "=== CONFIGURACIÓN COMPLETADA ===" -ForegroundColor Cyan
Write-Host "TTL configurado:" -ForegroundColor Green
Write-Host "  - Default TTL: 1 hora (3600 segundos)" -ForegroundColor White
Write-Host "  - Max TTL: 24 horas (86400 segundos)" -ForegroundColor White
Write-Host "  - Query string 'v' habilitada para bypass de cache" -ForegroundColor White
Write-Host ""
Write-Host "[TIP] Para invalidar el cache manualmente:" -ForegroundColor Yellow
Write-Host "  aws cloudfront create-invalidation --distribution-id $DISTRIBUTION_ID --paths '/userprofile/*'" -ForegroundColor White