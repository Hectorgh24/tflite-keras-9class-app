# 📱 Detector de Caídas - TFLite & Keras (9 Clases)

Este proyecto es una aplicación móvil avanzada para Android desarrollada en **Kotlin** y **Jetpack Compose**. El sistema implementa Inteligencia Artificial en el borde (Edge AI) para la detección proactiva de caídas, analizando datos cinemáticos en tiempo real.

---

## 📍 Índice

1. [🌍 Contexto General](#-contexto-general)
2. [✨ Mejoras Realizadas](#-mejoras-realizadas)
3. [🧠 Recursos de Inteligencia Artificial](#-recursos-de-inteligencia-artificial)
4. [📲 Guía de Instalación y Uso](#-guía-de-instalación-y-uso)
5. [📂 Estructura del Proyecto](#-estructura-del-proyecto)
6. [📊 Sistema de Monitoreo y Gráficos](#-sistema-de-monitoreo-y-gráficos)
7. [📖 Glosario Técnico](#-glosario-técnico)
8. [🚀 Funcionalidades](#-funcionalidades)
9. [⚡ Optimizaciones de Rendimiento y Estabilidad](#-optimizaciones-de-rendimiento-y-estabilidad)
10. [🐍 Script Python de Reconstrucción](#-script-python-de-reconstrucción)

---

## 🌍 Contexto General

La aplicación ha sido diseñada para ofrecer una capa de seguridad a usuarios vulnerables mediante el uso de redes neuronales convolucionales (Conv1D). El software monitorea el acelerómetro del dispositivo para identificar patrones de movimiento característicos de una caída. Al detectar un evento crítico, se inicia una cuenta regresiva visual y sonora que, de no ser cancelada por el usuario, activa un protocolo de emergencia que envía mensajes de texto (SMS), WhatsApp y realiza una llamada telefónica automática al contacto configurado.

---

## ✨ Mejoras Realizadas

Durante el ciclo de desarrollo se han implementado soluciones técnicas para superar obstáculos de compatibilidad y sistema:
* **Migración a Kotlin 2.0:** El proyecto utiliza las últimas especificaciones del compilador de Kotlin para un rendimiento óptimo en Jetpack Compose.
* **Implementación de LiteRT:** Se actualizó el motor de inferencia de TFLite a **LiteRT** para soportar el opcode 12 (`FULLY_CONNECTED`), permitiendo ejecutar modelos entrenados en versiones modernas de TensorFlow (2.17+).
* **Optimización de Memoria:** Carga del modelo mediante `MappedByteBuffer` para evitar la descompresión innecesaria y fallos de lectura en los recursos del APK.
* **Interfaz de Alerta Avanzada:** Inclusión de retroalimentación sonora con `ToneGenerator` y lógica de cuenta regresiva reactiva integrada en el flujo de navegación global.

---

## 🧠 Recursos de Inteligencia Artificial

La lógica de predicción se basa en dos archivos fundamentales ubicados en `app/src/main/assets`:

1.  **`entrenamiento_9_clases_mejor_modelo.tflite`**: El modelo de red neuronal exportado que clasifica 9 tipos de actividades humanas basándose en ventanas temporales de aceleración.
2.  **`scaler_9_clases.json`**: Contiene los parámetros de media (`mean`) y desviación estándar (`scale`) del `StandardScaler` de Python. Estos valores son inyectados en la aplicación para normalizar los datos del sensor antes de la inferencia, garantizando que el modelo reciba datos en la escala correcta.

---

## 📲 Guía de Instalación y Uso

### Clonación
```bash
git clone https://github.com/Hectorgh24/tflite-keras-9class-app.git
```

---

## 📂 Estructura del Proyecto
Archivos clave dentro de `app/src/main/java/com/empresa/aplicaciontensorflowliteandkeras/`:

* `FallDetectionService.kt`: Gestiona el servicio de primer plano con `ExecutorService` para inferencia en segundo plano segura.
* `FallDetectionClassifier.kt`: Núcleo de IA que interactúa con el intérprete de LiteRT.
* `SensorHandler.kt`: Maneja la captura del acelerómetro a 50Hz y publica cada muestra al historial para los gráficos.
* `DataPreprocessor.kt`: Clase encargada de la estandarización Z-Score de los datos.
* `EmergencyProtocol.kt`: Contiene la lógica aislada para detonar las alertas externas (SMS/Llamada/WhatsApp).
* `MonitoringState.kt`: Estado global reactivo (StateFlow) de la sesión, incluyendo historial de predicciones y sensor.
* `MonitoringLogManager.kt`: Gestiona el ciclo de vida de la sesión de monitoreo, el historial de gráficos y la exportación JSON.
* `ui/screen/`: Pantallas principales: `AlertScreen.kt`, `MonitorScreen.kt`, `MainScreen.kt`, `SettingsScreen.kt` y `AppNavigator.kt`.
* `ui/screen/charts/SensorChart.kt`: Gráfico de línea en tiempo real para los datos del acelerómetro (ejes X, Y, Z).
* `ui/screen/charts/TimelineChart.kt`: Gráfico de dispersión del historial de predicciones de las 9 clases a lo largo del tiempo.

---

## 📊 Sistema de Monitoreo y Gráficos

La pantalla **Ajustes** incluye un panel de monitoreo en tiempo real que se activa durante (y después de) una sesión. Contiene dos gráficos implementados con `Canvas` nativo de Jetpack Compose:

### 📈 Gráfico de Acelerómetro (`SensorChart`)
Muestra los datos crudos del acelerómetro en tiempo real, con una línea para cada eje:
- **Eje X** — Rojo
- **Eje Y** — Verde
- **Eje Z** — Azul

Características técnicas:
- Ventana deslizante de los **últimos 500 puntos** (~10 segundos a 50Hz) para no consumir memoria ilimitada.
- Rango fijo del eje Y de **-25 a +25 m/s²**.
- Cuadrícula automática cada 2 segundos en el eje X y cada 10 m/s² en el eje Y.

### 🗓️ Gráfico de Línea de Tiempo (`TimelineChart`)
Muestra el historial completo de predicciones del modelo como un **diagrama de dispersión**:
- El **eje Y** representa las 9 clases del modelo.
- El **eje X** representa el tiempo transcurrido en segundos desde el inicio de la sesión.
- Los puntos de **caída** (clases 1–8) se dibujan en **rojo**.
- La actividad **normal** (clase 0: Caminando) se dibuja en **azul**.
- Soporta **scroll horizontal** y ventana de visualización de 60 segundos para evitar desbordamiento de textura en Android.

### 🗂️ Clases del modelo (9 clases)
| Índice | Clase | Tipo |
|--------|-------|------|
| 0 | Caminando | Normal |
| 1 | Caída frontal | Caída |
| 2 | Caída a la derecha | Caída |
| 3 | Caída hacia atrás | Caída |
| 4 | Caída contra obstáculo | Caída |
| 5 | Caída (intentando protegerse) | Caída |
| 6 | Caída al sentarse | Caída |
| 7 | Desmayo / Síncope | Caída |
| 8 | Caída a la izquierda | Caída |

### 📁 Exportación de Sesión
Desde el panel de ajustes se puede generar un **reporte JSON** con la información completa de la sesión:
- Timestamps de inicio y fin (ISO 8601)
- Duración total en segundos
- Ventanas de sensor procesadas
- Número de caídas detectadas y alertas enviadas
- **Historial completo de predicciones** (segundo + clase predicha)

El archivo se guarda con el nombre `datos-monitoreo-tensorflow-keras-9-clases.json` en la carpeta **Descargas** del dispositivo.

---

## 📖 Glosario Técnico
* `50Hz`: El sensor registra 50 muestras de aceleración por cada segundo (una muestra cada 20 milisegundos).
* `Conv1D`: Red Neuronal Convolucional Unidimensional, ideal para analizar secuencias de series de tiempo como las del acelerómetro.
* `Inferencia`: El proceso de ejecución del modelo de IA sobre nuevos datos para obtener una predicción.
* `LiteRT`: Tecnología de Google que permite ejecutar modelos de ML en dispositivos móviles con baja latencia.
* `m/s² vs G`: La aplicación convierte las lecturas nativas de Android (metros por segundo al cuadrado) a unidades de Gravedad (G) para coincidir con el entrenamiento del dataset.
* `StandardScaler`: Método de normalización que resta la media y divide por la desviación estándar de los datos de entrenamiento.
* `Z-Score`: El resultado de la estandarización que indica cuántas desviaciones estándar está un dato lejos de la media.

---

## 🚀 Funcionalidades

La aplicación móvil ofrece las siguientes funcionalidades principales:

- **Detección de caídas en tiempo real**: Utiliza el acelerómetro del dispositivo para monitorear movimientos y detectar patrones característicos de caídas mediante un modelo de inteligencia artificial entrenado con 9 clases de actividades.
- **Monitoreo continuo**: Ejecuta un servicio en primer plano que mantiene el análisis activo incluso cuando la app está en segundo plano, registrando datos a 50Hz.
- **Clasificación de actividades**: El modelo clasifica 9 tipos diferentes de movimientos humanos, incluyendo caídas, para una detección precisa.
- **Normalización de datos**: Aplica estandarización Z-Score usando parámetros de media y desviación estándar del entrenamiento para preparar los datos antes de la inferencia.
- **Interfaz de usuario intuitiva**: Incluye pantallas para iniciar/detener monitoreo, configurar número de emergencia y visualizar alertas.
- **Protocolo de emergencia**: Al detectar una caída, inicia una cuenta regresiva con alertas sonoras; si no se cancela, envía SMS, mensajes de WhatsApp y realiza una llamada automática al contacto configurado.
- **Almacenamiento de logs**: Registra sesiones de monitoreo en archivos JSON, incluyendo métricas como ventanas analizadas, caídas detectadas y alertas activadas.
- **Generación de reportes**: Permite obtener reportes de la actividad de monitoreo desde la pantalla de configuración.

---

## ⚡ Optimizaciones de Rendimiento y Estabilidad

Mejoras implementadas para garantizar una operación confiable del servicio de monitoreo en segundo plano y una experiencia de usuario fluida:

### Temporizador de Sesión de 2 Minutos
- Cada sesión de monitoreo dura exactamente **120 segundos** y se auto-detiene al finalizar.
- Se muestra un temporizador visual en la pantalla de monitoreo que cambia a rojo en los últimos 10 segundos.
- Implementado con `CountDownTimer` que actualiza el estado reactivo `MonitoringState.remainingSeconds`.

### WakeLock Parcial para Ejecución en Segundo Plano
- Se adquiere un `PARTIAL_WAKE_LOCK` al iniciar el servicio para mantener la CPU activa incluso con la pantalla apagada.
- Timeout de seguridad de 3 minutos para evitar fugas de recursos.
- Se agregó el permiso `WAKE_LOCK` al `AndroidManifest.xml`.

### Optimización de Gráficos en Tiempo Real y UX
- Se separaron los datos del sensor en dos buffers:
  - `fullSensorHistory`: almacena **todas** las muestras para la exportación JSON completa.
  - `displaySensorBuffer`: buffer circular de 500 puntos para el gráfico en pantalla.
- Se implementó **throttle de publicación**: el `StateFlow` del sensor solo se actualiza cada 12 muestras (~4Hz visual), evitando recomposiciones excesivas de Compose que causaban congelamiento de gráficos.
- Se utiliza `CopyOnWriteArrayList` para evitar `ConcurrentModificationException` desde el hilo del sensor.
- Se implementó **scroll horizontal interactivo** en ambos gráficos (`SensorChart` y `TimelineChart`), asignando un ancho dinámico proporcional a la duración (40dp por segundo) para evitar la superposición de etiquetas.
- El gráfico de línea de tiempo ahora visualiza el historial completo de los **120 segundos** de la sesión (antes limitado a 60s).
- Se añadió **auto-scroll en tiempo real** para seguir el flujo de datos y líneas de cuadrícula ajustadas a intervalos precisos de **1 segundo**.

### Ventana Deslizante (Sliding Window) para Inferencia Continua
- En el `SensorHandler`, en lugar de vaciar completamente el búfer tras cada inferencia (151 muestras), ahora se aplica un desplazamiento temporal: solo se descartan las 50 muestras más antiguas (equivalente a 1 segundo) y se retienen las 101 más recientes. Esto genera ventanas de datos superpuestas, eliminando los "puntos ciegos" temporales y aumentando la precisión en la detección de caídas a lo largo del tiempo.

### Exportación Completa de Datos del Acelerómetro
- El reporte JSON ahora incluye el campo `sensorHistory` con **todos** los datos brutos del acelerómetro (offset en ms, ejes X/Y/Z).
- Esto permite reconstruir gráficos exactos en Python usando la nueva herramienta gráfica.

### Corrección de Navegación en Alertas de Caída
- Se cambió `FLAG_ACTIVITY_CLEAR_TOP` por `FLAG_ACTIVITY_SINGLE_TOP` en el intent de alerta.
- Se agregó `android:launchMode="singleTop"` en el `AndroidManifest.xml`.
- Se implementó `onNewIntent()` en `MainActivity` para manejar intents sin recrear la Activity.
- Esto evita que la navegación se reinicie y saque al usuario de la pantalla de monitoreo cuando se detecta una caída.

### Notificación Persistente
- Se agregó `.setOngoing(true)` a la notificación del servicio para evitar que el usuario la descarte accidentalmente.

---

## 🐍 Herramienta Python de Reconstrucción Visual (JSON a MP4)

Se diseñó un módulo externo de Python (ubicado en la carpeta `python_tools/`) para leer el JSON exportado y generar animaciones precisas.

### Características Técnicas de Generación de Video
- **MP4 Nativo sin dependencias de sistema:** La herramienta utiliza el paquete `imageio-ffmpeg` para descargar un binario portátil de FFmpeg interno en Python. Al vincularlo con `matplotlib.rcParams['animation.ffmpeg_path']`, el usuario no necesita instalar FFmpeg manualmente en Windows ni tocar sus variables de entorno.
- **Tolerancia a fallos (Fallback a GIF):** Si el motor principal falla al intentar generar el archivo `.mp4`, el sistema captura la excepción silenciosamente e invoca `Pillow` para generar una animación `.gif` de respaldo con el cálculo correcto de FPS.
- **Prevención de Bugs Gráficos:** Para sortear crasheos de Matplotlib (`too many indices for array`) al iniciar el scatter plot en el segundo cero, se inyectan matrices vacías matemáticas mediante `np.empty((0, 2))`.
- **Interfaz Fluida (Multi-Hilo) y Alta Resolución:** Se habilitó *DPI Awareness* en Windows para evitar textos borrosos. La generación de animaciones (que puede demorar varios minutos) fue aislada en un hilo secundario para evitar congelamientos ("No Responde") en la ventana de Tkinter, proveyendo retroalimentación textual secuencial durante el progreso.

### Instalación y Uso Automático
Esta herramienta contiene un auto-instalador: revisa e instala internamente las dependencias faltantes (`matplotlib`, `numpy`, `Pillow`, `imageio-ffmpeg`) sin que tengas que usar `pip` de forma manual.

1. Entra a la carpeta `python_tools/` que está en la raíz del proyecto.
2. Ejecuta la interfaz gráfica haciendo doble clic sobre el archivo o usando la terminal:
   ```bash
   python interfaz_grafica.py
   ```
3. La interfaz creará automáticamente dos carpetas: `input_json` y `output_videos`.
4. Coloca **solo tu archivo JSON** (ej. `datos-monitoreo-tensorflow-keras-9-clases.json`) en la carpeta `input_json/`.
5. Presiona el botón verde "Generar Videos" en la interfaz.
6. Los videos MP4 generados (`linea_tiempo_monitoreo.mp4` y `acelerometro_monitoreo.mp4`) aparecerán en la carpeta `output_videos/`.

---

Autor: Hector (Licenciatura en Tecnologías Computacionales)  
Última actualización: Junio 2026


## 🔬 Integración con Orquestador Multimodelo (Actualización)
Esta aplicación fue modificada para operar simultáneamente con otros 3 modelos de Inteligencia Artificial en un solo dispositivo (Poco F7) durante protocolos de investigación científica.

### Mejoras Críticas Implementadas:
1. **Restauración y START_STICKY (DummyForegroundService)**: Se inyectó y configuró el servicio con notificaciones de alta prioridad para garantizar visibilidad, y se activó el retorno START_STICKY para evitar bloqueos.
2. **Permiso de Notificaciones en Tiempo de Ejecución**: Inyección del permiso POST_NOTIFICATIONS en el manifiesto, solucionando bloqueos al instanciar el servicio en Android 13+.
3. **Inyección Dinámica de Ciclo de Vida**: El MainActivity.kt fue escaneado e intervenido dinámicamente para insertar el hook de startForegroundService en el onResume, logrando bypass completo de la restricción del acelerómetro.
4. **Sincronización UDP**: Integración final para escuchar comandos UDP y asegurar que el log de JSON comience y termine coordinado con el orquestador general y la grabación de la cámara.

### ⏱️ Rendimiento de Generación de Videos (Aceleración AMF)
Durante las pruebas de campo en un equipo HP Victus (AMD Radeon RX 6550M), el renderizado de gráficos de la telemetría tardó lo siguiente:
* **Video de Línea de Tiempo (Predicciones)**: ~5 minutos (24.11 MB)
* **Video de Acelerómetro (Ejes X,Y,Z)**: ~4 minutos (35.73 MB)
* **Tiempo Total por Ciclo (120s)**: ~9 minutos.
> Nota: Al ser una arquitectura TFLite de 9 clases extremadamente optimizada, el dibujado de sus datos en Matplotlib fue significativamente más veloz que en las arquitecturas de 17 clases.
