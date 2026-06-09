import os
import sys
import subprocess
import glob
import tkinter as tk
from tkinter import messagebox
import threading

# Habilitar DPI Awareness en Windows para evitar que la interfaz y las fuentes se vean borrosas/pixeleadas
try:
    import ctypes
    ctypes.windll.shcore.SetProcessDpiAwareness(1)
except Exception:
    pass

def check_and_install_dependencies():
    """Verifica si las librerias estan instaladas, de lo contrario las instala automaticamente."""
    required = {'matplotlib', 'numpy', 'Pillow', 'imageio-ffmpeg'}
    missing = []
    
    try:
        import matplotlib
    except ImportError:
        missing.append('matplotlib')
        
    try:
        import numpy
    except ImportError:
        missing.append('numpy')
        
    try:
        import PIL
    except ImportError:
        missing.append('Pillow')
        
    try:
        import imageio_ffmpeg
    except ImportError:
        missing.append('imageio-ffmpeg')
        
    if missing:
        print(f"Faltan dependencias: {missing}. Instalando automaticamente...")
        try:
            subprocess.check_call([sys.executable, '-m', 'pip', 'install', *missing])
            print("Instalacion completada. Reiniciando la herramienta...")
            os.execv(sys.executable, ['python'] + sys.argv)
        except Exception as e:
            # Usar un messagebox rudimentario antes de cargar la app completa si falla
            root = tk.Tk()
            root.withdraw()
            messagebox.showerror("Error Critico", f"No se pudieron instalar las dependencias automaticamente.\nError: {e}\n\nAbre la terminal e instala manualmente: pip install matplotlib numpy")
            sys.exit(1)

# Comprobar e instalar antes de importar nuestra logica
check_and_install_dependencies()

# Ahora podemos importar de forma segura
import generar_videos

# Configurar rutas dinamicamente (rutas relativas a donde esta este archivo)
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
INPUT_DIR = os.path.join(BASE_DIR, "input_json")
OUTPUT_DIR = os.path.join(BASE_DIR, "output_videos")

def setup_folders():
    """Genera en automatico las carpetas necesarias si no existen."""
    os.makedirs(INPUT_DIR, exist_ok=True)
    os.makedirs(OUTPUT_DIR, exist_ok=True)

def procesar_videos():
    setup_folders()
    # Buscar todos los JSON en la carpeta input_json
    json_files = glob.glob(os.path.join(INPUT_DIR, "*.json"))
    
    if len(json_files) == 0:
        messagebox.showwarning("Falta Archivo JSON", f"No se encontro ningun archivo .json en la carpeta:\n\n{INPUT_DIR}\n\nPor favor, pega tu archivo ahi y vuelve a intentarlo.")
        return
    
    if len(json_files) > 1:
        messagebox.showwarning("Demasiados Archivos", f"Hay {len(json_files)} archivos JSON en la carpeta. \n\nPara evitar confusiones, deja SOLO UNO a la vez y borra los demas.")
        return
    
    json_path = json_files[0]
    json_name = os.path.basename(json_path)
    
    btn_generar.config(state=tk.DISABLED)
    lbl_status.config(text=f"Procesando: {json_name}...\nPor favor espera, esto tomara varios minutos.", fg="blue")
    root.update()
    
    # Crear un hilo para que la interfaz de Tkinter no se congele ("No responde")
    def worker():
        try:
            data = generar_videos.cargar_datos(json_path)
            
            video_pred = os.path.join(OUTPUT_DIR, "TensorFlowKeras9_linea_tiempo_monitoreo.mp4")
            video_accel = os.path.join(OUTPUT_DIR, "TensorFlowKeras9_acelerometro_monitoreo.mp4")
            
            # Eliminar versiones viejas si existen para evitar sobreescritura confusa
            if os.path.exists(video_pred): os.remove(video_pred)
            if os.path.exists(video_accel): os.remove(video_accel)
            if os.path.exists(video_pred.replace(".mp4", ".gif")): os.remove(video_pred.replace(".mp4", ".gif"))
            if os.path.exists(video_accel.replace(".mp4", ".gif")): os.remove(video_accel.replace(".mp4", ".gif"))
            
            # Avisar que empezo el primero
            root.after(0, lambda: lbl_status.config(text="Generando video 1/2 (Grafico de clases)...", fg="blue"))
            generar_videos.generar_video_predicciones(data, video_pred)
            
            # Avisar que empezo el segundo
            root.after(0, lambda: lbl_status.config(text="Generando video 2/2 (Acelerometro)...", fg="blue"))
            generar_videos.generar_video_acelerometro(data, video_accel)
            
            root.after(0, lambda: lbl_status.config(text="Exito: Videos generados correctamente.", fg="green"))
            root.after(0, lambda: messagebox.showinfo("Proceso Completado", f"Los videos se han guardado exitosamente en la carpeta:\n\n{OUTPUT_DIR}\n\n(Ambos videos han sido generados)"))
            
        except Exception as e:
            root.after(0, lambda: lbl_status.config(text="Ocurrio un error.", fg="red"))
            root.after(0, lambda: messagebox.showerror("Error en el Proceso", f"Hubo un problema al generar los videos:\n{str(e)}"))
        finally:
            root.after(0, lambda: btn_generar.config(state=tk.NORMAL))

    # Iniciar el hilo en segundo plano
    threading.Thread(target=worker, daemon=True).start()

# --- INICIO DE LA INTERFAZ GRAFICA ---
setup_folders() # Crear carpetas al abrir la app
root = tk.Tk()
root.title("Reconstruccion Visual - Monitoreo")
# Quitar root.geometry("600x400") para permitir auto-ajuste al DPI de la pantalla
root.configure(bg="#f4f4f9")

# Padding externo general para que no este apretado
main_frame = tk.Frame(root, bg="#f4f4f9", padx=40, pady=30)
main_frame.pack(fill=tk.BOTH, expand=True)

# Estilos simples
font_title = ("Arial", 16, "bold")
font_text = ("Arial", 11)

tk.Label(main_frame, text="Herramienta de Videos de Monitoreo (9 Clases)", font=font_title, bg="#f4f4f9", fg="#333").pack(pady=(0, 20))

instrucciones = (
    "Sigue estos pasos:\n\n"
    "1. Ve a la carpeta que se acaba de crear llamada 'input_json'.\n"
    "2. Coloca ahi el archivo JSON exportado de la aplicacion (solo 1).\n"
    "3. Presiona el boton verde de abajo.\n"
    "4. Ve a la carpeta 'output_videos' para ver los resultados."
)
tk.Label(main_frame, text=instrucciones, font=font_text, bg="#f4f4f9", fg="#555", justify="left").pack(padx=20, pady=10)

btn_generar = tk.Button(main_frame, text="🚀 GENERAR VIDEOS", font=("Arial", 14, "bold"), bg="#4CAF50", fg="white", 
                        command=procesar_videos, relief=tk.RAISED, cursor="hand2", padx=20, pady=10)
btn_generar.pack(pady=25)

lbl_status = tk.Label(main_frame, text="Las carpetas input_json y output_videos estan listas.", font=("Arial", 10, "italic"), bg="#f4f4f9", fg="#777")
lbl_status.pack(pady=5)

root.mainloop()
