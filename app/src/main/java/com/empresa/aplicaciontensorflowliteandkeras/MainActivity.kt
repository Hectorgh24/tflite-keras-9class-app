package com.empresa.aplicaciontensorflowliteandkeras

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.concurrent.thread

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.empresa.aplicaciontensorflowliteandkeras.ui.theme.AplicacionTensorflowLiteAndKerasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { androidx.core.content.ContextCompat.startForegroundService(this, android.content.Intent(this, DummyForegroundService::class.java)) } catch (e: Exception) {}
        startUdpListener()
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

    private fun startUdpListener() {
        thread(isDaemon = true) {
            try {
                val socket = DatagramSocket(null)
                socket.reuseAddress = true
                socket.bind(java.net.InetSocketAddress(50000))
                socket.broadcast = true
                val buffer = ByteArray(256)
                while (true) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length).trim()
                    Log.d("UDP_LISTENER", "Recibido: $message")
                    
                    if (message == "START_MONITORING") {
                        if (!MonitoringState.isMonitoring.value) {
                            val serviceIntent = Intent(this, FallDetectionService::class.java)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                startForegroundService(serviceIntent)
                            } else {
                                startService(serviceIntent)
                            }
                        }
                    } else if (message == "STOP_MONITORING") {
                        if (MonitoringState.isMonitoring.value) {
                            val serviceIntent = Intent(this, FallDetectionService::class.java)
                            stopService(serviceIntent)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("UDP_LISTENER", "Error: ${e.message}")
            }
        }
    }

    override fun onResume() { super.onResume(); try { androidx.core.content.ContextCompat.startForegroundService(this, android.content.Intent(this, DummyForegroundService::class.java)) } catch (e: Exception) {} }
}