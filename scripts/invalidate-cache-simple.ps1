# Simple CloudFront Cache Invalidation Script
param(
    [string]$UserId = "",
    [switch]$All = $false
)

$DISTRIBUTION_ID = "E1HZ8WQ7IXAQXD"
$REGION = "eu-west-3"

Write-Host "=== CLOUDFRONT CACHE INVALIDATION ===" -ForegroundColor Cyan
Write-Host ""

# Determine paths to invalidate
$paths = @()

if ($All) {
    $paths += "/userprofile/*"
    Write-Host "[INFO] Invalidating ALL user profile cache" -ForegroundColor Yellow
} elseif ($UserId) {
    $paths += "/userprofile/$UserId/*"
    Write-Host "[INFO] Invalidating cache for user: $UserId" -ForegroundColor Yellow
} else {
    Write-Host "Usage:" -ForegroundColor White
    Write-Host "  .\invalidate-cache-simple.ps1 -UserId 'user-id'" -ForegroundColor Gray
    Write-Host "  .\invalidate-cache-simple.ps1 -All" -ForegroundColor Gray
    exit 0
}

Write-Host "[INFO] Creating CloudFront invalidation..." -ForegroundColor Yellow
Write-Host "  Distribution ID: $DISTRIBUTION_ID" -ForegroundColor Gray
Write-Host "  Paths: $($paths -join ', ')" -ForegroundColor Gray

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$callerReference = "invalidation-$timestamp"

# Create JSON for invalidation
$invalidationBatch = @{
    Paths = @{
        Quantity = $paths.Count
        Items = $paths
    }
    CallerReference = $callerReference
} | ConvertTo-Json -Depth 10

# Save to temp file
$tempFile = "invalidation-temp.json"
$invalidationBatch | Out-File -FilePath $tempFile -Encoding ASCII

# Create the invalidation
try {
    $result = aws cloudfront create-invalidation `
        --distribution-id $DISTRIBUTION_ID `
        --invalidation-batch file://$tempFile `
        --region $REGION `
        --output json | ConvertFrom-Json

    if ($result) {
        $invalidationId = $result.Invalidation.Id
        $status = $result.Invalidation.Status

        Write-Host ""
        Write-Host "[OK] Invalidation created successfully" -ForegroundColor Green
        Write-Host "  ID: $invalidationId" -ForegroundColor White
        Write-Host "  Status: $status" -ForegroundColor White
        Write-Host ""

        # Monitor progress for a few seconds
        Write-Host "[INFO] Monitoring progress..." -ForegroundColor Cyan
        $maxAttempts = 6
        $attempt = 0

        while ($attempt -lt $maxAttempts) {
            Start-Sleep -Seconds 5

            $statusCheck = aws cloudfront get-invalidation `
                --distribution-id $DISTRIBUTION_ID `
                --id $invalidationId `
                --region $REGION `
                --output json | ConvertFrom-Json

            $currentStatus = $statusCheck.Invalidation.Status

            if ($currentStatus -eq "Completed") {
                Write-Host "[OK] Invalidation completed!" -ForegroundColor Green
                break
            } else {
                $attempt++
                Write-Host "  Status: $currentStatus (attempt $attempt of $maxAttempts)" -ForegroundColor Gray
            }
        }

        if ($currentStatus -ne "Completed") {
            Write-Host "[INFO] Invalidation still in progress" -ForegroundColor Yellow
            Write-Host "Check status with:" -ForegroundColor Yellow
            Write-Host "  aws cloudfront get-invalidation --distribution-id $DISTRIBUTION_ID --id $invalidationId" -ForegroundColor White
        }
    }
} catch {
    Write-Host "[ERROR] Failed to create invalidation" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
} finally {
    # Clean up temp file
    Remove-Item $tempFile -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "=== PROCESS COMPLETED ===" -ForegroundColor Cyan
Write-Host "[INFO] Cache invalidation initiated" -ForegroundColor Green
Write-Host ""