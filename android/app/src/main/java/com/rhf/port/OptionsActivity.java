package com.rhf.port;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Opciones del juego, con el mismo estilo del launcher. Cada botón cambia su valor al pulsarlo. */
public class OptionsActivity extends Activity {
    private static final int CREAM = Color.rgb(255, 246, 220);
    private static final int INK = Color.rgb(30, 30, 30);

    private Typeface uiFont;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            uiFont = Typeface.createFromAsset(getAssets(), "fonts/rhf_ui.ttf");
        } catch (RuntimeException e) {
            uiFont = Typeface.DEFAULT_BOLD;
        }
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setPadding(dp(24), dp(24), dp(24), dp(24));
        column.setBackgroundColor(Color.rgb(255, 196, 80));

        TextView title = new TextView(this);
        title.setText("OPCIONES");
        title.setTypeface(uiFont);
        title.setTextSize(26);
        title.setTextColor(INK);
        column.addView(title);

        column.addView(optionButton(
                () -> "Resolución: " + GameOptions.scale(this) + "x",
                () -> {
                    int[] scales = GameOptions.SCALES;
                    int next = 0;
                    for (int i = 0; i < scales.length; i++) {
                        if (scales[i] == GameOptions.scale(this)) {
                            next = (i + 1) % scales.length;
                        }
                    }
                    GameOptions.setScale(this, scales[next]);
                }));
        column.addView(optionButton(
                () -> {
                    int fps = GameOptions.interpolationFps(this);
                    return fps <= 60 ? "Interpolación: apagada"
                            : "Interpolación: " + fps + " FPS (exp.)";
                },
                () -> GameOptions.cycleInterpolationFps(this)));
        column.addView(optionButton(
                () -> "Texturas HD: " + onOff(GameOptions.customTextures(this)),
                () -> GameOptions.setCustomTextures(this, !GameOptions.customTextures(this))));
        column.addView(optionButton(
                () -> "Mando: " + onOff(GameOptions.gamepads(this)),
                () -> GameOptions.setGamepads(this, !GameOptions.gamepads(this))));

        TextView back = optionButton(() -> "VOLVER", null);
        back.setOnClickListener(v -> finish());
        column.addView(back);
        setContentView(column);
    }

    private interface Label { String get(); }
    private interface Change { void apply(); }

    private TextView optionButton(Label label, Change change) {
        TextView button = new TextView(this);
        button.setTypeface(uiFont);
        button.setTextSize(18);
        button.setTextColor(INK);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(CREAM);
        shape.setStroke(dp(3), INK);
        shape.setCornerRadius(dp(12));
        button.setBackground(shape);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(320), LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(12);
        button.setLayoutParams(params);
        button.setText(label.get());
        if (change != null) {
            button.setOnClickListener(v -> {
                change.apply();
                button.setText(label.get());
            });
        }
        return button;
    }

    private static String onOff(boolean on) {
        return on ? "Activado" : "Desactivado";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
