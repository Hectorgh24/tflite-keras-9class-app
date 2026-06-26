package com.empresa.aplicaciontensorflowliteandkeras // Qué: Define el paquete del archivo. Para qué: Agrupa lógicamente las clases de la aplicación. Por qué: Mantiene la estructura organizativa requerida por Android.

import android.content.Context // Qué: Importa la clase Context. Para qué: Acceder a recursos de la aplicación. Por qué: Es necesario para acceder a los assets (archivos .tflite).
import android.util.Log // Qué: Importa la clase Log. Para qué: Registrar mensajes en consola (Logcat). Por qué: Crucial para depuración técnica sin detener el hilo principal.
import org.tensorflow.lite.Interpreter // Qué: Importa la clase Interpreter de TFLite. Para qué: Ejecutar la inferencia neuronal. Por qué: Es el núcleo del SDK de TensorFlow Lite para Android.
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer // Qué: Importa TensorBuffer. Para qué: Manejar arreglos n-dimensionales eficientemente. Por qué: Facilita la carga y extracción de memoria nativa.
import org.tensorflow.lite.DataType // Qué: Importa el enumerador DataType. Para qué: Especificar el tipo de dato de los tensores. Por qué: TFLite requiere precisión estricta de tipos.
import java.io.FileInputStream // Qué: Importa FileInputStream. Para qué: Leer el modelo compilado byte a byte. Por qué: Parte del proceso de carga directa del modelo.
import java.nio.MappedByteBuffer // Qué: Importa MappedByteBuffer. Para qué: Mapear el archivo en RAM. Por qué: Permite que el intérprete en C++ acceda directo a la memoria sin copiar datos a JVM.
import java.nio.channels.FileChannel // Qué: Importa FileChannel. Para qué: Manejar los punteros del archivo mapeado. Por qué: Obligatorio en operaciones de memoria de alta eficiencia.

class FallDetectionClassifier(context: Context) { // Qué: Declara la clase principal recibiendo un Contexto. Para qué: Encapsular toda la lógica de TensorFlow. Por qué: Aísla la IA de la UI y del servicio en background.

    private val interpreter: Interpreter // Qué: Declara el intérprete nativo. Para qué: Alojar el motor de IA cargado. Por qué: Su estado debe persistir en memoria.
    private val preprocessor = DataPreprocessor(context) // Qué: Instancia el preprocesador. Para qué: Normalizar entradas. Por qué: Centraliza el DSP antes de inyectar a la red.

    companion object { // Qué: Inicia bloque estático companion object. Para qué: Declarar constantes accesibles globalmente. Por qué: Optimiza memoria evitando instanciar variables por cada ejecución.
        private const val MODEL_PATH = "entrenamiento_9_clases_mejor_modelo.tflite" // Qué: Nombre del archivo de IA. Para qué: Indicar la ruta dentro de los Assets. Por qué: Mantiene flexibilidad en caso de actualizaciones de pesos.
        private const val INPUT_SIZE = 453 // Qué: Constante de 453 variables. Para qué: 151 muestras x 3 ejes. Por qué: Validar la forma del array de entrada antes del desborde de memoria.
        private const val OUTPUT_CLASSES = 9 // Qué: Constante de 9 salidas. Para qué: Tamaño del tensor final. Por qué: Fija la forma del búfer recolector de inferencia.
    } // Qué: Cierra el bloque companion. Para qué: N/A. Por qué: N/A.

    private val classLabels = arrayOf( // Qué: Declara un array inmutable de strings. Para qué: Guardar nombres legibles de cada clase. Por qué: Traduce el índice numérico ganador de la neurona a texto útil para la UI.
        "Caminando", // Qué: Clase 0. Para qué: Mapear índice 0. Por qué: Orden estricto según compilación.
        "Caída frontal", // Qué: Clase 1. Para qué: Mapear índice 1. Por qué: Orden estricto según compilación.
        "Caída a la derecha", // Qué: Clase 2. Para qué: Mapear índice 2. Por qué: Orden estricto según compilación.
        "Caída hacia atrás", // Qué: Clase 3. Para qué: Mapear índice 3. Por qué: Orden estricto según compilación.
        "Caída contra obstáculo", // Qué: Clase 4. Para qué: Mapear índice 4. Por qué: Orden estricto según compilación.
        "Caída (intentando protegerse)", // Qué: Clase 5. Para qué: Mapear índice 5. Por qué: Orden estricto según compilación.
        "Caída al sentarse", // Qué: Clase 6. Para qué: Mapear índice 6. Por qué: Orden estricto según compilación.
        "Desmayo / Síncope", // Qué: Clase 7. Para qué: Mapear índice 7. Por qué: Orden estricto según compilación.
        "Caída a la izquierda" // Qué: Clase 8. Para qué: Mapear índice 8. Por qué: Orden estricto según compilación.
    ) // Qué: Cierra la declaración del array. Para qué: N/A. Por qué: N/A.

