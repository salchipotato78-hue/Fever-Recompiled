# Hoja de ruta

## Fase 1: jugable en PC
- [ ] Verificar que el menú responde al teclado.
- [ ] Identificar la siguiente función que se detenga (el motor muestra el nombre en `run_log.txt`).
- [ ] Revisar si el audio (DSP) y el vídeo (GX) se comportan bien.

## Fase 2: Android (con el motor de Wii Party)
- [ ] Compilar el motor con el NDK de Android. Revisar los 12 archivos con código de Windows.
- [ ] Cambiar el renderizador: OpenGL ES en lugar de Direct3D 11 (`gx_render_gl.cpp` como base).
- [ ] Entrada: controles táctiles (referencia: `TouchOverlay.java` del repo de Wario).
- [ ] Ultrawide sin estirar (referencia: el repo de Wario).
- [ ] Icono y nombre de la app.
- [ ] Compilar la APK en GitHub Actions. El juego lo importa el usuario desde su propio disco.

## Notas
- El repositorio de Wario y el motor de Wii Party usan GPL-3. Al reutilizar código, conservar los avisos de autor.

## CI en GitHub Actions
- `.github/workflows/android-rhf.yml` descarga el DOL desde el secreto `RHF_DATA_URL`, verifica su SHA-256,
  traduce el juego con el motor, cachea la traducción y compila para arm64 (NDK 27).
- El juego no está en el repositorio: el código traducido solo existe dentro del runner y en la caché del repositorio privado.
- Al principio fallará: el motor tiene código de Windows (12 archivos) que hay que portar.
  Cada error del log es una tarea concreta de la Fase 2.
