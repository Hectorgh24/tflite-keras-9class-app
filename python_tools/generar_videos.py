import json
import os
import matplotlib.pyplot as plt
import matplotlib.animation as animation
import numpy as np

try:
    import imageio_ffmpeg
    plt.rcParams['animation.ffmpeg_path'] = imageio_ffmpeg.get_ffmpeg_exe()
except ImportError:
    pass

# Lista de clases para el modelo de 9 clases
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
    with open(json_path, 'r', encoding='utf-8') as f:
        return json.load(f)

def generar_video_predicciones(data, output_path):
    history = data.get("predictionHistory", [])
    if not history:
        raise ValueError("El historial de predicciones está vacío.")

    duration = data.get("durationSeconds", 30)
    fig, ax = plt.subplots(figsize=(10, 5), dpi=100)
    y_positions = list(range(len(CLASS_LIST)))
    max_window = 60
    
    fig.patch.set_facecolor('#1E1E1E')
    ax.set_facecolor('#121212')
    
    ax.set_yticks(y_positions)
    ax.set_yticklabels(CLASS_LIST, color='#E0E0E0', fontsize=9)
    ax.tick_params(colors='#E0E0E0')
    
    ax.set_title("Línea de Tiempo de Actividades y Caídas (9 Clases)", color='#FFFFFF', fontsize=12, fontweight='bold', pad=15)
    ax.set_xlabel("Tiempo (segundos)", color='#E0E0E0', fontsize=10, labelpad=10)
    ax.grid(True, which='both', color='#2C2C2C', linestyle='--', linewidth=0.5)

    scatter_normal = ax.scatter([], [], color='#00E5FF', s=50, label='Actividades normales', edgecolors='none')
    scatter_fall = ax.scatter([], [], color='#FF1744', s=60, label='Caídas detectadas', edgecolors='none')
    ax.legend(loc='upper right', facecolor='#1E1E1E', edgecolor='#2C2C2C', labelcolor='#E0E0E0')

    time_line = ax.axvline(x=0, color='#FFC107', linestyle='-', alpha=0.8, linewidth=1.5)

    for spine in ax.spines.values():
        spine.set_color('#2C2C2C')

    def init():
        scatter_normal.set_offsets(list(zip([], [])))
        scatter_fall.set_offsets(list(zip([], [])))
        time_line.set_xdata([0, 0])
        ax.set_xlim(0, max_window)
        return scatter_normal, scatter_fall, time_line

    def update(frame):
        current_predictions = [p for p in history if p['timeSeconds'] <= frame]
        norm_points = []
        fall_points = []
        
        for p in current_predictions:
            t = p['timeSeconds']
            class_name = p['className']
            if class_name in CLASS_LIST:
                y_idx = CLASS_LIST.index(class_name)
                # En 9 clases, los índices >= 1 son caídas
                if y_idx >= 1:
                    fall_points.append((t, y_idx))
                else:
                    norm_points.append((t, y_idx))

        if norm_points:
            scatter_normal.set_offsets(norm_points)
        if fall_points:
            scatter_fall.set_offsets(fall_points)

        time_line.set_xdata([frame, frame])

        if frame > max_window:
            ax.set_xlim(frame - max_window, frame)
        else:
            ax.set_xlim(0, max_window)

        return scatter_normal, scatter_fall, time_line

    frames_total = int(duration) + 2
    ani = animation.FuncAnimation(fig, update, frames=frames_total, init_func=init, blit=False, interval=1000)

    _guardar_animacion(ani, output_path)
    plt.close(fig)

def generar_video_acelerometro(data, output_path):
    sensor_data = data.get("sensorHistory", [])
    if not sensor_data:
        raise ValueError("El historial del sensor está vacío.")

    duration = data.get("durationSeconds", 30)
    times_ms = np.array([d["timeOffsetMillis"] for d in sensor_data], dtype=float)
    times_s = times_ms / 1000.0
    x_vals = np.array([d["x"] for d in sensor_data], dtype=float)
    y_vals = np.array([d["y"] for d in sensor_data], dtype=float)
    z_vals = np.array([d["z"] for d in sensor_data], dtype=float)

    fig, ax = plt.subplots(figsize=(10, 5), dpi=100)
    fig.patch.set_facecolor('#1E1E1E')
    ax.set_facecolor('#121212')

    ax.set_title("Datos del Acelerómetro", color='#FFFFFF', fontsize=12, fontweight='bold', pad=15)
    ax.set_xlabel("Tiempo (segundos)", color='#E0E0E0', fontsize=10, labelpad=10)
    ax.set_ylabel("Aceleración (m/s²)", color='#E0E0E0', fontsize=10, labelpad=10)
    ax.set_ylim(-25, 25)
    ax.tick_params(colors='#E0E0E0')
    ax.grid(True, color='#2C2C2C', linestyle='--', linewidth=0.5)

    for spine in ax.spines.values():
        spine.set_color('#2C2C2C')

    line_x, = ax.plot([], [], color='#EF5350', linewidth=1.5, label='Eje X')
    line_y, = ax.plot([], [], color='#66BB6A', linewidth=1.5, label='Eje Y')
    line_z, = ax.plot([], [], color='#42A5F5', linewidth=1.5, label='Eje Z')
    ax.legend(loc='upper right', facecolor='#1E1E1E', edgecolor='#2C2C2C', labelcolor='#E0E0E0')

    visible_window = 10.0

    def init():
        line_x.set_data([], [])
        line_y.set_data([], [])
        line_z.set_data([], [])
        ax.set_xlim(0, visible_window)
        return line_x, line_y, line_z

    def update(frame):
        current_time = float(frame)
        mask = times_s <= current_time
        t_vis = times_s[mask]
        x_vis = x_vals[mask]
        y_vis = y_vals[mask]
        z_vis = z_vals[mask]

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
    ani = animation.FuncAnimation(fig, update, frames=frames_total, init_func=init, blit=False, interval=1000)

    _guardar_animacion(ani, output_path)
    plt.close(fig)

def _guardar_animacion(ani, output_path):
    try:
        ani.save(output_path, writer='ffmpeg', fps=1)
    except Exception as e1:
        output_gif = output_path.replace(".mp4", ".gif")
        try:
            ani.save(output_gif, writer='pillow', fps=1)
        except Exception as e2:
            raise Exception(f"Fallo MP4: {str(e1)}\n\nFallo GIF: {str(e2)}\n\nAsegurate de que Pillow este instalado.")
