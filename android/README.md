# android/

Proyecto Gradle de la app. Lo que NO está aquí, y lo pone el flujo de CI:

- `app/src/main/jniLibs/arm64-v8a/libmain.so`: el juego compilado (CMake + NDK 27), sin símbolos de depuración.
- `app/src/main/java/org/libsdl/app/`: las clases Java de SDL3, copiadas desde el código fuente que descarga CMake.

## Al abrir la app: elegir el .zip

La primera vez la app pide un `.zip` con tu carpeta DATA del disco. El zip puede tener la carpeta `DATA`
o solo su contenido, pero debe incluir `sys/main.dol` y la carpeta `files/`.
Después de importarlo, la app abre el juego directamente. Para cambiar de datos, borra los datos de la app
(o reinstálala) y vuelve a elegir el zip.

Para crear el zip: comprime la carpeta `DATA` del disco extraído (la que tiene `sys` y `files` dentro).

## Ajustes

Por ahora no hay ajustes en la app. Al importar el zip se escribe `settings.ini` con valores fijos
(1x, texturas HD activas, interpolación apagada, bordes ultrawide negros, mando activo). Las opciones
volverán cuando haya un ultrawide real y buen rendimiento.
