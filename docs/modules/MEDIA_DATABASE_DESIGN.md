# 🗄️ Diseño de Base de Datos - Sistema Multimedia

## 📊 Tabla Principal: BIIHLIVE-MEDIA

### Estructura DynamoDB (NoSQL)

```yaml
Table: BIIHLIVE-MEDIA
Partition Key: PK
Sort Key: SK

# Estructura de Keys:
PK: USER#{userId}
SK: MEDIA#{mediaType}#{timestamp}#{mediaId}

# Ejemplo:
PK: USER#user123
SK: MEDIA#PHOTO#1706789456789#uuid-abc123
```

### Atributos del Item

```json
{
  // Keys primarias
  "PK": "USER#user123",
  "SK": "MEDIA#PHOTO#1706789456789#uuid-abc123",

  // Identificación
  "mediaId": "uuid-abc123",
  "userId": "user123",
  "mediaType": "PHOTO", // PHOTO | VIDEO | LIVE

  // URLs y rutas
  "mediaUrl": "gallery/user123/photos/1706789456789_abc123.jpg",
  "thumbnailUrl": "gallery/user123/thumbnails/1706789456789_abc123_thumb.jpg",
  "cloudFrontUrl": "https://d3xxx.cloudfront.net/",

  // Contenido
  "title": "Atardecer en Madrid",
  "descripcion": "Un atardecer increíble que siempre querrás ver",
  "caption": "Hermoso atardecer 🌅",
  "hashtags": ["madrid", "sunset", "photography", "nature"],
  "mentions": ["@user456", "@user789"],

  // Métricas de Engagement
  "score": 1523,              // Puntos totales (sistema de puntos)
  "visualizaciones": 45678,
  "compartidos": 234,
  "comentarios_count": 89,
  "guardados": 156,
  "reportes": 0,

  // Detalles de Puntuación (nuevo sistema)
  "puntuacion": {
    "total": 1523,
    "promedio": 4.2,
    "distribucion": {
      "5_puntos": 234,
      "4_puntos": 156,
      "3_puntos": 89,
      "2_puntos": 12,
      "1_punto": 5
    },
    "usuarios_puntuaron": 496
  },

  // Algoritmo y Ordenamiento
  "randomOrder": 0.5647761005753321,
  "trendingScore": 0.8234,
  "qualityScore": 0.9123,
  "freshnessScore": 0.9567,
  "engagementRate": 0.0334,  // (puntos+comentarios+compartidos)/visualizaciones

  // Información Técnica
  "metadata": {
    "fileSize": 2456789,      // bytes
    "originalWidth": 3024,
    "originalHeight": 4032,
    "processedWidth": 1920,
    "processedHeight": 2560,
    "aspectRatio": 0.75,
    "format": "jpeg",
    "duration": null,          // Solo para videos (segundos)
    "bitrate": null,           // Solo para videos
    "fps": null,               // Solo para videos
    "exif": {
      "camera": "iPhone 14 Pro",
      "iso": 100,
      "aperture": "f/1.78",
      "focalLength": "6.86mm"
    }
  },

  // Geolocalización
  "location": {
    "name": "Madrid, España",
    "latitude": 40.4168,
    "longitude": -3.7038,
    "placeId": "ChIJgTwKgJcpQg0RaSKMYcHeNsQ",
    "country": "ES",
    "city": "Madrid"
  },

  // Configuración y Privacidad
  "settings": {
    "visibility": "public",    // public | followers | private
    "commentsEnabled": true,
    "sharingEnabled": true,
    "downloadEnabled": false,
    "allowDuets": true,        // Para videos
    "allowStitch": true        // Para videos
  },

  // Moderación
  "moderation": {
    "status": "approved",      // pending | approved | rejected | flagged
    "isNSFW": false,
    "isFeatured": false,
    "isVerified": true,
    "aiLabels": ["sunset", "city", "landscape"],
    "aiScores": {
      "inappropriate": 0.02,
      "violence": 0.01,
      "adult": 0.03
    }
  },

  // Timestamps
  "createAt": "2024-01-30T10:30:45Z",
  "updatedAt": "2024-01-30T10:30:45Z",
  "publishedAt": "2024-01-30T10:31:00Z",

  // GSI attributes
  "GSI1PK": "MEDIA#PHOTO",
  "GSI1SK": "TRENDING#0.8234#2024-01-30",
  "GSI2PK": "LOCATION#Madrid",
  "GSI2SK": "MEDIA#2024-01-30#uuid-abc123",
  "GSI3PK": "HASHTAG#sunset",
  "GSI3SK": "SCORE#001523#uuid-abc123"
}
```

