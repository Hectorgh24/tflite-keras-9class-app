# Contexto Técnico: Monitoreo y Visualización de Gráficos

## 🤖 Nota para la Inteligencia Artificial (Contexto Inicial)
**Instrucción para la IA lectora:** Este documento ha sido diseñado específicamente para proporcionarte el contexto técnico completo de este proyecto. Si se te solicita redactar un reporte, manual, documentación o explicar el sistema, **debes basarte en este documento**. 
Aquí se explica detalladamente **qué se hizo, por qué se hizo y cómo se hizo**, detallando el flujo cronológico completo: desde que la aplicación Android inicia el monitoreo de caídas mediante sensores, hasta que esos datos se procesan y se convierten externamente en animaciones `.mp4`. 

---

## 1. Flujo Cronológico del Sistema (De Móvil a MP4)
El ciclo de vida de los datos sigue este orden estricto:
1. **Recolección:** La app Android captura los datos del acelerómetro a 50Hz usando un servicio en segundo plano continuo.
2. **Inferencia:** Los datos se procesan en ventanas de 151 muestras mediante el modelo TFLite para detectar si es una caída o una actividad diaria.
3. **Temporizador Estricto:** La sesión dura exactamente 120 segundos. Al llegar a 0, se auto-detiene de manera limpia.
4. **Exportación de JSON:** Se genera un reporte con el historial completo de la sesión (tiempos, aceleraciones brutas y clases predichas) y se guarda en el dispositivo móvil.
5. **Reconstrucción Python:** El JSON se pasa a una computadora, donde un script externo en Python lo lee, genera un entorno gráfico y lo exporta como video `.mp4`.

---

## 2. ¿Qué se hizo, por qué y cómo? (Arquitectura Técnica)

### A. Estabilidad Móvil en Android (Kotlin)
*   **¿Qué se hizo?** Se implementó un servicio en primer plano (`Foreground Service`) asociado a un `PowerManager.WakeLock` parcial y un temporizador `CountDownTimer`.
*   **¿Por qué?** Porque el sistema operativo Android tiende a suspender la CPU (modo Doze) cuando la pantalla se apaga, lo que provocaba que el temporizador de 120 segundos se pausara y recolectara datos de forma infinita.
*   **¿Cómo?** Al iniciar el servicio, se adquiere un WakeLock parcial con un límite de seguridad de 3 minutos. Esto mantiene la CPU activa garantizando que el `CountDownTimer` finalice exactamente en 120 segundos e invoque el método `stopSelf()` para apagar el servicio correctamente.

### B. Rendimiento Gráfico en Jetpack Compose
*   **¿Qué se hizo?** Se separó la recolección de datos en dos flujos: un búfer de visualización limitado (`displaySensorBuffer`) y un historial completo (`fullSensorHistory`), aplicando una limitación de actualizaciones (throttling).
*   **¿Por qué?** El sensor generaba datos a 50Hz, lo que saturaba la interfaz de usuario en Compose con recomposiciones excesivas, congelando (lagueando) los gráficos de la pantalla.
*   **¿Cómo?** Se usaron listas de escritura segura (`CopyOnWriteArrayList`). La gráfica en pantalla solo observa los últimos 300 puntos y se actualiza cada 12 muestras (~4Hz), mientras que en memoria oculta se guarda el 100% de la información para exportarla al final.

### C. Sistema Autónomo de Python y MP4 Nativo
*   **¿Qué se hizo?** Se diseñó un script externo (`python_tools/`) con una interfaz de usuario (`tkinter`) y un instalador automático de FFmpeg portátil.
*   **¿Por qué?** Los usuarios no técnicos fallaban al intentar generar videos `.mp4` porque Matplotlib requiere que FFmpeg esté instalado a nivel global del sistema operativo (Variables de entorno PATH).
*   **¿Cómo?** Se utilizó la librería de Python `imageio-ffmpeg`. Esta librería descarga un binario portátil de FFmpeg invisible al usuario. En el código, se inyectó la ruta de este binario local a `matplotlib.rcParams['animation.ffmpeg_path'] = imageio_ffmpeg.get_ffmpeg_exe()`, forzando a Matplotlib a compilar el MP4 sin dependencias externas del sistema.

### D. Tolerancia a Fallos y Prevención de Crasheos Visuales
*   **¿Qué se hizo?** Se implementaron esquemas de `Fallback` (respaldo) a GIF y corrección de matrices dispersas vacías.
*   **¿Por qué?** Al iniciar un video de predicciones en el segundo "0", Matplotlib se bloqueaba fatalmente (`array is 1-dimensional, but 2 were indexed`) porque no existían datos para graficar. Además, si un sistema no lograba decodificar H.264 para el MP4, el script colapsaba sin generar nada.
*   **¿Cómo?** 
    1.  Si ocurre un error crítico al escribir en `.mp4`, un bloque `try-except` captura el fallo y delega la tarea a `PillowWriter` para generar un `.gif` (Fallback).
    2.  Para el crasheo de datos vacíos, se forzó la inicialización del gráfico inyectando una matriz matemática bidimensional vacía estricta usando NumPy: `np.empty((0, 2))`.

---

## 3. Lógica Específica del Proyecto (9 Clases)
Este documento pertenece al **Proyecto de 9 clases**. 
En la reconstrucción visual (código Python en `generar_videos.py`), el color de los eventos graficados (Cyan para actividades seguras, Rojo para caídas de emergencia) obedece al siguiente índice lógico del modelo de machine learning:
- Actividades normales (Caminando): Índice `0`.
- Caídas reales (Caída frontal, caída hacia atrás, etc.): Índices del `1` al `8`.
- **Condición matemática implementada:** Se marca una anomalía visual si `y_idx >= 1`.
