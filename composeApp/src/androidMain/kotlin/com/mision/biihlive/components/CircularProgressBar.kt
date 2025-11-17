package com.mision.biihlive.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mision.biihlive.ui.theme.BiihliveOrange
import com.mision.biihlive.ui.theme.BiihliveOrangeLight

/**
 * Composable que muestra una barra de progreso circular alrededor del contenido
 * @param progress Progreso de 0.0 a 1.0
 * @param modifier Modificador del composable
 * @param size Tamaño del círculo
 * @param strokeWidth Ancho del trazo de la barra de progreso
 * @param backgroundColor Color de fondo de la barra
 * @param progressColor Color del progreso (puede ser un gradiente)
 * @param content Contenido dentro del círculo (típicamente el avatar)
 */
@Composable
fun CircularProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 170.dp,
    strokeWidth: Dp = 8.dp,
    backgroundColor: Color = BiihliveOrangeLight.copy(alpha = 0.3f),
    progressColor: Color = BiihliveOrange,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Dibujar el anillo de progreso
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val sweepAngle = progress * 360f
            val strokePx = strokeWidth.toPx()

            // Círculo de fondo
            drawCircle(
                color = backgroundColor,
                style = Stroke(width = strokePx)
            )

            // Arco de progreso (comienza desde abajo, sentido horario)
            rotate(degrees = 90f) {
                drawArc(
                    color = progressColor,
                    startAngle = 0f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round
                    )
                )
            }
        }

        // Contenido (avatar) con padding para no tocar la barra
        Box(
            modifier = Modifier
                .fillMaxSize()
                .size(size - (strokeWidth * 2)),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/**
 * Versión con gradiente para el progreso
 */
@Composable
fun CircularProgressBarGradient(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 170.dp,
    strokeWidth: Dp = 8.dp,
    backgroundColor: Color = BiihliveOrangeLight.copy(alpha = 0.3f),
    progressColors: List<Color> = listOf(BiihliveOrange, BiihliveOrangeLight),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val sweepAngle = progress * 360f
            val strokePx = strokeWidth.toPx()

            // Círculo de fondo
            drawCircle(
                color = backgroundColor,
                style = Stroke(width = strokePx)
            )

            // Arco de progreso con gradiente (comienza desde abajo, sentido horario)
            rotate(degrees = 90f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = progressColors
                    ),
                    startAngle = 0f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round
                    )
                )
            }
        }

        // Contenido con padding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .size(size - (strokeWidth * 2)),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}