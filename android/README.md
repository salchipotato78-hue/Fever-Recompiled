# android/

Proyecto Gradle de la app. Lo que NO está aquí, y lo pone el flujo de CI:

- `app/src/main/jniLibs/arm64-v8a/libmain.so`: el juego compilado (CMake + NDK 27).
- `app/src/main/java/org/libsdl/app/`: las clases Java de SDL3, copiadas desde el código fuente
  que descarga CMake.

Compilar la APK a mano requiere ambas cosas. El flujo de GitHub Actions lo hace solo.
