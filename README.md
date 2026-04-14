# 📱 Detector de Caídas - TFLite & Keras (9 Clases)

Este proyecto es una aplicación móvil avanzada para Android desarrollada en **Kotlin** y **Jetpack Compose**. El sistema implementa Inteligencia Artificial en el borde (Edge AI) para la detección proactiva de caídas, analizando datos cinemáticos en tiempo real.

---

## 📍 Índice

1. [🌍 Contexto General](#-contexto-general)
2. [✨ Mejoras Realizadas](#-mejoras-realizadas)
3. [🧠 Recursos de Inteligencia Artificial](#-recursos-de-inteligencia-artificial)
4. [📲 Guía de Instalación y Uso](#-guía-de-instalación-y-uso)
5. [📂 Estructura del Proyecto](#-estructura-del-proyecto)
6. [📖 Glosario Técnico](#-glosario-técnico)

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

* `FallDetectionService.kt`: Gestiona el servicio de primer plano (Foreground Service) que mantiene el monitoreo activo.
* `FallDetectionClassifier.kt`: Núcleo de IA que interactúa con el intérprete de LiteRT.
* `SensorHandler.kt`: Maneja la captura del acelerómetro a una frecuencia estricta de 50Hz.
* `DataPreprocessor.kt`: Clase encargada de la estandarización Z-Score de los datos.
* `EmergencyProtocol.kt`: Contiene la lógica aislada para detonar las alertas externas (SMS/Llamada/WhatsApp).
* `ui/screen/`: Carpeta que contiene `AlertScreen.kt` (UI de alerta), `MonitorScreen.kt` y `AppNavigator.kt` (control central de pantallas).

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
