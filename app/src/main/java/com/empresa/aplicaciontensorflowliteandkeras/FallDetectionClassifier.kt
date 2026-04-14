package com.empresa.aplicaciontensorflowliteandkeras

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.DataType
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class FallDetectionClassifier(context: Context) {

    private val interpreter: Interpreter
    private val preprocessor = DataPreprocessor(context)

    companion object {
        private const val MODEL_PATH = "entrenamiento_9_clases_mejor_modelo.tflite"
        private const val INPUT_SIZE = 453 // 151 muestras x 3 ejes
        private const val OUTPUT_CLASSES = 9
    }

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

    init {
        // Se utiliza el método de lectura nativo en lugar del problemático FileUtil
        val modelBuffer = loadModelFile(context, MODEL_PATH)
        val options = Interpreter.Options().apply {
            setNumThreads(2)
        }
        interpreter = Interpreter(modelBuffer, options)
        Log.d("Classifier", "Modelo TFLite cargado correctamente")
    }

    /**
     * Lee el modelo directamente desde los assets mapeándolo en memoria (Mmap).
     * Esto evita los errores de FileProvider y compresión de la librería de soporte.
     */
    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun classify(rawData: FloatArray): Pair<String, Float> {
        if (rawData.size != INPUT_SIZE) {
            Log.e("Classifier", "Error de dimensiones. Esperado: $INPUT_SIZE, Recibido: ${rawData.size}")
            return Pair("Error de dimensiones", 0f)
        }

        return try {
            preprocessor.standardizeInPlace(rawData)

            val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 151, 3), DataType.FLOAT32)
            inputBuffer.loadArray(rawData)

            val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, OUTPUT_CLASSES), DataType.FLOAT32)

            interpreter.run(inputBuffer.buffer, outputBuffer.buffer)

            val probabilities = outputBuffer.floatArray
            val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
            val confidence = probabilities[maxIndex]

            Pair(classLabels[maxIndex], confidence)

        } catch (e: Exception) {
            Log.e("Classifier", "Error crítico durante la inferencia con el modelo TFLite", e)
            Pair("Error", 0f)
        }
    }

    fun close() {
        interpreter.close()
    }
}