package com.rhf.port;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Ajustes dentro del juego, abiertos con el engranaje o con el botón Atrás. Ocupan el lado derecho y
 * dejan ver la carrera detrás. Cada cambio se guarda y se escribe en settings.ini al momento.
 */
public class GameSettingsPanel extends FrameLayout {
    private static final int INK = Color.rgb(30, 30, 30);
    private static final int CREAM = Color.rgb(255, 246, 220);
    private static final int ORANGE = Color.rgb(255, 196, 80);

    private final Context app;
    private final List<Row> rows = new ArrayList<>();

    /** Una fila del panel: su texto depende del valor actual y su toque cambia ese valor. */
    private interface Label { String get(); }
    private interface Change { void apply(); }

    private static final class Row {
        final TextView view;
        final Label label;
        Row(TextView view, Label label) { this.view = view; this.label = label; }
    }

    public GameSettingsPanel(Context context) {
        super(context);
        this.app = context.getApplicationContext();
        setBackgroundColor(Color.argb(150, 0, 0, 0));
        setVisibility(GONE);
        setOnClickListener(v -> hide());   // tocar fuera de la tarjeta lo cierra

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(16), dp(20), dp(16));
        GradientDrawable background = new GradientDrawable();
        background.setColor(ORANGE);
        background.setCornerRadius(dp(16));
        card.setBackground(background);
        card.setClickable(true);           // absorbe los toques de la tarjeta

        TextView title = new TextView(context);
        title.setText("AJUSTES");
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextSize(24);
        title.setTextColor(INK);
        title.setGravity(Gravity.CENTER);
        card.addView(title);

        row(card, () -> "Resolución: " + GameOptions.scale(app) + "x",
                () -> GameOptions.cycleScale(app));
        row(card, () -> {
            int fps = GameOptions.interpolationFps(app);
            return fps <= 60 ? "Interpolación: apagada" : "Interpolación: " + fps + " FPS (exp.)";
        }, () -> GameOptions.cycleInterpolationFps(app));
        row(card, () -> "Bordes ultrawide: " + (GameOptions.wideBackdrop(app) ? "difuminados" : "negros"),
                () -> GameOptions.setWideBackdrop(app, !GameOptions.wideBackdrop(app)));
        row(card, () -> "Texturas HD: " + onOff(GameOptions.customTextures(app)),
                () -> GameOptions.setCustomTextures(app, !GameOptions.customTextures(app)));
        row(card, () -> "Mando: " + onOff(GameOptions.gamepads(app)),
                () -> GameOptions.setGamepads(app, !GameOptions.gamepads(app)));
        row(card, () -> "VOLVER", null).setOnClickListener(v -> hide());

        LayoutParams params = new LayoutParams(dp(340), LayoutParams.WRAP_CONTENT,
                Gravity.END | Gravity.CENTER_VERTICAL);
        params.setMarginEnd(dp(16));
        addView(card, params);
    }

    public boolean isOpen() {
        return getVisibility() == VISIBLE;
    }

    public void show() {
        refresh();
        setVisibility(VISIBLE);
    }

    public void hide() {
        setVisibility(GONE);
    }

    /** Fila tocable. Con change == null, solo sirve de botón (VOLVER) y el listener lo pone quien llama. */
    private TextView row(LinearLayout card, Label label, Change change) {
        TextView view = new TextView(getContext());
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextSize(18);
        view.setTextColor(INK);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(CREAM);
        shape.setStroke(dp(3), INK);
        shape.setCornerRadius(dp(12));
        view.setBackground(shape);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(12);
        card.addView(view, params);
        view.setText(label.get());
        rows.add(new Row(view, label));
        if (change != null) {
            view.setOnClickListener(v -> {
                change.apply();
                save();
                refresh();
            });
        }
        return view;
    }

    /** Escribe settings.ini; el motor lo recarga en el siguiente frame. */
    private void save() {
        try {
            GameOptions.writeSettingsFile(app);
        } catch (IOException e) {
            // Si no se puede escribir, el cambio sigue en las preferencias y se aplica al reiniciar.
        }
    }

    private void refresh() {
        for (Row row : rows) {
            row.view.setText(row.label.get());
        }
    }

    private static String onOff(boolean on) {
        return on ? "Activadas" : "Desactivadas";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
