# android/

Proyecto Gradle de la app. Lo que NO está aquí, y lo pone el flujo de CI:

- `app/src/main/jniLibs/arm64-v8a/libmain.so`: el juego compilado (CMake + NDK 27), sin símbolos de depuración.
- `app/src/main/java/org/libsdl/app/`: las clases Java de SDL3, copiadas desde el código fuente que descarga CMake.

## Al abrir la app: elegir el .zip

La app siempre empieza pidiendo un `.zip` con tu carpeta DATA del disco; no se puede saltar esa pantalla.
El zip puede tener la carpeta `DATA` o solo su contenido, pero debe incluir `sys/main.dol` y la carpeta `files/`.
Si es el mismo zip que ya se importó (mismo nombre y tamaño), no se vuelve a extraer y el juego abre enseguida.

Para crear el zip: comprime la carpeta `DATA` del disco extraído (la que tiene `sys` y `files` dentro).

## Ajustes dentro del juego

Engranaje arriba a la derecha (o el botón Atrás): resolución, interpolación de FPS, bordes ultrawide
(difuminados o negros), texturas HD y mando. Cada cambio se guarda en `settings.ini` al momento.
