package py.loboplay.app.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.HashMap;
import java.util.Map;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import py.loboplay.app.R;
import py.loboplay.app.data.AppDatabase;
import py.loboplay.app.data.IconoHelper;
import py.loboplay.app.data.Sitio;
import py.loboplay.app.data.SitioDao;
import py.loboplay.app.util.Bordes;
import py.loboplay.app.util.DetectorVideo;
import py.loboplay.app.util.Hilos;
import py.loboplay.app.util.PaginasVideo;
import py.loboplay.app.util.Paises;

// Formulario para agregar o editar un sitio.
public class AgregarActivity extends AppCompatActivity {
    public static final String EXTRA_ID = "id";
    private static final long ESPERA_MS = 900;
    private static final long AVISO_LENTO_MS = 8000;

    private EditText campoNombre;
    private EditText campoUrl;
    private EditText campoVideo;
    private MaterialAutoCompleteTextView campoPais;
    private ImageView vistaIcono;
    private TextView estado;
    private View cargando;
    private View progresoIcono;
    private CompoundButton switchEscritorio;
    private CompoundButton switchPantallaCompleta;
    private CompoundButton switchBloquearAnuncios;

    private long idEdicion = -1;
    private String iconoPropio;
    private String iconoWebActual;
    private boolean refrescarWeb = false;

    // Lo que se averiguo de la pagina mientras se escribia la direccion.
    private Bitmap iconoSugerido;
    private String urlSugerida;
    private String ultimaConsulta;
    private int solicitud = 0;
    private boolean nombreEditado = false;
    private boolean poniendoNombre = false;

    // Pais elegido (codigo) y video detectado en la pagina.
    private String paisElegido;
    private boolean paisEditado = false;
    private String videoDetectado;
    private DetectorVideo detector;

    // Vista previa del video detectado.
    private View bloquePrueba;
    private android.widget.FrameLayout cajaPrueba;
    private TextView estadoPrueba;
    private View btnSinVideo;
    private WebView webPrueba;
    private int pruebaNumero = 0;
    private static final long PRUEBA_LIMITE_MS = 20000;
    private final Runnable pruebaVencida = this::pruebaSinConfirmar;
    private List<Paises.Pais> paises;

    private final Handler manejador = new Handler(Looper.getMainLooper());
    private final Runnable consultar = this::consultarPagina;
    private final Runnable avisoLento = this::avisarBusquedaLenta;

