package com.empresa.aplicaciontensorflowliteandkeras

import kotlinx.coroutines.flow.MutableStateFlow

object MonitoringState {
    val isMonitoring = MutableStateFlow(false)
    val currentPrediction = MutableStateFlow("Inactivo")
    val sosActive = MutableStateFlow(false)
    val countdown = MutableStateFlow(5)

    /** Historial de predicciones para el gráfico de línea de tiempo */
    val predictionHistory = MutableStateFlow<List<PredictionEvent>>(emptyList())

    /** Historial reciente del sensor para el gráfico de acelerómetro */
    val sensorHistory = MutableStateFlow<List<SensorEventData>>(emptyList())
}