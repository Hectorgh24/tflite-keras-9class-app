package com.empresa.aplicaciontensorflowliteandkeras.ui.screen.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empresa.aplicaciontensorflowliteandkeras.SensorEventData

/**
 * Gráfico de línea en tiempo real que muestra los datos crudos del acelerómetro
 * (ejes X, Y, Z) a lo largo del tiempo. Equivalente al LineChart de MPAndroidChart
 * del proyecto Edge Impulse, pero implementado como Canvas nativo de Compose.
 */
@Composable
fun SensorChart(
    sensorHistory: List<SensorEventData>,
    modifier: Modifier = Modifier
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    // Colores para cada eje del acelerómetro
    val colorX = Color(0xFFEF5350) // Rojo
    val colorY = Color(0xFF66BB6A) // Verde
    val colorZ = Color(0xFF42A5F5) // Azul

    Column(modifier = modifier) {
        Text(
            "Datos del Acelerómetro (Tiempo Real)",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Leyenda
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(color = colorX, label = "Eje X")
            LegendItem(color = colorY, label = "Eje Y")
            LegendItem(color = colorZ, label = "Eje Z")
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (sensorHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(surfaceVariantColor.copy(alpha = 0.3f)),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    "Sin datos del sensor aún",
                    color = onSurfaceColor.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }
            return
        }

        // Rango del eje Y: -25 a 25 m/s²
        val yMin = -25f
        val yMax = 25f

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val chartWidth = size.width
            val chartHeight = size.height
            val yRange = yMax - yMin

            // Margen izquierdo para etiquetas del eje Y
            val leftMargin = 50f
            val drawableWidth = chartWidth - leftMargin
            val bottomMargin = 30f
            val drawableHeight = chartHeight - bottomMargin

            // Líneas de cuadrícula horizontales y etiquetas Y
            val ySteps = listOf(-25f, -15f, -5f, 0f, 5f, 15f, 25f)
            for (yVal in ySteps) {
                val yPos = drawableHeight - ((yVal - yMin) / yRange) * drawableHeight
                drawLine(
                    color = onSurfaceColor.copy(alpha = 0.1f),
                    start = Offset(leftMargin, yPos),
                    end = Offset(chartWidth, yPos),
                    strokeWidth = 1f
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "${yVal.toInt()}",
                    leftMargin - 8f,
                    yPos + 8f,
                    android.graphics.Paint().apply {
                        color = onSurfaceColor.copy(alpha = 0.6f).toArgb()
                        textSize = 22f
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                )
            }

            if (sensorHistory.size < 2) return@Canvas

            // Calcular el rango de tiempo visible (últimos ~10 segundos)
            val firstTime = sensorHistory.first().timeOffsetMillis / 1000f
            val lastTime = sensorHistory.last().timeOffsetMillis / 1000f
            val visibleRange = maxOf(lastTime - firstTime, 1f)

            // Líneas de cuadrícula verticales (cada 2 segundos)
            val timeStart = firstTime
            var t = (timeStart.toInt() / 2 * 2).toFloat()
            while (t <= lastTime) {
                if (t >= firstTime) {
                    val xPos = leftMargin + ((t - firstTime) / visibleRange) * drawableWidth
                    drawLine(
                        color = onSurfaceColor.copy(alpha = 0.1f),
                        start = Offset(xPos, 0f),
                        end = Offset(xPos, drawableHeight),
                        strokeWidth = 1f
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "${t.toInt()}s",
                        xPos,
                        chartHeight - 4f,
                        android.graphics.Paint().apply {
                            color = onSurfaceColor.copy(alpha = 0.6f).toArgb()
                            textSize = 22f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
                t += 2f
            }

            // Función para dibujar una línea de datos
            fun drawDataLine(dataExtractor: (SensorEventData) -> Float, lineColor: Color) {
                val path = Path()
                var started = false
                for (data in sensorHistory) {
                    val xPos = leftMargin + ((data.timeOffsetMillis / 1000f - firstTime) / visibleRange) * drawableWidth
                    val value = dataExtractor(data).coerceIn(yMin, yMax)
                    val yPos = drawableHeight - ((value - yMin) / yRange) * drawableHeight

                    if (!started) {
                        path.moveTo(xPos, yPos)
                        started = true
                    } else {
                        path.lineTo(xPos, yPos)
                    }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3f)
                )
            }

            // Dibujar las tres líneas (ejes X, Y, Z)
            drawDataLine({ it.x }, colorX)
            drawDataLine({ it.y }, colorY)
            drawDataLine({ it.z }, colorZ)
        }
    }
}

/**
 * Elemento de leyenda simple con un indicador de color y texto.
 */
@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color, radius = size.minDimension / 2f)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
