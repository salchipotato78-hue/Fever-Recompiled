package com.rhf.port;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Pantalla de inicio y única entrada de la app. La primera vez pide un .zip con la carpeta DATA del disco
 * (la que contiene sys/ y files/) y la extrae en la carpeta interna de la app, en games/rhf/extracted,
 * que es la ruta que usa el motor. Después abre el juego.
 */
public class SetupActivity extends Activity {
    private static final int REQUEST_ZIP = 1;
    private TextView status;
    private Button chooseButton;

    /** Carpeta raíz de la app: el motor la usa como directorio de trabajo (RHF_HOME). */
    static File homeDir(android.content.Context context) {
        return context.getFilesDir();
    }

    static File extractedDir(android.content.Context context) {
        return new File(homeDir(context), "games/rhf/extracted");
    }

    static boolean dataReady(android.content.Context context) {
        File extracted = extractedDir(context);
        return new File(extracted, "sys/main.dol").isFile() && new File(extracted, "files").isDirectory();
    }

    private static final String PREFS = "rhf_import";

    /**
     * Entrada de la app. La primera vez pide el .zip; después, si los datos ya están importados, abre
     * el juego directamente. Para cambiar de datos hay que borrar los datos de la app (o reinstalarla).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean imported = getSharedPreferences(PREFS, MODE_PRIVATE).getString("zip_id", null) != null;
        if (imported && dataReady(this)) {
            try {
                GameOptions.writeSettingsFile(this);
            } catch (IOException e) {
                // Sin settings.ini el motor usa sus valores por defecto; el juego abre igual.
            }
            startGame();
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(48, 48, 48, 48);

        TextView title = new TextView(this);
        title.setText("Rhythm Heaven Fever");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        status = new TextView(this);
        status.setText("Elige el .zip con tu carpeta DATA del disco para jugar "
                + "(debe contener sys/main.dol y files/).");
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 32, 0, 32);
        layout.addView(status);

        chooseButton = new Button(this);
        chooseButton.setText("Elegir archivo ZIP");
        chooseButton.setOnClickListener(v -> chooseZip());
        layout.addView(chooseButton);

        setContentView(layout);
    }

    /** Nombre y tamaño del archivo elegido: sirven para saber si es el mismo zip que ya se importó. */
    private String zipIdentity(Uri uri) {
        String name = "?";
        long size = -1;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameColumn >= 0) name = cursor.getString(nameColumn);
                int sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) size = cursor.getLong(sizeColumn);
            }
        } catch (RuntimeException e) {
            // Sin metadatos no se puede comparar: el zip se importa de nuevo.
        }
        return name + "|" + size;
    }

    private void chooseZip() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_ZIP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_ZIP || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        chooseButton.setEnabled(false);
        String identity = zipIdentity(uri);
        boolean alreadyImported = identity.equals(getSharedPreferences(PREFS, MODE_PRIVATE).getString("zip_id", null))
                && dataReady(this);
        status.setText(alreadyImported ? "Juego ya importado, abriendo..."
                : "Importando el juego... esto puede tardar unos minutos.");
        new Thread(() -> {
            String error = null;
            try {
                if (!alreadyImported) {
                    extract(uri);
                    if (!dataReady(this)) {
                        error = "El .zip no contiene sys/main.dol y files/. Revisa que sea la carpeta DATA.";
                    } else {
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("zip_id", identity).apply();
                    }
                }
                if (error == null) {
                    GameOptions.writeSettingsFile(this);   // los ajustes llegan al motor antes de arrancar
                }
            } catch (IOException e) {
                error = "No pude leer el .zip: " + e.getMessage();
            }
            final String message = error;
            runOnUiThread(() -> {
                if (message == null) {
                    startGame();
                } else {
                    status.setText(message);
                    chooseButton.setEnabled(true);
                }
            });
        }).start();
    }

    /**
     * Extrae el zip. Se acepta con o sin una carpeta DATA delante: cualquier entrada que contenga
     * "sys/" o "files/" se copia a partir de ese punto. Se rechazan rutas que salgan de la carpeta.
     */
    private void extract(Uri uri) throws IOException {
        File extracted = extractedDir(this);
        deleteRecursively(extracted);
        if (!extracted.mkdirs() && !extracted.isDirectory()) {
            throw new IOException("no pude crear " + extracted);
        }
        String base = extracted.getCanonicalPath() + File.separator;

        InputStream raw = getContentResolver().openInputStream(uri);
        if (raw == null) throw new IOException("el archivo no se puede abrir");
        try (ZipInputStream zip = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                String relative = relativeToGame(name);
                if (relative == null) {
                    continue;
                }
                File out = new File(extracted, relative);
                if (!out.getCanonicalPath().startsWith(base)) {
                    continue;  // ruta peligrosa: fuera de la carpeta del juego
                }
                if (entry.isDirectory()) {
                    out.mkdirs();
                    continue;
                }
                File parent = out.getParentFile();
                if (parent != null) parent.mkdirs();
                try (OutputStream os = new FileOutputStream(out)) {
                    byte[] buffer = new byte[1 << 16];
                    int n;
                    while ((n = zip.read(buffer)) > 0) {
                        os.write(buffer, 0, n);
                    }
                }
            }
        }
    }

    /** "DATA/sys/main.dol" -> "sys/main.dol"; "files/x" -> "files/x"; otras entradas -> null. */
    static String relativeToGame(String name) {
        if (name.startsWith("sys/") || name.startsWith("files/")) return name;
        int sys = name.indexOf("/sys/");
        if (sys >= 0) return name.substring(sys + 1);
        int files = name.indexOf("/files/");
        if (files >= 0) return name.substring(files + 1);
        return null;
    }

    private static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) deleteRecursively(child);
        }
        file.delete();
    }

    private void startGame() {
        startActivity(new Intent(this, RhfActivity.class));
        finish();
    }
}
