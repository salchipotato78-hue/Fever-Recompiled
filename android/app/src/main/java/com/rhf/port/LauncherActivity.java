package com.rhf.port;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Launcher vertical de Rhythm Heaven Fever: fondo de tablero, encabezado con los personajes,
 * logo y los botones. El texto usa la fuente FeverHD (assets/fonts/rhf_ui.ttf).
 *
 * Si el juego ya está importado, JUGAR abre el juego. Si no, el botón lleva a la importación
 * (SetupActivity), que es la misma pantalla de antes.
 */
public class LauncherActivity extends Activity {
    private static final int CREAM = Color.rgb(255, 246, 220);
    private static final int ORANGE = Color.rgb(245, 176, 46);
    private static final int INK = Color.rgb(30, 30, 30);

    private Typeface uiFont;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            uiFont = Typeface.createFromAsset(getAssets(), "fonts/rhf_ui.ttf");
        } catch (RuntimeException e) {
            uiFont = Typeface.DEFAULT_BOLD;   // si falta la fuente, el launcher sigue funcionando
        }

        FrameLayout root = new FrameLayout(this);
        BitmapDrawable background = new BitmapDrawable(getResources(),
                android.graphics.BitmapFactory.decodeResource(getResources(), R.drawable.launcher_fondo));
        background.setTileModeXY(android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT);
        root.setBackground(background);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView banner = new ImageView(this);
        banner.setImageResource(R.drawable.launcher_baner);
        banner.setScaleType(ImageView.ScaleType.FIT_START);
        column.addView(banner, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.launcher_logo);
        logo.setAdjustViewBounds(true);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(260), LinearLayout.LayoutParams.WRAP_CONTENT);
        logoParams.topMargin = dp(12);
        column.addView(logo, logoParams);

        status = new TextView(this);
        status.setTypeface(uiFont);
        status.setTextColor(INK);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = dp(24);
        column.addView(status, statusParams);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dp(260), dp(56));
        buttonParams.topMargin = dp(16);
        column.addView(menuButton("JUGAR", v -> play()), buttonParams);
        column.addView(menuButton("OPCIONES", v -> startActivity(new Intent(this, OptionsActivity.class))),
                buttonParams);
        column.addView(menuButton("IMPORTAR JUEGO", v -> startActivity(new Intent(this, SetupActivity.class))),
                buttonParams);

        FrameLayout.LayoutParams columnParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP);
        root.addView(column, columnParams);
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean ready = SetupActivity.dataReady(this);
        status.setText(ready ? "Listo para jugar" : "Importa tu carpeta DATA del disco para jugar");
    }

    private void play() {
        if (SetupActivity.dataReady(this)) {
            try {
                GameOptions.writeSettingsFile(this);   // las opciones del launcher llegan al motor
            } catch (java.io.IOException e) {
                status.setText("No se pudieron guardar las opciones: " + e.getMessage());
                return;
            }
            startActivity(new Intent(this, RhfActivity.class));
        } else {
            startActivity(new Intent(this, SetupActivity.class));
        }
    }

    private TextView menuButton(String label, View.OnClickListener action) {
        TextView button = new TextView(this);
        button.setText(label);
        button.setTypeface(uiFont);
        button.setTextSize(20);
        button.setTextColor(INK);
        button.setGravity(Gravity.CENTER);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(CREAM);
        shape.setStroke(dp(4), INK);
        shape.setCornerRadius(dp(14));
        button.setBackground(shape);
        button.setOnClickListener(action);
        button.setOnTouchListener((v, event) -> {
            // Al pulsar, el botón cambia al naranja de la marca.
            switch (event.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    shape.setColor(ORANGE);
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    shape.setColor(CREAM);
                    break;
                default:
                    break;
            }
            return false;   // deja que el clic siga su curso
        });
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
