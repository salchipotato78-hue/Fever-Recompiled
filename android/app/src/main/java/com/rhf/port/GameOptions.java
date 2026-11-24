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

    public static boolean frameBlend(Context context) {
        return prefs(context).getBoolean("frame_blend", false);
    }

    public static void setFrameBlend(Context context, boolean on) {
        prefs(context).edit().putBoolean("frame_blend", on).apply();
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
        text.append("frame_blend=").append(frameBlend(context) ? 1 : 0).append('\n');
        text.append("\n[input]\n");
        text.append("gamepads=").append(gamepads(context) ? 1 : 0).append('\n');
        try (Writer out = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            out.write(text.toString());
        }
    }
}
