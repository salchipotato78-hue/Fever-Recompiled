# rhf-port

Port de **Rhythm Heaven Fever** (Wii, NTSC-U, ID `SOME01`) usando el motor de
[wiiparty-recomp](https://github.com/arelkair/wiiparty-recomp), con el objetivo de llevarlo a Android.

> **Estado:** el juego arranca en PC y llega a inicializar el sistema (IOS). Todavía no está verificado
> que se pueda jugar, y el port a Android no ha empezado.

## Aviso legal

- Este repositorio **no contiene** el juego, ni su DOL, ni archivos de Nintendo, ni código traducido.
- Cada persona debe usar su propio disco volcado legalmente.
- No se distribuyen APK con código del juego.

## Qué contiene

| Carpeta | Contenido |
|---|---|
| `games/rhf/` | Descripción del juego para el motor: `game.toml`, `game.cpp`, `CMakeLists.txt` |
| `games/rhf/analysis/` | Se genera localmente con `tools/make_analysis.py` (ver abajo) |
| `tools/` | `make_analysis.py`, `rhf_info.py`, `symbols_to_map.py` |
| `docs/` | `PROGRESS.md` (lo que descubrimos, paso a paso) y `ROADMAP.md` (plan de Android) |
| `archive/driftdroid/` | Intentos previos con DriftDroid (Mario Kart). Ya no es el camino activo |


## Compilar el APK en GitHub Actions

El flujo está en `.github/workflows/android-rhf.yml` y sigue el mismo modelo que WarioRecomp:
el juego no está en el repositorio. Tu repositorio debe ser **privado**, y hay que crear dos secretos
(Settings > Secrets and variables > Actions):

- `RHF_DATA_URL`: enlace directo a un `.zip` con tu carpeta DATA (con `sys/main.dol`), o al `main.dol`.
- `RHF_SYMBOLS_URL`: enlace directo al archivo de símbolos.

Después, en la pestaña **Actions**, ejecuta **Android APK (Rhythm Heaven Fever)**. El flujo verifica el
SHA-256 del DOL, traduce el juego con el motor, guarda la traducción en caché y compila para arm64.
Si la compilación para Android falla, el log queda en el artefacto `build-logs`.

## Cómo usarlo

1. Clona el motor: `git clone https://github.com/arelkair/wiiparty-recomp.git`
2. Copia `games/rhf` de este repositorio dentro de `wiiparty-recomp/games/`.
3. Copia tu carpeta extraída del disco dentro de `games/rhf/extracted/` (con `sys` y `files`).
4. Genera los archivos de análisis con tu archivo de símbolos:
   ```
   python games\rhf\tools\make_analysis.py ruta\al\symbols.txt games\rhf\analysis
   ```
5. Compila y ejecuta desde la raíz de `wiiparty-recomp`:
   ```
   $env:WP_GAME = "rhf"
   python recompiler\recomp.py
   cmake -S . -B build/rhf -G Ninja -DCMAKE_BUILD_TYPE=Release -DWP_GAME=rhf
   cmake --build build/rhf
   .\build\rhf\rhf.exe
   ```

## Créditos

- [wiiparty-recomp](https://github.com/arelkair/wiiparty-recomp): motor de recompilación (GPL-3).
- DriftDroid / WiiCompiled: primer intento, archivado.
- [Wario Land Shake It Android](https://github.com/salchipotato78-hue/Wario-Land-Shake-it-Android): referencia para la capa de Android (controles táctiles, ultrawide).
