# Script para configurar Cache-Control headers en objetos S3
# PowerShell script para Windows

# Variables de configuración
$BUCKET = "biihlivemedia"
$REGION = "eu-west-3"
$PREFIX = "userprofile/"

Write-Host "[INFO] Configurando headers Cache-Control en S3..." -ForegroundColor Cyan
Write-Host "[INFO] Bucket: $BUCKET" -ForegroundColor White
Write-Host "[INFO] Prefix: $PREFIX" -ForegroundColor White
Write-Host ""

# Función para actualizar metadata de un objeto
function Update-S3ObjectMetadata {
    param(
        [string]$Key,
        [string]$CacheControl
    )

    Write-Host "  Actualizando: $Key" -ForegroundColor Gray

    # Copiar el objeto sobre sí mismo con nuevos metadata
    $result = aws s3api copy-object `
        --bucket $BUCKET `
        --key "$Key" `
        --copy-source "$BUCKET/$Key" `
        --metadata-directive REPLACE `
        --cache-control "$CacheControl" `
        --content-type "image/jpeg" `
        --region $REGION `
        --output json 2>$null

    if ($LASTEXITCODE -eq 0) {
        Write-Host "    [OK] Cache-Control configurado" -ForegroundColor Green
        return $true
    } else {
        Write-Host "    [ERROR] No se pudo actualizar" -ForegroundColor Red
        return $false
    }
}

# Listar todos los objetos con el prefix
Write-Host "[INFO] Listando objetos en $PREFIX..." -ForegroundColor Yellow
$objects = aws s3api list-objects-v2 `
    --bucket $BUCKET `
    --prefix $PREFIX `
    --region $REGION `
    --output json | ConvertFrom-Json

if (-not $objects.Contents) {
    Write-Host "[ADVERTENCIA] No se encontraron objetos con el prefix $PREFIX" -ForegroundColor Yellow
    exit 0
}

$totalObjects = $objects.Contents.Count
Write-Host "[INFO] Encontrados $totalObjects objetos" -ForegroundColor Green
Write-Host ""

# Configurar Cache-Control basado en el tipo de archivo
$updatedCount = 0
$failedCount = 0

foreach ($object in $objects.Contents) {
    $key = $object.Key

    # Determinar Cache-Control basado en el tipo de archivo
    if ($key -match "thumbnail\.jpg$") {
        # Thumbnails: cache más largo (1 día)
        $cacheControl = "public, max-age=86400, s-maxage=86400"
        Write-Host "[THUMBNAIL] $key" -ForegroundColor Cyan
    }
    elseif ($key -match "full\.jpg$") {
        # Full images: cache más corto (1 hora)
        $cacheControl = "public, max-age=3600, s-maxage=3600"
        Write-Host "[FULL] $key" -ForegroundColor Yellow
    }
    else {
        # Otros archivos: cache moderado (4 horas)
        $cacheControl = "public, max-age=14400, s-maxage=14400"
        Write-Host "[OTHER] $key" -ForegroundColor Gray
    }

    if (Update-S3ObjectMetadata -Key $key -CacheControl $cacheControl) {
        $updatedCount++
    } else {
        $failedCount++
    }
}

Write-Host ""
Write-Host "=== RESUMEN ===" -ForegroundColor Cyan
Write-Host "Total de objetos: $totalObjects" -ForegroundColor White
Write-Host "Actualizados: $updatedCount" -ForegroundColor Green
Write-Host "Fallidos: $failedCount" -ForegroundColor Red
Write-Host ""
Write-Host "Cache-Control configurado:" -ForegroundColor Green
Write-Host "  - Thumbnails: 24 horas (86400 segundos)" -ForegroundColor White
Write-Host "  - Full images: 1 hora (3600 segundos)" -ForegroundColor White
Write-Host "  - Otros: 4 horas (14400 segundos)" -ForegroundColor White
Write-Host ""

# Verificar configuración de un objeto de ejemplo
if ($objects.Contents.Count -gt 0) {
    $sampleKey = $objects.Contents[0].Key
    Write-Host "[INFO] Verificando configuración de ejemplo..." -ForegroundColor Yellow
    $metadata = aws s3api head-object `
        --bucket $BUCKET `
        --key "$sampleKey" `
        --region $REGION `
        --output json | ConvertFrom-Json

    if ($metadata.CacheControl) {
        Write-Host "[OK] Cache-Control: $($metadata.CacheControl)" -ForegroundColor Green
    } else {
        Write-Host "[ADVERTENCIA] Cache-Control no configurado" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "[TIP] Los cambios se aplicarán gradualmente según expire el cache actual" -ForegroundColor Yellow
Write-Host "[TIP] Para aplicar cambios inmediatamente, ejecuta:" -ForegroundColor Yellow
Write-Host "  .\invalidate-cloudfront-cache.ps1" -ForegroundColor White