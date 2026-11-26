package com.rhf.port;

import android.content.Context;

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
 * Ajustes fijos del juego. Por ahora no hay opciones en la app: al importar el zip se escriben estos
 * valores en games/rhf/settings.ini, el archivo que lee el motor. Las opciones volverán cuando haya un
 * ultrawide real y buen rendimiento.
 */
public final class GameOptions {
    private GameOptions() {}

    /**
     * Escribe los valores fijos en games/rhf/settings.ini. Solo cambia las claves que pertenecen a la
     * app; el resto del archivo (controles, idioma, etc., que guarda el motor) se conserva tal cual.
     */
    public static void writeSettingsFile(Context context) throws IOException {
        File file = new File(context.getFilesDir(), "games/rhf/settings.ini");
        file.getParentFile().mkdirs();

        Map<String, String> owned = new LinkedHashMap<>();   // "seccion.clave" -> valor
        owned.put("video.scale", "1");
        owned.put("video.custom_textures", "1");
        owned.put("video.frame_interpolation", "0");
        owned.put("video.wide_backdrop", "0");
        owned.put("input.gamepads", "1");
        owned.put("video.skip_fade", "1");   // prueba de diagnóstico de pantalla negra; quitar al cerrar el caso

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
