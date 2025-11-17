# Script para invalidar el cache de CloudFront
# PowerShell script para Windows

param(
    [string]$UserId = "",
    [switch]$All = $false
)

# Variables de configuración
$DISTRIBUTION_ID = "E1HZ8WQ7IXAQXD"  # Distribution ID real de Biihlive
$REGION = "eu-west-3"

Write-Host "=== INVALIDACIÓN DE CACHE CLOUDFRONT ===" -ForegroundColor Cyan
Write-Host ""

# Determinar las rutas a invalidar
$paths = @()

if ($All) {
    # Invalidar todo el contenido de userprofile
    $paths += "/userprofile/*"
    Write-Host "[INFO] Invalidando TODO el cache de perfiles de usuario" -ForegroundColor Yellow
} elseif ($UserId) {
    # Invalidar solo un usuario específico
    $paths += "/userprofile/$UserId/*"
    Write-Host "[INFO] Invalidando cache del usuario: $UserId" -ForegroundColor Yellow
} else {
    Write-Host "Uso:" -ForegroundColor White
    Write-Host "  .\invalidate-cloudfront-cache.ps1 -UserId 'user-id-here'" -ForegroundColor Gray
    Write-Host "  .\invalidate-cloudfront-cache.ps1 -All" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Ejemplos:" -ForegroundColor White
    Write-Host "  .\invalidate-cloudfront-cache.ps1 -UserId '91b950fe-a0a1-7089-29fc-bd301495950b'" -ForegroundColor Gray
    Write-Host "  .\invalidate-cloudfront-cache.ps1 -All" -ForegroundColor Gray
    exit 0
}

# Crear la invalidación
Write-Host "[INFO] Creando invalidación en CloudFront..." -ForegroundColor Yellow
Write-Host "  Distribution ID: $DISTRIBUTION_ID" -ForegroundColor Gray
Write-Host "  Paths: $($paths -join ', ')" -ForegroundColor Gray

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$callerReference = "invalidation-$timestamp"

# Crear el JSON para la invalidación
$invalidationBatch = @{
    Paths = @{
        Quantity = $paths.Count
        Items = $paths
    }
    CallerReference = $callerReference
} | ConvertTo-Json -Depth 10

# Guardar en archivo temporal
$tempFile = "invalidation-temp.json"
$invalidationBatch | Out-File -FilePath $tempFile -Encoding UTF8

# Crear la invalidación
$result = aws cloudfront create-invalidation `
    --distribution-id $DISTRIBUTION_ID `
    --invalidation-batch file://$tempFile `
    --region $REGION `
    --output json | ConvertFrom-Json

# Limpiar archivo temporal
Remove-Item $tempFile -ErrorAction SilentlyContinue

if ($result) {
    $invalidationId = $result.Invalidation.Id
    $status = $result.Invalidation.Status

    Write-Host ""
    Write-Host "[OK] Invalidación creada exitosamente" -ForegroundColor Green
    Write-Host "  ID: $invalidationId" -ForegroundColor White
    Write-Host "  Estado: $status" -ForegroundColor White
    Write-Host "  Referencia: $callerReference" -ForegroundColor White
    Write-Host ""

    # Verificar el costo
    Write-Host "[IMPORTANTE] Costos de invalidación:" -ForegroundColor Yellow
    Write-Host "  - Las primeras 1,000 invalidaciones/mes son GRATIS" -ForegroundColor White
    Write-Host "  - Después: $0.005 USD por path invalidado" -ForegroundColor White
    Write-Host ""

    # Monitorear el progreso
    Write-Host "[INFO] Monitoreando progreso..." -ForegroundColor Cyan
    $maxAttempts = 20
    $attempt = 0

    while ($attempt -lt $maxAttempts) {
        Start-Sleep -Seconds 10

        $statusCheck = aws cloudfront get-invalidation `
            --distribution-id $DISTRIBUTION_ID `
            --id $invalidationId `
            --region $REGION `
            --output json | ConvertFrom-Json

        $currentStatus = $statusCheck.Invalidation.Status

        if ($currentStatus -eq "Completed") {
            Write-Host "[OK] Invalidación completada!" -ForegroundColor Green
            break
        } else {
            $attempt++
            Write-Host "  Estado: $currentStatus (intento $attempt de $maxAttempts)" -ForegroundColor Gray
        }
    }

    if ($currentStatus -ne "Completed") {
        Write-Host "[INFO] La invalidación sigue en progreso" -ForegroundColor Yellow
        Write-Host "[TIP] Puedes verificar el estado con:" -ForegroundColor Yellow
        Write-Host "  aws cloudfront get-invalidation --distribution-id $DISTRIBUTION_ID --id $invalidationId" -ForegroundColor White
    }

} else {
    Write-Host "[ERROR] No se pudo crear la invalidación" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host '=== PROCESO COMPLETADO ===' -ForegroundColor Cyan
Write-Host '[INFO] El cache ha sido invalidado' -ForegroundColor Green
Write-Host '[INFO] Images will be loaded from S3' -ForegroundColor Green
Write-Host ""