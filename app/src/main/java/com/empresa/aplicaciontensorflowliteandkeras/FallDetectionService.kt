package com.empresa.aplicaciontensorflowliteandkeras

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class FallDetectionService : Service() {

    private lateinit var classifier: FallDetectionClassifier
    private lateinit var sensorHandler: SensorHandler
    private lateinit var inferenceExecutor: ExecutorService
    @Volatile
    private var classifierReady = false

    /** Flag atomico para evitar saturar el executor con tareas de inferencia */
    private val inferenceInProgress = AtomicBoolean(false)

    /** WakeLock parcial para mantener la CPU activa incluso con pantalla apagada */
    private var wakeLock: PowerManager.WakeLock? = null

    /** Temporizador de 2 minutos (120 000 ms) para auto-detener la sesión */
    private var sessionTimer: CountDownTimer? = null

    override fun onCreate() {
        super.onCreate()

        // 1. Crear el canal de notificaciones (OBLIGATORIO para evitar cierres forzados)
        createNotificationChannel()

        // 2. Adquirir WakeLock parcial para que la CPU no se duerma en segundo plano
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "FallDetector::MonitoringWakeLock"
        ).apply {
            // Timeout de seguridad de 3 minutos (180s) por si algo falla
            acquire(3 * 60 * 1000L)
        }

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
            // Solo lanzar inferencia si no hay una en progreso.
            // Si el motor esta ocupado, registrar prediccion duplicada para mantener intervalos exactos de 1s.
            if (inferenceInProgress.compareAndSet(false, true)) {
                inferenceExecutor.execute {
                    try {
                        processInference(windowData)
                    } catch (e: Exception) {
                        Log.e("FallService", "Fallo critico al procesar la ventana de datos del sensor", e)
                    } finally {
                        inferenceInProgress.set(false)
                    }
                }
            } else {
                // Inferencia ocupada: duplicar ultima prediccion para no perder el intervalo de 1s
                MonitoringLogManager.recordDuplicatePrediction(this)
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

                // Usar FLAG_ACTIVITY_SINGLE_TOP para traer la Activity existente al frente
                // sin recrearla, evitando que la navegación se reinicie y saque al usuario
                // de la pantalla de monitoreo
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("FALL_DETECTED", true)
                    putExtra("FALL_TYPE", label)
                }
                startActivity(intent)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val emergencyNumber = intent?.getStringExtra("EMERGENCY_NUMBER") ?: ""
        MonitoringState.isMonitoring.value = true
        MonitoringState.remainingSeconds.value = 125 // 5 segundos de preparación + 120s reales

        val notification = NotificationCompat.Builder(this, "fall_channel")
            .setContentTitle("Protección activa")
            .setContentText("Monitoreando actividad en segundo plano")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(1, notification)
        }
        
        object : CountDownTimer(5_000L, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                MonitoringState.remainingSeconds.value = (millisUntilFinished / 1000).toInt() + 120
            }
            override fun onFinish() {
                MonitoringLogManager.startSession(this@FallDetectionService, emergencyNumber)
                sensorHandler.start()
                startSessionTimer()
            }
        }.start()

        return START_STICKY
    }

    /**
     * Temporizador de sesión: al llegar a 0, detiene el monitoreo automáticamente
     * guardando todos los datos correctamente.
     */
    private fun startSessionTimer() {
        sessionTimer?.cancel()
        sessionTimer = object : CountDownTimer(120_000L, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                MonitoringState.remainingSeconds.value = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                MonitoringState.remainingSeconds.value = 0
                Log.d("FallService", "Temporizador de 2 minutos completado. Auto-deteniendo monitoreo.")
                // Detener el servicio limpiamente (invoca onDestroy que guarda datos)
                stopSelf()
            }
        }.start()
    }

    override fun onDestroy() {
        // Cancelar temporizador si aún está activo
        sessionTimer?.cancel()
        sessionTimer = null

        MonitoringLogManager.stopSession(this)
        MonitoringState.isMonitoring.value = false
        MonitoringState.currentPrediction.value = "Inactivo"

        sensorHandler.stop()
        if (classifierReady) {
            classifier.close()
        }
        inferenceExecutor.shutdownNow()

        // Liberar WakeLock
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}