package com.empresa.aplicaciontensorflowliteandkeras

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun AlertScreen(
    countdown: Int,
    onCancel: () -> Unit,
    onTimeout: () -> Unit // Esta función se ejecutará cuando llegue a 0
) {
    // Generador de sonidos (Volumen al 50)
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_ALARM, 50) }

    // Limpiar sonido al cerrar la pantalla
    DisposableEffect(Unit) {
        onDispose {
            toneGenerator.release()
        }
    }

    // Lógica del contador y sonido
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            // Pitido corto cada segundo
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            delay(1000L)
            MonitoringState.countdown.value -= 1
        } else if (countdown == 0) {
            // Sonido final de confirmación
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1000)
            onTimeout() // Ejecuta el protocolo de emergencia
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("¡CAÍDA DETECTADA!", color = MaterialTheme.colorScheme.onError, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Enviando alerta en...", color = MaterialTheme.colorScheme.onError, fontSize = 24.sp)
        Text("$countdown", color = MaterialTheme.colorScheme.onError, fontSize = 72.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = {
                // Reiniciar estados al cancelar
                MonitoringState.countdown.value = 5
                MonitoringState.sosActive.value = false
                onCancel()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onError, contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(80.dp)
        ) {
            Text("CANCELAR ALERTA", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}