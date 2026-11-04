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
import android.webkit.JavascriptInterface;
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

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import com.google.android.material.snackbar.Snackbar;

import py.loboplay.app.R;
import py.loboplay.app.data.AppDatabase;
import py.loboplay.app.data.Sitio;
import py.loboplay.app.util.DetectorVideo;
import py.loboplay.app.util.DialogoBusqueda;
import py.loboplay.app.util.Hilos;
import py.loboplay.app.util.Ajustes;
import py.loboplay.app.util.PaginasVideo;
import py.loboplay.app.util.BloqueadorAnuncios;
import py.loboplay.app.util.Bordes;

// Navegador interno: barra superior, anuncios bloqueados, video a pantalla completa,
// imagen en imagen, modo escritorio y cursor para el control remoto.
public class NavegadorActivity extends AppCompatActivity {
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_ESCRITORIO = "escritorio";
    public static final String EXTRA_BLOQUEAR_ANUNCIOS = "bloquearAnuncios";
    public static final String EXTRA_PAGINA = "pagina";
    public static final String EXTRA_SITIO_ID = "sitioId";
    public static final String EXTRA_LISTA = "listaCanales";

    // Arma el intent para abrir un sitio (con su video si lo tiene) y la lista para cambiar de canal.
    public static android.content.Intent intentPara(android.content.Context ctx, Sitio sitio, long[] lista) {
        android.content.Intent i = new android.content.Intent(ctx, NavegadorActivity.class);
        boolean conVideo = sitio.videoUrl != null && !sitio.videoUrl.trim().isEmpty();
        i.putExtra(EXTRA_URL, conVideo ? sitio.videoUrl : sitio.url);
        i.putExtra(EXTRA_ESCRITORIO, sitio.escritorio);
        i.putExtra(EXTRA_BLOQUEAR_ANUNCIOS, sitio.bloquearAnuncios);
        i.putExtra(EXTRA_PAGINA, sitio.url);
        i.putExtra(EXTRA_SITIO_ID, sitio.id);
        if (lista != null) i.putExtra(EXTRA_LISTA, lista);
        return i;
    }

    // Reproductores de otro servidor (por ejemplo el de Telefuturo): la pagina trae su propia barra,
    // empieza sin sonido y no se ajusta a la pantalla. Este codigo estira el reproductor (con sus
    // botones) para que ocupe toda la pantalla y le activa el sonido las primeras veces.
    private static final String JS_AJUSTAR = "(function(){"
            + "function lpTodos(s,r,a){[].push.apply(a,r.querySelectorAll(s));"
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
            + "return {t:t,v:v};}"
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
            + "if(!window.__lpAjuste){window.__lpAjuste=setInterval(ver,1500);}"
            + "return 'ok';})()";

    private static final int MAX_AJUSTES = 25;   // uno por segundo

    // Pide al reproductor su pantalla completa (la misma que el boton del reproductor).
    private static final String JS_PANTALLA = "(function(){var v=document.querySelector('video');if(!v)return 'no';"
            + "var t=v.closest('[class*=player],[id*=player]')||v;"
            + "try{var f=t.requestFullscreen||t.webkitRequestFullscreen;var p=f.call(t);"
            + "if(p&&p.catch)p.catch(function(){});}catch(e){return 'no';}return 'ok';})()";

    private static final String AGENTE_ESCRITORIO =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private FrameLayout contenedor;
    private View panel;
    private View barra;
    private FrameLayout areaWeb;
    private TextView titulo;
    private WebView web;
    private ProgressBar progreso;

    // Video guardado del sitio: si falla se puede volver a detectar.
    private long sitioId = -1;
    private long[] listaCanales;
    private boolean cambiandoCanal;
    private String videoActual;
    private String paginaSitio;
    private DetectorVideo detector;
    private Snackbar avisoFallo;
    // Barra superior: en los sitios con video se esconde sola y vuelve al tocar el borde de arriba
    // o con la flecha arriba del control remoto.
    private boolean barraAutomatica;
    private boolean barraOculta;
    private final Runnable ocultarBarra = this::ocultarBarraSola;
    // Reproduciendo un .m3u8 en la pagina propia de la app: no hay anuncios que bloquear, y el bloqueador
    // (con una lista grande) podia cortar la lista del video y dar "manifestParsingError (200)".
    private boolean flujoPropio;
    private boolean autoRedetectado;   // ya se intento buscar el video de nuevo solo en esta apertura
    private boolean ajustarVideo;   // el video es un reproductor de otro servidor que hay que ajustar
    private String htmlFlujo;   // pagina propia que reproduce un flujo .m3u8 directo
    private String baseFlujo;
    private View vistaCompleta;
    private WebChromeClient.CustomViewCallback avisoCompleta;

