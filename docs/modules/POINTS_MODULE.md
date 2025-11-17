# 🏆 Sistema de Puntuación y Gamificación

## Estado: 🚧 En Desarrollo

## Concepto
Sistema de puntos y niveles para incentivar participación y engagement.

## Estructura de Puntos

### Acciones que Otorgan Puntos
| Acción | Puntos | Límite Diario |
|--------|--------|---------------|
| Login diario | 10 | 1 vez |
| Publicar foto | 25 | 5 fotos |
| Publicar video | 50 | 3 videos |
| Recibir like | 5 | Sin límite |
| Dar like | 1 | 50 likes |
| Nuevo seguidor | 10 | Sin límite |
| Comentario | 3 | 20 comentarios |
| Compartir | 15 | 10 shares |
| Live streaming (por min) | 2 | 120 min |
| Ver live (por min) | 1 | 60 min |

### Sistema de Niveles
```kotlin
fun calculateLevel(totalPoints: Int): Int {
    // Fórmula: Nivel = sqrt(puntos / 100)
    return sqrt(totalPoints / 100.0).toInt()
}

fun pointsForNextLevel(currentLevel: Int): Int {
    // Puntos necesarios = (nivel + 1)² × 100
    return ((currentLevel + 1) * (currentLevel + 1)) * 100
}
```

### Niveles y Badges
| Nivel | Puntos | Badge | Beneficios |
|-------|--------|-------|------------|
| 1-10 | 0-10K | Bronce | Básico |
| 11-25 | 10K-62.5K | Plata | +10% puntos |
| 26-50 | 62.5K-250K | Oro | +20% puntos, badge especial |
| 51-100 | 250K-1M | Diamante | +30% puntos, prioridad feed |
| 101+ | 1M+ | Leyenda | +50% puntos, features exclusivos |

## Implementación Backend

### Tabla DynamoDB: BIIHLIVE-POINTS
```yaml
PK: userId
SK: POINTS#TOTAL
Attributes:
  - totalPoints: Number
  - currentLevel: Number
  - dailyPoints: Number
  - lastReset: ISO8601

# Historial
PK: userId
SK: POINTS#timestamp
Attributes:
  - action: String
  - points: Number
  - metadata: Map
```

### Lambda: ProcessPoints
```python
def lambda_handler(event, context):
    # DynamoDB Stream trigger
    for record in event['Records']:
        if record['eventName'] == 'INSERT':
            action = record['dynamodb']['NewImage']
            award_points(action)
            check_level_up(user_id)
            update_rankings(user_id)
```

## UI Components

### Barra de Progreso de Nivel
```kotlin
@Composable
fun LevelProgressBar(
    currentPoints: Int,
    currentLevel: Int
) {
    val pointsInLevel = currentPoints - pointsForLevel(currentLevel)
    val pointsNeeded = pointsForNextLevel(currentLevel) - pointsForLevel(currentLevel)
    val progress = pointsInLevel.toFloat() / pointsNeeded

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Nivel $currentLevel", fontWeight = FontWeight.Bold)
            Text("${pointsInLevel}/${pointsNeeded} pts")
        }

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = BiihliveOrangeLight,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
```

### Badge de Nivel
```kotlin
@Composable
fun LevelBadge(level: Int) {
    val (color, icon) = when {
        level <= 10 -> Bronze to Icons.Default.Star
        level <= 25 -> Silver to Icons.Default.Stars
        level <= 50 -> Gold to Icons.Default.EmojiEvents
        level <= 100 -> Diamond to Icons.Default.Diamond
        else -> Legendary to Icons.Default.Whatshot
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Nivel $level",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
```

## Sistema de Rankings

### Tipos de Rankings
1. **Global** - Todos los usuarios
2. **País** - Por país del usuario
3. **Ciudad** - Por ciudad del usuario
4. **Semanal** - Reset cada lunes
5. **Mensual** - Reset día 1

### Queries GraphQL
```graphql
query GetRankings($type: RankingType!, $limit: Int) {
    getRankings(type: $type, limit: $limit) {
        items {
            position
            userId
            nickname
            avatar
            points
            level
        }
    }
}

query GetUserRanking($userId: ID!, $scope: RankingScope!) {
    getUserRanking(userId: $userId, scope: $scope) {
        position
        totalParticipants
        percentile
    }
}
```

## Notificaciones de Logros
```kotlin
sealed class Achievement {
    data class LevelUp(val newLevel: Int) : Achievement()
    data class Milestone(val points: Int) : Achievement()
    data class Streak(val days: Int) : Achievement()
    data class TopRanking(val position: Int, val scope: String) : Achievement()
}

fun showAchievementToast(achievement: Achievement) {
    when (achievement) {
        is LevelUp -> "🎉 ¡Nivel ${achievement.newLevel} alcanzado!"
        is Milestone -> "🏆 ¡${achievement.points} puntos conseguidos!"
        is Streak -> "🔥 ¡Racha de ${achievement.days} días!"
        is TopRanking -> "👑 ¡Eres #${achievement.position} en ${achievement.scope}!"
    }
}
```

## Analytics y Métricas
- Puntos promedio por usuario/día
- Distribución de niveles
- Acciones más realizadas
- Retención por nivel
- Conversión a niveles altos

## Próximas Features
- [ ] Multiplicadores de puntos
- [ ] Eventos especiales (2x puntos)
- [ ] Retos semanales
- [ ] Logros desbloqueables
- [ ] Tienda de recompensas
- [ ] Ligas competitivas
- [ ] Boost de puntos pagados