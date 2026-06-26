# Arquitectura y Flujo de Detección de Caídas: TensorFlow Lite & Keras (9 Clases)

Este documento detalla la estructura lógica y el flujo de datos del repositorio `tflite-keras-9class-app`. Este proyecto implementa inferencia de *Machine Learning* nativa utilizando un modelo de Inteligencia Artificial originado en Python (TensorFlow/Keras) y exportado para ejecución en dispositivos móviles.

## 1. El Núcleo de Inferencia: Desde `.keras` hasta `.tflite`
La red neuronal original fue diseñada, entrenada y evaluada en Python, guardando su estado y arquitectura en el formato nativo `.keras` (o H5). Sin embargo, el sistema operativo Android no posee los recursos ni las librerías necesarias para ejecutar este formato pesado en tiempo real.
* **El Puente de Conversión (TFLite):** En el entorno de Python, el modelo `.keras` fue convertido usando la utilidad `tf.lite.TFLiteConverter`. Esto genera un archivo de formato *FlatBuffer* (`model.tflite`). Este formato es extremadamente ligero, reduce la precisión de los pesos (cuantización opcional) y está optimizado específicamente para los procesadores ARM de los teléfonos móviles.
* **Alojamiento Físico:** El archivo `.tflite` resultante se deposita dentro de la carpeta `assets/` de la aplicación Android.
* **Cargador e Intérprete:** El archivo `FallDetectionClassifier.kt` instancia el motor `Interpreter` de `org.tensorflow.lite`. Mediante la técnica de mapeo de memoria en disco (`FileChannel.MapMode.READ_ONLY`), el archivo del modelo se carga en la memoria RAM del teléfono al instante y sin ahogar el recolector de basura (Garbage Collector).

## 2. Flujo de Datos para la Detección de Clases

El ciclo de vida de la detección sigue el patrón arquitectónico de Productor-Consumidor. Para garantizar que la inferencia no muera cuando el usuario bloquea la pantalla, todo el flujo es orquestado por un **Servicio en Segundo Plano** (`FallDetectionService.kt`).

### A. Adquisición (Productor) - `SensorHandler.kt`
1. **Delegación de Hardware:** `SensorHandler` es una clase dedicada exclusivamente a suscribirse y recibir los eventos crudos del Acelerómetro a una velocidad fija de 50Hz (1 lectura cada 20ms).
2. **Buffer y Desplazamiento (Sliding Window):** Los eventos (X, Y, Z) se almacenan continuamente en un `FloatArray` plano. Una vez que este arreglo acumula 151 muestras (3.02 segundos a 50Hz), la ventana se "desliza" borrando las muestras más viejas para dar espacio a las nuevas. Inmediatamente avisa al Servicio Central que hay un bloque listo.

### B. Preparación y Escalado (DSP) - `DataPreprocessor.kt`
1. **Limpieza Matemática Obligatoria:** Los modelos provenientes de `.keras` son muy estrictos matemáticamente. Si en Python se estandarizaron los datos (Media 0, Desviación Estándar 1), en Kotlin se debe hacer exactamente lo mismo.
2. **Z-Score Normalization:** Este módulo toma el buffer crudo entregado por el sensor y aplica este escalado estadístico (Z-score) para que los tensores (datos inyectados) coincidan con la distribución matemática que la Red Neuronal espera, evitando que el gradiente explote o devuelva predicciones basura.

### C. Inferencia (Procesamiento) - `FallDetectionService.kt`
1. **Cola Asíncrona (Executor):** El servicio toma el bloque estandarizado y lo empuja a una cola ejecutada por un `ExecutorService` (un Hilo Secundario aislado). Esto garantiza que la pesada matemática de matrices no congele la Interfaz de Usuario (UI Thread).
2. **Clasificación Nativa (TFLite Interpreter):** Se invoca a `classifier.classify()`. Este método inyecta el tensor preparado hacia el motor interno de C++ de TensorFlow Lite.
3. **Matriz Softmax (9 clases):** TFLite realiza la predicción y escupe un arreglo de 9 posiciones (`FloatArray(9)`). Cada posición contiene la probabilidad (de 0.0 a 1.0) de que la muestra pertenezca a esa clase específica.

### D. Evaluación (El Juez Supremo) - `FallDetectionService.kt`
1. **Extracción del Ganador:** Se busca el índice numérico (0 a 8) que tenga la probabilidad más alta (argmax) y se traduce a su nombre textual (Ej: `2 -> fall_forward`).
2. **Criterio de Alarma:**
   * ¿La etiqueta detectada es catalogada como una "caída" en la lista negra (`FALL_CLASSES`)?
   * ¿La probabilidad matemática (confianza) supera el umbral estricto del **90%** (`FALL_THRESHOLD`)?
   * ¿El sistema NO está reproduciendo ya una alarma (`!MonitoringState.sosActive`)?
3. **Detonación de Emergencia:** Si las tres reglas se cumplen, el servicio altera el estado global, lo que detona un protocolo visual e invoca a la Actividad Principal (`MainActivity`) para lanzar la pantalla roja y el SOS sonoro.

## 3. Gestor de Telemetría: `MonitoringLogManager.kt`
Almacena de manera asíncrona la telemetría en archivos `.json` locales para la posterior auditoría y generación de reportes en Python. Este logger está fuertemente protegido con Mutex (`@Synchronized`) y `CopyOnWriteArrayList` para sobrevivir al agresivo multihilo de la aplicación sin arrojar *ConcurrentModificationException*. Al término de los 120 segundos reglamentarios de la prueba, exporta los datos hacia la carpeta pública de descargas del Android.

## Resumen del Flujo de Ejecución (Pipeline):
1. **Sensor Acelerómetro (50Hz)** -> `SensorHandler.kt`
2. **Buffer Lleno (151 muestras, ~3s)** -> `FallDetectionService.kt`
3. **Estandarización Z-Score** -> `DataPreprocessor.kt`
4. **Envío Asíncrono (Executor Thread)** -> `FallDetectionClassifier.kt`
5. **Interpreter (TFLite mapeado desde .keras)** -> Realiza inferencia matricial
6. **Array Resultante (9 Probs Softmax)** -> `FallDetectionService.kt`
7. **Regla de Negocio (Juez > 0.90 + Clase Caída)** -> Activa Estado de Pánico
8. **Paralelo:** `MonitoringLogManager.kt` vuelca todo a disco (JSON) a 1Hz.
