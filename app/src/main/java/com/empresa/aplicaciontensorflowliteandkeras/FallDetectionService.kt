package com.empresa.aplicaciontensorflowliteandkeras

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class FallDetectionService : Service() {

    private lateinit var classifier: FallDetectionClassifier
    private lateinit var sensorHandler: SensorHandler

    override fun onCreate() {
        super.onCreate()

        // 1. Crear el canal de notificaciones (OBLIGATORIO para evitar cierres forzados)
        createNotificationChannel()

        classifier = FallDetectionClassifier(this)

        sensorHandler = SensorHandler(this) { windowData ->
            try {
                processInference(windowData)
            } catch (e: Exception) {
                Log.e("FallService", "Fallo crítico al procesar la ventana de datos del sensor", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "fall_channel",
                "Monitoreo de Caídas",
                NotificationManager.IMPORTANCE_LOW // IMPORTANCE_LOW evita que suene cada vez que inicia
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun processInference(data: FloatArray) {
        val (label, confidence) = classifier.classify(data)

        // Actualizar la UI en tiempo real
        val porcentaje = (confidence * 100).toInt()
        MonitoringState.currentPrediction.value = "$label ($porcentaje%)"

        // Lógica de detección (> 85% de confianza y que no sea caminar)
        if (label != "Caminando" && confidence > 0.85f) {
            Log.w("FallService", "CAÍDA DETECTADA: $label con $porcentaje%")

            if (!MonitoringState.sosActive.value) {
                MonitoringState.sosActive.value = true

                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("FALL_DETECTED", true)
                    putExtra("FALL_TYPE", label)
                }
                startActivity(intent)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MonitoringState.isMonitoring.value = true

        val notification = NotificationCompat.Builder(this, "fall_channel")
            .setContentTitle("Protección activa")
            .setContentText("Monitoreando actividad en segundo plano")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegúrate de que este ícono existe
            .build()

        // 2. Iniciar el servicio en primer plano de manera segura
        startForeground(1, notification)
        sensorHandler.start()

        return START_STICKY
    }

    override fun onDestroy() {
        MonitoringState.isMonitoring.value = false
        MonitoringState.currentPrediction.value = "Inactivo"

        sensorHandler.stop()
        classifier.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}