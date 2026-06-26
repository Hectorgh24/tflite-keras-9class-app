package com.empresa.aplicaciontensorflowliteandkeras // Qué: Declaración del paquete al que pertenece la clase. Para qué: Organizar el código fuente. Por qué: Android exige una estructura jerárquica de paquetes única por aplicación.

import android.content.Context // Qué: Importa la interfaz Context de Android. Para qué: Proporcionar acceso al entorno global de la aplicación. Por qué: Es necesario para obtener el servicio de sensores del sistema.
import android.hardware.Sensor // Qué: Importa la clase Sensor. Para qué: Representar un sensor de hardware físico o virtual. Por qué: Permite verificar propiedades y tipo del sensor (como acelerómetro).
import android.hardware.SensorEvent // Qué: Importa la clase SensorEvent. Para qué: Contener los datos en bruto generados por un sensor en un instante específico. Por qué: Obligatorio para leer la aceleración (ejes X, Y, Z) en milisegundos.
import android.hardware.SensorEventListener // Qué: Importa la interfaz SensorEventListener. Para qué: Implementar los métodos que escuchan asíncronamente los eventos de hardware. Por qué: Patrón Observer requerido nativamente por el OS.
import android.hardware.SensorManager // Qué: Importa la clase SensorManager. Para qué: Administrar la suscripción a todos los sensores disponibles en el dispositivo. Por qué: Punto de entrada indispensable del sistema para acceder al hardware móvil.
import android.util.Log // Qué: Importa Log de Android. Para qué: Imprimir eventos de desarrollo en consola de depuración. Por qué: Útil para diagnosticar si el sensor levantó o no.

