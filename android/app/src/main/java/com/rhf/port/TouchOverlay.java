package com.rhf.port;

import android.content.Context;
import android.hardware.input.InputManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import org.libsdl.app.SDLActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Controles táctiles para Rhythm Heaven Fever.
 *
 *  - A, B y +: visibles por defecto, en la posición de la imagen de referencia de los niveles.
 *  - Cruceta: para los menús. Se muestra por defecto y el interruptor de arriba al centro la oculta
 *    (o vuelve a mostrar) al estar en un nivel. El motor aún no dice en qué pantalla está el juego.
 *
 * Todo el overlay se oculta si hay un mando conectado. Los toques fuera de los botones no se consumen,
 * así que llegan al juego como puntero.
 *
 * Cada botón envía una tecla de Android que SDL traduce a la misma tecla que usa el motor por defecto
 * (src/input/keymap.cpp): A = Enter, B = Backspace, + = NumPlus, cruceta = flechas.
 */
public class TouchOverlay extends View {

    private static final class Button {
        final String label;
        final int keycode;
        final boolean dpad;       // solo se muestra con la cruceta encendida
        float cx, cy, radius;
        Bitmap bitmap;            // arte de Wario (A, B, +); null = círculo con texto
        final RectF target = new RectF();
        int holders;              // cuántos dedos lo mantienen pulsado
        boolean keyDown;          // la tecla está pulsada en SDL
        long downAt;              // momento en que se envió el key-down
        Runnable pendingUp;       // key-up aplazado hasta cumplir el tiempo mínimo
        Button(String label, int keycode, boolean dpad) {
            this.label = label;
            this.keycode = keycode;
            this.dpad = dpad;
        }
    }

    // Tiempo mínimo que una tecla táctil permanece pulsada. El motor lee el teclado una vez por
    // fotograma; un toque más corto que eso (down y up en el mismo fotograma) no llega al juego.
    private static final long MIN_HOLD_MS = 80;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final List<Button> buttons = new ArrayList<>();
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final float unit;   // tamaño base de botón en píxeles
    private final float margin;
    private final float density;

    private final RectF menuToggle = new RectF();   // interruptor de la cruceta, arriba al centro

    private final InputManager inputManager;

