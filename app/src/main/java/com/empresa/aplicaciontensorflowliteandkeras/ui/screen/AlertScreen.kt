package com.empresa.aplicaciontensorflowliteandkeras

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AlertScreen(countdown: Int, onCancel: () -> Unit) {
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
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onError, contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(80.dp)
        ) {
            Text("CANCELAR ALERTA", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}