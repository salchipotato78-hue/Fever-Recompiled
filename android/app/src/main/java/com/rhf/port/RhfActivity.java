package com.rhf.port;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
            // El motor escribe su log en stderr; en Android eso no llega a logcat. Con WP_LOG_FILE
            // se guarda en un archivo en la carpeta externa de la app (se lee con adb pull).
            java.io.File logDir = getExternalFilesDir(null);
            if (logDir == null) logDir = getFilesDir();
            Os.setenv("WP_LOG_FILE", new java.io.File(logDir, "rhf_log.txt").getAbsolutePath(), true);
            // stderr en su propio archivo (rhf_stderr.txt): los mensajes de error de SDL/GL van ahí.
            Os.setenv("WP_STDERR_FILE", new java.io.File(logDir, "rhf_stderr.txt").getAbsolutePath(), true);
            // Diagnóstico de video: registra los comandos GX para ver qué llega al renderizador.
            Os.setenv("WP_LOG_GX", "1", true);
        } catch (ErrnoException e) {
            // Sin estas variables el motor usa la carpeta actual; el juego lo indicará si no encuentra datos.
        }
        super.onCreate(savedInstanceState);
        // Botones táctiles encima de la superficie de SDL: cruceta, A, B y +.
        addContentView(new TouchOverlay(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars();
        }
    }

    /** Oculta la barra de estado y la de navegación; aparecen solo con un deslizamiento. */
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected String[] getLibraries() {
        return new String[] {"main"};
    }
}