    private final ActivityResultLauncher<String> selector = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) cargarIconoPropio(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar);
        Bordes.aplicar(this, false);

        campoNombre = findViewById(R.id.campoNombre);
        campoUrl = findViewById(R.id.campoUrl);
        campoVideo = findViewById(R.id.campoVideo);
        campoPais = findViewById(R.id.campoPais);
        vistaIcono = findViewById(R.id.vistaIcono);
        estado = findViewById(R.id.estado);
        cargando = findViewById(R.id.cargando);
        progresoIcono = findViewById(R.id.progresoIcono);
        switchEscritorio = findViewById(R.id.switchEscritorio);
        switchPantallaCompleta = findViewById(R.id.switchPantallaCompleta);
        switchBloquearAnuncios = findViewById(R.id.switchBloquearAnuncios);
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());

        // Si el usuario escribe el nombre a mano, ya no se lo cambiamos.
        campoNombre.addTextChangedListener(new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!poniendoNombre) nombreEditado = true;
            }
        });

        // Al escribir o pegar la direccion, espera un momento y busca la pagina.
        campoUrl.addTextChangedListener(new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                manejador.removeCallbacks(consultar);
                manejador.postDelayed(consultar, ESPERA_MS);
            }
        });
        prepararPaises();
        bloquePrueba = findViewById(R.id.bloquePrueba);
        cajaPrueba = findViewById(R.id.cajaPrueba);
        estadoPrueba = findViewById(R.id.estadoPrueba);
        btnSinVideo = findViewById(R.id.btnSinVideo);
        findViewById(R.id.btnProbar).setOnClickListener(v -> probarVideo());
        findViewById(R.id.btnRedetectar).setOnClickListener(v -> {
            String pagina = normalizar(campoUrl.getText().toString());
            if (pagina == null) {
                campoUrl.setError("La dirección no es válida");
                return;
            }
            buscarVideo(pagina);
        });
        btnSinVideo.setOnClickListener(v -> {
            cerrarPrueba();
            ponerVideo(null, R.string.video_no_encontrado);
        });
        campoUrl.setOnFocusChangeListener((v, tieneFoco) -> {
            if (!tieneFoco) {
                manejador.removeCallbacks(consultar);
                consultarPagina();
            }
        });
        campoUrl.setOnEditorActionListener((v, accion, evento) -> {
            manejador.removeCallbacks(consultar);
            consultarPagina();
            return false;
        });

        idEdicion = getIntent().getLongExtra(EXTRA_ID, -1);
        if (idEdicion >= 0) {
            ((TextView) findViewById(R.id.titulo)).setText(R.string.editar_sitio);
            Hilos.IO.execute(() -> {
                Sitio s = AppDatabase.get(getApplicationContext()).sitioDao().buscar(idEdicion);
                if (s != null) runOnUiThread(() -> rellenar(s));
            });
        }

        findViewById(R.id.btnCambiarIcono).setOnClickListener(v -> selector.launch("image/*"));
        findViewById(R.id.btnIconoPagina).setOnClickListener(v -> {
            iconoPropio = null;
            refrescarWeb = true;
            if (iconoSugerido != null) vistaIcono.setImageBitmap(iconoSugerido);
            else mostrarIcono(iconoWebActual);
            Toast.makeText(this, "Se usará el icono de la página al guardar", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnCancelar).setOnClickListener(v -> finish());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardar());
    }

    // Busca el titulo y el icono de la pagina para sugerirlos (sin frenar la pantalla).
    private void consultarPagina() {
        final String url = normalizar(campoUrl.getText().toString());
        if (url == null || url.equals(ultimaConsulta)) return;
        ultimaConsulta = url;
        final int mia = ++solicitud;
        estado.setText(R.string.buscando_info);
        mostrarCarga(true);
        manejador.removeCallbacks(avisoLento);
        manejador.postDelayed(avisoLento, AVISO_LENTO_MS);
        buscarVideo(url);
        Hilos.RED.execute(() -> {
            IconoHelper.Pagina pagina = IconoHelper.leerPagina(url);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || mia != solicitud) return;
                manejador.removeCallbacks(avisoLento);
                mostrarCarga(false);
                aplicarSugerencia(url, pagina);
            });
        });
    }

    // Lista de paises (desplegable) y sugerencia segun el final de la direccion (.com.py -> Paraguay).
    private void prepararPaises() {
        paises = Paises.todos();
        List<String> etiquetas = new ArrayList<>();
        for (Paises.Pais p : paises) etiquetas.add(p.etiqueta());
        campoPais.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, etiquetas));
        campoPais.setOnItemClickListener((padre, v, pos, id) -> {
            paisElegido = paises.get(pos).codigo;
            paisEditado = true;
        });
    }

    private void ponerPais(String codigo) {
        paisElegido = codigo;
        campoPais.setText(codigo == null ? "" : Paises.etiqueta(codigo), false);
    }

    // Busca en segundo plano el video de la pagina; el campo queda bloqueado y se llena solo.
    private void buscarVideo(String pagina) {
        if (detector != null) detector.cancelar();
        cerrarPrueba();
        ponerVideo(null, R.string.video_buscando);
        detector = DetectorVideo.buscar(this, pagina, video -> {
            if (isFinishing() || isDestroyed()) return;
            ponerVideo(video, video == null ? R.string.video_no_encontrado : R.string.video_encontrado);
        });
    }

    // Deja el video en el campo bloqueado y muestra u oculta el boton de prueba.
    private void ponerVideo(String video, int mensaje) {
        videoDetectado = video;
        campoVideo.setText(video == null ? "" : video);
        campoVideo.setHint(mensaje);
        bloquePrueba.setVisibility(video == null ? View.GONE : View.VISIBLE);
    }

    // Reproduce el video detectado, sin sonido, en un recuadro del formulario.
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void probarVideo() {
        final String video = videoDetectado;
        if (video == null) return;
        cerrarPrueba();
        final int mia = ++pruebaNumero;
        String pagina = normalizar(campoUrl.getText().toString());
        estadoPrueba.setVisibility(View.VISIBLE);
        estadoPrueba.setText(R.string.prueba_cargando);
        btnSinVideo.setVisibility(View.GONE);
        cajaPrueba.setVisibility(View.VISIBLE);
        cajaPrueba.getLayoutParams().height = (int) (getResources().getDisplayMetrics().widthPixels * 0.9f * 9f / 16f);

        webPrueba = new WebView(this);
        cajaPrueba.addView(webPrueba, new ViewGroup.LayoutParams(-1, -1));
        WebSettings ws = webPrueba.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        webPrueba.setWebViewClient(new WebViewClient());
        webPrueba.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void ok(String t) {
                runOnUiThread(() -> pruebaListo(mia, true, null));
            }

            @JavascriptInterface
            public void fallo(String t) {
                runOnUiThread(() -> pruebaListo(mia, false, t));
            }
        }, "lp");

        String base = PaginasVideo.baseDe(video, pagina);
        if (PaginasVideo.esFlujoHls(video)) {
            webPrueba.loadDataWithBaseURL(base, PaginasVideo.flujo(this, video, true), "text/html", "utf-8", null);
        } else if (PaginasVideo.esDailymotion(video)) {
            webPrueba.loadDataWithBaseURL(base, PaginasVideo.marco(video, true), "text/html", "utf-8", null);
        } else {
            Map<String, String> cabeceras = new HashMap<>();
            if (pagina != null) cabeceras.put("Referer", pagina);
            webPrueba.loadUrl(video, cabeceras);
            sondearVideoPropio(mia);
        }
        manejador.postDelayed(pruebaVencida, PRUEBA_LIMITE_MS);
    }

    // Para paginas de reproductor: mira si algun <video> de la pagina ya avanza.
    private void sondearVideoPropio(int mia) {
        manejador.postDelayed(() -> {
            if (mia != pruebaNumero || webPrueba == null) return;
            webPrueba.evaluateJavascript("(function(){var l=document.querySelectorAll('video');"
                    + "for(var i=0;i<l.length;i++){if(l[i].currentTime>0&&!l[i].paused)return 1;}return 0;})()", valor -> {
                if (mia != pruebaNumero) return;
                if ("1".equals(valor)) pruebaListo(mia, true, null);
                else sondearVideoPropio(mia);
            });
        }, 1500);
    }

    private void pruebaListo(int mia, boolean bien, String error) {
        if (mia != pruebaNumero || estadoPrueba == null) return;
        manejador.removeCallbacks(pruebaVencida);
        if (bien) {
            estadoPrueba.setText(R.string.prueba_ok);
            btnSinVideo.setVisibility(View.GONE);
        } else {
            estadoPrueba.setText(getString(R.string.prueba_fallo) + (error == null ? "" : error));
            btnSinVideo.setVisibility(View.VISIBLE);
        }
    }

    // Paso el tiempo sin que el video avance (puede ser un marco que no se puede leer desde afuera).
    private void pruebaSinConfirmar() {
        if (webPrueba == null) return;
        if (estadoPrueba.getText().toString().equals(getString(R.string.prueba_ok))) return;
        estadoPrueba.setText(R.string.prueba_dudosa);
        btnSinVideo.setVisibility(View.VISIBLE);
    }

    // Detiene y quita la vista previa.
    private void cerrarPrueba() {
        pruebaNumero++;
        manejador.removeCallbacks(pruebaVencida);
        if (webPrueba != null) {
            try {
                webPrueba.stopLoading();
                webPrueba.loadUrl("about:blank");
                cajaPrueba.removeView(webPrueba);
                webPrueba.destroy();
            } catch (Exception ignorar) {
            }
            webPrueba = null;
        }
        cajaPrueba.setVisibility(View.GONE);
        estadoPrueba.setVisibility(View.GONE);
        btnSinVideo.setVisibility(View.GONE);
    }

    // Si la pagina tarda, avisa que se puede guardar igual.
    private void avisarBusquedaLenta() {
        estado.setText(R.string.busqueda_lenta);
    }

    // Deja de esperar el resultado de la busqueda.
    private void cancelarBusqueda() {
        solicitud++;
        manejador.removeCallbacks(consultar);
        manejador.removeCallbacks(avisoLento);
        mostrarCarga(false);
    }

    // Muestra u oculta el indicador de carga (barra y circulo sobre el icono).
    private void mostrarCarga(boolean activa) {
        cargando.setVisibility(activa ? View.VISIBLE : View.GONE);
        progresoIcono.setVisibility(activa && iconoPropio == null ? View.VISIBLE : View.GONE);
    }

    // Pone el titulo y el icono encontrados, sin pisar lo que eligio el usuario.
    private void aplicarSugerencia(String url, IconoHelper.Pagina pagina) {
        urlSugerida = url;
        if (!paisEditado) ponerPais(Paises.deDominio(url));
        iconoSugerido = pagina.icono;

        boolean nombreVacio = campoNombre.getText().toString().trim().isEmpty();
        if (pagina.titulo != null && (!nombreEditado || nombreVacio)) {
            poniendoNombre = true;
            campoNombre.setText(pagina.titulo);
            campoNombre.setSelection(campoNombre.getText().length());
            poniendoNombre = false;
            nombreEditado = false;
        }
        if (iconoPropio == null && iconoSugerido != null) {
            vistaIcono.setImageBitmap(iconoSugerido);
        }
    }

    // Completa el formulario con un sitio existente.
    private void rellenar(Sitio s) {
        ultimaConsulta = s.url;
        campoNombre.setText(s.nombre);
        nombreEditado = true;
        campoUrl.setText(s.url);
        ponerVideo(s.videoUrl, s.videoUrl == null ? R.string.video_no_encontrado : R.string.video_encontrado);
        paisEditado = s.pais != null;
        ponerPais(s.pais);
        switchEscritorio.setChecked(s.escritorio);
        switchPantallaCompleta.setChecked(s.pantallaCompleta);
        switchBloquearAnuncios.setChecked(s.bloquearAnuncios);
        iconoPropio = s.iconoPropio;
        iconoWebActual = s.iconoWeb;
        mostrarIcono(iconoPropio != null ? iconoPropio : iconoWebActual);
    }

    // Muestra el icono elegido en la vista previa.
    private void mostrarIcono(String ruta) {
        if (ruta != null && new File(ruta).exists()) {
            vistaIcono.setImageBitmap(BitmapFactory.decodeFile(ruta));
        } else {
            vistaIcono.setImageDrawable(null);
        }
    }

    // Copia la imagen elegida a la carpeta de la app.
    private void cargarIconoPropio(Uri uri) {
        Hilos.IO.execute(() -> {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                Bitmap bmp = BitmapFactory.decodeStream(in);
                if (bmp == null) throw new IllegalStateException("sin imagen");
                String ruta = IconoHelper.guardar(getApplicationContext(), bmp, "propio");
                runOnUiThread(() -> {
                    iconoPropio = ruta;
                    mostrarIcono(ruta);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "No se pudo leer la imagen", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // Agrega https:// si falta y revisa de forma simple que sea una direccion valida.
    private String normalizar(String texto) {
        String t = texto.trim();
        if (t.isEmpty() || t.contains(" ")) return null;
        String minusculas = t.toLowerCase(Locale.ROOT);
        if (!minusculas.startsWith("http://") && !minusculas.startsWith("https://")) t = "https://" + t;
        String host = Uri.parse(t).getHost();
        if (host == null || !host.contains(".") || host.startsWith(".") || host.endsWith(".")) return null;
        return t;
    }

    // Si no hay nombre, usa el de la direccion (por ejemplo: Telefuturo).
    private String nombreDesdeUrl(String url) {
        String host = Uri.parse(url).getHost();
        if (host == null) return url;
        if (host.startsWith("www.")) host = host.substring(4);
        int punto = host.indexOf('.');
        String base = punto > 0 ? host.substring(0, punto) : host;
        if (base.isEmpty()) return host;
        return base.substring(0, 1).toUpperCase(Locale.ROOT) + base.substring(1);
    }

    // Guarda el sitio y deja listo su icono (el elegido o el de la pagina).
    private void guardar() {
        final String url = normalizar(campoUrl.getText().toString());
        if (url == null) {
            campoUrl.setError("La dirección no es válida");
            return;
        }
        String escrito = campoNombre.getText().toString().trim();
        final String nombre = escrito.isEmpty() ? nombreDesdeUrl(url) : escrito;
        final String fuente = url;
        final String video = videoDetectado;
        final String pais = paisElegido != null ? paisElegido : Paises.deDominio(url);
        cancelarBusqueda();
        findViewById(R.id.btnGuardar).setEnabled(false);

        final String propio = iconoPropio;
        final boolean forzarWeb = refrescarWeb;
        final long id = idEdicion;
        final boolean escritorio = switchEscritorio.isChecked();
        final boolean pantallaCompleta = switchPantallaCompleta.isChecked();
        final boolean bloquearAnuncios = switchBloquearAnuncios.isChecked();
        final Context app = getApplicationContext();
        // Icono que ya se bajo mientras se escribia la direccion (si coincide).
        final Bitmap sugerido = (propio == null && fuente.equals(urlSugerida)) ? iconoSugerido : null;

        Hilos.IO.execute(() -> {
            SitioDao dao = AppDatabase.get(app).sitioDao();
            Sitio existente = id >= 0 ? dao.buscar(id) : null;
            Sitio s = existente != null ? existente : new Sitio();

            boolean urlCambio = s.url == null || !s.url.equals(fuente);
            if (s.iconoPropio != null && !s.iconoPropio.equals(propio)) {
                IconoHelper.borrar(s.iconoPropio);
            }
            s.nombre = nombre;
            s.url = url;
            s.videoUrl = video;
            s.pais = pais;
            s.iconoPropio = propio;
            s.escritorio = escritorio;
            s.pantallaCompleta = pantallaCompleta;
            s.bloquearAnuncios = bloquearAnuncios;

            if (existente == null) {
                s.orden = dao.maxOrden() + 1;
                s.id = dao.insertar(s);
            } else {
                dao.actualizar(s);
            }

            if (propio == null) {
                String ruta = sugerido != null ? IconoHelper.guardar(app, sugerido, "web_" + s.id) : null;
                if (ruta != null) {
                    IconoHelper.borrar(s.iconoWeb);
                    s.iconoWeb = ruta;
                    dao.actualizar(s);
                } else if (urlCambio || forzarWeb || s.iconoWeb == null) {
                    IconoHelper.refrescarIconoWeb(app, s.id);
                }
            }
        });
        finish();
    }

    @Override
    protected void onDestroy() {
        if (detector != null) detector.cancelar();
        cerrarPrueba();
        manejador.removeCallbacks(consultar);
        super.onDestroy();
    }

    // Base para observar texto sin escribir los metodos que no se usan.
    private abstract static class SimpleWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {
        }

        @Override
        public void onTextChanged(CharSequence s, int a, int b, int c) {
        }
    }
}
