import json
import os
import matplotlib.pyplot as plt
import matplotlib.animation as animation
import numpy as np

# Lista de clases en el mismo orden que la aplicación Android (9 clases)
CLASS_LIST = [
    "Caminando",
    "Caída frontal",
    "Caída a la derecha",
    "Caída hacia atrás",
    "Caída contra obstáculo",
    "Caída (intentando protegerse)",
    "Caída al sentarse",
    "Desmayo / Síncope",
    "Caída a la izquierda"
]

def cargar_datos(json_path):
    if not os.path.exists(json_path):
        print(f"Error: No se encontró el archivo '{json_path}'.")
        print("Asegúrate de copiar el JSON exportado de la aplicación en esta misma carpeta.")
        return None
    with open(json_path, 'r', encoding='utf-8') as f:
        return json.load(f)

def generar_video_predicciones(data, output_path="linea_tiempo_monitoreo.mp4"):
    """Genera un video animado de la línea de tiempo de predicciones."""
    history = data.get("predictionHistory", [])
    if not history:
        print("El historial de predicciones está vacío. No hay datos para graficar.")
        return

    duration = data.get("durationSeconds", 30)
    print(f"Generando animación de la línea de tiempo. Duración: {duration}s. Predicciones registradas: {len(history)}")

    # Configuración de la figura de Matplotlib
    fig, ax = plt.subplots(figsize=(10, 5), dpi=100)
    
    # Mapeo de posiciones para el eje Y
    y_positions = list(range(len(CLASS_LIST)))
    
    # Límites iniciales de visualización (ventana deslizante de 60 segundos)
    max_window = 60  # Ventana de tiempo visible igual a la aplicación
    
    # Configurar estilos estéticos (estilo oscuro premium)
    fig.patch.set_facecolor('#1E1E1E')
    ax.set_facecolor('#121212')
    
    ax.set_yticks(y_positions)
    ax.set_yticklabels(CLASS_LIST, color='#E0E0E0', fontsize=9)
    ax.tick_params(colors='#E0E0E0')
    
    # Título y etiquetas
    ax.set_title("Línea de Tiempo de Actividades y Caídas (9 Clases - Reconstrucción)", color='#FFFFFF', fontsize=12, fontweight='bold', pad=15)
    ax.set_xlabel("Tiempo (segundos)", color='#E0E0E0', fontsize=10, labelpad=10)
    ax.grid(True, which='both', color='#2C2C2C', linestyle='--', linewidth=0.5)

    # Listas de puntos a graficar
    x_normal, y_normal = [], []
    x_fall, y_fall = [], []

    # Inicializar scatter plots en el gráfico
    scatter_normal = ax.scatter([], [], color='#00E5FF', s=50, label='Actividades normales', edgecolors='none')
    scatter_fall = ax.scatter([], [], color='#FF1744', s=60, label='Caídas detectadas', edgecolors='none')
    ax.legend(loc='upper right', facecolor='#1E1E1E', edgecolor='#2C2C2C', labelcolor='#E0E0E0')

    # Línea vertical del tiempo actual
    time_line = ax.axvline(x=0, color='#FFC107', linestyle='-', alpha=0.8, linewidth=1.5)

    # Ajustes de bordes estéticos
    for spine in ax.spines.values():
        spine.set_color('#2C2C2C')

    def init():
        scatter_normal.set_offsets(list(zip([], [])))
        scatter_fall.set_offsets(list(zip([], [])))
        time_line.set_xdata([0, 0])
        ax.set_xlim(0, max_window)
        return scatter_normal, scatter_fall, time_line

    def update(frame):
        # frame representa el segundo actual de la simulación (de 0 a duration)
        
        # Filtrar predicciones que ocurrieron hasta el segundo 'frame'
        current_predictions = [p for p in history if p['timeSeconds'] <= frame]
        
        norm_points = []
        fall_points = []
        
        for p in current_predictions:
            t = p['timeSeconds']
            class_name = p['className']
            if class_name in CLASS_LIST:
                y_idx = CLASS_LIST.index(class_name)
                
                # Distinguir entre caídas (índices 1 a 8) y actividades normales (0)
                if y_idx >= 1:
                    fall_points.append((t, y_idx))
                else:
                    norm_points.append((t, y_idx))

        # Actualizar datos de los scatters
        if norm_points:
            scatter_normal.set_offsets(norm_points)
        if fall_points:
            scatter_fall.set_offsets(fall_points)

        # Actualizar línea de tiempo actual
        time_line.set_xdata([frame, frame])

        # Simular comportamiento de la ventana deslizante (scrolling)
        if frame > max_window:
            ax.set_xlim(frame - max_window, frame)
        else:
            ax.set_xlim(0, max_window)

        return scatter_normal, scatter_fall, time_line

    # Crear la animación (1 frame por segundo)
    # duration + 2 segundos para dar holgura al final
    frames_total = int(duration) + 2
    ani = animation.FuncAnimation(
        fig, update, frames=frames_total, init_func=init, blit=False, interval=1000
    )

    _guardar_animacion(ani, output_path)
    plt.close(fig)


