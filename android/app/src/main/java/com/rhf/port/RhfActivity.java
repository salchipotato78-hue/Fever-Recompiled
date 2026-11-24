package com.rhf.port;

import org.libsdl.app.SDLActivity;

/**
 * Actividad del juego. SDL3 carga la librería nativa "main" (libmain.so, generada por el
 * flujo de CI) y ejecuta su función exportada SDL_main (engine/src/app/main.cpp).
 *
 * Las clases org.libsdl.app.* no están en este repositorio: el flujo de CI las copia desde
 * el código fuente de SDL3 que descarga CMake (android-project/app/src/main/java/org/libsdl/app).
 */
public class RhfActivity extends SDLActivity {
    @Override
    protected String[] getLibraries() {
        return new String[] {"main"};
    }
}
