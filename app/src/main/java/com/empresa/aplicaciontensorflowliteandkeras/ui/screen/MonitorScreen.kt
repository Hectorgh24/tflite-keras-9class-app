package com.empresa.aplicaciontensorflowliteandkeras

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MonitorScreen(
    hasPermissions: Boolean,
    isMonitoring: Boolean,
    emergencyNumber: String,
    currentPrediction: String,
    onNumberChange: (String) -> Unit,
    onRequestPermissions: () -> Unit,
    onToggleMonitoring: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!hasPermissions) {
            Text("Se requieren permisos para emitir alertas SOS.", color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequestPermissions) { Text("Otorgar Permisos") }
        } else {
            OutlinedTextField(
                value = emergencyNumber,
                onValueChange = { input ->
                    val digitsOnly = input.filter { it.isDigit() }
                    if (digitsOnly.length <= 10) {
                        onNumberChange(digitsOnly)
                    }
                },
                label = { Text("Contacto de Emergencia") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isMonitoring
            )

            Text(
                text = "${emergencyNumber.length} / 10 dígitos",
                fontSize = 12.sp,
                color = if (emergencyNumber.length == 10) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
            Text("Estado del Modelo:", fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(currentPrediction, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onToggleMonitoring,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isMonitoring) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            ) {
                Text(if (isMonitoring) "DETENER MONITOREO" else "INICIAR MONITOREO", fontSize = 18.sp)
            }
        }
    }
}