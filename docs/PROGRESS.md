# Registro de progreso

Orden cronológico de lo que se hizo y qué enseñó cada paso.

1. **Identificación del disco.** `boot.bin`: ID `SOME01`, NTSC-U, título RHYTHM HEAVEN FEVER.
2. **Ejecutable.** `main.dol`: entrada `0x80004050`, secciones de código y datos, bases SDA
   `r13 = 0x80326EC0` y `r2 = 0x80320FE0` (verificadas contra las secciones de símbolos).
3. **Mapa de símbolos.** 22.017 símbolos, 8.103 funciones, todas dentro del código del DOL.
4. **Primer intento con DriftDroid (Mario Kart).** Traducción completa (8.141 funciones) y compilación.
   Bloqueos encontrados, en orden:
   - Entrada fijada a Mario Kart (`0x800060A4`). Corregida a `0x80004050`.
   - Registros de hardware bloqueados por diseño (`0xCC003004`, PI). Se resolvió con memoria compartida.
   - Lectura de DSP bloqueada, después espera infinita sin lecturas: el juego esperaba respuesta de IOS.
   - Conclusión: las emulaciones del runtime estaban registradas en direcciones de Mario Kart.
5. **Cambio de base al motor de Wii Party.** Su motor es genérico y las sustituciones se enlazan por nombre.
   Bloqueos resueltos, en orden:
   - `rfi` sin soporte en funciones de configuración de memoria. Se sustituyeron por funciones que devuelven cero
     (prefijo `HleZero_`). Pasó de 38 a 31 instrucciones sin soporte.
   - Nombres con `\` en el mapa (`__sinit_\ExScene_cpp`) rompían el código C++ generado. Se reemplazó `\` por `_`.
   - `SelectThread` era `OSSelectThread` para el motor. Se renombró, sin añadirlo a las sustituciones
     (el motor la llama como código del juego).
   - Resultado: **el juego abre en PC** (`rhf.exe`) y pasa la inicialización de IOS.
6. **Pendiente en PC.** Verificar que el menú responde al teclado y que el juego avanza sin cerrarse.
7. **Primera ejecución en Android (log del dispositivo, 51 s).** Lo que sí funciona:
   - Carga de datos desde la carpeta interna y lectura de disco (HomeButton, Riq, cellanim, layouts).
   - Audio: DSP y salida a 48 kHz sin frames perdidos.
   - Entrada: ~200 lecturas por segundo.
   - Carga de las capas del título y del menú de selección de archivo (`layout_title`, `layout_fileSelect`).
   Lo que no funciona:
   - GX: **0 draw batches, 0 vértices y 0 copias EFB** durante todo el log. La GPU nunca recibe comandos.
   - La hebra de GPU está al 0 %. El juego suena pero la pantalla no cambia.
   - Hipótesis: el renderizador de Android no está conectado (la ruta de GX sigue siendo la de Windows/D3D11).
     Verificar con `gx_render_gl.cpp` del motor antes de tocar nada más.
8. **Controles táctiles (Android).** `TouchOverlay.java` añade cruceta, A, B y `+`, y los envía al motor como
   eventos de gamepad con `SDLActivity.onNativeKeyDown/Up`. Asumido: el motor lee esos códigos de tecla.
   Pendiente de prueba en el dispositivo.
9. **Segunda ejecución y lectura del motor (commit dddb9ce, clonado desde GitHub).**
   - El log nuevo es igual al anterior: mismas lecturas de disco, el último evento es a los 22 s (`RFL_DB.dat` no existe, -106).
   - `fps` del log cuenta copias XFB de GX (`g_frames++` en `execute_copy`), no VI. Por tanto el juego sí emite comandos GX, unos 60 por segundo.
   - `copy_to_framebuffer` del renderizador GL llama primero a `initialize()`, que registra "OpenGL renderer" y, si falla, "cannot initialise the OpenGL renderer". Ninguna de las dos líneas aparece. Es una contradicción que el log por sí solo no explica.
   - Ramas posibles: (a) el backend de render no es el GL que se espera; (b) `initialize()` no termina de registrarse; (c) la ventana de SDL (`window_thread`) no se crea y `present()` no llega a dibujar.
   - Pendiente: log con `WP_LOG_GX=1` (ya activado en RhfActivity para la siguiente prueba).
10. **Barra de estado y de navegación ocultas.** `RhfActivity` oculta ambas barras al ganar foco (API 30+ con `WindowInsetsController`, API 29 con `SYSTEM_UI_FLAG_IMMERSIVE_STICKY`).
11. **Causa confirmada en el stderr (rhf_stderr):** `cannot create an OpenGL 4.1 context: Could not initialize OpenGL / GLES library`. En Android no hay contexto GL 4.1 core, y sin contexto el renderer no se inicializa. El juego sí dibuja: el log de GX muestra `draws=33 vertices=132` por frame.
12. **Port a GLES (overlay):** `video_sdl.cpp` pide ES 3.2 y luego ES 3.0 en Android. `gx_render_gl.cpp` cambia el encabezado de los shaders a `#version 320 es` (validado con glslang), divide `a_uv[8]` en ocho atributos y desactiva `GL_DEPTH_CLAMP`. Pendiente: el dispositivo debe tener `GL_EXT_blend_func_extended` (doble fuente de blend). Si no lo tiene, el shader TEV no compila y el error aparece en el stderr.
