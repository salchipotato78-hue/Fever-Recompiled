"""Parche mínimo para compilar el motor de Wii Party con el NDK de Android.

Las comprobaciones de X11/Wayland y de audio (PulseAudio/PipeWire/ALSA) son de escritorio Linux.
En Android no aplican, así que se saltan cuando CMake define ANDROID (el toolchain de NDK lo hace).

Uso:  python tools/patch_engine_android.py engine
Es idempotente: si ya está aplicado, no cambia nada.
"""
import sys
from pathlib import Path

# El clon de wiiparty-recomp es la carpeta que se pasa; el motor vive en su subcarpeta engine/.
clone = Path(sys.argv[1] if len(sys.argv) > 1 else "engine")
cmake = clone / "engine" / "CMakeLists.txt"
if not cmake.exists():
    sys.exit(f"[error] no encuentro {cmake}. ¿Es la carpeta del clon de wiiparty-recomp? No se cambió nada.")
text = cmake.read_text(encoding="utf-8")

REPLACEMENTS = [
    ("if(NOT WP_HAVE_X11 AND NOT WP_HAVE_WAYLAND)",
     "if(NOT ANDROID AND NOT WP_HAVE_X11 AND NOT WP_HAVE_WAYLAND)"),
    ("if(NOT WP_HAVE_PULSEAUDIO AND NOT WP_HAVE_PIPEWIRE AND NOT WP_HAVE_ALSA)",
     "if(NOT ANDROID AND NOT WP_HAVE_PULSEAUDIO AND NOT WP_HAVE_PIPEWIRE AND NOT WP_HAVE_ALSA)"),
]
changed = 0
for old, new in REPLACEMENTS:
    if new in text:
        continue
    if text.count(old) != 1:
        sys.exit(f"[error] no encontré una sola coincidencia para: {old}. No se cambió nada.")
    text = text.replace(old, new)
    changed += 1
if changed:
    cmake.write_text(text, encoding="utf-8")
    print(f"Parche aplicado ({changed} cambio(s)) en {cmake}")
else:
    print("Ya estaba aplicado.")
