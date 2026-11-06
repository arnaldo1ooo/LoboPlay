package py.loboplay.app.ui;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import py.loboplay.app.util.Ajustes;

// Gestos del reproductor: deslizar arriba o abajo por el borde izquierdo cambia el brillo
// y por el borde derecho cambia el volumen.
class GestosLaterales {
    // Interfaz para cancelar el toque que ya habia recibido el video.
    interface Cancelar {
        void cancelar(MotionEvent evento);
    }

    private static final int NINGUNO = 0;
    private static final int BRILLO = 1;
    private static final int VOLUMEN = 2;

    private static final float ZONA = 0.2f;     // ancho de cada borde que responde
    private static final float MIN = 0.02f;

    // Se guarda para que el brillo siga igual al cambiar de canal.
    private static float brilloGuardado = -1f;

    private final Activity act;
    private final FrameLayout raiz;
    private final AudioManager audio;
    private final Handler manejador = new Handler(Looper.getMainLooper());
    private final int umbral;
    private TextView indicador;
    private final List<View> intros = new ArrayList<>();
    private ValueAnimator animacion;

    private int gesto = NINGUNO;
    private boolean arrastrando;
    private float inicioY;
    private float inicioX;
    private float valorInicial;

    GestosLaterales(Activity act, FrameLayout raiz) {
        this.act = act;
        this.raiz = raiz;
        this.audio = (AudioManager) act.getSystemService(Context.AUDIO_SERVICE);
        this.umbral = ViewConfiguration.get(act).getScaledTouchSlop();
        if (brilloGuardado >= 0 && Ajustes.gestoBrillo(act)) ponerBrillo(brilloGuardado);
    }

    // Revisa cada toque; devuelve true si lo uso (y entonces no debe llegar al video).
    boolean procesar(MotionEvent ev, int limiteInferior, Cancelar cancelar) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                arrastrando = false;
                gesto = NINGUNO;
                if (ev.getY() < limiteInferior) {
                    if (ev.getX() < raiz.getWidth() * ZONA && Ajustes.gestoBrillo(act)) gesto = BRILLO;
                    else if (ev.getX() > raiz.getWidth() * (1 - ZONA) && Ajustes.gestoVolumen(act)) gesto = VOLUMEN;
                }
                inicioX = ev.getX();
                inicioY = ev.getY();
                valorInicial = gesto == BRILLO ? brilloActual() : volumenActual();
                return false;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (!arrastrando) gesto = NINGUNO;
                return arrastrando;
            case MotionEvent.ACTION_MOVE:
                if (gesto == NINGUNO) return false;
                float dy = ev.getY() - inicioY;
                if (!arrastrando) {
                    float dx = ev.getX() - inicioX;
                    if (Math.abs(dy) < umbral * 2 || Math.abs(dy) < Math.abs(dx)) return false;
                    arrastrando = true;
                    quitarIntros();
                    MotionEvent c = MotionEvent.obtain(ev);
                    c.setAction(MotionEvent.ACTION_CANCEL);
                    cancelar.cancelar(c);
                    c.recycle();
                }
                float v = valorInicial - dy / (raiz.getHeight() * 0.7f);
                if (gesto == BRILLO) {
                    v = Math.max(MIN, Math.min(1f, v));
                    ponerBrillo(v);
                    brilloGuardado = v;
                    mostrar("☀ " + Math.round(v * 100) + "%", Gravity.START);
                } else {
                    v = Math.max(0f, Math.min(1f, v));
                    ponerVolumen(v);
                    mostrar((v == 0f ? "🔇 " : "🔊 ") + Math.round(v * 100) + "%", Gravity.END);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                boolean fue = arrastrando;
                arrastrando = false;
                gesto = NINGUNO;
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

    private void ponerBrillo(float valor) {
        WindowManager.LayoutParams p = act.getWindow().getAttributes();
        p.screenBrightness = valor;
        act.getWindow().setAttributes(p);
    }

    private float volumenActual() {
        if (audio == null) return 0.5f;
        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        return max <= 0 ? 0.5f : audio.getStreamVolume(AudioManager.STREAM_MUSIC) / (float) max;
    }

    private void ponerVolumen(float valor) {
        if (audio == null) return;
        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(valor * max), 0);
    }

