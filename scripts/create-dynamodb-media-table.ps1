# Script para crear tabla DynamoDB BIIHLIVE-MEDIA
# Sistema de galería multimedia Biihlive

Write-Host "[INFO] Creando tabla DynamoDB BIIHLIVE-MEDIA..." -ForegroundColor Green

$TABLE_NAME = "BIIHLIVE-MEDIA"
$REGION = "eu-west-3"

# Crear JSON de configuración de la tabla
$tableDefinition = @'
{
    "TableName": "BIIHLIVE-MEDIA",
    "AttributeDefinitions": [
        {
            "AttributeName": "PK",
            "AttributeType": "S"
        },
        {
            "AttributeName": "SK",
            "AttributeType": "S"
        },
        {
            "AttributeName": "GSI1PK",
            "AttributeType": "S"
        },
        {
            "AttributeName": "GSI1SK",
            "AttributeType": "S"
        },
        {
            "AttributeName": "GSI2PK",
            "AttributeType": "S"
        },
        {
            "AttributeName": "GSI2SK",
            "AttributeType": "S"
        },
        {
            "AttributeName": "GSI3PK",
            "AttributeType": "S"
        },
        {
            "AttributeName": "GSI3SK",
            "AttributeType": "S"
        },
        {
            "AttributeName": "GSI4PK",
            "AttributeType": "S"
        },
        {
            "AttributeName": "GSI4SK",
            "AttributeType": "S"
        }
    ],
    "KeySchema": [
        {
            "AttributeName": "PK",
            "KeyType": "HASH"
        },
        {
            "AttributeName": "SK",
            "KeyType": "RANGE"
        }
    ],
    "GlobalSecondaryIndexes": [
        {
            "IndexName": "GSI1-TrendingFeed",
            "Keys": [
                {
                    "AttributeName": "GSI1PK",
                    "KeyType": "HASH"
                },
                {
                    "AttributeName": "GSI1SK",
                    "KeyType": "RANGE"
                }
            ],
            "Projection": {
                "ProjectionType": "ALL"
            },
            "ProvisionedThroughput": {
                "ReadCapacityUnits": 5,
                "WriteCapacityUnits": 5
            }
        },
        {
            "IndexName": "GSI2-LocationFeed",
            "Keys": [
                {
                    "AttributeName": "GSI2PK",
                    "KeyType": "HASH"
                },
                {
                    "AttributeName": "GSI2SK",
                    "KeyType": "RANGE"
                }
            ],
            "Projection": {
                "ProjectionType": "ALL"
            },
            "ProvisionedThroughput": {
                "ReadCapacityUnits": 5,
                "WriteCapacityUnits": 5
            }
        },
        {
            "IndexName": "GSI3-HashtagFeed",
            "Keys": [
                {
                    "AttributeName": "GSI3PK",
                    "KeyType": "HASH"
                },
                {
                    "AttributeName": "GSI3SK",
                    "KeyType": "RANGE"
                }
            ],
            "Projection": {
                "ProjectionType": "ALL"
            },
            "ProvisionedThroughput": {
                "ReadCapacityUnits": 5,
                "WriteCapacityUnits": 5
            }
        },
        {
            "IndexName": "GSI4-ChronologicalFeed",
            "Keys": [
                {
                    "AttributeName": "GSI4PK",
                    "KeyType": "HASH"
                },
                {
                    "AttributeName": "GSI4SK",
                    "KeyType": "RANGE"
                }
            ],
            "Projection": {
                "ProjectionType": "ALL"
            },
            "ProvisionedThroughput": {
                "ReadCapacityUnits": 5,
                "WriteCapacityUnits": 5
            }
        }
    ],
    "BillingMode": "PAY_PER_REQUEST",
    "StreamSpecification": {
        "StreamEnabled": true,
        "StreamViewType": "NEW_AND_OLD_IMAGES"
    },
    "Tags": [
        {
            "Key": "Environment",
            "Value": "Production"
        },
        {
            "Key": "Application",
            "Value": "Biihlive"
        },
        {
            "Key": "Module",
            "Value": "Gallery"
        }
    ]
}
'@

# Guardar JSON temporalmente
$tableDefinition | Out-File -FilePath "table-definition.json" -Encoding UTF8

# Verificar si la tabla ya existe
Write-Host "[INFO] Verificando si la tabla ya existe..." -ForegroundColor Yellow
$tableExists = aws dynamodb describe-table --table-name $TABLE_NAME --region $REGION 2>$null

if ($tableExists) {
    Write-Host "[WARN] La tabla $TABLE_NAME ya existe" -ForegroundColor Yellow
    Write-Host "[INFO] Eliminando tabla existente..." -ForegroundColor Yellow

    # Eliminar tabla existente
    aws dynamodb delete-table --table-name $TABLE_NAME --region $REGION

    # Esperar a que se elimine completamente
    Write-Host "[INFO] Esperando eliminación de tabla..." -ForegroundColor Yellow
    aws dynamodb wait table-not-exists --table-name $TABLE_NAME --region $REGION
}

# Crear la tabla
Write-Host "[INFO] Creando tabla $TABLE_NAME..." -ForegroundColor Green
aws dynamodb create-table --cli-input-json file://table-definition.json --region $REGION

# Limpiar archivo temporal
Remove-Item "table-definition.json"

# Esperar a que la tabla esté activa
Write-Host "[INFO] Esperando a que la tabla esté activa..." -ForegroundColor Yellow
aws dynamodb wait table-exists --table-name $TABLE_NAME --region $REGION

# Verificar estado
Write-Host "`n[INFO] Estado de la tabla:" -ForegroundColor Green
aws dynamodb describe-table --table-name $TABLE_NAME --region $REGION --query "Table.TableStatus" --output text

Write-Host "`n[OK] Tabla BIIHLIVE-MEDIA creada exitosamente!" -ForegroundColor Green
Write-Host "[INFO] Detalles:" -ForegroundColor Cyan
Write-Host "  - Tabla: $TABLE_NAME"
Write-Host "  - Region: $REGION"
Write-Host "  - Billing: PAY_PER_REQUEST (On-Demand)"
Write-Host "  - Streams: Habilitados"
Write-Host "  - GSIs: 4 indices creados"