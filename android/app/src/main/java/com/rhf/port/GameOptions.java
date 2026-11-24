package com.rhf.port;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Opciones del juego que elige el jugador en el launcher. Se guardan en SharedPreferences y, antes
 * de abrir el juego, se escriben en games/rhf/settings.ini (el archivo que lee el motor).
 */
public final class GameOptions {
    private static final String PREFS = "rhf_options";

    /** Escala de render (video.scale): 1x es el original; más alto = más nítido, más costoso. */
    public static final int[] SCALES = {1, 2, 3, 4};

    private GameOptions() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static int scale(Context context) {
        return prefs(context).getInt("scale", 1);
    }

    public static void setScale(Context context, int scale) {
        prefs(context).edit().putInt("scale", scale).apply();
    }

    /** FPS que se muestran: 60 = apagado. 120, 180 y 240 interpolan frames (experimental). */
    public static final int[] INTERPOLATION_FPS = {60, 120, 180, 240};

    public static int interpolationFps(Context context) {
        // Migración: la opción anterior era un interruptor (frame_blend); si estaba activa, equivale a 120.
        int fallback = prefs(context).getBoolean("frame_blend", false) ? 120 : 60;
        return prefs(context).getInt("interpolation_fps", fallback);
    }

    /** Pasa al siguiente valor de INTERPOLATION_FPS (y vuelve a 60 al final). */
    public static void cycleInterpolationFps(Context context) {
        int current = interpolationFps(context);
        int next = INTERPOLATION_FPS[0];
        for (int i = 0; i < INTERPOLATION_FPS.length; i++) {
            if (INTERPOLATION_FPS[i] == current) {
                next = INTERPOLATION_FPS[(i + 1) % INTERPOLATION_FPS.length];
            }
        }
        prefs(context).edit().putInt("interpolation_fps", next).apply();
    }

    public static boolean customTextures(Context context) {
        return prefs(context).getBoolean("custom_textures", true);
    }

    public static void setCustomTextures(Context context, boolean on) {
        prefs(context).edit().putBoolean("custom_textures", on).apply();
    }

    public static boolean gamepads(Context context) {
        return prefs(context).getBoolean("gamepads", true);
    }

    public static void setGamepads(Context context, boolean on) {
        prefs(context).edit().putBoolean("gamepads", on).apply();
    }

    /** Escribe games/rhf/settings.ini con las opciones actuales. Solo las claves que el motor conoce. */
    public static void writeSettingsFile(Context context) throws IOException {
        File file = new File(context.getFilesDir(), "games/rhf/settings.ini");
        file.getParentFile().mkdirs();
        StringBuilder text = new StringBuilder();
        text.append("[video]\n");
        text.append("scale=").append(scale(context)).append('\n');
        text.append("custom_textures=").append(customTextures(context) ? 1 : 0).append('\n');
        text.append("frame_interpolation=").append(interpolationFps(context)).append('\n');
        text.append("\n[input]\n");
        text.append("gamepads=").append(gamepads(context) ? 1 : 0).append('\n');
        try (Writer out = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            out.write(text.toString());
        }
    }
}
