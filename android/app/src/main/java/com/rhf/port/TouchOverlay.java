package com.rhf.port;

import android.content.Context;
import android.hardware.input.InputManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import org.libsdl.app.SDLActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Controles táctiles mínimos para Rhythm Heaven Fever.
 *
 * Solo se muestran en el gameplay (el motor lo dice con nativeInGame); en los menús el toque llega
 * directo al juego, como en NSMBW. Si el motor no exporta nativeInGame, los botones se quedan visibles.
 *
 * Botones:
 *  - Cruceta: navegar menús.
 *  - A y B: tocar el ritmo y confirmar / cancelar.
 *  - +: pausa.
 *
 * Cada botón envía una tecla de Android que SDL traduce a la misma tecla que usa el motor por defecto
 * (src/input/keymap.cpp): A = Enter, B = Backspace, + = NumPlus, cruceta = flechas.
 */
public class TouchOverlay extends View {

    private static final class Button {
        final String label;
        final int keycode;
        final boolean big;
        float cx, cy, radius;
        Bitmap bitmap;            // arte de Wario (A, B, +); null = círculo con texto
        final RectF target = new RectF();
        int holders;              // cuántos dedos lo mantienen pulsado
        Button(String label, int keycode, boolean big) {
            this.label = label;
            this.keycode = keycode;
            this.big = big;
        }
    }

    private final List<Button> buttons = new ArrayList<>();
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final float unit;   // tamaño base de botón en píxeles
    private final float margin;

    private final InputManager inputManager;

    public TouchOverlay(Context context) {
        super(context);
        inputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        unit = 56f * dm.density;
        margin = 16f * dm.density;

        buttons.add(new Button("\u25B2", KeyEvent.KEYCODE_DPAD_UP, false));
        buttons.add(new Button("\u25BC", KeyEvent.KEYCODE_DPAD_DOWN, false));
        buttons.add(new Button("\u25C0", KeyEvent.KEYCODE_DPAD_LEFT, false));
        buttons.add(new Button("\u25B6", KeyEvent.KEYCODE_DPAD_RIGHT, false));
        buttons.add(new Button("A", KeyEvent.KEYCODE_ENTER, true));
        buttons.add(new Button("B", KeyEvent.KEYCODE_DEL, false));
        buttons.add(new Button("+", KeyEvent.KEYCODE_NUMPAD_ADD, false));

        fill.setColor(Color.WHITE);
        fill.setAlpha(60);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2f * dm.density);
        stroke.setColor(Color.WHITE);
        stroke.setAlpha(150);
        text.setColor(Color.WHITE);
        text.setAlpha(220);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(20f * dm.density);
        text.setFakeBoldText(true);

        attachArt("A", R.drawable.btn_a);
        attachArt("B", R.drawable.btn_b);
        attachArt("+", R.drawable.btn_plus);