def generar_video_acelerometro(data, output_path="acelerometro_monitoreo.mp4"):
    """
    Genera un video animado del gráfico de acelerómetro (ejes X, Y, Z) 
    reconstruyendo exactamente lo que se mostró en la app Android.
    """
    sensor_data = data.get("sensorHistory", [])
    if not sensor_data:
        print("El historial del sensor está vacío. No hay datos para graficar el acelerómetro.")
        return

    duration = data.get("durationSeconds", 30)
    print(f"Generando animación del acelerómetro. Duración: {duration}s. Muestras registradas: {len(sensor_data)}")

    # Extraer arrays de datos
    times_ms = np.array([d["timeOffsetMillis"] for d in sensor_data], dtype=float)
    times_s = times_ms / 1000.0
    x_vals = np.array([d["x"] for d in sensor_data], dtype=float)
    y_vals = np.array([d["y"] for d in sensor_data], dtype=float)
    z_vals = np.array([d["z"] for d in sensor_data], dtype=float)

    # Configuración de la figura (estilo oscuro premium, mismos colores que la app)
    fig, ax = plt.subplots(figsize=(10, 5), dpi=100)
    fig.patch.set_facecolor('#1E1E1E')
    ax.set_facecolor('#121212')

    ax.set_title("Datos del Acelerómetro (Reconstrucción)", color='#FFFFFF', fontsize=12, fontweight='bold', pad=15)
    ax.set_xlabel("Tiempo (segundos)", color='#E0E0E0', fontsize=10, labelpad=10)
    ax.set_ylabel("Aceleración (m/s²)", color='#E0E0E0', fontsize=10, labelpad=10)
    ax.set_ylim(-25, 25)
    ax.tick_params(colors='#E0E0E0')
    ax.grid(True, color='#2C2C2C', linestyle='--', linewidth=0.5)

    for spine in ax.spines.values():
        spine.set_color('#2C2C2C')

    # Colores idénticos a los de la app Android (SensorChart.kt)
    color_x = '#EF5350'  # Rojo
    color_y = '#66BB6A'  # Verde
    color_z = '#42A5F5'  # Azul

    line_x, = ax.plot([], [], color=color_x, linewidth=1.5, label='Eje X')
    line_y, = ax.plot([], [], color=color_y, linewidth=1.5, label='Eje Y')
    line_z, = ax.plot([], [], color=color_z, linewidth=1.5, label='Eje Z')
    ax.legend(loc='upper right', facecolor='#1E1E1E', edgecolor='#2C2C2C', labelcolor='#E0E0E0')

    # Ventana deslizante de ~10 segundos (igual que la app)
    visible_window = 10.0

    def init():
        line_x.set_data([], [])
        line_y.set_data([], [])
        line_z.set_data([], [])
        ax.set_xlim(0, visible_window)
        return line_x, line_y, line_z

    def update(frame):
        # frame = segundo actual
        current_time = float(frame)

        # Filtrar muestras hasta el segundo actual
        mask = times_s <= current_time
        t_vis = times_s[mask]
        x_vis = x_vals[mask]
        y_vis = y_vals[mask]
        z_vis = z_vals[mask]

        # Aplicar ventana deslizante: solo mostrar últimos 10 segundos
        if current_time > visible_window:
            window_mask = t_vis >= (current_time - visible_window)
            t_vis = t_vis[window_mask]
            x_vis = x_vis[window_mask]
            y_vis = y_vis[window_mask]
            z_vis = z_vis[window_mask]
            ax.set_xlim(current_time - visible_window, current_time)
        else:
            ax.set_xlim(0, visible_window)

        line_x.set_data(t_vis, x_vis)
        line_y.set_data(t_vis, y_vis)
        line_z.set_data(t_vis, z_vis)

        return line_x, line_y, line_z

    frames_total = int(duration) + 2
    ani = animation.FuncAnimation(
        fig, update, frames=frames_total, init_func=init, blit=False, interval=1000
    )

    _guardar_animacion(ani, output_path)
    plt.close(fig)


def _guardar_animacion(ani, output_path):
    """Intenta guardar la animación como MP4 (ffmpeg) o GIF (pillow) como fallback."""
    try:
        print(f"Guardando video como MP4 (requiere ffmpeg instalado en el sistema)...")
        ani.save(output_path, writer='ffmpeg', fps=1)
        print(f"¡Éxito! Video guardado en '{output_path}'.")
    except Exception as e:
        print(f"\nNo se pudo compilar a MP4 automáticamente debido a: {e}")
        print("Alternativa: Guardando como GIF animado...")
        try:
            output_gif = output_path.replace(".mp4", ".gif")
            ani.save(output_gif, writer='pillow', fps=1)
            print(f"¡Éxito! Archivo guardado como GIF en '{output_gif}'.")
        except Exception as gif_error:
            print(f"No se pudo guardar como GIF: {gif_error}")
            print("\nConsejo: Para compilar a MP4 en Windows, instala ffmpeg e incorpóralo a tus variables de entorno.")


if __name__ == "__main__":
    # Nombre del archivo JSON por defecto que exporta la app
    json_file = "datos-monitoreo-tensorflow-keras-9-clases.json"
    data = cargar_datos(json_file)
    if data:
        generar_video_predicciones(data)
        generar_video_acelerometro(data)