    // Muestra el porcentaje un momento, del lado donde se desliza.
    private void mostrar(String texto, int lado) {
        float d = act.getResources().getDisplayMetrics().density;
        if (indicador == null) {
            indicador = new TextView(act);
            indicador.setTextColor(Color.WHITE);
            indicador.setTextSize(18);
            indicador.setPadding((int) (16 * d), (int) (10 * d), (int) (16 * d), (int) (10 * d));
            GradientDrawable fondo = new GradientDrawable();
            fondo.setColor(0xB3000000);
            fondo.setCornerRadius(24 * d);
            indicador.setBackground(fondo);
            raiz.addView(indicador, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        }
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) indicador.getLayoutParams();
        lp.gravity = Gravity.CENTER_VERTICAL | lado;
        lp.leftMargin = (int) (24 * d);
        lp.rightMargin = (int) (24 * d);
        indicador.setLayoutParams(lp);
        indicador.setText(texto);
        indicador.setVisibility(View.VISIBLE);
        indicador.bringToFront();
        manejador.removeCallbacks(esconder);
        manejador.postDelayed(esconder, 900);
    }

    private final Runnable esconder = () -> {
        if (indicador != null) indicador.setVisibility(View.GONE);
    };

    // La primera vez de cada gesto, explica con flechas animadas hacia arriba y abajo donde se desliza.
    void presentarSiHaceFalta() {
        final boolean verBrillo = Ajustes.gestoBrillo(act) && !Ajustes.gestoBrilloVisto(act);
        final boolean verVolumen = Ajustes.gestoVolumen(act) && !Ajustes.gestoVolumenVisto(act);
        if (!verBrillo && !verVolumen) return;
        manejador.postDelayed(() -> {
            if (act.isFinishing() || act.isDestroyed() || !intros.isEmpty()) return;
            List<View[]> flechas = new ArrayList<>();
            if (verBrillo) {
                Ajustes.marcarGestoBrilloVisto(act);
                agregarIntro("☀", "Desliza para cambiar el brillo", Gravity.START, flechas);
            }
            if (verVolumen) {
                Ajustes.marcarGestoVolumenVisto(act);
                agregarIntro("🔊", "Desliza para cambiar el volumen", Gravity.END, flechas);
            }
            final float d = act.getResources().getDisplayMetrics().density;
            animacion = ValueAnimator.ofFloat(0f, 1f);
            animacion.setDuration(700);
            animacion.setRepeatCount(ValueAnimator.INFINITE);
            animacion.setRepeatMode(ValueAnimator.REVERSE);
            animacion.addUpdateListener(a -> {
                float t = (float) a.getAnimatedValue();
                for (View[] par : flechas) {
                    par[0].setTranslationY(-t * 14 * d);
                    par[1].setTranslationY(t * 14 * d);
                    par[0].setAlpha(1f - 0.5f * t);
                    par[1].setAlpha(1f - 0.5f * t);
                }
            });
            animacion.start();
            manejador.postDelayed(this::quitarIntros, 5000);
        }, 1500);
    }

    private void agregarIntro(String icono, String texto, int lado, List<View[]> flechas) {
        float d = act.getResources().getDisplayMetrics().density;
        LinearLayout panel = new LinearLayout(act);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setBackgroundColor(0xB3000000);
        panel.setPadding((int) (6 * d), 0, (int) (6 * d), 0);
        panel.setClickable(true);
        panel.setOnClickListener(v -> quitarIntros());

        TextView arriba = texto(panel, "▲", 26);
        texto(panel, icono, 30).setPadding(0, (int) (10 * d), 0, (int) (10 * d));
        TextView abajo = texto(panel, "▼", 26);
        TextView t = texto(panel, texto, 13);
        t.setPadding(0, (int) (14 * d), 0, 0);

        flechas.add(new View[]{arriba, abajo});
        intros.add(panel);
        raiz.addView(panel, new FrameLayout.LayoutParams((int) (raiz.getWidth() * ZONA),
                FrameLayout.LayoutParams.MATCH_PARENT, lado));
    }

    private TextView texto(LinearLayout padre, String contenido, int tamano) {
        TextView t = new TextView(act);
        t.setText(contenido);
        t.setTextColor(Color.WHITE);
        t.setTextSize(tamano);
        t.setGravity(Gravity.CENTER);
        padre.addView(t, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return t;
    }

    private void quitarIntros() {
        if (animacion != null) {
            animacion.cancel();
            animacion = null;
        }
        for (View v : new ArrayList<>(intros)) {
            v.animate().alpha(0f).setDuration(300).withEndAction(() -> raiz.removeView(v)).start();
        }
        intros.clear();
    }

    void liberar() {
        if (animacion != null) animacion.cancel();
        manejador.removeCallbacksAndMessages(null);
    }
}
