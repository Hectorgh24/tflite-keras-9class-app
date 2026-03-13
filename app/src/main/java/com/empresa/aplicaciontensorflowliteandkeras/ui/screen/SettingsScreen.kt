package com.empresa.aplicaciontensorflowliteandkeras

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
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
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text("Próximamente:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Text("- Sensibilidad del acelerómetro", color = MaterialTheme.colorScheme.onBackground)
            Text("- Activar enlace GPS en SMS", color = MaterialTheme.colorScheme.onBackground)
            Text("- Ejecución en segundo plano", color = MaterialTheme.colorScheme.onBackground)
        }
    }
}