        setFocusable(false);
    }

    private void attachArt(String label, int resId) {
        for (Button b : buttons) {
            if (b.label.equals(label)) {
                b.bitmap = BitmapFactory.decodeResource(getResources(), resId);
                return;
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Cruceta abajo a la izquierda.
        float dcx = margin + 1.5f * unit;
        float dcy = h - margin - 1.5f * unit;
        place("\u25B2", dcx, dcy - unit, 0.5f * unit);
        place("\u25BC", dcx, dcy + unit, 0.5f * unit);
        place("\u25C0", dcx - unit, dcy, 0.5f * unit);
        place("\u25B6", dcx + unit, dcy, 0.5f * unit);
        // A y B abajo a la derecha.
        place("A", w - margin - 0.9f * unit, h - margin - 0.9f * unit, 0.6f * unit);
        place("B", w - margin - 2.3f * unit, h - margin - 0.5f * unit, 0.5f * unit);
        // Pausa arriba a la derecha.
        place("+", w - margin - 0.6f * unit, margin + 0.6f * unit, 0.5f * unit);
    }

    private void place(String label, float cx, float cy, float radius) {
        for (Button b : buttons) {
            if (b.label.equals(label)) {
                b.cx = cx;
                b.cy = cy;
                b.radius = radius;
                return;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!visible) return;
        for (Button b : buttons) {
            if (b.bitmap != null) {
                // Arte de Wario: un poco más grande que el área de toque y más opaco al pulsar.
                float half = b.radius * 1.15f;
                b.target.set(b.cx - half, b.cy - half, b.cx + half, b.cy + half);
                bitmapPaint.setAlpha(b.holders > 0 ? 255 : 200);
                canvas.drawBitmap(b.bitmap, null, b.target, bitmapPaint);
                continue;
            }
            fill.setAlpha(b.holders > 0 ? 150 : 60);
            canvas.drawCircle(b.cx, b.cy, b.radius, fill);
            canvas.drawCircle(b.cx, b.cy, b.radius, stroke);
            float baseline = b.cy - (text.descent() + text.ascent()) / 2f;
            canvas.drawText(b.label, b.cx, baseline, text);
        }
    }

    private Button buttonAt(float x, float y) {
        for (Button b : buttons) {
            float dx = x - b.cx, dy = y - b.cy;
            if (dx * dx + dy * dy <= b.radius * b.radius * 1.2f) return b;
        }
        return null;
    }

    private void press(Button b) {
        if (b.holders++ == 0) {
            SDLActivity.onNativeKeyDown(b.keycode);
        }
    }

    private void release(Button b) {
        if (b.holders > 0 && --b.holders == 0) {
            SDLActivity.onNativeKeyUp(b.keycode);
        }
    }

    private void releaseAll() {
        for (Button b : buttons) {
            if (b.holders > 0) {
                b.holders = 1;
                release(b);
            }
        }
        invalidate();
    }

    // Guarda, para cada dedo (pointerId), el botón que está pulsando.
    private final java.util.HashMap<Integer, Button> fingers = new java.util.HashMap<>();

    /** True when the player is in a course or on the map with no menu up (runtime, GameActivity-style). */
    private static native boolean nativeInGame();

    private boolean gameVisible = true;   // lo que dice el motor: en gameplay y sin menú
    private boolean padConnected = false; // hay un mando conectado: el touch se oculta
    private boolean visible = true;       // resultado final: se dibuja y consume toques
    private boolean nativeMissing = false; // el motor aún no exporta nativeInGame: siempre visibles
    // Elección del jugador con el botón de la app: null = automático (gameplay y sin mando),
    // true / false = se muestran / se ocultan siempre, aunque haya mando o estemos en menús.
    private Boolean userChoice = null;

    /** Invierte lo que se ve ahora: si están visibles los oculta, y si no, los muestra. */
    public void toggleUserVisible() {
        userChoice = !visible;
        applyVisibility();
    }

    public boolean isUserVisible() {
        return visible;
    }

    private final Runnable visibilityPoll = new Runnable() {
        @Override
        public void run() {
            refreshVisibility();
            postDelayed(this, 100);
        }
    };

    // Un mando conectado (o desconectado) cambia la visibilidad del touch al momento.
    private final InputManager.InputDeviceListener deviceListener = new InputManager.InputDeviceListener() {
        @Override
        public void onInputDeviceAdded(int deviceId) {
            updatePad();
        }

        @Override
        public void onInputDeviceRemoved(int deviceId) {
            updatePad();
        }

        @Override
        public void onInputDeviceChanged(int deviceId) {
            updatePad();
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks(visibilityPoll);
        post(visibilityPoll);
        if (inputManager != null) {
            inputManager.registerInputDeviceListener(deviceListener, null);
        }
        updatePad();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(visibilityPoll);
        if (inputManager != null) {
            inputManager.unregisterInputDeviceListener(deviceListener);
        }
        super.onDetachedFromWindow();
    }

    private void refreshVisibility() {
        boolean inGame = true;
        if (!nativeMissing) {
            try {
                inGame = nativeInGame();
            } catch (UnsatisfiedLinkError e) {
                nativeMissing = true;   // sin el export, los botones se quedan como antes
            }
        }
        gameVisible = inGame;
        applyVisibility();
    }

    private void updatePad() {
        padConnected = hasGamepad();
        applyVisibility();
    }

    /** Hay algún mando (gamepad o joystick) conectado que no sea virtual. */
    private boolean hasGamepad() {
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(id);
            if (device == null || device.isVirtual()) continue;
            int sources = device.getSources();
            if ((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                    || (sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
                return true;
            }
        }
        return false;
    }

    private void applyVisibility() {
        boolean now = userChoice != null ? userChoice : (gameVisible && !padConnected);
        if (now == visible) return;
        visible = now;
        if (!visible) {
            // Al ocultarse se sueltan los botones: no queda ninguna tecla pulsada.
            releaseAll();
            fingers.clear();
        }
        invalidate();
        if (visibilityListener != null) visibilityListener.run();
    }

    private Runnable visibilityListener;

    /** Avisa cuando cambia la visibilidad (para que el botón de la app muestre el texto correcto). */
    public void setVisibilityListener(Runnable listener) {
        visibilityListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!visible) return false;   // en menús el toque lo recibe el juego
        int action = event.getActionMasked();
        boolean consumed = false;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int i = event.getActionIndex();
                consumed = touchDown(event.getPointerId(i), event.getX(i), event.getY(i));
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int id = event.getPointerId(i);
                    float x = event.getX(i), y = event.getY(i);
                    Button current = fingers.get(id);
                    if (current == null) continue;
                    Button under = buttonAt(x, y);
                    if (under != current) {
                        release(current);
                        fingers.remove(id);
                        if (under != null) {
                            fingers.put(id, under);
                            press(under);
                        }
                        invalidate();
                    }
                    consumed = true;
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int i = event.getActionIndex();
                touchUp(event.getPointerId(i));
                consumed = true;
                break;
            }
            case MotionEvent.ACTION_CANCEL:
                releaseAll();
                fingers.clear();
                consumed = true;
                break;
            default:
                break;
        }
        return consumed;
    }

    private boolean touchDown(int id, float x, float y) {
        Button b = buttonAt(x, y);
        if (b == null) return false;
        fingers.put(id, b);
        press(b);
        invalidate();
        return true;
    }

    private void touchUp(int id) {
        Button b = fingers.remove(id);
        if (b != null) {
            release(b);
            invalidate();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus) {
            fingers.clear();
            releaseAll();
        }
    }
}
