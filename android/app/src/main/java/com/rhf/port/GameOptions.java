package com.rhf.port;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ajustes del juego. Se guardan en SharedPreferences y se escriben en games/rhf/settings.ini, el
 * archivo que lee el motor. Se cambian desde el engranaje dentro del juego (GameSettingsPanel).
 */
public final class GameOptions {
    private static final String PREFS = "rhf_options";

    /** Escala de render (video.scale): 1x es el original; más alto = más nítido, más costoso. */
    public static final int[] SCALES = {1, 2, 3, 4};

    /** FPS que se muestran: 60 = apagado. 120, 180 y 240 interpolan frames (experimental). */
    public static final int[] INTERPOLATION_FPS = {60, 120, 180, 240};

    private GameOptions() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static int scale(Context context) {
        return prefs(context).getInt("scale", 1);
    }

    public static void cycleScale(Context context) {
        int current = scale(context);
        int next = SCALES[0];
        for (int i = 0; i < SCALES.length; i++) {
            if (SCALES[i] == current) {
                next = SCALES[(i + 1) % SCALES.length];
            }
        }
        prefs(context).edit().putInt("scale", next).apply();
    }

    public static int interpolationFps(Context context) {
        // Migración: la opción anterior era un interruptor (frame_blend); si estaba activa, equivale a 120.
        int fallback = prefs(context).getBoolean("frame_blend", false) ? 120 : 60;
        return prefs(context).getInt("interpolation_fps", fallback);
    }

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

    /** Bordes ultrawide: true = imagen difuminada a los lados; false = negro. */
    public static boolean wideBackdrop(Context context) {
        return prefs(context).getBoolean("wide_backdrop", true);
    }

    public static void setWideBackdrop(Context context, boolean on) {
        prefs(context).edit().putBoolean("wide_backdrop", on).apply();
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

    /**
     * Escribe las opciones en games/rhf/settings.ini. Solo cambia las claves que pertenecen a la app;
     * el resto del archivo (controles, idioma, etc., que guarda el motor) se conserva tal cual.
     */
    public static void writeSettingsFile(Context context) throws IOException {
        File file = new File(context.getFilesDir(), "games/rhf/settings.ini");
        file.getParentFile().mkdirs();

        Map<String, String> owned = new LinkedHashMap<>();   // "seccion.clave" -> valor
        owned.put("video.scale", String.valueOf(scale(context)));
        owned.put("video.custom_textures", customTextures(context) ? "1" : "0");
        owned.put("video.frame_interpolation", String.valueOf(interpolationFps(context)));
        owned.put("video.wide_backdrop", wideBackdrop(context) ? "1" : "0");
        owned.put("input.gamepads", gamepads(context) ? "1" : "0");

        List<String> lines = readLines(file);
        List<String> out = new ArrayList<>();
        Set<String> written = new HashSet<>();
        String section = "";
        for (String raw : lines) {
            String line = raw.trim();
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                out.add(raw);
                continue;
            }
            int equals = line.indexOf('=');
            if (equals > 0 && !line.startsWith(";") && !line.startsWith("#")) {
                String key = line.substring(0, equals).trim();
                String full = section + "." + key;
                if (owned.containsKey(full)) {
                    out.add(key + "=" + owned.get(full));
                    written.add(full);
                    continue;
                }
            }
            out.add(raw);
        }

        // Claves que aún no estaban en el archivo: se añaden en un bloque al final de su sección.
        Map<String, List<String>> missing = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : owned.entrySet()) {
            if (written.contains(entry.getKey())) continue;
            String full = entry.getKey();
            int dot = full.indexOf('.');
            String sec = full.substring(0, dot);
            String key = full.substring(dot + 1);
            if (!missing.containsKey(sec)) missing.put(sec, new ArrayList<>());
            missing.get(sec).add(key + "=" + entry.getValue());
        }
        for (Map.Entry<String, List<String>> block : missing.entrySet()) {
            out.add("");
            out.add("[" + block.getKey() + "]");
            out.addAll(block.getValue());
        }

        StringBuilder text = new StringBuilder();
        for (String line : out) {
            text.append(line).append('\n');
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            writer.write(text.toString());
        }
    }

    private static List<String> readLines(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        if (!file.isFile()) return lines;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
}
