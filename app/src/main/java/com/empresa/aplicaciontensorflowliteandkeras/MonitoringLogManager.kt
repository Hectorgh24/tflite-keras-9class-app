package com.empresa.aplicaciontensorflowliteandkeras

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Evento de predicción individual para el gráfico de línea de tiempo.
 * Almacena el segundo en que ocurrió y el nombre de la clase predicha.
 */
data class PredictionEvent(
    val timeSeconds: Int,
    val className: String
)

/**
 * Dato crudo del sensor acelerómetro para el gráfico en tiempo real.
 * Almacena el offset en milisegundos desde el inicio de la sesión.
 */
data class SensorEventData(
    val timeOffsetMillis: Long,
    val x: Float,
    val y: Float,
    val z: Float
)

data class MonitoringSessionLog(
    val sessionStartMillis: Long,
    val sessionEndMillis: Long? = null,
    val windowsProcessed: Int = 0,
    val fallCount: Int = 0,
    val alertsTriggered: Int = 0,
    val emergencyNumber: String = "",
    val currentPrediction: String = "Inactivo",
    val predictionHistory: MutableList<PredictionEvent> = mutableListOf(),
    @Transient val sensorHistory: MutableList<SensorEventData> = mutableListOf()
) {
    val durationSeconds: Long
        get() = if (sessionEndMillis != null) {
            (sessionEndMillis - sessionStartMillis) / 1000
        } else {
            (System.currentTimeMillis() - sessionStartMillis) / 1000
        }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("sessionStartMillis", sessionStartMillis)
            put("sessionStartIso", isoFormat(sessionStartMillis))
            put("sessionEndMillis", sessionEndMillis ?: JSONObject.NULL)
            put("sessionEndIso", sessionEndMillis?.let { isoFormat(it) } ?: JSONObject.NULL)
            put("durationSeconds", durationSeconds)
            put("windowsProcessed", windowsProcessed)
            put("fallCount", fallCount)
            put("alertsTriggered", alertsTriggered)
            put("emergencyNumber", emergencyNumber)
            put("currentPrediction", currentPrediction)

            val historyArray = JSONArray()
            predictionHistory.forEach { event ->
                val eventObj = JSONObject()
                eventObj.put("timeSeconds", event.timeSeconds)
                eventObj.put("className", event.className)
                historyArray.put(eventObj)
            }
            put("predictionHistory", historyArray)
        }
    }

    companion object {
        private fun isoFormat(timestamp: Long): String {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            return formatter.format(Date(timestamp))
        }

        fun fromJson(json: JSONObject): MonitoringSessionLog {
            return MonitoringSessionLog(
                sessionStartMillis = json.optLong("sessionStartMillis"),
                sessionEndMillis = if (json.isNull("sessionEndMillis")) null else json.optLong("sessionEndMillis"),
                windowsProcessed = json.optInt("windowsProcessed"),
                fallCount = json.optInt("fallCount"),
                alertsTriggered = json.optInt("alertsTriggered"),
                emergencyNumber = json.optString("emergencyNumber"),
                currentPrediction = json.optString("currentPrediction", "Inactivo"),
                predictionHistory = mutableListOf<PredictionEvent>().apply {
                    val arr = json.optJSONArray("predictionHistory")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i)
                            if (obj != null) {
                                add(PredictionEvent(obj.optInt("timeSeconds"), obj.optString("className")))
                            }
                        }
                    }
                }
            )
        }
    }
}

object MonitoringLogManager {
    private val _currentSession = MutableStateFlow<MonitoringSessionLog?>(null)
    val currentSession = _currentSession.asStateFlow()

    private const val LOG_FILE_NAME = "monitoring_log.json"
    private const val EXPORT_FILE_NAME = "datos-monitoreo-tensorflow-keras-9-clases.json"

    fun startSession(context: Context, emergencyNumber: String) {
        val session = MonitoringSessionLog(
            sessionStartMillis = System.currentTimeMillis(),
            emergencyNumber = emergencyNumber
        )
        _currentSession.value = session
        // Limpiar historial de gráficos al iniciar nueva sesión
        MonitoringState.predictionHistory.value = emptyList()
        MonitoringState.sensorHistory.value = emptyList()
        saveCurrentSession(context)
    }

    fun recordWindow(context: Context) {
        _currentSession.value?.let {
            _currentSession.value = it.copy(windowsProcessed = it.windowsProcessed + 1)
            saveCurrentSession(context)
        }
    }

    fun recordFall(context: Context) {
        _currentSession.value?.let {
            _currentSession.value = it.copy(fallCount = it.fallCount + 1)
            saveCurrentSession(context)
        }
    }

    /**
     * Registra datos crudos del sensor acelerómetro para el gráfico en tiempo real.
     * Mantiene solo los últimos 500 puntos (~10 segundos a 50Hz) para ahorrar memoria.
     */
    fun recordSensorData(x: Float, y: Float, z: Float) {
        _currentSession.value?.let { session ->
            val offset = System.currentTimeMillis() - session.sessionStartMillis
            session.sensorHistory.add(SensorEventData(offset, x, y, z))
            // Limitar a los últimos 500 puntos (aprox 10 seg a 50Hz)
            if (session.sensorHistory.size > 500) {
                session.sensorHistory.removeAt(0)
            }
            // Publicar snapshot al StateFlow para que Compose lo observe
            MonitoringState.sensorHistory.value = ArrayList(session.sensorHistory)
        }
    }

    fun recordAlert(context: Context) {
        _currentSession.value?.let {
            _currentSession.value = it.copy(alertsTriggered = it.alertsTriggered + 1)
            saveCurrentSession(context)
        }
    }

    /**
     * Actualiza la predicción actual y agrega el evento al historial de predicciones
     * para alimentar el gráfico de línea de tiempo.
     */
    fun updatePrediction(context: Context, prediction: String, className: String) {
        _currentSession.value?.let {
            it.predictionHistory.add(PredictionEvent(it.durationSeconds.toInt(), className))
            _currentSession.value = it.copy(currentPrediction = prediction)
            // Publicar snapshot al StateFlow para que Compose lo observe
            MonitoringState.predictionHistory.value = ArrayList(it.predictionHistory)
            saveCurrentSession(context)
        }
    }

    fun stopSession(context: Context) {
        _currentSession.value?.let {
            _currentSession.value = it.copy(sessionEndMillis = System.currentTimeMillis())
            saveCurrentSession(context)
        }
    }

    fun loadLastSession(context: Context): MonitoringSessionLog? {
        val file = File(context.filesDir, LOG_FILE_NAME)
        return if (file.exists()) {
            try {
                val json = JSONObject(file.readText())
                MonitoringSessionLog.fromJson(json)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun exportReport(context: Context): String? {
        val session = _currentSession.value ?: loadLastSession(context)
        return session?.let {
            val jsonContent = it.toJson().toString(2)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, EXPORT_FILE_NAME)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val itemUri: Uri = resolver.insert(collection, values) ?: return null
                resolver.openOutputStream(itemUri)?.use { outputStream ->
                    outputStream.write(jsonContent.toByteArray())
                } ?: return null

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
                itemUri.toString()
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val exportFile = File(downloadsDir, EXPORT_FILE_NAME)
                exportFile.writeText(jsonContent)
                exportFile.absolutePath
            }
        }
    }

    private fun saveCurrentSession(context: Context) {
        _currentSession.value?.let {
            val file = File(context.filesDir, LOG_FILE_NAME)
            file.writeText(it.toJson().toString(2))
        }
    }
}
