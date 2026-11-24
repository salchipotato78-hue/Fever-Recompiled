# android/

Proyecto Gradle de la app. Lo que NO está aquí, y lo pone el flujo de CI:

- `app/src/main/jniLibs/arm64-v8a/libmain.so`: el juego compilado (CMake + NDK 27), sin símbolos de depuración.
- `app/src/main/java/org/libsdl/app/`: las clases Java de SDL3, copiadas desde el código fuente que descarga CMake.

## Primer arranque: importar el juego

La app pide un `.zip` con tu carpeta DATA del disco. Puede tener la carpeta `DATA` o solo su contenido,
pero debe incluir `sys/main.dol` y la carpeta `files/`. La app copia los archivos a su carpeta interna y
después arranca el juego. La importación se hace una sola vez.

Para crear el zip: comprime la carpeta `DATA` del disco extraído (la que tiene `sys` y `files` dentro).