## 🔍 Global Secondary Indexes (GSIs)

### GSI1: Feed Principal y Trending
```yaml
GSI1PK: MEDIA#{mediaType}
GSI1SK: TRENDING#{score}#{date}
# Query: Obtener fotos/videos trending
```

### GSI2: Por Ubicación
```yaml
GSI2PK: LOCATION#{city}
GSI2SK: MEDIA#{date}#{mediaId}
# Query: Contenido por ciudad
```

### GSI3: Por Hashtags
```yaml
GSI3PK: HASHTAG#{tag}
GSI3SK: SCORE#{paddedScore}#{mediaId}
# Query: Buscar por hashtag ordenado por puntos
```

### GSI4: Feed Cronológico
```yaml
GSI4PK: FEED#PUBLIC
GSI4SK: TIMESTAMP#{reverseTimestamp}
# Query: Feed cronológico global
```

### GSI5: Por Usuario (Timeline)
```yaml
GSI5PK: TIMELINE#{userId}
GSI5SK: TIMESTAMP#{reverseTimestamp}
# Query: Timeline de un usuario específico
```

## 📋 Tablas Relacionadas

### 1. BIIHLIVE-MEDIA-INTERACTIONS

```yaml
# Puntos dados por usuarios
PK: MEDIA#{mediaId}
SK: POINTS#{userId}

Attributes:
  - points: 5              # 1-5 puntos
  - timestamp: ISO8601
  - userId: string
  - mediaId: string
```

### 2. BIIHLIVE-MEDIA-COMMENTS

```yaml
# Comentarios
PK: MEDIA#{mediaId}
SK: COMMENT#{timestamp}#{commentId}

Attributes:
  - commentId: uuid
  - userId: string
  - content: string
  - likesCount: number
  - parentCommentId: string (optional)
  - isPinned: boolean
  - timestamp: ISO8601
```

### 3. BIIHLIVE-MEDIA-VIEWS

```yaml
# Tracking detallado de vistas
PK: MEDIA#{mediaId}
SK: VIEW#{timestamp}#{userId}

Attributes:
  - userId: string (null para anónimos)
  - viewDuration: number (segundos)
  - viewPercentage: number (0-100)
  - source: string (feed|profile|direct|search)
  - timestamp: ISO8601
```

### 4. BIIHLIVE-MEDIA-SHARES

```yaml
# Compartidos
PK: MEDIA#{mediaId}
SK: SHARE#{timestamp}#{userId}

Attributes:
  - userId: string
  - shareType: string (internal|external|direct)
  - platform: string (whatsapp|twitter|instagram)
  - recipientId: string (para shares directos)
  - timestamp: ISO8601
```

## 🔄 Operaciones y Queries

### Subir Media
```python
def upload_media(user_id, media_type, file_data):
    # 1. Generar IDs y timestamps
    media_id = str(uuid.uuid4())
    timestamp = int(time.time() * 1000)

    # 2. Procesar archivo
    if media_type == "PHOTO":
        full_url = process_photo(file_data)
        thumb_url = generate_thumbnail(file_data)
    else:
        full_url = process_video(file_data)
        thumb_url = extract_video_thumbnail(file_data)

    # 3. Crear item en DynamoDB
    item = {
        'PK': f'USER#{user_id}',
        'SK': f'MEDIA#{media_type}#{timestamp}#{media_id}',
        'mediaId': media_id,
        'userId': user_id,
        'mediaType': media_type,
        'mediaUrl': full_url,
        'thumbnailUrl': thumb_url,
        'score': 0,
        'visualizaciones': 0,
        'createAt': datetime.now().isoformat(),
        # GSIs
        'GSI1PK': f'MEDIA#{media_type}',
        'GSI1SK': f'TRENDING#0000000000#{timestamp}',
        'GSI4PK': 'FEED#PUBLIC',
        'GSI4SK': f'TIMESTAMP#{9999999999999-timestamp}'
    }

    dynamodb.put_item(TableName='BIIHLIVE-MEDIA', Item=item)
    return media_id
```

