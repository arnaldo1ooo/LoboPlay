package py.loboplay.app.ui;

import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.app.UiModeManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Rational;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import py.loboplay.app.R;
import py.loboplay.app.util.Ajustes;
import py.loboplay.app.util.PaginasVideo;
import py.loboplay.app.util.BloqueadorAnuncios;
import py.loboplay.app.util.Bordes;

// Navegador interno: barra superior, anuncios bloqueados, video a pantalla completa,
// imagen en imagen, modo escritorio y cursor para el control remoto.
public class NavegadorActivity extends AppCompatActivity {
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_ESCRITORIO = "escritorio";
    public static final String EXTRA_PANTALLA_COMPLETA = "pantallaCompleta";
    public static final String EXTRA_BLOQUEAR_ANUNCIOS = "bloquearAnuncios";
    public static final String EXTRA_PAGINA = "pagina";

    // Codigo comun: encuentra el reproductor (un video con tamano, o si no un marco grande).
    // lpCaja devuelve el contenedor del video que incluye sus botones (silencio, pantalla completa...).
    private static final String JS_COMUN = "function lpTodos(s,r,a){[].push.apply(a,r.querySelectorAll(s));"
            + "[].slice.call(r.querySelectorAll('*')).forEach(function(e){if(e.shadowRoot)lpTodos(s,e.shadowRoot,a);});return a;}"
            + "function lpCaja(v){"
            + "var vr=v.getBoundingClientRect();var p=v.parentElement;"
            + "for(var i=0;p&&p!==document.body&&p!==document.documentElement&&i<6;i++,p=p.parentElement){"
            + "var r=p.getBoundingClientRect();"
            + "if(r.height>vr.height*1.4+10||r.width>vr.width*1.15+10)break;"
            + "if(p.querySelector('button,[role=button],input[type=range]'))return p;}"
            + "return v;}"
            + "function lpBuscar(){"
            + "var vs=lpTodos('video',document,[]);"
            + "vs.sort(function(a,b){return b.clientWidth*b.clientHeight-a.clientWidth*a.clientHeight;});"
            + "var fs=[].slice.call(document.querySelectorAll('iframe')).filter(function(f){"
            + "return f.clientWidth>=400&&f.clientHeight>=220;});"
            + "fs.sort(function(a,b){return b.clientWidth*b.clientHeight-a.clientWidth*a.clientHeight;});"
            + "var v=vs[0]&&vs[0].clientWidth>0&&vs[0].clientHeight>0?vs[0]:null,f=fs[0];"
            + "var t=v?lpCaja(v):(f||null);"
            + "return {t:t,v:v,nv:vs.length,nf:fs.length};}";

    // Solo mira: devuelve que encontro (para saber si ya hay reproductor).
    private static final String JS_BUSCAR = "(function(){" + JS_COMUN
            + "var r=lpBuscar();return JSON.stringify({ok:!!r.t,tipo:r.v?'VIDEO':(r.t?'IFRAME':''),nv:r.nv,nf:r.nf});})()";

    // Estira el reproductor (con sus botones) para que ocupe toda la pantalla y lo reproduce.
    // Se repite cada segundo y medio por si la pagina lo acomoda de nuevo. El sonido se activa
    // solo las primeras veces: despues respeta lo que elija el usuario.
    private static final String JS_INMERSIVO = "(function(){" + JS_COMUN
            + "function poner(e,k,v){e.style.setProperty(k,v,'important');}"
            + "function ver(){var r=lpBuscar();var t=r.t;if(!t)return false;"
            + "poner(t,'position','fixed');poner(t,'top','0');poner(t,'left','0');"
            + "poner(t,'width','100vw');poner(t,'height','100vh');poner(t,'max-width','none');"
            + "poner(t,'max-height','none');poner(t,'margin','0');poner(t,'padding','0');"
            + "poner(t,'border','0');poner(t,'z-index','2147483647');poner(t,'background','#000');"
            + "poner(t,'transform','none');"
            + "if(r.v&&t!==r.v){poner(r.v,'width','100%');poner(r.v,'height','100%');}"
            + "if(r.v)poner(r.v,'object-fit','contain');"
            + "for(var p=t.parentElement;p&&p!==document.body;p=p.parentElement){"
            + "poner(p,'transform','none');poner(p,'filter','none');poner(p,'perspective','none');"
            + "poner(p,'contain','none');poner(p,'overflow','visible');}"
            + "poner(document.documentElement,'overflow','hidden');"
            + "poner(document.body,'overflow','hidden');poner(document.body,'background','#000');"
            + "window.__lpN=(window.__lpN||0)+1;"
            + "if(r.v&&window.__lpN<=4){try{r.v.muted=false;if(r.v.paused)r.v.play();}catch(e){}}"
            + "return true;}"
            + "if(!ver())return 'no';"
            + "if(!window.__lpInmersivo){window.__lpInmersivo=setInterval(ver,1500);}"
            + "return 'ok';})()";

