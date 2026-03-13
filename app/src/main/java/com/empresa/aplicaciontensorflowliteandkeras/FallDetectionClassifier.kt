package com.empresa.aplicaciontensorflowliteandkeras

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.DataType

class FallDetectionClassifier(context: Context) {

    private var interpreter: Interpreter

    // Nombres de las clases según tu documento PDF
    private val classLabels = arrayOf(
        "Caminando",
        "Caída frontal",
        "Caída a la derecha",
        "Caída hacia atrás",
        "Caída contra obstáculo",
        "Caída (intentando protegerse)",
        "Caída al sentarse",
        "Desmayo / Síncope",
        "Caída a la izquierda"
    )
    /*private val classLabels = arrayOf(
        "Walking",
        "Generic falling forward",
        "Falling rightward",
        "Generic falling backward",
        "Hitting an obstacle in the fall",
        "Falling with protection strategies",
        "Falling backward-sitting-chair",
        "Syncope",
        "Falling leftward"
    )*/

    init {
        // Cargar el modelo desde la carpeta assets
        val modelBuffer = FileUtil.loadMappedFile(context, "entrenamiento_9_clases_mejor_modelo.tflite")

        // Optimización para gama baja: Limitar a 2 hilos en CPU
        val options = Interpreter.Options().apply {
            numThreads = 2
        }

        interpreter = Interpreter(modelBuffer, options)
    }

    /**
     * Recibe el arreglo preprocesado de tamaño 453, ejecuta la inferencia
     * y retorna un par con la clase predicha y su probabilidad.
     */
    fun classify(inputData: FloatArray): Pair<String, Float> {
        // Preparar buffer de entrada (1 lote, 453 características)
        val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 453), DataType.FLOAT32)
        inputBuffer.loadArray(inputData)

        // Preparar buffer de salida (1 lote, 9 clases)
        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 9), DataType.FLOAT32)

        // Ejecutar inferencia
        interpreter.run(inputBuffer.buffer, outputBuffer.buffer)

        // Obtener el arreglo de probabilidades
        val probabilities = outputBuffer.floatArray

        // Encontrar el índice con la mayor probabilidad (ArgMax)
        var maxIndex = 0
        var maxProb = probabilities[0]
        for (i in 1 until probabilities.size) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i]
                maxIndex = i
            }
        }

        return Pair(classLabels[maxIndex], maxProb)
    }

    fun close() {
        interpreter.close()
    }
}