### Obtener Feed Personalizado
```python
def get_personalized_feed(user_id, page_size=20):
    feeds = []

    # 1. Contenido reciente (30%)
    recent = query_gsi4_recent(limit=6)
    feeds.extend(recent)

    # 2. Trending (25%)
    trending = query_gsi1_trending(limit=5)
    feeds.extend(trending)

    # 3. Siguiendo (25%)
    following = query_following_media(user_id, limit=5)
    feeds.extend(following)

    # 4. Por ubicación (15%)
    location = query_gsi2_location(user_location, limit=3)
    feeds.extend(location)

    # 5. Descubrimiento (5%)
    discovery = query_random_media(limit=1)
    feeds.extend(discovery)

    # Mezclar con algoritmo
    return shuffle_weighted(feeds)
```

### Dar Puntos
```python
def give_points(user_id, media_id, points):
    # 1. Registrar interacción
    interaction = {
        'PK': f'MEDIA#{media_id}',
        'SK': f'POINTS#{user_id}',
        'points': points,
        'timestamp': datetime.now().isoformat()
    }

    # 2. Actualizar contador en media principal
    update_expression = """
        SET score = score + :points,
            puntuacion.total = puntuacion.total + :points,
            puntuacion.usuarios_puntuaron = puntuacion.usuarios_puntuaron + :one
    """

    dynamodb.update_item(
        TableName='BIIHLIVE-MEDIA',
        Key={'PK': f'USER#{owner_id}', 'SK': f'MEDIA#...'},
        UpdateExpression=update_expression,
        ExpressionAttributeValues={
            ':points': points,
            ':one': 1
        }
    )
```

## 📈 Algoritmo de Feed

### Factores de Ranking
```python
def calculate_media_score(media):
    # Pesos de cada factor
    WEIGHTS = {
        'freshness': 0.30,    # Qué tan reciente
        'engagement': 0.25,   # Interacciones
        'quality': 0.20,      # Calidad del contenido
        'relevance': 0.15,    # Relevancia para el usuario
        'viral': 0.10         # Potencial viral
    }

    # Calcular scores individuales
    freshness = calculate_freshness_score(media.createAt)
    engagement = (media.score + media.comentarios*2 + media.compartidos*3) / media.visualizaciones
    quality = media.qualityScore  # De AI
    relevance = calculate_user_relevance(user, media)
    viral = calculate_viral_potential(media)

    # Score final ponderado
    final_score = (
        freshness * WEIGHTS['freshness'] +
        engagement * WEIGHTS['engagement'] +
        quality * WEIGHTS['quality'] +
        relevance * WEIGHTS['relevance'] +
        viral * WEIGHTS['viral']
    )

    return final_score
```

## 🚀 Optimizaciones

1. **Batch Writes**: Para contadores, usar DynamoDB Streams + Lambda
2. **Caching**: ElastiCache para feeds populares
3. **CDN**: CloudFront para todas las imágenes/videos
4. **Compresión**: WebP para fotos, H.265 para videos
5. **Lazy Loading**: Cargar metadata primero, contenido después

## 📊 Métricas Clave

- **Engagement Rate**: (puntos + comentarios + compartidos) / visualizaciones
- **Viral Score**: compartidos² / tiempo_transcurrido
- **Quality Score**: resolución × (puntos_promedio / 5) × completion_rate
- **Retention**: usuarios_que_vuelven / usuarios_totales

---
*Este diseño soporta millones de items con queries eficientes y escalabilidad horizontal*