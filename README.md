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
git clone [https://github.com/hectorgh24/tflite-keras-9class-app.git](https://github.com/hectorgh24/tflite-keras-9class-app.git)
