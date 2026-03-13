package com.empresa.aplicaciontensorflowliteandkeras

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorHandler(
    context: Context,
    private val onWindowReady: (FloatArray) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Arreglo reutilizable para evitar carga en el Garbage Collector
    private val dataWindow = FloatArray(453)
    private var currentIndex = 0

    fun startListening() {
        // Usar SENSOR_DELAY_GAME (~50Hz) suele coincidir con datasets como UniMiB-SHAR
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        // Insertar valores X, Y, Z en el arreglo secuencialmente
        if (currentIndex < 453) {
            dataWindow[currentIndex++] = event.values[0] // X
            dataWindow[currentIndex++] = event.values[1] // Y
            dataWindow[currentIndex++] = event.values[2] // Z
        }

        // Si se llenó la ventana de 151 muestras (453 valores)
        if (currentIndex >= 453) {
            // Disparar callback enviando los datos crudos
            onWindowReady(dataWindow)

            // Reiniciar el índice para comenzar la siguiente ventana
            // Nota: Esto hace ventanas no superpuestas (no-overlapping).
            // Si el modelo entrenó con ventanas superpuestas (ej. 50% overlap),
            // aquí deberíamos desplazar los últimos datos al principio del arreglo.
            currentIndex = 0
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No requiere acción para este modelo
    }
}