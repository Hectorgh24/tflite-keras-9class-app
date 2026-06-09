package com.empresa.aplicaciontensorflowliteandkeras.ui.screen.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empresa.aplicaciontensorflowliteandkeras.PredictionEvent

/**
 * Lista ordenada de las 9 clases del modelo TensorFlow/Keras.
 * El orden corresponde a los índices del modelo entrenado.
 */
val CLASS_LIST_TF = listOf(
    "Caminando",
    "Caída frontal",
    "Caída a la derecha",
    "Caída hacia atrás",
    "Caída contra obstáculo",
    "Caída (intentando protegerse)",
    "Caída al sentarse",
    "Desmayo / Síncope",
    "Caída a la izquierda"
)

/**
 * Gráfico de dispersión (scatter) que muestra el historial de predicciones a lo largo
 * del tiempo. El eje Y representa las 9 clases y el eje X el tiempo en segundos.
 * Las caídas (índices 1-8) se dibujan en rojo y la actividad normal (índice 0) en azul.
 */
@Composable
fun TimelineChart(
    predictionHistory: List<PredictionEvent>,
    durationSeconds: Long,
    modifier: Modifier = Modifier
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    Column(modifier = modifier) {
        Text(
            "Línea de Tiempo de Predicciones",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        if (predictionHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(surfaceVariantColor.copy(alpha = 0.3f)),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    "Sin datos de predicción aún",
                    color = onSurfaceColor.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }
            return
        }

        // Mostrar todo el historial de los 120 segundos
        val maxTimeWindow = 120f
        val currentTime = maxOf(30f, durationSeconds.toFloat())
        val minTime = maxOf(0f, currentTime - maxTimeWindow)
        val maxTime = currentTime + 2f

        val scrollState = rememberScrollState()
        val density = LocalDensity.current

        // Anchura fija calculada sobre la ventana visible, dando un buen espacio para leer las etiquetas (40dp por segundo)
        val chartWidthDp = with(density) {
            maxOf(600.dp, ((maxTime - minTime) * 40).dp)
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            // Etiquetas del eje Y (nombres de clases)
            Column(
                modifier = Modifier
                    .width(110.dp)
                    .height(280.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                CLASS_LIST_TF.reversed().forEach { label ->
                    Text(
                        text = label,
                        fontSize = 8.sp,
                        lineHeight = 9.sp,
                        color = onSurfaceColor.copy(alpha = 0.8f),
                        maxLines = 2,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                }
            }

            // Área del gráfico con scroll horizontal
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(280.dp)
                    .horizontalScroll(scrollState)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(chartWidthDp)
                        .fillMaxHeight()
                ) {
                    val chartWidth = size.width
                    val chartHeight = size.height
                    val numClasses = CLASS_LIST_TF.size
                    val cellHeight = chartHeight / numClasses

                    // Líneas de cuadrícula horizontales
                    for (i in 0..numClasses) {
                        val y = i * cellHeight
                        drawLine(
                            color = onSurfaceColor.copy(alpha = 0.1f),
                            start = Offset(0f, y),
                            end = Offset(chartWidth, y),
                            strokeWidth = 1f
                        )
                    }

                    // Líneas de cuadrícula verticales cada 1 segundo dentro de la ventana de tiempo
                    val timeRange = maxTime - minTime
                    val xStep = chartWidth / timeRange

                    var t = (minTime / 1f).toInt() * 1f
                    while (t <= maxTime) {
                        if (t >= minTime) {
                            val x = (t - minTime) * xStep
                            drawLine(
                                color = onSurfaceColor.copy(alpha = 0.1f),
                                start = Offset(x, 0f),
                                end = Offset(x, chartHeight),
                                strokeWidth = 1f
                            )

                            // Etiqueta del eje X
                            drawContext.canvas.nativeCanvas.drawText(
                                "${t.toInt()}s",
                                x,
                                chartHeight - 4f,
                                android.graphics.Paint().apply {
                                    color = onSurfaceColor.copy(alpha = 0.6f).toArgb()
                                    textSize = 24f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                            )
                        }
                        t += 1f
                    }

                    // Dibujar puntos de predicción
                    for (event in predictionHistory) {
                        if (event.timeSeconds < minTime) continue // Ignorar eventos viejos que quedan fuera del área visible
                        val classIndex = CLASS_LIST_TF.indexOf(event.className)
                        if (classIndex < 0) continue

                        val x = (event.timeSeconds.toFloat() - minTime) * xStep
                        // Invertir Y para que la clase 0 esté abajo
                        val y = chartHeight - (classIndex + 0.5f) * cellHeight

                        val isFall = classIndex >= 1 // Índices 1-8 son caídas
                        val dotColor = if (isFall) errorColor else primaryColor

                        drawCircle(
                            color = dotColor,
                            radius = 6f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }

        // Auto-scroll al final
        LaunchedEffect(predictionHistory.size) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
}