    public TouchOverlay(Context context) {
        super(context);
        inputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        density = dm.density;
        unit = 56f * dm.density;
        margin = 16f * dm.density;

        buttons.add(new Button("A", KeyEvent.KEYCODE_ENTER, false));
        buttons.add(new Button("B", KeyEvent.KEYCODE_DEL, false));
        buttons.add(new Button("+", KeyEvent.KEYCODE_NUMPAD_ADD, false));
        buttons.add(new Button("\u25B2", KeyEvent.KEYCODE_DPAD_UP, true));
        buttons.add(new Button("\u25BC", KeyEvent.KEYCODE_DPAD_DOWN, true));
        buttons.add(new Button("\u25C0", KeyEvent.KEYCODE_DPAD_LEFT, true));
        buttons.add(new Button("\u25B6", KeyEvent.KEYCODE_DPAD_RIGHT, true));

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
        // A y B: a la altura de los dedos del jugador, B a la izquierda y A a la derecha. Medidas en
        // fracciones de la pantalla para que escalen con cualquier resolución.
        // Medidas de la imagen de referencia: B y A grandes, en las esquinas inferiores (parte de cada
        // botón queda fuera de la pantalla, como en la referencia). El arte se dibuja un 15 % más grande.
        place("B", 0.037f * w, 0.890f * h, 0.081f * w);
        place("A", 0.967f * w, 0.896f * h, 0.084f * w);
        // Pausa arriba a la derecha, en la esquina.
        place("+", 0.978f * w, 0.050f * h, 0.032f * w);
        // Cruceta a la izquierda, por encima de B para no tocarse.
        float dcx = 0.11f * w;
        float dcy = 0.42f * h;
        float arm = unit;
        float dr = 0.5f * unit;
        place("\u25B2", dcx, dcy - arm, dr);
        place("\u25BC", dcx, dcy + arm, dr);
        place("\u25C0", dcx - arm, dcy, dr);
        place("\u25B6", dcx + arm, dcy, dr);
        // Interruptor de la cruceta arriba al centro.
        float half = 80f * density;
        float cx = w / 2f;
        menuToggle.set(cx - half, margin, cx + half, margin + 40f * density);
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
            if (!isActive(b)) continue;
            if (b.bitmap != null) {
                // Arte: un poco más grande que el área de toque y más opaco al pulsar.
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
        // Interruptor de la cruceta.
        float radius = menuToggle.height() / 2f;
        fill.setAlpha(60);
        canvas.drawRoundRect(menuToggle, radius, radius, fill);
        canvas.drawRoundRect(menuToggle, radius, radius, stroke);
        float baseline = menuToggle.centerY() - (text.descent() + text.ascent()) / 2f;
        canvas.drawText(dpadShown ? "Cruceta: sí" : "Cruceta: no", menuToggle.centerX(), baseline, text);
    }

    /** Un botón se ve y recibe toques solo si el overlay está visible (y la cruceta, si es de la cruceta). */
    private boolean isActive(Button b) {
        return visible && (!b.dpad || dpadShown);
    }

    private Button buttonAt(float x, float y) {
        for (Button b : buttons) {
            if (!isActive(b)) continue;
            float dx = x - b.cx, dy = y - b.cy;
            if (dx * dx + dy * dy <= b.radius * b.radius * 1.2f) return b;
        }
        return null;
    }

    private void press(Button b) {
        if (b.pendingUp != null) {
            // Se volvió a pulsar antes de soltar la tecla: se cancela el key-up pendiente.
            handler.removeCallbacks(b.pendingUp);
            b.pendingUp = null;
        }
        if (b.holders++ == 0 && !b.keyDown) {
            b.keyDown = true;
            b.downAt = SystemClock.uptimeMillis();
            SDLActivity.onNativeKeyDown(b.keycode);
        }
    }

    private void release(Button b) {
        if (b.holders > 0 && --b.holders == 0) {
            long wait = MIN_HOLD_MS - (SystemClock.uptimeMillis() - b.downAt);
            if (wait <= 0) {
                sendKeyUp(b);
            } else {
                b.pendingUp = () -> {
                    b.pendingUp = null;
                    sendKeyUp(b);
                };
                handler.postDelayed(b.pendingUp, wait);
            }
        }
    }

    private void sendKeyUp(Button b) {
        if (b.keyDown && b.holders == 0) {
            b.keyDown = false;
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
    private final HashMap<Integer, Button> fingers = new HashMap<>();

    private boolean padConnected = false; // hay un mando conectado: el touch se oculta
    private boolean visible = true;       // resultado final: se dibuja y consume toques
    private boolean dpadShown = true;     // cruceta encendida (menús) o apagada (niveles)
    // Elección del jugador con el botón de la esquina: null = automático (visible salvo con mando),
    // true / false = se muestran / se ocultan siempre.
    private Boolean userChoice = null;

    /** Invierte lo que se ve ahora: si están visibles los oculta, y si no, los muestra. */
    public void toggleUserVisible() {
        userChoice = !visible;
        applyVisibility();
    }

    public boolean isUserVisible() {
        return visible;
    }

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
        if (inputManager != null) {
            inputManager.registerInputDeviceListener(deviceListener, null);
        }
        updatePad();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (inputManager != null) {
            inputManager.unregisterInputDeviceListener(deviceListener);
        }
        super.onDetachedFromWindow();
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
        boolean now = userChoice != null ? userChoice : !padConnected;
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

    /** Avisa cuando cambia la visibilidad (para que el botón de la app muestre el estado correcto). */
    public void setVisibilityListener(Runnable listener) {
        visibilityListener = listener;
    }

    /** Enciende o apaga la cruceta (menús). Al apagarla se sueltan sus botones. */
    private void toggleDpad() {
        dpadShown = !dpadShown;
        if (!dpadShown) {
            for (Button b : buttons) {
                if (b.dpad && b.holders > 0) {
                    b.holders = 1;
                    release(b);
                }
            }
            // Quitar los dedos que estaban sobre la cruceta.
            java.util.Iterator<Button> it = fingers.values().iterator();
            while (it.hasNext()) {
                if (it.next().dpad) it.remove();
            }
        }
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!visible) return false;   // oculto: el toque lo recibe el juego
        int action = event.getActionMasked();
        boolean consumed = false;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int i = event.getActionIndex();
                float x = event.getX(i), y = event.getY(i);
                if (menuToggle.contains(x, y)) {
                    if (action == MotionEvent.ACTION_DOWN) toggleDpad();
                    consumed = true;
                    break;
                }
                consumed = touchDown(event.getPointerId(i), x, y);
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
