package com.empresa.aplicaciontensorflowliteandkeras

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.empresa.aplicaciontensorflowliteandkeras.ui.theme.AplicacionTensorflowLiteAndKerasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AplicacionTensorflowLiteAndKerasTheme {
                AppNavigator()
            }
        }
    }

    /**
     * Se invoca cuando la Activity ya existe y recibe un nuevo Intent
     * (FLAG_ACTIVITY_SINGLE_TOP). Esto permite que la alerta de caída
     * traiga la app al frente sin recrear la Activity ni reiniciar la navegación.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // La alerta se maneja reactivamente via MonitoringState.sosActive
        // que ya está en true cuando llegamos aquí, así que no se necesita
        // procesamiento adicional del intent.
    }
}