    private CursorView cursor;
    private View btnCursor;
    private boolean esTv;
    private boolean cursorActivo;

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

        titulo.setText(nombreCorto(url));
        findViewById(R.id.btnAtras).setOnClickListener(v -> retroceder());
        findViewById(R.id.btnRecargar).setOnClickListener(v -> {
            if (htmlFlujo != null) cargarFlujo();
            else {
                web.reload();
                programarAjuste();
            }
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
                if (bloquear && !flujoPropio && BloqueadorAnuncios.esAnuncio(r.getUrl().getHost())) {
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

        final String pagina = getIntent().getStringExtra(EXTRA_PAGINA);
        sitioId = getIntent().getLongExtra(EXTRA_SITIO_ID, -1);
        listaCanales = getIntent().getLongArrayExtra(EXTRA_LISTA);
        if (sitioId >= 0 && listaCanales != null && listaCanales.length > 1) {
            View anterior = findViewById(R.id.btnCanalAnterior);
            View siguiente = findViewById(R.id.btnCanalSiguiente);
            anterior.setVisibility(View.VISIBLE);
            siguiente.setVisibility(View.VISIBLE);
            anterior.setOnClickListener(v -> cambiarCanal(-1));
            siguiente.setOnClickListener(v -> cambiarCanal(1));
        }
        if (sitioId >= 0 && pagina != null && !pagina.equals(url)) {
            videoActual = url;
            paginaSitio = pagina;
            barraAutomatica = true;
            // En TV, el canal abre limpio: sin barra ni barras del sistema, y se pide la pantalla completa.
            programarOcultarBarra(esTv ? 1500 : 4000);
            if (esTv) {
                barrasDelSistema(false);
                manejador.postDelayed(() -> pantallaCompletaTv(0), 3500);
            }
            View boton = findViewById(R.id.btnRedetectar);
            boton.setVisibility(View.VISIBLE);
            boton.setOnClickListener(v -> volverADetectar());
        }
        if (PaginasVideo.esFlujoHls(url)) {
            // Direccion directa del video (.m3u8): se reproduce con una pagina propia, sin anuncios ni deteccion.
            flujoPropio = true;
            prepararFlujo(url, pagina);
            // La pagina del reproductor avisa si el video arranca o falla.
            web.addJavascriptInterface(new Object() {
                @JavascriptInterface
                public void ok(String t) {
                    runOnUiThread(() -> {
                        if (avisoFallo != null) avisoFallo.dismiss();
                    });
                }

                @JavascriptInterface
                public void fallo(String t) {
                    runOnUiThread(() -> avisarFallo(t));
                }
            }, "lp");
            cargarFlujo();
        } else if (PaginasVideo.esDailymotion(url)) {
            // Reproductor de Dailymotion: se abre dentro de una pagina propia con el origen del canal.
            prepararMarco(url, pagina);
            cargarFlujo();
            // Un toque en el centro inicia el reproductor incrustado.
            manejador.postDelayed(() -> tocarEn(web.getWidth() / 2f, web.getHeight() / 2f), 3500);
        } else if (pagina != null && !pagina.equals(url)) {
            // El video es de otro servidor: se pide como si lo llamara la pagina del sitio.
            Map<String, String> cabeceras = new HashMap<>();
            cabeceras.put("Referer", pagina);
            web.loadUrl(url, cabeceras);
            ajustarVideo = true;
            programarAjuste();
        } else {
            web.loadUrl(url);
        }
        web.requestFocus();
    }

    // ---------- barra superior que se esconde ----------

    private void programarOcultarBarra(long ms) {
        if (!barraAutomatica) return;
        manejador.removeCallbacks(ocultarBarra);
        manejador.postDelayed(ocultarBarra, ms);
    }

    private void ocultarBarraSola() {
        if (!barraAutomatica || isFinishing() || isDestroyed()) return;
        // Si se esta usando (foco en la barra) o hay un video a pantalla completa, espera.
        if (barra.hasFocus() || vistaCompleta != null) {
            programarOcultarBarra(4000);
            return;
        }
        barraOculta = true;
        barra.setVisibility(View.GONE);
    }

    private void mostrarBarra() {
        barraOculta = false;
        barra.setVisibility(View.VISIBLE);
        programarOcultarBarra(5000);
    }

    // Tocar el borde de arriba vuelve a mostrar la barra; cualquier otro toque reinicia la espera.
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && barraAutomatica && vistaCompleta == null && !enPip()) {
            if (barraOculta) {
                if (ev.getY() < 64 * getResources().getDisplayMetrics().density) {
                    mostrarBarra();
                    return true;
                }
            } else {
                programarOcultarBarra(5000);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    // ---------- pantalla completa automatica en TV ----------

    // Reintenta unas veces (el video tarda en estar listo); se detiene si ya esta en pantalla completa.
    private void pantallaCompletaTv(int intento) {
        if (isFinishing() || isDestroyed() || vistaCompleta != null || intento >= 5) return;
        web.evaluateJavascript(JS_PANTALLA, valor -> {
            if (vistaCompleta != null) return;
            manejador.postDelayed(() -> pantallaCompletaTv(intento + 1), 2000);
        });
    }

    // ---------- ajuste del reproductor ----------

    // Despues de cargar, prueba una vez por segundo hasta que el reproductor quede ajustado.
    private void programarAjuste() {
        if (!ajustarVideo) return;
        manejador.postDelayed(() -> ajustarReproductor(0), 1500);
    }

    private void ajustarReproductor(int intento) {
        if (isFinishing() || isDestroyed() || intento >= MAX_AJUSTES) return;
        web.evaluateJavascript(JS_AJUSTAR, valor -> {
            if (valor != null && valor.contains("ok")) return;
            manejador.postDelayed(() -> ajustarReproductor(intento + 1), 1000);
        });
    }

    // ---------- video que cambio o no carga ----------

    // Avisa que el video no se pudo reproducir y ofrece buscarlo de nuevo.
    private void avisarFallo(String motivo) {
        if (videoActual == null || isFinishing() || isDestroyed()) return;
        // Si la lista del video no sirve (el canal cambio su direccion), lo busca de nuevo solo, una vez.
        if (!autoRedetectado && detector == null && motivo != null
                && (motivo.contains("manifest") || motivo.contains("levelLoad"))) {
            autoRedetectado = true;
            Toast.makeText(this, R.string.video_fallo_auto, Toast.LENGTH_SHORT).show();
            volverADetectar();
            return;
        }
        if (avisoFallo != null && avisoFallo.isShown()) return;
        mostrarBarra();
        avisoFallo = Snackbar.make(contenedor, R.string.video_fallo, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.redetectar_video, v -> volverADetectar());
        avisoFallo.show();
    }

    private androidx.appcompat.app.AlertDialog dialogoBusqueda;

    private void cerrarDialogoBusqueda() {
        if (dialogoBusqueda != null) {
            try {
                dialogoBusqueda.dismiss();
            } catch (Exception ignorar) {
            }
            dialogoBusqueda = null;
        }
    }

    // Busca otra vez el video en la pagina del sitio y, si cambio, lo guarda y lo abre.
    private void volverADetectar() {
        if (detector != null || paginaSitio == null) return;
        if (avisoFallo != null) avisoFallo.dismiss();
        // La pantalla queda bloqueada mientras se busca; Cancelar deja todo como estaba.
        dialogoBusqueda = DialogoBusqueda.mostrar(this, R.string.video_rebuscando, () -> {
            if (detector != null) detector.cancelar();
            detector = null;
        });
        detector = DetectorVideo.buscar(this, paginaSitio, getIntent().getBooleanExtra(EXTRA_BLOQUEAR_ANUNCIOS, true), video -> {
            detector = null;
            cerrarDialogoBusqueda();
            if (isFinishing() || isDestroyed()) return;
            if (video == null) {
                Toast.makeText(this, R.string.video_nuevo_no_hay, Toast.LENGTH_LONG).show();
                htmlFlujo = null;
                barraAutomatica = false;
                mostrarBarra();
                web.loadUrl(paginaSitio);
            } else if (video.equals(videoActual)) {
                Toast.makeText(this, R.string.video_mismo, Toast.LENGTH_SHORT).show();
                if (htmlFlujo != null) cargarFlujo();
                else {
                    web.reload();
                    programarAjuste();
                }
            } else {
                final long id = sitioId;
                final Context app = getApplicationContext();
                Hilos.IO.execute(() -> {
                    Sitio s = AppDatabase.get(app).sitioDao().buscar(id);
                    if (s != null) {
                        s.videoUrl = video;
                        AppDatabase.get(app).sitioDao().actualizar(s);
                    }
                });
                android.content.Intent nuevo = new android.content.Intent(getIntent());
                nuevo.putExtra(EXTRA_URL, video);
                startActivity(nuevo);
                finish();
            }
        });
        if (detector.finalizado()) detector = null;
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
        if (barra != null) barra.setVisibility(enPip || barraOculta ? View.GONE : View.VISIBLE);
        if (cursor != null) cursor.setVisibility(enPip ? View.GONE : (cursorActivo ? View.VISIBLE : View.GONE));
        if (!enPip) despertarCursor();
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
        manejador.removeCallbacks(esconderCursor);
        if (cursorActivo) manejador.postDelayed(esconderCursor, CURSOR_ESPERA_MS);
    }

    // El cursor se ve unos segundos y se esconde; sigue activo y reaparece al apretar cualquier flecha u OK.
    private final Runnable esconderCursor = () -> {
        if (cursor != null && cursorActivo) cursor.setVisibility(View.INVISIBLE);
    };
    private static final long CURSOR_ESPERA_MS = 4000;

    private void despertarCursor() {
        if (cursor == null || !cursorActivo) return;
        cursor.setVisibility(View.VISIBLE);
        manejador.removeCallbacks(esconderCursor);
        manejador.postDelayed(esconderCursor, CURSOR_ESPERA_MS);
    }

    // Pasa al canal anterior (-1) o siguiente (+1) de la lista desde donde se abrio.
    private void cambiarCanal(int paso) {
        if (cambiandoCanal || listaCanales == null || listaCanales.length < 2) return;
        int pos = -1;
        for (int k = 0; k < listaCanales.length; k++) if (listaCanales[k] == sitioId) pos = k;
        if (pos < 0) return;
        cambiandoCanal = true;
        final long destino = listaCanales[(pos + paso + listaCanales.length) % listaCanales.length];
        final android.content.Context app = getApplicationContext();
        Hilos.IO.execute(() -> {
            Sitio nuevo = AppDatabase.get(app).sitioDao().buscar(destino);
            if (nuevo != null) AppDatabase.get(app).sitioDao().marcarAbierto(destino, System.currentTimeMillis());
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (nuevo == null) {
                    cambiandoCanal = false;
                    return;
                }
                Toast.makeText(this, nuevo.nombre, Toast.LENGTH_SHORT).show();
                startActivity(intentPara(this, nuevo, listaCanales));
                overridePendingTransition(0, 0);
                finish();
            });
        });
    }

    // Maneja las flechas y OK del control remoto cuando el cursor esta activo.
    @Override
    public boolean dispatchKeyEvent(KeyEvent ev) {
        if (ev.getAction() == KeyEvent.ACTION_DOWN && ev.getRepeatCount() == 0 && !enPip()) {
            int t = ev.getKeyCode();
            if (t == KeyEvent.KEYCODE_CHANNEL_UP || t == KeyEvent.KEYCODE_PAGE_UP) {
                cambiarCanal(1);
                return true;
            }
            if (t == KeyEvent.KEYCODE_CHANNEL_DOWN || t == KeyEvent.KEYCODE_PAGE_DOWN) {
                cambiarCanal(-1);
                return true;
            }
            // Con las flechas: en el video propio (sin nada que tocar) izquierda/derecha cambian de canal;
            // en una pagina con cursor, al empujar contra el borde izquierdo o derecho de la pantalla.
            boolean izq = t == KeyEvent.KEYCODE_DPAD_LEFT;
            boolean der = t == KeyEvent.KEYCODE_DPAD_RIGHT;
            if ((izq || der) && listaCanales != null && listaCanales.length > 1) {
                boolean directo = flujoPropio && (barraOculta || cursorActivo);
                boolean enBorde = false;
                if (!directo && cursor != null && cursorActivo) {
                    float borde = 4 * getResources().getDisplayMetrics().density;
                    enBorde = izq ? cursor.getPosX() <= borde : cursor.getPosX() >= cursor.getWidth() - borde;
                }
                if (directo || enBorde) {
                    cambiarCanal(izq ? -1 : 1);
                    return true;
                }
            }
        }
        if (ev.getAction() == KeyEvent.ACTION_DOWN && vistaCompleta == null && !enPip()) {
            if (barraOculta && ev.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP && !cursorActivo) {
                mostrarBarra();
                findViewById(R.id.btnAtras).requestFocus();
                return true;
            }
            if (!barraOculta) programarOcultarBarra(5000);
        }
        if (cursor != null && cursorActivo && vistaCompleta == null && !enPip()) {
            int tecla = ev.getKeyCode();
            if (ev.getAction() == KeyEvent.ACTION_DOWN) despertarCursor();
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
                mostrarBarra();
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

    // Toque sobre la pagina en la posicion dada.
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
        barrasDelSistema(true);
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
        if (detector != null) detector.cancelar();
        cerrarDialogoBusqueda();
        manejador.removeCallbacksAndMessages(null);
        if (web != null) {
            ((ViewGroup) web.getParent()).removeView(web);
            web.destroy();
        }
        super.onDestroy();
    }
}