    private static final int MAX_BUSQUEDAS = 40;   // una por segundo

    private static final String AGENTE_ESCRITORIO =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private FrameLayout contenedor;
    private View panel;
    private View barra;
    private FrameLayout areaWeb;
    private TextView titulo;
    private WebView web;
    private ProgressBar progreso;
    private View portada;
    private String htmlFlujo;   // pagina propia que reproduce un flujo .m3u8 directo
    private String baseFlujo;
    private View vistaCompleta;
    private WebChromeClient.CustomViewCallback avisoCompleta;

    private CursorView cursor;
    private View btnCursor;
    private boolean esTv;
    private boolean cursorActivo;

    // Pantalla completa automatica (canales de TV)
    private boolean autoCompleta;
    private int busquedas;
    private boolean inmersivo;      // el reproductor ya ocupa toda la pantalla
    private TextView estadoPortada;
    private final Handler manejador = new Handler(Looper.getMainLooper());

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navegador);
        getWindow().getDecorView().setBackgroundColor(android.graphics.Color.BLACK);
        Bordes.aplicar(this, true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null) {
            finish();
            return;
        }

        BloqueadorAnuncios.cargar(this);
        // Los anuncios se bloquean salvo en los sitios que lo desactivan (algunos canales
        // no reproducen si detectan el bloqueador).
        final boolean bloquear = getIntent().getBooleanExtra(EXTRA_BLOQUEAR_ANUNCIOS, true);

        contenedor = findViewById(R.id.contenedor);
        panel = findViewById(R.id.panel);
        barra = findViewById(R.id.barra);
        areaWeb = findViewById(R.id.areaWeb);
        titulo = findViewById(R.id.titulo);
        web = findViewById(R.id.web);
        progreso = findViewById(R.id.progreso);
        btnCursor = findViewById(R.id.btnCursor);
        portada = findViewById(R.id.portada);

        titulo.setText(nombreCorto(url));
        findViewById(R.id.btnAtras).setOnClickListener(v -> retroceder());
        findViewById(R.id.btnRecargar).setOnClickListener(v -> {
            if (htmlFlujo != null) cargarFlujo();
            else web.reload();
        });
        findViewById(R.id.btnCerrar).setOnClickListener(v -> finish());

        WebSettings ajustes = web.getSettings();
        ajustes.setJavaScriptEnabled(true);
        ajustes.setDomStorageEnabled(true);
        ajustes.setMediaPlaybackRequiresUserGesture(false);
        ajustes.setSupportMultipleWindows(false);
        ajustes.setJavaScriptCanOpenWindowsAutomatically(false);
        if (getIntent().getBooleanExtra(EXTRA_ESCRITORIO, false)) {
            ajustes.setUserAgentString(AGENTE_ESCRITORIO);
            ajustes.setUseWideViewPort(true);
            ajustes.setLoadWithOverviewMode(true);
        }

        web.setWebViewClient(new WebViewClient() {
            // Muestra la direccion de la pagina que se esta cargando.
            @Override
            public void onPageStarted(WebView v, String direccion, Bitmap icono) {
                titulo.setText(nombreCorto(direccion));
            }

            // Corta los pedidos a dominios de publicidad.
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                if (bloquear && BloqueadorAnuncios.esAnuncio(r.getUrl().getHost())) {
                    return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
                }
                return super.shouldInterceptRequest(v, r);
            }

            // Evita saltos a otras apps o a sitios de publicidad.
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                if (bloquear && BloqueadorAnuncios.esAnuncio(r.getUrl().getHost())) return true;
                String esquema = r.getUrl().getScheme();
                return !("http".equals(esquema) || "https".equals(esquema));
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int avance) {
                progreso.setProgress(avance);
                progreso.setVisibility(avance < 100 ? View.VISIBLE : View.GONE);
            }

            // Cambia el titulo de la barra por el de la pagina.
            @Override
            public void onReceivedTitle(WebView v, String texto) {
                if (texto != null && !texto.trim().isEmpty()) titulo.setText(texto);
            }

            // Video a pantalla completa.
            @Override
            public void onShowCustomView(View vista, CustomViewCallback aviso) {
                if (vistaCompleta != null) {
                    aviso.onCustomViewHidden();
                    return;
                }
                vistaCompleta = vista;
                avisoCompleta = aviso;
                contenedor.addView(vista, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                panel.setVisibility(View.GONE);
                portada.setVisibility(View.GONE);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                barrasDelSistema(false);
            }

            @Override
            public void onHideCustomView() {
                salirPantallaCompleta();
            }

            // No deja que la pagina abra ventanas emergentes.
            @Override
            public boolean onCreateWindow(WebView v, boolean dialogo, boolean gesto, android.os.Message msg) {
                return false;
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                retroceder();
            }
        });

        prepararImagenEnImagen();
        prepararCursor();

        autoCompleta = getIntent().getBooleanExtra(EXTRA_PANTALLA_COMPLETA, false);
        if (autoCompleta) {
            // Tapa la pagina con el logo hasta que el video quede en pantalla completa.
            estadoPortada = findViewById(R.id.estadoPortada);
            portada.setVisibility(View.VISIBLE);
            portada.setOnClickListener(v -> abandonarAutomatico());
            barrasDelSistema(false);
            manejador.postDelayed(this::buscarReproductor, 1500);
        }

        final String pagina = getIntent().getStringExtra(EXTRA_PAGINA);
        if (PaginasVideo.esFlujoHls(url)) {
            // Direccion directa del video (.m3u8): se reproduce con una pagina propia, sin anuncios ni deteccion.
            prepararFlujo(url, pagina);
            cargarFlujo();
        } else if (PaginasVideo.esDailymotion(url)) {
            // Reproductor de Dailymotion: se abre dentro de una pagina propia con el origen del canal.
            prepararMarco(url, pagina);
            cargarFlujo();
        } else if (pagina != null && !pagina.equals(url)) {
            // El video es de otro servidor: se pide como si lo llamara la pagina del sitio.
            Map<String, String> cabeceras = new HashMap<>();
            cabeceras.put("Referer", pagina);
            web.loadUrl(url, cabeceras);
        } else {
            web.loadUrl(url);
        }
        web.requestFocus();
    }

    // ---------- flujo directo (.m3u8) ----------

    // Prepara la pagina propia del reproductor (flujo .m3u8 o marco de Dailymotion).
    private void prepararMarco(String reproductor, String pagina) {
        baseFlujo = PaginasVideo.baseDe(reproductor, pagina);
        htmlFlujo = PaginasVideo.marco(reproductor, false);
    }

    private void prepararFlujo(String flujo, String pagina) {
        baseFlujo = PaginasVideo.baseDe(flujo, pagina);
        htmlFlujo = PaginasVideo.flujo(this, flujo, false);
    }

    private void cargarFlujo() {
        web.loadDataWithBaseURL(baseFlujo, htmlFlujo, "text/html", "utf-8", null);
    }

    // ---------- pantalla completa automatica ----------

    // Espera a que aparezca el reproductor y lo estira a toda la pantalla, sin que el usuario toque nada.
    private void buscarReproductor() {
        if (!autoCompleta || inmersivo || isFinishing() || isDestroyed()) return;
        if (busquedas++ >= MAX_BUSQUEDAS) {
            abandonarAutomatico();
            Toast.makeText(this, "No se encontró el reproductor en la página", Toast.LENGTH_LONG).show();
            return;
        }
        web.evaluateJavascript(JS_BUSCAR, valor -> {
            if (!autoCompleta || inmersivo) return;
            String texto = textoJs(valor);
            try {
                JSONObject o = new JSONObject(texto);
                if (o.getBoolean("ok")) {
                    entrarInmersivo(o.getString("tipo"));
                    return;
                }
                if (busquedas >= 6) {
                    estadoPortada.setText("Buscando el reproductor… (videos: " + o.optInt("nv")
                            + ", marcos: " + o.optInt("nf") + ")\nToca la pantalla para ver la página");
                }
            } catch (Exception ignorado) {
                // la pagina todavia no esta lista
            }
            manejador.postDelayed(this::buscarReproductor, 1000);
        });
    }

    // Pone el reproductor a pantalla completa dentro de la pagina y oculta lo demas.
    private void entrarInmersivo(String tipo) {
        web.evaluateJavascript(JS_INMERSIVO, valor -> {
            if (!autoCompleta || inmersivo) return;
            if (!"ok".equals(textoJs(valor))) {
                manejador.postDelayed(this::buscarReproductor, 1000);
                return;
            }
            inmersivo = true;
            barra.setVisibility(View.GONE);
            portada.setVisibility(View.GONE);
            barrasDelSistema(false);
            web.requestFocus();
            // Si el reproductor es un marco (otra pagina), un toque en el centro lo inicia.
            if ("IFRAME".equals(tipo)) {
                manejador.postDelayed(() -> tocarEn(web.getWidth() / 2f, web.getHeight() / 2f), 3000);
            }
        });
    }

    // Si no se logra (o el usuario toca la portada), muestra la pagina normal.
    private void abandonarAutomatico() {
        autoCompleta = false;
        portada.setVisibility(View.GONE);
        barra.setVisibility(View.VISIBLE);
        barrasDelSistema(true);
    }

    // Convierte lo que devuelve evaluateJavascript (texto JSON) en un String normal.
    private static String textoJs(String valor) {
        if (valor == null || "null".equals(valor)) return "";
        try {
            Object o = new JSONTokener(valor).nextValue();
            return o instanceof String ? (String) o : "";
        } catch (Exception e) {
            return "";
        }
    }

    // ---------- imagen en imagen ----------

    // Dice si el dispositivo permite imagen en imagen.
    private boolean pipDisponible() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
    }

    // Muestra el boton de imagen en imagen si esta permitido en ajustes.
    private void prepararImagenEnImagen() {
        View boton = findViewById(R.id.btnPip);
        if (pipDisponible() && Ajustes.pipActivado(this)) {
            boton.setVisibility(View.VISIBLE);
            boton.setOnClickListener(v -> entrarPip());
        }
    }

    // Pasa a la ventana pequena.
    private void entrarPip() {
        if (!pipDisponible()) return;
        try {
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(16, 9))
                    .build();
            enterPictureInPictureMode(params);
        } catch (IllegalStateException ignorado) {
            Toast.makeText(this, "No se pudo abrir la ventana pequeña", Toast.LENGTH_SHORT).show();
        }
    }

    // Hay video en pantalla completa o algo sonando.
    private boolean hayVideo() {
        if (vistaCompleta != null) return true;
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        return audio != null && audio.isMusicActive();
    }

    // Al salir de la app con un video en marcha, sigue en ventana pequena.
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Ajustes.pipActivado(this) && pipDisponible() && hayVideo()) entrarPip();
    }

    @Override
    public void onPictureInPictureModeChanged(boolean enPip, Configuration config) {
        super.onPictureInPictureModeChanged(enPip, config);
        if (barra != null) barra.setVisibility(enPip || inmersivo ? View.GONE : View.VISIBLE);
        if (cursor != null) cursor.setVisibility(enPip ? View.GONE : (cursorActivo ? View.VISIBLE : View.GONE));
    }

    private boolean enPip() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode();
    }

    // ---------- cursor para control remoto ----------

    // Prepara el cursor si el dispositivo es una TV (o no tiene pantalla tactil).
    private void prepararCursor() {
        UiModeManager modo = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        esTv = (modo != null && modo.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION)
                || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
        if (!esTv) return;

        cursor = new CursorView(this);
        areaWeb.addView(cursor, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        btnCursor.setVisibility(View.VISIBLE);
        btnCursor.setOnClickListener(v -> alternarCursor());
        cursorActivo = Ajustes.cursorTv(this);
        aplicarCursor();
    }

    // Activa o desactiva el cursor.
    private void alternarCursor() {
        cursorActivo = !cursorActivo;
        aplicarCursor();
        if (cursorActivo) {
            web.requestFocus();
            Toast.makeText(this, "Cursor activado. Flechas para mover, OK para tocar.", Toast.LENGTH_SHORT).show();
        }
    }

    private void aplicarCursor() {
        if (cursor != null) cursor.setVisibility(cursorActivo ? View.VISIBLE : View.GONE);
        btnCursor.setSelected(cursorActivo);
    }

    // Maneja las flechas y OK del control remoto cuando el cursor esta activo.
    @Override
    public boolean dispatchKeyEvent(KeyEvent ev) {
        // Con la portada puesta, OK o Enter muestran la pagina normal.
        if (portada != null && portada.getVisibility() == View.VISIBLE && ev.getAction() == KeyEvent.ACTION_UP
                && (ev.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || ev.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            abandonarAutomatico();
            return true;
        }
        if (cursor != null && cursorActivo && vistaCompleta == null && !enPip()) {
            int tecla = ev.getKeyCode();
            boolean flecha = tecla == KeyEvent.KEYCODE_DPAD_UP || tecla == KeyEvent.KEYCODE_DPAD_DOWN
                    || tecla == KeyEvent.KEYCODE_DPAD_LEFT || tecla == KeyEvent.KEYCODE_DPAD_RIGHT;
            if (flecha) {
                if (ev.getAction() == KeyEvent.ACTION_DOWN) moverCursor(tecla, ev.getRepeatCount());
                return true;
            }
            if (tecla == KeyEvent.KEYCODE_DPAD_CENTER || tecla == KeyEvent.KEYCODE_ENTER
                    || tecla == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                if (ev.getAction() == KeyEvent.ACTION_UP) tocarConCursor();
                return true;
            }
        }
        if (cursor != null && ev.getKeyCode() == KeyEvent.KEYCODE_MENU
                && ev.getAction() == KeyEvent.ACTION_DOWN && ev.getRepeatCount() == 0) {
            alternarCursor();
            return true;
        }
        return super.dispatchKeyEvent(ev);
    }

    // Mueve el cursor; en los bordes baja o sube la pagina.
    private void moverCursor(int tecla, int repeticiones) {
        float d = getResources().getDisplayMetrics().density;
        float paso = 10 * d * (1 + Math.min(repeticiones, 12) / 3f);
        float borde = 4 * d;

        if (tecla == KeyEvent.KEYCODE_DPAD_UP && cursor.getPosY() <= borde) {
            if (web.getScrollY() > 0) {
                web.scrollBy(0, -(int) (paso * 2));
            } else {
                // Arriba del todo: pasa el foco a la barra superior.
                cursorActivo = false;
                aplicarCursor();
                btnCursor.requestFocus();
            }
            return;
        }
        if (tecla == KeyEvent.KEYCODE_DPAD_DOWN && cursor.getPosY() >= cursor.getHeight() - borde) {
            web.scrollBy(0, (int) (paso * 2));
            return;
        }
        float dx = 0;
        float dy = 0;
        if (tecla == KeyEvent.KEYCODE_DPAD_UP) dy = -paso;
        else if (tecla == KeyEvent.KEYCODE_DPAD_DOWN) dy = paso;
        else if (tecla == KeyEvent.KEYCODE_DPAD_LEFT) dx = -paso;
        else dx = paso;
        cursor.mover(dx, dy);
    }

    // Hace un toque en el lugar donde esta el cursor.
    private void tocarConCursor() {
        tocarEn(cursor.getPosX(), cursor.getPosY());
    }

    // Toque sobre la pagina en la posicion dada (sirve aunque la portada la tape).
    private void tocarEn(final float x, final float y) {
        long t = SystemClock.uptimeMillis();
        MotionEvent abajo = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0);
        web.dispatchTouchEvent(abajo);
        abajo.recycle();
        manejador.postDelayed(() -> {
            long t2 = SystemClock.uptimeMillis();
            MotionEvent arriba = MotionEvent.obtain(t2, t2, MotionEvent.ACTION_UP, x, y, 0);
            web.dispatchTouchEvent(arriba);
            arriba.recycle();
        }, 60);
    }

    // ---------- navegacion ----------

    // Vuelve a la pagina anterior, o cierra el navegador si no hay mas.
    private void retroceder() {
        if (vistaCompleta != null) {
            salirPantallaCompleta();
        } else if (inmersivo) {
            // En los canales con pantalla completa automatica, atras cierra el canal.
            finish();
        } else if (web.canGoBack()) {
            web.goBack();
        } else {
            finish();
        }
    }

    // Saca solo el nombre del sitio de una direccion.
    private String nombreCorto(String direccion) {
        String host = Uri.parse(direccion).getHost();
        return host == null ? direccion : host;
    }

    // Vuelve del video a pantalla completa a la pagina.
    private void salirPantallaCompleta() {
        if (vistaCompleta == null) return;
        contenedor.removeView(vistaCompleta);
        vistaCompleta = null;
        if (avisoCompleta != null) avisoCompleta.onCustomViewHidden();
        avisoCompleta = null;
        panel.setVisibility(View.VISIBLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        barrasDelSistema(!inmersivo);
    }

    // Muestra u oculta las barras del sistema.
    private void barrasDelSistema(boolean mostrar) {
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (mostrar) c.show(WindowInsetsCompat.Type.systemBars());
        else c.hide(WindowInsetsCompat.Type.systemBars());
    }

    // La pagina se pausa solo si la app deja de verse (no en la ventana pequena).
    @Override
    protected void onStop() {
        super.onStop();
        if (web != null && !enPip()) web.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (web != null) web.onResume();
    }

    @Override
    protected void onDestroy() {
        manejador.removeCallbacksAndMessages(null);
        if (web != null) {
            ((ViewGroup) web.getParent()).removeView(web);
            web.destroy();
        }
        super.onDestroy();
    }
}