    init { // Qué: Bloque de inicialización de la clase. Para qué: Ejecutar código de preparación de la memoria. Por qué: Debe ocurrir tan pronto como se instancie el clasificador en el servicio.
        // Se utiliza el método de lectura nativo en lugar del problemático FileUtil
        val modelBuffer = loadModelFile(context, MODEL_PATH) // Qué: Invoca función loadModelFile. Para qué: Traer el MappedByteBuffer cargado. Por qué: Evita dependencias inestables de TFLite Support.
        val options = Interpreter.Options().apply { // Qué: Crea opciones de compilación. Para qué: Configurar el motor. Por qué: Tuning específico según el dispositivo móvil.
            setNumThreads(2) // Qué: Configura la ejecución multihilo. Para qué: Aprovechar el procesador asignando 2 hilos nativos a la inferencia. Por qué: Equilibra latencia e impacto térmico de batería.
        } // Qué: Cierra apply de Options. Para qué: N/A. Por qué: N/A.
        interpreter = Interpreter(modelBuffer, options) // Qué: Instancia Interpreter pasando matriz de bytes y opciones. Para qué: Inicializa TensorFlow Lite en RAM C++. Por qué: Punto crítico, de fallar lanza excepciones JNI graves.
        Log.d("Classifier", "Modelo TFLite cargado correctamente") // Qué: Inyecta mensaje Log de nivel Debug. Para qué: Constatar el éxito de carga del modelo. Por qué: Confirmación visual para el desarrollador en logcat.
    } // Qué: Cierra el bloque init. Para qué: N/A. Por qué: N/A.

