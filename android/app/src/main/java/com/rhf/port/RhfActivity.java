package com.rhf.port;

import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;

import org.libsdl.app.SDLActivity;

/**
 * Actividad del juego. Antes de que SDL cargue la librería nativa, exporta RHF_HOME (la carpeta
 * interna de la app) para que el motor encuentre los datos importados en SetupActivity.
 * SDL carga la librería "main" (libmain.so) y ejecuta su SDL_main (engine/src/app/main.cpp).
 */
public class RhfActivity extends SDLActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            Os.setenv("RHF_HOME", getFilesDir().getAbsolutePath(), true);
        } catch (ErrnoException e) {
            // Sin RHF_HOME el motor usa la carpeta actual; el juego lo indicará si no encuentra datos.
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected String[] getLibraries() {
        return new String[] {"main"};
    }
}
