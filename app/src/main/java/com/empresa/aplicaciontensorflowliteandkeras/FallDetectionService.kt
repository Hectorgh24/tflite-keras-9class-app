package com.empresa.aplicaciontensorflowliteandkeras

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class FallDetectionService : Service() {

    private var classifier: FallDetectionClassifier? = null
    private var preprocessor: DataPreprocessor? = null
    private var sensorHandler: SensorHandler? = null

    private val CHANNEL_ID = "FallDetectionChannel"
    private val NOTIFICATION_ID = 1

    // Gestor de tareas en segundo plano
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var countdownJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Detector de Caídas Activo")
            .setContentText("Monitoreando sensores en segundo plano...")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        MonitoringState.isMonitoring.value = true

        iniciarModeloYSensores()

        return START_STICKY
    }

    private fun iniciarModeloYSensores() {
        try {
            classifier = FallDetectionClassifier(this)
            preprocessor = DataPreprocessor()

            sensorHandler = SensorHandler(this) { rawData ->
                preprocessor?.standardizeInPlace(rawData)
                val result = classifier?.classify(rawData)

                if (result != null) {
                    MonitoringState.currentPrediction.value = "${result.first} (${(result.second * 100).toInt()}%)"

                    if (result.first != "Caminando" && result.second > 0.80f && !MonitoringState.sosActive.value) {
                        dispararAlerta()
                    }
                }
            }
            sensorHandler?.startListening()
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun dispararAlerta() {
        MonitoringState.sosActive.value = true
        MonitoringState.countdown.value = 5 // Reiniciar temporizador

        // 1. Intentar despertar la pantalla (Android lo permite a veces si es una emergencia)
        val alertIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        try { startActivity(alertIntent) } catch (e: Exception) { /* Ignorar si Android lo bloquea */ }

        // 2. Ejecutar la cuenta regresiva independientemente de si la pantalla se abrió o no
        countdownJob?.cancel() // Cancelar cualquier temporizador anterior
        countdownJob = serviceScope.launch {
            while (MonitoringState.countdown.value > 0 && MonitoringState.sosActive.value) {
                delay(1000)
                MonitoringState.countdown.value -= 1
            }

            // Si llegó a cero y no fue cancelado por el usuario
            if (MonitoringState.countdown.value == 0 && MonitoringState.sosActive.value) {
                val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                val emergencyNumber = sharedPrefs.getString("phone", "") ?: ""

                if (emergencyNumber.isNotEmpty()) {
                    // Llamar al protocolo usando el Contexto del Servicio
                    ejecutarProtocoloEmergencia(this@FallDetectionService, emergencyNumber)
                }

                MonitoringState.sosActive.value = false // Reiniciar estado tras emitir alerta
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // Matar las corrutinas activas
        sensorHandler?.stopListening()
        classifier?.close()
        MonitoringState.isMonitoring.value = false
        MonitoringState.currentPrediction.value = "Inactivo"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servicio de Detección",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}