    /**
     * Lee el modelo directamente desde los assets mapeándolo en memoria (Mmap).
     * Esto evita los errores de FileProvider y compresión de la librería de soporte.
     */
    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer { // Qué: Declara función privada devolviendo un MappedByteBuffer. Para qué: Montar el modelo al espacio de memoria sin copiar. Por qué: Enorme optimización de memoria.
        val fileDescriptor = context.assets.openFd(modelPath) // Qué: Adquiere el FileDescriptor nativo. Para qué: Abrir una conexión de lectura al asset compilado. Por qué: Un asset comprimido necesita un descriptor formal para acceder a su puntero de disco.
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor) // Qué: Inicia canal de InputStream. Para qué: Transmitir secuencias de bytes. Por qué: Puente primitivo C-Style en Java para mover información bruta.
        val fileChannel = inputStream.channel // Qué: Deriva el canal desde el InputStream. Para qué: Habilitar manipulación puntero y mapas de memoria. Por qué: Es el único medio compatible para hacer el mmap() de Unix en Android.
        val startOffset = fileDescriptor.startOffset // Qué: Recupera el offset de byte. Para qué: Saber dónde inicia el archivo puro del modelo dentro del comprimido APK. Por qué: Los assets se apilan dentro del empaquetado; este offset ubica exactamente la cabecera.
        val declaredLength = fileDescriptor.declaredLength // Qué: Consigue los bytes declarados de la porción. Para qué: Saber la cantidad exacta de bytes que componen el modelo .tflite. Por qué: Evita desbordamiento al mapear la memoria leyendo demás.
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength) // Qué: Retorna una proyección READ_ONLY. Para qué: Mapear la RAM contra el disco físico. Por qué: Extrema eficiencia, el kernel gestiona los fallos de página transparentemente ahorrando JVM Heap memory.
    } // Qué: Cierra función loadModelFile. Para qué: N/A. Por qué: N/A.

    fun classify(rawData: FloatArray): Pair<String, Float> { // Qué: Declara función pública 'classify'. Para qué: Ser la compuerta externa para inyectar vectores de datos del sensor. Por qué: Retorna un objeto Pair combinando el nombre y el certidumbre, facilitando lectura.
        if (rawData.size != INPUT_SIZE) { // Qué: Condición if. Para qué: Chequear consistencia de la matriz entrante. Por qué: Evitar un fallo fatal por OutOfBounds exception en la capa de C++.
            Log.e("Classifier", "Error de dimensiones. Esperado: $INPUT_SIZE, Recibido: ${rawData.size}") // Qué: Imprime Error Log. Para qué: Notificar rechazo de datos asimétricos. Por qué: Depuración fundamental.
            return Pair("Error de dimensiones", 0f) // Qué: Devuelve fallback de error y ceros flotantes. Para qué: Detener el crasheo de la inferencia prematuramente protegiendo todo el servicio de caídas. Por qué: Manejo defensivo.
        } // Qué: Cierra el condicional de chequeo. Para qué: N/A. Por qué: N/A.

        return try { // Qué: Retorna directamente bloque try-catch. Para qué: Blindar el motor de inferencia nativo. Por qué: Cualquier problema en Tensores o JNI colapsará agresivamente toda la app.
            preprocessor.standardizeInPlace(rawData) // Qué: Llama standardizeInPlace al input. Para qué: Aplicar escalado Z-Score directamente sobre el FloatArray inyectado. Por qué: Optimiza RAM alterando los valores preexistentes (Zero-copy DSP approach).

            val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 151, 3), DataType.FLOAT32) // Qué: Crea un búfer bidimensional de tamaño fijo [1,151,3]. Para qué: Almacenar los datos preprocesados en arquitectura estandarizada. Por qué: TFLite exige que el tipo y el shape concuerde 1:1 con el precompilado FlatBuffer.
            inputBuffer.loadArray(rawData) // Qué: Realiza copia de elementos escalados. Para qué: Migrar de arreglo primitivo Kotlin a puntero nativo JNI en el búfer Tensor. Por qué: La API de interpreter nativo exige búferes compatibles y contiguos en RAM.

            val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, OUTPUT_CLASSES), DataType.FLOAT32) // Qué: Pide reservación para otro búfer de tamaño fijo 1x9. Para qué: Alojar pasivamente las probabilidades devueltas de salida por la neurona. Por qué: Obligatorio declarar memoria pre-asignada porque TFLite Support nunca aloja por su cuenta los resultados.

            interpreter.run(inputBuffer.buffer, outputBuffer.buffer) // Qué: Detona motor. Para qué: Provoca cálculo hacia adelante (Forward Pass). Por qué: Esta orden de código desata las multiplicaciones matemáticas completas de la IA en C++ utilizando la matriz como sumidero y destino a la vez.

            val probabilities = outputBuffer.floatArray // Qué: Convierte respuesta Buffer JNI a FloatArray Kotlin puro. Para qué: Interrogar resultados lógicamente en lenguaje alto nivel. Por qué: TensorBuffer requiere envolturas extraídas para usar iteradores y lógica.
            val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0 // Qué: Operación sobre índices array. Para qué: Aplicar lógica ArgMax y hallar la ubicación donde está la probabilidad flotante más grande de las 9 calculadas. Por qué: Obtiene directamente el Top 1 prediction y usa Elvis operator por seguridad contra listas vacías.
            val confidence = probabilities[maxIndex] // Qué: Extrae probabilidad absoluta mediante indexación ganadora. Para qué: Cuantificar la certeza pura del 0 a 1 flotante sobre esa clase. Por qué: Sin la certeza cruda el modelo ciego podría emitir falsos positivos muy bajos.

            Pair(classLabels[maxIndex], confidence) // Qué: Devuelve par emparejando la etiqueta del arreglo y su porcentaje de confianza flotante. Para qué: Enviar un objeto ordenado e interpelable para usar en condicionales lógicos en FallDetectionService. Por qué: Práctica común e íntegra funcional.

        } catch (e: Exception) { // Qué: Intercepta cualquier colapso imprevisto. Para qué: Aislar catástrofes del motor de IA o excepciones matemáticas sin matar la aplicación. Por qué: Arquitectura segura.
            Log.e("Classifier", "Error crítico durante la inferencia con el modelo TFLite", e) // Qué: Inserta Log.e detallado más StackTrace real. Para qué: Proveer de información fundamental de rastreo al programador. Por qué: Los crasheos JNI no son transparentes en reportes generales.
            Pair("Error", 0f) // Qué: Devuelve tupla Dummy de fallback en ceros netos. Para qué: Proveer algo manejable e inofensivo a quien hizo la consulta original sin alterar el flujo y sin disparar alertas falsas. Por qué: Arquitectura a prueba de balas.
        } // Qué: Fin del bloque try-catch que conforma el return explícito. Para qué: N/A. Por qué: N/A.
    } // Qué: Finaliza la declaración de la función inferencial 'classify'. Para qué: N/A. Por qué: N/A.

    fun close() { // Qué: Declara función para clausura formal de la clase. Para qué: Limpiar basura dejada por el objeto en el ciclo terminal de sistema. Por qué: La limpieza temprana es fundamental para evitar Memory Leaks (Fugas de memoria).
        interpreter.close() // Qué: Aplica el método close sobre el objeto Intérprete TFLite. Para qué: Liberar manualmente todo el espacio mapeado nativo, destruyendo el puente JNI asignado. Por qué: Si esto se olvida, la JVM no sabrá limpiar la RAM nativa y el OS asesinará el servicio por violar políticas de memoria.
    } // Qué: Fin de la declaración fun close. Para qué: N/A. Por qué: N/A.
} // Qué: Conclusión general de la clase. Para qué: N/A. Por qué: N/A.