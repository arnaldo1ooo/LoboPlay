package py.loboplay.app.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import py.loboplay.app.util.Ajustes;

// Cambia el brillo deslizando el dedo arriba o abajo por el lado izquierdo de la pantalla.
class GestoBrillo {
    // Interfaz para cancelar el toque que ya habia recibido el video.
    interface Cancelar {
        void cancelar(MotionEvent evento);
    }

    private static final float ZONA = 0.33f;     // parte izquierda de la pantalla que responde
    private static final float MIN = 0.02f;

    // Se guarda para que el brillo siga igual al cambiar de canal.
    private static float brilloGuardado = -1f;

    private final Activity act;
    private final FrameLayout raiz;
    private final Handler manejador = new Handler(Looper.getMainLooper());
    private final int umbral;
    private TextView indicador;
    private View intro;

    private boolean posible;
    private boolean arrastrando;
    private float inicioY;
    private float inicioX;
    private float brilloInicial;

    GestoBrillo(Activity act, FrameLayout raiz) {
        this.act = act;
        this.raiz = raiz;
        this.umbral = ViewConfiguration.get(act).getScaledTouchSlop();
        if (brilloGuardado >= 0 && Ajustes.gestoBrillo(act)) poner(brilloGuardado);
    }

    // Revisa cada toque; devuelve true si lo uso para el brillo (y entonces no debe llegar al video).
    boolean procesar(MotionEvent ev, int limiteInferior, Cancelar cancelar) {
        if (!Ajustes.gestoBrillo(act)) return false;
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                posible = ev.getX() < raiz.getWidth() * ZONA && ev.getY() < limiteInferior;
                arrastrando = false;
                inicioX = ev.getX();
                inicioY = ev.getY();
                brilloInicial = brilloActual();
                return false;
            case MotionEvent.ACTION_POINTER_DOWN:
                posible = false;
                return arrastrando;
            case MotionEvent.ACTION_MOVE:
                if (!posible) return false;
                float dy = ev.getY() - inicioY;
                if (!arrastrando) {
                    float dx = ev.getX() - inicioX;
                    if (Math.abs(dy) < umbral * 2 || Math.abs(dy) < Math.abs(dx)) return false;
                    arrastrando = true;
                    quitarIntro();
                    MotionEvent c = MotionEvent.obtain(ev);
                    c.setAction(MotionEvent.ACTION_CANCEL);
                    cancelar.cancelar(c);
                    c.recycle();
                }
                float v = brilloInicial - dy / (raiz.getHeight() * 0.7f);
                v = Math.max(MIN, Math.min(1f, v));
                poner(v);
                brilloGuardado = v;
                mostrar(v);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                boolean fue = arrastrando;
                arrastrando = false;
                posible = false;
                return fue;
            default:
                return arrastrando;
        }
    }

    private float brilloActual() {
        float w = act.getWindow().getAttributes().screenBrightness;
        if (w >= 0) return w;
        try {
            return Settings.System.getInt(act.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS) / 255f;
        } catch (Exception e) {
            return 0.5f;
        }
    }

    private void poner(float valor) {
        WindowManager.LayoutParams p = act.getWindow().getAttributes();
        p.screenBrightness = valor;
        act.getWindow().setAttributes(p);
    }

    // Muestra el porcentaje un momento.
    private void mostrar(float valor) {
        if (indicador == null) {
            indicador = new TextView(act);
            indicador.setTextColor(Color.WHITE);
            indicador.setTextSize(18);
            float d = act.getResources().getDisplayMetrics().density;
            indicador.setPadding((int) (16 * d), (int) (10 * d), (int) (16 * d), (int) (10 * d));
            GradientDrawable fondo = new GradientDrawable();
            fondo.setColor(0xB3000000);
            fondo.setCornerRadius(24 * d);
            indicador.setBackground(fondo);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL | Gravity.START);
            lp.leftMargin = (int) (24 * d);
            raiz.addView(indicador, lp);
        }
        indicador.setText("☀ " + Math.round(valor * 100) + "%");
        indicador.setVisibility(View.VISIBLE);
        indicador.bringToFront();
        manejador.removeCallbacks(esconder);
        manejador.postDelayed(esconder, 900);
    }

    private final Runnable esconder = () -> {
        if (indicador != null) indicador.setVisibility(View.GONE);
    };

    // La primera vez explica el gesto sobre la zona donde funciona.
    void presentarSiHaceFalta() {
        if (!Ajustes.gestoBrillo(act) || Ajustes.gestoBrilloVisto(act)) return;
        manejador.postDelayed(() -> {
            if (act.isFinishing() || act.isDestroyed() || intro != null) return;
            Ajustes.marcarGestoBrilloVisto(act);
            float d = act.getResources().getDisplayMetrics().density;
            TextView t = new TextView(act);
            t.setText("☀\n\nDesliza hacia arriba o abajo por este lado para cambiar el brillo\n\n"
                    + "Puedes desactivarlo en Ajustes");
            t.setTextColor(Color.WHITE);
            t.setTextSize(15);
            t.setGravity(Gravity.CENTER);
            t.setPadding((int) (12 * d), 0, (int) (12 * d), 0);
            t.setBackgroundColor(0xB3000000);
            t.setClickable(true);
            t.setOnClickListener(v -> quitarIntro());
            intro = t;
            raiz.addView(t, new FrameLayout.LayoutParams((int) (raiz.getWidth() * ZONA),
                    FrameLayout.LayoutParams.MATCH_PARENT, Gravity.START));
            manejador.postDelayed(this::quitarIntro, 5000);
        }, 1500);
    }

    private void quitarIntro() {
        if (intro == null) return;
        View v = intro;
        intro = null;
        v.animate().alpha(0f).setDuration(300).withEndAction(() -> raiz.removeView(v)).start();
    }

    void liberar() {
        manejador.removeCallbacksAndMessages(null);
    }
}