class SensorHandler( // Qué: Declara la clase principal SensorHandler. Para qué: Aislar toda la gestión del acelerómetro lejos del servicio principal. Por qué: Favorece la arquitectura modular y limpieza del código (Single Responsibility Principle).
    context: Context, // Qué: Inyecta una instancia de contexto de Android. Para qué: Invocar getSystemService nativo en la inicialización de variables. Por qué: Patrón de diseño de inyección de dependencias primitivo.
    private val onWindowReady: (FloatArray) -> Unit // Qué: Inyecta una función Lambda. Para qué: Disparar un Callback a la capa superior cuando se tengan 151 muestras exactas. Por qué: Evita dependencias circulares permitiendo programación funcional y asíncrona.
) : SensorEventListener { // Qué: Declara implementación de la interfaz Listener de Android. Para qué: Forzar a la clase a sobreescribir onSensorChanged y onAccuracyChanged. Por qué: Sin esta firma, el SensorManager de Android no la aceptará.

    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager // Qué: Declara e inicializa val privada inmutable para el administrador de sensores. Para qué: Usar el servicio a nivel de sistema. Por qué: Usa applicationContext deliberadamente para erradicar Memory Leaks si el servicio que lo llama es destruido.
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) // Qué: Declara inicialización de variable de hardware solicitando el acelerómetro default. Para qué: Vincularse al chip físico IMU del teléfono. Por qué: Provee las lecturas gravitacionales crudas necesarias para la neurona.

    companion object { // Qué: Abre bloque de objetos estáticos vinculados a la clase. Para qué: Guardar valores numéricos inmutables de configuración global. Por qué: Optimiza RAM no clonando en instancias múltiples y da legibilidad limpia al algoritmo.
        private const val WINDOW_SIZE = 151 // Qué: Declaración de 151. Para qué: Dimensionar la profundidad de recolección temporal antes de la inferencia (aprox 3 seg). Por qué: El modelo TFLite fue forjado estáticamente esperando exactamente 151 muestras temporales.
        private const val TOTAL_FEATURES = 453 // Qué: Declaración del tope de features multiplicando 151 * 3 ejes espaciales. Para qué: Tamaño estricto del buffer de unificación de datos aplanado. Por qué: Prevención temprana de excepciones por dimensión.
        private const val SAMPLING_PERIOD_US = 20000 // Qué: Determina la velocidad en microsegundos de la suscripción (20 mil). Para qué: Solicitar al OS entregar muestras de hardware exactamente a 50Hz. Por qué: 50Hz es la frecuencia empírica normalizada del dataset crudo de origen (SisFall/MobiFall).
    } // Qué: Finaliza bloque companion estático. Para qué: N/A. Por qué: N/A.

    // Buffers circulares para los 3 ejes
    private val xBuffer = FloatArray(WINDOW_SIZE) // Qué: Reserva un arreglo flotante nativo de tamaño fijo para el vector X. Para qué: Alojar iterativamente la métrica lateral del dispositivo. Por qué: Arreglos puros son muchísimo más rápidos que Listas de Java para memoria primitiva.
    private val yBuffer = FloatArray(WINDOW_SIZE) // Qué: Reserva un arreglo flotante nativo para la verticalidad Y. Para qué: Retener los datos longitudinales. Por qué: Garantiza espacio contiguo en memoria evitando el Garbage Collector de la JVM.
    private val zBuffer = FloatArray(WINDOW_SIZE) // Qué: Reserva un arreglo flotante nativo para el empuje transversal Z. Para qué: Preservar la métrica de profundidad y caídas de frente. Por qué: Eficiencia extrema en ejecución.
    private var currentIndex = 0 // Qué: Declara contador mutante en cero. Para qué: Llevar el conteo exacto de la muestra ingresada actualmente. Por qué: Controla lógicamente la saturación de los búferes y gatilla los eventos a las capas abstractas superiores.

    fun start() { // Qué: Declara función de acción pública. Para qué: Revestir el arranque del hardware. Por qué: Otorga control explícito del consumo de batería y CPU a los servicios superiores al decidir cuándo iniciar realmente.
        accelerometer?.let { // Qué: Aplica el método seguro let sobre la variable de acelerómetro. Para qué: Asegurarse que el hardware existe (teléfono no corrupto). Por qué: Previene NullPointerException fulminante en dispositivos que carezcan del sensor inercial físico.
            sensorManager.registerListener(this, it, SAMPLING_PERIOD_US) // Qué: Suscribe formalmente esta clase (this) al bus de datos del sensor. Para qué: Empezar a disparar onSensorChanged cada 20ms. Por qué: Abre el grifo del ciclo de vida asíncrono de los datos de Android.
            Log.d("SensorHandler", "Monitoreo iniciado a 50Hz") // Qué: Inyecta mensaje Debug en consola. Para qué: Facilitar trazabilidad de inicio exitoso. Por qué: Muy útil para diagnosticar si la etapa de setup se completó bien antes del ciclo vital del hardware.
        } // Qué: Fin del bloque let de validación de seguridad. Para qué: N/A. Por qué: N/A.
    } // Qué: Fin declaración de función de arranque. Para qué: N/A. Por qué: N/A.

    fun stop() { // Qué: Declara función de paro asíncrono pública. Para qué: Cortar el suministro de hardware. Por qué: Imperativo llamarlo en el onDestroy de los Servicios para abortar el monitoreo y no agotar la batería del usuario en background indeseado (zombie listener).
        sensorManager.unregisterListener(this) // Qué: Desregistra globalmente la suscripción. Para qué: Avisar al OS que ya no solicitamos sus ciclos del acelerómetro. Por qué: Cierra la válvula devolviendo la CPU al reposo profundo Doze.
    } // Qué: Fin de función de limpieza. Para qué: N/A. Por qué: N/A.

    override fun onSensorChanged(event: SensorEvent?) { // Qué: Sobreescritura asíncrona del método SensorEventListener. Para qué: Atrapar los llamados emitidos por el OS con nuevas muestras. Por qué: Diseño mandatario, es el núcleo del ciclo vital (se ejecuta idealmente cada 20ms).
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) { // Qué: Condicional if chequeando con safety calls que el evento capturado concuerda con un IMU acelerómetro puro. Para qué: Ignorar eventos fantasma o giroscópicos indeseados. Por qué: Descartar falsos positivos de otros chips ahorra recursos.
            // Guardar muestras en los buffers respectivos
            xBuffer[currentIndex] = event.values[0] // Qué: Incrusta el primer valor físico capturado en el casillero actual de la cubeta de X. Para qué: Acumular historia lateral. Por qué: values[0] es la constante nativa en la matriz del IMU.
            yBuffer[currentIndex] = event.values[1] // Qué: Incrusta el segundo valor del acelerómetro en el arreglo de Y. Para qué: Acumular aceleración en la verticalidad pura de gravedad. Por qué: values[1] por API estándar de Android.
            zBuffer[currentIndex] = event.values[2] // Qué: Incrusta la medida del eje transversal (Z) en el índice. Para qué: Sostener la historia de impactos frontales o espaldas en el móvil. Por qué: Única manera contigua y ordenada de preservar matrices matemáticas.

            // Registrar cada muestra individual para el gráfico en tiempo real
            MonitoringLogManager.recordSensorData(event.values[0], event.values[1], event.values[2]) // Qué: Llama un singleton asíncrono estático forzando guardar un log puro. Para qué: Depositar datos puros sin modificar en la memoria flash de manera constante. Por qué: Permitir reconstrucción del fenómeno IoT para evaluación posterior en los gráficos JSON en PC.

            currentIndex++ // Qué: Incrementa el puntero global en +1 unidad. Para qué: Señalar dónde depositar los siguientes tres flotantes en 20ms más. Por qué: Control estricto de máquina de estados.

            // Cuando llenamos la ventana de 151 muestras
            if (currentIndex >= WINDOW_SIZE) { // Qué: Desata compuerta si el puntero se saturó tocando 151 (borde crítico). Para qué: Consolidar y enviar la ventana llena a inferencia. Por qué: Empaqueta solo tramos consistentes.
                val flatBuffer = FloatArray(TOTAL_FEATURES) // Qué: Instancia el arreglo sumidero aplanado de 453 ranuras exactas. Para qué: Armar el tensor final concatenando los ejes X, Y y Z en fila india. Por qué: Es una optimización drástica versus crear matrices listas multidimensionales de Kotlin lentas de instanciar cada segundo.

                // IMPORTANTE: El modelo espera formato [X...X, Y...Y, Z...Z]
                // debido a la capa Reshape(3, 151) + Permute(2, 1) definida en Python
                System.arraycopy(xBuffer, 0, flatBuffer, 0, WINDOW_SIZE) // Qué: Pega masivamente en C-style todo el arreglo X (pos:0-150) sobre el buffer vacío al principio absoluto. Para qué: Clonación de arreglos instantánea. Por qué: Extremadamente rápido, elude bucles 'for' costosos en CPU.
                System.arraycopy(yBuffer, 0, flatBuffer, WINDOW_SIZE, WINDOW_SIZE) // Qué: Pega el bloque de aceleraciones Y (151 al 301) justito después del X recién pegado. Para qué: Estructurar correlación matemática de variables 1D. Por qué: Concatenación.
                System.arraycopy(zBuffer, 0, flatBuffer, WINDOW_SIZE * 2, WINDOW_SIZE) // Qué: Pega el tramo final Z (302 a 452). Para qué: Culminar el vector tensor 1D. Por qué: TFLite demanda memoria continua e inmutable plana.

                onWindowReady(flatBuffer) // Qué: Ejecuta asíncronamente el método Callback inyectado, escupiendo el FloatArray gigante relleno. Para qué: Liberar este bloque de responsabilidades y permitir a la IA atrapar el array e iniciar su magia de TFLite. Por qué: Mantiene arquitectura funcional orientada a eventos.

                // Desplazamiento (Sliding Window): eliminar las 50 muestras más antiguas (1 segundo a 50Hz)
                val shiftAmount = 50 // Qué: Declara valor de pérdida (50 frames / 1s). Para qué: Aplicar traslape y ventana deslizante sin perder todo lo recolectado. Por qué: Generar ventanas semi-nuevas cada segundo continuo en lugar de esperar 3 segundos para una predicción limpia incrementa resiliencia.
                val remaining = WINDOW_SIZE - shiftAmount // Qué: Deduce y graba la cifra de muestras que sobreviven (101). Para qué: Saber cuánto pegar. Por qué: Evita calcularlo tres veces gastando microciclos.
                System.arraycopy(xBuffer, shiftAmount, xBuffer, 0, remaining) // Qué: Machaca el propio arreglo X recorriendo todo a la izquierda borrando el inicio. Para qué: Avanzar el búfer circular sin instanciar basura. Por qué: Manipulación destructiva in-place de alta velocidad.
                System.arraycopy(yBuffer, shiftAmount, yBuffer, 0, remaining) // Qué: Recorre Y a la izquierda descartando lo arcaico y empujando las de enmedio al principio. Para qué: Traslape circular sobre el arreglo nativo original. Por qué: Eficiencia total de O(N).
                System.arraycopy(zBuffer, shiftAmount, zBuffer, 0, remaining) // Qué: Desliza eje Z hacia cero. Para qué: Preparar la cabecera para continuar. Por qué: Optimización para no alocar ni un byte de RAM nuevo durante la sobreescritura.
                
                currentIndex = remaining // Qué: Sobreescribe el contador general reduciéndolo de 151 a 101. Para qué: Marcar que el índice inicial disponible para escribir está en la muestra 101 en los arreglos. Por qué: Deja el sistema listo para requerir solo 50 muestras más antes de explotar la inferencia por siguiente ocasión.
            } // Qué: Final de bloque iterativo y condicionante de disparo inferencial. Para qué: N/A. Por qué: N/A.
        } // Qué: Final de condicionamiento de revisión cruzada de sensor idóneo. Para qué: N/A. Por qué: N/A.
    } // Qué: Cierre de sobreescritura primaria onSensorChanged. Para qué: N/A. Por qué: N/A.

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {} // Qué: Interfaz obligatoria forzosa inyectando un bloque vacío inútil. Para qué: Evitar compilación fallida del código Kotlin general. Por qué: El SDK de Android exige definirla al usar SensorEventListener independientemente de usarla o no (lo cual en acelerómetros es raro medir la calibración del error frente a campos magnéticos erráticos por ejemplo).
} // Qué: Fin estructural de la clase SensorHandler principal. Para qué: N/A. Por qué: N/A.