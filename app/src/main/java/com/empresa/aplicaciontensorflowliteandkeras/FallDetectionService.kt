package com.empresa.aplicaciontensorflowliteandkeras

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FallDetectionService : Service() {

    private lateinit var classifier: FallDetectionClassifier
    private lateinit var sensorHandler: SensorHandler
    private lateinit var inferenceExecutor: ExecutorService
    @Volatile
    private var classifierReady = false

    override fun onCreate() {
        super.onCreate()

        // 1. Crear el canal de notificaciones (OBLIGATORIO para evitar cierres forzados)
        createNotificationChannel()

        inferenceExecutor = Executors.newSingleThreadExecutor()
        inferenceExecutor.execute {
            try {
                classifier = FallDetectionClassifier(this)
                classifierReady = true
                Log.d("FallService", "Clasificador inicializado en segundo plano")
            } catch (e: Exception) {
                Log.e("FallService", "No se pudo inicializar el clasificador", e)
            }
        }

        sensorHandler = SensorHandler(this) { windowData ->
            inferenceExecutor.execute {
                try {
                    processInference(windowData)
                } catch (e: Exception) {
                    Log.e("FallService", "Fallo crítico al procesar la ventana de datos del sensor", e)
                }
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
        if (!classifierReady) {
            return
        }

        val (label, confidence) = classifier.classify(data)

        // Actualizar la UI en tiempo real
        val porcentaje = (confidence * 100).toInt()
        val predictionText = "$label ($porcentaje%)"
        MonitoringState.currentPrediction.value = predictionText
        // Pasar tanto el texto de predicción como el nombre de clase crudo para el gráfico
        MonitoringLogManager.updatePrediction(this, predictionText, label)
        MonitoringLogManager.recordWindow(this)

        // Lista de clases que representan una caída real (índices 1-8)
        val fallClasses = listOf(
            "Caída frontal", "Caída a la derecha", "Caída hacia atrás",
            "Caída contra obstáculo", "Caída (intentando protegerse)", "Caída al sentarse",
            "Desmayo / Síncope", "Caída a la izquierda"
        )

        // Lógica de detección: > 90% de confianza y debe ser estrictamente una clase de caída
        if (label in fallClasses && confidence > 0.90f) {
            MonitoringLogManager.recordFall(this)
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
        val emergencyNumber = intent?.getStringExtra("EMERGENCY_NUMBER") ?: ""
        MonitoringLogManager.startSession(this, emergencyNumber)
        MonitoringState.isMonitoring.value = true

        val notification = NotificationCompat.Builder(this, "fall_channel")
            .setContentTitle("Protección activa")
            .setContentText("Monitoreando actividad en segundo plano")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        // 2. Iniciar el servicio en primer plano con el tipo correcto (obligatorio en Android 14+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(1, notification)
        }
        sensorHandler.start()

        return START_STICKY
    }

    override fun onDestroy() {
        MonitoringLogManager.stopSession(this)
        MonitoringState.isMonitoring.value = false
        MonitoringState.currentPrediction.value = "Inactivo"

        sensorHandler.stop()
        if (classifierReady) {
            classifier.close()
        }
        inferenceExecutor.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}