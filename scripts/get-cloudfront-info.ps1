# Script para obtener informacion de CloudFront
# PowerShell script para Windows

$REGION = "eu-west-3"
$S3_BUCKET = "biihlivemedia"

Write-Host "=== INFORMACION DE CLOUDFRONT ===" -ForegroundColor Cyan
Write-Host ""

# Listar todas las distribuciones
Write-Host "[INFO] Buscando distribuciones de CloudFront..." -ForegroundColor Yellow
$distributions = aws cloudfront list-distributions --region $REGION --output json | ConvertFrom-Json

if (-not $distributions.DistributionList.Items) {
    Write-Host "[ADVERTENCIA] No se encontraron distribuciones de CloudFront" -ForegroundColor Yellow
    exit 0
}

Write-Host "[OK] Encontradas $($distributions.DistributionList.Items.Count) distribuciones" -ForegroundColor Green
Write-Host ""

# Buscar la distribucion que usa nuestro bucket S3
$targetDistribution = $null

foreach ($dist in $distributions.DistributionList.Items) {
    Write-Host "Distribucion: $($dist.Id)" -ForegroundColor White
    Write-Host "  Dominio: $($dist.DomainName)" -ForegroundColor Gray
    Write-Host "  Estado: $($dist.Status)" -ForegroundColor Gray
    Write-Host "  Comentario: $($dist.Comment)" -ForegroundColor Gray

    # Verificar si esta distribucion usa nuestro bucket
    foreach ($origin in $dist.Origins.Items) {
        if ($origin.DomainName -match $S3_BUCKET) {
            Write-Host "  [OK] Usa el bucket: $S3_BUCKET" -ForegroundColor Green
            $targetDistribution = $dist
            break
        }
    }

    Write-Host ""
}

if ($targetDistribution) {
    Write-Host "=== DISTRIBUCION OBJETIVO ENCONTRADA ===" -ForegroundColor Cyan
    Write-Host "Distribution ID: $($targetDistribution.Id)" -ForegroundColor Green
    Write-Host "Domain Name: $($targetDistribution.DomainName)" -ForegroundColor Green
    Write-Host "Alias CNAME: $($targetDistribution.Aliases.Items -join ', ')" -ForegroundColor Green
    Write-Host ""

    # Obtener configuracion detallada
    Write-Host "[INFO] Obteniendo configuracion detallada..." -ForegroundColor Yellow
    $config = aws cloudfront get-distribution --id $targetDistribution.Id --region $REGION --output json | ConvertFrom-Json

    if ($config) {
        Write-Host ""
        Write-Host "=== CONFIGURACION DE CACHE ===" -ForegroundColor Cyan

        # Configuracion por defecto
        $defaultBehavior = $config.Distribution.DistributionConfig.DefaultCacheBehavior
        Write-Host "Comportamiento por defecto:" -ForegroundColor White
        Write-Host "  Min TTL: $($defaultBehavior.MinTTL) segundos" -ForegroundColor Gray
        Write-Host "  Default TTL: $($defaultBehavior.DefaultTTL) segundos" -ForegroundColor Gray
        Write-Host "  Max TTL: $($defaultBehavior.MaxTTL) segundos" -ForegroundColor Gray

        # Cache behaviors personalizados
        if ($config.Distribution.DistributionConfig.CacheBehaviors.Items) {
            Write-Host ""
            Write-Host "Comportamientos personalizados:" -ForegroundColor White
            foreach ($behavior in $config.Distribution.DistributionConfig.CacheBehaviors.Items) {
                Write-Host "  Path: $($behavior.PathPattern)" -ForegroundColor Yellow
                Write-Host "    Min TTL: $($behavior.MinTTL) segundos" -ForegroundColor Gray
                Write-Host "    Default TTL: $($behavior.DefaultTTL) segundos" -ForegroundColor Gray
                Write-Host "    Max TTL: $($behavior.MaxTTL) segundos" -ForegroundColor Gray
            }
        }
    }

    # Guardar la informacion en un archivo
    $infoFile = "cloudfront-info.txt"
    @"
CLOUDFRONT CONFIGURATION FOR BIIHLIVE
=====================================

Distribution ID: $($targetDistribution.Id)
Domain Name: $($targetDistribution.DomainName)
Status: $($targetDistribution.Status)

IMPORTANT: Update these values in your scripts:
  configure-cloudfront-cache.ps1
  invalidate-cloudfront-cache.ps1

CloudFront URL for code:
https://$($targetDistribution.DomainName)
"@ | Out-File -FilePath $infoFile -Encoding UTF8

    Write-Host ""
    Write-Host "[OK] Informacion guardada en: $infoFile" -ForegroundColor Green

    # Generar comandos utiles
    Write-Host ""
    Write-Host "=== COMANDOS UTILES ===" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Para configurar el TTL:" -ForegroundColor Yellow
    Write-Host "  Update the DISTRIBUTION_ID in configure-cloudfront-cache.ps1 to: $($targetDistribution.Id)" -ForegroundColor White
    Write-Host ""
    Write-Host "Para invalidar el cache:" -ForegroundColor Yellow
    Write-Host "  aws cloudfront create-invalidation --distribution-id $($targetDistribution.Id) --paths '/userprofile/*'" -ForegroundColor White
    Write-Host ""
    Write-Host "CloudFront URL para usar en la app:" -ForegroundColor Yellow
    Write-Host "  https://$($targetDistribution.DomainName)" -ForegroundColor Green

} else {
    Write-Host "[ERROR] No se encontro una distribucion que use el bucket: $S3_BUCKET" -ForegroundColor Red
}