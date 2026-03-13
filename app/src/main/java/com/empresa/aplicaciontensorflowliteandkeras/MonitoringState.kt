package com.empresa.aplicaciontensorflowliteandkeras

import kotlinx.coroutines.flow.MutableStateFlow

object MonitoringState {
    val isMonitoring = MutableStateFlow(false)
    val currentPrediction = MutableStateFlow("Inactivo")
    val sosActive = MutableStateFlow(false)
    val countdown = MutableStateFlow(5) // Nueva variable
}