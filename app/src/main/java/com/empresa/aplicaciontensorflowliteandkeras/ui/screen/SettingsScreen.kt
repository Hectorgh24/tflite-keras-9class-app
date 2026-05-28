package com.empresa.aplicaciontensorflowliteandkeras

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empresa.aplicaciontensorflowliteandkeras.ui.screen.charts.SensorChart
import com.empresa.aplicaciontensorflowliteandkeras.ui.screen.charts.TimelineChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val session by MonitoringLogManager.currentSession.collectAsState()
    val savedSession by produceState<MonitoringSessionLog?>(initialValue = null, context) {
        value = MonitoringLogManager.loadLastSession(context)
    }
    var expanded by remember { mutableStateOf(false) }
    var reportPath by remember { mutableStateOf<String?>(null) }
    val visibleSession = session ?: savedSession

    // Observar datos de gráficos en tiempo real
    val predictionHistory by MonitoringState.predictionHistory.collectAsState()
    val sensorHistory by MonitoringState.sensorHistory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Volver", color = MaterialTheme.colorScheme.primary) }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { expanded = !expanded }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Monitoreo Log", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Visualizar métricas de monitoreo, gráficos y generar reporte JSON",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (expanded) "Ocultar" else "Abrir",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (expanded) {
                if (visibleSession != null) {
                    Text("Datos de monitoreo", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Inicio: ${formatTimestamp(visibleSession.sessionStartMillis)}")
                    Text("Fin: ${visibleSession.sessionEndMillis?.let { formatTimestamp(it) } ?: "Activo"}")
                    Text("Duración: ${visibleSession.durationSeconds} segundos")
                    Text("Ventanas procesadas: ${visibleSession.windowsProcessed}")
                    Text("Caídas detectadas: ${visibleSession.fallCount}")
                    Text("Alertas enviadas: ${visibleSession.alertsTriggered}")
                    Text("Número de emergencia: ${visibleSession.emergencyNumber}")
                    Text("Última predicción: ${visibleSession.currentPrediction}")

                    Spacer(modifier = Modifier.height(24.dp))

                    // ──── Gráfico de Línea de Tiempo de Predicciones ────
                    TimelineChart(
                        predictionHistory = if (predictionHistory.isNotEmpty())
                            predictionHistory
                        else
                            visibleSession.predictionHistory,
                        durationSeconds = visibleSession.durationSeconds,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ──── Gráfico de Acelerómetro en Tiempo Real ────
                    SensorChart(
                        sensorHistory = sensorHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                } else {
                    Text(
                        "No hay datos de sesión disponibles.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { reportPath = MonitoringLogManager.exportReport(context) },
                    enabled = visibleSession != null,
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Text("Obtener reporte")
                }

                reportPath?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Reporte guardado en:\n$it",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
