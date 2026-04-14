package com.empresa.aplicaciontensorflowliteandkeras

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class SensorHandler(
    context: Context,
    private val onWindowReady: (FloatArray) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    companion object {
        private const val WINDOW_SIZE = 151
        private const val TOTAL_FEATURES = 453 // 151 * 3
        private const val SAMPLING_PERIOD_US = 20000 // 50Hz (20ms entre muestras)
    }

    // Buffers circulares para los 3 ejes
    private val xBuffer = FloatArray(WINDOW_SIZE)
    private val yBuffer = FloatArray(WINDOW_SIZE)
    private val zBuffer = FloatArray(WINDOW_SIZE)
    private var currentIndex = 0

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SAMPLING_PERIOD_US)
            Log.d("SensorHandler", "Monitoreo iniciado a 50Hz")
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            // Guardar muestras en los buffers respectivos
            xBuffer[currentIndex] = event.values[0]
            yBuffer[currentIndex] = event.values[1]
            zBuffer[currentIndex] = event.values[2]

            currentIndex++

            // Cuando llenamos la ventana de 151 muestras
            if (currentIndex >= WINDOW_SIZE) {
                currentIndex = 0
                val flatBuffer = FloatArray(TOTAL_FEATURES)

                // IMPORTANTE: El modelo espera formato [X...X, Y...Y, Z...Z]
                // debido a la capa Reshape(3, 151) + Permute(2, 1) definida en Python
                System.arraycopy(xBuffer, 0, flatBuffer, 0, WINDOW_SIZE)
                System.arraycopy(yBuffer, 0, flatBuffer, WINDOW_SIZE, WINDOW_SIZE)
                System.arraycopy(zBuffer, 0, flatBuffer, WINDOW_SIZE * 2, WINDOW_SIZE)

                onWindowReady(flatBuffer)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}